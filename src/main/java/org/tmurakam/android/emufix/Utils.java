package org.tmurakam.android.emufix;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class
 */
public class Utils {
    public static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        while (true) {
            int ch = in.read();
            bos.write(ch);
            if (ch == -1) {
                throw new RuntimeException("No line");
            }
            if (ch == '\n') break;
        }
        return bos.toString("UTF-8");
    }
}
