package dev.khanh.mcp.dejaredmcp.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JarPathValidatorTest {

    @TempDir
    Path tempDir;

    // --- validate() ---

    @Test
    void validJarPath() throws IOException {
        Path jar = tempDir.resolve("test.jar");
        Files.write(jar, new byte[]{0x50, 0x4B, 0x03, 0x04});
        assertNull(JarPathValidator.validate(jar.toString()));
    }

    @Test
    void rejectsNull() {
        assertNotNull(JarPathValidator.validate(null));
    }

    @Test
    void rejectsBlank() {
        assertNotNull(JarPathValidator.validate("  "));
    }

    @Test
    void rejectsNonExistentFile() {
        String result = JarPathValidator.validate("/nonexistent/file.jar");
        assertNotNull(result);
        // Must not contain the input path (sanitized)
        assertFalse(result.contains("/nonexistent"));
    }

    @Test
    void rejectsNonJarExtension() throws IOException {
        Path txt = tempDir.resolve("file.txt");
        Files.writeString(txt, "hello");
        String result = JarPathValidator.validate(txt.toString());
        assertNotNull(result);
        // Must not contain the input path (sanitized)
        assertFalse(result.contains(tempDir.toString()));
    }

    @Test
    void rejectsPathTraversal() {
        String result = JarPathValidator.validate("/some/../etc/passwd.jar");
        assertNotNull(result);
    }

    @Test
    void rejectsRelativePath() {
        String result = JarPathValidator.validate("relative/path/test.jar");
        assertNotNull(result);
    }

    @Test
    void errorMessagesDoNotContainFilePaths() throws IOException {
        Path txt = tempDir.resolve("file.txt");
        Files.writeString(txt, "hello");

        String result = JarPathValidator.validate(txt.toString());
        assertNotNull(result);
        assertFalse(result.contains(txt.toString()), "Error should not contain input path");
    }

    // --- validateClassName() ---

    @Test
    void validClassName() {
        assertNull(JarPathValidator.validateClassName("com.example.MyClass"));
    }

    @Test
    void validInnerClassName() {
        assertNull(JarPathValidator.validateClassName("com.example.Outer$Inner"));
    }

    @Test
    void rejectsNullClassName() {
        assertNotNull(JarPathValidator.validateClassName(null));
    }

    @Test
    void rejectsClassNameWithSlash() {
        assertNotNull(JarPathValidator.validateClassName("com/example/MyClass"));
    }

    @Test
    void rejectsClassNameWithDotDot() {
        assertNotNull(JarPathValidator.validateClassName("com..example.MyClass"));
    }

    @Test
    void rejectsClassNameWithBackslash() {
        assertNotNull(JarPathValidator.validateClassName("com\\example\\MyClass"));
    }

    // --- validatePackageName() ---

    @Test
    void validPackageName() {
        assertNull(JarPathValidator.validatePackageName("com.example.service"));
    }

    @Test
    void rejectsNullPackageName() {
        assertNotNull(JarPathValidator.validatePackageName(null));
    }

    @Test
    void rejectsPackageNameWithSlash() {
        assertNotNull(JarPathValidator.validatePackageName("com/example/service"));
    }

    // --- validateResourcePath() ---

    @Test
    void validResourcePath() {
        assertNull(JarPathValidator.validateResourcePath("application.yml"));
    }

    @Test
    void validNestedResourcePath() {
        assertNull(JarPathValidator.validateResourcePath("META-INF/spring.factories"));
    }

    @Test
    void rejectsNullResourcePath() {
        assertNotNull(JarPathValidator.validateResourcePath(null));
    }

    @Test
    void rejectsResourcePathWithDotDot() {
        assertNotNull(JarPathValidator.validateResourcePath("../../../etc/passwd"));
    }

    @Test
    void rejectsResourcePathStartingWithSlash() {
        assertNotNull(JarPathValidator.validateResourcePath("/etc/passwd"));
    }

    @Test
    void rejectsResourcePathStartingWithBackslash() {
        assertNotNull(JarPathValidator.validateResourcePath("\\etc\\passwd"));
    }
}
