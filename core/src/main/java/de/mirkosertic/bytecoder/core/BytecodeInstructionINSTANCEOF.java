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

public class BytecodeInstructionINSTANCEOF extends BytecodeInstruction {

    private final int constantIndex;
    private final BytecodeConstantPool constantPool;

    public BytecodeInstructionINSTANCEOF(BytecodeOpcodeAddress aOpcodeIndex, int aConstantIndex, BytecodeConstantPool aConstantPool) {
        super(aOpcodeIndex);
        constantIndex = aConstantIndex;
        constantPool = aConstantPool;
    }

    public BytecodeClassinfoConstant getTypeRef() {
        return (BytecodeClassinfoConstant) constantPool.constantByIndex(constantIndex - 1);
    }

    @Override
    public void performLinking(BytecodeClass aOwningClass, BytecodeLinkerContext aLinkerContext) {
        BytecodeClassinfoConstant theType = getTypeRef();
        BytecodeUtf8Constant theName = theType.getConstant();
        if (theName.stringValue().startsWith("[")) {
            BytecodeTypeRef theTypeRef = aLinkerContext.getSignatureParser().toFieldType(theName);
            aLinkerContext.resolveTypeRef(theTypeRef);
        } else {
            aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(theName));
        }
    }
}