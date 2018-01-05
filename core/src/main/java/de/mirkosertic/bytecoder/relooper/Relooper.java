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
package de.mirkosertic.bytecoder.relooper;

import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.GraphNode;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Relooper {

    interface Node {
    }

    public static class SimpleNode implements Node {
        private final GraphNode node;
        private final Node next;

        public SimpleNode(GraphNode aNode, Node aNext) {
            node = aNode;
            next = aNext;
        }
    }

    public Node reloop(ControlFlowGraph aGraph) {
        return reloop(aGraph, aGraph.getKnownNodes());
    }

    private Node reloop(ControlFlowGraph aGraph, Collection<GraphNode> aNodeSoup) {
        if (aNodeSoup.isEmpty()) {
            return null;
        }

        Set<GraphNode> theAllNodes = new HashSet<>(aNodeSoup);
        Set<GraphNode> theJumpTargets = jumoTargetsOf(aNodeSoup);
        theAllNodes.removeAll(theJumpTargets);

        // Now theAllNodes contains only the entry nodes

        if (theAllNodes.size() == 1) {
            GraphNode theSingleNode = theAllNodes.iterator().next();
            Set<GraphNode> theDominated = aGraph.dominatedNodesOf(theSingleNode);
            theDominated.remove(theSingleNode);

            return new SimpleNode(theSingleNode, reloop(aGraph, theDominated));
        }

        throw new IllegalStateException("What do do now?");
    }

    public Set<GraphNode> jumoTargetsOf(Collection<GraphNode> aNode) {
        Set<GraphNode> theResults = new HashSet<>();
        for (GraphNode theNode : aNode) {
            for (Map.Entry<GraphNode.Edge, GraphNode> theEntry : theNode.getSuccessors().entrySet()) {
                if (theEntry.getKey().getType() == GraphNode.EdgeType.NORMAL) {
                    theResults.add(theEntry.getValue());
                }
            }
        }
        return theResults;
    }
}
