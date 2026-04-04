---
name: jar-analysis
description: >
  Explore, search, and decompile Java JAR files. Three workflows: Explore
  (top-down structure mapping), Hunt (search for specific code/strings),
  and Deep Analysis (single-class investigation). Use the workflow that
  matches the task.
---

# JAR Analysis

Three workflows for working with Java JAR files. Pick the one that fits your goal.

## Available Tools

| Tool | Cost | Purpose |
|------|------|---------|
| `dejared_list_packages` | Cheap | List all packages with class counts |
| `dejared_list_classes` | Cheap | List classes in a package (`recursive=true` for sub-packages) |
| `dejared_list_resources` | Cheap | List all non-class resource files with sizes |
| `dejared_read_resource` | Cheap | Read a resource file (yml, properties, xml, json, txt, sql, conf) |
| `dejared_dump_package_metadata` | Cheap | Batch metadata for multiple packages at once (annotations, fields, methods via ASM) |
| `dejared_get_metadata` | Cheap | Single class metadata (ASM-based, no decompilation) |
| `dejared_search_class` | Cheap | Find classes by name keyword (case-insensitive) |
| `dejared_search_string` | Cheap | Find string literals in bytecode constant pools (case-insensitive) |
| `dejared_decompile_class` | **Expensive** | Full source code decompilation (CFR/vineflower/procyon) |

## Shared Rules

- All tool calls require the **absolute path** to the JAR file.
- NEVER decompile a class without checking its metadata first.
- Default decompiler: CFR. If output is broken, try `vineflower` or `procyon`.
- Report results in structured format.

---

## Workflow 1: Explore

Use when exploring unknown JARs or understanding JAR architecture. Top-down approach: cheap tools first, decompilation last.

### Step 1: Map Package Structure
Call `dejared_list_packages` to get the full layout.
- Identify the root application package (highest class count, deepest nesting).
- Distinguish app code from shaded/third-party dependencies.

### Step 2: List Classes
Call `dejared_list_classes` with `recursive=true` on the root application package to get all classes in one call.
- For targeted exploration of specific packages, use `recursive=false`.

### Step 3: Discover Resources
Call `dejared_list_resources` to see what non-class files exist in the JAR.

### Step 4: Read Configuration
Call `dejared_read_resource` for files discovered in Step 3:
- `application.yml` / `application.yaml` / `application.properties`
- `META-INF/spring.factories` or `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- `logback.xml` / `log4j2.xml`

### Step 5: Batch Metadata
Call `dejared_dump_package_metadata` with **all important packages at once** (it accepts a list).
Example: `packageNames: ["com.example.service", "com.example.config", "com.example.controller"]`
- Returns annotations, fields, and method signatures for every class.
- One call replaces dozens of individual `dejared_get_metadata` calls.

### Step 6: Selective Decompilation
Call `dejared_decompile_class` ONLY for classes where you need method body logic.
- You MUST have reviewed metadata from Step 5 first.
- Decompile one class at a time; summarize before continuing.
- For JARs with >20 packages, group by prefix and summarize before diving in.

---

## Workflow 2: Hunt

Use when looking for specific functionality, URLs, SQL, error messages, or credentials. Search-driven approach.

### Search Tools

**`dejared_search_class`** -- Find by class name.
Case-insensitive keyword match against simple class names.
- Find specific classes: "UserService", "DatabaseConfig"
- Find patterns: "Controller", "Repository", "Factory", "Handler"

**`dejared_search_string`** -- Find by string literal.
Scans bytecode constant pools across all classes (case-insensitive).
- URLs, endpoints, API paths
- SQL queries, table names
- Error messages, log messages
- Config keys, credentials, secrets
- Useful keywords for security audits: "password", "secret", "token", "key", "jdbc", "http://"

### Steps

1. **Search** -- Pick the right search tool based on the goal.
2. **Triage** -- Review results, identify most relevant matches.
3. **Inspect** -- Call `dejared_get_metadata` on promising classes to understand their role (annotations, fields, methods).
4. **Decompile** -- Call `dejared_decompile_class` ONLY on classes where you need to see method body logic. Never skip step 3.
5. **Report** -- Summarize findings with context and implications.

Note: String search finds compiled constants only, not runtime-generated strings.

---

## Workflow 3: Deep Analysis

Use when investigating a single class in detail. Combines metadata extraction with multi-engine decompilation.

### Step 1: Metadata First
Call `dejared_get_metadata` to extract:
- Class hierarchy (superclass, interfaces)
- Annotations (`@Service`, `@Entity`, `@Controller`, etc.)
- Fields (state the class holds)
- Methods (behavior -- names, return types, parameters)

This tells you the class's role and size before decompiling.

### Step 2: Decompile
Call `dejared_decompile_class` (default engine: CFR). Analyze:
- Constructor logic and dependency injection
- Method implementations and business logic
- Error handling patterns
- Interactions with other classes

### Step 3: Cross-Reference (if needed)
If decompiled code references important classes:
- `dejared_search_class` to find them by name.
- `dejared_dump_package_metadata` on the same package to see sibling classes (accepts a list of packages).

### Step 4: Try Alternative Engine (if needed)
If CFR output has problems (broken lambdas, goto statements, placeholder bodies):
- `vineflower` -- best for modern Java features.
- `procyon` -- good for edge cases.

### Report Structure
1. **Role** -- What this class does (one sentence).
2. **Framework Integration** -- Annotations, Spring beans, injection points.
3. **Key Methods** -- Important methods and what they do.
4. **Dependencies** -- Other classes/services it uses.
5. **Notable Patterns** -- Design patterns, security concerns, performance notes.
