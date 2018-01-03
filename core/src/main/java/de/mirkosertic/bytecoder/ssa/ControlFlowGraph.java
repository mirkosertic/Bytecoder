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

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public Program getProgram() {
        return program;
    }

    public Set<GraphNode> finalNodes() {
        Set<GraphNode> theNodes = new HashSet<>();
        for (GraphNode theNode : knownNodes) {
            Set<GraphNode> theSuccessors = new HashSet<>();
            for (Map.Entry<GraphNode.Edge, GraphNode> theSuccessor : theNode.getSuccessors().entrySet()) {
                if (theSuccessor.getKey().getType() == GraphNode.EdgeType.NORMAL) {
                    theSuccessors.add(theSuccessor.getValue());
                }
            }
            if (theSuccessors.isEmpty()) {
                theNodes.add(theNode);
            }
        }
        return theNodes;
    }

    public void calculateReachabilityAndMarkBackEdges() {
        calculateReachabilityAndMarkBackEdges(new GraphNodePath(), startNode());
    }

    private void calculateReachabilityAndMarkBackEdges(GraphNodePath aPath, GraphNode aNode) {
        aNode.addReachablePath(aPath);
        for (Map.Entry<GraphNode.Edge, GraphNode> theEdge : aNode.getSuccessors().entrySet()) {
            GraphNodePath theChildPath = aPath.clone();
            theChildPath.addToPath(aNode);
            if (aPath.contains(theEdge.getValue())) {
                // This is a back edge
                theEdge.getKey().changeTo(GraphNode.EdgeType.BACK);
                theEdge.getValue().addReachablePath(theChildPath);
                // We have already visited the back edge, so we do not to continue here
                // As this would lead to an endless loop
            } else {
                // Normal edge
                // Continue with graph traversal
                calculateReachabilityAndMarkBackEdges(theChildPath, theEdge.getValue());
            }
        }
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

    public GraphNode startNode() {
        return nodeStartingAt(BytecodeOpcodeAddress.START_AT_ZERO);
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

    private String toHTMLLabel(GraphNode aNode) {
        StringBuilder theResult = new StringBuilder("<");

        BlockState theStartState = aNode.toStartState();
        BlockState theFinalState = aNode.toFinalState();
        Set<VariableDescription> theUsedDescs = new HashSet<>();
        theUsedDescs.addAll(theStartState.getPorts().keySet());
        theUsedDescs.addAll(theFinalState.getPorts().keySet());
        List<VariableDescription> theFinalList = new ArrayList<>(theUsedDescs);

        theResult.append("<table>");

        // Header
        if (!theFinalList.isEmpty()) {
            theResult.append("<tr>");
            for (VariableDescription theDesc : theFinalList) {
                if (theDesc instanceof LocalVariableDescription) {
                    LocalVariableDescription theV = (LocalVariableDescription) theDesc;
                    theResult.append("<td> V ");
                    theResult.append(theV.getIndex());
                    theResult.append("</td>");
                } else {
                    StackVariableDescription theV = (StackVariableDescription) theDesc;
                    theResult.append("<td> S ");
                    theResult.append(theV.getPos());
                    theResult.append("</td>");
                }
            }
            theResult.append("</tr>");
        }

        // Inputs
        if (!theFinalList.isEmpty()) {
            theResult.append("<tr>");
            for (VariableDescription theDesc : theFinalList) {
                Value theValue = theStartState.findBySlot(theDesc);
                if (theValue != null) {
                    if (theValue.consumedValues(Value.ConsumptionType.PHIPROPAGATE).size() > 1) {
                        theResult.append("<td bgcolor=\"orange\">X</td>");
                    } else {
                        theResult.append("<td>X</td>");
                    }
                } else {
                    theResult.append("<td bgcolor=\"lightgray\"></td>");
                }
            }
            theResult.append("</tr>");
        }

        // Label
        theResult.append("<tr>");
        theResult.append("<td colspan=\"");
        theResult.append(theFinalList.size());
        theResult.append("\">");
        theResult.append(" Node at ").append(aNode.getStartAddress().getAddress());
        theResult.append("</td></tr>");

        // Outputs
        if (!theFinalList.isEmpty()) {
            theResult.append("<tr>");
            for (VariableDescription theDesc : theFinalList) {
                Value theValue = theFinalState.findBySlot(theDesc);
                if (theValue != null) {
                    theResult.append("<td>X</td>");
                } else {
                    theResult.append("<td bgcolor=\"lightgray\"></td>");
                }
            }
            theResult.append("</tr>");
        }

        // Types
        if (!theFinalList.isEmpty()) {
            theResult.append("<tr>");
            for (VariableDescription theDesc : theFinalList) {
                Value theValue = theFinalState.findBySlot(theDesc);
                if (theValue != null) {
                    theResult.append("<td>");
                    theResult.append(theValue.resolveType().resolve());
                    theResult.append("</td>");
                } else {
                    theResult.append("<td></td>");
                }
            }
            theResult.append("</tr>");
        }

        theResult.append("</table>");
        theResult.append(">");
        return theResult.toString();
    }

    public String toDOT() {
        StringWriter theStr = new StringWriter();
        try (PrintWriter thePW = new PrintWriter(theStr)) {
            thePW.println("digraph CFG {");

            for (GraphNode theNode : knownNodes) {
                thePW.print("   N" + theNode.getStartAddress().getAddress());
                thePW.print(" [shape=none, margin=0, label=");
                thePW.print(toHTMLLabel(theNode));
                thePW.println("];");

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
