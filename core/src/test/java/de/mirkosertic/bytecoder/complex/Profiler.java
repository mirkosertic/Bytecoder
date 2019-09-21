package de.mirkosertic.bytecoder.complex;

import de.mirkosertic.bytecoder.allocator.Allocator;
import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.CompileTarget;
import de.mirkosertic.bytecoder.backend.js.JSCompileResult;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.optimizer.KnownOptimizer;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;

import java.lang.reflect.Method;

public class Profiler {

    private static final Slf4JLogger LOGGER = new Slf4JLogger();

    public static void main(final String[] args) throws NoSuchMethodException {
        int counter = 0;
        while(true) {
            counter ++ ;
            System.out.println("Performing run " + counter);
            final Class theClassToTest = JBox2DTest.class;
            final Method theMethodToTest = theClassToTest.getMethod("testSimpleBall", new Class[0]);
            final CompileTarget theCompileTarget = new CompileTarget(theClassToTest.getClassLoader(),
                    CompileTarget.BackendType.js);

            final BytecodeMethodSignature theSignature = theCompileTarget.toMethodSignature(theMethodToTest);

            final CompileOptions theOptions = new CompileOptions(LOGGER, true, KnownOptimizer.ALL, true, "bytecoder", 512, 512, false, false, Allocator.passthru);
            final JSCompileResult result = (JSCompileResult) theCompileTarget
                    .compile(theOptions, theClassToTest, theMethodToTest.getName(), theSignature);
            final JSCompileResult.JSContent content = result.getContent()[0];
        }
    }
}
