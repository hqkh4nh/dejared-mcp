package dev.khanh.mcp.dejaredmcp;

import dev.khanh.mcp.dejaredmcp.config.ToolConfig;
import dev.khanh.mcp.dejaredmcp.service.BytecodeAnalyzerService;
import dev.khanh.mcp.dejaredmcp.service.DecompilerService;
import dev.khanh.mcp.dejaredmcp.service.JarExplorerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class DejaredMcpApplicationTests {

    @Autowired
    private JarExplorerService jarExplorerService;

    @Autowired
    private BytecodeAnalyzerService bytecodeAnalyzerService;

    @Autowired
    private DecompilerService decompilerService;

    @Autowired
    private ToolConfig toolConfig;

    @Test
    void contextLoads() {
        assertNotNull(jarExplorerService);
        assertNotNull(bytecodeAnalyzerService);
        assertNotNull(decompilerService);
        assertNotNull(toolConfig);
    }
}
