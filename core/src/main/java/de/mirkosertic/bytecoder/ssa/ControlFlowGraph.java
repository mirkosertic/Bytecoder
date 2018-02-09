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
import java.util.function.Consumer;

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
            GraphNodePath theChildPath = new GraphNodePath(aPath);
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

    private static class IDRegister {

        private final List<Object> objects;

        public IDRegister() {
            objects = new ArrayList<>();
        }

        public String idFor(Object aObject) {
            if (!objects.contains(aObject)) {
                objects.add(aObject);
            }
            return "" + objects.indexOf(aObject);
        }
    }

    static class DotContext {
        final RegionNode regionNode;
        final ExpressionList expressionList;
        final String owningNodeName;

        public DotContext(RegionNode aRegionNode, ExpressionList aExpressionList, String aOwningNodeName) {
            regionNode = aRegionNode;
            expressionList = aExpressionList;
            owningNodeName = aOwningNodeName;
        }
    }

    class DotJump {
        final String source;
        final String target;
        final boolean backEdge;

        public DotJump(String aSource, String aTarget, boolean aBackEdge) {
            source = aSource;
            target = aTarget;
            backEdge = aBackEdge;
        }
    }

    public String toDOT() {

        IDRegister theRegister = new IDRegister();
        List<DotJump> theJumps = new ArrayList<>();

        StringWriter theStr = new StringWriter();
        try (PrintWriter thePW = new PrintWriter(theStr)) {

            Set<Value> theAllValues = new HashSet<>();

            thePW.println("digraph CFG {");

            Consumer<DotContext> theExpressionConsumer = new Consumer<DotContext>() {

                @Override
                public void accept(DotContext aContext) {
                    List<Expression> theExpressios = aContext.expressionList.toList();
                    for (Expression theExpression : theExpressios) {

                        theAllValues.add(theExpression);

                        String theNodeName = theRegister.idFor(theExpression);

                        thePW.print("       ");
                        thePW.print(aContext.owningNodeName);
                        thePW.print(" -> E_");
                        thePW.print(theNodeName);
                        thePW.print("[style=dotted,color=blue,label=\"e-");
                        thePW.print(theExpressios.indexOf(theExpression));
                        thePW.println("\"];");

                        printIncomingValues(theExpression, "E_" + theNodeName);

                        if (theExpression instanceof GotoExpression) {
                            GotoExpression theGoto = (GotoExpression) theExpression;
                            RegionNode theJumpTarget = nodeStartingAt(theGoto.getJumpTarget());

                            String theJumpTargetRegion = theRegister.idFor(theJumpTarget);

                            theJumps.add(new DotJump("E_" + theNodeName,
                                    "C_" + theJumpTargetRegion + "_control",
                                    aContext.regionNode.hasBackEdgeTo(theJumpTarget)));
                        }

                        if (theExpression instanceof ExpressionListContainer) {
                            ExpressionListContainer theList = (ExpressionListContainer) theExpression;
                            for (ExpressionList theCont : theList.getExpressionLists()) {
                                DotContext theContext = new DotContext(aContext.regionNode, theCont, "E_" + theNodeName);
                                accept(theContext);
                            }
                        }
                    }
                }

                private void printIncomingValues(Value aValue, String aReceivingNodeID) {
                    if (aValue instanceof VariableAssignmentExpression) {
                        VariableAssignmentExpression theAssignment = (VariableAssignmentExpression) aValue;
                        Variable theTarget = theAssignment.getVariable();
                        theAllValues.add(theTarget);

                        thePW.print("       ");
                        thePW.print(aReceivingNodeID);
                        thePW.print(" -> V_");
                        thePW.print(theRegister.idFor(theTarget));
                        thePW.println(";");

                        Value theValue = theAssignment.getValue();
                        theAllValues.add(theValue);

                        String theValueID = null;
                        if (theValue instanceof Expression) {
                            theValueID = "E_" + theRegister.idFor(theValue);
                        }
                        if (theValue instanceof PrimitiveValue) {
                            theValueID = "P_" + theRegister.idFor(theValue);
                        }
                        if (theValue instanceof Variable) {
                            theValueID = "V_" + theRegister.idFor(theValue);
                        }

                        thePW.print("       ");
                        thePW.print(theValueID);
                        thePW.print(" -> ");
                        thePW.print(aReceivingNodeID);
                        thePW.println(";");

                        if (theValue instanceof Expression) {
                            printIncomingValues(theValue, theValueID);
                        }

                    } else {
                        List<Value> theIncomingDataFlows = aValue.incomingDataFlows();
                        for (Value theValue : theIncomingDataFlows) {
                            theAllValues.add(theValue);

                            String theValueID = null;
                            if (theValue instanceof Expression) {
                                theValueID = "E_" + theRegister.idFor(theValue);
                            }
                            if (theValue instanceof PrimitiveValue) {
                                theValueID = "P_" + theRegister.idFor(theValue);
                            }
                            if (theValue instanceof Variable) {
                                theValueID = "V_" + theRegister.idFor(theValue);
                            }

                            thePW.print("       ");

                            thePW.print(theValueID);

                            thePW.print(" -> ");
                            thePW.print(aReceivingNodeID);
                            thePW.println(";");

                            if (theValue instanceof Expression) {
                                printIncomingValues(theValue, theValueID);
                            }
                        }
                    }
                }
            };

            for (RegionNode theRegion : knownNodes) {

                String theRegionID = theRegister.idFor(theRegion);

                thePW.print("   subgraph cluster_");
                thePW.print(theRegionID);
                thePW.println(" {");


                thePW.print("       label=\"Region Address ");
                thePW.print(theRegion.getStartAddress().getAddress());
                thePW.println("\";");

                thePW.println("       style=filled;");
                thePW.println("       color=lightgray;");
                thePW.println();

                thePW.print("       C_");
                thePW.print(theRegionID);
                thePW.println("_control [shape=box, label=\"Control\"];");

                DotContext theContext = new DotContext(theRegion, theRegion.getExpressions(), "C_" + theRegionID+"_control");
                theExpressionConsumer.accept(theContext);

                thePW.println("   }");
            }

            for (Value theValue : theAllValues) {

                String theNodeID = theRegister.idFor(theValue);

                thePW.print("   ");

                if (theValue instanceof Expression) {
                    thePW.print("E_");
                }
                if (theValue instanceof PrimitiveValue) {
                    thePW.print("P_");
                }
                if (theValue instanceof Variable) {
                    thePW.print("V_");
                }

                thePW.print(theNodeID);

                thePW.print("[");

                if (theValue instanceof Expression) {
                    Expression theValueExpression = (Expression) theValue;
                    thePW.print("label=\"");
                    thePW.print(theValueExpression.getClass().getSimpleName().replace("Expression", ""));
                    thePW.print("\"");
                }
                if (theValue instanceof PrimitiveValue) {
                    PrimitiveValue thePrimitive = (PrimitiveValue) theValue;
                    thePW.print("label=\"");
                    thePW.print(thePrimitive.getClass().getSimpleName().replace("Value", ""));
                    thePW.print("\",color=orange");
                }
                if (theValue instanceof Variable) {
                    Variable theVariable = (Variable) theValue;
                    thePW.print("label=\"");
                    thePW.print(theVariable.getName());
                    thePW.print("\",color=green");
                }

                thePW.println("];");

            }

            for (DotJump theJump : theJumps) {

                thePW.print("   ");
                thePW.print(theJump.source);
                thePW.print(" -> ");
                thePW.print(theJump.target);
                if (theJump.backEdge) {
                    thePW.println(" [label=\"back-edge\",color=blue,style=dotted];");
                } else {
                    thePW.println("[color=blue,style=dotted];");
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