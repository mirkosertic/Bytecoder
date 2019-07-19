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
package de.mirkosertic.bytecoder.stackifier;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.DebugInformation;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class StackifierTest {

    @Test
    public void testOnlyOneNode() throws IrreducibleControlFlowException {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode startNode = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier();
        final StructuredControlFlow<RegionNode> graph = stackifier.stackify(g);

        final StringWriter sw = new StringWriter();
        graph.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(new PrintWriter(sw)));

        assertEquals("RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}" + System.lineSeparator(), sw.toString());
    }

    @Test
    public void testSimpleSequence() throws IrreducibleControlFlowException {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode startNode = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        startNode.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(10)));
        final RegionNode node1 = g.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        node1.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(1), new BytecodeOpcodeAddress(20)));
        final RegionNode node2 = g.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        startNode.addSuccessor(node1);
        node1.addSuccessor(node2);
        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier();

        final StructuredControlFlow<RegionNode> graph = stackifier.stackify(g);
        final StringWriter sw = new StringWriter();
        graph.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(new PrintWriter(sw)));

        assertEquals("RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}" + System.lineSeparator() +
                "RegionNode{startAddress=BytecodeOpcodeAddress{address=10}}" + System.lineSeparator() +
                "RegionNode{startAddress=BytecodeOpcodeAddress{address=20}}" + System.lineSeparator(), sw.toString());
    }

    @Test
    public void testSimpleSequenceWithoutGotos() throws IrreducibleControlFlowException {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode startNode = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        final RegionNode node1 = g.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        final RegionNode node2 = g.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        startNode.addSuccessor(node1);
        node1.addSuccessor(node2);
        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier();

        final StructuredControlFlow<RegionNode> graph = stackifier.stackify(g);
        final StringWriter sw = new StringWriter();
        graph.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(new PrintWriter(sw)));

        assertEquals("RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}" + System.lineSeparator() +
                "RegionNode{startAddress=BytecodeOpcodeAddress{address=10}}" + System.lineSeparator() +
                "RegionNode{startAddress=BytecodeOpcodeAddress{address=20}}" + System.lineSeparator(), sw.toString());
    }

    @Test
    public void testIfThenElse() throws IrreducibleControlFlowException {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode startNode = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        final RegionNode a = g.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        a.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(30)));
        final RegionNode b = g.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        b.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(2), new BytecodeOpcodeAddress(30)));
        final RegionNode c = g.createAt(new BytecodeOpcodeAddress(30), RegionNode.BlockType.NORMAL);
        startNode.addSuccessor(a);
        startNode.addSuccessor(b);
        a.addSuccessor(c);
        b.addSuccessor(c);

        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier();

        final StructuredControlFlow<RegionNode> graph = stackifier.stackify(g);
        final StringWriter sw = new StringWriter();
        graph.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(new PrintWriter(sw)));

        assertEquals("BLOCK $B_0_3: {" + System.lineSeparator() +
                "    BLOCK $B_0_2: {" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=10}}" + System.lineSeparator() +
                "        break $B_0_3" + System.lineSeparator() +
                "    } ; Closing block $B_0_2" + System.lineSeparator() +
                "    BLOCK $B_2_3: {" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=20}}" + System.lineSeparator() +
                "        break $B_0_3" + System.lineSeparator() +
                "    } ; Closing block $B_2_3" + System.lineSeparator() +
                "} ; Closing block $B_0_3" + System.lineSeparator() +
                "RegionNode{startAddress=BytecodeOpcodeAddress{address=30}}" + System.lineSeparator(), sw.toString());
    }

    @Test
    public void testSimpleLoop() throws IrreducibleControlFlowException {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode startNode = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        startNode.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(10)));
        final RegionNode node1 = g.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        node1.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(1), new BytecodeOpcodeAddress(0)));
        startNode.addSuccessor(node1);
        node1.addSuccessor(startNode);
        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier();

        final StructuredControlFlow<RegionNode> graph = stackifier.stackify(g);
        final StringWriter sw = new StringWriter();
        graph.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(new PrintWriter(sw)));

        assertEquals("LOOP $L_0_1: {" + System.lineSeparator() +
                "    RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}" + System.lineSeparator() +
                "    RegionNode{startAddress=BytecodeOpcodeAddress{address=10}}" + System.lineSeparator() +
                "    continue $L_0_1" + System.lineSeparator() +
                "} ; Closing block $L_0_1" + System.lineSeparator(), sw.toString());
    }

    @Test
    public void testComplexExampleWithLoopAsLastElement() throws IrreducibleControlFlowException {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);

        final RegionNode startNode = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        final RegionNode node10 = g.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        final RegionNode node23 = g.createAt(new BytecodeOpcodeAddress(23), RegionNode.BlockType.NORMAL);
        final RegionNode node25 = g.createAt(new BytecodeOpcodeAddress(25), RegionNode.BlockType.NORMAL);
        final RegionNode node62 = g.createAt(new BytecodeOpcodeAddress(62), RegionNode.BlockType.NORMAL);
        final RegionNode node65 = g.createAt(new BytecodeOpcodeAddress(65), RegionNode.BlockType.NORMAL);
        final RegionNode node74 = g.createAt(new BytecodeOpcodeAddress(74), RegionNode.BlockType.NORMAL);
        final RegionNode node81 = g.createAt(new BytecodeOpcodeAddress(81), RegionNode.BlockType.NORMAL);
        final RegionNode node109 = g.createAt(new BytecodeOpcodeAddress(109), RegionNode.BlockType.NORMAL);
        final RegionNode node103 = g.createAt(new BytecodeOpcodeAddress(103), RegionNode.BlockType.NORMAL);
        final RegionNode node100 = g.createAt(new BytecodeOpcodeAddress(100), RegionNode.BlockType.NORMAL);
        startNode.addSuccessor(node10);
        startNode.addSuccessor(node74);
        node10.addSuccessor(node23);
        node10.addSuccessor(node25);
        node25.addSuccessor(node62);
        node25.addSuccessor(node65);
        node65.addSuccessor(node74);
        node74.addSuccessor(node81);
        node74.addSuccessor(node109);
        node81.addSuccessor(node103);
        node81.addSuccessor(node100);
        node100.addSuccessor(node74);
        node100.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(100), new BytecodeOpcodeAddress(74)));

        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier();

        final StructuredControlFlow<RegionNode> graph = stackifier.stackify(g);
        final StringWriter sw = new StringWriter();
        graph.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(new PrintWriter(sw)));

        /*assertEquals("BLOCK $B_0_6: {" + System.lineSeparator() +
                "    RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}" + System.lineSeparator() +
                "    BLOCK $B_1_3: {" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=10}}" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=23}}" + System.lineSeparator() +
                "    } ; Closing block $B_1_3" + System.lineSeparator() +
                "    BLOCK $B_3_5: {" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=25}}" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=62}}" + System.lineSeparator() +
                "    } ; Closing block $B_3_5" + System.lineSeparator() +
                "    BLOCK $B_5_6: {" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=65}}" + System.lineSeparator() +
                "    } ; Closing block $B_5_6" + System.lineSeparator() +
                "} ; Closing block $B_0_6" + System.lineSeparator() +
                "LOOP $L_6_10: {" + System.lineSeparator() +
                "    BLOCK $B_6_10: {" + System.lineSeparator() +
                "        BLOCK $B_6_9: {" + System.lineSeparator() +
                "            BLOCK $B_6_8: {" + System.lineSeparator() +
                "                RegionNode{startAddress=BytecodeOpcodeAddress{address=74}}" + System.lineSeparator() +
                "                RegionNode{startAddress=BytecodeOpcodeAddress{address=81}}" + System.lineSeparator() +
                "            } ; Closing block $B_6_8" + System.lineSeparator() +
                "            RegionNode{startAddress=BytecodeOpcodeAddress{address=109}}" + System.lineSeparator() +
                "        } ; Closing block $B_6_9" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=103}}" + System.lineSeparator() +
                "    } ; Closing block $B_6_10" + System.lineSeparator() +
                "    RegionNode{startAddress=BytecodeOpcodeAddress{address=100}}" + System.lineSeparator() +
                "    continue $L_6_10" + System.lineSeparator() +
                "} ; Closing block $L_6_10" + System.lineSeparator(), sw.toString());
         */
    }
}