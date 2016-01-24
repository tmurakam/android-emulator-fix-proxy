package org.tmurakam.android.emufix;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Server Response Forwarder
 */
public class ServerResponseForwarder {
    /**
     * Forward proxy server response
     * @param in InputStream from proxy server
     * @param out OutputStream to client
     * @param isConnectMethod true for CONNECT method
     * @throws IOException
     */
    public static void forward(InputStream in, OutputStream out, boolean isConnectMethod) throws IOException {
        // read status line
        String line = Utils.readLine(in);
        out.write(line.getBytes());

        // read headers
        while(true) {
            line = Utils.readLine(in);

            if (line.equals("\r\n") || line.equals("\n")) {
                out.write(line.getBytes());
                break; // end of header
            }
            if (!isConnectMethod) {
                out.write(line.getBytes());
            }
        }

        // forward body
        final int BufferSize = 10240;
        byte[] serverBuffer = new byte[BufferSize];

        while (true) {
            int len = in.read(serverBuffer);
            if (len < 0) {
                break;
            }
            out.write(serverBuffer, 0, len);
        }
    }
}
