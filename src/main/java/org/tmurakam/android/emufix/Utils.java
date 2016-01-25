package org.tmurakam.android.emufix;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Utility class
 */
public class Utils {
    public static ByteBuffer readLine(SocketChannel in) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(10240);

        while (true) {
            buf.limit(buf.position() + 1); // read only 1 byte
            if (in.read(buf) < 0) {
                throw new RuntimeException("No line");
            }

            byte ch = buf.get(buf.position() - 1);
            if (ch == '\n') break;
        }

        buf.flip();
        return buf;
    }
}
