package dev.khanh.mcp.dejaredmcp.config;

import dev.khanh.mcp.dejaredmcp.service.BytecodeAnalyzerService;
import dev.khanh.mcp.dejaredmcp.service.DecompilerService;
import dev.khanh.mcp.dejaredmcp.service.JarExplorerService;
import dev.khanh.mcp.dejaredmcp.validation.JarPathValidator;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Tool(name = "dejared_list_packages",
          description = "List all packages in a JAR file with class counts. Use this first to explore the structure of a JAR.")
    public String listPackages(
            @ToolParam(description = "Absolute path to the JAR file") String jarFilePath) {
        String error = JarPathValidator.validate(jarFilePath);
        if (error != null) return error;
        return jarExplorer.listPackages(jarFilePath);
    }

    @Tool(name = "dejared_list_classes",
          description = "List classes in a package. Set recursive=true to include all sub-packages in one call (avoids multiple round-trips).")
    public String listClasses(
            @ToolParam(description = "Absolute path to the JAR file") String jarFilePath,
            @ToolParam(description = "Package name, e.g. com.example.config") String packageName,
            @ToolParam(description = "If true, include classes from all sub-packages recursively. Default: false", required = false) Boolean recursive) {
        String error = JarPathValidator.validate(jarFilePath);
        if (error != null) return error;
        return jarExplorer.listClasses(jarFilePath, packageName, Boolean.TRUE.equals(recursive));
    }

    @Tool(name = "dejared_read_resource",
          description = "Read a text-based resource file (yml, yaml, xml, properties, json, txt, sql, conf) from inside a JAR.")
    public String readResource(
            @ToolParam(description = "Absolute path to the JAR file") String jarFilePath,
            @ToolParam(description = "Path to the resource inside the JAR, e.g. application.yml or META-INF/spring.factories") String resourcePath) {
        String error = JarPathValidator.validate(jarFilePath);
        if (error != null) return error;
        return jarExplorer.readResource(jarFilePath, resourcePath);
    }

    @Tool(name = "dejared_list_resources",
          description = "List all non-class resource files inside a JAR (config files, properties, XML, etc.) with their sizes. Use this to discover what resources exist before calling dejared_read_resource — avoids guessing file names.")
    public String listResources(
            @ToolParam(description = "Absolute path to the JAR file") String jarFilePath) {
        String error = JarPathValidator.validate(jarFilePath);
        if (error != null) return error;
        return jarExplorer.listResources(jarFilePath);
    }

    // --- Group 2: Hunting ---

    @Tool(name = "dejared_search_class",
          description = "Search for classes by name keyword (case-insensitive) within a JAR file.")
    public String searchClass(
            @ToolParam(description = "Absolute path to the JAR file") String jarFilePath,
            @ToolParam(description = "Keyword to search in class names") String keyword) {
        String error = JarPathValidator.validate(jarFilePath);
        if (error != null) return error;
        return jarExplorer.searchClass(jarFilePath, keyword);
    }

    @Tool(name = "dejared_search_string",
          description = "Search for string literals in bytecode constant pools across all classes in a JAR. Useful for finding hardcoded URLs, SQL queries, error messages, API keys, etc.")
    public String searchString(
            @ToolParam(description = "Absolute path to the JAR file") String jarFilePath,
            @ToolParam(description = "Text to search for (case-insensitive)") String query) {
        String error = JarPathValidator.validate(jarFilePath);
        if (error != null) return error;
        return bytecodeAnalyzer.searchString(jarFilePath, query);
    }

    // --- Group 3: Deep Analysis ---

    @Tool(name = "dejared_get_metadata",
          description = "Extract class metadata using ASM bytecode analysis (no decompilation needed). Returns superclass, interfaces, annotations, fields, and methods with their signatures. Much faster than decompiling — use this to inspect a class before deciding to decompile it.")
    public String getMetadata(
            @ToolParam(description = "Absolute path to the JAR file") String jarFilePath,
            @ToolParam(description = "Fully qualified class name, e.g. com.example.config.DatabaseConfig") String className) {
        String error = JarPathValidator.validate(jarFilePath);
        if (error != null) return error;
        return bytecodeAnalyzer.getMetadata(jarFilePath, className);
    }

    @Tool(name = "dejared_decompile_class",
          description = "Decompile a .class file to Java source code. Default engine is CFR. If decompilation fails or produces poor results (e.g. obfuscated code), retry with engine=vineflower or engine=procyon for better output.")
    public String decompileClass(
            @ToolParam(description = "Absolute path to the JAR file") String jarFilePath,
            @ToolParam(description = "Fully qualified class name, e.g. com.example.config.DatabaseConfig") String className,
            @ToolParam(description = "Decompiler engine: cfr (default), vineflower, or procyon", required = false) String engine) {
        String error = JarPathValidator.validate(jarFilePath);
        if (error != null) return error;
        return decompiler.decompile(jarFilePath, className, engine);
    }

    @Bean
    public ToolCallbackProvider dejaredTools() {
        return MethodToolCallbackProvider.builder()
                .toolObjects(this)
                .build();
    }
}
