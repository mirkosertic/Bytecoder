package de.mirkosertic.bytecoder;

import de.mirkosertic.bytecoder.core.Slf4JLogger;
import de.mirkosertic.bytecoder.core.backend.BackendType;
import de.mirkosertic.bytecoder.core.backend.GeneratedMethodsRegistry;
import de.mirkosertic.bytecoder.core.backend.js.JSStructuredControlflowCodeGenerator;
import de.mirkosertic.bytecoder.core.backend.sequencer.DominatorTree;
import de.mirkosertic.bytecoder.core.backend.sequencer.Sequencer;
import de.mirkosertic.bytecoder.core.backend.wasm.WasmIntrinsics;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.loader.BytecoderLoader;
import de.mirkosertic.bytecoder.core.optimizer.Optimizations;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;
import de.mirkosertic.bytecoder.core.parser.Loader;
import org.objectweb.asm.Type;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.stream.Collectors;

public class IRExport {

    public static void doit(final String[] args) {

    }

    public int dosomething(final int value) {
        doit(new String[0]);
        final IRExport dummy = new IRExport();
        int x = value;
        for (int i = 0; i < 100; i++) {
            x = x + i + value;
        }
        return x;
    }

    public static void main(final String[] args) throws IOException {
        final Class javaClass = IRExport.class;
        //final Class javaClass = FuncState.class;
        final ClassLoader cl = javaClass.getClassLoader();
        final Loader loader = new BytecoderLoader(cl);

        final CompileUnit compileUnit = new CompileUnit(loader, new Slf4JLogger(), new WasmIntrinsics());
        final Type invokedType = Type.getType(javaClass);

        final ResolvedMethod method = compileUnit.resolveMainMethod(invokedType, "dosomething", Type.getMethodType(Type.INT_TYPE, Type.INT_TYPE));
        //final ResolvedMethod method = compileUnit.resolveMainMethod(invokedType, "need_value", Type.getMethodType(Type.BOOLEAN_TYPE, Type.INT_TYPE));

        //final ResolvedMethod method = compileUnit.resolveMainMethod(Type.getType(Buffer.class), "<clinit>", Type.getMethodType(Type.VOID_TYPE));

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

        int optIter = 0;
        while (Optimizations.DEFAULT.optimize(BackendType.Wasm, compileUnit, method)) {
            System.out.println("Running Optimization Step " + ++optIter);
        }

//        new VariableIsConstant().optimize(compileUnit, method);

        // Drop no longer used constants
        for (final Node unusedConstant : g.nodes().stream().filter(t -> ((t.isConstant()) && t.outgoingDataFlows().length == 0)).collect(Collectors.toList())) {
            g.deleteNode(unusedConstant);
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
