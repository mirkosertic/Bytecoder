/*
 * Copyright 2018 Mirko Sertic
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
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Node<V extends Node, E extends EdgeType> {

    private final List<Edge<E, V>> outgoingEdges;
    private final List<Edge<E, V>> incomingEdges;

    public Node() {
        outgoingEdges = new ArrayList<>();
        incomingEdges = new ArrayList<>();
    }

    protected void addIncomingEdge(final Edge aEdge) {
        incomingEdges.add(aEdge);
    }

    public <T extends Edge<E, V>> Stream<T> outgoingEdges() {
        return (Stream<T>) outgoingEdges.stream();
    }

    public <T extends Edge<E, V>> Stream<T> outgoingEdges(final Predicate<E> aPredicate) {
        return (Stream<T>) outgoingEdges.stream().filter(t -> aPredicate.test(t.edgeType()));
    }

    public <T extends Node> T addEdgeTo(final E aType, final T aTargetNode) {
        final Edge theNewEdge = new Edge(this, aType, aTargetNode);
        outgoingEdges.add(theNewEdge);
        aTargetNode.addIncomingEdge(theNewEdge);
        return aTargetNode;
    }

    public <T extends Edge<E, V>> Stream<T> incomingEdges(final Predicate<E> aPredicate) {
        return (Stream<T>) incomingEdges.stream().filter(t -> aPredicate.test(t.edgeType()));
    }

    public <T extends Edge<E, V>> Stream<T> incomingEdges() {
        return (Stream<T>) incomingEdges.stream();
    }

    public <T extends Node> Optional<T> singleOutgoingNodeMatching(final Predicate<E> aPredicate) {
        final List<Edge> theEdges = outgoingEdges(aPredicate).collect(Collectors.toList());
        if (theEdges.isEmpty()) {
            return Optional.empty();
        }
        if (theEdges.size() > 1) {
            throw new IllegalStateException("Too many edges found!");
        }
        return Optional.of((T) theEdges.get(0).targetNode());
    }
}