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

import java.util.List;
import java.util.Map;
import java.util.Stack;

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
import de.mirkosertic.bytecoder.ssa.IFElseExpression;
import de.mirkosertic.bytecoder.ssa.IFExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;

public class Stackifier {

    public static abstract class StackifierStructuredControlFlowWriter extends StructuredControlFlowWriter<RegionNode> {

        private final Stackifier stackifier;
        private final Stack<RegionNode> nodes;

        public StackifierStructuredControlFlowWriter(final Stackifier stackifier) {
            this.stackifier = stackifier;
            this.nodes = new Stack<>();
        }

        private Expression potentiallyReplaceGoto(final Expression aExpression) {
            final RegionNode currentNode = nodes.peek();
            if (aExpression instanceof GotoExpression) {
                final GotoExpression theGoto = (GotoExpression) aExpression;
                final BytecodeOpcodeAddress theTarget = theGoto.jumpTarget();

                final RegionNode theTargetNode = stackifier.controlFlowGraph.nodeStartingAt(theTarget);

                if (stackifier.flow.indexOf(theTargetNode) == stackifier.flow.indexOf(currentNode) + 1 && theTargetNode.isOnlyReachableThruRegularFlow(currentNode)
                        && currentNode.getSuccessors().entrySet().stream().filter(t -> t.getValue().getType() == RegionNode.BlockType.NORMAL).count() == 1) {
                    // We are branching to the strictly dominated successor
                    // The goto can be removed
                    return null;
                }

                for (int i=hierarchy.size() - 1;i>=0;i--) {
                    final Block<RegionNode> block = hierarchy.get(i);

                    switch (block.getArrow().getEdgeType()) {
                    case back:
                        if (theTargetNode == block.getArrow().getHead()) {
                            return new ContinueExpression(
                                    theGoto.getProgram(),
                                    theGoto.getAddress(),
                                    block.getLabel(),
                                    theTarget
                            );
                        }
                        if (theTargetNode == block.getArrow().getNewTail()) {
                            return new BreakExpression(
                                    theGoto.getProgram(),
                                    theGoto.getAddress(),
                                    block.getLabel(),
                                    theTarget
                            );
                        }
                        break;
                    case forward:
                        if (theTargetNode == block.getArrow().getHead()) {
                            return new BreakExpression(
                                    theGoto.getProgram(),
                                    theGoto.getAddress(),
                                    block.getLabel(),
                                    theTarget
                            );
                        }
                        break;
                    default:
                        throw new IllegalArgumentException();
                    }
                }

                throw new IllegalStateException(String.format("Don't know how to handle Goto %s from %d to %d in %s", theTarget, stackifier.flow.indexOf(currentNode), stackifier.flow.indexOf(theTargetNode), currentNode.getStartAddress()));
            }
            return aExpression;
        }

        public final void writeExpressionList(final RegionNode currentNode, final ExpressionList aList) {
            final List<Expression> el = aList.toList();
            for (int i=0;i < el.size(); i++) {
                final Expression converted = potentiallyReplaceGoto(el.get(i));
                if (converted instanceof ExpressionListContainer) {
                    final ExpressionListContainer c = (ExpressionListContainer) converted;
                    for (final ExpressionList l : c.getExpressionLists()) {
                        for (final Expression k : l.toList()) {
                            final Expression conv = potentiallyReplaceGoto(k);
                            if (conv == null) {
                                l.remove(k);
                            } else if (conv != k) {
                                l.replace(k, conv);
                            }
                        }
                    }
                }
                if (converted != null) {

                    if (converted instanceof IFExpression) {
                        final IFExpression ie = (IFExpression) converted;
                        final ExpressionList elsePart = new ExpressionList();
                        for (int k = i+1; k<el.size();k++) {
                            final Expression c1 = potentiallyReplaceGoto(el.get(k));
                            if (c1 != null) {
                                elsePart.add(c1);
                            }
                        }

                        writeExpression(currentNode, new IFElseExpression(ie.getProgram(), ie.getAddress(), ie, elsePart));
                        return;
                    }

/*                    if (converted instanceof BreakExpression || converted instanceof GotoExpression) {
                        final RegionNode theNextBlock;
                        if (converted instanceof BreakExpression) {
                            final BreakExpression b = (BreakExpression) converted;
                            theNextBlock = stackifier.getControlFlowGraph().nodeStartingAt(b.jumpTarget());
                        } else {
                            final GotoExpression g = (GotoExpression) converted;
                            theNextBlock = stackifier.getControlFlowGraph().nodeStartingAt(g.jumpTarget());
                        }

                        if (theNextBlock.isStrictlyDominatedBy(currentNode)) {
                            final Set<RegionNode> dominatedSet = new HashSet<>();
                            dominatedSet.add(theNextBlock);
                            dominatedSet.addAll(theNextBlock.dominatedNodes());

                            int minIndex = Integer.MAX_VALUE;
                            int maxIndex = Integer.MIN_VALUE;
                            for (final RegionNode dom : dominatedSet) {
                                final int i = stackifier.flow.indexOf(dom);
                                // Sanity check
                                // Ignore nodes not known to stackifier
                                // That might be exceptional control flow nodes
                                // which are not visible to stackifier but
                                // part of the domination set
                                if (i>=0) {
                                    minIndex = Math.min(minIndex, i);
                                    maxIndex = Math.max(maxIndex, i);
                                }
                            }
                            final List<RegionNode> toBeProcessed = stackifier.flow.slice(minIndex, maxIndex);

                            beginBlockFor(new Block<>(new Label("lala"), new JumpArrow<RegionNode>(EdgeType.forward, toBeProcessed.get(0), stackifier.flow.nextOf(toBeProcessed.get(toBeProcessed.size() - 1)))));

                            stackifier.flow.writeStructuredControlFlow(this, toBeProcessed);

                            closeBlock();

                            for (final RegionNode dom : dominatedSet) {
                                markAsProcessed(dom);
                            }

                            return;
                        }

                        if (converted instanceof GotoExpression) {
                            if (stackifier.flow.indexOf(theNextBlock) == stackifier.flow.indexOf(currentNode) + 1 && theNextBlock.isOnlyReachableThruRegularFlow(currentNode)
                                    && currentNode.getSuccessors().entrySet().stream().filter(t -> t.getValue().getType() == RegionNode.BlockType.NORMAL).count() == 1) {
                                // We are branching to the strictly dominated successor
                                // The goto can be removed
                                return;
                            }
                        }
                    }*/

                    writeExpression(currentNode, converted);
                }
            }
        }

        public abstract void writeExpression(RegionNode currentNode, Expression e);

        @Override
        public void write(final RegionNode node) {
            nodes.push(node);
            writeExpressionList(node, node.getExpressions());
            nodes.pop();
        }
    }

    private final StructuredControlFlow<RegionNode> flow;
    private final ControlFlowGraph controlFlowGraph;

    public Stackifier(final ControlFlowGraph controlFlowGraph) throws IrreducibleControlFlowException {
        this.controlFlowGraph = controlFlowGraph;
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
        flow = builder.build();
    }

    public ControlFlowGraph getControlFlowGraph() {
        return controlFlowGraph;
    }

    public void writeStructuredControlFlow(final StackifierStructuredControlFlowWriter writer) {
        flow.writeStructuredControlFlow(writer);
    }
}