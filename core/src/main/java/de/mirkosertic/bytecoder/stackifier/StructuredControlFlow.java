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

public class StructuredControlFlow<T> {

    private final List<JumpArrow<T>> knownJumpArrows;
    private final List<T> nodesInOrder;

    StructuredControlFlow(final List<JumpArrow<T>> knownJumpArrows, final List<T> nodesInOrder) {
        this.knownJumpArrows = knownJumpArrows;
        this.nodesInOrder = nodesInOrder;
    }

    private int indexOf(final T aValue) {
        return nodesInOrder.indexOf(aValue);
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
                                throw new IllegalArgumentException(String.format("{%d,%d} are head to head", indexOf(v), indexOf(backward.getTail())));
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

    public void writeStructuredControlFlow(final StructuredControlFlowWriter<T> writer) {

        // TODO: Filter single jumps to dominated nodes here

        writer.begin();
        for (final T node : nodesInOrder) {
            final List<JumpArrow<T>> forwardEdgesStartingFromHere = new ArrayList<>();
            final List<JumpArrow<T>> forwardEdgesEndingHere = new ArrayList<>();
            final List<JumpArrow<T>> backedgesJumpingToHere = new ArrayList<>();
            final List<JumpArrow<T>> backedgesJumpingFromHere = new ArrayList<>();
            for (final JumpArrow<T> arrow : knownJumpArrows) {
                switch (arrow.getEdgeType()) {
                    case forward:
                        if (arrow.getTail() == node) {
                            forwardEdgesStartingFromHere.add(arrow);
                        }
                        if (arrow.getHead() == node) {
                            forwardEdgesEndingHere.add(arrow);
                        }
                        break;
                    case back:
                        if (arrow.getHead() == node) {
                            backedgesJumpingToHere.add(arrow);
                        }
                        if (arrow.getTail() == node) {
                            backedgesJumpingFromHere.add(arrow);
                        }
                        break;
                }
            }

            for (int i=0;i<forwardEdgesEndingHere.size();i++) {
                writer.closeBlock();
            }

            for (final JumpArrow<T> arrow : backedgesJumpingToHere) {
                writer.beginLoopFor(arrow);
            }

            for (final JumpArrow<T> jumpArrow : forwardEdgesStartingFromHere) {
                writer.beginBlockFor(jumpArrow);
            }

            writer.write(node);

            for (int i=0;i<backedgesJumpingFromHere.size();i++) {
                writer.closeBlock();
            }
        }
        writer.end();
    }
}
