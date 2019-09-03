/*
 * Copyright 2019 Mirko Sertic
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
package de.mirkosertic.bytecoder.allocator;

import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Variable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public abstract class AbstractAllocator {

    protected final Map<Variable, Register> registerAssignments;
    protected final Map<TypeRef.Native, List<Register>> knownRegisters;
    protected final Function<TypeRef.Native, TypeRef.Native> typeConverter;

    public AbstractAllocator(final Function<TypeRef.Native, TypeRef.Native> aTypeConverter) {
        registerAssignments = new HashMap<>();
        knownRegisters = new HashMap<>();
        typeConverter = aTypeConverter;
    }


    public Set<TypeRef.Native> usedRegisterTypes() {
        return knownRegisters.keySet();
    }

    public List<Register> registersOfType(final TypeRef.Native aType) {
        return knownRegisters.get(typeConverter.apply(aType));
    }

    public Register registerAssignmentFor(final Variable v) {
        return registerAssignments.get(v);
    }
}
