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
package de.mirkosertic.bytecoder.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.io.PrintWriter;
import java.util.List;

import org.junit.Test;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.ssa.ControlFlowEdgeType;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.DebugInformation;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.RegionNode;

public class GraphDFSOrderTest {

    @Test
    public void testSimpleNode() {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph graph = new ControlFlowGraph(p);
        final RegionNode startNode = graph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        graph.calculateReachabilityAndMarkBackEdges();

        final GraphDFSOrder<RegionNode> order = new GraphDFSOrder(graph.startNode(), RegionNode.NODE_COMPARATOR,
                RegionNode.FORWARD_EDGE_FILTER_REGULAR_FLOW_ONLY);

        final List<RegionNode> nodes = order.getNodesInOrder();

        order.printDebug(new PrintWriter(System.out));

        assertEquals(1, nodes.size());
        assertSame(startNode, nodes.get(0));
    }

    @Test
    public void testSimpleFlow() {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph graph = new ControlFlowGraph(p);
        final RegionNode startNode = graph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        final RegionNode node2 = graph.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        final RegionNode node1 = graph.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);

        startNode.addEdgeTo(ControlFlowEdgeType.forward, node1);
        node1.addEdgeTo(ControlFlowEdgeType.forward, node2);
        graph.calculateReachabilityAndMarkBackEdges();

        final GraphDFSOrder<RegionNode> order = new GraphDFSOrder(graph.startNode(), RegionNode.NODE_COMPARATOR,
                RegionNode.FORWARD_EDGE_FILTER_REGULAR_FLOW_ONLY);
        order.printDebug(new PrintWriter(System.out));

        final List<RegionNode> nodes = order.getNodesInOrder();

        assertEquals(3, nodes.size());
        assertSame(startNode, nodes.get(0));
        assertSame(node1, nodes.get(1));
        assertSame(node2, nodes.get(2));
    }

    @Test
    public void testJoiningFlow() {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph graph = new ControlFlowGraph(p);
        final RegionNode startNode = graph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        final RegionNode node2 = graph.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        final RegionNode node1 = graph.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        final RegionNode node3 = graph.createAt(new BytecodeOpcodeAddress(30), RegionNode.BlockType.NORMAL);

        startNode.addEdgeTo(ControlFlowEdgeType.forward, node1);
        startNode.addEdgeTo(ControlFlowEdgeType.forward, node2);
        node1.addEdgeTo(ControlFlowEdgeType.forward, node3);
        node2.addEdgeTo(ControlFlowEdgeType.forward, node3);
        graph.calculateReachabilityAndMarkBackEdges();

        final GraphDFSOrder<RegionNode> order = new GraphDFSOrder(graph.startNode(), RegionNode.NODE_COMPARATOR,
                RegionNode.FORWARD_EDGE_FILTER_REGULAR_FLOW_ONLY);
        order.printDebug(new PrintWriter(System.out));

        final List<RegionNode> nodes = order.getNodesInOrder();

        assertEquals(4, nodes.size());
        assertSame(startNode, nodes.get(0));
        assertSame(node1, nodes.get(1));
        assertSame(node2, nodes.get(2));
        assertSame(node3, nodes.get(3));
    }

    @Test
    public void testJoiningFlowWithLoop() {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph graph = new ControlFlowGraph(p);
        final RegionNode startNode = graph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        final RegionNode node2 = graph.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        final RegionNode node1 = graph.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        final RegionNode node3 = graph.createAt(new BytecodeOpcodeAddress(30), RegionNode.BlockType.NORMAL);
        final RegionNode node4 = graph.createAt(new BytecodeOpcodeAddress(40), RegionNode.BlockType.NORMAL);

        startNode.addEdgeTo(ControlFlowEdgeType.forward, node1);
        node1.addEdgeTo(ControlFlowEdgeType.forward, node2);
        startNode.addEdgeTo(ControlFlowEdgeType.forward, node3);
        node3.addEdgeTo(ControlFlowEdgeType.forward, node4);
        node2.addEdgeTo(ControlFlowEdgeType.forward, node4);
        graph.calculateReachabilityAndMarkBackEdges();

        final GraphDFSOrder<RegionNode> order = new GraphDFSOrder(graph.startNode(), RegionNode.NODE_COMPARATOR,
                RegionNode.FORWARD_EDGE_FILTER_REGULAR_FLOW_ONLY);
        order.printDebug(new PrintWriter(System.out));

        final List<RegionNode> nodes = order.getNodesInOrder();

        assertEquals(5, nodes.size());
        assertSame(startNode, nodes.get(0));
        assertSame(node1, nodes.get(1));
        assertSame(node2, nodes.get(2));
        assertSame(node3, nodes.get(3));
        assertSame(node4, nodes.get(4));
    }
}