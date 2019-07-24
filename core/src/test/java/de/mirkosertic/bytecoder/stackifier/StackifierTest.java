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

import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.IFExpression;
import de.mirkosertic.bytecoder.ssa.IntegerValue;
import org.junit.Test;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.DebugInformation;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.RegionNode;

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
        graph.printDebug(new PrintWriter(System.out));
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
        startNode.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(10)));
        startNode.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(1), new BytecodeOpcodeAddress(20)));

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
    public void testSimpleLoopWithSuccessor1() throws IrreducibleControlFlowException {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode startNode = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        startNode.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(20)));
        final RegionNode node1 = g.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        node1.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(1), new BytecodeOpcodeAddress(0)));
        final RegionNode node2 = g.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);

        startNode.addSuccessor(node1);
        startNode.addSuccessor(node2);
        node1.addSuccessor(startNode);
        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier();

        final StructuredControlFlow<RegionNode> graph = stackifier.stackify(g);
        final StringWriter sw = new StringWriter();

        graph.printDebug(new PrintWriter(System.out));

        graph.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(new PrintWriter(sw)));

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
    public void testSimpleLoopWithSuccessor2() throws IrreducibleControlFlowException {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode startNode = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        startNode.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(0)));
        final RegionNode node1 = g.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        node1.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(1), new BytecodeOpcodeAddress(20)));
        final RegionNode node2 = g.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);

        startNode.addSuccessor(node1);
        startNode.addSuccessor(node2);
        node1.addSuccessor(startNode);
        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier();

        final StructuredControlFlow<RegionNode> graph = stackifier.stackify(g);
        final StringWriter sw = new StringWriter();
        graph.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(new PrintWriter(sw)));

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
    public void testSimpleLoopDoubleExit() throws IrreducibleControlFlowException {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode startNode = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        startNode.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(20)));
        final RegionNode node1 = g.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        node1.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(1), new BytecodeOpcodeAddress(20)));
        node1.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(1), new BytecodeOpcodeAddress(0)));
        final RegionNode node2 = g.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);

        startNode.addSuccessor(node1);
        startNode.addSuccessor(node2);
        node1.addSuccessor(startNode);
        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier();

        final StructuredControlFlow<RegionNode> graph = stackifier.stackify(g);

        graph.printDebug(new PrintWriter(System.out));

        final StringWriter sw = new StringWriter();
        graph.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(new PrintWriter(sw)));

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
    public void testSimpleLoopDoubleContinue() throws IrreducibleControlFlowException {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode startNode = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        startNode.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(0)));
        final RegionNode node1 = g.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        node1.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(1), new BytecodeOpcodeAddress(20)));
        node1.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(1), new BytecodeOpcodeAddress(0)));
        final RegionNode node2 = g.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);

        startNode.addSuccessor(node1);
        startNode.addSuccessor(node2);
        node1.addSuccessor(startNode);
        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier();

        final StructuredControlFlow<RegionNode> graph = stackifier.stackify(g);
        final StringWriter sw = new StringWriter();
        graph.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(new PrintWriter(sw)));

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
    public void testMoreComplexExample() throws IrreducibleControlFlowException {
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

        startNode.addSuccessor(node10);
        startNode.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(0), new BytecodeOpcodeAddress(10)));

        node10.addSuccessor(node16);
        node10.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(10), new BytecodeOpcodeAddress(16)));
        node10.addSuccessor(node41);
        node10.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(11), new BytecodeOpcodeAddress(41)));

        node16.addSuccessor(node35);
        node16.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(16), new BytecodeOpcodeAddress(35)));
        node16.addSuccessor(node31);
        node16.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(17), new BytecodeOpcodeAddress(31)));

        node35.addSuccessor(node10);
        node35.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(35), new BytecodeOpcodeAddress(10)));

        node41.addSuccessor(node51);
        node41.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(41), new BytecodeOpcodeAddress(51)));

        node51.addSuccessor(node57);
        node51.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(51), new BytecodeOpcodeAddress(57)));
        node51.addSuccessor(node82);
        node51.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(52), new BytecodeOpcodeAddress(82)));

        node57.addSuccessor(node72);
        node57.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(57), new BytecodeOpcodeAddress(72)));
        node57.addSuccessor(node76);
        node57.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(58), new BytecodeOpcodeAddress(76)));

        node76.addSuccessor(node51);
        node76.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(76), new BytecodeOpcodeAddress(51)));

        node82.addSuccessor(node95);
        node82.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(82), new BytecodeOpcodeAddress(95)));
        node82.addSuccessor(node99);
        node82.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(83), new BytecodeOpcodeAddress(99)));

        node99.addSuccessor(node112);
        node99.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(99), new BytecodeOpcodeAddress(112)));
        node99.addSuccessor(node116);
        node99.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(100), new BytecodeOpcodeAddress(116)));

        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier();

        final StructuredControlFlow<RegionNode> graph = stackifier.stackify(g);
        final StringWriter sw = new StringWriter();
        graph.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(new PrintWriter(sw)));

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
        node74.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(74), new BytecodeOpcodeAddress(109)));
        node74.addSuccessor(node109);
        node81.addSuccessor(node103);
        node81.addSuccessor(node100);
        node100.addSuccessor(node74);
        node100.getExpressions().add(new GotoExpression(p, new BytecodeOpcodeAddress(100), new BytecodeOpcodeAddress(74)));

        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier();

        final StructuredControlFlow<RegionNode> graph = stackifier.stackify(g);

        graph.printDebug(new PrintWriter(System.out));

        final StringWriter sw = new StringWriter();
        graph.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(new PrintWriter(sw)));

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
    public void testAnotherComplexExample() throws IrreducibleControlFlowException {
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

        node0.addSuccessor(node1);
        node1.addSuccessor(node2);
        node1.addSuccessor(node3);
        node3.addSuccessor(node1);

        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier();

        final StructuredControlFlow<RegionNode> graph = stackifier.stackify(g);
        final StringWriter sw = new StringWriter();
        graph.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(new PrintWriter(sw)));

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
    public void testCondition() throws IrreducibleControlFlowException {
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

        startNode.addSuccessor(node1);
        node1.addSuccessor(node2);
        node1.addSuccessor(node3);
        node2.addSuccessor(node3);

        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier();

        final StructuredControlFlow<RegionNode> graph = stackifier.stackify(g);

        graph.printDebug(new PrintWriter(System.out));

        final StringWriter sw = new StringWriter();
        graph.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(new PrintWriter(sw)));

        assertEquals("RegionNode{startAddress=BytecodeOpcodeAddress{address=0}}" + System.lineSeparator() +
                "BLOCK $B_1_3: {" + System.lineSeparator() +
                "    BLOCK $B_1_2: {" + System.lineSeparator() +
                "        RegionNode{startAddress=BytecodeOpcodeAddress{address=10}}" + System.lineSeparator() +
                "        if " + System.lineSeparator() +
                "            break $B_1_2" + System.lineSeparator() +
                "        break $B_1_3" + System.lineSeparator() +
                "    } ; Closing block $B_1_2" + System.lineSeparator() +
                "    RegionNode{startAddress=BytecodeOpcodeAddress{address=20}}" + System.lineSeparator() +
                "    break $B_1_3" + System.lineSeparator() +
                "} ; Closing block $B_1_3" + System.lineSeparator() +
                "RegionNode{startAddress=BytecodeOpcodeAddress{address=30}}" + System.lineSeparator(), sw.toString());
    }

    @Test
    public void testCompletInitLoopBoundaries() throws IrreducibleControlFlowException {

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

        node0.addSuccessor(node1);
        node0.addSuccessor(node2);
        node1.addSuccessor(node3);
        node2.addSuccessor(node3);
        node3.addSuccessor(node4);
        node3.addSuccessor(node9);
        node4.addSuccessor(node5);
        node4.addSuccessor(node9);
        node5.addSuccessor(node9);
        node5.addSuccessor(node6);
        node6.addSuccessor(node7);
        node7.addSuccessor(node8);
        node7.addSuccessor(node9);
        node8.addSuccessor(node7);

        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier();

        final StructuredControlFlow<RegionNode> graph = stackifier.stackify(g);

        graph.printDebug(new PrintWriter(System.out));

        final StringWriter sw = new StringWriter();
        graph.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(new PrintWriter(sw)));

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
                "                RegionNode{startAddress=BytecodeOpcodeAddress{address=46}}" + System.lineSeparator() +
                "                RegionNode{startAddress=BytecodeOpcodeAddress{address=53}}" + System.lineSeparator() +
                "            } ; Closing block $L_7_8" + System.lineSeparator() +
                "        } ; Closing block $B_5_9" + System.lineSeparator() +
                "    } ; Closing block $B_4_9" + System.lineSeparator() +
                "} ; Closing block $B_3_9" + System.lineSeparator() +
                "RegionNode{startAddress=BytecodeOpcodeAddress{address=66}}" + System.lineSeparator(), sw.toString());
    }
}