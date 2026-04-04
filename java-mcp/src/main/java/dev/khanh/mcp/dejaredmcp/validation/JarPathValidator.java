package dev.khanh.mcp.dejaredmcp.validation;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Validates user-supplied paths, class names, package names, and resource paths
 * before they are used in JAR operations.
 *
 * <p>All validation methods return {@code null} on success or an error message on failure.
 * Error messages are sanitized — they never include user-supplied paths or file system details.
 */
public final class JarPathValidator {

    private static final String CLASS_NAME_PATTERN =
            "^[a-zA-Z_$][a-zA-Z0-9_$]*(\\.[a-zA-Z_$][a-zA-Z0-9_$]*)*$";

    private static final String PACKAGE_NAME_PATTERN =
            "^[a-zA-Z_$][a-zA-Z0-9_$]*(\\.[a-zA-Z_$][a-zA-Z0-9_$]*)*$";

    private JarPathValidator() {}

    /**
     * Validates the given JAR file path.
     *
     * <p>Checks: non-null/blank, absolute path, normalizes to resolve traversal,
     * resolves symlinks via {@code toRealPath()}, and verifies {@code .jar} extension.
     *
     * @param jarFilePath the absolute path to validate
     * @return {@code null} if valid, or a sanitized error message
     */
    public static String validate(String jarFilePath) {
        if (jarFilePath == null || jarFilePath.isBlank()) {
            return "Error: JAR file path is required.";
        }

        Path path;
        try {
            path = Path.of(jarFilePath).normalize();
        } catch (Exception e) {
            return "Error: Invalid file path.";
        }

        if (!path.isAbsolute()) {
            return "Error: Absolute path required.";
        }

        Path realPath;
        try {
            realPath = path.toRealPath();
        } catch (IOException e) {
            return "Error: File not found.";
        }

        if (!realPath.toString().toLowerCase().endsWith(".jar")) {
            return "Error: Not a .jar file.";
        }

        return null;
    }

    /**
     * Validates a fully-qualified Java class name (e.g. {@code "com.example.Foo$Bar"}).
     *
     * @param className the class name to validate
     * @return {@code null} if valid, or an error message
     */
    public static String validateClassName(String className) {
        if (className == null || className.isBlank()) {
            return "Error: Class name is required.";
        }
        if (className.contains("..") || className.contains("/") || className.contains("\\")) {
            return "Error: Invalid class name.";
        }
        if (!className.matches(CLASS_NAME_PATTERN)) {
            return "Error: Invalid class name format.";
        }
        return null;
    }

    /**
     * Validates a dot-separated Java package name (e.g. {@code "com.example.service"}).
     *
     * @param packageName the package name to validate
     * @return {@code null} if valid, or an error message
     */
    public static String validatePackageName(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return "Error: Package name is required.";
        }
        if (!packageName.matches(PACKAGE_NAME_PATTERN)) {
            return "Error: Invalid package name format.";
        }
        return null;
    }

    /**
     * Validates a resource path inside a JAR (e.g. {@code "META-INF/spring.factories"}).
     *
     * <p>Rejects null/blank, path traversal ({@code ".."}), and absolute paths.
     *
     * @param resourcePath the resource path to validate
     * @return {@code null} if valid, or an error message
     */
    public static String validateResourcePath(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return "Error: Resource path is required.";
        }
        if (resourcePath.contains("..")) {
            return "Error: Invalid resource path.";
        }
        if (resourcePath.startsWith("/") || resourcePath.startsWith("\\")) {
            return "Error: Resource path must be relative.";
        }
        return null;
    }
}
