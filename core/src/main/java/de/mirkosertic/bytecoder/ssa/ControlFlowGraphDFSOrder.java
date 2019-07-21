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
package de.mirkosertic.bytecoder.ssa;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

public class ControlFlowGraphDFSOrder {

    private final List<RegionNode> nodesInOrder;

    public ControlFlowGraphDFSOrder(final ControlFlowGraph graph) {
        final List<RegionNode> lastNodes = new ArrayList<>();
        final RegionNode startNode = graph.startNode();
        final Stack<RegionNode> currentPath = new Stack<>();
        currentPath.add(startNode);
        final Set<RegionNode> marked = new HashSet<>();
        while(!currentPath.isEmpty()) {
            final RegionNode currentNode= currentPath.peek();
            final List<RegionNode> forwardNodes = currentNode.getSuccessors().entrySet().stream()
                    .filter(t -> t.getKey().getType() == EdgeType.forward && t.getValue().getType() == RegionNode.BlockType.NORMAL)
                    .map(Map.Entry::getValue)
                    .sorted(Comparator.comparingInt(o -> o.getStartAddress().getAddress()))
                    .collect(Collectors.toList());
            if (!forwardNodes.isEmpty()) {
                boolean somethingFound = false;
                for (final RegionNode node : forwardNodes) {
                    if (marked.add(node)) {
                        currentPath.push(node);
                        somethingFound = true;
                    }
                }
                if (!somethingFound) {
                    lastNodes.add(currentNode);
                    currentPath.pop();
                }
            } else {
                lastNodes.add(currentNode);
                currentPath.pop();
            }
        }
        nodesInOrder = new ArrayList<>();
        for (int i=lastNodes.size()- 1; i>=0; i--) {
            nodesInOrder.add(lastNodes.get(i));
        }
    }

    public List<RegionNode> getNodesInOrder() {
        return nodesInOrder;
    }

    public void printDebug(final PrintWriter pw) {
        System.out.println("Topologic order:");
        for (final RegionNode node : nodesInOrder) {
            pw.print("    ");
            pw.print(node.getStartAddress());
            pw.print(" SUCC : ");
            for (final Map.Entry<RegionNode.Edge, RegionNode> theEntry : node.getSuccessors().entrySet()) {
                pw.print(theEntry.getKey().getType());
                pw.print(":");
                pw.print(theEntry.getValue().getStartAddress());
                pw.print(" ");
            }
            pw.println();
        }
        pw.flush();
    }
}
