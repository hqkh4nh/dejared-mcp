package dev.khanh.mcp.dejaredmcp.decompiler;

import dev.khanh.mcp.dejaredmcp.model.DecompileResult;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;

@Component
public class VineflowerEngine implements DecompilerEngine {

    @Override
    public String name() {
        return "vineflower";
    }

    @Override
    public DecompileResult decompile(byte[] classBytes, String className) {
        String internalName = className.replace('.', '/');

        try {
            var saver = new InMemoryResultSaver();

            Map<String, Object> options = new HashMap<>();
            options.put("rbr", "1");  // remove bridge methods
            options.put("dgs", "1");  // decompile generic signatures
            options.put("din", "1");  // decompile inner classes
            options.put("asc", "1");  // ASCII string characters
            options.put("hdc", "1");  // hide default constructor
            options.put("log", "WARN");

            IBytecodeProvider provider = (externalPath, internalPath) -> classBytes;

            Fernflower fernflower = new Fernflower(provider, saver, options, null);

            // Add the source (using the class file path as the external path)
            fernflower.addSource(new java.io.File(internalName + ".class"));
            fernflower.decompileContext();

            String source = saver.getResult();
            if (source == null || source.isBlank()) {
                return DecompileResult.fail("vineflower", "Vineflower produced empty output for " + className);
            }
            return DecompileResult.ok("vineflower", source);
        } catch (Exception e) {
            return DecompileResult.fail("vineflower", "Vineflower failed to decompile " + className + ": " + e.getMessage());
        }
    }

    private static class InMemoryResultSaver implements IResultSaver {
        private String result;

        String getResult() {
            return result;
        }

        @Override
        public void saveClassFile(String path, String qualifiedName,
                                  String entryName, String content, int[] mapping) {
            this.result = content;
        }

        @Override public void saveFolder(String path) {}
        @Override public void copyFile(String source, String path, String entryName) {}
        @Override public void createArchive(String path, String archiveName, Manifest manifest) {}
        @Override public void saveDirEntry(String path, String archiveName, String entryName) {}
        @Override public void copyEntry(String source, String path, String archiveName, String entry) {}
        @Override public void saveClassEntry(String path, String archiveName,
                                             String qualifiedName, String entryName, String content) {}
        @Override public void closeArchive(String path, String archiveName) {}
    }
}
