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
package de.mirkosertic.bytecoder.ssa;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;

public class ControlFlowGraphSCCTest {

    @Test
    public void testSimpleNode() {
        final Program p = new Program(DebugInformation.empty(), null);
        final ControlFlowGraph graph = new ControlFlowGraph(p);
        final RegionNode startNode = graph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        graph.calculateReachabilityAndMarkBackEdges();

        final ControlFlowGraphSCC scc = new ControlFlowGraphSCC(graph);
        final List<List<RegionNode>> nodes = scc.getConnectedComponentList();

        assertEquals(1, nodes.size());
        assertEquals(Collections.singletonList(startNode), nodes.get(0));

        assertEquals(Collections.singletonList(startNode), scc.getNodesInOrder());
    }

    @Test
    public void testSimpleFlow() {
        final Program p = new Program(DebugInformation.empty(), null);
        final ControlFlowGraph graph = new ControlFlowGraph(p);
        final RegionNode startNode = graph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        final RegionNode node2 = graph.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        final RegionNode node1 = graph.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        startNode.addEdgeTo(ControlFlowEdgeType.forward, node1);
        node1.addEdgeTo(ControlFlowEdgeType.forward, node2);
        graph.calculateReachabilityAndMarkBackEdges();

        final ControlFlowGraphSCC scc = new ControlFlowGraphSCC(graph);
        final List<List<RegionNode>> nodes = scc.getConnectedComponentList();

        assertEquals(3, nodes.size());
        assertEquals(Collections.singletonList(node2), nodes.get(0));
        assertEquals(Collections.singletonList(node1), nodes.get(1));
        assertEquals(Collections.singletonList(startNode), nodes.get(2));

        assertEquals(Arrays.asList(startNode, node1, node2), scc.getNodesInOrder());
    }

    @Test
    public void testJoiningFlow() {
        final Program p = new Program(DebugInformation.empty(), null);
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


        final ControlFlowGraphSCC scc = new ControlFlowGraphSCC(graph);
        final List<List<RegionNode>> nodes = scc.getConnectedComponentList();

        assertEquals(4, nodes.size());

        assertEquals(Collections.singletonList(startNode), nodes.get(3));
        assertEquals(Collections.singletonList(node1), nodes.get(2));
        assertEquals(Collections.singletonList(node2), nodes.get(1));
        assertEquals(Collections.singletonList(node3), nodes.get(0));

        assertEquals(Arrays.asList(startNode, node1, node2, node3), scc.getNodesInOrder());
    }

    @Test
    public void testLoop1() {
        final Program p = new Program(DebugInformation.empty(), null);
        final ControlFlowGraph graph = new ControlFlowGraph(p);
        final RegionNode startNode = graph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        final RegionNode node2 = graph.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        final RegionNode node1 = graph.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        startNode.addEdgeTo(ControlFlowEdgeType.forward, node1);
        node1.addEdgeTo(ControlFlowEdgeType.forward, node2);
        node2.addEdgeTo(ControlFlowEdgeType.forward, startNode);
        graph.calculateReachabilityAndMarkBackEdges();

        final ControlFlowGraphSCC scc = new ControlFlowGraphSCC(graph);
        final List<List<RegionNode>> nodes = scc.getConnectedComponentList();

        assertEquals(1, nodes.size());
        assertEquals(Arrays.asList(node2, node1, startNode), nodes.get(0));

        assertEquals(Arrays.asList(startNode, node1, node2), scc.getNodesInOrder());
    }

    @Test
    public void testLoop2() {
        final Program p = new Program(DebugInformation.empty(), null);
        final ControlFlowGraph graph = new ControlFlowGraph(p);
        final RegionNode startNode = graph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        final RegionNode node2 = graph.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        final RegionNode node1 = graph.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        startNode.addEdgeTo(ControlFlowEdgeType.forward, node1);
        node1.addEdgeTo(ControlFlowEdgeType.forward, node2);
        node1.addEdgeTo(ControlFlowEdgeType.forward, startNode);
        graph.calculateReachabilityAndMarkBackEdges();

        final ControlFlowGraphSCC scc = new ControlFlowGraphSCC(graph);
        final List<List<RegionNode>> nodes = scc.getConnectedComponentList();

        assertEquals(2, nodes.size());
        assertEquals(Arrays.asList(node1, startNode), nodes.get(1));
        assertEquals(Collections.singletonList(node2), nodes.get(0));

        assertEquals(Arrays.asList(startNode, node1, node2), scc.getNodesInOrder());
    }

    @Test
    public void testLoop3Joining() {
        final Program p = new Program(DebugInformation.empty(), null);
        final ControlFlowGraph graph = new ControlFlowGraph(p);
        final RegionNode startNode = graph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        final RegionNode node1 = graph.createAt(new BytecodeOpcodeAddress(10), RegionNode.BlockType.NORMAL);
        final RegionNode node2 = graph.createAt(new BytecodeOpcodeAddress(20), RegionNode.BlockType.NORMAL);
        final RegionNode node3 = graph.createAt(new BytecodeOpcodeAddress(30), RegionNode.BlockType.NORMAL);
        final RegionNode node4 = graph.createAt(new BytecodeOpcodeAddress(40), RegionNode.BlockType.NORMAL);
        startNode.addEdgeTo(ControlFlowEdgeType.forward, node1);
        node1.addEdgeTo(ControlFlowEdgeType.forward, node3);
        node1.addEdgeTo(ControlFlowEdgeType.forward, node2);
        node2.addEdgeTo(ControlFlowEdgeType.forward, node1);
        node2.addEdgeTo(ControlFlowEdgeType.forward, node4);
        node3.addEdgeTo(ControlFlowEdgeType.forward, node4);
        graph.calculateReachabilityAndMarkBackEdges();

        final ControlFlowGraphSCC scc = new ControlFlowGraphSCC(graph);
        final List<List<RegionNode>> nodes = scc.getConnectedComponentList();

        assertEquals(4, nodes.size());
        assertEquals(Collections.singletonList(node4), nodes.get(0));
        assertEquals(Collections.singletonList(node3), nodes.get(1));
        assertEquals(Arrays.asList(node2, node1), nodes.get(2));
        assertEquals(Collections.singletonList(startNode), nodes.get(3));

        assertEquals(Arrays.asList(startNode, node1, node2, node3, node4), scc.getNodesInOrder());
    }


}