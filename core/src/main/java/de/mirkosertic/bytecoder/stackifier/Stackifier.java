/*
 * Copyright 2019 Mirko Sertic
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
package de.mirkosertic.bytecoder.stackifier;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.ssa.BreakExpression;
import de.mirkosertic.bytecoder.ssa.ContinueExpression;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraphRegionNodeTopologicOrder;
import de.mirkosertic.bytecoder.ssa.EdgeType;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Stackifier {

    public StructuredControlFlow<RegionNode> stackify(final ControlFlowGraph controlFlowGraph) {
        final List<RegionNode> sorted = new ControlFlowGraphRegionNodeTopologicOrder(controlFlowGraph).getNodesInOrder();
        final StructuredControlFlowBuilder<RegionNode> builder = new StructuredControlFlowBuilder<>(sorted);
        for (final RegionNode node : sorted) {
            for (final Map.Entry<RegionNode.Edge, RegionNode> succ : node.getSuccessors().entrySet()) {
                switch (succ.getKey().getType()) {
                    case forward:
                        if (sorted.contains(succ.getValue())) {
                            builder.add(EdgeType.forward, node, succ.getValue());
                        }
                        break;
                    case back:
                        if (sorted.contains(succ.getValue())) {
                            builder.add(EdgeType.back, node, succ.getValue());
                        }
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }
        }
        final StructuredControlFlow<RegionNode> flow = builder.build();

        final Map<BytecodeOpcodeAddress, RegionNode> nodeAdresses = new HashMap<>();
        for (final RegionNode theNode : flow.getNodesInOrder()) {
            nodeAdresses.put(theNode.getStartAddress(), theNode);
        }

        // Now we have to replace all the gotos in the code
        flow.writeStructuredControlFlow(new StructuredControlFlowWriter<RegionNode>() {

            @Override
            public void write(final RegionNode node) {
                replaceGotosIn(flow, nodeAdresses, node, node.getExpressions(), hierarchy);
            }
        });

        return flow;
    }

    private void replaceGotosIn(final StructuredControlFlow<RegionNode> flow, final Map<BytecodeOpcodeAddress, RegionNode> nodeAdresses, final RegionNode currentNode, final ExpressionList aList, final Stack<Block<RegionNode>> hierarchy) {
        for (final Expression theExptession : aList.toList()) {
            if (theExptession instanceof ExpressionListContainer) {
                final ExpressionListContainer container = (ExpressionListContainer) theExptession;
                for (final ExpressionList innerList : container.getExpressionLists()) {
                    replaceGotosIn(flow, nodeAdresses, currentNode, innerList, hierarchy);
                }
            }
            if (theExptession instanceof GotoExpression) {
                final GotoExpression theGoto = (GotoExpression) theExptession;
                final BytecodeOpcodeAddress theTarget = theGoto.getJumpTarget();

                final RegionNode theTargetNode = nodeAdresses.get(theTarget);
                final int currentIndex = flow.indexOf(currentNode);
                final int targetIndex = flow.indexOf(theTargetNode);

                if (theTargetNode.isStrictlyDominatedBy(currentNode)) {
                    // We are branching to the successor
                    // The goto can be replaced
                    aList.remove(theGoto);
                    continue;
                }

                if (targetIndex <= currentIndex) {
                    // Back-Edge, we create a continue
                    for (int i=hierarchy.size() - 1;i>=0;i--) {
                        final Block<RegionNode> block = hierarchy.get(i);
                        final JumpArrow<RegionNode> arrow = block.getArrow();
                        if (arrow.getEdgeType() == EdgeType.back) {
                            if (arrow.getHead() == theTargetNode) {
                                // We create a continue here
                                aList.replace(theGoto, new ContinueExpression(
                                        theGoto.getProgram(),
                                        theGoto.getAddress(),
                                        block.getLabel(),
                                        theTarget
                                ));
                            }
                        }
                    }
                    throw new IllegalStateException("UNDEFINED LOOP TO " + theTarget.getAddress());
                }
                // Normal edge, we create a break
                for (final Block<RegionNode> block : hierarchy) {
                    final JumpArrow<RegionNode> arrow = block.getArrow();
                    if (arrow.getEdgeType() == EdgeType.forward) {
                        if (arrow.getHead() == theTargetNode) {
                            aList.replace(theGoto, new BreakExpression(
                                    theGoto.getProgram(),
                                    theGoto.getAddress(),
                                    block.getLabel(),
                                    theTarget
                            ));
                        }
                    }
                }

                // We break out of the complete hierarchy
                aList.replace(theGoto, new BreakExpression(
                        theGoto.getProgram(),
                        theGoto.getAddress(),
                        hierarchy.get(0).getLabel(),
                        theTarget
                ));
            }
        }
    }
}
