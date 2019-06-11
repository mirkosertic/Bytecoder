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

import de.mirkosertic.bytecoder.classlib.Array;

public class BytecodeInstructionANEWARRAY extends BytecodeInstruction {

    private final int typeIndex;
    private final BytecodeConstantPool constantPool;

    public BytecodeInstructionANEWARRAY(final BytecodeOpcodeAddress aIndex, final int aTypeIndex, final BytecodeConstantPool aConstantPool) {
        super(aIndex);
        typeIndex = aTypeIndex;
        constantPool = aConstantPool;
    }

    public BytecodeObjectTypeRef getObjectType() {
        return BytecodeObjectTypeRef.fromRuntimeClass(Array.class);
    }

    public BytecodeTypeRef getArrayType(final BytecodeSignatureParser aSignatureParser) {
        final BytecodeClassinfoConstant theClassInfo = getTypeConstant();
        final String theType = theClassInfo.getConstant().stringValue();
        if (theType.startsWith("[")) {
            return aSignatureParser.toFieldType(theClassInfo.getConstant());
        }
        return BytecodeObjectTypeRef.fromUtf8Constant(theClassInfo.getConstant());
    }

    private BytecodeClassinfoConstant getTypeConstant() {
        return (BytecodeClassinfoConstant) constantPool.constantByIndex(typeIndex - 1);
    }

    @Override
    public void performLinking(final BytecodeClass aOwningClass, final BytecodeLinkerContext aLinkerContext) {
        aLinkerContext.resolveClass(getObjectType());
        aLinkerContext.resolveTypeRef(getArrayType(aLinkerContext.getSignatureParser()));
    }
}
