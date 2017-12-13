/*
 * Copyright 2017 Mirko Sertic
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;

public class ControlFlowGraph {

    public interface Node {
    }

    public static class SequenceOfSimpleNodes implements Node {
        private final List<SimpleNode> nodes;

        public SequenceOfSimpleNodes(List<SimpleNode> aNodes) {
            nodes = aNodes;
        }

        public List<SimpleNode> getNodes() {
            return nodes;
        }
    }

    public static class SimpleNode implements Node {
        private final GraphNode node;

        public SimpleNode(GraphNode aNode) {
            node = aNode;
        }

        public GraphNode getNode() {
            return node;
        }
    }

    private final List<GraphNode> dominatedNodes;
    private final List<GraphNode> knownNodes;
    private final Program program;

    public ControlFlowGraph(Program aProgram) {
        program = aProgram;
        dominatedNodes = new ArrayList<>();
        knownNodes = new ArrayList<>();
    }

    public GraphNode createAt(BytecodeOpcodeAddress aAddress, GraphNode.BlockType aType) {
        GraphNode theNewBlock = new GraphNode(aType, program, aAddress);
        addDominatedNode(theNewBlock);
        return theNewBlock;
    }

    public void addDominatedNode(GraphNode aGraphNode) {
        dominatedNodes.add(aGraphNode);
        knownNodes.add(aGraphNode);
    }

    public GraphNode nodeStartingAt(BytecodeOpcodeAddress aAddress) {
        for (GraphNode theBlock : knownNodes) {
            if (aAddress.equals(theBlock.getStartAddress())) {
                return theBlock;
            }
        }
        throw new IllegalArgumentException("Unknown address : " + aAddress.getAddress());
    }


    public List<GraphNode> getDominatedNodes() {
        return new ArrayList<>(dominatedNodes);
    }

    public List<GraphNode> getKnownNodes() {
        return new ArrayList<>(knownNodes);
    }

    public Node toRootNode() {
        if (dominatedNodes.size() == 1) {
            GraphNode theSingleNode = dominatedNodes.get(0);
            if (!theSingleNode.containsGoto()) {
                return new SimpleNode(theSingleNode);
            }
        }
        List<SimpleNode> theNodes = new ArrayList<>();
        for (GraphNode theNode : dominatedNodes) {
            theNodes.add(new SimpleNode(theNode));
        }
        return new SequenceOfSimpleNodes(theNodes);
    }

    public void removeDominatedNode(GraphNode aNode) {
        dominatedNodes.remove(aNode);
    }

    public String toDOT() {
        StringWriter theStr = new StringWriter();
        try (PrintWriter thePW = new PrintWriter(theStr)) {
            thePW.println("digraph graphnam {");

            for (GraphNode theNode : knownNodes) {
                thePW.print("   N" + theNode.getStartAddress().getAddress());
                thePW.println(";");

                for (Map.Entry<GraphNode.Edge, GraphNode> theSuccessor : theNode.getSuccessors().entrySet()) {
                    thePW.print("   N" + theNode.getStartAddress().getAddress());
                    thePW.print(" -> ");
                    thePW.print("   N" + theSuccessor.getValue().getStartAddress().getAddress());
                    if (theSuccessor.getKey().getType() == GraphNode.EdgeType.BACK) {
                        thePW.print(" [ label = \"back-edge\"]");
                    }
                    thePW.println(";");
                }
            }

            thePW.println("}");
        }
        return theStr.toString();
    }
}
