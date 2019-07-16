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

    private String indent(final int l) {
        final StringBuilder b = new StringBuilder();
        for (int i=0;i<l;i++) {
            b.append("    ");
        }
        return b.toString();
    }

    public void printStructurePseudoCode(final PrintStream stream) {
        stream.println("Program structure");
        stream.println();
        int level = 0;
        for (final T node : nodesInOrder) {
            final List<JumpArrow> forwardEdgesStartingFromHere = new ArrayList<>();
            final List<JumpArrow> forwardEdgesEndingHere = new ArrayList<>();
            final List<JumpArrow> backedgesJumpingToHere = new ArrayList<>();
            final List<JumpArrow> backedgesJumpingFromHere = new ArrayList<>();
            for (final JumpArrow arrow : knownJumpArrows) {
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
                level--;
                stream.print(indent(level));
                stream.println("}");
            }

            for (final JumpArrow arrow : backedgesJumpingToHere) {
                stream.print(indent(level));
                stream.print("LOOP: {");
                stream.print("; Arrow ");
                stream.print(knownJumpArrows.indexOf(arrow));
                stream.println();
                level++;
            }

            for (final JumpArrow jumpArrow : forwardEdgesStartingFromHere) {
                stream.print(indent(level));
                stream.print("BLOCK: {");
                stream.print("; Arrow ");
                stream.print(knownJumpArrows.indexOf(jumpArrow));
                stream.println();
                level++;
            }

            stream.print(indent(level));
            stream.print(String.format("Node %d", indexOf(node)));
            stream.println();

            for (int i=0;i<backedgesJumpingFromHere.size();i++) {
                level--;
                stream.print(indent(level));
                stream.println("}");
            }
        }
        if (level != 0) {
            throw new IllegalStateException("Indentation must be zero at the end of output!");
        }
    }
}
