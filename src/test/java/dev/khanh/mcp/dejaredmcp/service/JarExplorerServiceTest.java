package dev.khanh.mcp.dejaredmcp.service;

import dev.khanh.mcp.dejaredmcp.testutil.TestJarBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JarExplorerServiceTest {

    static Path testJar;
    static JarExplorerService service;

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void setup() throws IOException {
        testJar = TestJarBuilder.createTestJar(tempDir);
        service = new JarExplorerService();
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
        // com.example.hello has HelloWorld + Greeter = 2 classes
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

    @Test
    void readResource_readsProperties() {
        var result = service.readResource(testJar.toString(), "application.properties");
        assertTrue(result.contains("app.name=TestApp"));
        assertTrue(result.contains("app.version=1.0"));
    }

    @Test
    void readResource_rejectsUnsupportedType() {
        var result = service.readResource(testJar.toString(), "data.bin");
        assertTrue(result.contains("Error"));
        assertTrue(result.contains("not a supported text format"));
    }

    @Test
    void readResource_notFound() {
        var result = service.readResource(testJar.toString(), "missing.yml");
        assertTrue(result.contains("Error"));
        assertTrue(result.contains("not found"));
    }
}
