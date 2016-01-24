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
            new ProxyThread(socket).run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ProxyThread extends Thread {
        private Socket fClient;
        private Socket fServer = null;

        public ProxyThread(Socket client) {
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
            ProxyState state = ProxyState.READING_STATUS_LINE;

            InputStream clientIn = fClient.getInputStream();
            OutputStream clientOut = fClient.getOutputStream();

            InputStream serverIn = fServer.getInputStream();
            OutputStream serverOut = fServer.getOutputStream();

            final int BufferSize = 10240;
            byte[] clientBuffer = new byte[BufferSize];
            byte[] serverBuffer = new byte[BufferSize];

            int lineLength = 0;

            while (true) {
                // proxy client -> server
                int len = clientIn.available();
                if (len > 0) {
                    if (len > BufferSize) {
                        len = BufferSize;
                    }
                    len = clientIn.read(clientBuffer, 0, len);
                    if (len > 0) {
                        serverOut.write(clientBuffer, 0, len);
                    }
                }

                // proxy server -> client
                len = serverIn.available();
                if (len == 0) continue;

                int ch;

                switch (state) {
                    case READING_BODY:
                        if (len > BufferSize) {
                            len = BufferSize;
                        }
                        len = serverIn.read(serverBuffer, 0, len);
                        if (len > 0) {
                            clientOut.write(serverBuffer, 0, len);
                        }
                        break;

                    case READING_STATUS_LINE:
                        ch = serverIn.read();
                        clientOut.write(ch);
                        if (ch == '\n') {
                            state = ProxyState.READING_HEADERS;
                            lineLength = 0;
                        }
                        break;

                    case READING_HEADERS:
                        ch = serverIn.read();
                        if (ch != '\r' && ch != '\n') {
                            lineLength++;
                        }
                        if (ch == '\n') {
                            if (lineLength == 0) {
                                clientOut.write('\r');
                                clientOut.write('\n');
                                state = ProxyState.READING_BODY;
                            }
                            lineLength = 0;
                        }
                        break;
                    }
                }
            }
        }
    }
}
