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
package de.mirkosertic.bytecoder.asm.sequencer;

import de.mirkosertic.bytecoder.asm.AbstractVar;
import de.mirkosertic.bytecoder.asm.ArrayStore;
import de.mirkosertic.bytecoder.asm.CheckCast;
import de.mirkosertic.bytecoder.asm.ControlTokenConsumer;
import de.mirkosertic.bytecoder.asm.Copy;
import de.mirkosertic.bytecoder.asm.FrameDebugInfo;
import de.mirkosertic.bytecoder.asm.Goto;
import de.mirkosertic.bytecoder.asm.Graph;
import de.mirkosertic.bytecoder.asm.If;
import de.mirkosertic.bytecoder.asm.InstanceMethodInvocation;
import de.mirkosertic.bytecoder.asm.LineNumberDebugInfo;
import de.mirkosertic.bytecoder.asm.LookupSwitch;
import de.mirkosertic.bytecoder.asm.MonitorEnter;
import de.mirkosertic.bytecoder.asm.MonitorExit;
import de.mirkosertic.bytecoder.asm.Node;
import de.mirkosertic.bytecoder.asm.Projection;
import de.mirkosertic.bytecoder.asm.Region;
import de.mirkosertic.bytecoder.asm.Return;
import de.mirkosertic.bytecoder.asm.ReturnValue;
import de.mirkosertic.bytecoder.asm.SetClassField;
import de.mirkosertic.bytecoder.asm.SetInstanceField;
import de.mirkosertic.bytecoder.asm.StaticMethodInvocation;
import de.mirkosertic.bytecoder.asm.TableSwitch;
import de.mirkosertic.bytecoder.asm.TryCatch;
import de.mirkosertic.bytecoder.asm.Unwind;
import de.mirkosertic.bytecoder.asm.VirtualMethodInvocation;

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

        final List<AbstractVar> phis = g.nodes().stream().filter(t -> t instanceof AbstractVar).map(t -> (AbstractVar) t).collect(Collectors.toList());
        codegenerator.registerVariables(phis);

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
            if (current instanceof TryCatch) {
                // Special-Case : branching
                visit((TryCatch) current, activeStack);
                current = null;
            } else if (current instanceof If) {
                // Special-Case : branching
                visit((If) current, activeStack);
                current = null;
            } else if (current instanceof TableSwitch) {
                // Special-Case : branching
                visit((TableSwitch) current, activeStack);
                current = null;
            } else if (current instanceof LookupSwitch) {
                // Special-Case : branching
                visit((LookupSwitch) current, activeStack);
                current = null;
            } else {
                // Regular case
                if (current instanceof Region) {
                    visit((Region) current, activeStack);
                    current = followUpProcessor.apply(current);
                } else if (current instanceof InstanceMethodInvocation) {
                    codegenerator.write((InstanceMethodInvocation) current);
                    current = followUpProcessor.apply(current);
                } else if (current instanceof VirtualMethodInvocation) {
                    codegenerator.write((VirtualMethodInvocation) current);
                    current = followUpProcessor.apply(current);
                } else if (current instanceof StaticMethodInvocation) {
                    codegenerator.write((StaticMethodInvocation) current);
                    current = followUpProcessor.apply(current);
                } else if (current instanceof Copy) {
                    codegenerator.write((Copy) current);
                    current = followUpProcessor.apply(current);
                } else if (current instanceof Return) {
                    codegenerator.write((Return) current);
                    // We are finished here
                    current = null;
                } else if (current instanceof ReturnValue) {
                    codegenerator.write((ReturnValue) current);
                    // We are finished here
                    current = null;
                } else if (current instanceof SetInstanceField) {
                    codegenerator.write((SetInstanceField) current);
                    current = followUpProcessor.apply(current);
                } else if (current instanceof ArrayStore) {
                    codegenerator.write((ArrayStore) current);
                    current = followUpProcessor.apply(current);
                } else if (current instanceof SetClassField) {
                    codegenerator.write((SetClassField) current);
                    current = followUpProcessor.apply(current);
                } else if (current instanceof LineNumberDebugInfo) {
                    codegenerator.write((LineNumberDebugInfo) current);
                    current = followUpProcessor.apply(current);
                } else if (current instanceof FrameDebugInfo) {
                    codegenerator.write((FrameDebugInfo) current);
                    current = followUpProcessor.apply(current);
                } else if (current instanceof Goto) {
                    codegenerator.write((Goto) current);
                    current = followUpProcessor.apply(current);
                } else if (current instanceof MonitorEnter) {
                    codegenerator.write((MonitorEnter) current);
                    current = followUpProcessor.apply(current);
                } else if (current instanceof MonitorExit) {
                    codegenerator.write((MonitorExit) current);
                    current = followUpProcessor.apply(current);
                } else if (current instanceof Unwind) {
                    // We are finished here
                    codegenerator.write((Unwind) current);
                    current = null;
                } else if (current instanceof CheckCast) {
                    codegenerator.write((CheckCast) current);
                    current = followUpProcessor.apply(current);
                } else {
                    throw new IllegalStateException("Not implemented : " + current.getClass().getSimpleName());
                }
            }
        }

        while (activeStack.size() > startHeight) {
            final Block pop = activeStack.pop();
            codegenerator.finishBlock();
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
        codegenerator.writeDebugNote("GOTO " + graph.nodes().indexOf(target) + " from " + graph.nodes().indexOf(currentToken) + " to " + target.getClass().getSimpleName() + " " + target.additionalDebugInfo());
        // TODO: We have to insert a goto here!
        System.out.println("GOTO " + graph.nodes().indexOf(target) + " from " + graph.nodes().indexOf(currentToken) + " to " + target.getClass().getSimpleName() + " " + target.additionalDebugInfo());
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

            codegenerator.finishBlock();
            activeStack.pop();

            visitDominationTreeOf(target, activeStack);
        }

        if (hasIncomingBackEdges) {
            codegenerator.finishBlock();
            activeStack.pop();
        }

    }

    private void visit(final If node, final Stack<Block> activeStack) {
        visitBranchingNodeTemplate(node, activeStack, blocks -> {
            codegenerator.writeIfAndStartTrueBlock(node);

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

            codegenerator.finishBlock();
        });
    }

    private void visit(final TableSwitch node, final Stack<Block> as) {

        visitBranchingNodeTemplate(node, as, blocks -> {
            codegenerator.writeSwitch(node);

            for (final Map.Entry<Projection, ControlTokenConsumer> entry : node.controlFlowsTo.entrySet()) {
                if (entry.getKey() instanceof Projection.IndexedProjection) {
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
                    codegenerator.finishSwitchCase();
                }
            }

            codegenerator.finishBlock();
        });
    }

    private void visit(final LookupSwitch node, final Stack<Block> as) {

        visitBranchingNodeTemplate(node, as, blocks -> {
            codegenerator.writeSwitch(node);

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
                    codegenerator.finishSwitchCase();
                }
            }

            codegenerator.finishBlock();
        });
    }

    private void visit(final TryCatch node, final Stack<Block> as) {

        visitBranchingNodeTemplate(node, as, blocks -> {

            final boolean hasExceptionHandler = node.controlFlowsTo.keySet().stream().anyMatch(t -> t instanceof Projection.ExceptionHandler);
            codegenerator.startTryCatch(null);

            for (final Map.Entry<Projection, ControlTokenConsumer> entry : node.controlFlowsTo.entrySet()) {
                if (entry.getKey() instanceof Projection.TryCatchGuardedProjection) {
                    visitDominationTreeOf(entry.getValue(), blocks);
                }
            }

            if (hasExceptionHandler) {
                boolean first = true;
                for (final Map.Entry<Projection, ControlTokenConsumer> handler : node.controlFlowsTo.entrySet()) {
                    if (handler.getKey() instanceof Projection.ExceptionHandler) {
                        if (first) {
                            first = false;
                            codegenerator.startCatchBlock();
                        }

                        final Projection.ExceptionHandler exceptionHandler = (Projection.ExceptionHandler) handler.getKey();

                        codegenerator.startCatchHandler(exceptionHandler.type);

                        visitDominationTreeOf(handler.getValue(), blocks);

                        codegenerator.endCatchHandler();
                    }
                }
            }

            codegenerator.finishBlock();
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
