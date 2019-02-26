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

import de.mirkosertic.bytecoder.core.BytecodeExceptionTableEntry;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RegionNode {

    public static class ExceptionHandler {

        private final BytecodeOpcodeAddress startPc;
        private final BytecodeOpcodeAddress endPC;
        private final List<BytecodeExceptionTableEntry> catchEntries;

        public ExceptionHandler(final BytecodeOpcodeAddress startPc, final BytecodeOpcodeAddress endPC) {
            this.startPc = startPc;
            this.endPC = endPC;
            this.catchEntries = new ArrayList<>();
        }

        public void addCatchEntry(final BytecodeExceptionTableEntry aEntry) {
            catchEntries.add(aEntry);
        }

        public boolean regionMatchesTo(final BytecodeExceptionTableEntry aEntry) {
            return startPc.equals(aEntry.getStartPC()) && endPC.equals(aEntry.getEndPc());
        }

        public List<BytecodeExceptionTableEntry> getCatchEntries() {
            return catchEntries;
        }

        public BytecodeOpcodeAddress getStartPc() {
            return startPc;
        }

        public BytecodeOpcodeAddress getEndPC() {
            return endPC;
        }

        public boolean sameCatchBlockAs(final ExceptionHandler aOther) {
            if (catchEntries.size() != aOther.catchEntries.size()) {
                return false;
            }
            for (final BytecodeExceptionTableEntry theCatch : catchEntries) {
                boolean found = false;
                for (final BytecodeExceptionTableEntry theOtherCatch : catchEntries) {
                    if (theOtherCatch.getHandlerPc().getAddress() == theCatch.getHandlerPc().getAddress() &&
                        theOtherCatch.getCatchTypeAsInt() == theCatch.getCatchTypeAsInt()) {
                        found = true;
                    }
                }
                if (!found) {
                    return false;
                }
            }
            return true;
        }
    }

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

        public Edge(final EdgeType aType) {
            type = aType;
        }

        public void changeTo(final EdgeType aType) {
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

    protected RegionNode(
            final ControlFlowGraph aOwningGraph, final BlockType aType, final Program aProgram, final BytecodeOpcodeAddress aStartAddress) {
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

    public List<GraphNodePath> getReachableBy() {
        return reachableBy;
    }

    public ExpressionList getExpressions() {
         return expressions;
    }

    public void addReachablePath(final GraphNodePath aPath) {
        reachableBy.add(aPath);
    }

    public BlockType getType() {
        return type;
    }

    public List<RegionNode> getPredecessors() {
        final List<RegionNode> theResult = new ArrayList<>();
        for (final GraphNodePath thePath : reachableBy) {
            if (!thePath.isEmpty()) {
                theResult.add(thePath.lastElement());
            }
        }
        return theResult;
    }

    public boolean hasBackEdgeTo(final RegionNode aNode) {
        for (final Map.Entry<Edge, RegionNode> theEntry : successors.entrySet()) {
            if (theEntry.getKey().getType() == EdgeType.BACK) {
                if (theEntry.getValue() == aNode) {
                    return true;
                }
            }
        }
        return false;
    }

    private Set<RegionNode> predecessorCacheWithoutBackEdges;

    public Set<RegionNode> getPredecessorsIgnoringBackEdges() {
        if (predecessorCacheWithoutBackEdges == null) {
            final Set<RegionNode> theResult = new HashSet<>();
            for (final GraphNodePath thePath : reachableBy) {
                if (!thePath.isEmpty()) {
                    final RegionNode theLastElement = thePath.lastElement();
                    if (!theLastElement.hasBackEdgeTo(this)) {
                        theResult.add(theLastElement);
                    }
                }
            }
            predecessorCacheWithoutBackEdges = theResult;
        }
        return predecessorCacheWithoutBackEdges;
    }

    public void addSuccessor(final RegionNode aBlock) {
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

    private TypeRef.Native toNative(final TypeRef aTypeRef) {
        if (aTypeRef instanceof TypeRef.Native) {
            return (TypeRef.Native) aTypeRef;
        }
        return aTypeRef.resolve();
    }

    public Variable setLocalVariable(final BytecodeOpcodeAddress aAddress, final int aIndex, final TypeRef aType, final Value aValue) {
        final String theName = "local_" + aIndex + "_" + toNative(aType).name();
        for (final Variable v : program.getVariables()) {
            if (v.getName().equals(theName)) {
                expressions.add(new VariableAssignmentExpression(program, aAddress, v, aValue));
                v.receivesDataFrom(aValue);
                return v;
            }
        }
        final Variable v = program.createVariable(theName, aValue.resolveType());
        expressions.add(new VariableAssignmentExpression(program, aAddress, v, aValue));
        v.initializeWith(aValue);
        return v;
    }

    public Variable findLocalVariable(final int index, final TypeRef aType) {
        final String theName = "local_" + index + "_" + toNative(aType).name();
        final List<Variable> theKnown = program.getVariables().stream().filter(t -> t.getName().equals(theName)).collect(Collectors.toList());
        if (theKnown.size() != 1) {
            // At this point we assume there is such a variable and we use it
            return program.createVariable(theName, aType);
        }
        return theKnown.get(0);
    }


    public Variable newVariable(final TypeRef aType) {
        return program.createVariable(aType);
    }

    public Variable newVariable(final BytecodeOpcodeAddress aAddress, final TypeRef aType, final Value aValue)  {
        if (aValue instanceof Variable) {
            final Variable theVar = (Variable) aValue;
            if (theVar.isSynthetic()) {
                return theVar;
            }

        }
        return newVariable(aAddress, aType, aValue, false);
    }

    private Variable newVariable(final BytecodeOpcodeAddress aAddress, final TypeRef aType, final Value aValue, final boolean aIsImport)  {
        final Variable theNewVariable = newVariable(aType);
        theNewVariable.initializeWith(aValue);
        if (!aIsImport) {
            expressions.add(new VariableAssignmentExpression(program, aAddress, theNewVariable, aValue));
        }
        return theNewVariable;
    }

    public Variable newImportedVariable(final TypeRef aType, final VariableDescription aDescription) {
        final Variable theVariable = newVariable(aType);
        imported.put(aDescription, theVariable);
        return theVariable;
    }

    public boolean canThrowException() {
        for (final Map.Entry<Edge, RegionNode> theEntry : successors.entrySet()) {
            if (theEntry.getValue().getType() == BlockType.EXCEPTION_HANDLER) {
                return true;
            }
        }
        return false;
    }

    public void addToExportedList(final Value aValue, final VariableDescription aDescription) {
        exported.put(aDescription, aValue);
    }

    public void addToImportedList(final Value aValue, final VariableDescription aDescription) {
        imported.put(aDescription, aValue);
    }

    public BlockState toStartState() {
        final BlockState theState = new BlockState();
        for (final Map.Entry<VariableDescription, Value> theEntry : imported.entrySet()) {
            theState.assignToPort(theEntry.getKey(), theEntry.getValue());
        }
        return theState;
    }

    public boolean isStrictlyDominatedBy(final RegionNode aNode) {
        final List<RegionNode> thePredecessors = new ArrayList<>(getPredecessors());
        return thePredecessors.size() == 1 && thePredecessors.contains(aNode);
    }

    public boolean isOnlyReachableThru(final RegionNode aOtherNode) {
        // Start nodes are not reachable by anything
        if (reachableBy.isEmpty()) {
            return false;
        }
        // All paths to this node must go thru aOtherNode
        for (final GraphNodePath thePath : reachableBy) {
            if (!thePath.contains(aOtherNode)) {
                return false;
            }
        }
        return true;
    }

    public Set<RegionNode> forwardReachableNodes() {
        final Set<RegionNode> theNodes = new HashSet<>();
        forwardReachableNodes(theNodes, this);
        return theNodes;
    }

    public Set<RegionNode> dominatedNodes() {
        return owningGraph.dominatedNodesOf(this);
    }

    private static void forwardReachableNodes(final Set<RegionNode> aResult, final RegionNode aNode) {
        if (aResult.add(aNode)) {
            for (final Map.Entry<Edge,RegionNode> theSuc : aNode.successors.entrySet()) {
                if (theSuc.getKey().type == EdgeType.NORMAL) {
                    forwardReachableNodes(aResult, theSuc.getValue());
                }
            }
        }
    }
}