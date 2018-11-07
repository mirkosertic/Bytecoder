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
package de.mirkosertic.bytecoder.backend.wasm.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExceptionIndex {

    private final List<WASMException> events;

    ExceptionIndex() {
        events = new ArrayList<>();
    }

    public void add(final WASMException event) {
        events.add(event);
    }

    public WASMException byLabel(final String eventName) {
        for (final WASMException g : events) {
            if (Objects.equals(eventName, g.getLabel())) {
                return g;
            }
        }
        throw new IllegalArgumentException("No such exception : " + eventName);
    }

    public int size() {
        return events.size();
    }

    public WASMException get(final int i) {
        return events.get(i);
    }

    public int indexOf(final WASMException event) {
        return events.indexOf(event);
    }

    public boolean isEmpty() {
        return events.isEmpty();
    }
}