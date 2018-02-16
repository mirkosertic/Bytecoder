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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public abstract class BytecodeOverrideMap<T> {

    interface UniqueIdentifier {
    }

    public static class Entry<T> {
        private final BytecodeLinkedClass providingClass;
        private final T value;

        public Entry(BytecodeLinkedClass aProvidingClass, T aField) {
            providingClass = aProvidingClass;
            value = aField;
        }

        public BytecodeLinkedClass getProvidingClass() {
            return providingClass;
        }

        public T getValue() {
            return value;
        }
    }

    protected final List<Entry<T>> entries;

    public BytecodeOverrideMap() {
        entries = new ArrayList<>();
    }

    public void register(BytecodeLinkedClass aProvidingClass, T aValue) {
        entries.add(new Entry<>(aProvidingClass, aValue));
    }

    public void merge(BytecodeOverrideMap<T> aOtherMap) {
        entries.addAll(aOtherMap.entries);
    }

    public Stream<Entry<T>> stream() {
        Map<UniqueIdentifier, Entry<T>> theResult = new HashMap<>();
        for (Entry<T> theEntry : entries) {
            theResult.put(identifierFor(theEntry.getValue()), theEntry);
        }
        return entries.stream().filter(t -> theResult.values().contains(t));
    }

    protected abstract UniqueIdentifier identifierFor(T aValue);
}