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

public class BytecodeInstructionINVOKESPECIAL extends BytecodeInstructionGenericInvoke {

    public BytecodeInstructionINVOKESPECIAL(BytecodeOpcodeAddress aOpcodeIndex, int aIndex, BytecodeConstantPool aConstantPool) {
        super(aOpcodeIndex, aIndex, aConstantPool);
    }

    @Override
    public void performLinking(BytecodeLinkerContext aLinkerContext) {
        BytecodeMethodRefConstant theMethodRefConstant = getMethodReference();
        BytecodeClassinfoConstant theClassConstant = theMethodRefConstant.getClassIndex().getClassConstant();
        BytecodeNameAndTypeConstant theMethodRef = theMethodRefConstant.getNameAndTypeIndex().getNameAndType();

        BytecodeMethodSignature theSig = theMethodRef.getDescriptorIndex().methodSignature();
        BytecodeUtf8Constant theClassName = theClassConstant.getConstant();

        BytecodeUtf8Constant theName = theMethodRef.getNameIndex().getName();
        if ("<init>".equals(theName.stringValue())) {
            aLinkerContext.linkConstructorInvocation(new BytecodeObjectTypeRef(theClassName.stringValue().replace("/", ".")),
                    theSig);
        } else {
            aLinkerContext.linkVirtualMethod(new BytecodeObjectTypeRef(theClassName.stringValue().replace("/", ".")),
                    theName.stringValue(), theSig);
        }
    }
}
