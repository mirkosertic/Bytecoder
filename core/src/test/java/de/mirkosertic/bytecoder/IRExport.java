package de.mirkosertic.bytecoder;

import de.mirkosertic.bytecoder.core.Slf4JLogger;
import de.mirkosertic.bytecoder.core.backend.wasm.WasmIntrinsics;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.loader.BytecoderLoader;
import de.mirkosertic.bytecoder.core.optimizer.Optimizations;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;
import de.mirkosertic.bytecoder.core.parser.Loader;
import org.objectweb.asm.Type;

public class IRExport {

    public static void dosomething() {
        int x = 0;
        for (int i = 0; i < 100; i++) {
            x = x + i;
        }
    }

    public static void main(String[] args) {
        final Class javaClass = IRExport.class;
        final ClassLoader cl = javaClass.getClassLoader();
        final Loader loader = new BytecoderLoader(cl);

        final CompileUnit compileUnit = new CompileUnit(loader, new Slf4JLogger(), new WasmIntrinsics());
        final Type invokedType = Type.getType(javaClass);

        final ResolvedMethod method = compileUnit.resolveMainMethod(invokedType, "dosomething", Type.getMethodType(Type.VOID_TYPE));

        compileUnit.finalizeLinkingHierarchy();

        compileUnit.logStatistics();

        Optimizations.DEFAULT.optimize(compileUnit, method);

        method.methodBody.writeDebugTo(System.out);

    }
}
