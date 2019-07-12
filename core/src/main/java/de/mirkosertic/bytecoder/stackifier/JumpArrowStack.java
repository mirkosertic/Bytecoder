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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class JumpArrowStack {

    private final List<JumpArrow> knownJumpArrows;

    public JumpArrowStack() {
        knownJumpArrows = new ArrayList<>();
    }

    public void add(final EdgeType edgeType, final int source, final int destination) {
        knownJumpArrows.add(new JumpArrow(edgeType, source, destination));
    }

    private List<JumpArrow> forwardArrowsWithHead(final int head) {
        final List<JumpArrow> forwardArrows = new ArrayList<>();
        for (final JumpArrow f : knownJumpArrows) {
            if (f.getEdgeType() == EdgeType.forward && f.getHead() == head) {
                forwardArrows.add(f);
            }
        }
        Collections.sort(forwardArrows, (o1, o2) -> Integer.compare(o2.getTail(), o1.getTail()));
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

    public void stackify(final List<Integer> nodesInOrder) {
        final Stack<Integer> s = new Stack<>();
        for (final int v : nodesInOrder) {
            for (final JumpArrow forward: forwardArrowsWithHead(v)) {
                if (!s.isEmpty()) {
                    while (forward.getTail() < s.peek()) {
                        final int w = s.pop();
                        for (final JumpArrow backward : backwardArrowsWithHead(w)) {
                            if (backward.getTail() > v) {
                                throw new IllegalArgumentException("" + v + "," + backward + " are head-to-head");
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
}
