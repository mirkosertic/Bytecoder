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
package de.mirkosertic.bytecoder.ssa;

import java.util.List;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;

public class NewMultiArrayExpression extends Expression {

    private final BytecodeTypeRef type;

    public NewMultiArrayExpression(final Program aProgram, final BytecodeOpcodeAddress aAddress, final BytecodeTypeRef aType, final List<Value> aDimensions) {
        super(aProgram, aAddress);
        type = aType;
        receivesDataFrom(aDimensions);
    }

    public BytecodeTypeRef getType() {
        return type;
    }

    @Override
    public TypeRef resolveType() {
        return TypeRef.Native.REFERENCE;
    }
}