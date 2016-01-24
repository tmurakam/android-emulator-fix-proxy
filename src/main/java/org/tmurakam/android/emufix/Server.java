package org.tmurakam.android.emufix;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Logger;

/**
 * Server main
 */
public class Server {
    private int fLocalPort;
    private String fProxyServer;
    private int fProxyPort;

    private static Logger logger = Logger.getLogger("Server");

    public Server(int localPort, String proxyServer, int proxyPort) {
        fLocalPort = localPort;
        fProxyServer = proxyServer;
        fProxyPort = proxyPort;
    }

    public void run() {
        try (ServerSocket server = new ServerSocket(fLocalPort)) {
            logger.info("Server started");
            while (true) {
                Socket socket = server.accept();

                logger.info("Client connected");
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
                    logger.warning("Unknown proxy host: " + e.getMessage());
                    return;
                } catch (IOException e) {
                    logger.warning("Connect Proxy Server failed: " + e.getMessage());
                    return;
                }

                // Read request line from client
                byte[] requestLineBytes = Utils.readLine(fClient.getInputStream());
                fServer.getOutputStream().write(requestLineBytes);

                String requestLine = new String(requestLineBytes, "UTF-8");
                logger.finer(requestLine);

                // start client -> server forwarder
                new PlainForwarderThread(fClient, fServer).start();

                // run server -> client forwarder
                new ServerResponseForwarder(fServer.getInputStream(), fClient.getOutputStream(), requestLine.startsWith("CONNECT"))
                        .forward();
            }
            catch (Exception e) {
                logger.warning(e.getMessage());
            }
            finally {
                try {
                    fClient.close();
                    if (fServer != null) {
                        fServer.close();
                    }
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Plain forwarder thread
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
                logger.warning(e.getMessage());
            }
        }

        private void forwarder() throws IOException {
            InputStream in = fInSocket.getInputStream();
            OutputStream out = fOutSocket.getOutputStream();

            try {
                final int BufferSize = 10240;
                byte[] buffer = new byte[BufferSize];

                while (true) {
                    int len = in.read(buffer);
                    if (len < 0) {
                        fInSocket.shutdownInput();
                        fOutSocket.shutdownOutput();
                        break;
                    }

                    out.write(buffer, 0, len);
                }
            } catch (SocketException e) {
                logger.warning(e.getMessage());
                fInSocket.close();
                fOutSocket.close();
            }
        }
    }
}
