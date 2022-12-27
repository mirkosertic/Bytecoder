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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public abstract class ControlTokenConsumer extends Node {

    final Map<Projection, List<ControlTokenConsumer>> controlFlowsTo;

    public ControlTokenConsumer(final Type type) {
        super(type);
        controlFlowsTo = new HashMap<>();
    }

    public void addControlFlowTo(final Projection projection, final ControlTokenConsumer node) {
        if (node == this) {
            System.out.println("FIXME: Infinite control flow recursion");
            return;
        }
        if (controlFlowsTo.containsKey(projection)) {
            System.out.println("There is already a control flow with projection " + projection);
        }
        final List<ControlTokenConsumer> list = controlFlowsTo.computeIfAbsent(projection, t -> new ArrayList<>());
        list.add(node);
    }

    public ControlTokenConsumer flowForProjection(final Class<?> p) {
        for (final Map.Entry<Projection, List<ControlTokenConsumer>> entry : controlFlowsTo.entrySet()) {
            if (entry.getKey().getClass().isAssignableFrom(p)) {
                return entry.getValue().get(0);
            }
        }
        return null;
    }
}
