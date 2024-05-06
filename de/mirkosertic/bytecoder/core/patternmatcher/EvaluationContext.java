/*
 * Copyright 2024 Mirko Sertic
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
package de.mirkosertic.bytecoder.core.patternmatcher;

import de.mirkosertic.bytecoder.core.ir.Node;

import java.util.HashMap;
import java.util.Map;

class EvaluationContext {

    private final Node[] nodeIndex;
    private final Map<Node, Node[]> outgoingFlows;

    EvaluationContext(final Node rootNode, final int indexSize) {
        this.nodeIndex = new Node[indexSize];
        this.nodeIndex[0] = rootNode;
        this.outgoingFlows = new HashMap<>();
    }

    Node getRoot() {
        return nodeIndex[0];
    }

    Node[] outgoingDataFlowsFor(final Node node) {
        return outgoingFlows.computeIfAbsent(node, Node::outgoingDataFlows);
    }

    boolean nodeKnownAt(final int index) {
        return nodeIndex[index] != null;
    }

    void registerNodeAt(final int index, final Node node) {
        nodeIndex[index] = node;
    }

    Node getNodeAt(final int index) {
        return nodeIndex[index];
    }
}
