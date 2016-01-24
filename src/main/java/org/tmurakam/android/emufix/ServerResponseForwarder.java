package org.tmurakam.android.emufix;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Server Response Forwarder
 */
public class ServerResponseForwarder {
    private enum ProxyState {
        READING_STATUS_LINE,
        READING_HEADERS,
        READING_BODY
    }


    private InputStream in;
    private OutputStream out;
    private boolean isConnectMethod;

    public ServerResponseForwarder(InputStream serverIn, OutputStream clientOut, boolean isConnectMethod) {
        this.in = serverIn;
        this.out = clientOut;
        this.isConnectMethod = isConnectMethod;
    }

    public void forward() throws IOException {
        final int BufferSize = 10240;
        byte[] serverBuffer = new byte[BufferSize];

        ProxyState state = ProxyState.READING_STATUS_LINE;
        int headerLength = 0;

        while (true) {
            int ch;
            boolean end = false;

            switch (state) {
                case READING_STATUS_LINE:
                    ch = in.read();
                    if (ch == -1) {
                        end = true;
                        break;
                    }
                    out.write(ch);
                    if (ch == '\n') {
                        state = ProxyState.READING_HEADERS;
                    }
                    break;

                case READING_HEADERS:
                    ch = in.read();
                    if (ch == -1) {
                        end = true;
                        break;
                    }
                    if (!isConnectMethod) { // skip response header for CONNECT method.
                        out.write(ch);
                    }
                    if (ch != '\r' && ch != '\n') {
                        headerLength++;
                    }
                    else if (ch == '\n') {
                        if (headerLength == 0) {
                            if (isConnectMethod) {
                                out.write('\r');
                                out.write('\n');
                            }
                            state = ProxyState.READING_BODY;
                        }
                        headerLength = 0;
                    }
                    break;

                case READING_BODY:
                    int len = in.read(serverBuffer);
                    if (len < 0) {
                        end = true;
                        break;
                    }
                    out.write(serverBuffer, 0, len);
                    break;
            }

            if (end) break;
        }
    }
}
