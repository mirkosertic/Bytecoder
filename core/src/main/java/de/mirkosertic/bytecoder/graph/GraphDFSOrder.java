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
package de.mirkosertic.bytecoder.graph;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class GraphDFSOrder<T extends Node<? extends Node, ? extends EdgeType>> {

    private final List<T> nodesInOrder;

    public GraphDFSOrder(final T graphStartNode, final Comparator<T> nodeComparator, final Predicate<Edge<EdgeType, T>> edgeFilter) {
        final List<T> lastNodes = new ArrayList<>();
        final T startNode = graphStartNode;
        final Stack<T> currentPath = new Stack<>();
        currentPath.add(startNode);
        final Set<T> marked = new HashSet<>();
        marked.add(startNode);
        while(!currentPath.isEmpty()) {
            final T currentNode= currentPath.peek();
            final List<T> forwardNodes = currentNode.outgoingEdges()
                    .filter((Predicate<Edge<? extends EdgeType, ? extends Node>>) edge -> edgeFilter
                            .test((Edge<EdgeType, T>) edge))
                    .map(t -> (T) t.targetNode()).sorted(nodeComparator).collect(Collectors.toList());
            if (!forwardNodes.isEmpty()) {
                boolean somethingFound = false;
                for (final T node : forwardNodes) {
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

    public List<T> getNodesInOrder() {
        return nodesInOrder;
    }

    public void printDebug(final PrintWriter pw) {
        System.out.println("Topologic order:");
        for (final T node : nodesInOrder) {
            pw.print("    ");
            pw.print(node);
            pw.print(" SUCC : ");
            for (final Edge edge : node.outgoingEdges().collect(Collectors.toList())) {
                pw.print(edge.edgeType());
                pw.print(":");
                pw.print(edge.targetNode());
                pw.print(" ");
            }
            pw.println();
        }
        pw.flush();
    }

}
