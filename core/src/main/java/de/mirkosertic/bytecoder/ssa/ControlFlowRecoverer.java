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
package de.mirkosertic.bytecoder.ssa;

import java.util.Map;
import java.util.Set;

public class ControlFlowRecoverer {

    public interface Node {
    }

    public static class SimpleNode implements Node {

        private final GraphNode basicBlock;
        private final Node next;

        public SimpleNode(GraphNode aBasicBlock, Node aNext) {
            basicBlock = aBasicBlock;
            next = aNext;
        }
    }

    public Node recoverFrom(ControlFlowGraph aGraph) {
        GraphNode theStartNode = aGraph.startNode();
        Set<GraphNode> theDominated = aGraph.dominatedNodesOf(theStartNode);
        return recoverFrom(aGraph, theStartNode, theDominated);
    }

    private Node recoverFrom(ControlFlowGraph aGraph, GraphNode aNode, Set<GraphNode> aDominatedNodes) {
        // If there are no dominated nodes, where are also no successors, this is clearly a simple node
        if (aDominatedNodes.isEmpty()) {
            return new SimpleNode(aNode, null);
        }

        Map<GraphNode.Edge, GraphNode> theSuccessors = aNode.getSuccessors();
        if (theSuccessors.size() == 1) {
            // We have one successor
            Map.Entry<GraphNode.Edge, GraphNode> theEntry = theSuccessors.entrySet().iterator().next();
            if (theEntry.getKey().getType() == GraphNode.EdgeType.NORMAL) {
                // There is only one forward edge, so we can wrap this in a simple node
                GraphNode theSuccessor = theEntry.getValue();
                Set<GraphNode> theDominated = aGraph.dominatedNodesOf(theSuccessor);
                return new SimpleNode(aNode, recoverFrom(aGraph, theSuccessor, theDominated));
            }
        }
        throw new IllegalStateException("Don't know how to handle" + aNode.getStartAddress());
    }
}
