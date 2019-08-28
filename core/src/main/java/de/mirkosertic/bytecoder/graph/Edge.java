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

public class Edge<TYPE extends EdgeType, TARGET extends Node> {

    private Node sourceNode;
    private TYPE edgeType;
    private TARGET targetNode;

    public Edge(final Node aSourceNode, final TYPE aType, final TARGET aTargetNode) {
        sourceNode = aSourceNode;
        edgeType = aType;
        targetNode = aTargetNode;
    }

    public TYPE edgeType() {
        return edgeType;
    }

    public Node sourceNode() {
        return sourceNode;
    }

    public TARGET targetNode() {
        return targetNode;
    }

    public void newSourceIs(final Node aNewSource) {
        sourceNode = aNewSource;
    }

    public void newTargetId(final TARGET aNewTarget) {
        targetNode = aNewTarget;
    }

    public void newTypeIs(final TYPE aEdgeType) {
        edgeType = aEdgeType;
    }
}