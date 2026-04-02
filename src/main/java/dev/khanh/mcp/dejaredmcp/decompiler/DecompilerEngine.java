package dev.khanh.mcp.dejaredmcp.decompiler;

import dev.khanh.mcp.dejaredmcp.model.DecompileResult;

public interface DecompilerEngine {

    String name();

    DecompileResult decompile(byte[] classBytes, String className);
}
