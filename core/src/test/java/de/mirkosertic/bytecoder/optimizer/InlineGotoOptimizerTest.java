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
package de.mirkosertic.bytecoder.optimizer;

import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.relooper.Relooper;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.IFExpression;
import de.mirkosertic.bytecoder.ssa.IntegerValue;
import org.junit.Assert;
import org.junit.Test;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ReturnExpression;
import org.mockito.Mockito;

public class InlineGotoOptimizerTest {

    @Test
    public void testSimpleFlow() {
        Program theProgram = new Program();
        ControlFlowGraph theGraph = theProgram.getControlFlowGraph();

        RegionNode theNode = theGraph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        RegionNode theNode2 = theGraph.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);

        theNode.getExpressions().add(new GotoExpression(new BytecodeOpcodeAddress(20)));
        theNode.addSuccessor(theNode2);
        theNode2.getExpressions().add(new ReturnExpression());

        theGraph.calculateReachabilityAndMarkBackEdges();

        InlineGotoOptimizer theOptimizer = new InlineGotoOptimizer();
        theOptimizer.optimize(theGraph, null);

        Assert.assertEquals(1, theGraph.getKnownNodes().size(), 0);
        Assert.assertEquals(1, theGraph.getDominatedNodes().size(), 0);
        Assert.assertTrue(theGraph.getKnownNodes().contains(theNode));
        Assert.assertEquals(1, theNode.getExpressions().size(), 0);
        Assert.assertTrue(theNode.getExpressions().toList().get(0) instanceof ReturnExpression);

        Assert.assertEquals(0, theNode.getSuccessors().size(), 0);

        CompileOptions theOptions = Mockito.mock(CompileOptions.class);
        Relooper theRelooper = new Relooper(theOptions);
        Relooper.Block theRoot = theRelooper.reloop(theGraph);
    }

    @Test
    public void testSimpleFlowTwoLevels() {
        Program theProgram = new Program();
        ControlFlowGraph theGraph = theProgram.getControlFlowGraph();

        RegionNode theNode = theGraph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        RegionNode theNode1 = theGraph.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        RegionNode theNode2 = theGraph.createAt(new BytecodeOpcodeAddress(40), RegionNode.BlockType.NORMAL);

        theNode.getExpressions().add(new GotoExpression(new BytecodeOpcodeAddress(20)));
        theNode.addSuccessor(theNode1);

        theNode1.getExpressions().add(new GotoExpression(new BytecodeOpcodeAddress(40)));
        theNode1.addSuccessor(theNode2);

        theNode2.getExpressions().add(new ReturnExpression());

        theGraph.calculateReachabilityAndMarkBackEdges();

        InlineGotoOptimizer theOptimizer = new InlineGotoOptimizer();
        theOptimizer.optimize(theGraph, null);

        Assert.assertEquals(1, theGraph.getKnownNodes().size(), 0);
        Assert.assertEquals(1, theGraph.getDominatedNodes().size(), 0);
        Assert.assertTrue(theGraph.getKnownNodes().contains(theNode));
        Assert.assertEquals(1, theNode.getExpressions().size(), 0);
        Assert.assertTrue(theNode.getExpressions().toList().get(0) instanceof ReturnExpression);

        Assert.assertEquals(0, theNode.getSuccessors().size(), 0);

        CompileOptions theOptions = Mockito.mock(CompileOptions.class);
        Relooper theRelooper = new Relooper(theOptions);
        Relooper.Block theRoot = theRelooper.reloop(theGraph);
    }

    @Test
    public void testIf() {
        Program theProgram = new Program();
        ControlFlowGraph theGraph = theProgram.getControlFlowGraph();

        RegionNode theNode = theGraph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        RegionNode theNode1 = theGraph.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        RegionNode theNode2 = theGraph.createAt(new BytecodeOpcodeAddress(40), RegionNode.BlockType.NORMAL);
        RegionNode theNode3 = theGraph.createAt(new BytecodeOpcodeAddress(60), RegionNode.BlockType.NORMAL);

        theNode.addSuccessor(theNode1);
        theNode.addSuccessor(theNode2);
        ExpressionList theTrueClause = new ExpressionList();
        theTrueClause.add(new GotoExpression(new BytecodeOpcodeAddress(20)));
        IFExpression theIF = new IFExpression(new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(20), new IntegerValue(10), theTrueClause);
        theNode.getExpressions().add(theIF);
        theNode.getExpressions().add(new GotoExpression(new BytecodeOpcodeAddress(40)));

        theNode1.addSuccessor(theNode3);
        theNode1.getExpressions().add(new GotoExpression(new BytecodeOpcodeAddress(60)));

        theNode2.addSuccessor(theNode3);
        theNode2.getExpressions().add(new GotoExpression(new BytecodeOpcodeAddress(60)));

        theNode3.getExpressions().add(new ReturnExpression());

        theGraph.calculateReachabilityAndMarkBackEdges();

        InlineGotoOptimizer theOptimizer = new InlineGotoOptimizer();
        theOptimizer.optimize(theGraph, null);

        System.out.println(theGraph.toDOT());
    }
}