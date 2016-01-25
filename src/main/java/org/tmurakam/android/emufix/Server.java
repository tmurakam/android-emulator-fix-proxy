package org.tmurakam.android.emufix;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
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
        try (ServerSocketChannel server = ServerSocketChannel.open()) {
            server.bind(new InetSocketAddress(fLocalPort));
            logger.info("Server started");

            while (true) {
                SocketChannel socket = server.accept();

                logger.info("Client connected");
                new ServerForwarderThread(socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ServerForwarderThread extends Thread {
        private SocketChannel fClient;
        private SocketChannel fServer = null;

        public ServerForwarderThread(SocketChannel client) {
            fClient = client;
        }

        public void run() {
            try {
                // connect proxy server
                try {
                    fServer = SocketChannel.open();
                    fServer.connect(new InetSocketAddress(fProxyServer, fProxyPort));
                } catch (UnknownHostException e) {
                    logger.warning("Unknown proxy host: " + e.getMessage());
                    return;
                } catch (IOException e) {
                    logger.warning("Connect Proxy Server failed: " + e.getMessage());
                    return;
                }

                // Read request line from client
                ByteBuffer requestLine = Utils.readLine(fClient);
                fServer.write(requestLine);

                String strRequestLine = new String(requestLine.array(), "UTF-8");
                logger.finer(strRequestLine);

                mainLoop(strRequestLine.startsWith("CONNECT"));
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

        private void mainLoop(boolean isConnectMethod) throws IOException {
            fServer.configureBlocking(false);
            fClient.configureBlocking(false);

            Selector selector = Selector.open();
            fServer.register(selector, SelectionKey.OP_READ);
            fClient.register(selector, SelectionKey.OP_READ);

            while (selector.select() > 0) {
                for (SelectionKey key : selector.selectedKeys()) {
                    if (key.channel() == fServer) {
                        // TODO:
                    }
                    else if (key.channel() == fClient) {
                        // TODO:
                    }
                }
            }
        }
    }

    /**
     * Plain forwarder thread
     */
    /*
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
    */
}
