package dev.khanh.mcp.dejaredmcp.service;

import dev.khanh.mcp.dejaredmcp.decompiler.CfrEngine;
import dev.khanh.mcp.dejaredmcp.decompiler.ProcyonEngine;
import dev.khanh.mcp.dejaredmcp.decompiler.VineflowerEngine;
import dev.khanh.mcp.dejaredmcp.testutil.TestJarBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DecompilerServiceTest {

    static Path testJar;
    static DecompilerService service;

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void setup() throws IOException {
        testJar = TestJarBuilder.createTestJar(tempDir);
        service = new DecompilerService(
                List.of(new CfrEngine(), new VineflowerEngine(), new ProcyonEngine()),
                500,
                30
        );
    }

    @Test
    void decompileWithCfr_returnsSource() {
        var result = service.decompile(testJar.toString(), "com.example.hello.HelloWorld", "cfr");
        assertTrue(result.contains("HelloWorld") || result.contains("class"), "Should contain class definition");
        assertFalse(result.startsWith("Error"));
    }

    @Test
    void decompileWithProcyon_returnsSource() {
        var result = service.decompile(testJar.toString(), "com.example.hello.HelloWorld", "procyon");
        assertTrue(result.contains("HelloWorld") || result.contains("class"), "Should contain class definition");
        assertFalse(result.startsWith("Error"));
    }

    @Test
    void decompileDefaultEngine_usesCfr() {
        var result = service.decompile(testJar.toString(), "com.example.hello.HelloWorld", null);
        assertFalse(result.startsWith("Error"));
    }

    @Test
    void decompileUnknownEngine() {
        var result = service.decompile(testJar.toString(), "com.example.hello.HelloWorld", "unknown");
        assertTrue(result.contains("Error"));
        assertTrue(result.contains("Unknown engine"));
    }

    @Test
    void decompileClassNotFound() {
        var result = service.decompile(testJar.toString(), "com.example.NonExistent", "cfr");
        assertTrue(result.contains("Error"));
        assertTrue(result.contains("not found"));
    }

    @Test
    void cacheHit_returnsSameResult() {
        String first = service.decompile(testJar.toString(), "com.example.util.StringHelper", "cfr");
        String second = service.decompile(testJar.toString(), "com.example.util.StringHelper", "cfr");
        assertEquals(first, second);
    }

    @Test
    void decompile_rejectsClassNameWithSlash() {
        String result = service.decompile(testJar.toString(), "com/example/hello/HelloWorld", null);
        assertTrue(result.startsWith("Error:"));
        assertTrue(result.contains("Invalid class name"));
    }

    @Test
    void decompile_rejectsClassNameWithDotDot() {
        String result = service.decompile(testJar.toString(), "com..example.HelloWorld", null);
        assertTrue(result.startsWith("Error:"));
    }

    @Test
    void decompile_rejectsNullClassName() {
        String result = service.decompile(testJar.toString(), null, null);
        assertTrue(result.startsWith("Error:"));
    }

    @Test
    void decompile_errorMessagesDoNotLeakPaths() {
        String result = service.decompile("/nonexistent/secret/path.jar", "com.example.Foo", null);
        assertFalse(result.contains("/nonexistent/secret"));
    }
}
