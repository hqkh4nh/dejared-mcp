package dev.khanh.mcp.dejaredmcp.config;

import dev.khanh.mcp.dejaredmcp.service.BytecodeAnalyzerService;
import dev.khanh.mcp.dejaredmcp.service.DecompilerService;
import dev.khanh.mcp.dejaredmcp.service.JarExplorerService;
import dev.khanh.mcp.dejaredmcp.validation.JarPathValidator;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.context.annotation.Configuration;

/**
 * Registers all MCP tool methods for the dejared server.
 *
 * <p>Each {@code @McpTool}-annotated method is exposed as an MCP tool that LLMs can invoke.
 * Tool descriptions guide efficient usage (e.g. metadata before decompilation).
 * All tools are annotated with {@code @McpAnnotations} hints: read-only, non-destructive,
 * idempotent, and closed-world. All tools validate JAR paths via {@link JarPathValidator}
 * before processing.
 *
 * @see JarExplorerService
 * @see BytecodeAnalyzerService
 * @see DecompilerService
 */
@Configuration(proxyBeanMethods = false)
public class ToolConfig {

    private final JarExplorerService jarExplorer;
    private final BytecodeAnalyzerService bytecodeAnalyzer;
    private final DecompilerService decompiler;

    public ToolConfig(JarExplorerService jarExplorer,
                      BytecodeAnalyzerService bytecodeAnalyzer,
                      DecompilerService decompiler) {
        this.jarExplorer = jarExplorer;
        this.bytecodeAnalyzer = bytecodeAnalyzer;
        this.decompiler = decompiler;
    }

    // --- Group 1: Discovery ---

    @McpTool(name = "dejared_list_packages",
             title = "List Packages",
             description = "List all packages in a JAR file with their class counts.",
             annotations = @McpTool.McpAnnotations(
                 readOnlyHint = true,
                 destructiveHint = false,
                 idempotentHint = true,
                 openWorldHint = false
             ))
    public String listPackages(
            @McpToolParam(description = "Absolute path to the JAR file") String jarFilePath) {
        String error = JarPathValidator.validate(jarFilePath);
        if (error != null) return error;
        return jarExplorer.listPackages(jarFilePath);
    }

    @McpTool(name = "dejared_list_classes",
             title = "List Classes",
             description = "List classes in a package. Use recursive=true to include sub-packages in a single call.",
             annotations = @McpTool.McpAnnotations(
                 readOnlyHint = true,
                 destructiveHint = false,
                 idempotentHint = true,
                 openWorldHint = false
             ))
    public String listClasses(
            @McpToolParam(description = "Absolute path to the JAR file") String jarFilePath,
            @McpToolParam(description = "Package name, e.g. com.example.config") String packageName,
            @McpToolParam(description = "Include sub-packages recursively (default: false)", required = false) Boolean recursive) {
        String error = JarPathValidator.validate(jarFilePath);
        if (error != null) return error;
        return jarExplorer.listClasses(jarFilePath, packageName, Boolean.TRUE.equals(recursive));
    }

    @McpTool(name = "dejared_read_resource",
             title = "Read Resource",
             description = "Read a text resource file from inside a JAR. Binary content is automatically rejected.",
             annotations = @McpTool.McpAnnotations(
                 readOnlyHint = true,
                 destructiveHint = false,
                 idempotentHint = true,
                 openWorldHint = false
             ))
    public String readResource(
            @McpToolParam(description = "Absolute path to the JAR file") String jarFilePath,
            @McpToolParam(description = "Resource path inside the JAR, e.g. application.yml or META-INF/spring.factories") String resourcePath) {
        String error = JarPathValidator.validate(jarFilePath);
        if (error != null) return error;
        return jarExplorer.readResource(jarFilePath, resourcePath);
    }

    @McpTool(name = "dejared_list_resources",
             title = "List Resources",
             description = "List all non-class resource files inside a JAR with their sizes (config, properties, XML, etc.).",
             annotations = @McpTool.McpAnnotations(
                 readOnlyHint = true,
                 destructiveHint = false,
                 idempotentHint = true,
                 openWorldHint = false
             ))
    public String listResources(
            @McpToolParam(description = "Absolute path to the JAR file") String jarFilePath) {
        String error = JarPathValidator.validate(jarFilePath);
        if (error != null) return error;
        return jarExplorer.listResources(jarFilePath);
    }

    // --- Group 2: Hunting ---

    @McpTool(name = "dejared_search_class",
             title = "Search Class",
             description = "Search for classes by name keyword (case-insensitive) across all packages in a JAR.",
             annotations = @McpTool.McpAnnotations(
                 readOnlyHint = true,
                 destructiveHint = false,
                 idempotentHint = true,
                 openWorldHint = false
             ))
    public String searchClass(
            @McpToolParam(description = "Absolute path to the JAR file") String jarFilePath,
            @McpToolParam(description = "Keyword to search in class names") String keyword) {
        String error = JarPathValidator.validate(jarFilePath);
        if (error != null) return error;
        return jarExplorer.searchClass(jarFilePath, keyword);
    }

    @McpTool(name = "dejared_search_string",
             title = "Search String",
             description = "Search for string literals in bytecode constant pools across all classes in a JAR (URLs, SQL, error messages, config values, etc.).",
             annotations = @McpTool.McpAnnotations(
                 readOnlyHint = true,
                 destructiveHint = false,
                 idempotentHint = true,
                 openWorldHint = false
             ))
    public String searchString(
            @McpToolParam(description = "Absolute path to the JAR file") String jarFilePath,
            @McpToolParam(description = "Text to search for (case-insensitive)") String query) {
        String error = JarPathValidator.validate(jarFilePath);
        if (error != null) return error;
        return bytecodeAnalyzer.searchString(jarFilePath, query);
    }

    // --- Group 3: Deep Analysis ---

    @McpTool(name = "dejared_dump_package_metadata",
             title = "Dump Package Metadata",
             description = "Dump structural metadata (annotations, fields, methods) for all classes in one or more packages via ASM bytecode analysis. Accepts multiple packages in one call.",
             annotations = @McpTool.McpAnnotations(
                 readOnlyHint = true,
                 destructiveHint = false,
                 idempotentHint = true,
                 openWorldHint = false
             ))
    public String dumpPackageMetadata(
            @McpToolParam(description = "Absolute path to the JAR file") String jarFilePath,
            @McpToolParam(description = "List of package names, e.g. [\"com.example.service\", \"com.example.config\"]") java.util.List<String> packageNames) {
        String error = JarPathValidator.validate(jarFilePath);
        if (error != null) return error;
        return bytecodeAnalyzer.dumpPackageMetadata(jarFilePath, packageNames);
    }

    @McpTool(name = "dejared_get_metadata",
             title = "Get Class Metadata",
             description = "Extract class metadata via ASM bytecode analysis — superclass, interfaces, annotations, fields, and method signatures. Much faster than decompilation.",
             annotations = @McpTool.McpAnnotations(
                 readOnlyHint = true,
                 destructiveHint = false,
                 idempotentHint = true,
                 openWorldHint = false
             ))
    public String getMetadata(
            @McpToolParam(description = "Absolute path to the JAR file") String jarFilePath,
            @McpToolParam(description = "Fully qualified class name, e.g. com.example.config.DatabaseConfig") String className) {
        String error = JarPathValidator.validate(jarFilePath);
        if (error != null) return error;
        return bytecodeAnalyzer.getMetadata(jarFilePath, className);
    }

    @McpTool(name = "dejared_decompile_class",
             title = "Decompile Class",
             description = "EXPENSIVE: Decompile a class to full Java source code. Output can be very large for complex classes. Check metadata first to decide if decompilation is necessary. Default engine: cfr. Alternatives: vineflower (modern Java features), procyon (obfuscated bytecode).",
             annotations = @McpTool.McpAnnotations(
                 readOnlyHint = true,
                 destructiveHint = false,
                 idempotentHint = true,
                 openWorldHint = false
             ))
    public String decompileClass(
            @McpToolParam(description = "Absolute path to the JAR file") String jarFilePath,
            @McpToolParam(description = "Fully qualified class name, e.g. com.example.config.DatabaseConfig") String className,
            @McpToolParam(description = "Decompiler engine: cfr (default), vineflower, or procyon", required = false) String engine) {
        String error = JarPathValidator.validate(jarFilePath);
        if (error != null) return error;
        return decompiler.decompile(jarFilePath, className, engine);
    }
}
