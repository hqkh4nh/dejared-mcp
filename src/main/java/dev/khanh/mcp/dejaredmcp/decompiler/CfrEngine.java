package dev.khanh.mcp.dejaredmcp.decompiler;

import dev.khanh.mcp.dejaredmcp.model.DecompileResult;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * Decompiler engine backed by CFR (Class File Reader).
 *
 * <p>CFR is the default decompiler engine — fast and handles most Java bytecode well.
 * Uses an in-memory {@link ClassFileSource} to feed class bytes directly to CFR's
 * driver API, and captures output via a custom {@link OutputSinkFactory}.
 *
 * @see <a href="https://github.com/leibnitz27/cfr">CFR on GitHub</a>
 */
@Component
public class CfrEngine implements DecompilerEngine {

    @Override
    public String name() {
        return "cfr";
    }

    @Override
    public DecompileResult decompile(byte[] classBytes, String className) {
        String classFilePath = className.replace('.', '/') + ".class";

        try {
            ClassFileSource source = new ClassFileSource() {
                @Override
                public void informAnalysisRelativePathDetail(String usePath, String specFilePath) {}

                @Override
                public Collection<String> addJar(String jarPath) {
                    return Collections.emptyList();
                }

                @Override
                public String getPossiblyRenamedPath(String path) {
                    return path;
                }

                @Override
                public Pair<byte[], String> getClassFileContent(String path) throws IOException {
                    String normalized = path.startsWith("/") ? path.substring(1) : path;
                    if (normalized.equals(classFilePath)) {
                        return Pair.make(classBytes, classFilePath);
                    }
                    return null;
                }
            };

            StringBuilder result = new StringBuilder();

            OutputSinkFactory sinkFactory = new OutputSinkFactory() {
                @Override
                public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> available) {
                    return Collections.singletonList(SinkClass.STRING);
                }

                @Override
                public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                    if (sinkType == SinkType.JAVA) {
                        return x -> result.append(x);
                    }
                    return x -> {};
                }
            };

            Map<String, String> options = new HashMap<>();
            options.put("showversion", "false");

            CfrDriver driver = new CfrDriver.Builder()
                    .withClassFileSource(source)
                    .withOutputSink(sinkFactory)
                    .withOptions(options)
                    .build();

            driver.analyse(Collections.singletonList(classFilePath));

            String source_ = result.toString();
            if (source_.isBlank()) {
                return DecompileResult.fail("cfr", "CFR produced empty output for " + className);
            }
            return DecompileResult.ok("cfr", source_);
        } catch (Exception e) {
            return DecompileResult.fail("cfr", "CFR failed to decompile " + className + ": " + e.getMessage());
        }
    }
}
