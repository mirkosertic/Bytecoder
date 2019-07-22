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
import de.mirkosertic.bytecoder.ssa.ControlFlowGraphDFSOrder;
import de.mirkosertic.bytecoder.ssa.EdgeType;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Stackifier {

    public StructuredControlFlow<RegionNode> stackify(final ControlFlowGraph controlFlowGraph) throws IrreducibleControlFlowException {
        final ControlFlowGraphDFSOrder order = new ControlFlowGraphDFSOrder(controlFlowGraph);
        final List<RegionNode> sorted = order.getNodesInOrder();
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
        try {
            flow.writeStructuredControlFlow(new StructuredControlFlowWriter<RegionNode>() {

                @Override
                public void write(final RegionNode node) {
                    replaceGotosIn(flow, nodeAdresses, node, node.getExpressions(), hierarchy, true);
                }
            });
        } catch (final RuntimeException e) {
            order.printDebug(new PrintWriter(System.out));
            throw e;
        }

        flow.writeStructuredControlFlow(new StructuredControlFlowWriter<RegionNode>() {

            @Override
            public void write(final RegionNode node) {
                replaceGotosIn(flow, nodeAdresses, node, node.getExpressions(), hierarchy, false);
            }
        });

        return flow;
    }

    private void replaceGotosIn(final StructuredControlFlow<RegionNode> flow, final Map<BytecodeOpcodeAddress, RegionNode> nodeAdresses, final RegionNode currentNode, final ExpressionList aList, final Stack<Block<RegionNode>> hierarchy, final boolean testMode) {
        expressiontest: for (final Expression theExpression : aList.toList()) {
            if (theExpression instanceof ExpressionListContainer) {
                final ExpressionListContainer container = (ExpressionListContainer) theExpression;
                for (final ExpressionList innerList : container.getExpressionLists()) {
                    replaceGotosIn(flow, nodeAdresses, currentNode, innerList, hierarchy, testMode);
                }
            }
            if (theExpression instanceof GotoExpression) {
                final GotoExpression theGoto = (GotoExpression) theExpression;
                final BytecodeOpcodeAddress theTarget = theGoto.getJumpTarget();

                final RegionNode theTargetNode = nodeAdresses.get(theTarget);

                if (flow.indexOf(theTargetNode) == flow.indexOf(currentNode) + 1 && theTargetNode.isOnlyReachableThru(currentNode)) {
                    // We are branching to the strictly dominated successor
                    // The goto can be removed
                    if (!testMode) {
                        aList.remove(theGoto);
                    }
                    continue expressiontest;
                }

                for (int i=hierarchy.size() - 1;i>=0;i--) {
                    final Block<RegionNode> block = hierarchy.get(i);

                    switch (block.getArrow().getEdgeType()) {
                        case forward:
                            if (theTargetNode == block.getArrow().getHead()) {
                                if (!testMode) {
                                    aList.replace(theGoto, new BreakExpression(
                                            theGoto.getProgram(),
                                            theGoto.getAddress(),
                                            block.getLabel(),
                                            theTarget
                                    ));
                                }

                                continue expressiontest;
                            }
                            break;
                        case back:
                            if (theTargetNode == block.getArrow().getHead()) {
                                if (!testMode) {
                                    aList.replace(theGoto, new ContinueExpression(
                                            theGoto.getProgram(),
                                            theGoto.getAddress(),
                                            block.getLabel(),
                                            theTarget
                                    ));
                                }

                                continue expressiontest;
                            }
                            if (theTargetNode == block.getArrow().getNewTail()) {
                                if (!testMode) {
                                    aList.replace(theGoto, new BreakExpression(
                                            theGoto.getProgram(),
                                            theGoto.getAddress(),
                                            block.getLabel(),
                                            theTarget
                                    ));
                                }

                                continue expressiontest;
                            }
                            break;
                        default:
                            throw new IllegalStateException();
                    }
                }

                flow.printDebug(new PrintWriter(System.out));
                flow.writeStructuredControlFlow(new DebugStructurecControlFlowWriter(new PrintWriter(System.out)));
                throw new IllegalStateException(String.format("Don't know how to handle Goto %s from %d to %d in %s", theTarget, flow.indexOf(currentNode), flow.indexOf(theTargetNode), currentNode.getStartAddress()));
            }
        }
    }
}
