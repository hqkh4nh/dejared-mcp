package dev.khanh.mcp.dejaredmcp.service;

import dev.khanh.mcp.dejaredmcp.validation.JarPathValidator;
import dev.khanh.mcp.dejaredmcp.validation.TextProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Explores the structure and contents of JAR files, providing package listing,
 * class discovery, search, and resource reading capabilities.
 */
@Service
public class JarExplorerService {

    private static final Logger log = LoggerFactory.getLogger(JarExplorerService.class);

    private final long maxResourceSize;

    public JarExplorerService(@Value("${dejared.security.max-resource-size:5242880}") long maxResourceSize) {
        this.maxResourceSize = maxResourceSize;
    }

    /**
     * Lists all packages in a JAR with their class counts.
     *
     * @param jarFilePath path to the JAR file
     * @return formatted list of packages with class counts, or an error message
     */
    public String listPackages(String jarFilePath) {
        try (var jarFile = new JarFile(jarFilePath)) {
            Map<String, Integer> packageCounts = new TreeMap<>();

            jarFile.stream()
                    .map(JarEntry::getName)
                    .filter(name -> name.endsWith(".class"))
                    .forEach(name -> {
                        int lastSlash = name.lastIndexOf('/');
                        String pkg = lastSlash > 0
                                ? name.substring(0, lastSlash).replace('/', '.')
                                : "(default package)";
                        packageCounts.merge(pkg, 1, Integer::sum);
                    });

            if (packageCounts.isEmpty()) {
                return "No packages found.";
            }

            return packageCounts.entrySet().stream()
                    .map(e -> e.getKey() + " (" + e.getValue() + " classes)")
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            log.warn("Failed to read JAR file", e);
            return "Error: Failed to read JAR file.";
        }
    }

    /**
     * Lists all classes that are direct members of the specified package.
     *
     * @param jarFilePath  path to the JAR file
     * @param packageName  fully qualified package name
     * @return formatted list of class names, or an error message
     */
    public String listClasses(String jarFilePath, String packageName) {
        return listClasses(jarFilePath, packageName, false);
    }

    /**
     * Lists classes in a package, optionally including all sub-packages.
     *
     * @param jarFilePath  path to the JAR file
     * @param packageName  fully qualified package name
     * @param recursive    if {@code true}, include classes in sub-packages
     * @return formatted list of class names, or an error message
     */
    public String listClasses(String jarFilePath, String packageName, boolean recursive) {
        try (var jarFile = new JarFile(jarFilePath)) {
            String packagePath = packageName.replace('.', '/') + "/";

            var stream = jarFile.stream()
                    .map(JarEntry::getName)
                    .filter(name -> name.endsWith(".class"))
                    .filter(name -> name.startsWith(packagePath));

            if (!recursive) {
                stream = stream.filter(name -> !name.substring(packagePath.length()).contains("/"));
            }

            List<String> classes = stream
                    .map(name -> name.substring(0, name.length() - 6).replace('/', '.'))
                    .sorted()
                    .toList();

            if (classes.isEmpty()) {
                return "Error: Package not found.";
            }

            return String.join("\n", classes);
        } catch (IOException e) {
            log.warn("Failed to read JAR file", e);
            return "Error: Failed to read JAR file.";
        }
    }

    /**
     * Searches for classes whose simple name contains the given keyword (case-insensitive).
     *
     * @param jarFilePath path to the JAR file
     * @param keyword     search keyword to match against simple class names
     * @return formatted list of matching fully qualified class names, or an error message
     */
    public String searchClass(String jarFilePath, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "Error: Search keyword must not be null or blank.";
        }

        try (var jarFile = new JarFile(jarFilePath)) {
            String lowerKeyword = keyword.toLowerCase();

            List<String> matches = jarFile.stream()
                    .map(JarEntry::getName)
                    .filter(name -> name.endsWith(".class"))
                    .map(name -> name.substring(0, name.length() - 6).replace('/', '.'))
                    .filter(name -> {
                        String simpleName = name.substring(name.lastIndexOf('.') + 1);
                        return simpleName.toLowerCase().contains(lowerKeyword);
                    })
                    .sorted()
                    .toList();

            if (matches.isEmpty()) {
                return "No classes found matching '" + keyword + "'.";
            }

            return String.join("\n", matches);
        } catch (IOException e) {
            log.warn("Failed to read JAR file", e);
            return "Error: Failed to read JAR file.";
        }
    }

    /**
     * Reads a text-based resource file from inside a JAR. Uses content-based detection
     * to reject binary files and enforces a configurable size limit.
     *
     * @param jarFilePath  path to the JAR file
     * @param resourcePath path of the resource entry inside the JAR
     * @return the resource content as a UTF-8 string, or an error message
     */
    public String readResource(String jarFilePath, String resourcePath) {
        String pathError = JarPathValidator.validateResourcePath(resourcePath);
        if (pathError != null) {
            return pathError;
        }

        try (var jarFile = new JarFile(jarFilePath)) {
            JarEntry entry = jarFile.getJarEntry(resourcePath);
            if (entry == null) {
                return "Error: Resource not found.";
            }

            // Zip bomb check
            if (isSuspiciousEntry(entry)) {
                log.warn("Suspicious compression ratio for entry: {}", entry.getName());
                return "Error: Suspicious compression ratio detected.";
            }

            // Size limit check
            if (entry.getSize() != -1 && entry.getSize() > maxResourceSize) {
                return "Error: Resource too large (" + entry.getSize() + " bytes, max " + maxResourceSize + " bytes).";
            }

            // Content-based text detection (first pass)
            try (var probeStream = jarFile.getInputStream(entry)) {
                if (!TextProbe.isLikelyText(probeStream)) {
                    return "Error: Resource appears to be a binary file.";
                }
            }

            // Read full content (second pass, with size limit)
            try (var is = new BoundedInputStream(jarFile.getInputStream(entry), maxResourceSize)) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (ResourceTooLargeException e) {
            return "Error: Resource too large (exceeded " + maxResourceSize + " bytes limit).";
        } catch (IOException e) {
            log.warn("Failed to read resource", e);
            return "Error: Failed to read resource.";
        }
    }

    /**
     * Lists all non-class resource files inside a JAR with their sizes.
     *
     * @param jarFilePath path to the JAR file
     * @return formatted list of resource paths and sizes, or an error message
     */
    public String listResources(String jarFilePath) {
        try (var jarFile = new JarFile(jarFilePath)) {
            List<String> resources = jarFile.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> !entry.getName().endsWith(".class"))
                    .sorted(Comparator.comparing(JarEntry::getName))
                    .map(entry -> entry.getName() + " (" + entry.getSize() + " bytes)")
                    .toList();

            if (resources.isEmpty()) {
                return "No resource files found.";
            }

            return String.join("\n", resources);
        } catch (IOException e) {
            log.warn("Failed to read JAR file", e);
            return "Error: Failed to read JAR file.";
        }
    }

    static boolean isSuspiciousEntry(JarEntry entry) {
        long compressed = entry.getCompressedSize();
        long uncompressed = entry.getSize();
        if (compressed > 0 && uncompressed > 0) {
            return uncompressed / compressed > 100;
        }
        return false;
    }

    private static class ResourceTooLargeException extends IOException {
        ResourceTooLargeException(String message) {
            super(message);
        }
    }

    private static class BoundedInputStream extends InputStream {
        private final InputStream delegate;
        private final long limit;
        private long count;

        BoundedInputStream(InputStream delegate, long limit) {
            this.delegate = delegate;
            this.limit = limit;
        }

        @Override
        public int read() throws IOException {
            if (count >= limit) {
                throw new ResourceTooLargeException("Stream exceeded " + limit + " bytes");
            }
            int b = delegate.read();
            if (b != -1) count++;
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (count >= limit) {
                throw new ResourceTooLargeException("Stream exceeded " + limit + " bytes");
            }
            int maxRead = (int) Math.min(len, limit - count);
            int n = delegate.read(b, off, maxRead);
            if (n > 0) count += n;
            return n;
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
