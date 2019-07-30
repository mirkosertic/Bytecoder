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
package de.mirkosertic.bytecoder.optimizer;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.DebugInformation;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.ReturnExpression;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class InlineDominatedNodesOptimizerTest {

    @Test
    public void testSimpleSequence() {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode startNode = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        startNode.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(10)));
        final RegionNode node1 = g.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        node1.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(1), new BytecodeOpcodeAddress(20)));
        final RegionNode node2 = g.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        final ReturnExpression ret = new ReturnExpression(p,new BytecodeOpcodeAddress(20));
        node2.getExpressions().add(ret);
        startNode.addSuccessor(node1);
        node1.addSuccessor(node2);
        g.calculateReachabilityAndMarkBackEdges();

        new InlineDominatedNodesOptimizer().optimize(g, null);

        assertEquals(1, g.getKnownNodes().size());
        final RegionNode remaining = g.nodeStartingAt(BytecodeOpcodeAddress.START_AT_ZERO);
        final ExpressionList l = remaining.getExpressions();
        assertEquals(Collections.singletonList(ret), l.toList());
    }

    @Test
    public void testSimpleLoopDoubleExit() {

        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode startNode = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        startNode.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(20)));
        startNode.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(10)));
        final RegionNode node1 = g.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        node1.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(1), new BytecodeOpcodeAddress(20)));
        node1.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(1), new BytecodeOpcodeAddress(0)));
        final RegionNode node2 = g.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);

        startNode.addSuccessor(node1);
        startNode.addSuccessor(node2);
        node1.addSuccessor(startNode);
        g.calculateReachabilityAndMarkBackEdges();

        new InlineDominatedNodesOptimizer().optimize(g, null);

        assertEquals(1, g.getKnownNodes().size());
    }
}