/*
 * Copyright 2019 Mirko Sertic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mirkosertic.bytecoder.ssa;

import de.mirkosertic.bytecoder.backend.js.JSIntrinsics;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeLoader;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LivenessTest {

    private static int singleBlockMethod(final int a, final int b) {
        final int c = a + b + 20;
        return c;
    }

    @Test
    public void testSingleBlockMethodLiveness() {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new JSIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(getClass()));
        theLinkedClass.resolveStaticMethod("singleBlockMethod", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull("singleBlockMethod", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);

        final List<Variable> vars = p.getVariables();
        for (final Variable v : vars) {
            System.out.println(String.format("%s Def at %d, LastUsedAt %d", v.getName(), v.liveRange().getDefinedAt(), v.liveRange().getLastUsedAt()));
        }

        final ControlFlowGraph graph = p.getControlFlowGraph();
        final List<RegionNode> preorder = graph.dominators().getPreOrder();
        assertEquals(1, preorder.size());
        final RegionNode node = preorder.get(0);
        assertEquals(0L, node.getStartAnalysisTime());
        assertEquals(8L, node.getFinishedAnalysisTime());

        final BlockState liveIn = node.liveIn();
        assertEquals(2, liveIn.getPorts().size());
        assertNotNull(liveIn.getPorts().get(new LocalVariableDescription(0, TypeRef.Native.INT)));
        assertNotNull(liveIn.getPorts().get(new LocalVariableDescription(1, TypeRef.Native.INT)));

        final BlockState liveOut = node.liveOut();
        assertTrue(liveOut.getPorts().isEmpty());
    }

    private static int simpleForwardConditional(final int a, final int b) {
        final int unused = 100;
        int x = 10;
        int y = 20;
        int z = 30;
        if (a > 0) {
            x+=10;
        } else {
            y+=20;
        }
        z++;
        return 0;
    }

    @Test
    public void testSimpleForwardConditionalLiveness() {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new JSIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(getClass()));
        theLinkedClass.resolveStaticMethod("simpleForwardConditional", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull("simpleForwardConditional", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);

        final List<Variable> vars = p.getVariables();
        for (final Variable v : vars) {
            System.out.println(String.format("%s Def at %d, LastUsedAt %d", v.getName(), v.liveRange().getDefinedAt(), v.liveRange().getLastUsedAt()));
        }

        final ControlFlowGraph graph = p.getControlFlowGraph();
        final List<RegionNode> preorder = graph.dominators().getPreOrder();
        assertEquals(4, preorder.size());
        final RegionNode startNode = preorder.get(0);
        final RegionNode trueBranch = preorder.get(1);
        final RegionNode falseBranch = preorder.get(2);
        final RegionNode finalNode = preorder.get(3);

        assertEquals(0L, startNode.getStartAnalysisTime());
        assertEquals(10L, startNode.getFinishedAnalysisTime());

        assertEquals(10L, trueBranch.getStartAnalysisTime());
        assertEquals(12L, trueBranch.getFinishedAnalysisTime());

        assertEquals(12L, falseBranch.getStartAnalysisTime());
        assertEquals(13L, falseBranch.getFinishedAnalysisTime());

        assertEquals(13L, finalNode.getStartAnalysisTime());
        assertEquals(16L, finalNode.getFinishedAnalysisTime());

        final BlockState startLiveIn = startNode.liveIn();
        assertEquals(1, startLiveIn.getPorts().size());
        assertNotNull(startLiveIn.getPorts().get(new LocalVariableDescription(0, TypeRef.Native.INT)));

        final BlockState trueLiveIn = trueBranch.liveIn();
        assertEquals(2, trueLiveIn.getPorts().size());
        assertNotNull(trueLiveIn.getPorts().get(new LocalVariableDescription(3, TypeRef.Native.INT)));
        assertNotNull(trueLiveIn.getPorts().get(new LocalVariableDescription(5, TypeRef.Native.INT)));

        final BlockState trueLiveOut = trueBranch.liveOut();
        assertEquals(1, trueLiveOut.getPorts().size());
        assertNotNull(trueLiveOut.getPorts().get(new LocalVariableDescription(5, TypeRef.Native.INT)));

        final BlockState falseLiveIn = falseBranch.liveIn();
        assertEquals(2, trueLiveIn.getPorts().size());
        assertNotNull(falseLiveIn.getPorts().get(new LocalVariableDescription(4, TypeRef.Native.INT)));
        assertNotNull(falseLiveIn.getPorts().get(new LocalVariableDescription(5, TypeRef.Native.INT)));

        final BlockState falseLiveOut = falseBranch.liveOut();
        assertEquals(1, falseLiveOut.getPorts().size());
        assertNotNull(falseLiveOut.getPorts().get(new LocalVariableDescription(5, TypeRef.Native.INT)));

        final BlockState finalLiveIn = finalNode.liveIn();
        assertEquals(1, finalLiveIn.getPorts().size());
        assertNotNull(finalLiveIn.getPorts().get(new LocalVariableDescription(5, TypeRef.Native.INT)));

        final BlockState finalLiveOut = finalNode.liveOut();
        assertTrue(finalLiveOut.getPorts().isEmpty());
    }

    private static int simpleForLoop(final int a, final int b) {
        final int x = 10;
        int y = 20;
        for (int i=0;i<10;i++) {
            y = y + 1;
            final int z = 30;
        }
        final int k = 30;
        return y;
    }

    @Test
    public void simpleForLoopLiveness() {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new JSIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(getClass()));
        theLinkedClass.resolveStaticMethod("simpleForLoop", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull("simpleForLoop", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);

        final List<Variable> vars = p.getVariables();
        for (final Variable v : vars) {
            System.out.println(String.format("%s Def at %d, LastUsedAt %d", v.getName(), v.liveRange().getDefinedAt(), v.liveRange().getLastUsedAt()));
        }

        final ControlFlowGraph graph = p.getControlFlowGraph();
        final List<RegionNode> preorder = graph.dominators().getPreOrder();
        assertEquals(4, preorder.size());
        final RegionNode startNode = preorder.get(0);
        assertEquals(0L, startNode.getStartAnalysisTime());
        assertEquals(6L, startNode.getFinishedAnalysisTime());

        final RegionNode loopHeader = preorder.get(1);
        assertEquals(6L, loopHeader.getStartAnalysisTime());
        assertEquals(9L, loopHeader.getFinishedAnalysisTime());

        final RegionNode loopBody = preorder.get(2);
        assertEquals(9L, loopBody.getStartAnalysisTime());
        assertEquals(17L, loopBody.getFinishedAnalysisTime());

        final RegionNode finalNode = preorder.get(3);
        assertEquals(17L, finalNode.getStartAnalysisTime());
        assertEquals(21L, finalNode.getFinishedAnalysisTime());

        final BlockState startLiveIn = startNode.liveIn();
        assertTrue(startLiveIn.getPorts().isEmpty());

        final BlockState startLiveOut = startNode.liveOut();
        assertEquals(2, startLiveOut.getPorts().size());
        assertNotNull(startLiveOut.getPorts().get(new LocalVariableDescription(3, TypeRef.Native.INT)));
        assertNotNull(startLiveOut.getPorts().get(new LocalVariableDescription(4, TypeRef.Native.INT)));

        final BlockState loopHeaderLiveIn = loopHeader.liveIn();
        assertEquals(2, loopHeaderLiveIn.getPorts().size());
        assertNotNull(loopHeaderLiveIn.getPorts().get(new LocalVariableDescription(3, TypeRef.Native.INT)));
        assertNotNull(loopHeaderLiveIn.getPorts().get(new LocalVariableDescription(4, TypeRef.Native.INT)));

        final BlockState loopHeaderLiveOut = loopHeader.liveOut();
        assertEquals(2, loopHeaderLiveOut.getPorts().size());
        assertNotNull(loopHeaderLiveOut.getPorts().get(new LocalVariableDescription(3, TypeRef.Native.INT)));
        assertNotNull(loopHeaderLiveOut.getPorts().get(new LocalVariableDescription(4, TypeRef.Native.INT)));

        final BlockState loopBodyLiveIn = loopBody.liveIn();
        assertEquals(2, loopBodyLiveIn.getPorts().size());
        assertNotNull(loopBodyLiveIn.getPorts().get(new LocalVariableDescription(3, TypeRef.Native.INT)));
        assertNotNull(loopBodyLiveIn.getPorts().get(new LocalVariableDescription(4, TypeRef.Native.INT)));

        final BlockState loopBodyLiveOut = loopBody.liveOut();
        assertEquals(2, loopBodyLiveOut.getPorts().size());
        assertNotNull(loopBodyLiveOut.getPorts().get(new LocalVariableDescription(3, TypeRef.Native.INT)));
        assertNotNull(loopBodyLiveOut.getPorts().get(new LocalVariableDescription(4, TypeRef.Native.INT)));

        final BlockState finalLiveIn = finalNode.liveIn();
        assertEquals(1, finalLiveIn.getPorts().size());
        assertNotNull(finalLiveIn.getPorts().get(new LocalVariableDescription(3, TypeRef.Native.INT)));

        final BlockState finalLiveOut = finalNode.liveOut();
        assertTrue(finalLiveOut.getPorts().isEmpty());
    }
}
