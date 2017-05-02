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

import sun.reflect.ConstantPool;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class BytecodeLinkedClass {

    private final BytecodeObjectTypeRef className;
    private final BytecodeClass bytecodeClass;
    private final Set<BytecodeMethod> linkedMethods;
    private final BytecodeLinkerContext linkerContext;

    public BytecodeLinkedClass(BytecodeLinkerContext aLinkerContext, BytecodeObjectTypeRef aClassName, BytecodeClass aBytecodeClass) {
        className = aClassName;
        bytecodeClass = aBytecodeClass;
        linkedMethods = new HashSet<>();
        linkerContext = aLinkerContext;
    }

    private void link(BytecodeTypeRef aTypeRef) {
        if (aTypeRef.isPrimitive()) {
            return;
        }
        if (aTypeRef instanceof BytecodeArrayTypeRef) {
            BytecodeArrayTypeRef theArrayRef = (BytecodeArrayTypeRef) aTypeRef;
            link(theArrayRef.getType());
            return;
        }
        if (aTypeRef instanceof BytecodeObjectTypeRef) {
            linkerContext.linkClass((BytecodeObjectTypeRef) aTypeRef);
        }
    }

    public void linkMethod(String aMethodName) {
        BytecodeMethod theMethod = bytecodeClass.methodByName(aMethodName);
        BytecodeMethodSignature theSignature = theMethod.getSignature();
        link(theSignature.getReturnType());
        for (BytecodeTypeRef theArgument : theSignature.getArguments()) {
            link(theArgument);
        }
        linkedMethods.add(theMethod);

        // Check static dependencies
        BytecodeCodeAttributeInfo theCode = theMethod.attributeByType(BytecodeCodeAttributeInfo.class);
        BytecodeProgramm theProgram = theCode.getProgramm();
        for (BytecodeInstruction theInstruction : theProgram.getInstructions()) {
            if (theInstruction instanceof BytecodeInstructionINVOKESTATIC) {
                BytecodeInstructionINVOKESTATIC theStaticInvoke = (BytecodeInstructionINVOKESTATIC) theInstruction;

                BytecodeMethodRefConstant theMethodRefConstant = theStaticInvoke.getMethodRefConstant();
                BytecodeClassinfoConstant theClassConstant = theMethodRefConstant.getClassIndex().getClassConstant();
                BytecodeNameAndTypeConstant theMethodRef = theMethodRefConstant.getNameAndTypeIndex().getNameAndType();
                BytecodeMethodSignature theSig = theMethodRef.getDescriptorIndex().methodSignature();
                BytecodeUtf8Constant theName = theMethodRef.getNameIndex().getName();

                linkerContext.linkClassMethod(new BytecodeObjectTypeRef(theClassConstant.getConstant().stringValue().replace("/",".")),
                        theName.stringValue());
            }
        }
    }

    public BytecodeConstantPool getConstantPool() {
        return bytecodeClass.getConstantPool();
    }

    public void forEachMethod(Consumer<BytecodeMethod> aMethod) {
        linkedMethods.forEach(aMethod);
    }
}
