package dev.khanh.mcp.dejaredmcp.decompiler;

import dev.khanh.mcp.dejaredmcp.model.DecompileResult;

/**
 * Contract for a Java decompiler engine.
 *
 * <p>Implementations wrap a specific decompiler library (e.g. CFR, Vineflower, Procyon)
 * and are auto-discovered by Spring as beans. The {@link DecompilerService} routes
 * requests to the appropriate engine by {@link #name()}.
 */
public interface DecompilerEngine {

    /**
     * Returns the unique engine identifier used for routing (e.g. {@code "cfr"}).
     */
    String name();

    /**
     * Decompiles raw {@code .class} bytes into Java source code.
     *
     * @param classBytes the bytecode of the class to decompile
     * @param className  the fully-qualified class name (e.g. {@code "com.example.Foo"})
     * @return a {@link DecompileResult} indicating success with source code, or failure with an error message
     */
    DecompileResult decompile(byte[] classBytes, String className);
}
