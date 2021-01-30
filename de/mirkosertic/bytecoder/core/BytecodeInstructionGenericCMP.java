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
package de.mirkosertic.bytecoder.core;

import de.mirkosertic.bytecoder.ssa.TypeRef;

public class BytecodeInstructionGenericCMP extends BytecodeInstruction {

    public enum Type {
        l, g
    }

    private final Type type;
    private final BytecodePrimitiveTypeRef primitiveTypeRef;

    public BytecodeInstructionGenericCMP(BytecodeOpcodeAddress aOpcodeIndex, Type aType, BytecodePrimitiveTypeRef aTypeRef) {
        super(aOpcodeIndex);
        type = aType;
        primitiveTypeRef = aTypeRef;
    }

    public Type getType() {
        return type;
    }

    public BytecodePrimitiveTypeRef getPrimitiveTypeRef() {
        return primitiveTypeRef;
    }
}
