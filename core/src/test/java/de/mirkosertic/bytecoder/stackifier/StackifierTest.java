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
    public void testOnlyOneNode() {
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
    public void testSimpleSequence() {
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
    public void testSimpleSequenceWithoutGotos() {
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
    public void testIfThenElse() {
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
                "    }" + System.lineSeparator() +
                "    BLOCK $B_2_3: {" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=20}}" + System.lineSeparator() +
                "        break $B_0_3" + System.lineSeparator() +
                "    }" + System.lineSeparator() +
                "}" + System.lineSeparator() +
                "RegionNode{startAddress=BytecodeOpcodeAddress{address=30}}" + System.lineSeparator(), sw.toString());
    }
}