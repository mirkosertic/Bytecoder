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

import de.mirkosertic.bytecoder.asm.ControlTokenConsumer;
import de.mirkosertic.bytecoder.asm.Copy;
import de.mirkosertic.bytecoder.asm.EdgeType;
import de.mirkosertic.bytecoder.asm.Graph;
import de.mirkosertic.bytecoder.asm.If;
import de.mirkosertic.bytecoder.asm.MethodInvocation;
import de.mirkosertic.bytecoder.asm.Projection;
import de.mirkosertic.bytecoder.asm.Region;
import de.mirkosertic.bytecoder.asm.ReturnNothing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Sequencer {

    public static class Block {

        public enum Type {
            LOOP, NORMAL
        }

        private final String label;
        private final Type type;

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

    public Sequencer(final Graph g, final DominatorTree dominatorTree) {
        this.graph = g;
        this.dominatorTree = dominatorTree;
        final ControlTokenConsumer startNode = g.regionByLabel(Graph.START_REGION_NAME);
        visit(startNode, new ArrayList<>());
    }

    private void visit(final ControlTokenConsumer node, final List<Block> activeStack) {
        if (node instanceof Region) {
            visit((Region) node, activeStack);
        } else if (node instanceof MethodInvocation) {
            visit((MethodInvocation) node, activeStack);
        } else if (node instanceof Copy) {
            visit((Copy) node, activeStack);
        } else if (node instanceof If) {
            visit((If) node, activeStack);
        } else if (node instanceof ReturnNothing) {
            visit((ReturnNothing) node, activeStack);
        } else {
            throw new IllegalStateException("Not implemented : " + node.getClass().getSimpleName());
        }
    }

    private void printIndent(final int size) {
        for (int i = 0; i < size; i++) {
            System.out.print(" ");
        }
    }

    private void generateGOTO(final ControlTokenConsumer target, final List<Block> activeStack) {
        printIndent(activeStack.size());
        for (final Block b : activeStack) {
            if (b.breakLeadsTo == target) {
                System.out.println("Break " + b.label);
                return;
            }
            if (b.continueLeadsTo == target) {
                System.out.println("Continue " + b.label);
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

    private void visit(final MethodInvocation node, final List<Block> activeStack) {

        printIndent(activeStack.size());

        System.out.print(node.additionalDebugInfo());
        System.out.print(" // ");
        System.out.print(graph.nodes().indexOf(node));
        System.out.print(" ");
        System.out.println(node.getClass().getSimpleName());

        processSuccessors(node, activeStack);
    }

    private void visit(final Copy node, final List<Block> activeStack) {

        printIndent(activeStack.size());

        System.out.print("Copy ");
        System.out.print(node.additionalDebugInfo());
        System.out.print(" // ");
        System.out.print(graph.nodes().indexOf(node));
        System.out.print(" ");
        System.out.println(node.getClass().getSimpleName());

        processSuccessors(node, activeStack);
    }

    private void visit(final If node, final List<Block> activeStack) {

        printIndent(activeStack.size());

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

            System.out.print(newBlock.label);
            System.out.print(": ");
        } else {
            newStack.add(new Block(node.getClass().getSimpleName() + graph.nodes().indexOf(node), Block.Type.NORMAL, null, null));
        }

        System.out.print("If ");
        System.out.print(node.additionalDebugInfo());
        System.out.print(" // ");
        System.out.print(graph.nodes().indexOf(node));
        System.out.print(" ");
        System.out.println(node.getClass().getSimpleName());

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

        printIndent(activeStack.size());
        System.out.println("}");

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

        printIndent(activeStack.size());

        final Block b;
        if (hasIncomingBackEdges) {
            b = new Block(node.label, Block.Type.LOOP, node, null);
        } else {
            b = new Block(node.label, Block.Type.NORMAL, null, null);
        }
        final List<Block> newStackForDominatedNodes = new ArrayList<>(activeStack);
        newStackForDominatedNodes.add(b);

        System.out.print(node.label);
        System.out.print(": {  // ");
        System.out.print(graph.nodes().indexOf(node));
        System.out.print(" ");
        System.out.print(node.getClass().getSimpleName());
        System.out.print(" ");
        System.out.print(node.additionalDebugInfo());
        System.out.print(" Order : ");
        System.out.println(dominatorTree.getRpo().indexOf(node));

        processSuccessors(node, newStackForDominatedNodes);

        printIndent(activeStack.size());
        System.out.println("}");
    }

    private void visit(final ReturnNothing node, final List<Block> activeStack) {

        printIndent(activeStack.size());

        System.out.print("Return");
        System.out.print(" // ");
        System.out.print(graph.nodes().indexOf(node));
        System.out.print(" ");
        System.out.print(node.getClass().getSimpleName());
        System.out.print(" ");
        System.out.println(node.additionalDebugInfo());
    }
}
