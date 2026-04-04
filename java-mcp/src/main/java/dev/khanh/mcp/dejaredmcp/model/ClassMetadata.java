package dev.khanh.mcp.dejaredmcp.model;

import java.util.List;

/**
 * Structural metadata of a Java class extracted from bytecode via ASM.
 *
 * <p>Contains the class hierarchy, annotations, fields, and methods without
 * performing a full decompilation — significantly faster for inspection.
 *
 * @param className   fully-qualified class name
 * @param superClass  fully-qualified superclass name
 * @param interfaces  implemented interfaces
 * @param annotations class-level annotations (simple names prefixed with {@code @})
 * @param fields      declared fields with type and access modifiers
 * @param methods     declared methods with signature and access modifiers
 */
public record ClassMetadata(
        String className,
        String superClass,
        List<String> interfaces,
        List<String> annotations,
        List<FieldInfo> fields,
        List<MethodInfo> methods
) {

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("Class: ").append(className).append("\n");
        sb.append("Extends: ").append(superClass).append("\n");
        if (!interfaces.isEmpty()) {
            sb.append("Implements: ").append(String.join(", ", interfaces)).append("\n");
        }
        if (!annotations.isEmpty()) {
            sb.append("Annotations: ").append(String.join(", ", annotations)).append("\n");
        }
        sb.append("\nFields:\n");
        fields.forEach(f -> sb.append("  ").append(f).append("\n"));
        sb.append("\nMethods:\n");
        methods.forEach(m -> sb.append("  ").append(m).append("\n"));
        return sb.toString();
    }
}
