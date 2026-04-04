package dev.khanh.mcp.dejaredmcp.validation;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Validates absolute paths to JAR files before they are opened.
 *
 * <p>Checks for null/blank input, path traversal ({@code ".."}), file existence,
 * and {@code .jar} extension. Used by {@link dev.khanh.mcp.dejaredmcp.config.ToolConfig}
 * as a guard before all tool operations.
 */
public final class JarPathValidator {

    private JarPathValidator() {}

    /**
     * Validates the given JAR file path.
     *
     * @param jarFilePath the absolute path to validate
     * @return {@code null} if valid, or an error message string prefixed with {@code "Error:"}
     */
    public static String validate(String jarFilePath) {
        if (jarFilePath == null || jarFilePath.isBlank()) {
            return "Error: JAR file path is required.";
        }

        if (jarFilePath.contains("..")) {
            return "Error: Invalid path contains '..': " + jarFilePath;
        }

        Path path = Path.of(jarFilePath);

        if (!Files.exists(path)) {
            return "Error: File not found: " + jarFilePath;
        }

        if (!jarFilePath.toLowerCase().endsWith(".jar")) {
            return "Error: Not a .jar file: " + jarFilePath;
        }

        return null;
    }
}
