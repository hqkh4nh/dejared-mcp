package dev.khanh.mcp.dejaredmcp.service;

import dev.khanh.mcp.dejaredmcp.testutil.TestJarBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class JarExplorerServiceTest {

    static Path testJar;
    static JarExplorerService service;

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void setup() throws IOException {
        testJar = TestJarBuilder.createTestJar(tempDir);
        service = new JarExplorerService(5 * 1024 * 1024); // 5MB limit
    }

    @Test
    void listPackages_returnsAllPackages() {
        var result = service.listPackages(testJar.toString());
        assertTrue(result.contains("com.example.hello"));
        assertTrue(result.contains("com.example.util"));
    }

    @Test
    void listPackages_showsClassCount() {
        var result = service.listPackages(testJar.toString());
        assertTrue(result.contains("2 classes"));
    }

    @Test
    void listClasses_returnsClassesInPackage() {
        var result = service.listClasses(testJar.toString(), "com.example.hello");
        assertTrue(result.contains("com.example.hello.HelloWorld"));
        assertTrue(result.contains("com.example.hello.Greeter"));
        assertFalse(result.contains("StringHelper"));
    }

    @Test
    void listClasses_recursiveIncludesSubPackages() {
        var result = service.listClasses(testJar.toString(), "com.example", true);
        assertTrue(result.contains("com.example.hello.HelloWorld"));
        assertTrue(result.contains("com.example.hello.Greeter"));
        assertTrue(result.contains("com.example.util.StringHelper"));
    }

    @Test
    void listClasses_nonRecursiveExcludesSubPackages() {
        var result = service.listClasses(testJar.toString(), "com.example.hello", false);
        assertTrue(result.contains("com.example.hello.HelloWorld"));
        assertFalse(result.contains("com.example.util.StringHelper"));
    }

    @Test
    void listClasses_unknownPackage() {
        var result = service.listClasses(testJar.toString(), "com.nonexistent");
        assertTrue(result.contains("Error"));
    }

    @Test
    void searchClass_findsMatchingClasses() {
        var result = service.searchClass(testJar.toString(), "Hello");
        assertTrue(result.contains("HelloWorld"));
        assertFalse(result.contains("StringHelper"));
    }

    @Test
    void searchClass_caseInsensitive() {
        var result = service.searchClass(testJar.toString(), "hello");
        assertTrue(result.contains("HelloWorld"));
    }

    @Test
    void searchClass_noMatches() {
        var result = service.searchClass(testJar.toString(), "NonExistent");
        assertTrue(result.contains("No classes found"));
    }

    // --- readResource: content-based detection ---

    @Test
    void readResource_readsPropertiesFile() {
        var result = service.readResource(testJar.toString(), "application.properties");
        assertTrue(result.contains("app.name=TestApp"));
        assertTrue(result.contains("app.version=1.0"));
    }

    @Test
    void readResource_rejectsBinaryFile() {
        var result = service.readResource(testJar.toString(), "data.bin");
        assertTrue(result.contains("Error"));
        assertTrue(result.contains("binary"));
    }

    @Test
    void readResource_notFound() {
        var result = service.readResource(testJar.toString(), "missing.yml");
        assertTrue(result.contains("Error"));
    }

    @Test
    void readResource_rejectsPathTraversal() {
        var result = service.readResource(testJar.toString(), "../../../etc/passwd");
        assertTrue(result.contains("Error"));
        assertFalse(result.contains("etc/passwd"));
    }

    @Test
    void readResource_rejectsAbsolutePath() {
        var result = service.readResource(testJar.toString(), "/etc/passwd");
        assertTrue(result.contains("Error"));
    }

    // --- readResource: size limit ---

    @Test
    void readResource_rejectsOversizedResource() throws IOException {
        Path bigJar = tempDir.resolve("big.jar");
        try (var jos = new JarOutputStream(new java.io.FileOutputStream(bigJar.toFile()))) {
            var entry = new JarEntry("big.txt");
            jos.putNextEntry(entry);
            byte[] data = "A".repeat(100).getBytes();
            jos.write(data);
            jos.closeEntry();
        }
        var smallLimitService = new JarExplorerService(50); // 50 byte limit
        var result = smallLimitService.readResource(bigJar.toString(), "big.txt");
        assertTrue(result.contains("Error"));
        assertTrue(result.contains("too large"));
    }

    // --- listResources ---

    @Test
    void listResources_returnsNonClassFiles() {
        var result = service.listResources(testJar.toString());
        assertTrue(result.contains("application.properties"));
        assertTrue(result.contains("data.bin"));
        assertFalse(result.contains(".class"));
    }

    @Test
    void listResources_emptyJarReturnsMessage() throws IOException {
        Path emptyJar = tempDir.resolve("empty.jar");
        try (var jos = new JarOutputStream(new java.io.FileOutputStream(emptyJar.toFile()))) {
            jos.putNextEntry(new JarEntry("com/example/Foo.class"));
            jos.write(new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE});
            jos.closeEntry();
        }
        var result = service.listResources(emptyJar.toString());
        assertTrue(result.contains("No resource"));
    }

    // --- error sanitization ---

    @Test
    void errorMessages_doNotContainFilePaths() {
        var result = service.listPackages("/nonexistent/path/to/file.jar");
        assertFalse(result.contains("/nonexistent/path"));
    }
}
