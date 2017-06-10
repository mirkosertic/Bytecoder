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

public class BytecodeInstructionINVOKEDYNAMIC extends BytecodeInstruction implements BytecodeInstructionInvoke {

    private final int index;
    private final BytecodeConstantPool constantPool;

    public BytecodeInstructionINVOKEDYNAMIC(BytecodeOpcodeAddress aIndex, int aConstantIndex, BytecodeConstantPool aConstantPool) {
        super(aIndex);
        index = aConstantIndex;
        constantPool = aConstantPool;
    }

    public BytecodeInvokeDynamicConstant getCallSite() {
        return (BytecodeInvokeDynamicConstant) constantPool.constantByIndex(index - 1);
    }

    private void link(BytecodeLinkerContext aLinkerContext, BytecodeReferenceKind aKind, BytecodeConstant aReference) {
        switch (aKind) {
            case REF_invokeStatic:
                BytecodeMethodRefConstant theStaticReference = (BytecodeMethodRefConstant) aReference;

                BytecodeLinkedClass theLinkedClass = aLinkerContext.linkClass(BytecodeObjectTypeRef.fromUtf8Constant(theStaticReference.getClassIndex().getClassConstant().getConstant()));
                BytecodeNameAndTypeConstant theNameAndType = theStaticReference.getNameAndTypeIndex().getNameAndType();
                theLinkedClass.linkVirtualMethod(theNameAndType.getNameIndex().getName().stringValue(),
                        theNameAndType.getDescriptorIndex().methodSignature());
                break;
            default:
                throw new IllegalStateException("Not implemented refkind for invokedynamic : " + aKind);
        }

    }

    @Override
    public void performLinking(BytecodeClass aOwningClass, BytecodeLinkerContext aLinkerContext) {

        System.out.println(" callsite index is " + index);

        BytecodeInvokeDynamicConstant theConstant = getCallSite();

        System.out.println("   and maps to bs method " + theConstant.getBootstrapMethodAttributeIndex().getIndex());

        BytecodeBootstrapMethodsAttributeInfo theBootStrapMethods = aOwningClass.getAttributes().getByType(BytecodeBootstrapMethodsAttributeInfo.class);
        BytecodeBootstrapMethod theBootstrapMethod = theBootStrapMethods.methodByIndex(theConstant.getBootstrapMethodAttributeIndex().getIndex());

        BytecodeMethodHandleConstant theMethodRef = theBootstrapMethod.getMethodRef();

        link(aLinkerContext, theMethodRef.getReferenceKind(), theMethodRef.getReferenceIndex().getConstant());

        for (BytecodeConstant theArgument : theBootstrapMethod.getArguments()) {

            if (theArgument instanceof BytecodeMethodRefConstant) {
                BytecodeMethodRefConstant theRef = (BytecodeMethodRefConstant) theArgument;
                BytecodeLinkedClass theLinkedClass = aLinkerContext.linkClass(BytecodeObjectTypeRef.fromUtf8Constant(theRef.getClassIndex().getClassConstant().getConstant()));
                BytecodeNameAndTypeConstant theNameAndType = theRef.getNameAndTypeIndex().getNameAndType();

                String theMethodName = theNameAndType.getNameIndex().getName().stringValue();

                if (theMethodName.equals("<init>")) {
                    theLinkedClass.linkConstructorInvocation(theNameAndType.getDescriptorIndex().methodSignature());
                } else {
                    theLinkedClass.linkVirtualMethod(theNameAndType.getNameIndex().getName().stringValue(),
                            theNameAndType.getDescriptorIndex().methodSignature());
                }
            }
            if (theArgument instanceof BytecodeMethodHandleConstant) {
                BytecodeMethodHandleConstant theHandle = (BytecodeMethodHandleConstant) theArgument;
                link(aLinkerContext, theHandle.getReferenceKind(), theHandle.getReferenceIndex().getConstant());
            }
        }
    }
}
