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

import java.util.List;
import java.util.function.Function;

public enum Allocator {
    linear {
        @Override
        public AbstractAllocator allocate(final List<Variable> aVariables, final Function<TypeRef, TypeRef> aTypeConverter) {
            return new LinearRegisterAllocator(aVariables, aTypeConverter);
        }
    },

    passthru {
        @Override
        public AbstractAllocator allocate(final List<Variable> aVariables, final Function<TypeRef, TypeRef> aTypeConverter) {
            return new PassThruRegisterAllocator(aVariables, aTypeConverter);
        }
    }
    ;
    public abstract AbstractAllocator allocate(final List<Variable> aVariables, final Function<TypeRef, TypeRef> aTypeConverter);
}
