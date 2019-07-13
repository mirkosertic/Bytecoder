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

public class GotoGraph {

    private final List<JumpArrow> knownJumpArrows;
    private final List<Integer> nodesInOrder;

    GotoGraph(final List<JumpArrow> knownJumpArrows, final List<Integer> nodesInOrder) {
        this.knownJumpArrows = knownJumpArrows;
        this.nodesInOrder = nodesInOrder;
    }

    private List<JumpArrow> forwardArrowsWithHead(final int head) {
        final List<JumpArrow> forwardArrows = new ArrayList<>();
        for (final JumpArrow f : knownJumpArrows) {
            if (f.getEdgeType() == EdgeType.forward && f.getHead() == head) {
                forwardArrows.add(f);
            }
        }
        forwardArrows.sort((o1, o2) -> Integer.compare(o2.getTail(), o1.getTail()));
        return forwardArrows;
    }

    private List<JumpArrow> jumpArrowsSortedByTail() {
        final List<JumpArrow> forwardArrows = new ArrayList<>(knownJumpArrows);
        forwardArrows.sort(Comparator.comparingInt(JumpArrow::getTail));
        return forwardArrows;
    }

    private List<JumpArrow> backwardArrowsWithHead(final int head) {
        final List<JumpArrow> backwardArrows = new ArrayList<>();
        for (final JumpArrow f : knownJumpArrows) {
            if (f.getEdgeType() == EdgeType.back && f.getHead() == head) {
                backwardArrows.add(f);
            }
        }
        return backwardArrows;
    }

    void stackify() {
        final Stack<Integer> s = new Stack<>();
        for (final int v : nodesInOrder) {
            for (final JumpArrow forward: forwardArrowsWithHead(v)) {
                if (!s.isEmpty()) {
                    while (forward.getTail() < s.peek()) {
                        final int w = s.pop();
                        for (final JumpArrow backward : backwardArrowsWithHead(w)) {
                            if (backward.getTail() > v) {
                                throw new IllegalArgumentException(String.format("{%d,%d} are head to head", v, backward.getTail()));
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
            final int w = s.pop();
            for (final JumpArrow backward : backwardArrowsWithHead(w)) {
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
        for (final Integer integer : nodesInOrder) {
            stream.print(String.format("%3d", integer));
            stream.print(" ");
        }
        stream.println();
        for (final JumpArrow arrow : jumpArrowsSortedByTail()) {
            stream.print(String.format("%3d-%3d ", arrow.getTail(),arrow.getHead()));
            final int tail = newTail ? arrow.getNewTail() : arrow.getTail();
            final int head = arrow.getHead();
            if (arrow.getEdgeType() == EdgeType.forward) {
                for (int i=0;i<tail;i++) {
                    stream.print("    ");
                }
                stream.print("  ");
                for (int i=tail;i<head;i++) {
                    stream.print("----");
                }
                stream.print(">");
            } else {
                for (int i=0;i<head;i++) {
                    stream.print("    ");
                }
                stream.print("  <-");
                for (int i=tail;i<head-1;i++) {
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
        int level = 0;
        for (final int node : nodesInOrder) {
            final List<JumpArrow> backedgesJumpingToHere = new ArrayList<>();
            final List<JumpArrow> backedgesJumpingFromHere = new ArrayList<>();
            for (final JumpArrow arrow : knownJumpArrows) {
                switch (arrow.getEdgeType()) {
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
            for (int i=0;i<backedgesJumpingToHere.size();i++) {
                stream.print(indent(level));
                stream.print("LOOP: {");
                stream.print("; Arrow ");
                stream.print(knownJumpArrows.indexOf(backedgesJumpingToHere.get(i)));
                stream.println();
                level++;
            }
            stream.print(indent(level));
            stream.print(String.format("Node %d", node));
            stream.println();

            for (int i=0;i<backedgesJumpingFromHere.size();i++) {
                level--;
                stream.print(indent(level));
                stream.println("}");
            }
        }
    }
}
