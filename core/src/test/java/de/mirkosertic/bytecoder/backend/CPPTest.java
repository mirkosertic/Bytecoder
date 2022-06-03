package de.mirkosertic.bytecoder.backend;

import de.mirkosertic.bytecoder.allocator.Allocator;
import de.mirkosertic.bytecoder.backend.cpp.CPPCompileResult;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.optimizer.KnownOptimizer;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;

public class CPPTest {

    private static final Slf4JLogger LOGGER = new Slf4JLogger();

    public static void main(final String[] args) throws NoSuchMethodException, IOException {
        final Class theClassToTest = Testclass.class;
        final Method theMethodToTest = theClassToTest.getMethod("compute", int.class, int.class);
        final CompileTarget theCompileTarget = new CompileTarget(theClassToTest.getClassLoader(),
                CompileTarget.BackendType.cpp);

        final BytecodeMethodSignature theSignature = theCompileTarget.toMethodSignature(theMethodToTest);

        final CompileOptions theOptions = new CompileOptions(LOGGER, true, KnownOptimizer.NONE, true, "bytecoder", 512, 512, false, false, Allocator.passthru, new String[0], new String[0], null, false);
        final CPPCompileResult result = (CPPCompileResult) theCompileTarget
                .compile(theOptions, theClassToTest, theMethodToTest.getName(), theSignature);

        final File f = new File("/home/mirkosertic/Development/Projects/Bytecoder/core/src/test/gen");
        for (final CompileResult.Content content : result.getContent()) {
            if (content instanceof CompileResult.StringContent) {
                final File target = new File(f, content.getFileName());
                try (final FileWriter fw = new FileWriter(target)) {
                    fw.append(content.asString());
                }
            }
        }

    }
}
