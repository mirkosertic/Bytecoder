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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ControlTokenConsumer extends Node {

    public final Map<Projection, List<ControlTokenConsumer>> controlFlowsTo;
    public final List<ControlTokenConsumer> controlComingFrom;

    public ControlTokenConsumer() {
        controlFlowsTo = new HashMap<>();
        controlComingFrom = new ArrayList<>();
    }

    public void addControlFlowTo(final Projection projection, final ControlTokenConsumer node) {
        if (node == this) {
            throw new IllegalStateException("FIXME: Infinite control flow recursion");
        }
        if (controlFlowsTo.containsKey(projection)) {
            System.out.println("There is already a control flow with projection " + projection);
        }
        final List<ControlTokenConsumer> list = controlFlowsTo.computeIfAbsent(projection, t -> new ArrayList<>());
        list.add(node);
        node.controlComingFrom.add(this);
    }

    public ControlTokenConsumer flowForProjection(final Class<?> p) {
        for (final Map.Entry<Projection, List<ControlTokenConsumer>> entry : controlFlowsTo.entrySet()) {
            if (entry.getKey().getClass().isAssignableFrom(p)) {
                return entry.getValue().get(0);
            }
        }
        return null;
    }

    public void deleteControlFlowTo(final ControlTokenConsumer consumer) {
        final Set<Projection> keysToDelete = new HashSet<>();
        for (final Map.Entry<Projection, List<ControlTokenConsumer>> entry : controlFlowsTo.entrySet()) {
            if (entry.getValue().remove(consumer)) {
                consumer.controlComingFrom.remove(this);
            }
            if (entry.getValue().isEmpty()) {
                keysToDelete.add(entry.getKey());
            }
        }
        for (final Projection p : keysToDelete) {
            controlFlowsTo.remove(p);
        }
    }

    public void deleteControlFlowFrom(final ControlTokenConsumer consumer) {
        controlComingFrom.remove(consumer);
    }

}
