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

public class BytecodeInstructionINVOKEINTERFACE extends BytecodeInstruction implements BytecodeInstructionInvoke {

    private final int methodIndex;
    private final int count;
    private final BytecodeConstantPool constantPool;

    public BytecodeInstructionINVOKEINTERFACE(BytecodeOpcodeAddress aOpcodeIndex, int aMethodIndex, int aCount, BytecodeConstantPool aConstantPool) {
        super(aOpcodeIndex);
        methodIndex = aMethodIndex;
        count = aCount;
        constantPool = aConstantPool;
    }

    public BytecodeInterfaceRefConstant getMethodDescriptor() {
        return (BytecodeInterfaceRefConstant) constantPool.constantByIndex(methodIndex - 1);
    }

    @Override
    public void performLinking(BytecodeClass aOwningClass, BytecodeLinkerContext aLinkerContext) {
        BytecodeInterfaceRefConstant theMethodRefConstant = getMethodDescriptor();
        BytecodeClassinfoConstant theClassConstant = theMethodRefConstant.getClassIndex().getClassConstant();
        BytecodeNameAndTypeConstant theMethodRef = theMethodRefConstant.getNameAndTypeIndex().getNameAndType();

        BytecodeMethodSignature theSig = theMethodRef.getDescriptorIndex().methodSignature();
        BytecodeUtf8Constant theName = theMethodRef.getNameIndex().getName();

        BytecodeUtf8Constant theConstant = theClassConstant.getConstant();
        aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(theConstant)).resolveVirtualMethod(theName.stringValue(), theSig);
    }
}
