package dev.khanh.mcp.dejaredmcp.service;

import dev.khanh.mcp.dejaredmcp.model.*;
import dev.khanh.mcp.dejaredmcp.validation.JarPathValidator;
import org.objectweb.asm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Analyzes Java bytecode using ASM to extract class metadata and search string constants.
 *
 * <p>Unlike decompilation, bytecode analysis is fast and does not require a decompiler engine.
 * It operates directly on {@code .class} entries inside JAR files.
 */
@Service
public class BytecodeAnalyzerService {

    private static final Logger log = LoggerFactory.getLogger(BytecodeAnalyzerService.class);

    /**
     * Extracts structural metadata (hierarchy, annotations, fields, methods) from a class
     * without decompiling it.
     *
     * @param jarFilePath absolute path to the JAR file
     * @param className   fully-qualified class name (e.g. {@code "com.example.Foo"})
     * @return formatted metadata string, or an error message prefixed with {@code "Error:"}
     */
    public String getMetadata(String jarFilePath, String className) {
        String classError = JarPathValidator.validateClassName(className);
        if (classError != null) {
            return classError;
        }

        String classPath = className.replace('.', '/') + ".class";

        try (var jarFile = new JarFile(jarFilePath)) {
            JarEntry entry = jarFile.getJarEntry(classPath);
            if (entry == null) {
                return "Error: Class not found.";
            }

            // Zip bomb check
            if (JarExplorerService.isSuspiciousEntry(entry)) {
                return "Error: Suspicious compression ratio detected.";
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
            log.warn("Failed to read JAR file", e);
            return "Error: Failed to read JAR file.";
        }
    }

    /**
     * Dumps structural metadata for all direct classes in one or more packages using ASM.
     *
     * <p>Accepts multiple package names to batch metadata extraction in a single call,
     * avoiding N+1 round-trips. Uses {@code ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG}
     * for fast analysis without decompilation.
     *
     * @param jarFilePath  absolute path to the JAR file
     * @param packageNames list of dot-separated package names
     * @return compact metadata for all classes in the requested packages
     */
    public String dumpPackageMetadata(String jarFilePath, List<String> packageNames) {
        for (String pkg : packageNames) {
            String pkgError = JarPathValidator.validatePackageName(pkg);
            if (pkgError != null) {
                return pkgError;
            }
        }

        try (var jarFile = new JarFile(jarFilePath)) {
            Set<String> packagePaths = packageNames.stream()
                    .map(pkg -> pkg.replace('.', '/') + "/")
                    .collect(Collectors.toSet());

            List<JarEntry> classEntries = jarFile.stream()
                    .filter(entry -> entry.getName().endsWith(".class"))
                    .filter(entry -> packagePaths.stream().anyMatch(pp ->
                            entry.getName().startsWith(pp)
                                    && !entry.getName().substring(pp.length()).contains("/")))
                    .sorted(Comparator.comparing(JarEntry::getName))
                    .toList();

            if (classEntries.isEmpty()) {
                return "Error: No classes found in the specified packages.";
            }

            var sb = new StringBuilder();
            for (JarEntry entry : classEntries) {
                try (var is = jarFile.getInputStream(entry)) {
                    byte[] classBytes = is.readAllBytes();
                    ClassReader reader = new ClassReader(classBytes);
                    MetadataCollector collector = new MetadataCollector();
                    reader.accept(collector, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);

                    String className = entry.getName()
                            .substring(0, entry.getName().length() - 6)
                            .replace('/', '.');

                    sb.append("--- ").append(className).append(" ---\n");

                    if (!collector.annotations.isEmpty()) {
                        sb.append("  Annotations: ").append(String.join(", ", collector.annotations)).append("\n");
                    }
                    sb.append("  Extends: ").append(toJavaName(collector.superName)).append("\n");
                    if (!collector.interfaces.isEmpty()) {
                        sb.append("  Implements: ")
                                .append(collector.interfaces.stream().map(BytecodeAnalyzerService::toJavaName).collect(Collectors.joining(", ")))
                                .append("\n");
                    }
                    if (!collector.fields.isEmpty()) {
                        sb.append("  Fields:\n");
                        collector.fields.forEach(f -> sb.append("    ").append(f).append("\n"));
                    }
                    if (!collector.methods.isEmpty()) {
                        sb.append("  Methods:\n");
                        collector.methods.forEach(m -> sb.append("    ").append(m).append("\n"));
                    }
                    sb.append("\n");
                } catch (IOException ignored) {
                }
            }

            return sb.toString().stripTrailing();
        } catch (IOException e) {
            log.warn("Failed to read JAR file", e);
            return "Error: Failed to read JAR file.";
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
                return "No strings found matching '" + query + "'.";
            }

            var sb = new StringBuilder();
            results.forEach((cls, strings) -> {
                sb.append(cls).append(":\n");
                strings.forEach(s -> sb.append("  \"").append(s).append("\"\n"));
            });
            return sb.toString();
        } catch (IOException e) {
            log.warn("Failed to read JAR file", e);
            return "Error: Failed to read JAR file.";
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

    /** ASM visitor that collects class structural metadata without reading method bodies. */
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

    /** ASM visitor that searches for string literals in constant pools and LDC instructions. */
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
