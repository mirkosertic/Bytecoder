package de.mirkosertic.bytecoder;

import de.mirkosertic.bytecoder.core.Slf4JLogger;
import de.mirkosertic.bytecoder.core.backend.BackendType;
import de.mirkosertic.bytecoder.core.backend.GeneratedMethodsRegistry;
import de.mirkosertic.bytecoder.core.backend.js.JSStructuredControlflowCodeGenerator;
import de.mirkosertic.bytecoder.core.backend.sequencer.DominatorTree;
import de.mirkosertic.bytecoder.core.backend.sequencer.Sequencer;
import de.mirkosertic.bytecoder.core.backend.wasm.WasmIntrinsics;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.loader.BytecoderLoader;
import de.mirkosertic.bytecoder.core.optimizer.Optimizations;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;
import de.mirkosertic.bytecoder.core.parser.Loader;
import org.objectweb.asm.Type;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class IRExport {

    int target;
    int target2;

    public static void doit(final String[] args) {

    }

    public int dosomething(final int value) {
        //doit(new String[0]);
        //final IRExport dummy = new IRExport();
        assert value > 10;
        int x = value;
        for (int i = 0; i < 100; i++) {
            x = x + i + value;
        }
        IRExport t = new IRExport();
        t.target = x;
        t.target2 = x + 1;
        return x;
    }

    public static void main(final String[] args) throws IOException, ClassNotFoundException {
        final Class javaClass = IRExport.class;
        //final Class javaClass = FuncState.class;
        final ClassLoader cl = javaClass.getClassLoader();
        final Loader loader = new BytecoderLoader(cl);

        final CompileUnit compileUnit = new CompileUnit(loader, new Slf4JLogger(), new WasmIntrinsics());
        final Type invokedType = Type.getType(javaClass);

        final ResolvedMethod method = compileUnit.resolveMainMethod(invokedType, "dosomething", Type.getMethodType(Type.INT_TYPE, Type.INT_TYPE));
        //final ResolvedMethod method = compileUnit.resolveMainMethod(invokedType, "need_value", Type.getMethodType(Type.BOOLEAN_TYPE, Type.INT_TYPE));

        //final ResolvedMethod method = compileUnit.resolveMainMethod(Type.getType(Buffer.class), "<clinit>", Type.getMethodType(Type.VOID_TYPE));
        //final ResolvedMethod method = compileUnit.resolveMainMethod(Type.getType("Ljdk/internal/util/ArraysSupport;"), "<clinit>", Type.getMethodType(Type.VOID_TYPE));
        //final ResolvedMethod method = compileUnit.resolveMainMethod(Type.getType("Ljava/lang/Object;"), "equals", Type.getMethodType(Type.BOOLEAN_TYPE, Type.getType(Object.class)));

        compileUnit.finalizeLinkingHierarchy();

        compileUnit.logStatistics();

        final Graph g = method.methodBody;

        try (final FileOutputStream fos = new FileOutputStream("debug.dot")) {
            g.writeDebugTo(fos);
        }

        try (final FileOutputStream fos = new FileOutputStream("debug_dt.dot")) {
            final DominatorTree dt = new DominatorTree(g);
            dt.writeDebugTo(fos);
        }

        while (Optimizations.ALL.optimize(BackendType.JS, compileUnit, method)) {
        }

        try (final FileOutputStream fos = new FileOutputStream("debug_optimized.dot")) {
            g.writeDebugTo(fos);
        }

        try (final FileOutputStream fos = new FileOutputStream("debug_dt_optimized.dot")) {
            final DominatorTree dt = new DominatorTree(g);
            dt.writeDebugTo(fos);
        }

        final GeneratedMethodsRegistry generatedMethodsRegistry = new GeneratedMethodsRegistry();
        final PrintWriter pw = new PrintWriter(System.out);
        new Sequencer(g, new DominatorTree(g), new JSStructuredControlflowCodeGenerator(compileUnit, method.owner, pw, generatedMethodsRegistry));
        pw.flush();
    }
}
