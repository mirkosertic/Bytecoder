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

import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ReturnExpression;
import org.junit.Test;
import org.mockito.Mockito;

public class RelooperTest {

    @Test
    public void testDirectFlow() {
        Program theProgram = new Program();
        ControlFlowGraph theGraph = new ControlFlowGraph(theProgram);

        RegionNode theNode1 = theGraph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        theNode1.getExpressions().add(new GotoExpression(new BytecodeOpcodeAddress(10)));
        RegionNode theNode2 = theGraph.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        theNode2.getExpressions().add(new GotoExpression(new BytecodeOpcodeAddress(20)));
        RegionNode theNode3 = theGraph.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        theNode3.getExpressions().add(new ReturnExpression());

        theNode1.addSuccessor(theNode2);
        theNode2.addSuccessor(theNode3);

        theGraph.calculateReachabilityAndMarkBackEdges();

        CompileOptions theOptions = Mockito.mock(CompileOptions.class);
        Relooper theRelooper = new Relooper(theOptions);
        Relooper.Block theBlock = theRelooper.reloop(theGraph);

        theRelooper.debugPrint(System.out, theBlock);
    }

    @Test
    public void testEndlessLoop() {
        Program theProgram = new Program();
        ControlFlowGraph theGraph = new ControlFlowGraph(theProgram);

        RegionNode theNode1 = theGraph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        theNode1.getExpressions().add(new GotoExpression(new BytecodeOpcodeAddress(10)));
        RegionNode theNode2 = theGraph.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        theNode2.getExpressions().add(new GotoExpression(new BytecodeOpcodeAddress(20)));
        RegionNode theNode3 = theGraph.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        theNode3.getExpressions().add(new GotoExpression(BytecodeOpcodeAddress.START_AT_ZERO));

        theNode1.addSuccessor(theNode2);
        theNode2.addSuccessor(theNode3);
        theNode3.addSuccessor(theNode1);

        theGraph.calculateReachabilityAndMarkBackEdges();

        CompileOptions theOptions = Mockito.mock(CompileOptions.class);
        Relooper theRelooper = new Relooper(theOptions);
        Relooper.Block theBlock = theRelooper.reloop(theGraph);

        theRelooper.debugPrint(System.out, theBlock);
    }

    @Test
    public void testIf() {
        Program theProgram = new Program();
        ControlFlowGraph theGraph = new ControlFlowGraph(theProgram);

        RegionNode theNode1 = theGraph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        theNode1.getExpressions().add(new GotoExpression(new BytecodeOpcodeAddress(10)));

        RegionNode theNode2 = theGraph.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        theNode2.getExpressions().add(new GotoExpression(new BytecodeOpcodeAddress(20)));

        RegionNode theNode3 = theGraph.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        theNode3.getExpressions().add(new ReturnExpression());

        theNode1.addSuccessor(theNode2);
        theNode1.addSuccessor(theNode3);
        theNode2.addSuccessor(theNode3);

        theGraph.calculateReachabilityAndMarkBackEdges();

        CompileOptions theOptions = Mockito.mock(CompileOptions.class);
        Relooper theRelooper = new Relooper(theOptions);
        Relooper.Block theBlock = theRelooper.reloop(theGraph);

        theRelooper.debugPrint(System.out, theBlock);
    }

    @Test
    public void testSwitchCase() {
        Program theProgram = new Program();
        ControlFlowGraph theGraph = new ControlFlowGraph(theProgram);

        RegionNode theNode1 = theGraph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);

        RegionNode theNode2 = theGraph.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        theNode2.getExpressions().add(new ReturnExpression());

        RegionNode theNode3 = theGraph.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        theNode3.getExpressions().add(new ReturnExpression());

        RegionNode theNode4 = theGraph.createAt(new BytecodeOpcodeAddress(30), RegionNode.BlockType.NORMAL);
        theNode4.getExpressions().add(new ReturnExpression());

        theNode1.addSuccessor(theNode2);
        theNode1.addSuccessor(theNode3);
        theNode1.addSuccessor(theNode4);

        theGraph.calculateReachabilityAndMarkBackEdges();

        CompileOptions theOptions = Mockito.mock(CompileOptions.class);
        Relooper theRelooper = new Relooper(theOptions);
        Relooper.Block theBlock = theRelooper.reloop(theGraph);

        theRelooper.debugPrint(System.out, theBlock);
    }

    @Test
    public void testNestedIfs() {

/**
        digraph CFG {
            N0 [shape=none, margin=0, label=<<table><tr><td> V 0</td><td> V 1</td><td> V 2</td><td> V 3</td></tr><tr><td>X</td><td>X</td><td>X</td><td>X</td></tr><tr><td colspan="4"> Node at 0</td></tr><tr><td>X</td><td>X</td><td>X</td><td>X</td></tr><tr><td>REFERENCE</td><td>FLOAT</td><td>FLOAT</td><td>FLOAT</td></tr></table>>];
            N0 ->    N39;
            N0 ->    N8;
            N8 [shape=none, margin=0, label=<<table><tr><td> V 0</td><td> V 1</td><td> V 2</td><td> V 3</td></tr><tr><td>X</td><td>X</td><td>X</td><td>X</td></tr><tr><td colspan="4"> Node at 8</td></tr><tr><td>X</td><td>X</td><td>X</td><td>X</td></tr><tr><td>REFERENCE</td><td>FLOAT</td><td>FLOAT</td><td>FLOAT</td></tr></table>>];
            N8 ->    N19;
            N8 ->    N39;
            N19 [shape=none, margin=0, label=<<table><tr><td> V 0</td><td> V 1</td><td> V 2</td></tr><tr><td>X</td><td>X</td><td>X</td></tr><tr><td colspan="3"> Node at 19</td></tr><tr><td>X</td><td>X</td><td>X</td></tr><tr><td>REFERENCE</td><td>FLOAT</td><td>FLOAT</td></tr></table>>];
            N19 ->    N39;
            N39 [shape=none, margin=0, label=<<table><tr><td colspan="0"> Node at 39</td></tr></table>>];
        }*/

        Program theProgram = new Program();
        ControlFlowGraph theGraph = new ControlFlowGraph(theProgram);

        RegionNode theNode0 = theGraph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        RegionNode theNode8 = theGraph.createAt(new BytecodeOpcodeAddress(8), RegionNode.BlockType.NORMAL);
        RegionNode theNode19 = theGraph.createAt(new BytecodeOpcodeAddress(19), RegionNode.BlockType.NORMAL);
        RegionNode theNode39 = theGraph.createAt(new BytecodeOpcodeAddress(39), RegionNode.BlockType.NORMAL);

        theNode0.addSuccessor(theNode8);
        theNode0.addSuccessor(theNode39);

        theNode8.addSuccessor(theNode19);
        theNode8.addSuccessor(theNode39);

        theNode19.addSuccessor(theNode39);

        theGraph.calculateReachabilityAndMarkBackEdges();

        CompileOptions theOptions = Mockito.mock(CompileOptions.class);
        Relooper theRelooper = new Relooper(theOptions);
        Relooper.Block theBlock = theRelooper.reloop(theGraph);

        theRelooper.debugPrint(System.out, theBlock);
    }
}