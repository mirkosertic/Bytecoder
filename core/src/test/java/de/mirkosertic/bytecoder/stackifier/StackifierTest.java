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

import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;

import de.mirkosertic.bytecoder.ssa.ControlFlowEdgeType;
import org.junit.Test;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.DebugInformation;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.IFExpression;
import de.mirkosertic.bytecoder.ssa.IntegerValue;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.RegionNode;

public class StackifierTest {

    @Test
    public void testOnlyOneNode() throws HeadToHeadControlFlowException {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode startNode = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier(g);
        final StringWriter sw = new StringWriter();
        stackifier.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(stackifier, new PrintWriter(sw)));

        assertEquals("RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}" + System.lineSeparator(), sw.toString());
    }

    @Test
    public void testSimpleSequence() throws HeadToHeadControlFlowException {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode startNode = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        startNode.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(10)));
        final RegionNode node1 = g.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        node1.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(1), new BytecodeOpcodeAddress(20)));
        final RegionNode node2 = g.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        startNode.addEdgeTo(ControlFlowEdgeType.forward, node1);
        node1.addEdgeTo(ControlFlowEdgeType.forward, node2);
        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier(g);
        final StringWriter sw = new StringWriter();
        stackifier.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(stackifier, new PrintWriter(sw)));

        assertEquals("RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}" + System.lineSeparator() +
                "RegionNode{startAddress=BytecodeOpcodeAddress{address=10}}" + System.lineSeparator() +
                "RegionNode{startAddress=BytecodeOpcodeAddress{address=20}}" + System.lineSeparator(), sw.toString());
    }

    @Test
    public void testSimpleSequenceWithoutGotos() throws HeadToHeadControlFlowException {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode startNode = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        final RegionNode node1 = g.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        final RegionNode node2 = g.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        startNode.addEdgeTo(ControlFlowEdgeType.forward, node1);
        node1.addEdgeTo(ControlFlowEdgeType.forward, node2);
        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier(g);
        final StringWriter sw = new StringWriter();
        stackifier.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(stackifier, new PrintWriter(sw)));

        assertEquals("RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}" + System.lineSeparator() +
                "RegionNode{startAddress=BytecodeOpcodeAddress{address=10}}" + System.lineSeparator() +
                "RegionNode{startAddress=BytecodeOpcodeAddress{address=20}}" + System.lineSeparator(), sw.toString());
    }

    @Test
    public void testIfThenElse() throws HeadToHeadControlFlowException {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode startNode = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        startNode.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(10)));
        startNode.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(1), new BytecodeOpcodeAddress(20)));

        final RegionNode a = g.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        a.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(30)));
        final RegionNode b = g.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        b.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(2), new BytecodeOpcodeAddress(30)));
        final RegionNode c = g.createAt(new BytecodeOpcodeAddress(30), RegionNode.BlockType.NORMAL);
        startNode.addEdgeTo(ControlFlowEdgeType.forward, a);
        startNode.addEdgeTo(ControlFlowEdgeType.forward, b);
        a.addEdgeTo(ControlFlowEdgeType.forward, c);
        b.addEdgeTo(ControlFlowEdgeType.forward, c);

        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier(g);
        final StringWriter sw = new StringWriter();
        stackifier.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(stackifier, new PrintWriter(sw)));

        assertEquals("BLOCK $B_0_3: {" + System.lineSeparator() +
                "    BLOCK $B_0_2: {" + System.lineSeparator() +
                "        BLOCK $B_0_1: {" + System.lineSeparator() +
                "            RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}" + System.lineSeparator() +
                "            break $B_0_1" + System.lineSeparator() +
                "            break $B_0_2" + System.lineSeparator() +
                "        } ; Closing block $B_0_1" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=10}}" + System.lineSeparator() +
                "        break $B_0_3" + System.lineSeparator() +
                "    } ; Closing block $B_0_2" + System.lineSeparator() +
                "    RegionNode{startAddress=BytecodeOpcodeAddress{address=20}}" + System.lineSeparator() +
                "    break $B_0_3" + System.lineSeparator() +
                "} ; Closing block $B_0_3" + System.lineSeparator() +
                "RegionNode{startAddress=BytecodeOpcodeAddress{address=30}}" + System.lineSeparator(), sw.toString());
    }

    @Test
    public void testSimpleLoop() throws HeadToHeadControlFlowException {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode startNode = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        startNode.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(10)));
        final RegionNode node1 = g.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        node1.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(1), new BytecodeOpcodeAddress(0)));
        startNode.addEdgeTo(ControlFlowEdgeType.forward, node1);
        node1.addEdgeTo(ControlFlowEdgeType.forward, startNode);
        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier(g);
        final StringWriter sw = new StringWriter();

        stackifier.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(stackifier, new PrintWriter(sw)));

        assertEquals("LOOP $L_0_1: {" + System.lineSeparator() +
                "    RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}" + System.lineSeparator() +
                "    RegionNode{startAddress=BytecodeOpcodeAddress{address=10}}" + System.lineSeparator() +
                "    continue $L_0_1" + System.lineSeparator() +
                "} ; Closing block $L_0_1" + System.lineSeparator(), sw.toString());
    }

    @Test
    public void testSimpleLoopWithSuccessor1() throws HeadToHeadControlFlowException {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode startNode = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        startNode.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(20)));
        final RegionNode node1 = g.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        node1.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(1), new BytecodeOpcodeAddress(0)));
        final RegionNode node2 = g.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);

        startNode.addEdgeTo(ControlFlowEdgeType.forward, node1);
        startNode.addEdgeTo(ControlFlowEdgeType.forward, node2);
        node1.addEdgeTo(ControlFlowEdgeType.forward, startNode);
        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier(g);
        final StringWriter sw = new StringWriter();
        stackifier.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(stackifier, new PrintWriter(sw)));

        assertEquals("LOOP $L_0_2: {" + System.lineSeparator() +
                "    BLOCK $B_0_2: {" + System.lineSeparator() +
                "        BLOCK $B_0_1: {" + System.lineSeparator() +
                "            RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}" + System.lineSeparator() +
                "            break $B_0_2" + System.lineSeparator() +
                "        } ; Closing block $B_0_1" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=10}}" + System.lineSeparator() +
                "        continue $L_0_2" + System.lineSeparator() +
                "    } ; Closing block $B_0_2" + System.lineSeparator() +
                "    RegionNode{startAddress=BytecodeOpcodeAddress{address=20}}" + System.lineSeparator() +
                "} ; Closing block $L_0_2" + System.lineSeparator(), sw.toString());
    }

    @Test
    public void testSimpleLoopWithSuccessor2() throws HeadToHeadControlFlowException {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode startNode = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        startNode.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(0)));
        final RegionNode node1 = g.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        node1.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(1), new BytecodeOpcodeAddress(20)));
        final RegionNode node2 = g.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);

        startNode.addEdgeTo(ControlFlowEdgeType.forward, node1);
        startNode.addEdgeTo(ControlFlowEdgeType.forward, node2);
        node1.addEdgeTo(ControlFlowEdgeType.forward, startNode);
        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier(g);
        final StringWriter sw = new StringWriter();
        stackifier.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(stackifier, new PrintWriter(sw)));

        assertEquals("LOOP $L_0_2: {" + System.lineSeparator() +
                "    BLOCK $B_0_2: {" + System.lineSeparator() +
                "        BLOCK $B_0_1: {" + System.lineSeparator() +
                "            RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}" + System.lineSeparator() +
                "            continue $L_0_2" + System.lineSeparator() +
                "        } ; Closing block $B_0_1" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=10}}" + System.lineSeparator() +
                "        break $B_0_2" + System.lineSeparator() +
                "    } ; Closing block $B_0_2" + System.lineSeparator() +
                "    RegionNode{startAddress=BytecodeOpcodeAddress{address=20}}" + System.lineSeparator() +
                "} ; Closing block $L_0_2" + System.lineSeparator(), sw.toString());
    }

    @Test
    public void testSimpleLoopDoubleExit() throws HeadToHeadControlFlowException {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode startNode = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        startNode.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(20)));
        final RegionNode node1 = g.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        node1.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(1), new BytecodeOpcodeAddress(20)));
        node1.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(1), new BytecodeOpcodeAddress(0)));
        final RegionNode node2 = g.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);

        startNode.addEdgeTo(ControlFlowEdgeType.forward, node1);
        startNode.addEdgeTo(ControlFlowEdgeType.forward, node2);
        node1.addEdgeTo(ControlFlowEdgeType.forward, startNode);
        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier(g);
        final StringWriter sw = new StringWriter();
        stackifier.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(stackifier, new PrintWriter(sw)));

        assertEquals("LOOP $L_0_2: {" + System.lineSeparator() +
                "    BLOCK $B_0_2: {" + System.lineSeparator() +
                "        BLOCK $B_0_1: {" + System.lineSeparator() +
                "            RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}" + System.lineSeparator() +
                "            break $B_0_2" + System.lineSeparator() +
                "        } ; Closing block $B_0_1" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=10}}" + System.lineSeparator() +
                "        break $B_0_2" + System.lineSeparator() +
                "        continue $L_0_2" + System.lineSeparator() +
                "    } ; Closing block $B_0_2" + System.lineSeparator() +
                "    RegionNode{startAddress=BytecodeOpcodeAddress{address=20}}" + System.lineSeparator() +
                "} ; Closing block $L_0_2" + System.lineSeparator(), sw.toString());
    }

    @Test
    public void testSimpleLoopDoubleContinue() throws HeadToHeadControlFlowException {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode startNode = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        startNode.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(0)));
        final RegionNode node1 = g.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        node1.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(1), new BytecodeOpcodeAddress(20)));
        node1.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(1), new BytecodeOpcodeAddress(0)));
        final RegionNode node2 = g.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);

        startNode.addEdgeTo(ControlFlowEdgeType.forward, node1);
        startNode.addEdgeTo(ControlFlowEdgeType.forward, node2);
        node1.addEdgeTo(ControlFlowEdgeType.forward, startNode);
        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier(g);
        final StringWriter sw = new StringWriter();
        stackifier.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(stackifier, new PrintWriter(sw)));

        assertEquals("LOOP $L_0_2: {" + System.lineSeparator() +
                "    BLOCK $B_0_2: {" + System.lineSeparator() +
                "        BLOCK $B_0_1: {" + System.lineSeparator() +
                "            RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}" + System.lineSeparator() +
                "            continue $L_0_2" + System.lineSeparator() +
                "        } ; Closing block $B_0_1" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=10}}" + System.lineSeparator() +
                "        break $B_0_2" + System.lineSeparator() +
                "        continue $L_0_2" + System.lineSeparator() +
                "    } ; Closing block $B_0_2" + System.lineSeparator() +
                "    RegionNode{startAddress=BytecodeOpcodeAddress{address=20}}" + System.lineSeparator() +
                "} ; Closing block $L_0_2" + System.lineSeparator(), sw.toString());
    }

    @Test
    public void testMoreComplexExample() throws HeadToHeadControlFlowException {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);

        final RegionNode startNode = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        final RegionNode node10 = g.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        final RegionNode node16 = g.createAt(new BytecodeOpcodeAddress(16), RegionNode.BlockType.NORMAL);
        final RegionNode node31 = g.createAt(new BytecodeOpcodeAddress(31), RegionNode.BlockType.NORMAL);
        final RegionNode node35= g.createAt(new BytecodeOpcodeAddress(35), RegionNode.BlockType.NORMAL);
        final RegionNode node41= g.createAt(new BytecodeOpcodeAddress(41), RegionNode.BlockType.NORMAL);
        final RegionNode node51= g.createAt(new BytecodeOpcodeAddress(51), RegionNode.BlockType.NORMAL);
        final RegionNode node57= g.createAt(new BytecodeOpcodeAddress(57), RegionNode.BlockType.NORMAL);
        final RegionNode node72= g.createAt(new BytecodeOpcodeAddress(72), RegionNode.BlockType.NORMAL);
        final RegionNode node76= g.createAt(new BytecodeOpcodeAddress(76), RegionNode.BlockType.NORMAL);
        final RegionNode node82= g.createAt(new BytecodeOpcodeAddress(82), RegionNode.BlockType.NORMAL);
        final RegionNode node95= g.createAt(new BytecodeOpcodeAddress(95), RegionNode.BlockType.NORMAL);
        final RegionNode node99= g.createAt(new BytecodeOpcodeAddress(99), RegionNode.BlockType.NORMAL);
        final RegionNode node112= g.createAt(new BytecodeOpcodeAddress(112), RegionNode.BlockType.NORMAL);
        final RegionNode node116= g.createAt(new BytecodeOpcodeAddress(116), RegionNode.BlockType.NORMAL);

        startNode.addEdgeTo(ControlFlowEdgeType.forward, node10);
        startNode.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(10)));

        node10.addEdgeTo(ControlFlowEdgeType.forward, node16);
        node10.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(10), new BytecodeOpcodeAddress(16)));
        node10.addEdgeTo(ControlFlowEdgeType.forward, node41);
        node10.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(11), new BytecodeOpcodeAddress(41)));

        node16.addEdgeTo(ControlFlowEdgeType.forward, node35);
        node16.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(16), new BytecodeOpcodeAddress(35)));
        node16.addEdgeTo(ControlFlowEdgeType.forward, node31);
        node16.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(17), new BytecodeOpcodeAddress(31)));

        node35.addEdgeTo(ControlFlowEdgeType.forward, node10);
        node35.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(35), new BytecodeOpcodeAddress(10)));

        node41.addEdgeTo(ControlFlowEdgeType.forward, node51);
        node41.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(41), new BytecodeOpcodeAddress(51)));

        node51.addEdgeTo(ControlFlowEdgeType.forward, node57);
        node51.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(51), new BytecodeOpcodeAddress(57)));
        node51.addEdgeTo(ControlFlowEdgeType.forward, node82);
        node51.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(52), new BytecodeOpcodeAddress(82)));

        node57.addEdgeTo(ControlFlowEdgeType.forward, node72);
        node57.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(57), new BytecodeOpcodeAddress(72)));
        node57.addEdgeTo(ControlFlowEdgeType.forward, node76);
        node57.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(58), new BytecodeOpcodeAddress(76)));

        node76.addEdgeTo(ControlFlowEdgeType.forward, node51);
        node76.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(76), new BytecodeOpcodeAddress(51)));

        node82.addEdgeTo(ControlFlowEdgeType.forward, node95);
        node82.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(82), new BytecodeOpcodeAddress(95)));
        node82.addEdgeTo(ControlFlowEdgeType.forward, node99);
        node82.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(83), new BytecodeOpcodeAddress(99)));

        node99.addEdgeTo(ControlFlowEdgeType.forward, node112);
        node99.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(99), new BytecodeOpcodeAddress(112)));
        node99.addEdgeTo(ControlFlowEdgeType.forward, node116);
        node99.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(100), new BytecodeOpcodeAddress(116)));

        g.calculateReachabilityAndMarkBackEdges();
        final Stackifier stackifier = new Stackifier(g);
        final StringWriter sw = new StringWriter();
        stackifier.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(stackifier, new PrintWriter(sw)));

        assertEquals("RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}" + System.lineSeparator() +
                "LOOP $L_1_14: {" + System.lineSeparator() +
                "    BLOCK $B_1_5: {" + System.lineSeparator() +
                "        BLOCK $B_1_2: {" + System.lineSeparator() +
                "            RegionNode{startAddress=BytecodeOpcodeAddress{address=10}}" + System.lineSeparator() +
                "            break $B_1_2" + System.lineSeparator() +
                "            break $B_1_5" + System.lineSeparator() +
                "        } ; Closing block $B_1_2" + System.lineSeparator() +
                "        BLOCK $B_2_4: {" + System.lineSeparator() +
                "            BLOCK $B_2_3: {" + System.lineSeparator() +
                "                RegionNode{startAddress=BytecodeOpcodeAddress{address=16}}" + System.lineSeparator() +
                "                break $B_2_4" + System.lineSeparator() +
                "                break $B_2_3" + System.lineSeparator() +
                "            } ; Closing block $B_2_3" + System.lineSeparator() +
                "            RegionNode{startAddress=BytecodeOpcodeAddress{address=31}}" + System.lineSeparator() +
                "        } ; Closing block $B_2_4" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=35}}" + System.lineSeparator() +
                "        continue $L_1_14" + System.lineSeparator() +
                "    } ; Closing block $B_1_5" + System.lineSeparator() +
                "    RegionNode{startAddress=BytecodeOpcodeAddress{address=41}}" + System.lineSeparator() +
                "    LOOP $L_6_14: {" + System.lineSeparator() +
                "        BLOCK $B_6_10: {" + System.lineSeparator() +
                "            BLOCK $B_6_7: {" + System.lineSeparator() +
                "                RegionNode{startAddress=BytecodeOpcodeAddress{address=51}}" + System.lineSeparator() +
                "                break $B_6_7" + System.lineSeparator() +
                "                break $B_6_10" + System.lineSeparator() +
                "            } ; Closing block $B_6_7" + System.lineSeparator() +
                "            BLOCK $B_7_9: {" + System.lineSeparator() +
                "                BLOCK $B_7_8: {" + System.lineSeparator() +
                "                    RegionNode{startAddress=BytecodeOpcodeAddress{address=57}}" + System.lineSeparator() +
                "                    break $B_7_8" + System.lineSeparator() +
                "                    break $B_7_9" + System.lineSeparator() +
                "                } ; Closing block $B_7_8" + System.lineSeparator() +
                "                RegionNode{startAddress=BytecodeOpcodeAddress{address=72}}" + System.lineSeparator() +
                "            } ; Closing block $B_7_9" + System.lineSeparator() +
                "            RegionNode{startAddress=BytecodeOpcodeAddress{address=76}}" + System.lineSeparator() +
                "            continue $L_6_14" + System.lineSeparator() +
                "        } ; Closing block $B_6_10" + System.lineSeparator() +
                "        BLOCK $B_10_12: {" + System.lineSeparator() +
                "            BLOCK $B_10_11: {" + System.lineSeparator() +
                "                RegionNode{startAddress=BytecodeOpcodeAddress{address=82}}" + System.lineSeparator() +
                "                break $B_10_11" + System.lineSeparator() +
                "                break $B_10_12" + System.lineSeparator() +
                "            } ; Closing block $B_10_11" + System.lineSeparator() +
                "            RegionNode{startAddress=BytecodeOpcodeAddress{address=95}}" + System.lineSeparator() +
                "        } ; Closing block $B_10_12" + System.lineSeparator() +
                "        BLOCK $B_12_14: {" + System.lineSeparator() +
                "            BLOCK $B_12_13: {" + System.lineSeparator() +
                "                RegionNode{startAddress=BytecodeOpcodeAddress{address=99}}" + System.lineSeparator() +
                "                break $B_12_13" + System.lineSeparator() +
                "                break $B_12_14" + System.lineSeparator() +
                "            } ; Closing block $B_12_13" + System.lineSeparator() +
                "            RegionNode{startAddress=BytecodeOpcodeAddress{address=112}}" + System.lineSeparator() +
                "        } ; Closing block $B_12_14" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=116}}" + System.lineSeparator() +
                "    } ; Closing block $L_6_14" + System.lineSeparator() +
                "} ; Closing block $L_1_14" + System.lineSeparator(), sw.toString());
    }

    @Test
    public void testComplexExampleWithLoopAsLastElement() throws HeadToHeadControlFlowException {
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
        startNode.addEdgeTo(ControlFlowEdgeType.forward, node10);
        startNode.addEdgeTo(ControlFlowEdgeType.forward, node74);
        node10.addEdgeTo(ControlFlowEdgeType.forward, node23);
        node10.addEdgeTo(ControlFlowEdgeType.forward, node25);
        node25.addEdgeTo(ControlFlowEdgeType.forward, node62);
        node25.addEdgeTo(ControlFlowEdgeType.forward, node65);
        node65.addEdgeTo(ControlFlowEdgeType.forward, node74);
        node74.addEdgeTo(ControlFlowEdgeType.forward, node81);
        node74.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(74), new BytecodeOpcodeAddress(109)));
        node74.addEdgeTo(ControlFlowEdgeType.forward, node109);
        node81.addEdgeTo(ControlFlowEdgeType.forward, node103);
        node81.addEdgeTo(ControlFlowEdgeType.forward, node100);
        node100.addEdgeTo(ControlFlowEdgeType.forward, node74);
        node100.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(100), new BytecodeOpcodeAddress(74)));

        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier(g);
        final StringWriter sw = new StringWriter();
        stackifier.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(stackifier, new PrintWriter(sw)));

        assertEquals("BLOCK $B_0_6: {" + System.lineSeparator() +
                "    BLOCK $B_0_1: {" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}" + System.lineSeparator() +
                "    } ; Closing block $B_0_1" + System.lineSeparator() +
                "    BLOCK $B_1_3: {" + System.lineSeparator() +
                "        BLOCK $B_1_2: {" + System.lineSeparator() +
                "            RegionNode{startAddress=BytecodeOpcodeAddress{address=10}}" + System.lineSeparator() +
                "        } ; Closing block $B_1_2" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=23}}" + System.lineSeparator() +
                "    } ; Closing block $B_1_3" + System.lineSeparator() +
                "    BLOCK $B_3_5: {" + System.lineSeparator() +
                "        BLOCK $B_3_4: {" + System.lineSeparator() +
                "            RegionNode{startAddress=BytecodeOpcodeAddress{address=25}}" + System.lineSeparator() +
                "        } ; Closing block $B_3_4" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=62}}" + System.lineSeparator() +
                "    } ; Closing block $B_3_5" + System.lineSeparator() +
                "    RegionNode{startAddress=BytecodeOpcodeAddress{address=65}}" + System.lineSeparator() +
                "} ; Closing block $B_0_6" + System.lineSeparator() +
                "LOOP $L_6_10: {" + System.lineSeparator() +
                "    BLOCK $B_6_10: {" + System.lineSeparator() +
                "        BLOCK $B_6_7: {" + System.lineSeparator() +
                "            RegionNode{startAddress=BytecodeOpcodeAddress{address=74}}" + System.lineSeparator() +
                "            break $B_6_10" + System.lineSeparator() +
                "        } ; Closing block $B_6_7" + System.lineSeparator() +
                "        BLOCK $B_7_9: {" + System.lineSeparator() +
                "            BLOCK $B_7_8: {" + System.lineSeparator() +
                "                RegionNode{startAddress=BytecodeOpcodeAddress{address=81}}" + System.lineSeparator() +
                "            } ; Closing block $B_7_8" + System.lineSeparator() +
                "            RegionNode{startAddress=BytecodeOpcodeAddress{address=100}}" + System.lineSeparator() +
                "            continue $L_6_10" + System.lineSeparator() +
                "        } ; Closing block $B_7_9" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=103}}" + System.lineSeparator() +
                "    } ; Closing block $B_6_10" + System.lineSeparator() +
                "    RegionNode{startAddress=BytecodeOpcodeAddress{address=109}}" + System.lineSeparator() +
                "} ; Closing block $L_6_10" + System.lineSeparator(), sw.toString());
    }

    @Test
    public void testAnotherComplexExample() throws HeadToHeadControlFlowException {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);

        final RegionNode node0 = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        node0.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(2)));

        final RegionNode node1 = g.createAt(new BytecodeOpcodeAddress(2), RegionNode.BlockType.NORMAL);
        node1.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(2), new BytecodeOpcodeAddress(13)));
        node1.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(3), new BytecodeOpcodeAddress(11)));

        final RegionNode node2 = g.createAt(new BytecodeOpcodeAddress(11), RegionNode.BlockType.NORMAL);

        final RegionNode node3 = g.createAt(new BytecodeOpcodeAddress(13), RegionNode.BlockType.NORMAL);
        node3.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(13), new BytecodeOpcodeAddress(2)));

        node0.addEdgeTo(ControlFlowEdgeType.forward, node1);
        node1.addEdgeTo(ControlFlowEdgeType.forward, node2);
        node1.addEdgeTo(ControlFlowEdgeType.forward, node3);
        node3.addEdgeTo(ControlFlowEdgeType.forward, node1);

        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier(g);
        final StringWriter sw = new StringWriter();
        stackifier.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(stackifier, new PrintWriter(sw)));

        assertEquals("RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}" + System.lineSeparator() +
                "LOOP $L_1_3: {" + System.lineSeparator() +
                "    BLOCK $B_1_3: {" + System.lineSeparator() +
                "        BLOCK $B_1_2: {" + System.lineSeparator() +
                "            RegionNode{startAddress=BytecodeOpcodeAddress{address=2}}" + System.lineSeparator() +
                "            break $B_1_3" + System.lineSeparator() +
                "            break $B_1_2" + System.lineSeparator() +
                "        } ; Closing block $B_1_2" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=11}}" + System.lineSeparator() +
                "    } ; Closing block $B_1_3" + System.lineSeparator() +
                "    RegionNode{startAddress=BytecodeOpcodeAddress{address=13}}" + System.lineSeparator() +
                "    continue $L_1_3" + System.lineSeparator() +
                "} ; Closing block $L_1_3" + System.lineSeparator(), sw.toString());
    }

    @Test
    public void testCondition() throws HeadToHeadControlFlowException {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode startNode = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        startNode.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(10)));
        final RegionNode node1 = g.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        final ExpressionList e = new ExpressionList();
        e.add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(20)));
        node1.getExpressions().add(new IFExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(20), new IntegerValue(0), e));
        node1.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(30)));
        final RegionNode node2 = g.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        node2.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(30)));
        final RegionNode node3 = g.createAt(new BytecodeOpcodeAddress(30), RegionNode.BlockType.NORMAL);

        startNode.addEdgeTo(ControlFlowEdgeType.forward, node1);
        node1.addEdgeTo(ControlFlowEdgeType.forward, node2);
        node1.addEdgeTo(ControlFlowEdgeType.forward, node3);
        node2.addEdgeTo(ControlFlowEdgeType.forward, node3);

        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier(g);
        final StringWriter sw = new StringWriter();
        stackifier.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(stackifier, new PrintWriter(sw)));

        assertEquals("RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}" + System.lineSeparator() +
                "BLOCK $B_1_3: {" + System.lineSeparator() +
                "    BLOCK $B_1_2: {" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=10}}" + System.lineSeparator() +
                "        if " + System.lineSeparator() +
                "        break $B_1_2" + System.lineSeparator() +
                "        else " + System.lineSeparator() +
                "        break $B_1_3" + System.lineSeparator() +
                "    } ; Closing block $B_1_2" + System.lineSeparator() +
                "    RegionNode{startAddress=BytecodeOpcodeAddress{address=20}}" + System.lineSeparator() +
                "    break $B_1_3" + System.lineSeparator() +
                "} ; Closing block $B_1_3" + System.lineSeparator() +
                "RegionNode{startAddress=BytecodeOpcodeAddress{address=30}}" + System.lineSeparator(), sw.toString());
    }

    @Test
    public void testCompleteInitLoopBoundaries() throws HeadToHeadControlFlowException {

        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode node0 = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        final RegionNode node1 = g.createAt(new BytecodeOpcodeAddress(13), RegionNode.BlockType.NORMAL);
        final RegionNode node2 = g.createAt(new BytecodeOpcodeAddress(17), RegionNode.BlockType.NORMAL);
        final RegionNode node3 = g.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        final RegionNode node4 = g.createAt(new BytecodeOpcodeAddress(27), RegionNode.BlockType.NORMAL);
        final RegionNode node5 = g.createAt(new BytecodeOpcodeAddress(33), RegionNode.BlockType.NORMAL);
        final RegionNode node6 = g.createAt(new BytecodeOpcodeAddress(39), RegionNode.BlockType.NORMAL);
        final RegionNode node7 = g.createAt(new BytecodeOpcodeAddress(46), RegionNode.BlockType.NORMAL);
        final RegionNode node8 = g.createAt(new BytecodeOpcodeAddress(53), RegionNode.BlockType.NORMAL);
        final RegionNode node9 = g.createAt(new BytecodeOpcodeAddress(66), RegionNode.BlockType.NORMAL);

        node0.addEdgeTo(ControlFlowEdgeType.forward, node1);
        node0.addEdgeTo(ControlFlowEdgeType.forward, node2);
        node1.addEdgeTo(ControlFlowEdgeType.forward, node3);
        node2.addEdgeTo(ControlFlowEdgeType.forward, node3);
        node3.addEdgeTo(ControlFlowEdgeType.forward, node4);
        node3.addEdgeTo(ControlFlowEdgeType.forward, node9);
        node4.addEdgeTo(ControlFlowEdgeType.forward, node5);
        node4.addEdgeTo(ControlFlowEdgeType.forward, node9);
        node5.addEdgeTo(ControlFlowEdgeType.forward, node9);
        node5.addEdgeTo(ControlFlowEdgeType.forward, node6);
        node6.addEdgeTo(ControlFlowEdgeType.forward, node7);
        node7.addEdgeTo(ControlFlowEdgeType.forward, node8);
        node7.addEdgeTo(ControlFlowEdgeType.forward, node9);
        node8.addEdgeTo(ControlFlowEdgeType.forward, node7);

        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier(g);
        final StringWriter sw = new StringWriter();
        stackifier.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(stackifier, new PrintWriter(sw)));

        assertEquals("BLOCK $B_0_3: {" + System.lineSeparator() +
                "    BLOCK $B_0_2: {" + System.lineSeparator() +
                "        BLOCK $B_0_1: {" + System.lineSeparator() +
                "            RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}" + System.lineSeparator() +
                "        } ; Closing block $B_0_1" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=13}}" + System.lineSeparator() +
                "    } ; Closing block $B_0_2" + System.lineSeparator() +
                "    RegionNode{startAddress=BytecodeOpcodeAddress{address=17}}" + System.lineSeparator() +
                "} ; Closing block $B_0_3" + System.lineSeparator() +
                "BLOCK $B_3_9: {" + System.lineSeparator() +
                "    BLOCK $B_3_4: {" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=20}}" + System.lineSeparator() +
                "    } ; Closing block $B_3_4" + System.lineSeparator() +
                "    BLOCK $B_4_9: {" + System.lineSeparator() +
                "        BLOCK $B_4_5: {" + System.lineSeparator() +
                "            RegionNode{startAddress=BytecodeOpcodeAddress{address=27}}" + System.lineSeparator() +
                "        } ; Closing block $B_4_5" + System.lineSeparator() +
                "        BLOCK $B_5_9: {" + System.lineSeparator() +
                "            BLOCK $B_5_6: {" + System.lineSeparator() +
                "                RegionNode{startAddress=BytecodeOpcodeAddress{address=33}}" + System.lineSeparator() +
                "            } ; Closing block $B_5_6" + System.lineSeparator() +
                "            RegionNode{startAddress=BytecodeOpcodeAddress{address=39}}" + System.lineSeparator() +
                "            LOOP $L_7_8: {" + System.lineSeparator() +
                "                BLOCK $B_7_8: {" + System.lineSeparator() +
                "                    RegionNode{startAddress=BytecodeOpcodeAddress{address=46}}" + System.lineSeparator() +
                "                } ; Closing block $B_7_8" + System.lineSeparator() +
                "                RegionNode{startAddress=BytecodeOpcodeAddress{address=53}}" + System.lineSeparator() +
                "            } ; Closing block $L_7_8" + System.lineSeparator() +
                "        } ; Closing block $B_5_9" + System.lineSeparator() +
                "    } ; Closing block $B_4_9" + System.lineSeparator() +
                "} ; Closing block $B_3_9" + System.lineSeparator() +
                "RegionNode{startAddress=BytecodeOpcodeAddress{address=66}}" + System.lineSeparator(), sw.toString());
    }

    @Test
    public void testIfShoulNotExitLoop() throws HeadToHeadControlFlowException {

        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode node0 = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        node0.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(79)));
        node0.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(1), new BytecodeOpcodeAddress(138)));
        final RegionNode node1 = g.createAt(new BytecodeOpcodeAddress(79), RegionNode.BlockType.NORMAL);
        final RegionNode node2 = g.createAt(new BytecodeOpcodeAddress(90), RegionNode.BlockType.NORMAL);

        final ExpressionList e = new ExpressionList();
        e.add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(138)));
        node2.getExpressions().add(new IFExpression(p, new BytecodeOpcodeAddress(90), new BytecodeOpcodeAddress(138), new IntegerValue(0), e));
        node2.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(96)));


        final RegionNode node3 = g.createAt(new BytecodeOpcodeAddress(96), RegionNode.BlockType.NORMAL);
        final RegionNode node4 = g.createAt(new BytecodeOpcodeAddress(138), RegionNode.BlockType.NORMAL);
        final RegionNode node5 = g.createAt(new BytecodeOpcodeAddress(147), RegionNode.BlockType.NORMAL);
        final RegionNode node6 = g.createAt(new BytecodeOpcodeAddress(190), RegionNode.BlockType.NORMAL);
        final RegionNode node7 = g.createAt(new BytecodeOpcodeAddress(197), RegionNode.BlockType.NORMAL);
        final RegionNode node8 = g.createAt(new BytecodeOpcodeAddress(205), RegionNode.BlockType.NORMAL);
        final RegionNode node9 = g.createAt(new BytecodeOpcodeAddress(219), RegionNode.BlockType.NORMAL);
        final RegionNode node10 = g.createAt(new BytecodeOpcodeAddress(247), RegionNode.BlockType.NORMAL);

        node0.addEdgeTo(ControlFlowEdgeType.forward, node1);
        node0.addEdgeTo(ControlFlowEdgeType.forward, node4);
        node1.addEdgeTo(ControlFlowEdgeType.forward, node2);
        node2.addEdgeTo(ControlFlowEdgeType.forward, node3);
        node2.addEdgeTo(ControlFlowEdgeType.forward, node4);
        node3.addEdgeTo(ControlFlowEdgeType.forward, node2);
        node4.addEdgeTo(ControlFlowEdgeType.forward, node5);
        node4.addEdgeTo(ControlFlowEdgeType.forward, node10);
        node5.addEdgeTo(ControlFlowEdgeType.forward, node6);
        node6.addEdgeTo(ControlFlowEdgeType.forward, node10);
        node6.addEdgeTo(ControlFlowEdgeType.forward, node7);
        node7.addEdgeTo(ControlFlowEdgeType.forward, node9);
        node7.addEdgeTo(ControlFlowEdgeType.forward, node8);
        node8.addEdgeTo(ControlFlowEdgeType.forward, node9);
        node9.addEdgeTo(ControlFlowEdgeType.forward, node6);

        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier(g);
        final StringWriter sw = new StringWriter();
        stackifier.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(stackifier, new PrintWriter(sw)));

        assertEquals("BLOCK $B_0_4: {" + System.lineSeparator() +
                "    BLOCK $B_0_1: {" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}" + System.lineSeparator() +
                "        break $B_0_1" + System.lineSeparator() +
                "        break $B_0_4" + System.lineSeparator() +
                "    } ; Closing block $B_0_1" + System.lineSeparator() +
                "    RegionNode{startAddress=BytecodeOpcodeAddress{address=79}}" + System.lineSeparator() +
                "    LOOP $L_2_3: {" + System.lineSeparator() +
                "        BLOCK $B_2_3: {" + System.lineSeparator() +
                "            RegionNode{startAddress=BytecodeOpcodeAddress{address=90}}" + System.lineSeparator() +
                "            if " + System.lineSeparator() +
                "            break $B_0_4" + System.lineSeparator() +
                "            else " + System.lineSeparator() +
                "            break $B_2_3" + System.lineSeparator() +
                "        } ; Closing block $B_2_3" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=96}}" + System.lineSeparator() +
                "    } ; Closing block $L_2_3" + System.lineSeparator() +
                "} ; Closing block $B_0_4" + System.lineSeparator() +
                "BLOCK $B_4_10: {" + System.lineSeparator() +
                "    BLOCK $B_4_5: {" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=138}}" + System.lineSeparator() +
                "    } ; Closing block $B_4_5" + System.lineSeparator() +
                "    RegionNode{startAddress=BytecodeOpcodeAddress{address=147}}" + System.lineSeparator() +
                "    LOOP $L_6_9: {" + System.lineSeparator() +
                "        BLOCK $B_6_7: {" + System.lineSeparator() +
                "            RegionNode{startAddress=BytecodeOpcodeAddress{address=190}}" + System.lineSeparator() +
                "        } ; Closing block $B_6_7" + System.lineSeparator() +
                "        BLOCK $B_7_9: {" + System.lineSeparator() +
                "            BLOCK $B_7_8: {" + System.lineSeparator() +
                "                RegionNode{startAddress=BytecodeOpcodeAddress{address=197}}" + System.lineSeparator() +
                "            } ; Closing block $B_7_8" + System.lineSeparator() +
                "            RegionNode{startAddress=BytecodeOpcodeAddress{address=205}}" + System.lineSeparator() +
                "        } ; Closing block $B_7_9" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=219}}" + System.lineSeparator() +
                "    } ; Closing block $L_6_9" + System.lineSeparator() +
                "} ; Closing block $B_4_10" + System.lineSeparator() +
                "RegionNode{startAddress=BytecodeOpcodeAddress{address=247}}" + System.lineSeparator() , sw.toString());
    }

    @Test
    public void testInlinedGoto() throws HeadToHeadControlFlowException {

        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode node0 = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        final RegionNode node90 = g.createAt(new BytecodeOpcodeAddress(90), RegionNode.BlockType.NORMAL);
        final RegionNode node138 = g.createAt(new BytecodeOpcodeAddress(138), RegionNode.BlockType.NORMAL);
        final RegionNode node190 = g.createAt(new BytecodeOpcodeAddress(190), RegionNode.BlockType.NORMAL);
        final RegionNode node247 = g.createAt(new BytecodeOpcodeAddress(247), RegionNode.BlockType.NORMAL);

        node0.addEdgeTo(ControlFlowEdgeType.forward, node138);
        node0.addEdgeTo(ControlFlowEdgeType.forward, node90);
        node90.addEdgeTo(ControlFlowEdgeType.forward, node90);
        node90.addEdgeTo(ControlFlowEdgeType.forward, node138);
        node138.addEdgeTo(ControlFlowEdgeType.forward, node247);
        node138.addEdgeTo(ControlFlowEdgeType.forward, node190);
        node190.addEdgeTo(ControlFlowEdgeType.forward, node190);
        node190.addEdgeTo(ControlFlowEdgeType.forward, node247);

        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier(g);
        final StringWriter sw = new StringWriter();
        stackifier.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(stackifier, new PrintWriter(sw)));

        stackifier.printDebug(new PrintWriter(System.out));

        assertEquals("BLOCK $B_0_2: {" + System.lineSeparator() +
                "    BLOCK $B_0_1: {" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}" + System.lineSeparator() +
                "    } ; Closing block $B_0_1" + System.lineSeparator() +
                "    LOOP $L_1_1: {" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=90}}" + System.lineSeparator() +
                "    } ; Closing block $L_1_1" + System.lineSeparator() +
                "} ; Closing block $B_0_2" + System.lineSeparator() +
                "BLOCK $B_2_4: {" + System.lineSeparator() +
                "    BLOCK $B_2_3: {" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=138}}" + System.lineSeparator() +
                "    } ; Closing block $B_2_3" + System.lineSeparator() +
                "    LOOP $L_3_3: {" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=190}}" + System.lineSeparator() +
                "    } ; Closing block $L_3_3" + System.lineSeparator() +
                "} ; Closing block $B_2_4" + System.lineSeparator() +
                "RegionNode{startAddress=BytecodeOpcodeAddress{address=247}}" + System.lineSeparator(), sw.toString());
    }

    @Test
    public void testOverlapping() throws HeadToHeadControlFlowException {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode node0 = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        final RegionNode node1 = g.createAt(new BytecodeOpcodeAddress(87), RegionNode.BlockType.NORMAL);
        final RegionNode node2 = g.createAt(new BytecodeOpcodeAddress(107), RegionNode.BlockType.NORMAL);
        final RegionNode node3 = g.createAt(new BytecodeOpcodeAddress(451), RegionNode.BlockType.NORMAL);
        final RegionNode node4 = g.createAt(new BytecodeOpcodeAddress(524), RegionNode.BlockType.NORMAL);
        final RegionNode node5 = g.createAt(new BytecodeOpcodeAddress(759), RegionNode.BlockType.NORMAL);

        node0.addEdgeTo(ControlFlowEdgeType.forward, node1);
        node1.addEdgeTo(ControlFlowEdgeType.forward, node4);
        node1.addEdgeTo(ControlFlowEdgeType.forward, node2);
        node2.addEdgeTo(ControlFlowEdgeType.forward, node3);
        node2.addEdgeTo(ControlFlowEdgeType.forward, node4);
        node2.addEdgeTo(ControlFlowEdgeType.forward, node4);
        node2.addEdgeTo(ControlFlowEdgeType.forward, node4);
        node2.addEdgeTo(ControlFlowEdgeType.forward, node2);
        node3.addEdgeTo(ControlFlowEdgeType.forward, node4);
        node3.addEdgeTo(ControlFlowEdgeType.forward, node1);
        node3.addEdgeTo(ControlFlowEdgeType.forward, node3);
        node4.addEdgeTo(ControlFlowEdgeType.forward, node5);
        node4.addEdgeTo(ControlFlowEdgeType.forward, node5);
        node4.addEdgeTo(ControlFlowEdgeType.forward, node5);

        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier(g);

        stackifier.printDebug(new PrintWriter(System.out));

        final StringWriter sw = new StringWriter();
        stackifier.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(stackifier, new PrintWriter(sw)));

        assertEquals("RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}" + System.lineSeparator() +
                "LOOP $L_1_5: {" + System.lineSeparator() +
                "    BLOCK $B_1_4: {" + System.lineSeparator() +
                "        BLOCK $B_1_2: {" + System.lineSeparator() +
                "            RegionNode{startAddress=BytecodeOpcodeAddress{address=87}}" + System.lineSeparator() +
                "        } ; Closing block $B_1_2" + System.lineSeparator() +
                "        LOOP $L_2_3: {" + System.lineSeparator() +
                "            BLOCK $B_2_3: {" + System.lineSeparator() +
                "                RegionNode{startAddress=BytecodeOpcodeAddress{address=107}}" + System.lineSeparator() +
                "            } ; Closing block $B_2_3" + System.lineSeparator() +
                "            LOOP $L_3_3: {" + System.lineSeparator() +
                "                RegionNode{startAddress=BytecodeOpcodeAddress{address=451}}" + System.lineSeparator() +
                "            } ; Closing block $L_3_3" + System.lineSeparator() +
                "        } ; Closing block $L_2_3" + System.lineSeparator() +
                "    } ; Closing block $B_1_4" + System.lineSeparator() +
                "    RegionNode{startAddress=BytecodeOpcodeAddress{address=524}}" + System.lineSeparator() +
                "    RegionNode{startAddress=BytecodeOpcodeAddress{address=759}}" + System.lineSeparator() +
                "} ; Closing block $L_1_5" + System.lineSeparator(), sw.toString());
    }

    @Test
    public void testHeadToHeadCrossing1() {
        /*

Original:
          0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15  16  17  18  19  20  21  22  23  24  25  26  27  28  29  30  31  32  33  34  35  36  37
  0-  3   ------------>
  0-  1   ---->
  1-  3       -------->
  1-  2       ---->
  3-  4               ---->
  4-  5                   ---->
  4-  6                   -------->
  5- 34                       -------------------------------------------------------------------------------------------------------------------->
  6-  7                           ---->
  6-  8                           -------->
  7- 34                               ------------------------------------------------------------------------------------------------------------>
  8-  9                                   ---->
  9- 10                                       ---->
  9- 11                                       -------->
 10- 30                                           -------------------------------------------------------------------------------->
 11- 12                                               ---->
 11- 13                                               -------->
 12- 30                                                   ------------------------------------------------------------------------>
 13- 15                                                       -------->
 13- 14                                                       ---->
 14- 30                                                           ---------------------------------------------------------------->
 15- 17                                                               -------->
 15- 16                                                               ---->
 16- 30                                                                   -------------------------------------------------------->
 17- 18                                                                       ---->
 18- 20                                                                           -------->
 18- 19                                                                           ---->
 19- 21                                                                               -------->
 20- 21                                                                                   ---->
 21- 22                                                                                       ---->
 21- 23                                                                                       -------->
 22- 28                                                                                           ------------------------>
 23- 24                                                                                               ---->
 23- 25                                                                                               -------->
 24- 26                                                                                                   -------->
 25- 26                                                                                                       ---->
 26- 27                                                                                                           ---->
 26- 37                                                                                                           -------------------------------------------->
 27- 28                                                                                                               ---->
 28- 36                                                                                                                   -------------------------------->
 28- 29                                                                                                                   ---->
 29- 30                                                                                                                       ---->
 30- 32                                                                                                                           -------->
 30- 31                                                                                                                           ---->
 31- 34                                                                                                                               ------------>
 32- 35                                                                                                                                   ------------>
 32- 33                                                                                                                                   ---->
 33- 34                                                                                                                                       ---->
 35-  4                   <----
 36-  9                                       <----
 37- 18                                                                           <----
 forward 0 -> 3
 forward 0 -> 1
 forward 1 -> 3
 forward 1 -> 2
 forward 3 -> 4
 forward 4 -> 5
 forward 4 -> 6
 forward 5 -> 34
 forward 6 -> 7
 forward 6 -> 8
 forward 7 -> 34
 forward 8 -> 9
 forward 9 -> 10
 forward 9 -> 11
 forward 10 -> 30
 forward 11 -> 12
 forward 11 -> 13
 forward 12 -> 30
 forward 13 -> 15
 forward 13 -> 14
 forward 14 -> 30
 forward 15 -> 17
 forward 15 -> 16
 forward 16 -> 30
 forward 17 -> 18
 forward 18 -> 20
 forward 18 -> 19
 forward 19 -> 21
 forward 20 -> 21
 forward 21 -> 22
 forward 21 -> 23
 forward 22 -> 28
 forward 23 -> 24
 forward 23 -> 25
 forward 24 -> 26
 forward 25 -> 26
 forward 26 -> 27
 forward 26 -> 37
 forward 27 -> 28
 forward 28 -> 36
 forward 28 -> 29
 forward 29 -> 30
 forward 30 -> 32
 forward 30 -> 31
 forward 31 -> 34
 forward 32 -> 35
 forward 32 -> 33
 forward 33 -> 34
 back 35 -> 4
 back 36 -> 9
 back 37 -> 18

Stackified:
          0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15  16  17  18  19  20  21  22  23  24  25  26  27  28  29  30  31  32  33  34  35  36  37
  0-  3   ------------>
  0-  1   ---->
  1-  3       -------->
  1-  2       ---->
  3-  4               ---->
  4-  5                   ---->
  4-  6                   -------->
  5- 34                       -------------------------------------------------------------------------------------------------------------------->
  6-  7                           ---->
  6-  8                           -------->
  7- 34                               ------------------------------------------------------------------------------------------------------------>
  8-  9                                   ---->
  9- 10                                       ---->
  9- 11                                       -------->
 10- 30                                           -------------------------------------------------------------------------------->
 11- 12                                               ---->
 11- 13                                               -------->
 12- 30                                                   ------------------------------------------------------------------------>
 13- 15                                                       -------->
 13- 14                                                       ---->
 14- 30                                                           ---------------------------------------------------------------->
 15- 17                                                               -------->
 15- 16                                                               ---->
 16- 30                                                                   -------------------------------------------------------->
 17- 18                                                                       ---->
 18- 20                                                                           -------->
 18- 19                                                                           ---->
 19- 21                                                                           ------------>
 20- 21                                                                                   ---->
 21- 22                                                                                       ---->
 21- 23                                                                                       -------->
 22- 28                                                                                       ---------------------------->
 23- 24                                                                                               ---->
 23- 25                                                                                               -------->
 24- 26                                                                                               ------------>
 25- 26                                                                                                       ---->
 26- 27                                                                                                           ---->
 26- 37                                                                                                           -------------------------------------------->
 27- 28                                                                                                               ---->
 28- 36                                                                                                                   -------------------------------->
 28- 29                                                                                                                   ---->
 29- 30                                                                                                                       ---->
 30- 32                                                                                                                           -------->
 30- 31                                                                                                                           ---->
 31- 34                                                                                                                               ------------>
 32- 35                                                                                                                                   ------------>
 32- 33                                                                                                                                   ---->
 33- 34                                                                                                                                       ---->
 35-  4                   <----
 36-  9                                       <----
 37- 18                                                                           <----
 forward 0 -> 3
 forward 0 -> 1
 forward 1 -> 3
 forward 1 -> 2
 forward 3 -> 4
 forward 4 -> 5
 forward 4 -> 6
 forward 5 -> 34
 forward 6 -> 7
 forward 6 -> 8
 forward 7 -> 34
 forward 8 -> 9
 forward 9 -> 10
 forward 9 -> 11
 forward 10 -> 30
 forward 11 -> 12
 forward 11 -> 13
 forward 12 -> 30
 forward 13 -> 15
 forward 13 -> 14
 forward 14 -> 30
 forward 15 -> 17
 forward 15 -> 16
 forward 16 -> 30
 forward 17 -> 18
 forward 18 -> 20
 forward 18 -> 19
 forward 18 -> 21
 forward 20 -> 21
 forward 21 -> 22
 forward 21 -> 23
 forward 21 -> 28
 forward 23 -> 24
 forward 23 -> 25
 forward 23 -> 26
 forward 25 -> 26
 forward 26 -> 27
 forward 26 -> 37
 forward 27 -> 28
 forward 28 -> 36
 forward 28 -> 29
 forward 29 -> 30
 forward 30 -> 32
 forward 30 -> 31
 forward 31 -> 34
 forward 32 -> 35
 forward 32 -> 33
 forward 33 -> 34
 back 35 -> 4
 back 36 -> 9
 back 37 -> 18

Data:
 0 RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}
 1 RegionNode{startAddress=BytecodeOpcodeAddress{address=112}}
 2 RegionNode{startAddress=BytecodeOpcodeAddress{address=120}}
 3 RegionNode{startAddress=BytecodeOpcodeAddress{address=128}}
 4 RegionNode{startAddress=BytecodeOpcodeAddress{address=172}}
 5 RegionNode{startAddress=BytecodeOpcodeAddress{address=256}}
 6 RegionNode{startAddress=BytecodeOpcodeAddress{address=271}}
 7 RegionNode{startAddress=BytecodeOpcodeAddress{address=287}}
 8 RegionNode{startAddress=BytecodeOpcodeAddress{address=303}}
 9 RegionNode{startAddress=BytecodeOpcodeAddress{address=338}}
 10 RegionNode{startAddress=BytecodeOpcodeAddress{address=364}}
 11 RegionNode{startAddress=BytecodeOpcodeAddress{address=383}}
 12 RegionNode{startAddress=BytecodeOpcodeAddress{address=394}}
 13 RegionNode{startAddress=BytecodeOpcodeAddress{address=401}}
 14 RegionNode{startAddress=BytecodeOpcodeAddress{address=435}}
 15 RegionNode{startAddress=BytecodeOpcodeAddress{address=454}}
 16 RegionNode{startAddress=BytecodeOpcodeAddress{address=465}}
 17 RegionNode{startAddress=BytecodeOpcodeAddress{address=484}}
 18 RegionNode{startAddress=BytecodeOpcodeAddress{address=495}}
 19 RegionNode{startAddress=BytecodeOpcodeAddress{address=503}}
 20 RegionNode{startAddress=BytecodeOpcodeAddress{address=528}}
 21 RegionNode{startAddress=BytecodeOpcodeAddress{address=538}}
 22 RegionNode{startAddress=BytecodeOpcodeAddress{address=575}}
 23 RegionNode{startAddress=BytecodeOpcodeAddress{address=582}}
 24 RegionNode{startAddress=BytecodeOpcodeAddress{address=590}}
 25 RegionNode{startAddress=BytecodeOpcodeAddress{address=601}}
 26 RegionNode{startAddress=BytecodeOpcodeAddress{address=609}}
 27 RegionNode{startAddress=BytecodeOpcodeAddress{address=627}}
 28 RegionNode{startAddress=BytecodeOpcodeAddress{address=633}}
 29 RegionNode{startAddress=BytecodeOpcodeAddress{address=654}}
 30 RegionNode{startAddress=BytecodeOpcodeAddress{address=660}}
 31 RegionNode{startAddress=BytecodeOpcodeAddress{address=676}}
 32 RegionNode{startAddress=BytecodeOpcodeAddress{address=679}}
 33 RegionNode{startAddress=BytecodeOpcodeAddress{address=687}}
 34 RegionNode{startAddress=BytecodeOpcodeAddress{address=706}}
 35 RegionNode{startAddress=BytecodeOpcodeAddress{address=703}}
 36 RegionNode{startAddress=BytecodeOpcodeAddress{address=657}}
 37 RegionNode{startAddress=BytecodeOpcodeAddress{address=630}}


         */
    }

    @Test
    public void testHeadToHeadCrossing2() {
        /*

{21,22} are head to head, arrow 32

Original:
          0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15  16  17  18  19  20  21  22  23
  0-  1   ---->
  1-  2       ---->
  1- 23       ---------------------------------------------------------------------------------------->
  2-  3           ---->
  2-  4           -------->
  3- 21               ------------------------------------------------------------------------>
  4-  5                   ---->
  5-  7                       -------->
  5-  6                       ---->
  6- 13                           ---------------------------->
  7-  9                               -------->
  7-  8                               ---->
  8- 13                                   -------------------->
  9- 11                                       -------->
  9- 10                                       ---->
 10- 11                                           ---->
 10- 12                                           -------->
 11- 13                                               -------->
 11- 12                                               ---->
 12- 13                                                   ---->
 13- 14                                                       ---->
 13- 15                                                       -------->
 14- 16                                                           -------->
 15- 16                                                               ---->
 16- 22                                                                   ------------------------>
 16- 17                                                                   ---->
 17- 18                                                                       ---->
 17- 19                                                                       -------->
 18- 20                                                                           -------->
 19- 20                                                                               ---->
 20- 21                                                                                   ---->
 21-  1       <----
 22-  5                       <----
 forward 0 -> 1
 forward 1 -> 2
 forward 1 -> 23
 forward 2 -> 3
 forward 2 -> 4
 forward 3 -> 21
 forward 4 -> 5
 forward 5 -> 7
 forward 5 -> 6
 forward 6 -> 13
 forward 7 -> 9
 forward 7 -> 8
 forward 8 -> 13
 forward 9 -> 11
 forward 9 -> 10
 forward 10 -> 11
 forward 10 -> 12
 forward 11 -> 13
 forward 11 -> 12
 forward 12 -> 13
 forward 13 -> 14
 forward 13 -> 15
 forward 14 -> 16
 forward 15 -> 16
 forward 16 -> 22
 forward 16 -> 17
 forward 17 -> 18
 forward 17 -> 19
 forward 18 -> 20
 forward 19 -> 20
 forward 20 -> 21
 back 21 -> 1
 back 22 -> 5

Stackified:
          0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15  16  17  18  19  20  21  22  23
  0-  1   ---->
  1-  2       ---->
  1- 23       ---------------------------------------------------------------------------------------->
  2-  3           ---->
  2-  4           -------->
  3- 21               ------------------------------------------------------------------------>
  4-  5                   ---->
  5-  7                       -------->
  5-  6                       ---->
  6- 13                       -------------------------------->
  7-  9                               -------->
  7-  8                               ---->
  8- 13                               ------------------------>
  9- 11                                       -------->
  9- 10                                       ---->
 10- 11                                           ---->
 10- 12                                       ------------>
 11- 13                                       ---------------->
 11- 12                                               ---->
 12- 13                                                   ---->
 13- 14                                                       ---->
 13- 15                                                       -------->
 14- 16                                                       ------------>
 15- 16                                                               ---->
 16- 22                                                                   ------------------------>
 16- 17                                                                   ---->
 17- 18                                                                       ---->
 17- 19                                                                       -------->
 18- 20                                                                       ------------>
 19- 20                                                                               ---->
 20- 21                                                                                   ---->
 21-  1       <----
 22-  5                       <----
 forward 0 -> 1
 forward 1 -> 2
 forward 1 -> 23
 forward 2 -> 3
 forward 2 -> 4
 forward 3 -> 21
 forward 4 -> 5
 forward 5 -> 7
 forward 5 -> 6
 forward 5 -> 13
 forward 7 -> 9
 forward 7 -> 8
 forward 7 -> 13
 forward 9 -> 11
 forward 9 -> 10
 forward 10 -> 11
 forward 9 -> 12
 forward 9 -> 13
 forward 11 -> 12
 forward 12 -> 13
 forward 13 -> 14
 forward 13 -> 15
 forward 13 -> 16
 forward 15 -> 16
 forward 16 -> 22
 forward 16 -> 17
 forward 17 -> 18
 forward 17 -> 19
 forward 17 -> 20
 forward 19 -> 20
 forward 20 -> 21
 back 21 -> 1
 back 22 -> 5

Data:
 0 RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}
 1 RegionNode{startAddress=BytecodeOpcodeAddress{address=4}}
 2 RegionNode{startAddress=BytecodeOpcodeAddress{address=8}}
 3 RegionNode{startAddress=BytecodeOpcodeAddress{address=31}}
 4 RegionNode{startAddress=BytecodeOpcodeAddress{address=46}}
 5 RegionNode{startAddress=BytecodeOpcodeAddress{address=64}}
 6 RegionNode{startAddress=BytecodeOpcodeAddress{address=84}}
 7 RegionNode{startAddress=BytecodeOpcodeAddress{address=90}}
 8 RegionNode{startAddress=BytecodeOpcodeAddress{address=97}}
 9 RegionNode{startAddress=BytecodeOpcodeAddress{address=103}}
 10 RegionNode{startAddress=BytecodeOpcodeAddress{address=108}}
 11 RegionNode{startAddress=BytecodeOpcodeAddress{address=119}}
 12 RegionNode{startAddress=BytecodeOpcodeAddress{address=134}}
 13 RegionNode{startAddress=BytecodeOpcodeAddress{address=143}}
 14 RegionNode{startAddress=BytecodeOpcodeAddress{address=152}}
 15 RegionNode{startAddress=BytecodeOpcodeAddress{address=160}}
 16 RegionNode{startAddress=BytecodeOpcodeAddress{address=165}}
 17 RegionNode{startAddress=BytecodeOpcodeAddress{address=171}}
 18 RegionNode{startAddress=BytecodeOpcodeAddress{address=182}}
 19 RegionNode{startAddress=BytecodeOpcodeAddress{address=191}}
 20 RegionNode{startAddress=BytecodeOpcodeAddress{address=197}}
 21 RegionNode{startAddress=BytecodeOpcodeAddress{address=209}}
 22 RegionNode{startAddress=BytecodeOpcodeAddress{address=206}}
 23 RegionNode{startAddress=BytecodeOpcodeAddress{address=215}}



         */
    }
}