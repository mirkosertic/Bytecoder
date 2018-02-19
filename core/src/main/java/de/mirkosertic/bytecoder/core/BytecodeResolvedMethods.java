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

    private static class MethodIdentifier {
        private final String methodName;
        private final BytecodeMethodSignature signature;

        public MethodIdentifier(String aName, BytecodeMethodSignature aSignature) {
            methodName = aName;
            signature = aSignature;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodIdentifier that = (MethodIdentifier) o;
            if (!methodName.equals(that.methodName)) {
                return false;
            }
            if (!(signature.getArguments().length == that.signature.getArguments().length)) {
                return false;
            }
            return Objects.equals(methodName, that.methodName) &&
                    Objects.equals(signature, that.signature);
        }

        @Override
        public int hashCode() {
            return Objects.hash(methodName, signature);
        }
    }

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
    }


    protected final List<MethodEntry> entries;

    public BytecodeResolvedMethods() {
        entries = new ArrayList<>();
    }

    public void merge(BytecodeResolvedMethods aOtherMethods) {
        entries.addAll(aOtherMethods.entries);
    }

    public void register(BytecodeLinkedClass aProvidingClass, BytecodeMethod aMethod) {
        entries.add(new MethodEntry(aProvidingClass, aMethod));
    }

    public Stream<MethodEntry> stream() {
        return entries.stream();
    }
}
