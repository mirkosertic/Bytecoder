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
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import de.mirkosertic.bytecoder.ssa.ControlFlowEdgeType;

public class PartitioningTest {

    @Test
    public void testEmptyGraph() {
        final Partitioning thePartitioning = new Partitioning(new HashSet(), t -> true);
        final List<Set<Node>> thePartitions = thePartitioning.partitions();
        assertTrue(thePartitions.isEmpty());
    }

    @Test
    public void testSingleNodeGraph() {
        final Set<Node> theNodes = new HashSet<>();
        final Node node1 = new Node();
        theNodes.add(node1);

        final Partitioning thePartitioning = new Partitioning(theNodes, t -> true);
        final List<Set<Node>> thePartitions = thePartitioning.partitions();
        assertEquals(1, thePartitions.size());
        assertEquals(1, thePartitions.get(0).size());
        assertTrue(thePartitions.get(0).contains(node1));
    }

    @Test
    public void testSinglePartitionGraph() {
        final Set<Node> theNodes = new HashSet<>();
        final Node node1 = new Node();
        theNodes.add(node1);
        final Node node2 = new Node();
        theNodes.add(node2);
        node1.addEdgeTo(ControlFlowEdgeType.forward, node2);

        final Partitioning thePartitioning = new Partitioning(theNodes, t -> true);
        final List<Set<Node>> thePartitions = thePartitioning.partitions();
        assertEquals(1, thePartitions.size());
        assertEquals(2, thePartitions.get(0).size());
        assertTrue(thePartitions.get(0).contains(node1));
        assertTrue(thePartitions.get(0).contains(node2));
    }

    @Test
    public void testMultiPartitionGraph() {
        final Set<Node> theNodes = new HashSet<>();
        final Node node1 = new Node();
        theNodes.add(node1);
        final Node node2 = new Node();
        theNodes.add(node2);

        final Partitioning thePartitioning = new Partitioning(theNodes, t -> true);
        final List<Set<Node>> thePartitions = thePartitioning.partitions();
        assertEquals(2, thePartitions.size());
        assertEquals(1, thePartitions.get(0).size());
        assertEquals(1, thePartitions.get(1).size());
    }
}