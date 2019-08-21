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

import org.junit.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DominatorsTest {

    public static class NamedNode extends Node<Node, EdgeType> {

        private final String name;

        public NamedNode(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "NamedNode{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }

    public enum SimpleEdgeType implements EdgeType {
        instance
    }

    @Test
    public void testSimpleFlow1() {
        final NamedNode node1 = new NamedNode("node1");
        final NamedNode node2 = new NamedNode("node2");
        final NamedNode node3 = new NamedNode("node3");
        final NamedNode node4 = new NamedNode("node4");
        node1.addEdgeTo(SimpleEdgeType.instance, node2);
        node2.addEdgeTo(SimpleEdgeType.instance, node3);
        node3.addEdgeTo(SimpleEdgeType.instance, node4);
        node1.addEdgeTo(SimpleEdgeType.instance, node4);

        final Dominators<NamedNode> d = new Dominators(node1,
                (Comparator<NamedNode>) (o1, o2) -> o1.name.compareToIgnoreCase(o2.name));
        final List<NamedNode> preOrder = d.getPreOrder();
        assertEquals(4, preOrder.size());
        assertSame(node1, preOrder.get(0));
        assertSame(node2, preOrder.get(1));
        assertSame(node3, preOrder.get(2));
        assertSame(node4, preOrder.get(3));

        assertSame(node1, d.getIDom(node1));
        assertSame(node1, d.getIDom(node2));
        assertSame(node2, d.getIDom(node3));
        assertSame(node1, d.getIDom(node4));

        /*
              node1 -> node 2 -> node 3 -> node 4
                \                           /
                ----------------------------
         */

        assertTrue(d.dominates(node1, node1));
        assertTrue(d.dominates(node1, node2));
        assertTrue(d.dominates(node1, node3));
        assertTrue(d.dominates(node1, node4));
        assertTrue(d.dominates(node2, node3));
        assertFalse(d.dominates(node3, node4));

        final Set<NamedNode> theDomSetNode1 = d.domSetOf(node1);
        assertEquals(4, theDomSetNode1.size());
        assertTrue(theDomSetNode1.contains(node1));
        assertTrue(theDomSetNode1.contains(node2));
        assertTrue(theDomSetNode1.contains(node3));
        assertTrue(theDomSetNode1.contains(node4));

        final Set<NamedNode> theDomSetNode2 = d.domSetOf(node2);
        assertEquals(2, theDomSetNode2.size());
        assertTrue(theDomSetNode2.contains(node2));
        assertTrue(theDomSetNode2.contains(node3));
    }

    @Test
    public void testSimpleFlow2() {
        final NamedNode node1 = new NamedNode("node1");
        final NamedNode node2 = new NamedNode("node2");
        final NamedNode node3 = new NamedNode("node3");
        final NamedNode node4 = new NamedNode("node4");
        node1.addEdgeTo(SimpleEdgeType.instance, node2);
        node1.addEdgeTo(SimpleEdgeType.instance, node3);
        node2.addEdgeTo(SimpleEdgeType.instance, node4);
        node3.addEdgeTo(SimpleEdgeType.instance, node4);

        final Dominators<NamedNode> d = new Dominators(node1,
                (Comparator<NamedNode>) (o1, o2) -> o1.name.compareToIgnoreCase(o2.name));
        final List<NamedNode> preOrder = d.getPreOrder();

        assertEquals(4, preOrder.size());
        assertSame(node1, preOrder.get(0));
        assertSame(node2, preOrder.get(1));
        assertSame(node3, preOrder.get(2));
        assertSame(node4, preOrder.get(3));

        assertSame(node1, d.getIDom(node1));
        assertSame(node1, d.getIDom(node2));
        assertSame(node1, d.getIDom(node3));
        assertSame(node1, d.getIDom(node4));

        /*
              node1 -> node 2 -----> node 4
                \                    /
                -----> node 3 ------
         */

        assertTrue(d.dominates(node1, node1));
        assertTrue(d.dominates(node1, node2));
        assertTrue(d.dominates(node1, node3));
        assertTrue(d.dominates(node1, node4));
        assertFalse(d.dominates(node2, node4));
        assertFalse(d.dominates(node3, node4));

        final Set<NamedNode> theDomSetNode3 = d.domSetOf(node3);
        assertEquals(1, theDomSetNode3.size());
        assertTrue(theDomSetNode3.contains(node3));

    }
}