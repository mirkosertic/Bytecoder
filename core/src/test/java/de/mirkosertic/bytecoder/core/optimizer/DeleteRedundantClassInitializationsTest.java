package de.mirkosertic.bytecoder.core.optimizer;

import de.mirkosertic.bytecoder.api.Logger;
import de.mirkosertic.bytecoder.core.Slf4JLogger;
import de.mirkosertic.bytecoder.core.backend.BackendType;
import de.mirkosertic.bytecoder.core.ir.AnalysisStack;
import de.mirkosertic.bytecoder.core.ir.ClassInitialization;
import de.mirkosertic.bytecoder.core.ir.EdgeType;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.If;
import de.mirkosertic.bytecoder.core.ir.Region;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.ir.Return;
import de.mirkosertic.bytecoder.core.ir.StandardProjections;
import de.mirkosertic.bytecoder.core.loader.BytecoderLoader;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;
import de.mirkosertic.bytecoder.core.parser.CoreIntrinsics;
import de.mirkosertic.bytecoder.core.parser.Loader;
import org.junit.Test;
import org.objectweb.asm.Type;

import static org.junit.Assert.*;

public class DeleteRedundantClassInitializationsTest {

    public static class Dummy {
    }

    @Test
    public void testDoNothingOnEmptyGraph() {
        final Logger logger = new Slf4JLogger();
        final Loader loader = new BytecoderLoader(getClass().getClassLoader());
        final CompileUnit compileUnit = new CompileUnit(loader, logger, new CoreIntrinsics());
        final Graph g = new Graph(logger);
        final ResolvedMethod resolvedMethod = new ResolvedMethod(null, null, null);
        resolvedMethod.methodBody = g;

        final Region startRegion = g.newStartRegion();
        final Return ret = g.newReturnNothing();
        startRegion.addControlFlowTo(StandardProjections.DEFAULT, ret);

        final DeleteRedundantClassInitializations optimizer = new DeleteRedundantClassInitializations();
        assertFalse(optimizer.optimize(BackendType.Wasm, compileUnit, resolvedMethod));
    }

    @Test
    public void testDeleteInSimpleFlow() {
        final Logger logger = new Slf4JLogger();

        final Type typeWithoutInitializer = Type.getType(Dummy.class);
        final CompileUnit compileUnit = new CompileUnit(new BytecoderLoader(getClass().getClassLoader()), logger, new CoreIntrinsics());
        compileUnit.resolveClass(typeWithoutInitializer, new AnalysisStack());

        final Graph g = new Graph(logger);
        final ResolvedMethod resolvedMethod = new ResolvedMethod(null, null, null);
        resolvedMethod.methodBody = g;

        final Region startRegion = g.newStartRegion();
        final Return ret = g.newReturnNothing();
        final ClassInitialization classInitialization = g.newClassInitialization(typeWithoutInitializer);
        startRegion.addControlFlowTo(StandardProjections.DEFAULT, classInitialization);
        classInitialization.addControlFlowTo(StandardProjections.DEFAULT, ret);

        final DeleteRedundantClassInitializations optimizer = new DeleteRedundantClassInitializations();
        assertFalse(optimizer.optimize(BackendType.Wasm, compileUnit, resolvedMethod));
        assertFalse(g.nodes().contains(classInitialization));
        assertEquals(2, g.nodes().size());

        assertTrue(classInitialization.controlComingFrom.isEmpty());
        assertTrue(classInitialization.controlFlowsTo.isEmpty());

        assertEquals(1, startRegion.controlFlowsTo.size());
        assertSame(ret, startRegion.controlFlowsTo.get(StandardProjections.DEFAULT));
        assertEquals(1, ret.controlComingFrom.size());
        assertTrue(ret.controlComingFrom.contains(startRegion));
    }

    @Test
    public void testDeleteWithBackEdge() {
        final Logger logger = new Slf4JLogger();

        final Type typeWithoutInitializer = Type.getType(Dummy.class);
        final CompileUnit compileUnit = new CompileUnit(new BytecoderLoader(getClass().getClassLoader()), logger, new CoreIntrinsics());
        compileUnit.resolveClass(typeWithoutInitializer, new AnalysisStack());

        final Graph g = new Graph(logger);
        final ResolvedMethod resolvedMethod = new ResolvedMethod(null, null, null);
        resolvedMethod.methodBody = g;

        final Region startRegion = g.newStartRegion();
        final Region jump = g.newRegion("Jump");
        final Return ret = g.newReturnNothing();
        final ClassInitialization ci1 = g.newClassInitialization(typeWithoutInitializer);
        final ClassInitialization ci2 = g.newClassInitialization(typeWithoutInitializer);
        final If iff = g.newIf();

        startRegion.addControlFlowTo(StandardProjections.DEFAULT, jump);
        jump.addControlFlowTo(StandardProjections.DEFAULT, ci1);
        ci1.addControlFlowTo(StandardProjections.DEFAULT, ci2);
        ci2.addControlFlowTo(StandardProjections.DEFAULT, iff);
        iff.addControlFlowTo(StandardProjections.TRUE, ret);
        iff.addControlFlowTo(StandardProjections.FALSE.withEdgeType(EdgeType.BACK), jump);

        final DeleteRedundantClassInitializations optimizer = new DeleteRedundantClassInitializations();
        assertFalse(optimizer.optimize(BackendType.Wasm, compileUnit, resolvedMethod));
        assertEquals(4, g.nodes().size());

        assertTrue(g.nodes().contains(startRegion));
        assertTrue(g.nodes().contains(jump));
        assertTrue(g.nodes().contains(iff));
        assertTrue(g.nodes().contains(ret));
    }
}
