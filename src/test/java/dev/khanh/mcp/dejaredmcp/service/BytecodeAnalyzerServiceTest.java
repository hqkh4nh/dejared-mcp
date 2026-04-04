package dev.khanh.mcp.dejaredmcp.service;

import dev.khanh.mcp.dejaredmcp.testutil.TestJarBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeAnalyzerServiceTest {

    static Path testJar;
    static BytecodeAnalyzerService service;

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void setup() throws IOException {
        testJar = TestJarBuilder.createTestJar(tempDir);
        service = new BytecodeAnalyzerService();
    }

    @Test
    void getMetadata_showsSuperclass() {
        var result = service.getMetadata(testJar.toString(), "com.example.hello.HelloWorld");
        assertTrue(result.contains("java.lang.Object"));
    }

    @Test
    void getMetadata_showsInterface() {
        var result = service.getMetadata(testJar.toString(), "com.example.hello.HelloWorld");
        assertTrue(result.contains("Greeter"));
    }

    @Test
    void getMetadata_showsAnnotation() {
        var result = service.getMetadata(testJar.toString(), "com.example.hello.HelloWorld");
        assertTrue(result.contains("Deprecated"));
    }

    @Test
    void getMetadata_showsFields() {
        var result = service.getMetadata(testJar.toString(), "com.example.hello.HelloWorld");
        assertTrue(result.contains("message"));
        assertTrue(result.contains("String"));
    }

    @Test
    void getMetadata_showsMethods() {
        var result = service.getMetadata(testJar.toString(), "com.example.hello.HelloWorld");
        assertTrue(result.contains("main"));
        assertTrue(result.contains("greet"));
    }

    @Test
    void getMetadata_classNotFound() {
        var result = service.getMetadata(testJar.toString(), "com.example.NonExistent");
        assertTrue(result.contains("Error"));
        assertTrue(result.contains("not found"));
    }

    @Test
    void dumpPackageMetadata_returnsAllClassesInPackage() {
        var result = service.dumpPackageMetadata(testJar.toString(), List.of("com.example.hello"));
        assertTrue(result.contains("HelloWorld"));
        assertTrue(result.contains("Greeter"));
        assertFalse(result.contains("StringHelper"));
    }

    @Test
    void dumpPackageMetadata_showsFieldsAndMethods() {
        var result = service.dumpPackageMetadata(testJar.toString(), List.of("com.example.hello"));
        assertTrue(result.contains("message"));
        assertTrue(result.contains("greet"));
        assertTrue(result.contains("main"));
    }

    @Test
    void dumpPackageMetadata_showsAnnotations() {
        var result = service.dumpPackageMetadata(testJar.toString(), List.of("com.example.hello"));
        assertTrue(result.contains("Deprecated"));
    }

    @Test
    void dumpPackageMetadata_batchMultiplePackages() {
        var result = service.dumpPackageMetadata(testJar.toString(), List.of("com.example.hello", "com.example.util"));
        assertTrue(result.contains("HelloWorld"));
        assertTrue(result.contains("Greeter"));
        assertTrue(result.contains("StringHelper"));
    }

    @Test
    void dumpPackageMetadata_unknownPackage() {
        var result = service.dumpPackageMetadata(testJar.toString(), List.of("com.nonexistent"));
        assertTrue(result.contains("Error"));
    }

    @Test
    void searchString_findsStringLiteral() {
        var result = service.searchString(testJar.toString(), "Hello from DeJared");
        assertTrue(result.contains("HelloWorld"));
        assertTrue(result.contains("Hello from DeJared!"));
    }

    @Test
    void searchString_findsDbUrl() {
        var result = service.searchString(testJar.toString(), "jdbc");
        assertTrue(result.contains("StringHelper"));
        assertTrue(result.contains("jdbc:mysql"));
    }

    @Test
    void searchString_caseInsensitive() {
        var result = service.searchString(testJar.toString(), "connection failed");
        assertTrue(result.contains("StringHelper"));
    }

    @Test
    void searchString_noMatches() {
        var result = service.searchString(testJar.toString(), "zzz_nonexistent_zzz");
        assertTrue(result.contains("No strings found"));
    }
}
