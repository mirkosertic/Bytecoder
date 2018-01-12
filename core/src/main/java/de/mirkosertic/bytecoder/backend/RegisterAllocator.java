/*
 * Copyright 2017 Mirko Sertic
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
package de.mirkosertic.bytecoder.backend;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Variable;

public class RegisterAllocator {

    public class Register {

        private final String name;
        private final TypeRef typeRef;
        private final Set<Register> liveWith;

        public Register(String aName, TypeRef aTypeRef) {
            name = aName;
            typeRef = aTypeRef;
            liveWith = new HashSet<>();
        }

        public String getName() {
            return name;
        }

        public TypeRef getTypeRef() {
            return typeRef;
        }
    }

    private final Map<Variable, Register> mapping;

    public RegisterAllocator(Set<Variable> aVariables) {
        mapping = new HashMap<>();
        for (Variable theVariable : aVariables) {
            Register theRegister = new Register("r" + mapping.size(), theVariable.resolveType());
            mapping.put(theVariable, theRegister);
        }
    }

    public Register resolve(Variable aVariable) {
        return mapping.get(aVariable);
    }

    public Set<Register> allRegisters() {
        return new HashSet<>(mapping.values());
    }
}