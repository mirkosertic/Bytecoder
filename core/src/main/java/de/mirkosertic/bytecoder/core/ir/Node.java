/*
 * Copyright 2022 Mirko Sertic
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
package de.mirkosertic.bytecoder.core.ir;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class Node {

    public Node[] incomingDataFlows;

    boolean error;

    final Graph owner;

    public final NodeType nodeType;

    Node(final Graph owner, final NodeType nodeType) {
        this.owner = owner;
        this.nodeType = nodeType;
        this.incomingDataFlows = new Node[0];
    }

    public String additionalDebugInfo() {
        return "";
    }

    public void addIncomingData(final Node... nodes) {
        if (incomingDataFlows.length == 0) {
            incomingDataFlows = nodes;
        } else {
            final Node[] newData = new Node[incomingDataFlows.length + nodes.length];
            System.arraycopy(incomingDataFlows, 0, newData, 0, incomingDataFlows.length);
            System.arraycopy(nodes, 0, newData, incomingDataFlows.length, nodes.length);
            incomingDataFlows = newData;
        }
    }

    public Node[] outgoingDataFlows() {
        return owner.outgoingDataFlowsFor(this);
    }

    public final void remapDataFlow(final Node original, final Node newValue) {
        for (int i = 0; i < incomingDataFlows.length; i++) {
            if (incomingDataFlows[i] == original) {
                incomingDataFlows[i] = newValue;
            }
        }
    }

    public boolean isConstant() {
        return false;
    }

    private boolean hasSideEffectInternal(final Set<Node> visited) {
        if (hasSideSideEffect()) {
            return true;
        }
        if (visited.add(this)) {
            for (final Node n : incomingDataFlows) {
                if (n.hasSideEffectInternal(visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasSideSideEffect() {
        return false;
    }

    public boolean hasSideSideEffectRecursive() {
        return hasSideEffectInternal(new HashSet<>());
    }

    public abstract <T extends Node> T stampInto(final Graph target);

    public void sanityCheck() {
        for (final Node incoming : incomingDataFlows) {
            if (!owner.nodes().contains(incoming)) {
                throw new IllegalStateException("Incoming node " + incoming + " is not part of the graph for " + this.nodeType + "!");
            }
        }
    }

    public void removeFromIncomingData(final Node workingItem) {
        incomingDataFlows = Arrays.stream(incomingDataFlows).filter(t -> t != workingItem).toArray(Node[]::new);
    }
}
