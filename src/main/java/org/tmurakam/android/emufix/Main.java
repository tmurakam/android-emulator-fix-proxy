package org.tmurakam.android.emufix;

/**
 * Entry class
 */
public class Main {

    public static void main(String[] argv) {
        if (argv.length != 3) {
            usage();
        }

        int localPort = Integer.parseInt(argv[0]);
        String proxyServer = argv[1];
        int proxyPort = Integer.parseInt(argv[2]);

        new Server(localPort, proxyServer, proxyPort).run();
    }

    private static void usage() {
        System.out.println("Usage: emufix-proxy [local port] [upstream proxy addr] [upstream proxy port]");
        System.exit(1);
    }
}
