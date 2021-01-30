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

import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;

import java.util.List;

public class NewInstanceAndConstructExpression extends InvocationExpression implements ValueWithEscapeCheck {

    private final BytecodeObjectTypeRef clazz;
    private boolean escaping;

    public NewInstanceAndConstructExpression(final Program aProgram, final BytecodeOpcodeAddress aAddress, final BytecodeObjectTypeRef aClazz,
                                             final BytecodeMethodSignature aMethodSignature, final List<Value> aArguments) {
        super(aProgram, aAddress, aMethodSignature);
        clazz = aClazz;

        receivesDataFrom(aArguments);
    }

    public BytecodeObjectTypeRef getClazz() {
        return clazz;
    }

    @Override
    public TypeRef resolveType() {
        return TypeRef.toType(clazz);
    }

    @Override
    public void markAsEscaped() {
        escaping = true;
    }

    @Override
    public boolean isEscaping() {
        return escaping;
    }
}
