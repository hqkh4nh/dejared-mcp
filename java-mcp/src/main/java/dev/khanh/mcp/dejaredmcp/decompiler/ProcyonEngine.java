package dev.khanh.mcp.dejaredmcp.decompiler;

import com.strobel.assembler.metadata.ArrayTypeLoader;
import com.strobel.assembler.metadata.ClasspathTypeLoader;
import com.strobel.assembler.metadata.CompositeTypeLoader;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import dev.khanh.mcp.dejaredmcp.model.DecompileResult;
import org.springframework.stereotype.Component;

import java.io.StringWriter;

/**
 * Decompiler engine backed by Procyon.
 *
 * <p>Procyon handles some edge cases better than CFR/Vineflower (e.g. certain obfuscated
 * bytecode patterns). Uses a {@link CompositeTypeLoader} that combines an
 * {@link ArrayTypeLoader} (for the target class bytes) with a {@link ClasspathTypeLoader}
 * (for resolving referenced JDK types).
 *
 * @see <a href="https://github.com/mstrobel/procyon">Procyon on GitHub</a>
 */
@Component
public class ProcyonEngine implements DecompilerEngine {

    @Override
    public String name() {
        return "procyon";
    }

    @Override
    public DecompileResult decompile(byte[] classBytes, String className) {
        String internalName = className.replace('.', '/');

        try {
            ITypeLoader loader = new CompositeTypeLoader(
                    new ArrayTypeLoader(classBytes),
                    new ClasspathTypeLoader()
            );

            DecompilerSettings settings = DecompilerSettings.javaDefaults();
            settings.setTypeLoader(loader);
            settings.setForceExplicitImports(true);

            StringWriter writer = new StringWriter();
            PlainTextOutput output = new PlainTextOutput(writer);

            Decompiler.decompile(internalName, output, settings);

            String source = writer.toString();
            if (source.isBlank()) {
                return DecompileResult.fail("procyon", "Procyon produced empty output for " + className);
            }
            return DecompileResult.ok("procyon", source);
        } catch (Exception e) {
            return DecompileResult.fail("procyon", "Procyon failed to decompile " + className + ": " + e.getMessage());
        }
    }
}
