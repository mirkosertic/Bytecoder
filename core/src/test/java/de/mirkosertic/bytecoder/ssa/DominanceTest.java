/*
 * Copyright 2018 Mirko Sertic
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import org.junit.Test;

import java.util.Set;

public class DominanceTest {

    @Test
    public void testDirectFlow() {
        final Program theProgram = new Program(DebugInformation.empty(), null);
        final ControlFlowGraph theGraph = new ControlFlowGraph(theProgram);

        final RegionNode theNode1 = theGraph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        theNode1.getExpressions().add(new GotoExpression(null, null, new BytecodeOpcodeAddress(10)));
        final RegionNode theNode2 = theGraph.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        theNode2.getExpressions().add(new GotoExpression(null, null,new BytecodeOpcodeAddress(20)));
        final RegionNode theNode3 = theGraph.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        theNode3.getExpressions().add(new ReturnExpression(null, null));

        theNode1.addEdgeTo(ControlFlowEdgeType.forward, theNode2);
        theNode2.addEdgeTo(ControlFlowEdgeType.forward, theNode3);

        theGraph.calculateReachabilityAndMarkBackEdges();

        assertTrue(theNode2.isDominatedBy(theNode1));
        assertTrue(theNode3.isDominatedBy(theNode1));
        assertTrue(theNode3.isDominatedBy(theNode2));

        assertFalse(theNode1.isDominatedBy(theNode2));
        assertFalse(theNode1.isDominatedBy(theNode3));
        assertFalse(theNode2.isDominatedBy(theNode3));

        final Set<RegionNode> theDom1 = theGraph.dominatedNodesOf(theNode1);
        assertEquals(3, theDom1.size(), 0);
        assertTrue(theDom1.contains(theNode2));
        assertTrue(theDom1.contains(theNode3));
        assertTrue(theDom1.contains(theNode1));

        final Set<RegionNode> theDom2 = theGraph.dominatedNodesOf(theNode2);
        assertEquals(2, theDom2.size(), 0);
        assertTrue(theDom2.contains(theNode3));
        assertTrue(theDom2.contains(theNode2));
        assertFalse(theDom2.contains(theNode1));
    }

    @Test
    public void testEndlessLoop() {
        final Program theProgram = new Program(DebugInformation.empty(), null);
        final ControlFlowGraph theGraph = new ControlFlowGraph(theProgram);

        final RegionNode theNode1 = theGraph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        theNode1.getExpressions().add(new GotoExpression(null, null, new BytecodeOpcodeAddress(10)));
        final RegionNode theNode2 = theGraph.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        theNode2.getExpressions().add(new GotoExpression(null, null, new BytecodeOpcodeAddress(20)));
        final RegionNode theNode3 = theGraph.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        theNode3.getExpressions().add(new GotoExpression(null, null, BytecodeOpcodeAddress.START_AT_ZERO));

        theNode1.addEdgeTo(ControlFlowEdgeType.forward, theNode2);
        theNode2.addEdgeTo(ControlFlowEdgeType.forward, theNode3);
        theNode3.addEdgeTo(ControlFlowEdgeType.forward, theNode1);

        theGraph.calculateReachabilityAndMarkBackEdges();
    }

    @Test
    public void testIFElseWithJoining() {
        final Program theProgram = new Program(DebugInformation.empty(), null);
        final ControlFlowGraph theGraph = new ControlFlowGraph(theProgram);

        final RegionNode theNode1 = theGraph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);

        final ExpressionList theExpressions = new ExpressionList();
        final IFExpression theIF = new IFExpression(null, new BytecodeOpcodeAddress(1), new BytecodeOpcodeAddress(10),
                new IntegerValue(1), theExpressions);
        theExpressions.add(new GotoExpression(null, null, new BytecodeOpcodeAddress(10)));
        theNode1.getExpressions().add(theIF);
        theNode1.getExpressions().add(new GotoExpression(null, null, new BytecodeOpcodeAddress(20)));

        final RegionNode theNode2 = theGraph.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        theNode2.getExpressions().add(new GotoExpression(null, null, new BytecodeOpcodeAddress(30)));

        final RegionNode theNode3 = theGraph.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        theNode3.getExpressions().add(new GotoExpression(null, null, new BytecodeOpcodeAddress(30)));

        final RegionNode theNode4 = theGraph.createAt(new BytecodeOpcodeAddress(30), RegionNode.BlockType.NORMAL);
        theNode4.getExpressions().add(new ReturnExpression(null, null));

        theNode1.addEdgeTo(ControlFlowEdgeType.forward, theNode2);
        theNode1.addEdgeTo(ControlFlowEdgeType.forward, theNode3);
        theNode2.addEdgeTo(ControlFlowEdgeType.forward, theNode4);
        theNode3.addEdgeTo(ControlFlowEdgeType.forward, theNode4);

        theGraph.calculateReachabilityAndMarkBackEdges();

        assertTrue(theNode4.isDominatedBy(theNode1));
        assertFalse(theNode4.isDominatedBy(theNode2));
        assertFalse(theNode4.isDominatedBy(theNode3));

        final Set<RegionNode> theDom1 = theGraph.dominatedNodesOf(theNode1);
        assertEquals(4, theDom1.size(), 0);
        assertTrue(theDom1.contains(theNode2));
        assertTrue(theDom1.contains(theNode3));
        assertTrue(theDom1.contains(theNode4));
    }
}
