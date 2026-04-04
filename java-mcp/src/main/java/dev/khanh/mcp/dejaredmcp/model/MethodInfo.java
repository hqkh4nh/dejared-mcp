package dev.khanh.mcp.dejaredmcp.model;

import java.util.List;

/**
 * A method declared in a Java class, extracted from bytecode metadata.
 *
 * @param name           the method name
 * @param returnType     the return type as a Java type name
 * @param parameterTypes list of parameter types as Java type names
 * @param modifiers      access modifiers (e.g. {@code "public static"})
 */
public record MethodInfo(String name, String returnType, List<String> parameterTypes, String modifiers) {

    @Override
    public String toString() {
        return modifiers + " " + returnType + " " + name + "(" + String.join(", ", parameterTypes) + ")";
    }
}
