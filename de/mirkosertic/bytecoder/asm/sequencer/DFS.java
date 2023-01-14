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
import de.mirkosertic.bytecoder.asm.EdgeType;
import de.mirkosertic.bytecoder.asm.Projection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class DFS {

    private final List<ControlTokenConsumer> nodesInOrder;

    public DFS(final ControlTokenConsumer startNode) {
        final List<ControlTokenConsumer> reversePostOrder = new ArrayList<>();
        final Stack<ControlTokenConsumer> currentPath = new Stack<>();
        currentPath.add(startNode);
        final Set<ControlTokenConsumer> marked = new HashSet<>();
        marked.add(startNode);
        while(!currentPath.isEmpty()) {
            final ControlTokenConsumer currentNode = currentPath.peek();
            final List<ControlTokenConsumer> forwardNodes = new ArrayList<>();
            for (final Map.Entry<Projection, ControlTokenConsumer> entry : currentNode.controlFlowsTo.entrySet()) {
                if (entry.getKey().edgeType() == EdgeType.FORWARD) {
                    forwardNodes.add(entry.getValue());
                }
            }
            if (!forwardNodes.isEmpty()) {
                boolean somethingFound = false;
                for (final ControlTokenConsumer node : forwardNodes) {
                    if (marked.add(node)) {
                        currentPath.push(node);
                        somethingFound = true;
                    }
                }
                if (!somethingFound) {
                    reversePostOrder.add(currentNode);
                    currentPath.pop();
                }
            } else {
                reversePostOrder.add(currentNode);
                currentPath.pop();
            }
        }

        nodesInOrder = new ArrayList<>();
        for (int i = reversePostOrder.size() - 1; i >= 0; i--) {
            nodesInOrder.add(reversePostOrder.get(i));
        }
    }

    public List<ControlTokenConsumer> getTopoligicalOrder() {
        return nodesInOrder;
    }
}
