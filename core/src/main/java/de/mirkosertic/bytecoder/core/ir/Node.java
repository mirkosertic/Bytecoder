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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Node {

    public Node[] incomingDataFlows;
    public Node[] outgoingFlows;

    boolean error;

    boolean tobepruned;

    public Node() {
        this.incomingDataFlows = new Node[0];
        this.outgoingFlows = new Node[0];
    }

    public void markAsDoBePruned() {
        tobepruned = true;
    }

    public boolean isMarkedToBePruned() {
        return tobepruned;
    }

    public String additionalDebugInfo() {
        return "";
    }

    public void addIncomingData(final Node... nodes) {
        addIncomingDataNoInternal(nodes);
        for (final Node n : nodes) {
            n.addOutgoingData(this);
        }
    }

    private void addIncomingDataNoInternal(final Node... nodes) {
        if (incomingDataFlows.length == 0) {
            incomingDataFlows = nodes;
        } else {
            final Node[] newData = new Node[incomingDataFlows.length + nodes.length];
            System.arraycopy(incomingDataFlows, 0, newData, 0, incomingDataFlows.length);
            System.arraycopy(nodes, 0, newData, incomingDataFlows.length, nodes.length);
            incomingDataFlows = newData;
        }
    }

    public void addOutgoingData(final Node node) {
        if (outgoingFlows.length == 0) {
            outgoingFlows = new Node[] {node};
        } else {
            final Node[] newData = new Node[outgoingFlows.length + 1];
            System.arraycopy(outgoingFlows, 0, newData, 0, outgoingFlows.length);
            newData[outgoingFlows.length] = node;
            outgoingFlows = newData;
        }
    }

    public void removeFromOutgoingData(final Node node) {
        final List<Node> l = new ArrayList<>(Arrays.asList(outgoingFlows));
        l.remove(node);
        outgoingFlows = l.toArray(new Node[0]);
    }

    public void clearIncomingData() {
        for (final Node incoming : incomingDataFlows) {
            incoming.removeFromOutgoingData(this);
        }
        this.incomingDataFlows = new Node[0];
    }

    public boolean remapDataFlow(final Node original, final Node newValue) {
        boolean changed = false;
        for (int i = 0; i < incomingDataFlows.length; i++) {
            if (incomingDataFlows[i] == original) {
                incomingDataFlows[i] = newValue;
                newValue.addOutgoingData(this);
                changed = true;
            }
        }
        for (int i = 0; i < outgoingFlows.length; i++) {
            if (outgoingFlows[i] == original) {
                outgoingFlows[i] = newValue;
                newValue.addIncomingDataNoInternal(this);
                changed = true;
            }
        }
        return changed;
    }

    public void removeFromIncomingData(final Node node) {
        final List<Node> l = new ArrayList<>(Arrays.asList(incomingDataFlows));
        l.remove(node);
        incomingDataFlows = l.toArray(new Node[0]);
    }
}
