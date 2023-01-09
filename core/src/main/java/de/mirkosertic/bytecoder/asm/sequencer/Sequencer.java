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
import de.mirkosertic.bytecoder.asm.ControlTokenConsumer;
import de.mirkosertic.bytecoder.asm.Copy;
import de.mirkosertic.bytecoder.asm.EdgeType;
import de.mirkosertic.bytecoder.asm.Graph;
import de.mirkosertic.bytecoder.asm.If;
import de.mirkosertic.bytecoder.asm.InstanceMethodInvocation;
import de.mirkosertic.bytecoder.asm.Projection;
import de.mirkosertic.bytecoder.asm.Region;
import de.mirkosertic.bytecoder.asm.Return;
import de.mirkosertic.bytecoder.asm.ReturnValue;
import de.mirkosertic.bytecoder.asm.SetClassField;
import de.mirkosertic.bytecoder.asm.SetInstanceField;
import de.mirkosertic.bytecoder.asm.StaticMethodInvocation;
import de.mirkosertic.bytecoder.asm.VirtualMethodInvocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

        visit(startNode, new ArrayList<>());
    }

    private void visit(final ControlTokenConsumer node, final List<Block> activeStack) {
        if (node instanceof Region) {
            visit((Region) node, activeStack);
        } else if (node instanceof InstanceMethodInvocation) {
            visit((InstanceMethodInvocation) node, activeStack);
        } else if (node instanceof VirtualMethodInvocation) {
            visit((VirtualMethodInvocation) node, activeStack);
        } else if (node instanceof StaticMethodInvocation) {
            visit((StaticMethodInvocation) node, activeStack);
        } else if (node instanceof Copy) {
            visit((Copy) node, activeStack);
        } else if (node instanceof If) {
            visit((If) node, activeStack);
        } else if (node instanceof Return) {
            visit((Return) node, activeStack);
        } else if (node instanceof ReturnValue) {
            visit((ReturnValue) node, activeStack);
        } else if (node instanceof SetInstanceField) {
            visit((SetInstanceField) node, activeStack);
        } else if (node instanceof ArrayStore) {
            visit((ArrayStore) node, activeStack);
        } else if (node instanceof SetClassField) {
            visit((SetClassField) node, activeStack);
        } else {
            throw new IllegalStateException("Not implemented : " + node.getClass().getSimpleName());
        }
    }

    private void generateGOTO(final ControlTokenConsumer target, final List<Block> activeStack) {
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
        // TODO: We have to insert a goto here!
        System.out.println("GOTO " + graph.nodes().indexOf(target) + " " + target.getClass().getSimpleName() + " " + target.additionalDebugInfo());
    }

    private void processSuccessors(final ControlTokenConsumer node, final List<Block> activeStack) {
        for (final Map.Entry<Projection, List<ControlTokenConsumer>> entry : node.controlFlowsTo.entrySet()) {
            for (final ControlTokenConsumer successor : entry.getValue()) {
                if (dominatorTree.dominates(node, successor)) {
                    // We can continue to the child
                    visit(successor, activeStack);
                } else {
                    generateGOTO(successor, activeStack);
                }
            }
        }
    }

    private void visit(final InstanceMethodInvocation node, final List<Block> activeStack) {

        codegenerator.write(node);

        processSuccessors(node, activeStack);
    }

    private void visit(final VirtualMethodInvocation node, final List<Block> activeStack) {

        codegenerator.write(node);

        processSuccessors(node, activeStack);
    }

    private void visit(final StaticMethodInvocation node, final List<Block> activeStack) {

        codegenerator.write(node);

        processSuccessors(node, activeStack);
    }

    private void visit(final Copy node, final List<Block> activeStack) {

        codegenerator.write(node);

        processSuccessors(node, activeStack);
    }

    private void visit(final SetInstanceField node, final List<Block> activeStack) {

        codegenerator.write(node);

        processSuccessors(node, activeStack);
    }

    private void visit(final SetClassField node, final List<Block> activeStack) {

        codegenerator.write(node);

        processSuccessors(node, activeStack);
    }

    private void visit(final ArrayStore node, final List<Block> activeStack) {

        codegenerator.write(node);

        processSuccessors(node, activeStack);
    }

    private void visit(final If node, final List<Block> activeStack) {

        final Set<ControlTokenConsumer> dominated = dominatorTree.immediatelyDominatedNodesOf(node);
        final Map<ControlTokenConsumer, Set<ControlTokenConsumer>> groupDependencies = new HashMap<>();
        for (final ControlTokenConsumer dom : dominated) {
            final Set<ControlTokenConsumer> domset = dominatorTree.domSetOf(dom);
            for (final ControlTokenConsumer d : domset) {
                for (final Map.Entry<Projection, List<ControlTokenConsumer>> entry : d.controlFlowsTo.entrySet()) {
                    for (final ControlTokenConsumer target : entry.getValue()) {
                        if (!domset.contains(target) && dominated.contains(target)) {
                            // Jump out of the current domination set
                            final Set<ControlTokenConsumer> jumpTargets = groupDependencies.computeIfAbsent(target, x -> new HashSet<>());
                            jumpTargets.add(d);
                        }
                    }
                }
            }
        }

        final List<Block> newStack = new ArrayList<>(activeStack);
        if (groupDependencies.size() > 0) {
            if (groupDependencies.size() > 1) {
                throw new IllegalStateException("More than one post-if node.");
            }
            final ControlTokenConsumer next = groupDependencies.keySet().iterator().next();
            final Block newBlock = new Block("IF_" + graph.nodes().indexOf(node), Block.Type.NORMAL, null, next);
            newStack.add(newBlock);

            codegenerator.writeIfAndStartTrueBlock(node, newBlock.label);

        } else {
            newStack.add(new Block(node.getClass().getSimpleName() + graph.nodes().indexOf(node), Block.Type.NORMAL, null, null));

            codegenerator.writeIfAndStartTrueBlock(node, null);
        }

        for (final Map.Entry<Projection, List<ControlTokenConsumer>> entry : node.controlFlowsTo.entrySet()) {
            if (entry.getKey() instanceof Projection.TrueProjection) {
                for (final ControlTokenConsumer successor : entry.getValue()) {
                    if (dominatorTree.dominates(node, successor)) {
                        visit(successor, newStack);
                    } else {
                        generateGOTO(successor, newStack);
                    }
                }
            }
        }

        codegenerator.startIfElseBlock(node);

        for (final Map.Entry<Projection, List<ControlTokenConsumer>> entry : node.controlFlowsTo.entrySet()) {
            if (entry.getKey() instanceof Projection.FalseProjection) {
                for (final ControlTokenConsumer successor : entry.getValue()) {
                    if (dominatorTree.dominates(node, successor)) {
                        visit(successor, newStack);
                    } else {
                        generateGOTO(successor, newStack);
                    }
                }
            }
        }

        codegenerator.finishBlock();

        if (groupDependencies.size() > 0) {
            final ControlTokenConsumer next = groupDependencies.keySet().iterator().next();
            visit(next, activeStack);
        }
    }

    private void visit(final Region node, final List<Block> activeStack) {

        boolean hasIncomingBackEdges = false;
        for (final ControlTokenConsumer pred : node.controlComingFrom) {
            for (final Map.Entry<Projection, List<ControlTokenConsumer>> entry : pred.controlFlowsTo.entrySet()) {
                if (entry.getKey().edgeType() == EdgeType.BACK && entry.getValue().contains(node)) {
                    hasIncomingBackEdges = true;
                }
            }
        }

        final Block b;
        if (hasIncomingBackEdges) {
            b = new Block(node.label, Block.Type.LOOP, node, null);
        } else {
            b = new Block(node.label, Block.Type.NORMAL, null, null);
        }
        final List<Block> newStackForDominatedNodes = new ArrayList<>(activeStack);
        newStackForDominatedNodes.add(b);

        if (Graph.START_REGION_NAME.equals(b.label)) {

            processSuccessors(node, newStackForDominatedNodes);

        } else {
            codegenerator.startBlock(b);

            processSuccessors(node, newStackForDominatedNodes);

            codegenerator.finishBlock();
        }
    }

    private void visit(final Return node, final List<Block> activeStack) {

        codegenerator.write(node);
    }

    private void visit(final ReturnValue node, final List<Block> activeStack) {

        codegenerator.write(node);
    }

}
