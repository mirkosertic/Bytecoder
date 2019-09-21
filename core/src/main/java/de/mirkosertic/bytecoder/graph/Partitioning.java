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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Partitioning<T extends Node<T, ? extends EdgeType>> {

    private final List<Set<T>> partitions;

    public Partitioning(final Set<T> nodes, final Predicate<Edge<EdgeType, T>> edgeFilter) {
        partitions = new ArrayList<>();

        final Set<T> theWorkList = new HashSet<>(nodes);
        while (!theWorkList.isEmpty()) {
            // We have our worklist and pick a random node from it and perform a DFS
            // All found nodes in this search are part of a partition
            // We remove the partition from the worklist and continue
            // till the worklist is empty.

            final T startNode = theWorkList.iterator().next();
            final Stack<T> currentPath = new Stack<>();
            currentPath.add(startNode);
            final Set<T> marked = new HashSet<>();
            marked.add(startNode);

            while(!currentPath.isEmpty()) {
                final T currentNode= currentPath.peek();
                final Set<T> theOutgoingNodes = currentNode.outgoingEdges()
                        .filter((Predicate<Edge<? extends EdgeType, ? extends Node>>) edge -> edgeFilter.test((Edge<EdgeType, T>) edge))
                        .map(t -> (T) (t).targetNode()).collect(Collectors.toSet());
                final Set<T> theIncomingNodes = currentNode.incomingEdges()
                        .filter((Predicate<Edge<? extends EdgeType, ? extends Node>>) edge -> edgeFilter.test((Edge<EdgeType, T>) edge))
                        .map(t -> (T) (t).sourceNode()).collect(Collectors.toSet());

                final Set<T> theConnectedNodes = new HashSet<>(theOutgoingNodes);
                theConnectedNodes.addAll(theIncomingNodes);

                if (!theConnectedNodes.isEmpty()) {
                    boolean somethingFound = false;
                    for (final T node : theConnectedNodes) {
                        if (marked.add(node)) {
                            currentPath.push(node);
                            somethingFound = true;
                        }
                    }
                    if (!somethingFound) {
                        currentPath.pop();
                    }
                } else {
                    currentPath.pop();
                }
            }

            partitions.add(marked);
            theWorkList.removeAll(marked);
        }
    }

    public List<Set<T>> partitions() {
        return partitions;
    }
}
