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
package de.mirkosertic.bytecoder.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class BytecodeResolvedMethods {

    public static class MethodEntry {
        private final BytecodeLinkedClass providingClass;
        private final BytecodeMethod value;

        public MethodEntry(BytecodeLinkedClass aProvidingClass, BytecodeMethod aField) {
            providingClass = aProvidingClass;
            value = aField;
        }

        public BytecodeLinkedClass getProvidingClass() {
            return providingClass;
        }

        public BytecodeMethod getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            MethodEntry that = (MethodEntry) o;
            return Objects.equals(providingClass, that.providingClass) &&
                    Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(providingClass, value);
        }
    }

    protected final List<MethodEntry> entries;

    public BytecodeResolvedMethods() {
        entries = new ArrayList<>();
    }

    public void merge(BytecodeResolvedMethods aOtherMethods) {
        for (MethodEntry theOtherEntry : aOtherMethods.entries) {
            if (!entries.contains(theOtherEntry)) {
                entries.add(theOtherEntry);
            }
        }
    }

    public void register(BytecodeLinkedClass aProvidingClass, BytecodeMethod aMethod) {
        MethodEntry theNewEntry = new MethodEntry(aProvidingClass, aMethod);
        if (!entries.contains(theNewEntry)) {
            entries.add(theNewEntry);
        }
    }

    public Stream<MethodEntry> stream() {
        return entries.stream();
    }
}
