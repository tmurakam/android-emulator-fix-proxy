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

        }
    }
}
