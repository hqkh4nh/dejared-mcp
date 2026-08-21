---
name: jar-analysis
description: >
  Explore, search, read, and decompile Java JAR, WAR, EAR, AAR, and
  Spring Boot fat-jar files via the dejared MCP server. Use this skill
  whenever the user references a `.jar` file path, asks what a
  third-party Java library does, wants to read a config embedded in a
  JAR (application.yml, spring.factories, MANIFEST.MF), needs class
  metadata (superclass, annotations, method signatures), is chasing a
  stack trace into a dependency without source, or is reverse-engineering
  a plugin/mod/obfuscated artifact. Applies to artifacts in
  `~/.m2/repository`, `~/.gradle/caches`, `build/libs/`, `target/`,
  `lib/`, and `libs/`. Prefer the dejared MCP tools over `jar tf`,
  `jar xf`, `unzip`, `javap -p`, `javap -c`, `javap -v`, or running a
  decompiler manually, even if the user doesn't explicitly ask for
  "the dejared tools" or "decompilation".
---

# JAR Analysis

## When This Skill Applies

Reach for these tools, never the shell, whenever the task touches the **inside** of a Java artifact:

| You would reach for...                       | Use this instead                                       |
|----------------------------------------------|--------------------------------------------------------|
| `jar tf some.jar`                            | `dejared_list_packages` / `dejared_list_classes`       |
| `jar tf some.jar \| grep -v .class`          | `dejared_list_resources`                               |
| `unzip -p some.jar application.yml`          | `dejared_read_resource`                                |
| `jar tf some.jar \| grep -i FooBar`          | `dejared_search_class`                                 |
| `strings some.jar \| grep http`              | `dejared_search_string`                                |
| `javap -p com.foo.Bar`                       | `dejared_get_metadata`                                 |
| `javap -p` across a package                  | `dejared_dump_package_metadata`                        |
| Running CFR / Vineflower / Procyon manually  | `dejared_decompile_class`                              |

Applies to `.jar`, `.war`, `.ear`, `.aar` and any artifact under `~/.m2/repository`, `~/.gradle/caches`, `build/libs/`, `target/`, `lib/`, or `libs/`. All calls need an **absolute path** to the archive.

## Available Tools

| Tool | Cost | Purpose |
|------|------|---------|
| `dejared_list_packages` | Cheap | List all packages with class counts |
| `dejared_list_classes` | Cheap | List classes in a package (`recursive=true` for sub-packages) |
| `dejared_list_resources` | Cheap | List all non-class resource files with sizes |
| `dejared_read_resource` | Cheap | Read a text resource file from inside a JAR (content-based detection, no extension restrictions) |
| `dejared_dump_package_metadata` | Cheap | Batch metadata for multiple packages at once (annotations, fields, methods via ASM) |
| `dejared_get_metadata` | Cheap | Single class metadata (ASM-based, no decompilation) |
| `dejared_search_class` | Cheap | Find classes by name keyword (case-insensitive) |
| `dejared_search_string` | Cheap | Find string literals in bytecode constant pools (case-insensitive) |
| `dejared_decompile_class` | **Expensive** | Full source code decompilation (CFR/vineflower/procyon) |

## Shared Rules

- All tool calls require the **absolute path** to the JAR file.
- Prefer these MCP tools over shelling out to jar/javap/unzip/strings or asking the user to decompile manually. Structured output is faster and safer for the agent to consume.
- Check metadata before decompiling: it often provides enough information without the cost of full decompilation.
- Default decompiler: CFR. If output is broken, try `vineflower` or `procyon`.
- Report results in structured format.

---

## Quick Use Cases

**Quick Lookup**: find a class by name (`dejared_search_class`) or find a string in bytecode (`dejared_search_string`). One call, done.

**Read Config**: `dejared_list_resources` to see what's in the JAR, then `dejared_read_resource` to read it (application.yml, pom.xml, logback.xml, etc.).

**Get Signatures**: `dejared_get_metadata` for a single class, or `dejared_dump_package_metadata` for an entire package (accepts a list). Returns class hierarchy, annotations, fields, and method signatures without decompiling.

---

## Workflows

**Explore**: top-down JAR mapping:
`dejared_list_packages` → `dejared_list_classes` (use `recursive=true`) → `dejared_list_resources` → `dejared_read_resource` for configs → `dejared_dump_package_metadata` (batch all important packages in one call) → selective `dejared_decompile_class` only where method body logic is needed. If CFR output has issues, retry with `vineflower` or `procyon`.

**Hunt**: search-driven investigation:
Pick search tool (`dejared_search_class` or `dejared_search_string`) → triage results → `dejared_get_metadata` on promising matches → `dejared_decompile_class` only if needed.

**Deep Analysis**: single class investigation:
`dejared_get_metadata` first → `dejared_decompile_class` (CFR default) → cross-reference with `dejared_search_class` or `dejared_dump_package_metadata` if needed. If CFR output has issues (broken lambdas, `goto` statements, missing method bodies), retry with `vineflower` (best for modern Java features like records, sealed classes, pattern matching) or `procyon` (handles some obfuscated/edge-case bytecode better).
