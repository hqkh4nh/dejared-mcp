[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=flat-square&logo=openjdk&logoColor=white)]()
[![Node.js](https://img.shields.io/badge/Node.js-20%2B-339933?style=flat-square&logo=node.js&logoColor=white)]()
[![npm version](https://img.shields.io/npm/v/dejared-mcp?style=flat-square&logo=npm&logoColor=white)](https://www.npmjs.com/package/dejared-mcp)
[![npm downloads](https://img.shields.io/npm/dt/dejared-mcp?style=flat-square&color=cb3837&label=downloads)](https://www.npmjs.com/package/dejared-mcp)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

# dejared-mcp

A Model Context Protocol (MCP) server for exploring, analyzing, and decompiling Java JAR files.
Designed for AI-powered development tools, dejared-mcp connects your IDE or CLI assistant
to inspect package structures, search bytecode, and decompile classes
using CFR, Vineflower, or Procyon.

[![Claude Code](https://img.shields.io/badge/Claude_Code-plugin-D97757?style=for-the-badge&logo=claude&logoColor=white)](#claude-code)
[![Codex](https://img.shields.io/badge/Codex-plugin-000000?style=for-the-badge&logo=openai&logoColor=white)](#codex)
[![Install in VS Code](https://img.shields.io/badge/Install_in-VS_Code-0098FF?style=for-the-badge&logo=visualstudiocode&logoColor=white)](https://vscode.dev/redirect/mcp/install?name=dejared&config=%7B%22type%22%3A%22stdio%22%2C%22command%22%3A%22npx%22%2C%22args%22%3A%5B%22-y%22%2C%22dejared-mcp%22%5D%7D)
[![Add to Cursor](https://img.shields.io/badge/Add_to-Cursor-000000?style=for-the-badge&logo=cursor&logoColor=white)](https://cursor.com/install-mcp?name=dejared&config=eyJjb21tYW5kIjoibnB4IiwiYXJncyI6WyIteSIsImRlamFyZWQtbWNwIl19)

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Install as a plugin (recommended)](#install-as-a-plugin-recommended)
- [Install as an MCP server](#install-as-an-mcp-server)
- [Features](#features)
- [Decompiler Engines](#decompiler-engines)
- [Custom Java Path](#custom-java-path)
- [Server Configuration](#server-configuration)
- [How It Works](#how-it-works)
- [FAQ and Troubleshooting](#faq-and-troubleshooting)
- [Contributing](#contributing)
- [Third-Party Licenses](#third-party-licenses)
- [License](#license)

## Overview

dejared-mcp is a Java-based MCP server distributed as an npm package.
It provides nine tools organized into three categories: discovery, hunting,
and deep analysis. AI assistants use these tools to navigate JAR file
structures, search for classes and string literals in bytecode, and
decompile `.class` files back to readable Java source code.

Any ZIP-based Java archive works: `.jar`, `.war`, `.ear`, `.aar`, and
Spring Boot fat-jars.

The npm package acts as a thin wrapper that downloads and caches the
server JAR on first run, then spawns it via `java -jar` using stdio
transport.

## Prerequisites

- **Node.js** 20 or later
- **Java** 17 or later (JRE is sufficient)

## Quick Start

The best install is the plugin. One command pair gives your agent both halves:
the **nine MCP tools**, and the **`jar-analysis` skill** that tells it when to
reach for them instead of shelling out to `jar tf`, `unzip`, or `javap`.

```bash
claude plugin marketplace add hqkh4nh/dejared-mcp
claude plugin install dejared@dejared-mcp-marketplace
```

Codex carries the same bundle. See
[Install as a plugin](#install-as-a-plugin-recommended).
Every other client takes the [plain MCP config](#install-as-an-mcp-server).

## Install as a plugin (recommended)

The MCP server on its own gives an agent nine tools. It does not tell the agent
*when* to use them, so most agents keep reaching for `jar tf`, `unzip -p`, and
`javap -p` out of habit and never touch the tools you installed.

A plugin ships both parts together:

| Part | What it does |
|------|--------------|
| MCP server | The nine tools listed under [Features](#features) |
| `jar-analysis` skill | Routes JAR questions to those tools, picks the cheap tool before the expensive one, and knows which decompiler to retry with |

Two tools support that bundle today. Everything else is
[MCP-only](#install-as-an-mcp-server).

### Claude Code

```bash
claude plugin marketplace add hqkh4nh/dejared-mcp
claude plugin install dejared@dejared-mcp-marketplace
```

Inside a session, the same commands work as `/plugin marketplace add ...` and
`/plugin install ...`. If the install summary says `Run /reload-plugins to
activate`, run it.

### Codex

```bash
codex plugin marketplace add hqkh4nh/dejared-mcp
codex plugin add dejared@dejared-mcp-marketplace
```

Start a new session afterwards so the bundled skill and server load. Check with
`codex mcp list`. `dejared` should be listed as `enabled`.

## Install as an MCP server

For clients without a plugin system. You get the tools, but not the skill that
steers the agent toward them.

Most clients accept this standard config:

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

### From the command line

If your tool has a CLI, one line is enough:

```bash
claude mcp add dejared -- npx -y dejared-mcp     # Claude Code
codex mcp add dejared -- npx -y dejared-mcp      # Codex
amp mcp add dejared -- npx -y dejared-mcp        # Amp
gemini mcp add dejared npx -y dejared-mcp        # Gemini CLI
```

### Where the config file lives

Drop the standard config into the file for your tool:

| Tool | Location |
|------|----------|
| Claude Desktop | `%APPDATA%\Claude\claude_desktop_config.json` (Windows) · `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS) |
| Claude Code | `.mcp.json` in the project root |
| Cursor | `.cursor/mcp.json` (project) · `~/.cursor/mcp.json` (global) |
| Windsurf | MCP servers panel in settings, or `mcpServers` in your settings file |
| Cline | MCP settings file, via the extension's MCP Servers panel |
| Antigravity Editor | `~/.gemini/antigravity/mcp_config.json` |
| Kiro | `.kiro/settings/mcp.json` |
| Copilot CLI | `~/.copilot/mcp-config.json`, or run `/mcp add` |
| Gemini CLI | `~/.gemini/settings.json` (global) · `.gemini/settings.json` (project) |
| Goose | Advanced settings → Extensions → Add custom extension, type `STDIO` |
| JetBrains IDEs | Settings → Tools → AI Assistant → Model Context Protocol (MCP) |
| Qodo Gen | Chat panel → Connect more tools → + Add new MCP |

A few setups need more than the standard config. Expand those below.

<details>
<summary><strong>Codex</strong>: TOML instead of JSON</summary>

```bash
codex mcp add dejared -- npx -y dejared-mcp
codex mcp list
```

The `--` matters: everything after it is the server's launch command.

Or edit `~/.codex/config.toml` (global) or `.codex/config.toml` (project):

```toml
[mcp_servers.dejared]
command = "npx"
args = ["-y", "dejared-mcp"]

# Only if Java is not on your PATH
[mcp_servers.dejared.env]
DEJARED_JAVA_PATH = "/path/to/java"
```

</details>

<details>
<summary><strong>VS Code (GitHub Copilot)</strong>: uses <code>servers</code>, not <code>mcpServers</code></summary>

Create or edit `.vscode/mcp.json`:

```json
{
  "servers": {
    "dejared": {
      "command": "npx",
      "args": ["-y", "dejared-mcp"]
    }
  }
}
```

After saving, click **Start** above the server entry, then use Agent mode in Copilot Chat.

</details>

<details>
<summary><strong>opencode</strong>: command is an array</summary>

Edit `~/.config/opencode/opencode.json`:

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "dejared": {
      "type": "local",
      "command": ["npx", "-y", "dejared-mcp"],
      "enabled": true
    }
  }
}
```

</details>

<details>
<summary><strong>Pure Java (no Node.js required)</strong></summary>

If you prefer to run the server JAR directly without Node.js:

1. Download the latest JAR from [GitHub Releases](https://github.com/hqkh4nh/dejared-mcp/releases).

2. Run it:
   ```bash
   java -jar dejared-mcp-<version>.jar
   ```

3. Configure your MCP client to use the JAR directly instead of `npx`:

   ```json
   {
     "mcpServers": {
       "dejared": {
         "command": "java",
         "args": ["-jar", "/path/to/dejared-mcp-<version>.jar"]
       }
     }
   }
   ```

</details>

## Features

### Discovery

Browse and read JAR contents.

| Tool | Description |
|------|-------------|
| `dejared_list_packages` | List all packages with class counts |
| `dejared_list_classes` | List classes in a specific package |
| `dejared_list_resources` | List non-class resource files |
| `dejared_read_resource` | Read text resources (YAML, XML, properties, JSON, and others) |

### Hunting

Search inside JAR files.

| Tool | Description |
|------|-------------|
| `dejared_search_class` | Search classes by name |
| `dejared_search_string` | Search string literals in bytecode (URLs, SQL, error messages) |

### Deep Analysis

Inspect metadata and decompile classes.

| Tool | Description |
|------|-------------|
| `dejared_get_metadata` | Extract class metadata via ASM (fast, no decompilation) |
| `dejared_dump_package_metadata` | Batch metadata extraction for entire packages |
| `dejared_decompile_class` | Decompile `.class` files to Java source code |

## Decompiler Engines

dejared-mcp supports three decompiler engines. The engine can be
specified per request via the `dejared_decompile_class` tool.

| Engine | Description |
|--------|-------------|
| **CFR** (default) | Reliable general-purpose decompiler |
| **Vineflower** | Modern fork of FernFlower, handles newer Java features well |
| **Procyon** | Alternative engine, can handle some edge cases better |

## Custom Java Path

If Java is not in your system PATH, set the `DEJARED_JAVA_PATH` environment variable in your MCP config. This applies to all `npx`-based configurations:

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

## Server Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `dejared.cache.max-size` | `500` | Max entries in the decompilation LRU cache |
| `dejared.security.max-resource-size` | `5242880` | Max resource file size (bytes) |
| `dejared.security.decompile-timeout-seconds` | `30` | Timeout per decompilation |

## How It Works

The npm package is a thin Node.js wrapper. On first run it:

1. Checks the platform cache directory for a cached JAR matching the current version.
   - **Linux**: `$XDG_CACHE_HOME/dejared-mcp` (defaults to `~/.cache/dejared-mcp`)
   - **macOS**: `~/Library/Caches/dejared-mcp`
   - **Windows**: `%LOCALAPPDATA%\dejared-mcp`
2. Downloads the JAR from GitHub Releases if not cached.
3. Spawns `java -jar` with stdio inherited for MCP transport.

The server communicates over stdio using the Model Context Protocol.

## FAQ and Troubleshooting

**Q: Java is installed but the server cannot find it.**

Set the `DEJARED_JAVA_PATH` environment variable in your MCP
configuration. See [Custom Java Path](#custom-java-path).

**Q: The server fails to start with a permission error.**

Ensure that the cached JAR file is readable. The cache location
depends on your platform. See [How It Works](#how-it-works) for
the cache directory paths.

**Q: Decompilation times out or returns an error.**

Some classes are difficult to decompile. Try a different engine
by specifying `vineflower` or `procyon` in the `dejared_decompile_class`
tool. The default timeout is 30 seconds and can be adjusted via
`dejared.security.decompile-timeout-seconds`.

**Q: Which Java version do I need?**

Java 17 or later. A JRE is sufficient; you do not need a full JDK.

**Q: Can I run the server without Node.js?**

Yes. Download the JAR from GitHub Releases and run it directly
with `java -jar`. See the "Pure Java" section under
[Install as an MCP server](#install-as-an-mcp-server).

## Contributing

Contributions are welcome. Please follow these guidelines:

1. Fork the repository and create a feature branch from `master`.
2. Ensure your changes compile and pass existing tests.
3. Write clear commit messages describing the purpose of each change.
4. Open a pull request against `master` with a description of what
   the change does and why.

Every manifest listed under [Project Structure](#project-structure) carries a
`version` field, and CI fails the build if any of them drifts from
`package.json`. Bump them together.

### Project Structure

```
dejared-mcp/
├── bin/                    # Node.js CLI entry point
├── lib/                    # Node.js wrapper (JAR download and process management)
├── java-mcp/               # Java MCP server (Spring Boot, Gradle)
│   └── src/
├── skills/                 # Agent skills, shared by every plugin manifest below
├── .mcp.json               # MCP server definition, shared the same way
├── .claude-plugin/         # Claude Code plugin + marketplace manifests
├── .codex-plugin/          # Codex plugin manifest
├── .agents/plugins/        # Codex marketplace manifest
├── package.json            # npm package manifest
└── server.json             # MCP Registry manifest
```

### Building from Source

```bash
cd java-mcp
./gradlew build
```

### Reporting Issues

Open an issue on [GitHub Issues](https://github.com/hqkh4nh/dejared-mcp/issues)
with steps to reproduce the problem, your Java version, and your
Node.js version.

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

This project is licensed under the [MIT License](LICENSE).
