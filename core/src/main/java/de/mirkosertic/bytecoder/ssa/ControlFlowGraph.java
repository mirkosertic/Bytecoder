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
import de.mirkosertic.bytecoder.core.BytecodeProgram;
import de.mirkosertic.bytecoder.graph.Dominators;
import de.mirkosertic.bytecoder.graph.Edge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

public class ControlFlowGraph {

    private final List<RegionNode> knownNodes;
    private final Program program;
    private Dominators<RegionNode> dominators;
    private Dominators<RegionNode> regularFlowDominators;

    public ControlFlowGraph(final Program aProgram) {
        program = aProgram;
        knownNodes = new ArrayList<>();
    }

    public Program getProgram() {
        return program;
    }

    public Dominators<RegionNode> dominators() {
        return dominators;
    }

    public boolean dominates(final RegionNode dominator, final RegionNode dominated) {
        return dominators.dominates(dominator, dominated);
    }

    public boolean dominatesInRegularFlowOnly(final RegionNode node, final RegionNode targetNode) {
        return regularFlowDominators.dominates(node, targetNode);
    }

    public void calculateReachabilityAndMarkBackEdges() {
        final Stack<RegionNode> currentPath = new Stack<>();
        calculateReachabilityAndMarkBackEdges(currentPath, startNode(), new HashSet<>());

        dominators = new Dominators<>(startNode(), RegionNode.NODE_COMPARATOR);
        regularFlowDominators = new Dominators<>(startNode(), RegionNode.NODE_COMPARATOR, t -> t.targetNode().getType() == RegionNode.BlockType.NORMAL);
    }

    private void calculateReachabilityAndMarkBackEdges(final Stack<RegionNode> aCurrentPath, final RegionNode aNode, final Set<RegionNode> aVisited) {
        if (aVisited.add(aNode)) {
            aCurrentPath.push(aNode);
            for (final Edge<ControlFlowEdgeType, RegionNode> theEdge : aNode.outgoingEdges().collect(Collectors.toList())) {
                final RegionNode theTarget = theEdge.targetNode();
                if (aCurrentPath.contains(theTarget)) {
                    // This is a back edge
                    theEdge.newTypeIs(ControlFlowEdgeType.back);
                    // We have already visited the back edge, so we do not to continue here
                    // As this would lead to an endless loop
                } else {
                    // Normal edge
                    // Continue with graph traversal
                    calculateReachabilityAndMarkBackEdges(aCurrentPath, theTarget, aVisited);
                }
            }
            aCurrentPath.pop();
        }
    }

    public RegionNode createAt(final BytecodeOpcodeAddress aAddress, final RegionNode.BlockType aType) {
        final RegionNode theNewBlock = new RegionNode(this, aType, program, aAddress);
        knownNodes.add(theNewBlock);
        return theNewBlock;
    }

    public RegionNode startNode() {
        return nodeStartingAt(BytecodeOpcodeAddress.START_AT_ZERO);
    }

    public RegionNode nodeStartingAt(final BytecodeOpcodeAddress aAddress) {
        for (final RegionNode theBlock : knownNodes) {
            if (Objects.equals(aAddress, theBlock.getStartAddress())) {
                return theBlock;
            }
        }
        throw new IllegalArgumentException("Unknown address : " + aAddress.getAddress());
    }

    public List<RegionNode.ExceptionHandler> exceptionHandlersStartingAt(final BytecodeOpcodeAddress aAddress) {
        final List<RegionNode.ExceptionHandler> theHandler = new ArrayList<>();
        final BytecodeProgram.FlowInformation theFlowinfo = program.getFlowInformation();
        if (theFlowinfo != null) {
            final BytecodeProgram theBytecode = theFlowinfo.getProgram();
            for (final BytecodeExceptionTableEntry theEntry : theBytecode.getExceptionHandlers()) {
                if (theEntry.getStartPC().equals(aAddress) && !theEntry.isFinally()) {
                    RegionNode.ExceptionHandler theMatchingHandler = null;
                    for (final RegionNode.ExceptionHandler theExisting : theHandler) {
                        if (theExisting.regionMatchesTo(theEntry)) {
                            theMatchingHandler = theExisting;
                        }
                    }
                    if (theMatchingHandler == null) {
                        theMatchingHandler = new RegionNode.ExceptionHandler(theEntry.getStartPC(), theEntry.getEndPc());
                        theHandler.add(theMatchingHandler);
                    }

                    theMatchingHandler.addCatchEntry(theEntry);
                }
            }
        }
        theHandler.sort((o1, o2) -> Integer.compare(o2.getEndPC().getAddress(),  o1.getEndPC().getAddress()));
        return theHandler;
    }

    public boolean isImmediatelyDominatedBy(final RegionNode aDominator, final RegionNode aNode) {
        return dominators.getIDom(aNode) == aDominator;
    }

    protected Set<RegionNode> dominatedNodesOf(final RegionNode aNode) {
        return dominators.domSetOf(aNode);
    }
}