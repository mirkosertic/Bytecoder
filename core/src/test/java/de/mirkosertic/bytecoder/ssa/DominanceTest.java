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

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DominanceTest {

    @Test
    public void testDirectFlow() {
        Program theProgram = new Program();
        ControlFlowGraph theGraph = new ControlFlowGraph(theProgram);

        GraphNode theNode1 = theGraph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, GraphNode.BlockType.NORMAL);
        theNode1.addExpression(new GotoExpression(new BytecodeOpcodeAddress(10)));
        GraphNode theNode2 = theGraph.createAt(new BytecodeOpcodeAddress(10), GraphNode.BlockType.NORMAL);
        theNode2.addExpression(new GotoExpression(new BytecodeOpcodeAddress(20)));
        GraphNode theNode3 = theGraph.createAt(new BytecodeOpcodeAddress(20), GraphNode.BlockType.NORMAL);
        theNode3.addExpression(new ReturnExpression());

        theNode1.addSuccessor(theNode2);
        theNode2.addSuccessor(theNode3);

        theGraph.calculateReachabilityAndMarkBackEdges();

        assertTrue(theNode2.isOnlyReachableThru(theNode1));
        assertTrue(theNode3.isOnlyReachableThru(theNode1));
        assertTrue(theNode3.isOnlyReachableThru(theNode2));

        assertFalse(theNode1.isOnlyReachableThru(theNode2));
        assertFalse(theNode1.isOnlyReachableThru(theNode3));
        assertFalse(theNode2.isOnlyReachableThru(theNode3));

        Set<GraphNode> theDom1 = theGraph.dominatedNodesOf(theNode1);
        assertEquals(3, theDom1.size(), 0);
        assertTrue(theDom1.contains(theNode2));
        assertTrue(theDom1.contains(theNode3));
        assertTrue(theDom1.contains(theNode1));

        Set<GraphNode> theDom2 = theGraph.dominatedNodesOf(theNode2);
        assertEquals(2, theDom2.size(), 0);
        assertTrue(theDom2.contains(theNode3));
        assertTrue(theDom2.contains(theNode2));
        assertFalse(theDom2.contains(theNode1));
    }

    @Test
    public void testEndlessLoop() {
        Program theProgram = new Program();
        ControlFlowGraph theGraph = new ControlFlowGraph(theProgram);

        GraphNode theNode1 = theGraph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, GraphNode.BlockType.NORMAL);
        theNode1.addExpression(new GotoExpression(new BytecodeOpcodeAddress(10)));
        GraphNode theNode2 = theGraph.createAt(new BytecodeOpcodeAddress(10), GraphNode.BlockType.NORMAL);
        theNode2.addExpression(new GotoExpression(new BytecodeOpcodeAddress(20)));
        GraphNode theNode3 = theGraph.createAt(new BytecodeOpcodeAddress(20), GraphNode.BlockType.NORMAL);
        theNode3.addExpression(new GotoExpression(BytecodeOpcodeAddress.START_AT_ZERO));

        theNode1.addSuccessor(theNode2);
        theNode2.addSuccessor(theNode3);
        theNode3.addSuccessor(theNode1);

        theGraph.calculateReachabilityAndMarkBackEdges();
    }

    @Test
    public void testIFElseWithJoining() {
        Program theProgram = new Program();
        ControlFlowGraph theGraph = new ControlFlowGraph(theProgram);

        GraphNode theNode1 = theGraph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, GraphNode.BlockType.NORMAL);

        ExpressionList theExpressions = new ExpressionList();
        IFExpression theIF = new IFExpression(new BytecodeOpcodeAddress(1), new BytecodeOpcodeAddress(10),
                new IntegerValue(1), theExpressions);
        theExpressions.add(new GotoExpression(new BytecodeOpcodeAddress(10)));
        theNode1.addExpression(theIF);
        theNode1.addExpression(new GotoExpression(new BytecodeOpcodeAddress(20)));

        GraphNode theNode2 = theGraph.createAt(new BytecodeOpcodeAddress(10), GraphNode.BlockType.NORMAL);
        theNode2.addExpression(new GotoExpression(new BytecodeOpcodeAddress(30)));

        GraphNode theNode3 = theGraph.createAt(new BytecodeOpcodeAddress(20), GraphNode.BlockType.NORMAL);
        theNode3.addExpression(new GotoExpression(new BytecodeOpcodeAddress(30)));

        GraphNode theNode4 = theGraph.createAt(new BytecodeOpcodeAddress(30), GraphNode.BlockType.NORMAL);
        theNode4.addExpression(new ReturnExpression());

        theNode1.addSuccessor(theNode2);
        theNode1.addSuccessor(theNode3);
        theNode2.addSuccessor(theNode4);
        theNode3.addSuccessor(theNode4);

        theGraph.calculateReachabilityAndMarkBackEdges();

        assertTrue(theNode4.isOnlyReachableThru(theNode1));
        assertFalse(theNode4.isOnlyReachableThru(theNode2));
        assertFalse(theNode4.isOnlyReachableThru(theNode3));

        Set<GraphNode> theDom1 = theGraph.dominatedNodesOf(theNode1);
        assertEquals(3, theDom1.size(), 0);
        assertTrue(theDom1.contains(theNode2));
        assertTrue(theDom1.contains(theNode3));
        assertTrue(theDom1.contains(theNode4));
    }
}
