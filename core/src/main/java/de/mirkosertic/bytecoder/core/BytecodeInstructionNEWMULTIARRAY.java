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

public class BytecodeInstructionNEWMULTIARRAY extends BytecodeInstruction {

    private final int typeIndex;
    private final BytecodeConstantPool constantPool;
    private final int dimensions;

    public BytecodeInstructionNEWMULTIARRAY(BytecodeOpcodeAddress aOpcodeIndex, int aTypeIndex, int aDimensions, BytecodeConstantPool aConstantPool) {
        super(aOpcodeIndex);
        typeIndex = aTypeIndex;
        constantPool = aConstantPool;
        dimensions = aDimensions;
    }

    public BytecodeClassinfoConstant getTypeConstant() {
        return (BytecodeClassinfoConstant) constantPool.constantByIndex(typeIndex - 1);
    }

    public BytecodeObjectTypeRef getObjectType() {
        return BytecodeObjectTypeRef.fromRuntimeClass(Array.class);
    }

    public int getDimensions() {
        return dimensions;
    }

    @Override
    public void performLinking(BytecodeClass aOwningClass, BytecodeLinkerContext aLinkerContext) {
        aLinkerContext.resolveClass(getObjectType());

        BytecodeClassinfoConstant theConstant = getTypeConstant();
        String theClassName = theConstant.getConstant().stringValue();

        BytecodeTypeRef[] theTypes = aLinkerContext.getSignatureParser().toTypes(theClassName);
        aLinkerContext.resolveTypeRef(theTypes[0]);
    }
}