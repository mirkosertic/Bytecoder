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

import java.lang.reflect.Array;

public class BytecodeInstructionNEWARRAY extends BytecodeInstruction {

    private final BytecodePrimitiveTypeRef type;

    public BytecodeInstructionNEWARRAY(BytecodeOpcodeAddress aOpcodeIndex, BytecodePrimitiveTypeRef aType) {
        super(aOpcodeIndex);
        type = aType;
    }

    public BytecodeObjectTypeRef getObjectType() {
        return BytecodeObjectTypeRef.fromRuntimeClass(Array.class);
    }

    public BytecodePrimitiveTypeRef getPrimitiveType() {
        return type;
    }

    @Override
    public void performLinking(BytecodeClass aOwningClass, BytecodeLinkerContext aLinkerContext) {
        aLinkerContext.resolveClass(getObjectType());
    }
}