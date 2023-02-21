/*
 * Copyright 2023 Mirko Sertic
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
package de.mirkosertic.bytecoder.asm.backend.wasm;


import de.mirkosertic.bytecoder.asm.ir.ResolvedMethod;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MethodToIDMapper {

    private static class Entry {
        final String name;
        final Type type;

        public Entry(final String name, final Type type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final Entry entry = (Entry) o;
            return name.equals(entry.name) && type.equals(entry.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    private final List<Entry> knownEntries;

    public MethodToIDMapper() {
        knownEntries = new ArrayList<>();
    }

    public int resolveIdFor(final ResolvedMethod method) {
        final Entry e = new Entry(method.methodNode.name, method.methodType);
        final int i = knownEntries.indexOf(e);
        if (i >= 0) {
            return i;
        }
        knownEntries.add(e);
        return knownEntries.size() - 1;
    }
}
