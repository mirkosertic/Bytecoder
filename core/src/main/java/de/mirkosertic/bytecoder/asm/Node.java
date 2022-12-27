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
import org.objectweb.asm.tree.analysis.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Node implements Value {

    public final Type type;

    public final List<Node> incomingDataFlows;
    public final List<Node> outgoingFlows;

    boolean error;

    public Node(final Type type) {
        this.type = type;
        this.incomingDataFlows = new ArrayList<>();
        this.outgoingFlows = new ArrayList<>();
    }

    @Override
    public int getSize() {
        return this.type != Type.LONG_TYPE && this.type != Type.DOUBLE_TYPE ? 1 : 2;
    }

    public void addIncomingData(final Node... nodes) {
        Collections.addAll(incomingDataFlows, nodes);
        for (final Node n : nodes) {
            n.addOutgoingData(this);
        }
    }

    public void addOutgoingData(final Node node) {
        if (!outgoingFlows.contains(node)) {
            outgoingFlows.add(node);
        }
    }
}
