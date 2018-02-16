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

import java.util.Objects;

public class BytecodeFieldMap extends BytecodeOverrideMap<BytecodeField> {

    public static class StringIdentifier implements UniqueIdentifier {

        private final String identifier;

        public StringIdentifier(String aIdentifier) {
            identifier = aIdentifier;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            StringIdentifier that = (StringIdentifier) o;
            return Objects.equals(identifier, that.identifier);
        }

        @Override
        public int hashCode() {
            return Objects.hash(identifier);
        }
    }

    public BytecodeFieldMap() {
    }

    public Entry fieldByName(String aFieldName) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry<BytecodeField> theEntry = entries.get(i);
            if (Objects.equals(theEntry.getValue().getName().stringValue(), aFieldName)) {
                return theEntry;
            }
        }
        return null;
    }

    @Override
    protected UniqueIdentifier identifierFor(final BytecodeField aValue) {
        return new StringIdentifier(aValue.getName().stringValue());
    }
}