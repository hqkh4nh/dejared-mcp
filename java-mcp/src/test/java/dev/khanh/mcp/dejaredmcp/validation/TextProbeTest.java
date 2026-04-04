package dev.khanh.mcp.dejaredmcp.validation;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class TextProbeTest {

    @Test
    void detectsPlainTextAsText() throws IOException {
        byte[] content = "Hello, World!\nThis is plain text.".getBytes(StandardCharsets.UTF_8);
        assertTrue(TextProbe.isLikelyText(new ByteArrayInputStream(content)));
    }

    @Test
    void detectsJsonAsText() throws IOException {
        byte[] content = "{\"key\": \"value\", \"number\": 42}".getBytes(StandardCharsets.UTF_8);
        assertTrue(TextProbe.isLikelyText(new ByteArrayInputStream(content)));
    }

    @Test
    void detectsYamlAsText() throws IOException {
        byte[] content = "spring:\n  application:\n    name: test\n".getBytes(StandardCharsets.UTF_8);
        assertTrue(TextProbe.isLikelyText(new ByteArrayInputStream(content)));
    }

    @Test
    void detectsXmlAsText() throws IOException {
        byte[] content = "<?xml version=\"1.0\"?>\n<root><item>value</item></root>".getBytes(StandardCharsets.UTF_8);
        assertTrue(TextProbe.isLikelyText(new ByteArrayInputStream(content)));
    }

    @Test
    void detectsPropertiesAsText() throws IOException {
        byte[] content = "app.name=TestApp\napp.version=1.0\n".getBytes(StandardCharsets.UTF_8);
        assertTrue(TextProbe.isLikelyText(new ByteArrayInputStream(content)));
    }

    @Test
    void detectsSqlAsText() throws IOException {
        byte[] content = "SELECT * FROM users WHERE id = 1;\n".getBytes(StandardCharsets.UTF_8);
        assertTrue(TextProbe.isLikelyText(new ByteArrayInputStream(content)));
    }

    @Test
    void detectsUtf8WithAccentsAsText() throws IOException {
        byte[] content = "Héllo Wörld! Ñoño café résumé".getBytes(StandardCharsets.UTF_8);
        assertTrue(TextProbe.isLikelyText(new ByteArrayInputStream(content)));
    }

    @Test
    void detectsEmptyContentAsText() throws IOException {
        assertTrue(TextProbe.isLikelyText(new ByteArrayInputStream(new byte[0])));
    }

    @Test
    void detectsNullBytesAsBinary() throws IOException {
        byte[] content = new byte[]{0x00, 0x01, 0x02, 0x03};
        assertFalse(TextProbe.isLikelyText(new ByteArrayInputStream(content)));
    }

    @Test
    void detectsClassBytecodeAsBinary() throws IOException {
        byte[] content = new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, 0x00, 0x00, 0x00, 0x41};
        assertFalse(TextProbe.isLikelyText(new ByteArrayInputStream(content)));
    }

    @Test
    void detectsPngHeaderAsBinary() throws IOException {
        byte[] content = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        assertFalse(TextProbe.isLikelyText(new ByteArrayInputStream(content)));
    }

    @Test
    void detectsRandomBinaryAsBinary() throws IOException {
        byte[] content = new byte[256];
        for (int i = 0; i < 256; i++) content[i] = (byte) i;
        assertFalse(TextProbe.isLikelyText(new ByteArrayInputStream(content)));
    }

    @Test
    void detectsWhitespaceOnlyAsText() throws IOException {
        byte[] content = "   \t\n\r\n   \t".getBytes(StandardCharsets.UTF_8);
        assertTrue(TextProbe.isLikelyText(new ByteArrayInputStream(content)));
    }
}
