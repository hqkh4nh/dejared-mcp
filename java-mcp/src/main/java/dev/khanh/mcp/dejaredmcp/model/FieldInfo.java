package dev.khanh.mcp.dejaredmcp.model;

/**
 * A field declared in a Java class, extracted from bytecode metadata.
 *
 * @param name      the field name
 * @param type      the field type as a Java type name (e.g. {@code "String"}, {@code "int"})
 * @param modifiers access modifiers (e.g. {@code "private final"})
 */
public record FieldInfo(String name, String type, String modifiers) {

    @Override
    public String toString() {
        return modifiers + " " + type + " " + name;
    }
}
