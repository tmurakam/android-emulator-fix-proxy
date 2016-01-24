package org.tmurakam.android.emufix;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by tmurakam on 2016/01/24.
 */
public class Server {
    private int fLocalPort;
    private String fProxyServer;
    private int fProxyPort;

    private enum ProxyState {
        READING_STATUS_LINE,
        READING_HEADERS,
        READING_BODY
    }

    public Server(int localPort, String proxyServer, int proxyPort) {
        fLocalPort = localPort;
        fProxyServer = proxyServer;
        fProxyPort = proxyPort;
    }

    public void run() {
        try (ServerSocket server = new ServerSocket(fLocalPort)) {
            while (true) {
                Socket socket = server.accept();
                new ServerForwarderThread(socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ServerForwarderThread extends Thread {
        private Socket fClient;
        private Socket fServer = null;

        public ServerForwarderThread(Socket client) {
            fClient = client;
        }

        public void run() {
            try {
                // connect proxy server
                try {
                    fServer = new Socket(fProxyServer, fProxyPort);
                } catch (UnknownHostException e) {
                    //e.printStackTrace();
                    System.err.println("Unknown host: " + fProxyServer);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // クライアントから先頭6バイトを読む : CONNECT メソッド判別
                byte[] head = new byte[6];
                int len = fClient.getInputStream().read(head);
                fServer.getOutputStream().write(head, 0, len);

                String strHead = new String(head, "UTF-8");

                new PlainForwarderThread(fClient, fServer).start();
                forwarder(strHead.equals("CONNECT"));
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fServer != null) {
                        fServer.close();
                    }
                    fClient.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        private void forwarder(boolean isConnectMethod) throws IOException {
            ProxyState state = ProxyState.READING_STATUS_LINE;

            InputStream serverIn = fServer.getInputStream();
            OutputStream clientOut = fClient.getOutputStream();

            final int BufferSize = 10240;
            byte[] serverBuffer = new byte[BufferSize];

            int headerLength = 0;

            while (true) {
                int ch;
                boolean end = false;

                switch (state) {
                    case READING_STATUS_LINE:
                        ch = serverIn.read();
                        clientOut.write(ch);
                        if (ch == '\n') {
                            state = ProxyState.READING_HEADERS;
                        }
                        break;

                    case READING_HEADERS:
                        ch = serverIn.read();
                        if (isConnectMethod) {
                            // CONNECT の場合は header をスキップする
                            clientOut.write(ch);
                        }
                        if (ch != '\r' && ch != '\n') {
                            headerLength++;
                        }
                        else if (ch == '\n') {
                            if (headerLength == 0) {
                                if (isConnectMethod) {
                                    clientOut.write('\r');
                                    clientOut.write('\n');
                                }
                                state = ProxyState.READING_BODY;
                            }
                            headerLength = 0;
                        }
                        break;

                    case READING_BODY:
                        int len = serverIn.read(serverBuffer, 0, serverBuffer.length);
                        if (len < 0) {
                            end = true;
                            break;
                        }
                        clientOut.write(serverBuffer, 0, len);
                        break;
                }

                if (end) break;
            }

            serverIn.close();
            clientOut.close();
        }
    }

    /**
     * 無加工でフォワードするスレッド
     */
    private static class PlainForwarderThread extends Thread {
        private Socket fInSocket;
        private Socket fOutSocket;

        public PlainForwarderThread(Socket inSocket, Socket outSocket) {
            fInSocket = inSocket;
            fOutSocket = outSocket;
        }

        public void run() {
            try {
                forwarder();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void forwarder() throws IOException {
            InputStream in = fInSocket.getInputStream();
            OutputStream out = fOutSocket.getOutputStream();

            try {
                final int BufferSize = 10240;
                byte[] buffer = new byte[BufferSize];

                while (true) {
                    int len = in.read(buffer, 0, buffer.length);
                    if (len < 0) break;

                    out.write(buffer, 0, len);
                }
            }
            finally {
                in.close();
                out.close();

                fInSocket.close();
                fOutSocket.close();
            }
        }
    }
}
