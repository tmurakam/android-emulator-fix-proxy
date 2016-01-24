package org.tmurakam.android.emufix;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
            Socket socket = server.accept();
            new ClientForwarderThread(socket).run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientForwarderThread extends Thread {
        private Socket fClient;
        private Socket fServer = null;

        public ClientForwarderThread(Socket client) {
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

                new ServerForwarderThread(fClient, fServer).run();

                forwarder();
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

        private void forwarder() throws IOException {
            InputStream clientIn = fClient.getInputStream();
            OutputStream serverOut = fServer.getOutputStream();

            final int BufferSize = 10240;
            byte[] clientBuffer = new byte[BufferSize];

            while (true) {
                // proxy client -> server
                int len = clientIn.read(clientBuffer, 0, clientBuffer.length);
                if (len < 0) break;

                serverOut.write(clientBuffer, 0, len);
            }

            clientIn.close();
            serverOut.close();
        }
    }

    private static class ServerForwarderThread extends Thread {
        private Socket fClient;
        private Socket fServer;

        public ServerForwarderThread(Socket client, Socket server) {
            fClient = client;
            fServer = server;
        }

        private void forwarder() throws IOException {
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
                    case READING_BODY:
                        int len = serverIn.read(serverBuffer, 0, serverBuffer.length);
                        if (len < 0) {
                            end = true;
                            break;
                        }
                        clientOut.write(serverBuffer, 0, len);
                        break;

                    case READING_STATUS_LINE:
                        ch = serverIn.read();
                        clientOut.write(ch);
                        if (ch == '\n') {
                            state = ProxyState.READING_HEADERS;
                        }
                        break;

                    case READING_HEADERS:
                        ch = serverIn.read();
                        if (ch != '\r' && ch != '\n') {
                            headerLength++;
                        }
                        else if (ch == '\n') {
                            if (headerLength == 0) {
                                clientOut.write('\r');
                                clientOut.write('\n');
                                state = ProxyState.READING_BODY;
                            }
                            headerLength = 0;
                        }
                        break;
                }

                if (end) break;
            }

            serverIn.close();
            clientOut.close();
        }
    }
}
