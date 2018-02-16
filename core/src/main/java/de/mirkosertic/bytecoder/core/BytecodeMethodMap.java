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

public class BytecodeMethodMap extends BytecodeOverrideMap<BytecodeMethod> {

    public static class MethodIdentifier implements UniqueIdentifier {

        private final BytecodeMethod method;

        public MethodIdentifier(BytecodeMethod aMethod) {
            method = aMethod;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof MethodIdentifier)) {
                return false;
            }
            MethodIdentifier theIdentifier = (MethodIdentifier) o;
            if (!Objects.equals(method.getName().stringValue(),
                    theIdentifier.method.getName().stringValue())) {
                return false;
            }
            BytecodeMethodSignature theSignature = method.getSignature();
            BytecodeMethodSignature theOtherSignature = theIdentifier.method.getSignature();
            if (theSignature.getArguments().length == theOtherSignature.getArguments().length) {
                // TODO: Better method matching based on language override rules
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(method.getName().stringValue());
        }
    }

    public BytecodeMethodMap() {
    }

    @Override
    protected UniqueIdentifier identifierFor(BytecodeMethod aValue) {
        return new MethodIdentifier(aValue);
    }
}
