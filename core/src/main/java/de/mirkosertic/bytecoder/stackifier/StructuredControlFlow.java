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

import de.mirkosertic.bytecoder.ssa.EdgeType;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class StructuredControlFlow<T> {

    private final List<JumpArrow<T>> knownJumpArrows;
    private final List<T> nodesInOrder;

    StructuredControlFlow(final List<JumpArrow<T>> knownJumpArrows, final List<T> nodesInOrder) {
        this.knownJumpArrows = knownJumpArrows;
        this.nodesInOrder = nodesInOrder;
    }

    public List<T> getNodesInOrder() {
        return nodesInOrder;
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
            if (f.getEdgeType() == EdgeType.forward && f.getHead() == head) {
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
            if (f.getEdgeType() == EdgeType.back && indexOf(f.getHead()) == indexOf(head)) {
                backwardArrows.add(f);
            }
        }
        return backwardArrows;
    }

    void stackify() {
        final Stack<T> s = new Stack<>();
        for (final T v : nodesInOrder) {
            for (final JumpArrow<T> forward: forwardArrowsWithHead(v)) {
                if (!s.isEmpty()) {
                    while (indexOf(forward.getTail()) < indexOf(s.peek())) {
                        final T w = s.pop();
                        for (final JumpArrow<T> backward : backwardArrowsWithHead(w)) {
                            if (indexOf(backward.getTail()) > indexOf(v)) {
                                throw new IllegalArgumentException(String.format("{%d,%d} are head to head, arrow %d ", indexOf(v), indexOf(backward.getTail()), knownJumpArrows.indexOf(backward)));
                            } else {
                                backward.setNewTail(v);
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

    public void printDebug(final PrintStream pw) {
        pw.println("Original:");
        printDebug(pw, false);
        pw.println();
        pw.println("Stackified:");
        printDebug(pw, true);
        pw.println();
        pw.println("Data:");
        for (int i=0;i<nodesInOrder.size();i++) {
            pw.println(String.format(" %d %s", i, nodesInOrder.get(i)));
        }
        for (final JumpArrow<T> arrow : knownJumpArrows) {
            pw.println(String.format(" %s %d -> %d", arrow.getEdgeType(), indexOf(arrow.getTail()), indexOf(arrow.getHead())));
        }
    }

    private void printDebug(final PrintStream stream, final boolean newTail) {
        stream.print("        ");
        for (final T v : nodesInOrder) {
            stream.print(String.format("%3d", indexOf(v)));
            stream.print(" ");
        }
        stream.println();
        for (final JumpArrow<T> arrow : jumpArrowsSortedByTail()) {
            stream.print(String.format("%3d-%3d ", indexOf(arrow.getTail()) ,indexOf(arrow.getHead())));
            final T tail = newTail ? arrow.getNewTail() : arrow.getTail();
            final T head = arrow.getHead();
            if (arrow.getEdgeType() == EdgeType.forward) {
                for (int i=0;i<indexOf(tail);i++) {
                    stream.print("    ");
                }
                stream.print("  ");
                for (int i=indexOf(tail);i<indexOf(head);i++) {
                    stream.print("----");
                }
                stream.print(">");
            } else {
                for (int i=0;i<indexOf(head);i++) {
                    stream.print("    ");
                }
                stream.print("  <-");
                for (int i=indexOf(tail);i<indexOf(head)-1;i++) {
                    stream.print("----");
                }
                stream.print("---");
            }
            stream.println();
        }
    }

    private static class Block<T> {
        private final JumpArrow<T> arrow;
        private final int endsBefore;

        public Block(final JumpArrow<T> arrow, final int endsBefore) {
            this.arrow = arrow;
            this.endsBefore = endsBefore;
        }
    }

    public void writeStructuredControlFlow(final StructuredControlFlowWriter<T> writer) {

        // Filter single jumps to dominated nodes here
        final List<JumpArrow<T>> filteredJumpArrows = knownJumpArrows.stream()
                .filter(arrow -> {
                    switch (arrow.getEdgeType()) {
                        case back:
                            return true;
                        case forward:
                            if (indexOf(arrow.getTail()) + 1 == indexOf(arrow.getHead())) {
                                if (knownJumpArrows.stream().filter(
                                        t -> t.getEdgeType() == EdgeType.forward && t.getHead() == arrow.getHead()).count() == 1) {
                                    return false;
                                }
                                return true;
                            }
                            return true;
                        default:
                            throw new IllegalStateException();
                    }
                })
                .collect(Collectors.toList());

        // We need this stack to keep track of currently active blocks and loops
        final Stack<Block<T>> blockStack = new Stack<>();

        writer.begin();
        for (final T node : nodesInOrder) {

            // We need all starting blocks from here
            // Sorted by their head in descending order
            // So we can build a stack of blocks correctly
            final List<Block<T>> blocksStartingFromHere = filteredJumpArrows.stream()
                    .filter(t -> t.getEdgeType() == EdgeType.forward && t.getNewTail() == node)
                    .map(t -> new Block<>(t, indexOf(t.getHead())))
                    .collect(Collectors.toList());

            // Back-Edges form loops, so we have to collect them
            // and sort them correctly to place them at the right
            // position onto the stack
            final List<JumpArrow<T>> backEdgesToHere =  filteredJumpArrows.stream()
                    .filter(t -> t.getEdgeType() == EdgeType.back && t.getHead() == node)
                    .collect(Collectors.toList());

            if (!backEdgesToHere.isEmpty()) {
                // TODO: We have to join back edges to the same head
                for (final JumpArrow<T> back : backEdgesToHere) {
                    blocksStartingFromHere.add(new Block<>(back, indexOf(back.getNewTail())));
                }
            }

            // We sort the blocks by their closing position
            // we get sorted blocks from widest to smallest
            // We have top place the blocks in this exact order
            blocksStartingFromHere.sort((o1, o2) -> Integer.compare(o2.endsBefore, o1.endsBefore));

            while (!blockStack.isEmpty() && (blockStack.peek().endsBefore == indexOf(node))) {
                writer.closeBlock();
                blockStack.pop();
            }

            for (final Block<T> block : blocksStartingFromHere) {
                switch (block.arrow.getEdgeType()) {
                    case forward:
                        writer.beginBlockFor(block.arrow);
                        break;
                    case back:
                        writer.beginLoopFor(block.arrow);
                        break;
                    default:
                        throw new IllegalStateException();
                }
                blockStack.push(block);
            }

            writer.write(node);
        }

        while (!blockStack.isEmpty()) {
            writer.closeBlock();
            blockStack.pop();
        }

        writer.end();
    }
}
