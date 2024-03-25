/*
 * Copyright 2023 Mirko Sertic
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
package de.mirkosertic.bytecoder.core.backend.sequencer;

import de.mirkosertic.bytecoder.core.ir.AbstractVar;
import de.mirkosertic.bytecoder.core.ir.ArrayStore;
import de.mirkosertic.bytecoder.core.ir.ClassInitialization;
import de.mirkosertic.bytecoder.core.ir.ControlTokenConsumer;
import de.mirkosertic.bytecoder.core.ir.Copy;
import de.mirkosertic.bytecoder.core.ir.FrameDebugInfo;
import de.mirkosertic.bytecoder.core.ir.Goto;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.If;
import de.mirkosertic.bytecoder.core.ir.LineNumberDebugInfo;
import de.mirkosertic.bytecoder.core.ir.LookupSwitch;
import de.mirkosertic.bytecoder.core.ir.MethodInvocation;
import de.mirkosertic.bytecoder.core.ir.MonitorEnter;
import de.mirkosertic.bytecoder.core.ir.MonitorExit;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.Projection;
import de.mirkosertic.bytecoder.core.ir.Region;
import de.mirkosertic.bytecoder.core.ir.Return;
import de.mirkosertic.bytecoder.core.ir.ReturnValue;
import de.mirkosertic.bytecoder.core.ir.SetClassField;
import de.mirkosertic.bytecoder.core.ir.SetInstanceField;
import de.mirkosertic.bytecoder.core.ir.TableSwitch;
import de.mirkosertic.bytecoder.core.ir.TryCatch;
import de.mirkosertic.bytecoder.core.ir.Unwind;

import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Sequencer {

    public static class Block {

        public enum Type {
            LOOP, NORMAL
        }

        public final String label;
        public final Type type;

        private final ControlTokenConsumer continueLeadsTo;

        private final ControlTokenConsumer breakLeadsTo;

        public Block(final String label, final Type type, final ControlTokenConsumer continueLeadsTo, final ControlTokenConsumer breakLeadsTo) {
            this.label = label;
            this.type = type;
            this.continueLeadsTo = continueLeadsTo;
            this.breakLeadsTo = breakLeadsTo;
        }
    }

    private final DominatorTree dominatorTree;

    private final Graph graph;

    private final StructuredControlflowCodeGenerator codegenerator;

    public Sequencer(final Graph g, final DominatorTree dominatorTree, final StructuredControlflowCodeGenerator codegenerator) {
        this.graph = g;
        this.dominatorTree = dominatorTree;
        this.codegenerator = codegenerator;
        final ControlTokenConsumer startNode = g.regionByLabel(Graph.START_REGION_NAME);

        final List<AbstractVar> variables = g.nodes().stream().filter(t -> t instanceof AbstractVar).map(t -> (AbstractVar) t).collect(Collectors.toList());
        codegenerator.registerVariables(variables);

        visitDominationTreeOf(startNode, new Stack<>());
    }

    private void visitDominationTreeOf(final ControlTokenConsumer startNode, final Stack<Block> activeStack) {
        final int startHeight = activeStack.size();

        ControlTokenConsumer current = startNode;

        final Function<ControlTokenConsumer, ControlTokenConsumer> followUpProcessor = controlTokenConsumer -> {
            for (final Map.Entry<Projection, ControlTokenConsumer> entry : controlTokenConsumer.controlFlowsTo.entrySet()) {
                if (dominatorTree.getIDom(entry.getValue()) == controlTokenConsumer) {
                    // We can continue to the child
                    return entry.getValue();
                }
                generateGOTO(controlTokenConsumer, entry.getValue(), activeStack);
            }
            return null;
        };

        while (current != null) {
            switch (current.nodeType) {
                case TryCatch: {
                    // Special-Case : branching
                    visit((TryCatch) current, activeStack);
                    current = null;
                    break;
                }
                case If: {
                    // Special-Case : branching
                    visit((If) current, activeStack);
                    current = null;
                    break;
                }
                case TableSwitch: {
                    // Special-Case : branching
                    visit((TableSwitch) current, activeStack);
                    current = null;
                    break;
                }
                case LookupSwitch: {
                    // Special-Case : branching
                    visit((LookupSwitch) current, activeStack);
                    current = null;
                    break;
                }
                case Region: {
                    visit((Region) current, activeStack);
                    current = followUpProcessor.apply(current);
                    break;
                }
                case MethodInvocation: {
                    codegenerator.write((MethodInvocation) current);
                    current = followUpProcessor.apply(current);
                    break;
                }
                case Copy: {
                    codegenerator.write((Copy) current);
                    current = followUpProcessor.apply(current);
                    break;
                }
                case Return: {
                    codegenerator.write((Return) current);
                    // We are finished here
                    current = null;
                    break;
                }
                case ReturnValue: {
                    codegenerator.write((ReturnValue) current);
                    // We are finished here
                    current = null;
                    break;
                }
                case SetInstanceField: {
                    codegenerator.write((SetInstanceField) current);
                    current = followUpProcessor.apply(current);
                    break;
                }
                case ArrayStore: {
                    codegenerator.write((ArrayStore) current);
                    current = followUpProcessor.apply(current);
                    break;
                }
                case SetClassField: {
                    codegenerator.write((SetClassField) current);
                    current = followUpProcessor.apply(current);
                    break;
                }
                case LineNumberDebugInfo: {
                    codegenerator.write((LineNumberDebugInfo) current);
                    current = followUpProcessor.apply(current);
                    break;
                }
                case FrameDebugInfo: {
                    codegenerator.write((FrameDebugInfo) current);
                    current = followUpProcessor.apply(current);
                    break;
                }
                case Goto: {
                    codegenerator.write((Goto) current);
                    current = followUpProcessor.apply(current);
                    break;
                }
                case MonitorEnter: {
                    codegenerator.write((MonitorEnter) current);
                    current = followUpProcessor.apply(current);
                    break;
                }
                case MonitorExit: {
                    codegenerator.write((MonitorExit) current);
                    current = followUpProcessor.apply(current);
                    break;
                }
                case Unwind: {
                    codegenerator.write((Unwind) current);
                    // We are finished here
                    current = null;
                    break;
                }
                case ClassInitialization: {
                    codegenerator.write((ClassInitialization) current);
                    current = followUpProcessor.apply(current);
                    break;
                }
                case Nop: {
                    current = followUpProcessor.apply(current);
                    break;
                }
                default:
                    throw new IllegalStateException("Unsupported node type : " + current.nodeType);
            }
        }

        while (activeStack.size() > startHeight) {
            codegenerator.finishBlock(activeStack.pop(), activeStack.isEmpty());
        }
    }

    private void generateGOTO(final ControlTokenConsumer currentToken, final ControlTokenConsumer target, final Stack<Block> activeStack) {
        for (final Block b : activeStack) {
            if (b.breakLeadsTo == target) {
                codegenerator.writeBreakTo(b.label);
                return;
            }
            if (b.continueLeadsTo == target) {
                codegenerator.writeContinueTo(b.label);
                return;
            }
        }
        throw new IllegalStateException("GOTO " + graph.nodes().indexOf(target) + " from " + graph.nodes().indexOf(currentToken) + " to " + target.getClass().getSimpleName() + " " + target.additionalDebugInfo());
    }

    private void visitBranchingNodeTemplate(final ControlTokenConsumer node, final Stack<Block> activeStack, final Consumer<Stack<Block>> nodeCallback) {

        final List<Node> nodes = graph.nodes();
        final List<ControlTokenConsumer> rpo = dominatorTree.getRpo();
        final List<ControlTokenConsumer> orderedBlocks = dominatorTree.immediatelyDominatedNodesOf(node)
                .stream()
                // We are only interested in merge nodes
                .filter(t -> t.controlComingFrom.size() > 1)
                // And sort them in reverse post order
                .sorted((o1, o2) -> {
                    final int a = rpo.indexOf(o1);
                    final int b = rpo.indexOf(o2);
                    if ((a == -1) || (b == 1)) {
                        throw new IllegalStateException("Don't know what to do");
                    }
                    return Integer.compare(b, a);
                })
                .collect(Collectors.toList());

        final boolean hasIncomingBackEdges = node.hasIncomingBackEdges();

        final String prefix = node.getClass().getSimpleName() + "_";
        final int selfIndex = nodes.indexOf(node);

        if (hasIncomingBackEdges) {
            final Block b = new Block(prefix + selfIndex, Block.Type.LOOP, node, null);
            activeStack.push(b);
            codegenerator.startBlock(b);
        }

        for (int i = 0; i < orderedBlocks.size(); i++) {
            final ControlTokenConsumer target = orderedBlocks.get(i);

            final Block newBlock = new Block(prefix + selfIndex + "_" + i, Block.Type.NORMAL, null, target);
            activeStack.push(newBlock);

            codegenerator.startBlock(newBlock);
        }

        nodeCallback.accept(activeStack);

        for (int i = orderedBlocks.size() - 1; i >= 0; i--) {
            final ControlTokenConsumer target = orderedBlocks.get(i);

            codegenerator.finishBlock(activeStack.pop(), activeStack.isEmpty());

            visitDominationTreeOf(target, activeStack);
        }

        if (hasIncomingBackEdges) {
            codegenerator.finishBlock(activeStack.pop(), activeStack.isEmpty());
        }
    }

    private void visit(final If node, final Stack<Block> activeStack) {
        visitBranchingNodeTemplate(node, activeStack, blocks -> {
            codegenerator.startIfWithTrueBlock(node);

            for (final Map.Entry<Projection, ControlTokenConsumer> entry : node.controlFlowsTo.entrySet()) {
                if (entry.getKey() instanceof Projection.TrueProjection) {
                    if (entry.getValue().controlComingFrom.size() > 1) {
                        // Merge nodes are handled in block form
                        generateGOTO(node, entry.getValue(), blocks);
                    } else {
                        visitDominationTreeOf(entry.getValue(), blocks);
                    }
                }
            }

            codegenerator.startIfElseBlock(node);

            for (final Map.Entry<Projection, ControlTokenConsumer> entry : node.controlFlowsTo.entrySet()) {
                if (entry.getKey() instanceof Projection.FalseProjection) {
                    if (entry.getValue().controlComingFrom.size() > 1) {
                        // Merge nodes are handled in block form
                        generateGOTO(node, entry.getValue(), blocks);
                    } else {
                        visitDominationTreeOf(entry.getValue(), blocks);
                    }
                }
            }

            codegenerator.finishIfBlock();
        });
    }

    private void visit(final TableSwitch node, final Stack<Block> as) {

        visitBranchingNodeTemplate(node, as, blocks -> {
            codegenerator.startTableSwitch(node);

            final List<Map.Entry<Projection, ControlTokenConsumer>> sortedProjections =
                    node.controlFlowsTo.entrySet().stream()
                            .filter(entry -> entry.getKey() instanceof Projection.IndexedProjection)
                            .sorted((o1, o2) -> {
                                final Projection.IndexedProjection a = (Projection.IndexedProjection) o1.getKey();
                                final Projection.IndexedProjection b = (Projection.IndexedProjection) o2.getKey();
                                return Integer.compare(a.index, b.index);

                            }).collect(Collectors.toList());

            for (final Map.Entry<Projection, ControlTokenConsumer> entry : sortedProjections) {
                final Projection.IndexedProjection indexedProjection = (Projection.IndexedProjection) entry.getKey();
                codegenerator.writeSwitchCase(indexedProjection.index);
                if (entry.getValue().controlComingFrom.size() > 1) {
                    // Merge nodes are handled in block form
                    generateGOTO(node, entry.getValue(), blocks);
                } else {
                    visitDominationTreeOf(entry.getValue(), blocks);
                }
                codegenerator.finishSwitchCase();
            }

            codegenerator.startTableSwitchDefaultBlock();

            for (final Map.Entry<Projection, ControlTokenConsumer> entry : node.controlFlowsTo.entrySet()) {
                if (entry.getKey() instanceof Projection.DefaultProjection) {
                    if (entry.getValue().controlComingFrom.size() > 1) {
                        // Merge nodes are handled in block form
                        generateGOTO(node, entry.getValue(), blocks);
                    } else {
                        visitDominationTreeOf(entry.getValue(), blocks);
                    }
                }
            }

            codegenerator.finishTableSwitchDefaultBlock();

            codegenerator.finishTableSwitch();
        });
    }

    private void visit(final LookupSwitch node, final Stack<Block> as) {

        visitBranchingNodeTemplate(node, as, blocks -> {
            codegenerator.startLookupSwitch(node);

            for (final Map.Entry<Projection, ControlTokenConsumer> entry : node.controlFlowsTo.entrySet()) {
                if (entry.getKey() instanceof Projection.KeyedProjection) {
                    final Projection.KeyedProjection indexedProjection = (Projection.KeyedProjection) entry.getKey();
                    codegenerator.writeSwitchCase(indexedProjection.key);

                    if (entry.getValue().controlComingFrom.size() > 1) {
                        // Merge nodes are handled in block form
                        generateGOTO(node, entry.getValue(), blocks);
                    } else {
                        visitDominationTreeOf(entry.getValue(), blocks);
                    }

                    codegenerator.finishSwitchCase();
                }
            }

            for (final Map.Entry<Projection, ControlTokenConsumer> entry : node.controlFlowsTo.entrySet()) {
                if (entry.getKey() instanceof Projection.DefaultProjection) {

                    codegenerator.writeSwitchDefaultCase();

                    if (entry.getValue().controlComingFrom.size() > 1) {
                        // Merge nodes are handled in block form
                        generateGOTO(node, entry.getValue(), blocks);
                    } else {
                        visitDominationTreeOf(entry.getValue(), blocks);
                    }

                    codegenerator.finishSwitchDefault();
                }
            }

            codegenerator.finishLookupSwitch();
        });
    }

    int tryCatchCounter = 0;

    private void visit(final TryCatch node, final Stack<Block> as) {

        visitBranchingNodeTemplate(node, as, blocks -> {

            final boolean hasExceptionHandler = node.controlFlowsTo.keySet().stream().anyMatch(t -> t instanceof Projection.ExceptionHandler);
            if (hasExceptionHandler) {
                codegenerator.startTryCatch("trycatch_" + tryCatchCounter++);
            }

            for (final Map.Entry<Projection, ControlTokenConsumer> entry : node.controlFlowsTo.entrySet()) {
                if (entry.getKey() instanceof Projection.TryCatchGuardedProjection) {
                    visitDominationTreeOf(entry.getValue(), blocks);
                }
            }

            if (hasExceptionHandler) {
                boolean first = true;
                final List<Map.Entry<Projection, ControlTokenConsumer>> sortedProjections =
                        node.controlFlowsTo.entrySet().stream()
                                .filter(entry -> entry.getKey() instanceof Projection.ExceptionHandler)
                                .sorted((o1, o2) -> {
                                    final Projection.ExceptionHandler a = (Projection.ExceptionHandler) o1.getKey();
                                    final Projection.ExceptionHandler b = (Projection.ExceptionHandler) o2.getKey();
                                    return Integer.compare(a.position, b.position);
                                }).collect(Collectors.toList());

                for (final Map.Entry<Projection, ControlTokenConsumer> handler : sortedProjections) {
                    if (first) {
                        first = false;
                        codegenerator.startCatchBlock();
                    }

                    final Projection.ExceptionHandler exceptionHandler = (Projection.ExceptionHandler) handler.getKey();

                    codegenerator.startCatchHandler(exceptionHandler.type);

                    visitDominationTreeOf(handler.getValue(), blocks);

                    codegenerator.finishCatchHandler();
                }

                codegenerator.writeRethrowException();

                codegenerator.finishTryCatch();
            }
        });
    }

    private void visit(final Region node, final Stack<Block> activeStack) {

        final boolean hasIncomingBackEdges = node.hasIncomingBackEdges();

        if (!Graph.START_REGION_NAME.equals(node.label) && hasIncomingBackEdges) {
            final Block b = new Block(node.label, Block.Type.LOOP, node, null);
            activeStack.push(b);
            codegenerator.startBlock(b);
        }
    }
}
