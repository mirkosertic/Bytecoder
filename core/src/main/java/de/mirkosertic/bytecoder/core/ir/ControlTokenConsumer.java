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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class ControlTokenConsumer extends Node {

    public final Map<Projection, ControlTokenConsumer> controlFlowsTo;
    public final Set<ControlTokenConsumer> controlComingFrom;

    ControlTokenConsumer(final Graph owner, final NodeType nodeType) {
        super(owner, nodeType);
        controlFlowsTo = new HashMap<>();
        controlComingFrom = new HashSet<>();
    }

    public void addControlFlowTo(final Projection projection, final ControlTokenConsumer node) {
        if (node == this) {
            throw new IllegalStateException("FIXME: Infinite control flow recursion");
        }

        controlFlowsTo.put(projection, node);
        node.controlComingFrom.add(this);
    }

    public boolean hasIncomingBackEdges() {
        for (final ControlTokenConsumer pred : controlComingFrom) {
            for (final Map.Entry<Projection, ControlTokenConsumer> entry : pred.controlFlowsTo.entrySet()) {
                if (entry.getKey().edgeType() == EdgeType.BACK && entry.getValue() == this) {
                    return true;
                }
            }
        }
        return false;
    }
}
