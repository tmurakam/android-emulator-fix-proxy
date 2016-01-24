package org.tmurakam.android.emufix;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Forwarder
 */
public class Forwarder {
    /**
     * Forward proxy server response
     * @param in InputStream from proxy server
     * @param out OutputStream to client
     * @param isConnectMethod true for CONNECT method
     * @throws IOException
     */
    public static void forwardServerResponse(InputStream in, OutputStream out, boolean isConnectMethod) throws IOException {
        // read status line
        String line = Utils.readLine(in);
        out.write(line.getBytes("UTF-8"));

        // read headers
        while(true) {
            line = Utils.readLine(in);

            if (line.equals("\r\n") || line.equals("\n")) {
                out.write(line.getBytes("UTF-8"));
                break; // end of header
            }
            if (!isConnectMethod) {
                out.write(line.getBytes("UTF-8"));
            }
        }

        forward(in, out);
    }

    /**
     * Plain forwarder
     * @param in
     * @param out
     * @throws IOException
     */
    public static void forward(InputStream in, OutputStream out) throws IOException {
        final int BufferSize = 10240;
        byte[] buffer = new byte[BufferSize];

        while (true) {
            int len = in.read(buffer);
            if (len < 0) {
                break;
            }
            out.write(buffer, 0, len);
        }
    }
}
