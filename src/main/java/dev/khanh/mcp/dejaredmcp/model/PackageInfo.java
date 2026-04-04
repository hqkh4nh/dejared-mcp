package dev.khanh.mcp.dejaredmcp.model;

/**
 * Summary of a Java package inside a JAR file.
 *
 * @param packageName dot-separated package name (e.g. {@code "com.example.service"})
 * @param classCount  number of classes directly in this package
 */
public record PackageInfo(String packageName, int classCount) {

    @Override
    public String toString() {
        return packageName + " (" + classCount + " classes)";
    }
}
