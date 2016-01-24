package org.tmurakam.android.emufix;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static junit.framework.TestCase.assertEquals;

/**
 * ServerResponse Forwarder Test
 */
public class ServerResponseForwarderTest {
    @Test
    public void testConnect() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 200 OK\r\n");
        sb.append("Server: dummy\r\n");
        sb.append("\r\n");
        sb.append("Content");

        ByteArrayInputStream in = new ByteArrayInputStream(sb.toString().getBytes("UTF-8"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ServerResponseForwarder.forward(in, out, true);

        String os = out.toString("UTF-8");
        assertEquals("HTTP/1.1 200 OK\r\n\r\nContent", os);
    }

    @Test
    public void testNormal() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 200 OK\r\n");
        sb.append("Server: dummy\r\n");
        sb.append("\r\n");
        sb.append("Content");

        ByteArrayInputStream in = new ByteArrayInputStream(sb.toString().getBytes("UTF-8"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ServerResponseForwarder.forward(in, out, false);

        String os = out.toString("UTF-8");
        assertEquals(sb.toString(), os);
    }
}
