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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import de.mirkosertic.bytecoder.ssa.ControlFlowEdgeType;
import de.mirkosertic.bytecoder.ssa.Label;

public class StructuredControlFlow<T> {

    private final List<JumpArrow<T>> knownJumpArrows;
    private final List<T> nodesInOrder;

    StructuredControlFlow(final List<JumpArrow<T>> knownJumpArrows, final List<T> nodesInOrder) {
        this.knownJumpArrows = knownJumpArrows;
        this.nodesInOrder = nodesInOrder;
    }

    public int indexOf(final T aValue) {
        return nodesInOrder.indexOf(aValue);
    }

    public int indexOf(final JumpArrow<T> arrow) {
        return knownJumpArrows.indexOf(arrow);
    }

    private List<JumpArrow<T>> forwardArrowsWithHead(final T head) {
        final List<JumpArrow<T>> forwardArrows = new ArrayList<>();
        for (final JumpArrow<T> f : knownJumpArrows) {
            if (f.getEdgeType() == ControlFlowEdgeType.forward && f.getHead() == head) {
                forwardArrows.add(f);
            }
        }
        forwardArrows.sort((o1, o2) -> Integer.compare(indexOf(o2.getTail()), indexOf(o1.getTail())));
        return forwardArrows;
    }

    private List<JumpArrow<T>> jumpArrowsSortedByTail() {
        final List<JumpArrow<T>> forwardArrows = new ArrayList<>(knownJumpArrows);
        forwardArrows.sort(Comparator.comparingInt(value -> indexOf(value.getTail())));
        return forwardArrows;
    }

    private List<JumpArrow<T>> backwardArrowsWithHead(final T head) {
        final List<JumpArrow<T>> backwardArrows = new ArrayList<>();
        for (final JumpArrow<T> f : knownJumpArrows) {
            if (f.getEdgeType() == ControlFlowEdgeType.back && indexOf(f.getHead()) == indexOf(head)) {
                backwardArrows.add(f);
            }
        }
        return backwardArrows;
    }

    void stackify() throws HeadToHeadControlFlowException {
        final Stack<T> s = new Stack<>();
        for (final T v : nodesInOrder) {
            for (final JumpArrow<T> forward: forwardArrowsWithHead(v)) {
                if (!s.isEmpty()) {
                    while (indexOf(forward.getTail()) < indexOf(s.peek())) {
                        final T w = s.pop();
                        for (final JumpArrow<T> backward : backwardArrowsWithHead(w)) {
                            if (indexOf(backward.getTail()) > indexOf(v)) {
                                throw new HeadToHeadControlFlowException(String.format("{%d,%d} are head to head, arrow %d ", indexOf(v), indexOf(backward.getTail()), knownJumpArrows.indexOf(backward)));
                            } else {
                                backward.setNewTail(nodesInOrder.get(indexOf(v) - 1));
                            }
                        }
                    }
                    forward.setNewTail(s.peek());
                }
            }
            s.push(v);
        }
        while (!s.isEmpty()) {
            final T w = s.pop();
            for (final JumpArrow<T> backward : backwardArrowsWithHead(w)) {
                backward.setNewTail(nodesInOrder.get(nodesInOrder.size() - 1));
            }
        }
    }

    public void printDebug(final PrintWriter pw) {
        pw.println("Original:");
        printDebug(pw, false);
        for (final JumpArrow<T> arrow : knownJumpArrows) {
            pw.println(String.format(" %s %d -> %d", arrow.getEdgeType(), indexOf(arrow.getTail()), indexOf(arrow.getHead())));
        }
        pw.println();
        pw.println("Stackified:");
        printDebug(pw, true);
        for (final JumpArrow<T> arrow : knownJumpArrows) {
            pw.println(String.format(" %s %d -> %d", arrow.getEdgeType(), indexOf(arrow.getNewTail()), indexOf(arrow.getHead())));
        }
        pw.println();
        pw.println("Data:");
        for (int i=0;i<nodesInOrder.size();i++) {
            pw.println(String.format(" %d %s", i, nodesInOrder.get(i)));
        }
        pw.flush();
    }

    private void printDebug(final PrintWriter pw, final boolean newTail) {
        pw.print("        ");
        for (final T v : nodesInOrder) {
            pw.print(String.format("%3d", indexOf(v)));
            pw.print(" ");
        }
        pw.println();
        for (final JumpArrow<T> arrow : jumpArrowsSortedByTail()) {
            pw.print(String.format("%3d-%3d ", indexOf(arrow.getTail()) ,indexOf(arrow.getHead())));
            final T tail = newTail ? arrow.getNewTail() : arrow.getTail();
            final T head = arrow.getHead();
            if (arrow.getEdgeType() == ControlFlowEdgeType.forward) {
                for (int i=0;i<indexOf(tail);i++) {
                    pw.print("    ");
                }
                pw.print("  ");
                for (int i=indexOf(tail);i<indexOf(head);i++) {
                    pw.print("----");
                }
                pw.print(">");
            } else {
                for (int i=0;i<indexOf(head);i++) {
                    pw.print("    ");
                }
                pw.print("  <-");
                for (int i=indexOf(tail);i<indexOf(head)-1;i++) {
                    pw.print("----");
                }
                pw.print("---");
            }
            pw.println();
        }
    }

    private Label toLabel(final JumpArrow<T> arrow) {
        switch (arrow.getEdgeType()) {
            case forward:
                return new Label(String.format("B_%d_%d", indexOf(arrow.getNewTail()), indexOf(arrow.getHead())));
            case back:
                return new Label(String.format("L_%d_%d", indexOf(arrow.getHead()), indexOf(arrow.getNewTail())));
            default:
                throw new IllegalArgumentException();
        }
    }

    private boolean contains(final JumpArrow<T> loop, final JumpArrow<T> block) {
        final int loopHeadIdx = indexOf(loop.getHead());
        final int loopTailIdx = indexOf(loop.getNewTail());

        final int blockHeadIdx = indexOf(block.getHead());
        final int blockTailIdx = indexOf(block.getNewTail());

        if (blockTailIdx >= loopHeadIdx &&
            blockTailIdx <= loopTailIdx &&
            blockHeadIdx >= loopHeadIdx &&
            blockHeadIdx <= loopTailIdx) {
            return true;
        }

        return false;
    }

    public void writeStructuredControlFlow(final StructuredControlFlowWriter<T> writer) {

        writer.begin();

        writeStructuredControlFlow(writer, nodesInOrder);

        writer.end();
    }

    private boolean endsBefore(final Block<T> block, final T node) {
        switch (block.getArrow().getEdgeType()) {
            case forward:
                return indexOf(block.getEnding()) <= indexOf(node);
            case back:
                return indexOf(block.getEnding()) < indexOf(node);
        }
        throw new IllegalArgumentException();
    }

    public void writeStructuredControlFlow(final StructuredControlFlowWriter<T> writer, final List<T> nodes) {

        // We have to filter some arrows to prevent confusion while
        // generating the output

        // TODO: This one is really ugly and should be replaced by real graph dominance tests
        final List<JumpArrow<T>> filteredJumpArrows = knownJumpArrows.stream()
                .filter(arrow -> {
                    switch (arrow.getEdgeType()) {
                    case back:
                        return true;
                    case forward:
                        if (indexOf(arrow.getTail()) + 1 == indexOf(arrow.getHead())) {
                            // Forward jumps to a strictly dominated successor do not start a block
                            if (knownJumpArrows.stream().filter(t -> t.getTail() == arrow.getTail()).count() == 1) {
                                return false;
                            }
                            // Also forward jumps out of a loop do not create new blocks
                            // As the loop can always be exited
                            if (knownJumpArrows.stream().filter(
                                    t -> t.getEdgeType() == ControlFlowEdgeType.back && t.getNewTail() == arrow.getNewTail()).count() == 1) {
                                return false;
                            }

                            return true;
                        }

                        // Jumps out of a loop to the loops direct successor also does not create a block
                        // Those Jumps are always possible by breaking the loop
                        for (JumpArrow<T> a : knownJumpArrows) {
                            if (a.getEdgeType() == ControlFlowEdgeType.back &&
                                    indexOf(arrow.getNewTail()) >= indexOf(a.getHead()) &&
                                    indexOf(arrow.getNewTail()) <= indexOf(a.getNewTail()) &&
                                    indexOf(arrow.getHead()) == indexOf(a.getNewTail()) + 1 &&
                                    !contains(a, arrow)) {
                                return false;
                            }
                        }

                        return true;
                    default:
                        throw new IllegalStateException();
                    }
                })
                .collect(Collectors.toList());

        // We need this stack to keep track of currently active blocks and loops
        final Stack<Block<T>> blockStack = new Stack<>();

        for (final T node : nodes) {

            // We need all starting blocks from here
            // Sorted by their head in descending order
            // So we can build a stack of blocks correctly
            final Set<Block<T>> uniqueBlocksStartingFromHere = filteredJumpArrows.stream()
                    .filter(t -> t.getEdgeType() == ControlFlowEdgeType.forward && t.getNewTail() == node)
                    .map(t -> new Block<>(toLabel(t), t))
                    .collect(Collectors.toSet());

            // Back-Edges form loops, so we have to collect them
            // and sort them correctly to place them at the right
            // position onto the stack
            final List<JumpArrow<T>> backEdgesToHere =  filteredJumpArrows.stream()
                    .filter(t -> t.getEdgeType() == ControlFlowEdgeType.back && t.getHead() == node)
                    .collect(Collectors.toList());

            // TODO: We have to join back edges to the same head
            for (final JumpArrow<T> back : backEdgesToHere) {
                uniqueBlocksStartingFromHere.add(new Block<>(toLabel(back), back));
            }

            // We have a uniqze List of Blocks here, so we convert them to a list so we can sort them
            final List<Block<T>> blocksStartingFromHere = new ArrayList<>(uniqueBlocksStartingFromHere);

            // We sort the blocks by their closing position
            // we get sorted blocks from widest to smallest
            // We have top place the blocks in this exact order
            blocksStartingFromHere.sort((o1, o2) -> {
                final int a = o1.getArrow().getEdgeType() == ControlFlowEdgeType.forward ? indexOf(o1.getEnding()) : indexOf(o1.getEnding()) + 1;
                final int b = o2.getArrow().getEdgeType() == ControlFlowEdgeType.forward ? indexOf(o2.getEnding()) : indexOf(o2.getEnding()) + 1;
                return Integer.compare(b, a);
            });

            while (!blockStack.isEmpty() && (endsBefore(blockStack.peek(), node))) {
                writer.closeBlock();
                blockStack.pop();
            }

            if (!blockStack.isEmpty() && (indexOf(blockStack.peek().getEnding()) == indexOf(node)) && (blockStack.peek().getArrow().getEdgeType() == ControlFlowEdgeType.back)) {

                for (final Block<T> block : blocksStartingFromHere) {
                    switch (block.arrow.getEdgeType()) {
                        case forward:
                            // Forward jumps are only possible to the end
                            // of Blocks on the stack
                            check: {
                                for (final Block<T> onStack : blockStack) {
                                    if (onStack.getArrow().getEdgeType() == ControlFlowEdgeType.forward &&
                                        onStack.getEnding() == block.getEnding()) {
                                        // Jump is possible
                                        break check;
                                    }
                                }
                                // No viable option found, we give up
                                printDebug(new PrintWriter(System.out));
                                throw new IllegalStateException(String.format("Don't know what to do for node %s. Closing loop with starting blocks at the same place!", node));
                            }
                            break;
                        case back:
                            // Can happen in case of self loops
                            writer.beginLoopFor(block);
                            blockStack.push(block);
                            break;
                        default:
                            throw new IllegalStateException();
                    }
                }

                writer.write(node);

                while (!blockStack.isEmpty() && (indexOf(blockStack.peek().getEnding()) == indexOf(node)) && (blockStack.peek().getArrow().getEdgeType() == ControlFlowEdgeType.back)) {
                    writer.closeBlock();
                    blockStack.pop();
                }

            } else {

                for (final Block<T> block : blocksStartingFromHere) {
                    switch (block.arrow.getEdgeType()) {
                    case forward:
                        writer.beginBlockFor(block);
                        break;
                    case back:
                        writer.beginLoopFor(block);
                        break;
                    default:
                        throw new IllegalStateException();
                    }
                    blockStack.push(block);
                }

                writer.write(node);
            }
        }

        while (!blockStack.isEmpty()) {
            writer.closeBlock();
            blockStack.pop();
        }
    }
}
