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

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;

import java.util.Map;
import java.util.Set;

public class ControlFlowRecoverer {

    interface Node {
    }

    public static class SimpleNode implements Node {

        private final GraphNode basicBlock;
        private final Node next;

        private SimpleNode(GraphNode aBasicBlock, Node aNext) {
            basicBlock = aBasicBlock;
            next = aNext;
        }

        public GraphNode getBasicBlock() {
            return basicBlock;
        }

        public Node getNext() {
            return next;
        }
    }

    public static class LoopNode implements Node {

        private final Node loopBody;

        public LoopNode(Node aLoopBody) {
            loopBody = aLoopBody;
        }
    }

    public Node recoverFrom(ControlFlowGraph aGraph) {
        GraphNode theStartNode = aGraph.startNode();
        return recoverFrom(aGraph, theStartNode);
    }

    private Node recoverFrom(ControlFlowGraph aGraph, GraphNode aNode) {
        Set<GraphNode> theDominatedNodes = aGraph.dominatedNodesOf(aNode);

        // Check if this is a loop
        for (GraphNode theNode : theDominatedNodes) {
            for (Map.Entry<GraphNode.Edge, GraphNode> theEntry : theNode.getSuccessors().entrySet()) {
                if (theEntry.getKey().getType() == GraphNode.EdgeType.BACK && theEntry.getValue() == aNode) {
                    // Back edge found!, this is clearly a loop

                    // TODO: implement loop generation here!
                }
            }
        }

        // If there are no dominated nodes, there are also no successors, this is clearly a simple node
        if (theDominatedNodes.isEmpty()) {
            return new SimpleNode(aNode, null);
        }

        Map<GraphNode.Edge, GraphNode> theSuccessors = aNode.getSuccessors();
        if (theSuccessors.size() == 1) {
            // We have one successor
            Map.Entry<GraphNode.Edge, GraphNode> theEntry = theSuccessors.entrySet().iterator().next();
            if (theEntry.getKey().getType() == GraphNode.EdgeType.NORMAL) {
                // There is only one forward edge, so we can wrap this in a simple node
                GraphNode theSuccessor = theEntry.getValue();
                Node theNext = recoverFrom(aGraph, theSuccessor);

                replaceGotoWith(aNode.getExpressions(), theSuccessor.getStartAddress(),
                        new InlinedNodeExpression(theSuccessor));

                aGraph.removeDominatedNode(theSuccessor);

                return new SimpleNode(aNode, theNext);
            }
            // One successor, and it is a back-edge. This case should not happen, so we throw an exception
            throw new IllegalStateException("Don't know how to handle " + aNode.getStartAddress().getAddress());
        }
        throw new IllegalStateException("Don't know how to handle " + aNode.getStartAddress().getAddress());
    }

    private void replaceGotoWith(ExpressionList aList, BytecodeOpcodeAddress aAddress, Expression aExpression) {
        for (Expression theExpression : aList.toList()) {
            if (theExpression instanceof GotoExpression) {
                GotoExpression theGoto = (GotoExpression) theExpression;
                if (theGoto.getJumpTarget().equals(aAddress)) {
                    aList.replace(theGoto, aExpression);
                }
            }
        }
    }
}
