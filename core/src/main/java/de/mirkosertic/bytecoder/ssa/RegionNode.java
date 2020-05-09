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
import de.mirkosertic.bytecoder.graph.Edge;
import de.mirkosertic.bytecoder.graph.Node;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RegionNode extends Node<RegionNode, ControlFlowEdgeType> {

    public final static Comparator<RegionNode> NODE_COMPARATOR = Comparator.comparingInt(o -> o.getStartAddress().getAddress());

    public static final Predicate<Edge> FORWARD_EDGE_FILTER_REGULAR_FLOW_ONLY = edge -> edge.edgeType() == ControlFlowEdgeType.forward &&
            ((RegionNode) edge.targetNode()).getType() == BlockType.NORMAL;

    public static final Predicate<Edge> ALL_SUCCCESSORS_REGULAR_FLOW_ONLY = edge ->
            ((RegionNode) edge.targetNode()).getType() == BlockType.NORMAL;

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

    private final BytecodeOpcodeAddress startAddress;
    private final Program program;
    private final BlockType type;
    private final Map<VariableDescription, Value> liveIn;
    private final Map<VariableDescription, Value> liveOut;
    private final ControlFlowGraph owningGraph;
    private final ExpressionList expressions;
    private long startAnalysisTime;
    private long finishedAnalysisTime;

    protected RegionNode(
            final ControlFlowGraph aOwningGraph, final BlockType aType, final Program aProgram, final BytecodeOpcodeAddress aStartAddress) {
        type = aType;
        owningGraph = aOwningGraph;
        startAddress = aStartAddress;
        program = aProgram;
        liveIn = new HashMap<>();
        liveOut = new HashMap<>();
        expressions = new ExpressionList();
    }

    public long getStartAnalysisTime() {
        return startAnalysisTime;
    }

    public void setStartAnalysisTime(final long startAnalysisTime) {
        this.startAnalysisTime = startAnalysisTime;
    }

    public long getFinishedAnalysisTime() {
        return finishedAnalysisTime;
    }

    public void setFinishedAnalysisTime(final long finishedAnalysisTime) {
        this.finishedAnalysisTime = finishedAnalysisTime;
    }

    public ExpressionList getExpressions() {
         return expressions;
    }

    public BlockType getType() {
        return type;
    }

    public boolean hasBackEdgeTo(final RegionNode aNode) {
        for (final Edge edge : outgoingEdges(t -> t == ControlFlowEdgeType.back).collect(Collectors.toList())) {
            if (edge.targetNode() == aNode) {
                return true;
            }
        }
        return false;
    }

    public boolean hasIncomingBackEdges() {
        return incomingEdges().anyMatch(t -> t.edgeType() == ControlFlowEdgeType.back);
    }

    public Set<RegionNode> getPredecessorsIgnoringBackEdges() {
        return incomingEdges().filter(t -> t.edgeType() == ControlFlowEdgeType.forward).map(t -> (RegionNode) t.sourceNode()).collect(Collectors.toSet());
    }

    public Set<RegionNode> getPredecessors() {
        return incomingEdges().map(t -> (RegionNode) t.sourceNode()).collect(Collectors.toSet());
    }

    public BytecodeOpcodeAddress getStartAddress() {
        return startAddress;
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
        final Variable theNewVariable = newVariable(aType);
        theNewVariable.initializeWith(aValue, program.getAnalysisTime());
        expressions.add(new VariableAssignmentExpression(program, aAddress, theNewVariable, aValue));
        return theNewVariable;
    }

    public void addToLiveIn(final Value aValue, final VariableDescription aDescription) {
        liveIn.put(aDescription, aValue);
    }

    public void addToLiveOut(final Value aValue, final VariableDescription aDescription) {
        liveOut.put(aDescription, aValue);
    }

    public BlockState liveIn() {
        final BlockState theState = new BlockState();
        for (final Map.Entry<VariableDescription, Value> theEntry : liveIn.entrySet()) {
            theState.assignToPort(theEntry.getKey(), theEntry.getValue());
        }
        return theState;
    }

    public BlockState liveOut() {
        final BlockState theState = new BlockState();
        for (final Map.Entry<VariableDescription, Value> theEntry : liveOut.entrySet()) {
            theState.assignToPort(theEntry.getKey(), theEntry.getValue());
        }
        return theState;
    }

    public boolean isImmediatelyDominatedBy(final RegionNode aNode) {
        final Set<RegionNode> thePredecessors = getPredecessorsIgnoringBackEdges();
        return thePredecessors.size() == 1 && thePredecessors.contains(aNode);
    }

    public boolean isDominatedBy(final RegionNode aOtherNode) {
        return owningGraph.dominates(aOtherNode, this);
    }

    public Set<RegionNode> dominatedNodes() {
        return owningGraph.dominatedNodesOf(this);
    }

    @Override
    public <T extends Node> T addEdgeTo(final ControlFlowEdgeType aType, final T aTargetNode) {
        if (outgoingEdges().noneMatch(t -> t.targetNode() == aTargetNode)) {
            return super.addEdgeTo(aType, aTargetNode);
        }
        return null;
    }

    @Override
    public String toString() {
        return "RegionNode{" +
                "startAddress=" + startAddress +
                '}';
    }
}