package dev.khanh.mcp.dejaredmcp.testutil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Builds test JAR files containing pre-compiled .class bytecode and resource files.
 * Uses raw bytecode generation (ASM) to avoid needing javac at test time.
 */
public final class TestJarBuilder {

    private TestJarBuilder() {}

    /**
     * Creates a test JAR at the given path with:
     * - com/example/hello/HelloWorld.class (public class with main method, field, annotation)
     * - com/example/hello/Greeter.class (interface with greet method)
     * - com/example/util/StringHelper.class (utility class with string constant)
     * - application.properties (text resource)
     * - data.bin (binary file)
     */
    public static Path createTestJar(Path directory) throws IOException {
        Path jarPath = directory.resolve("test.jar");
        try (var jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {

            // -- com/example/hello/HelloWorld.class --
            addEntry(jos, "com/example/hello/HelloWorld.class", buildHelloWorldClass());

            // -- com/example/hello/Greeter.class --
            addEntry(jos, "com/example/hello/Greeter.class", buildGreeterInterface());

            // -- com/example/util/StringHelper.class --
            addEntry(jos, "com/example/util/StringHelper.class", buildStringHelperClass());

            // -- application.properties --
            addEntry(jos, "application.properties", "app.name=TestApp\napp.version=1.0\n".getBytes());

            // -- data.bin (binary) --
            addEntry(jos, "data.bin", new byte[]{0x00, 0x01, 0x02, 0x03});
        }
        return jarPath;
    }

    private static void addEntry(JarOutputStream jos, String name, byte[] content) throws IOException {
        jos.putNextEntry(new JarEntry(name));
        jos.write(content);
        jos.closeEntry();
    }

    /**
     * Generates bytecode for:
     * @Deprecated
     * public class HelloWorld implements Greeter {
     *     private String message = "Hello, World!";
     *     public static void main(String[] args) { System.out.println("Hello from DeJared!"); }
     *     public String greet(String name) { return "Hello, " + name; }
     * }
     */
    private static byte[] buildHelloWorldClass() {
        var cw = new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_FRAMES);
        cw.visit(
                org.objectweb.asm.Opcodes.V21,
                org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_SUPER,
                "com/example/hello/HelloWorld",
                null,
                "java/lang/Object",
                new String[]{"com/example/hello/Greeter"}
        );

        // @Deprecated annotation
        cw.visitAnnotation("Ljava/lang/Deprecated;", true).visitEnd();

        // private String message = "Hello, World!";
        cw.visitField(
                org.objectweb.asm.Opcodes.ACC_PRIVATE,
                "message",
                "Ljava/lang/String;",
                null,
                "Hello, World!"
        ).visitEnd();

        // Constructor
        var init = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        init.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        init.visitLdcInsn("Hello, World!");
        init.visitFieldInsn(org.objectweb.asm.Opcodes.PUTFIELD, "com/example/hello/HelloWorld", "message", "Ljava/lang/String;");
        init.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        init.visitMaxs(2, 1);
        init.visitEnd();

        // public static void main(String[] args)
        var main = cw.visitMethod(
                org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_STATIC,
                "main",
                "([Ljava/lang/String;)V",
                null, null
        );
        main.visitCode();
        main.visitFieldInsn(org.objectweb.asm.Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        main.visitLdcInsn("Hello from DeJared!");
        main.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        main.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        main.visitMaxs(2, 1);
        main.visitEnd();

        // public String greet(String name)
        var greet = cw.visitMethod(
                org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "greet",
                "(Ljava/lang/String;)Ljava/lang/String;",
                null, null
        );
        greet.visitCode();
        greet.visitTypeInsn(org.objectweb.asm.Opcodes.NEW, "java/lang/StringBuilder");
        greet.visitInsn(org.objectweb.asm.Opcodes.DUP);
        greet.visitLdcInsn("Hello, ");
        greet.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
        greet.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 1);
        greet.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        greet.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        greet.visitInsn(org.objectweb.asm.Opcodes.ARETURN);
        greet.visitMaxs(3, 2);
        greet.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * Generates bytecode for:
     * public interface Greeter {
     *     String greet(String name);
     * }
     */
    private static byte[] buildGreeterInterface() {
        var cw = new org.objectweb.asm.ClassWriter(0);
        cw.visit(
                org.objectweb.asm.Opcodes.V21,
                org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_INTERFACE | org.objectweb.asm.Opcodes.ACC_ABSTRACT,
                "com/example/hello/Greeter",
                null,
                "java/lang/Object",
                null
        );
        cw.visitMethod(
                org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_ABSTRACT,
                "greet",
                "(Ljava/lang/String;)Ljava/lang/String;",
                null, null
        ).visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * Generates bytecode for:
     * public class StringHelper {
     *     public static final String DB_URL = "jdbc:mysql://localhost:3306/mydb";
     *     public static final String ERROR_MSG = "Connection failed: timeout";
     *     public static String toUpper(String input) { return input.toUpperCase(); }
     * }
     */
    private static byte[] buildStringHelperClass() {
        var cw = new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_FRAMES);
        cw.visit(
                org.objectweb.asm.Opcodes.V21,
                org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_SUPER,
                "com/example/util/StringHelper",
                null,
                "java/lang/Object",
                null
        );

        // public static final String DB_URL
        cw.visitField(
                org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_STATIC | org.objectweb.asm.Opcodes.ACC_FINAL,
                "DB_URL", "Ljava/lang/String;", null,
                "jdbc:mysql://localhost:3306/mydb"
        ).visitEnd();

        // public static final String ERROR_MSG
        cw.visitField(
                org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_STATIC | org.objectweb.asm.Opcodes.ACC_FINAL,
                "ERROR_MSG", "Ljava/lang/String;", null,
                "Connection failed: timeout"
        ).visitEnd();

        // Constructor
        var init = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        init.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();

        // public static String toUpper(String input)
        var method = cw.visitMethod(
                org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_STATIC,
                "toUpper",
                "(Ljava/lang/String;)Ljava/lang/String;",
                null, null
        );
        method.visitCode();
        method.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        method.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL, "java/lang/String", "toUpperCase", "()Ljava/lang/String;", false);
        method.visitInsn(org.objectweb.asm.Opcodes.ARETURN);
        method.visitMaxs(1, 1);
        method.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
