package dev.khanh.mcp.dejaredmcp.service;

import dev.khanh.mcp.dejaredmcp.model.*;
import org.objectweb.asm.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Analyzes Java bytecode using ASM to extract class metadata and search string constants.
 *
 * <p>Unlike decompilation, bytecode analysis is fast and does not require a decompiler engine.
 * It operates directly on {@code .class} entries inside JAR files.
 */
@Service
public class BytecodeAnalyzerService {

    /**
     * Extracts structural metadata (hierarchy, annotations, fields, methods) from a class
     * without decompiling it.
     *
     * @param jarFilePath absolute path to the JAR file
     * @param className   fully-qualified class name (e.g. {@code "com.example.Foo"})
     * @return formatted metadata string, or an error message prefixed with {@code "Error:"}
     */
    public String getMetadata(String jarFilePath, String className) {
        String classPath = className.replace('.', '/') + ".class";

        try (var jarFile = new JarFile(jarFilePath)) {
            JarEntry entry = jarFile.getJarEntry(classPath);
            if (entry == null) {
                return "Error: Class '" + className + "' not found in " + jarFilePath;
            }

            byte[] classBytes;
            try (var is = jarFile.getInputStream(entry)) {
                classBytes = is.readAllBytes();
            }

            ClassReader reader = new ClassReader(classBytes);
            MetadataCollector collector = new MetadataCollector();
            reader.accept(collector, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);

            ClassMetadata metadata = new ClassMetadata(
                    className,
                    toJavaName(collector.superName),
                    collector.interfaces.stream().map(BytecodeAnalyzerService::toJavaName).toList(),
                    collector.annotations,
                    collector.fields,
                    collector.methods
            );

            return metadata.toString();
        } catch (IOException e) {
            return "Error: Failed to read JAR file: " + e.getMessage();
        }
    }

    /**
     * Searches for string literals across all classes in a JAR (case-insensitive).
     *
     * <p>Scans both constant pool values (static fields) and {@code LDC} instructions
     * (strings loaded at runtime). Useful for finding hardcoded URLs, SQL, error messages, etc.
     *
     * @param jarFilePath absolute path to the JAR file
     * @param query       text to search for (case-insensitive)
     * @return matching strings grouped by class name, or an error message
     */
    public String searchString(String jarFilePath, String query) {
        String lowerQuery = query.toLowerCase();

        try (var jarFile = new JarFile(jarFilePath)) {
            Map<String, List<String>> results = new LinkedHashMap<>();

            jarFile.stream()
                    .filter(entry -> entry.getName().endsWith(".class"))
                    .forEach(entry -> {
                        try (var is = jarFile.getInputStream(entry)) {
                            byte[] classBytes = is.readAllBytes();
                            ClassReader reader = new ClassReader(classBytes);
                            StringCollector collector = new StringCollector(lowerQuery);
                            reader.accept(collector, 0);

                            if (!collector.matches.isEmpty()) {
                                String clsName = entry.getName()
                                        .substring(0, entry.getName().length() - 6)
                                        .replace('/', '.');
                                results.put(clsName, collector.matches);
                            }
                        } catch (IOException ignored) {
                        }
                    });

            if (results.isEmpty()) {
                return "No strings found matching '" + query + "' in " + jarFilePath;
            }

            var sb = new StringBuilder();
            results.forEach((cls, strings) -> {
                sb.append(cls).append(":\n");
                strings.forEach(s -> sb.append("  \"").append(s).append("\"\n"));
            });
            return sb.toString();
        } catch (IOException e) {
            return "Error: Failed to read JAR file: " + e.getMessage();
        }
    }

    private static String toJavaName(String internalName) {
        if (internalName == null) return "null";
        return internalName.replace('/', '.');
    }

    private static String descriptorToJavaType(String descriptor) {
        return switch (descriptor) {
            case "Z" -> "boolean";
            case "B" -> "byte";
            case "C" -> "char";
            case "S" -> "short";
            case "I" -> "int";
            case "J" -> "long";
            case "F" -> "float";
            case "D" -> "double";
            case "V" -> "void";
            default -> {
                if (descriptor.startsWith("[")) {
                    yield descriptorToJavaType(descriptor.substring(1)) + "[]";
                }
                if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
                    String clsName = descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
                    int lastDot = clsName.lastIndexOf('.');
                    yield lastDot >= 0 ? clsName.substring(lastDot + 1) : clsName;
                }
                yield descriptor;
            }
        };
    }

    private static String accessToModifiers(int access) {
        var mods = new ArrayList<String>();
        if ((access & Opcodes.ACC_PUBLIC) != 0) mods.add("public");
        if ((access & Opcodes.ACC_PRIVATE) != 0) mods.add("private");
        if ((access & Opcodes.ACC_PROTECTED) != 0) mods.add("protected");
        if ((access & Opcodes.ACC_STATIC) != 0) mods.add("static");
        if ((access & Opcodes.ACC_FINAL) != 0) mods.add("final");
        if ((access & Opcodes.ACC_ABSTRACT) != 0) mods.add("abstract");
        return String.join(" ", mods);
    }

    private static class MetadataCollector extends ClassVisitor {
        String superName;
        List<String> interfaces = new ArrayList<>();
        List<String> annotations = new ArrayList<>();
        List<FieldInfo> fields = new ArrayList<>();
        List<MethodInfo> methods = new ArrayList<>();

        MetadataCollector() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            this.superName = superName;
            if (interfaces != null) {
                this.interfaces.addAll(Arrays.asList(interfaces));
            }
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
                String name = descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
                int lastDot = name.lastIndexOf('.');
                annotations.add("@" + (lastDot >= 0 ? name.substring(lastDot + 1) : name));
            }
            return null;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor,
                                       String signature, Object value) {
            fields.add(new FieldInfo(
                    name,
                    descriptorToJavaType(descriptor),
                    accessToModifiers(access)
            ));
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            Type methodType = Type.getMethodType(descriptor);
            List<String> params = Arrays.stream(methodType.getArgumentTypes())
                    .map(t -> descriptorToJavaType(t.getDescriptor()))
                    .toList();
            methods.add(new MethodInfo(
                    name,
                    descriptorToJavaType(methodType.getReturnType().getDescriptor()),
                    params,
                    accessToModifiers(access)
            ));
            return null;
        }
    }

    private static class StringCollector extends ClassVisitor {
        final String lowerQuery;
        final List<String> matches = new ArrayList<>();

        StringCollector(String lowerQuery) {
            super(Opcodes.ASM9);
            this.lowerQuery = lowerQuery;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor,
                                       String signature, Object value) {
            if (value instanceof String s && s.toLowerCase().contains(lowerQuery)) {
                matches.add(s);
            }
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public void visitLdcInsn(Object value) {
                    if (value instanceof String s && s.toLowerCase().contains(lowerQuery)) {
                        if (!matches.contains(s)) {
                            matches.add(s);
                        }
                    }
                }
            };
        }
    }
}
