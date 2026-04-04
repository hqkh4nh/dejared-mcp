package dev.khanh.mcp.dejaredmcp.validation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Probes the first bytes of a stream to determine if the content is likely text or binary.
 *
 * <p>Uses a three-check approach: null byte detection, UTF-8 validation,
 * and control character ratio analysis. This replaces hardcoded extension allowlists
 * with content-based detection.
 */
public final class TextProbe {

    private static final int PROBE_SIZE = 8192;

    private TextProbe() {}

    /**
     * Returns {@code true} if the content from the stream appears to be text.
     *
     * <p>Reads up to 8192 bytes and checks:
     * <ol>
     *   <li>No null bytes (binary indicator, same heuristic as Git)</li>
     *   <li>Valid UTF-8 encoding</li>
     *   <li>Control character ratio below 5% (excluding tab, newline, carriage return)</li>
     * </ol>
     *
     * @param is the input stream to probe (will consume up to 8192 bytes)
     * @return {@code true} if content appears to be text
     * @throws IOException if reading from the stream fails
     */
    public static boolean isLikelyText(InputStream is) throws IOException {
        byte[] buffer = is.readNBytes(PROBE_SIZE);
        if (buffer.length == 0) {
            return true;
        }

        // 1. Null byte = binary
        for (byte b : buffer) {
            if (b == 0) {
                return false;
            }
        }

        // 2. Must be valid UTF-8
        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            decoder.decode(ByteBuffer.wrap(buffer));
        } catch (CharacterCodingException e) {
            return false;
        }

        // 3. Low control character ratio (< 5%)
        int controlCount = 0;
        for (byte b : buffer) {
            int unsigned = b & 0xFF;
            if (unsigned < 0x20 && unsigned != '\t' && unsigned != '\n' && unsigned != '\r') {
                controlCount++;
            }
        }
        return controlCount <= buffer.length * 0.05;
    }
}
