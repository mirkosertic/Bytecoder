package de.mirkosertic.bytecoder.ssa;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import org.junit.Test;

import java.io.PrintWriter;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ControlFlowGraphDFSOrderTest {

    @Test
    public void testSimpleNode() {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph graph = new ControlFlowGraph(p);
        final RegionNode startNode = graph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        graph.calculateReachabilityAndMarkBackEdges();

        final ControlFlowGraphDFSOrder order = new ControlFlowGraphDFSOrder(graph);
        order.printDebug(new PrintWriter(System.out));

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
        startNode.addSuccessor(node1);
        node1.addSuccessor(node2);
        graph.calculateReachabilityAndMarkBackEdges();

        final ControlFlowGraphDFSOrder order = new ControlFlowGraphDFSOrder(graph);
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
        startNode.addSuccessor(node1);
        startNode.addSuccessor(node2);
        node1.addSuccessor(node3);
        node2.addSuccessor(node3);
        graph.calculateReachabilityAndMarkBackEdges();

        final ControlFlowGraphDFSOrder order = new ControlFlowGraphDFSOrder(graph);
        order.printDebug(new PrintWriter(System.out));

        final List<RegionNode> nodes = order.getNodesInOrder();

        assertEquals(4, nodes.size());
        assertSame(startNode, nodes.get(0));
        assertSame(node1, nodes.get(1));
        assertSame(node2, nodes.get(2));
        assertSame(node3, nodes.get(3));

    }
}