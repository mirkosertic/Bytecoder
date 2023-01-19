package de.mirkosertic.bytecoder.asm;

import de.mirkosertic.bytecoder.asm.loader.BytecoderLoader;
import de.mirkosertic.bytecoder.asm.parser.CompileUnit;
import de.mirkosertic.bytecoder.asm.parser.CoreIntrinsics;
import de.mirkosertic.bytecoder.asm.parser.Loader;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;
import org.junit.Test;
import org.objectweb.asm.Type;

import java.util.Collections;

public class Test2 {

    @Test
    public void testLinkage() {
        final AnalysisStack analysisStack = new AnalysisStack();

        final ClassLoader cl = Test.class.getClassLoader();
        final Loader loader = new BytecoderLoader(cl);
        final CompileUnit compileUnit = new CompileUnit(loader, new Slf4JLogger(), new CoreIntrinsics(Collections.emptyList()));
        try {
            final ResolvedMethod method = compileUnit.resolveMainMethod(Type.getType(InterfaceLinkTest.class), "testCompute", Type.getMethodType(Type.VOID_TYPE));
            analysisStack.dumpAnalysisStack(System.out);
        } catch (final AnalysisException e) {
            e.getAnalysisStack().dumpAnalysisStack(System.out);
            throw e;
        }
        compileUnit.finalizeLinkingHierarchy();
    }
}
