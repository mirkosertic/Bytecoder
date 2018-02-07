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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.graph.Node;

public class RegionNode extends Node {

    public enum BlockType {
        NORMAL,
        EXCEPTION_HANDLER,
        FINALLY,
    }

    public enum EdgeType {
        NORMAL, BACK
    }

    public static class Edge {

        private EdgeType type;

        public Edge(EdgeType aType) {
            type = aType;
        }

        public void changeTo(EdgeType aType) {
            type = aType;
        }

        public EdgeType getType() {
            return type;
        }
    }

    private final BytecodeOpcodeAddress startAddress;
    private final Program program;
    private final Map<Edge, RegionNode> successors;
    private final BlockType type;
    private final Map<VariableDescription, Value> imported;
    private final Map<VariableDescription, Value> exported;
    private final List<GraphNodePath> reachableBy;
    private final ControlFlowGraph owningGraph;
    private final ExpressionList expressions;

    protected RegionNode(ControlFlowGraph aOwningGraph, BlockType aType, Program aProgram, BytecodeOpcodeAddress aStartAddress) {
        type = aType;
        owningGraph = aOwningGraph;
        startAddress = aStartAddress;
        program = aProgram;
        successors = new HashMap<>();
        imported = new HashMap<>();
        exported = new HashMap<>();
        reachableBy = new ArrayList<>();
        expressions = new ExpressionList();
    }

    public ExpressionList getExpressions() {
         return expressions;
    }

    public void addReachablePath(GraphNodePath aPath) {
        reachableBy.add(aPath);
    }

    public List<GraphNodePath> reachableBy() {
        return reachableBy;
    }

    public BlockType getType() {
        return type;
    }

    public List<RegionNode> getPredecessors() {
        List<RegionNode> theResult = new ArrayList<>();
        for (GraphNodePath thePath : reachableBy) {
            if (!thePath.isEmpty()) {
                theResult.add(thePath.lastElement());
            }
        }
        return theResult;
    }

    public boolean hasBackEdgeTo(RegionNode aNode) {
        for (Map.Entry<Edge, RegionNode> theEntry : successors.entrySet()) {
            if (theEntry.getKey().getType() == EdgeType.BACK) {
                if (theEntry.getValue() == aNode) {
                    return true;
                }
            }
        }
        return false;
    }

    public Set<RegionNode> getPredecessorsIgnoringBackEdges() {
        Set<RegionNode> theResult = new HashSet<>();
        for (GraphNodePath thePath : reachableBy) {
            if (!thePath.isEmpty()) {
                RegionNode theLastElement = thePath.lastElement();
                if (!theLastElement.hasBackEdgeTo(this)) {
                    theResult.add(theLastElement);
                }
            }
        }
        return theResult;
    }

    public void addSuccessor(RegionNode aBlock) {
        if (!successors.values().contains(aBlock)) {
            successors.put(new Edge(EdgeType.NORMAL), aBlock);
        }
    }

    public Map<Edge, RegionNode> getSuccessors() {
        return successors;
    }

    public BytecodeOpcodeAddress getStartAddress() {
        return startAddress;
    }

    public Variable newVariable(TypeRef aType) {
        return program.createVariable(aType);
    }

    public Variable newVariable(TypeRef aType, Value aValue)  {
        if (aValue instanceof Variable) {
            Variable theVar = (Variable) aValue;
            if (theVar.isSynthetic()) {
                return theVar;
            }

        }
        return newVariable(aType, aValue, false);
    }

    public Variable newVariable(TypeRef aType, Value aValue, boolean aIsImport)  {
        Variable theNewVariable = newVariable(aType);
        theNewVariable.initializeWith(aValue);
        if (!aIsImport) {
            expressions.add(new VariableAssignmentExpression(theNewVariable, aValue));
        }
        return theNewVariable;
    }

    public Variable newImportedVariable(TypeRef aType, VariableDescription aDescription) {
        Variable theVariable = newVariable(aType);
        imported.put(aDescription, theVariable);
        return theVariable;
    }

    public void addToExportedList(Value aValue, VariableDescription aDescription) {
        exported.put(aDescription, aValue);
    }

    public void addToImportedList(Value aValue, VariableDescription aDescription) {
        imported.put(aDescription, aValue);
    }

    public BlockState toFinalState() {
        BlockState theState = new BlockState();
        for (Map.Entry<VariableDescription, Value> theEntry : exported.entrySet()) {
            theState.assignToPort(theEntry.getKey(), theEntry.getValue());
        }
        return theState;
    }

    public BlockState toStartState() {
        BlockState theState = new BlockState();
        for (Map.Entry<VariableDescription, Value> theEntry : imported.entrySet()) {
            theState.assignToPort(theEntry.getKey(), theEntry.getValue());
        }
        return theState;
    }


    public boolean isStrictlyDominatedBy(RegionNode aNode) {
        List<RegionNode> thePredecessors = new ArrayList<>(getPredecessors());
        return thePredecessors.size() == 1 && thePredecessors.contains(aNode);
    }

    public void deleteVariable(Variable aVariable) {
        deleteVariable(aVariable, expressions);
    }

    private void deleteVariable(Variable aVariable, ExpressionList aList) {
        for (Expression theExpression : aList.toList()) {
            if (theExpression instanceof ExpressionListContainer) {
                ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                for (ExpressionList theList : theContainer.getExpressionLists()) {
                    deleteVariable(aVariable, theList);
                }
            }
            if (theExpression instanceof VariableAssignmentExpression) {
                VariableAssignmentExpression theInit = (VariableAssignmentExpression) theExpression;
                List<Value> theIncomingDataFlows = theInit.incomingDataFlows();
                if (theIncomingDataFlows.get(0) == aVariable) {
                    aList.remove(theExpression);
                }
            }
        }
    }

    public boolean isOnlyReachableThru(RegionNode aOtherNode) {
        // Start nodes are not reachable by anything
        if (reachableBy.isEmpty()) {
            return false;
        }
        // All paths to this node must go thru aOtherNode
        for (GraphNodePath thePath : reachableBy) {
            if (!thePath.contains(aOtherNode)) {
                return false;
            }
        }
        return true;
    }

    public Set<RegionNode> forwardReachableNodes() {
        Set<RegionNode> theNodes = new HashSet<>();
        forwardReachableNodes(theNodes, this);
        return theNodes;
    }

    public Set<RegionNode> dominatedNodes() {
        return owningGraph.dominatedNodesOf(this);
    }

    private static void forwardReachableNodes(Set<RegionNode> aResult, RegionNode aNode) {
        if (aResult.add(aNode)) {
            for (Map.Entry<Edge,RegionNode> theSuc : aNode.successors.entrySet()) {
                if (theSuc.getKey().type == EdgeType.NORMAL) {
                    forwardReachableNodes(aResult, theSuc.getValue());
                }
            }
        }
    }

    public void removeEdgesTo(RegionNode aNode) {
        Map<Edge, RegionNode> theNewSucc = new HashMap<>();
        for (Map.Entry<Edge, RegionNode> theEntry : successors.entrySet()) {
            if (!(theEntry.getValue() == aNode)) {
                theNewSucc.put(theEntry.getKey(), theEntry.getValue());
            }
        }
        successors.clear();
        successors.putAll(theNewSucc);
    }

    public void removeFromPaths(RegionNode aNode) {
        for (GraphNodePath thePath : reachableBy) {
            thePath.remove(aNode);
        }
    }

    public void inheritSuccessorsOf(RegionNode aNode) {
        for (Map.Entry<Edge, RegionNode> theEntry : aNode.successors.entrySet()) {
            successors.put(theEntry.getKey(), theEntry.getValue());
        }
    }
}