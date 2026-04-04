package dev.khanh.mcp.dejaredmcp.model;

/**
 * Result of a decompilation attempt.
 *
 * <p>Use the factory methods {@link #ok(String, String)} and {@link #fail(String, String)}
 * instead of the canonical constructor.
 *
 * @param success    {@code true} if decompilation succeeded
 * @param engineUsed identifier of the engine that performed the decompilation
 * @param content    the decompiled Java source code (present only on success)
 * @param error      an error message (present only on failure)
 */
public record DecompileResult(boolean success, String engineUsed, String content, String error) {

    /** Creates a successful result containing decompiled source code. */
    public static DecompileResult ok(String engineUsed, String sourceCode) {
        return new DecompileResult(true, engineUsed, sourceCode, null);
    }

    /** Creates a failed result containing an error message. */
    public static DecompileResult fail(String engineUsed, String error) {
        return new DecompileResult(false, engineUsed, null, error);
    }
}
