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
}
