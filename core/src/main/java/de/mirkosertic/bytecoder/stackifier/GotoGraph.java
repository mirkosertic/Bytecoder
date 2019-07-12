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
    }

    private void printDebug(final PrintStream pw, final boolean newTail) {
        pw.print("        ");
        for (Integer integer : nodesInOrder) {
            pw.print(String.format("%3d", integer));
            pw.print(" ");
        }
        pw.println();
        for (final JumpArrow arrow : jumpArrowsSortedByTail()) {
            pw.print(String.format("%3d-%3d ", arrow.getTail(),arrow.getHead()));
            final int tail = newTail ? arrow.getNewTail() : arrow.getTail();
            final int head = arrow.getHead();
            if (arrow.getEdgeType() == EdgeType.forward) {
                for (int i=0;i<tail;i++) {
                    pw.print("    ");
                }
                pw.print("  ");
                for (int i=tail;i<head;i++) {
                    pw.print("----");
                }
                pw.print(">");
            } else {
                for (int i=0;i<head;i++) {
                    pw.print("    ");
                }
                pw.print("  <-");
                for (int i=tail;i<head-1;i++) {
                    pw.print("----");
                }
                pw.print("---");
            }
            pw.println();
        }
    }

}
