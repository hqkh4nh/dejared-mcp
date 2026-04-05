# dejared-mcp

An MCP (Model Context Protocol) server that lets AI assistants explore, analyze, and decompile Java JAR files.

## Quick Start

Add to your MCP client configuration:

```json
{
  "mcpServers": {
    "dejared": {
      "command": "npx",
      "args": ["-y", "dejared-mcp"]
    }
  }
}
```

That's it. The wrapper automatically downloads the server JAR on first run.

### Custom Java Path

If Java is not in your system PATH, set the `DEJARED_JAVA_PATH` environment variable:

```json
{
  "mcpServers": {
    "dejared": {
      "command": "npx",
      "args": ["-y", "dejared-mcp"],
      "env": {
        "DEJARED_JAVA_PATH": "/path/to/java"
      }
    }
  }
}
```

## Requirements

- Node.js 18+
- Java 21+ (JRE is sufficient)

## Features

**Discovery** - Browse JAR structure:
- `dejared_list_packages` - List all packages with class counts
- `dejared_list_classes` - List classes in a specific package
- `dejared_list_resources` - List non-class resource files
- `dejared_read_resource` - Read text resources (yml, xml, properties, json, etc.)

**Hunting** - Search inside JARs:
- `dejared_search_class` - Search classes by name
- `dejared_search_string` - Search string literals in bytecode (URLs, SQL, error messages, etc.)

**Deep Analysis** - Inspect and decompile:
- `dejared_get_metadata` - Extract class metadata via ASM (fast, no decompilation)
- `dejared_dump_package_metadata` - Batch metadata for entire packages
- `dejared_decompile_class` - Decompile `.class` to Java source code

## Decompiler Engines

| Engine | Description |
|--------|-------------|
| **CFR** (default) | Reliable general-purpose decompiler |
| **Vineflower** | Modern fork of FernFlower, good with newer Java features |
| **Procyon** | Alternative engine, can handle some edge cases better |

## How It Works

The npm package is a thin Node.js wrapper. On first run it:
1. Checks `~/.dejared-mcp/` for a cached JAR matching the current version
2. Downloads the JAR from GitHub Releases if not cached
3. Spawns `java -jar` with stdio inherited for MCP transport

The server communicates over stdio.

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `dejared.cache.max-size` | `500` | Max entries in the decompilation LRU cache |
| `dejared.security.max-resource-size` | `5242880` | Max resource file size (bytes) |
| `dejared.security.decompile-timeout-seconds` | `30` | Timeout per decompilation |

## Third-Party Licenses

This project uses the following open-source libraries:

| Library | License |
|---------|---------|
| [Spring Boot](https://spring.io/projects/spring-boot) | Apache 2.0 |
| [Spring AI](https://spring.io/projects/spring-ai) | Apache 2.0 |
| [ASM](https://asm.ow2.io/) | BSD 3-Clause |
| [CFR](https://github.com/leibnitz27/cfr) | MIT |
| [Vineflower](https://github.com/Vineflower/vineflower) | Apache 2.0 |
| [Procyon](https://github.com/mstrobel/procyon) | Apache 2.0 |

## License

[MIT](LICENSE)
