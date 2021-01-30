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

public class BytecodeInstructionINVOKEVIRTUAL extends BytecodeInstructionGenericInvoke {

    public BytecodeInstructionINVOKEVIRTUAL(final BytecodeOpcodeAddress aOpcodeIndex, final int aIndex, final BytecodeConstantPool aConstantPool) {
        super(aOpcodeIndex, aIndex, aConstantPool);
    }

    @Override
    public void performLinking(final BytecodeClass aOwningClass, final BytecodeLinkerContext aLinkerContext) {
        final BytecodeMethodRefConstant theMethodRefConstant = getMethodReference();
        final BytecodeClassinfoConstant theClassConstant = theMethodRefConstant.getClassIndex().getClassConstant();
        final BytecodeNameAndTypeConstant theMethodRef = theMethodRefConstant.getNameAndTypeIndex().getNameAndType();

        final BytecodeMethodSignature theSig = theMethodRef.getDescriptorIndex().methodSignature();
        final BytecodeUtf8Constant theName = theMethodRef.getNameIndex().getName();

        final BytecodeUtf8Constant theConstant = theClassConstant.getConstant();
        final String theClassName = theConstant.stringValue();
        if (theClassName.startsWith("[")) {

            final BytecodeTypeRef[] theTypes = aLinkerContext.getSignatureParser().toTypes(theClassName);
            final BytecodeTypeRef theSingleType = theTypes[0];
            aLinkerContext.resolveTypeRef(theSingleType);

            // We are linking an Array here, so mark the corresponding name
            final BytecodeObjectTypeRef theTypeRef = BytecodeObjectTypeRef.fromRuntimeClass(Array.class);
            final BytecodeLinkedClass theClass = aLinkerContext.resolveClass(theTypeRef);
            if (!theClass.resolveVirtualMethod(theName.stringValue(), theSig)) {
                if (!theClass.getBytecodeClass().getAccessFlags().isAbstract()) {
                    throw new IllegalStateException("Cannot find virtual method " + theName.stringValue() + " in non-abstract class " + theClassConstant.getConstant().stringValue() + " with signature " + theSig.toString());
                }
            }
        } else {
            final BytecodeLinkedClass theClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(theConstant));
            if (!theClass
                    .resolveVirtualMethod(theName.stringValue(), theSig)) {
                if (!theClass.getBytecodeClass().getAccessFlags().isAbstract()) {
                    throw new IllegalStateException("Cannot find virtual method " + theName.stringValue() + " in non-abstract class " + theClassConstant.getConstant().stringValue() + " with signature " + theSig.toString());
                }
            }
        }
    }
}