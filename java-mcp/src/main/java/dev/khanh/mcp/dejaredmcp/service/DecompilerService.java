package dev.khanh.mcp.dejaredmcp.service;

import dev.khanh.mcp.dejaredmcp.decompiler.DecompilerEngine;
import dev.khanh.mcp.dejaredmcp.model.DecompileResult;
import dev.khanh.mcp.dejaredmcp.validation.JarPathValidator;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.JarFile;

/**
 * Orchestrates Java class decompilation across multiple {@link DecompilerEngine} implementations.
 *
 * <p>Features: LRU result caching (configurable size), timeout enforcement per decompilation,
 * engine selection by name with automatic fallback suggestions on failure.
 */
@Service
public class DecompilerService {

    private static final Logger log = LoggerFactory.getLogger(DecompilerService.class);

    private final Map<String, DecompilerEngine> engines;
    private final Map<String, String> cache;
    private final int decompileTimeoutSeconds;
    private final ExecutorService decompileExecutor;

    public DecompilerService(List<DecompilerEngine> engineList,
                             @Value("${dejared.cache.max-size:500}") int maxCacheSize,
                             @Value("${dejared.security.decompile-timeout-seconds:30}") int decompileTimeoutSeconds) {
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

        this.decompileTimeoutSeconds = decompileTimeoutSeconds;
        this.decompileExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "decompile-worker");
            t.setDaemon(true);
            return t;
        });
    }

    @PreDestroy
    void shutdown() {
        decompileExecutor.shutdownNow();
    }

    /**
     * Decompiles a class from a JAR file, returning Java source code or an error message.
     *
     * @param jarFilePath path to the JAR file
     * @param className   fully qualified class name
     * @param engine      decompiler engine name (e.g. "cfr"), or {@code null} for default
     * @return decompiled source code, or an error message prefixed with "Error:"
     */
    public String decompile(String jarFilePath, String className, String engine) {
        // Validate class name
        String classError = JarPathValidator.validateClassName(className);
        if (classError != null) {
            return classError;
        }

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
                return "Error: Class not found.";
            }

            // Zip bomb check
            if (JarExplorerService.isSuspiciousEntry(entry)) {
                return "Error: Suspicious compression ratio detected.";
            }

            try (var is = jarFile.getInputStream(entry)) {
                classBytes = is.readAllBytes();
            }
        } catch (IOException e) {
            log.warn("Failed to read JAR file", e);
            return "Error: Failed to read JAR file.";
        }

        // Decompile with timeout
        Future<DecompileResult> future = decompileExecutor.submit(
                () -> decompilerEngine.decompile(classBytes, className)
        );
        DecompileResult result;
        try {
            result = future.get(decompileTimeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            return "Error: Decompilation timed out after " + decompileTimeoutSeconds + " seconds.";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: Decompilation was interrupted.";
        } catch (ExecutionException e) {
            log.warn("Decompilation failed", e);
            return "Error: Decompilation failed.";
        }

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
