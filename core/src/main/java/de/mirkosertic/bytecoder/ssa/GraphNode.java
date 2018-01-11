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

public class GraphNode {

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
    private final ExpressionList expressions;
    private final Program program;
    private final Map<Edge, GraphNode> successors;
    private final BlockType type;
    private final Map<VariableDescription, Value> imported;
    private final Map<VariableDescription, Value> exported;
    private final List<GraphNodePath> reachableBy;
    private final ControlFlowGraph owningGraph;

    protected GraphNode(ControlFlowGraph aOwningGraph, BlockType aType, Program aProgram, BytecodeOpcodeAddress aStartAddress) {
        type = aType;
        owningGraph = aOwningGraph;
        startAddress = aStartAddress;
        program = aProgram;
        expressions = new ExpressionList();
        successors = new HashMap<>();
        imported = new HashMap<>();
        exported = new HashMap<>();
        reachableBy = new ArrayList<>();
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

    public List<GraphNode> getPredecessors() {
        List<GraphNode> theResult = new ArrayList<>();
        for (GraphNodePath thePath : reachableBy) {
            if (!thePath.isEmpty()) {
                theResult.add(thePath.lastElement());
            }
        }
        return theResult;
    }

    public boolean hasBackEdgeTo(GraphNode aNode) {
        for (Map.Entry<Edge, GraphNode> theEntry : successors.entrySet()) {
            if (theEntry.getKey().getType() == EdgeType.BACK) {
                if (theEntry.getValue() == aNode) {
                    return true;
                }
            }
        }
        return false;
    }

    public Set<GraphNode> getPredecessorsIgnoringBackEdges() {
        Set<GraphNode> theResult = new HashSet<>();
        for (GraphNodePath thePath : reachableBy) {
            if (!thePath.isEmpty()) {
                GraphNode theLastElement = thePath.lastElement();
                if (!theLastElement.hasBackEdgeTo(this)) {
                    theResult.add(theLastElement);
                }
            }
        }
        return theResult;
    }

    public void addSuccessor(GraphNode aBlock) {
        if (!successors.values().contains(aBlock)) {
            successors.put(new Edge(EdgeType.NORMAL), aBlock);
        }
    }

    public Map<Edge, GraphNode> getSuccessors() {
        return successors;
    }

    public BytecodeOpcodeAddress getStartAddress() {
        return startAddress;
    }

    public Variable newVariable(TypeRef aType) {
        return program.createVariable(aType);
    }

    public Variable newVariable(TypeRef aType, Value aValue)  {
        return newVariable(aType, aValue, false);
    }

    public Variable newVariable(TypeRef aType, Value aValue, boolean aIsImport)  {
        Variable theNewVariable = newVariable(aType);
        theNewVariable.initializeWith(aValue);
        if (!aIsImport) {
            expressions.add(new InitVariableExpression(theNewVariable, aValue));
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

    public void addExpression(Expression aExpression) {
        expressions.add(aExpression);
    }

    public ExpressionList getExpressions() {
        return expressions;
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

    public boolean endWithNeverReturningExpression() {
        Expression theLastExpression = expressions.lastExpression();
        return theLastExpression instanceof ReturnExpression ||
                theLastExpression instanceof ReturnValueExpression ||
                theLastExpression instanceof TableSwitchExpression ||
                theLastExpression instanceof LookupSwitchExpression ||
                theLastExpression instanceof ThrowExpression ||
                theLastExpression instanceof ExtendedIFExpression ||
                theLastExpression instanceof GotoExpression;
    }

    public boolean endsWithReturn() {
        Expression theLastExpression = expressions.lastExpression();
        return theLastExpression instanceof ReturnExpression ||
                theLastExpression instanceof ReturnValueExpression;
    }

    public boolean isStrictlyDominatedBy(GraphNode aNode) {
        List<GraphNode> thePredecessors = new ArrayList<>(getPredecessors());
        return thePredecessors.size() == 1 && thePredecessors.contains(aNode);
    }

    public boolean containsGoto() {
        return containsGoto(expressions);
    }

    private boolean containsGoto(ExpressionList aExpressionList) {
        for (Expression theExpression : aExpressionList.toList()) {
            if (theExpression instanceof GotoExpression) {
                return true;
            }
            if (theExpression instanceof ExpressionListContainer) {
                ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                for (ExpressionList theList : theContainer.getExpressionLists()) {
                    if (containsGoto(theList)) {
                        return true;
                    }
                }
            }
        }
        return false;
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
            if (theExpression instanceof InitVariableExpression) {
                InitVariableExpression theInit = (InitVariableExpression) theExpression;
                if (theInit.getVariable() == aVariable) {
                    aList.remove(theExpression);
                }
            }
        }
    }

    public boolean isOnlyReachableThru(GraphNode aOtherNode) {
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

    public Set<GraphNode> forwardReachableNodes() {
        Set<GraphNode> theNodes = new HashSet<>();
        forwardReachableNodes(theNodes, this);
        return theNodes;
    }

    public Set<GraphNode> dominatedNodes() {
        return owningGraph.dominatedNodesOf(this);
    }

    private static void forwardReachableNodes(Set<GraphNode> aResult, GraphNode aNode) {
        if (aResult.add(aNode)) {
            for (Map.Entry<Edge,GraphNode> theSuc : aNode.successors.entrySet()) {
                if (theSuc.getKey().type == EdgeType.NORMAL) {
                    forwardReachableNodes(aResult, theSuc.getValue());
                }
            }
        }
    }

    public void removeEdgesTo(GraphNode aNode) {
        Map<Edge, GraphNode> theNewSucc = new HashMap<>();
        for (Map.Entry<Edge, GraphNode> theEntry : successors.entrySet()) {
            if (!(theEntry.getValue() == aNode)) {
                theNewSucc.put(theEntry.getKey(), theEntry.getValue());
            }
        }
        successors.clear();
        successors.putAll(theNewSucc);
    }

    public void removeFromPaths(GraphNode aNode) {
        for (GraphNodePath thePath : reachableBy) {
            thePath.remove(aNode);
        }
    }

    public void inheritSuccessorsOf(GraphNode aNode) {
        for (Map.Entry<Edge, GraphNode> theEntry : aNode.successors.entrySet()) {
            successors.put(theEntry.getKey(), theEntry.getValue());
        }
    }
}