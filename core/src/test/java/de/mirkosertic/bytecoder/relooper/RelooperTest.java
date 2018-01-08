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
package de.mirkosertic.bytecoder.relooper;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.GraphNode;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ReturnExpression;
import org.junit.Test;

public class RelooperTest {

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

        Relooper theRelooper = new Relooper();
        Relooper.Block theBlock = theRelooper.reloop(theGraph);

        theRelooper.debugPrint(System.out, theBlock);
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

        Relooper theRelooper = new Relooper();
        Relooper.Block theBlock = theRelooper.reloop(theGraph);

        theRelooper.debugPrint(System.out, theBlock);
    }

    @Test
    public void testIf() {
        Program theProgram = new Program();
        ControlFlowGraph theGraph = new ControlFlowGraph(theProgram);

        GraphNode theNode1 = theGraph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, GraphNode.BlockType.NORMAL);
        theNode1.addExpression(new GotoExpression(new BytecodeOpcodeAddress(10)));

        GraphNode theNode2 = theGraph.createAt(new BytecodeOpcodeAddress(10), GraphNode.BlockType.NORMAL);
        theNode2.addExpression(new GotoExpression(new BytecodeOpcodeAddress(20)));

        GraphNode theNode3 = theGraph.createAt(new BytecodeOpcodeAddress(20), GraphNode.BlockType.NORMAL);
        theNode3.addExpression(new ReturnExpression());

        theNode1.addSuccessor(theNode2);
        theNode1.addSuccessor(theNode3);
        theNode2.addSuccessor(theNode3);

        theGraph.calculateReachabilityAndMarkBackEdges();

        Relooper theRelooper = new Relooper();
        Relooper.Block theBlock = theRelooper.reloop(theGraph);

        theRelooper.debugPrint(System.out, theBlock);
    }

    @Test
    public void testSwitchCase() {
        Program theProgram = new Program();
        ControlFlowGraph theGraph = new ControlFlowGraph(theProgram);

        GraphNode theNode1 = theGraph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, GraphNode.BlockType.NORMAL);

        GraphNode theNode2 = theGraph.createAt(new BytecodeOpcodeAddress(10), GraphNode.BlockType.NORMAL);
        theNode2.addExpression(new ReturnExpression());

        GraphNode theNode3 = theGraph.createAt(new BytecodeOpcodeAddress(20), GraphNode.BlockType.NORMAL);
        theNode3.addExpression(new ReturnExpression());

        GraphNode theNode4 = theGraph.createAt(new BytecodeOpcodeAddress(30), GraphNode.BlockType.NORMAL);
        theNode4.addExpression(new ReturnExpression());

        theNode1.addSuccessor(theNode2);
        theNode1.addSuccessor(theNode3);
        theNode1.addSuccessor(theNode4);

        theGraph.calculateReachabilityAndMarkBackEdges();

        Relooper theRelooper = new Relooper();
        Relooper.Block theBlock = theRelooper.reloop(theGraph);

        theRelooper.debugPrint(System.out, theBlock);

    }
}