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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;

public class ControlFlowGraph {

    private final List<RegionNode> dominatedNodes;
    private final List<RegionNode> knownNodes;
    private final Program program;

    public ControlFlowGraph(Program aProgram) {
        program = aProgram;
        dominatedNodes = new ArrayList<>();
        knownNodes = new ArrayList<>();
    }

    public Program getProgram() {
        return program;
    }

    public Set<RegionNode> finalNodes() {
        Set<RegionNode> theNodes = new HashSet<>();
        for (RegionNode theNode : knownNodes) {
            Set<RegionNode> theSuccessors = new HashSet<>();
            for (Map.Entry<RegionNode.Edge, RegionNode> theSuccessor : theNode.getSuccessors().entrySet()) {
                if (theSuccessor.getKey().getType() == RegionNode.EdgeType.NORMAL) {
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

    private void calculateReachabilityAndMarkBackEdges(GraphNodePath aPath, RegionNode aNode) {
        aNode.addReachablePath(aPath);
        for (Map.Entry<RegionNode.Edge, RegionNode> theEdge : aNode.getSuccessors().entrySet()) {
            GraphNodePath theChildPath = aPath.clone();
            theChildPath.addToPath(aNode);
            if (aPath.contains(theEdge.getValue())) {
                // This is a back edge
                theEdge.getKey().changeTo(RegionNode.EdgeType.BACK);
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

    public RegionNode createAt(BytecodeOpcodeAddress aAddress, RegionNode.BlockType aType) {
        RegionNode theNewBlock = new RegionNode(this, aType, program, aAddress);
        addDominatedNode(theNewBlock);
        return theNewBlock;
    }

    public void addDominatedNode(RegionNode aGraphNode) {
        dominatedNodes.add(aGraphNode);
        knownNodes.add(aGraphNode);
    }

    public RegionNode startNode() {
        return nodeStartingAt(BytecodeOpcodeAddress.START_AT_ZERO);
    }

    public RegionNode nodeStartingAt(BytecodeOpcodeAddress aAddress) {
        for (RegionNode theBlock : knownNodes) {
            if (Objects.equals(aAddress, theBlock.getStartAddress())) {
                return theBlock;
            }
        }
        throw new IllegalArgumentException("Unknown address : " + aAddress.getAddress());
    }


    public List<RegionNode> getDominatedNodes() {
        return new ArrayList<>(dominatedNodes);
    }

    public List<RegionNode> getKnownNodes() {
        return new ArrayList<>(knownNodes);
    }

    private String toHTMLLabel(RegionNode aNode) {
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

            for (RegionNode theNode : knownNodes) {
                thePW.print("   N" + theNode.getStartAddress().getAddress());
                thePW.print(" [shape=none, margin=0, label=");
                thePW.print(toHTMLLabel(theNode));
                thePW.println("];");

                for (Map.Entry<RegionNode.Edge, RegionNode> theSuccessor : theNode.getSuccessors().entrySet()) {
                    thePW.print("   N" + theNode.getStartAddress().getAddress());
                    thePW.print(" -> ");
                    thePW.print("   N" + theSuccessor.getValue().getStartAddress().getAddress());
                    if (theSuccessor.getKey().getType() == RegionNode.EdgeType.BACK) {
                        thePW.print(" [ label = \"back-edge\"]");
                    }
                    thePW.println(";");
                }
            }

            thePW.println("}");
        }
        return theStr.toString();
    }

    protected Set<RegionNode> dominatedNodesOf(RegionNode aNode) {
        Set<RegionNode> theResult = new HashSet<>();
        theResult.add(aNode);
        for (RegionNode theNode : knownNodes) {
            if (theNode.isOnlyReachableThru(aNode)) {
                theResult.add(theNode);
            }
        }
        return theResult;
    }

    public void delete(RegionNode aNode) {
        for (RegionNode theNode : knownNodes) {
            theNode.removeEdgesTo(aNode);
            theNode.removeFromPaths(aNode);
        }
        knownNodes.remove(aNode);
        dominatedNodes.remove(aNode);
    }
}