package dev.khanh.mcp.dejaredmcp.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Explores the structure and contents of JAR files.
 *
 * <p>Provides package/class listing, class name search, and text resource reading.
 * All public methods accept an absolute JAR path and return a human-readable string
 * (or an error message prefixed with {@code "Error:"}).
 */
@Service
public class JarExplorerService {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "yml", "yaml", "xml", "properties", "json", "txt", "sql", "conf"
    );

    /**
     * Lists all packages in a JAR with their class counts, sorted alphabetically.
     *
     * @param jarFilePath absolute path to the JAR file
     * @return one package per line in the format {@code "com.example (5 classes)"}
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
                return "No packages found in " + jarFilePath;
            }

            return packageCounts.entrySet().stream()
                    .map(e -> e.getKey() + " (" + e.getValue() + " classes)")
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            return "Error: Failed to read JAR file: " + e.getMessage();
        }
    }

    /**
     * Lists all classes that are direct members of a specific package (non-recursive).
     *
     * @param jarFilePath absolute path to the JAR file
     * @param packageName dot-separated package name (e.g. {@code "com.example.config"})
     * @return one fully-qualified class name per line, sorted alphabetically
     */
    public String listClasses(String jarFilePath, String packageName) {
        return listClasses(jarFilePath, packageName, false);
    }

    /**
     * Lists classes in a package, optionally including all sub-packages.
     *
     * @param jarFilePath absolute path to the JAR file
     * @param packageName dot-separated package name (e.g. {@code "com.example.config"})
     * @param recursive   if {@code true}, includes classes from all sub-packages
     * @return one fully-qualified class name per line, sorted alphabetically
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
                return "Error: Package '" + packageName + "' not found in " + jarFilePath;
            }

            return String.join("\n", classes);
        } catch (IOException e) {
            return "Error: Failed to read JAR file: " + e.getMessage();
        }
    }

    /**
     * Searches for classes whose simple name contains the given keyword (case-insensitive).
     *
     * @param jarFilePath absolute path to the JAR file
     * @param keyword     search term matched against simple class names
     * @return matching fully-qualified class names, one per line
     */
    public String searchClass(String jarFilePath, String keyword) {
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
                return "No classes found matching '" + keyword + "' in " + jarFilePath;
            }

            return String.join("\n", matches);
        } catch (IOException e) {
            return "Error: Failed to read JAR file: " + e.getMessage();
        }
    }

    /**
     * Reads a text-based resource file from inside a JAR.
     *
     * <p>Only files with supported extensions are allowed:
     * yml, yaml, xml, properties, json, txt, sql, conf.
     *
     * @param jarFilePath  absolute path to the JAR file
     * @param resourcePath path within the JAR (e.g. {@code "application.yml"})
     * @return the resource content as UTF-8 text
     */
    public String readResource(String jarFilePath, String resourcePath) {
        String extension = getExtension(resourcePath);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            return "Error: Resource '" + resourcePath + "' not a supported text format (supported: "
                    + String.join(", ", SUPPORTED_EXTENSIONS) + ")";
        }

        try (var jarFile = new JarFile(jarFilePath)) {
            JarEntry entry = jarFile.getJarEntry(resourcePath);
            if (entry == null) {
                return "Error: Resource '" + resourcePath + "' not found in " + jarFilePath;
            }

            try (var is = jarFile.getInputStream(entry)) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            return "Error: Failed to read resource: " + e.getMessage();
        }
    }

    /**
     * Lists all non-class resource files inside a JAR with their sizes.
     *
     * <p>Filters out {@code .class} files and directory entries, returning
     * resource paths with sizes in a human-readable format. Use this to discover
     * available resources before calling {@link #readResource}.
     *
     * @param jarFilePath absolute path to the JAR file
     * @return one resource per line in the format {@code "path/to/resource (1234 bytes)"},
     *         sorted alphabetically
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
                return "No resource files found in " + jarFilePath;
            }

            return String.join("\n", resources);
        } catch (IOException e) {
            return "Error: Failed to read JAR file: " + e.getMessage();
        }
    }

    private static String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }
}
