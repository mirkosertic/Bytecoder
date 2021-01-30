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

public class BytecodeResolvedFields {

    public static class FieldEntry {
        private final BytecodeLinkedClass providingClass;
        private final BytecodeField value;

        public FieldEntry(BytecodeLinkedClass aProvidingClass, BytecodeField aField) {
            providingClass = aProvidingClass;
            value = aField;
        }

        public BytecodeLinkedClass getProvidingClass() {
            return providingClass;
        }

        public BytecodeField getValue() {
            return value;
        }
    }

    protected final List<FieldEntry> entries;

    public BytecodeResolvedFields() {
        entries = new ArrayList<>();
    }

    public void register(BytecodeLinkedClass aProvidingClass, BytecodeField aValue) {
        entries.add(new FieldEntry(aProvidingClass, aValue));
    }

    public void merge(BytecodeResolvedFields aOtherFields) {
        entries.addAll(aOtherFields.entries);
    }

    public Stream<FieldEntry> stream() {
        return entries.stream();
    }

    public Stream<FieldEntry> streamForStaticFields() {
        return stream().filter(t -> t.value.getAccessFlags().isStatic());
    }

    public Stream<FieldEntry> streamForInstanceFields() {
        return stream().filter(t -> !t.value.getAccessFlags().isStatic());
    }

    public FieldEntry fieldByName(String aFieldName) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            FieldEntry theEntry = entries.get(i);
            if (Objects.equals(theEntry.getValue().getName().stringValue(), aFieldName)) {
                return theEntry;
            }
        }
        return null;
    }
}