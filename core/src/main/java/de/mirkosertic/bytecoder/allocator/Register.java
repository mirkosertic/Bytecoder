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

import java.util.Objects;

public class Register {

    private final long number;
    private final TypeRef type;

    public Register(final long number, final TypeRef type) {
        this.number = number;
        this.type = type;
    }

    public long getNumber() {
        return number;
    }

    public TypeRef getType() {
        return type;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Register register = (Register) o;
        return number == register.number &&
                type.equals(register.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, type);
    }
}
