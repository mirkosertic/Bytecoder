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
package de.mirkosertic.bytecoder.asm;

import org.objectweb.asm.Type;

public class Node {

    public final Type type;

    public Node[] incomingDataFlows;
    public Node[] outgoingFlows;

    boolean error;

    public Node(final Type type) {
        this.type = type;
        this.incomingDataFlows = new Node[0];
        this.outgoingFlows = new Node[0];
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
        for (final Node n : nodes) {
            n.addOutgoingData(this);
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
}
