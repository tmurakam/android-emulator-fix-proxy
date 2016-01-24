package org.tmurakam.android.emufix;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
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
                String requestLine = Utils.readLine(fClient.getInputStream());
                fServer.getOutputStream().write(requestLine.getBytes("UTF-8"));

                logger.finer(requestLine);

                // start client -> server forwarder
                new PlainForwarderThread(fClient, fServer).start();

                // run server -> client forwarder
                Forwarder.forwardServerResponse(fServer.getInputStream(), fClient.getOutputStream(),
                        requestLine.startsWith("CONNECT"));
            }
            catch (Exception e) {
                logger.warning(e.getMessage());
            }
            finally {
                //logger.info("server closed");
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
                Forwarder.forward(fInSocket.getInputStream(), fOutSocket.getOutputStream());
            } catch (IOException e) {
                logger.warning(e.getMessage());
                try {
                    fInSocket.close();
                    fOutSocket.close();
                } catch (IOException e2) {
                    // ignore
                }
            } finally {
                //logger.info("forwarder stopped");
            }
        }
    }
}
