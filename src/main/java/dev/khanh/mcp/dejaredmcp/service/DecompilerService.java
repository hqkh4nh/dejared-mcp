package dev.khanh.mcp.dejaredmcp.service;

import dev.khanh.mcp.dejaredmcp.decompiler.DecompilerEngine;
import dev.khanh.mcp.dejaredmcp.model.DecompileResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.jar.JarFile;

/**
 * Orchestrates Java class decompilation across multiple {@link DecompilerEngine} implementations.
 *
 * <p>Maintains a thread-safe LRU cache (keyed by {@code jarPath:className:engine}) to avoid
 * redundant decompilation. The cache size is configurable via {@code dejared.cache.max-size}
 * (default 500).
 */
@Service
public class DecompilerService {

    private final Map<String, DecompilerEngine> engines;
    private final Map<String, String> cache;

    public DecompilerService(List<DecompilerEngine> engineList,
                             @Value("${dejared.cache.max-size:500}") int maxCacheSize) {
        this.engines = new HashMap<>();
        for (DecompilerEngine engine : engineList) {
            this.engines.put(engine.name(), engine);
        }

        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > maxCacheSize;
            }
        });
    }

    /**
     * Decompiles a class from a JAR file, returning Java source code or an error message.
     *
     * <p>Results are cached; subsequent calls with the same arguments return the cached output.
     * On failure, the error message suggests alternative engines the caller can try.
     *
     * @param jarFilePath absolute path to the JAR file
     * @param className   fully-qualified class name (e.g. {@code "com.example.Foo"})
     * @param engine      engine name ({@code "cfr"}, {@code "vineflower"}, {@code "procyon"});
     *                    defaults to {@code "cfr"} if {@code null} or blank
     * @return decompiled source code, or an error message prefixed with {@code "Error:"}
     */
    public String decompile(String jarFilePath, String className, String engine) {
        String engineName = (engine == null || engine.isBlank()) ? "cfr" : engine.toLowerCase();

        DecompilerEngine decompilerEngine = engines.get(engineName);
        if (decompilerEngine == null) {
            return "Error: Unknown engine '" + engine + "'. Available engines: " + String.join(", ", engines.keySet());
        }

        String cacheKey = jarFilePath + ":" + className + ":" + engineName;
        String cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // Extract class bytes from JAR
        String classPath = className.replace('.', '/') + ".class";
        byte[] classBytes;
        try (var jarFile = new JarFile(jarFilePath)) {
            var entry = jarFile.getJarEntry(classPath);
            if (entry == null) {
                return "Error: Class '" + className + "' not found in " + jarFilePath;
            }
            try (var is = jarFile.getInputStream(entry)) {
                classBytes = is.readAllBytes();
            }
        } catch (IOException e) {
            return "Error: Failed to read JAR file: " + e.getMessage();
        }

        DecompileResult result = decompilerEngine.decompile(classBytes, className);

        if (result.success()) {
            String output = result.content();
            cache.put(cacheKey, output);
            return output;
        } else {
            List<String> alternatives = engines.keySet().stream()
                    .filter(name -> !name.equals(engineName))
                    .sorted()
                    .toList();
            return "Error: " + result.error()
                    + ". Try again with engine=" + String.join(" or engine=", alternatives) + ".";
        }
    }
}
