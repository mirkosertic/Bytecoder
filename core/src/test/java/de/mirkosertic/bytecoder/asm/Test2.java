package de.mirkosertic.bytecoder.asm;

import de.mirkosertic.bytecoder.asm.parser.CompileUnit;
import de.mirkosertic.bytecoder.asm.parser.CoreIntrinsics;
import org.junit.Test;
import org.objectweb.asm.Type;

import java.util.Collections;

public class Test2 {

    @Test
    public void testLinkage() {
        final AnalysisStack analysisStack = new AnalysisStack();

        final ClassLoader cl = Test.class.getClassLoader();
        final CompileUnit compileUnit = new CompileUnit(cl, new CoreIntrinsics(Collections.emptyList()));
        final Type invokedType = Type.getObjectType("jdk/internal/loader/AbstractClassLoaderValue$Memoizer");
        final ResolvedClass resolvedClass = compileUnit.resolveClass(invokedType, analysisStack);
        try {
            final ResolvedMethod method = resolvedClass.resolveMethod("get", Type.getMethodType(Type.getType(Object.class)), analysisStack);
            analysisStack.dumpAnalysisStack(System.out);
        } catch (final AnalysisException e) {
            e.getAnalysisStack().dumpAnalysisStack(System.out);
            throw e;
        }
    }
}