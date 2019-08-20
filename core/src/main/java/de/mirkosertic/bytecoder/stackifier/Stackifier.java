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

import java.io.PrintWriter;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.graph.Edge;
import de.mirkosertic.bytecoder.graph.GraphDFSOrder;
import de.mirkosertic.bytecoder.ssa.BreakExpression;
import de.mirkosertic.bytecoder.ssa.ContinueExpression;
import de.mirkosertic.bytecoder.ssa.ControlFlowEdgeType;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
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

                if (stackifier.flow.indexOf(theTargetNode) == stackifier.flow.indexOf(currentNode) + 1 && stackifier.controlFlowGraph.dominatesInRegularFlowOnly(currentNode, theTargetNode)
                        && currentNode.outgoingEdges().filter(t -> t.targetNode().getType() == RegionNode.BlockType.NORMAL).count() == 1) {
                    // We are branching to the strictly dominated successor
                    // The goto can be removed
                    return null;
                }

                int numLoops = 0;
                for (int i=hierarchy.size() - 1;i>=0;i--) {
                    final Block<RegionNode> block = hierarchy.get(i);

                    switch (block.getArrow().getEdgeType()) {
                    case back:
                        numLoops++;
                        if (theTargetNode == block.getArrow().getHead()) {
                            final ContinueExpression theContinue =  new ContinueExpression(
                                    theGoto.getProgram(),
                                    theGoto.getAddress(),
                                    block.getLabel(),
                                    theTarget
                            );
                            if (numLoops == 1) {
                                theContinue.noJumpLabelRequired();
                            }
                            return theContinue;
                        }
                        if (theTargetNode == block.getArrow().getNewTail()) {
                            final BreakExpression theBreak = new BreakExpression(
                                    theGoto.getProgram(),
                                    theGoto.getAddress(),
                                    block.getLabel(),
                                    theTarget
                            );
                            if (numLoops == 1) {
                                theBreak.noJumpLabelRequired();
                            }
                            return theBreak;
                        }
                        break;
                    case forward:
                        if (theTargetNode == block.getArrow().getHead()) {
                            final BreakExpression theBreak = new BreakExpression(
                                    theGoto.getProgram(),
                                    theGoto.getAddress(),
                                    block.getLabel(),
                                    theTarget
                            );
                            return theBreak;
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

        private void replaceGotosAndEnhanceIFExpressions(final ExpressionList aList) {
            final List<Expression> el = aList.toList();
            for (int i=0;i < el.size(); i++) {
                final Expression original = el.get(i);
                final Expression converted = potentiallyReplaceGoto(original);
                if (converted instanceof ExpressionListContainer) {
                    final ExpressionListContainer c = (ExpressionListContainer) converted;
                    for (final ExpressionList l : c.getExpressionLists()) {
                        replaceGotosAndEnhanceIFExpressions(l);
                    }
                }
                if (converted != null) {

                    if (converted instanceof IFExpression) {
                        final IFExpression ie = (IFExpression) converted;
                        final ExpressionList elsePart = new ExpressionList();
                        for (int k = i+1; k<el.size();k++) {
                            final Expression elsePartElement = el.get(k);
                            elsePart.add(elsePartElement);
                            aList.remove(elsePartElement);
                        }

                        replaceGotosAndEnhanceIFExpressions(elsePart);

                        aList.replace(original, new IFElseExpression(ie.getProgram(), ie.getAddress(), ie, elsePart));
                        return;
                    }

                    if (original != converted) {
                        aList.replace(original, converted);
                    }

                } else {
                    aList.remove(original);
                }
            }

        }

        public final void writeExpressionList(final RegionNode currentNode, final ExpressionList aList) {
            replaceGotosAndEnhanceIFExpressions(aList);
            for (final Expression e : aList.toList()) {
                writeExpression(currentNode, e);
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

    public Stackifier(final ControlFlowGraph controlFlowGraph) throws HeadToHeadControlFlowException {
        this.controlFlowGraph = controlFlowGraph;
        final GraphDFSOrder<RegionNode> order = new GraphDFSOrder(controlFlowGraph.startNode(),
                RegionNode.NODE_COMPARATOR,
                RegionNode.FORWARD_EDGE_FILTER_REGULAR_FLOW_ONLY);
        final List<RegionNode> sorted = order.getNodesInOrder();
        final StructuredControlFlowBuilder<RegionNode> builder = new StructuredControlFlowBuilder<>(sorted);
        for (final RegionNode node : sorted) {
            for (final Edge<ControlFlowEdgeType, RegionNode> succ : node.outgoingEdges().collect(Collectors.toList())) {
                switch (succ.edgeType()) {
                case forward:
                    if (sorted.contains(succ.targetNode())) {
                        builder.add(ControlFlowEdgeType.forward, node, succ.targetNode());
                    }
                    break;
                case back:
                    if (sorted.contains(succ.targetNode())) {
                        builder.add(ControlFlowEdgeType.back, node, succ.targetNode());
                    }
                    break;
                default:
                    throw new IllegalStateException();
                }
            }
        }
        flow = builder.build();
    }

    public void writeStructuredControlFlow(final StackifierStructuredControlFlowWriter writer) {
        flow.writeStructuredControlFlow(writer);
    }

    public void printDebug(final PrintWriter printWriter) {
        flow.printDebug(printWriter);
    }
}