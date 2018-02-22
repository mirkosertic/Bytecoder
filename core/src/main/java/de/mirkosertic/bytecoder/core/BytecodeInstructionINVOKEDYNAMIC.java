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

                BytecodeLinkedClass theLinkedClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(theStaticReference.getClassIndex().getClassConstant().getConstant()));
                BytecodeNameAndTypeConstant theNameAndType = theStaticReference.getNameAndTypeIndex().getNameAndType();
                theLinkedClass.resolveVirtualMethod(theNameAndType.getNameIndex().getName().stringValue(),
                        theNameAndType.getDescriptorIndex().methodSignature());
                break;
            default:
                throw new IllegalStateException("Not implemented refkind for invokedynamic : " + aKind);
        }

    }

    @Override
    public void performLinking(BytecodeClass aOwningClass, BytecodeLinkerContext aLinkerContext) {

        BytecodeInvokeDynamicConstant theConstant = getCallSite();

        BytecodeBootstrapMethodsAttributeInfo theBootStrapMethods = aOwningClass.getAttributes().getByType(BytecodeBootstrapMethodsAttributeInfo.class);
        BytecodeBootstrapMethod theBootstrapMethod = theBootStrapMethods.methodByIndex(theConstant.getBootstrapMethodAttributeIndex().getIndex());

        BytecodeMethodHandleConstant theMethodRef = theBootstrapMethod.getMethodRef();
        BytecodeMethodRefConstant theBootstrapMethodToInvoke = (BytecodeMethodRefConstant) theMethodRef.getReferenceIndex().getConstant();

        switch (theMethodRef.getReferenceKind()) {
            case REF_invokeStatic: {
                // Link the static method
                aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(theBootstrapMethodToInvoke.getClassIndex().getClassConstant().getConstant()))
                        .resolveStaticMethod(theBootstrapMethodToInvoke.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                                theBootstrapMethodToInvoke.getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature());

                // in this case we assume that the invoke dynamic can be replaced by an invokestatic
                // to the implementing method, but only indirectly using a callsite object aka function pointer
                for (BytecodeConstant theBootstrapArgument : theBootstrapMethod.getArguments()) {
                    if (theBootstrapArgument instanceof BytecodeMethodHandleConstant) {
                        BytecodeMethodHandleConstant theHandle = (BytecodeMethodHandleConstant) theBootstrapArgument;
                        BytecodeMethodRefConstant theImplementingMethodRef = (BytecodeMethodRefConstant) theHandle.getReferenceIndex().getConstant();

                        BytecodeObjectTypeRef theClass = BytecodeObjectTypeRef.fromUtf8Constant(theImplementingMethodRef.getClassIndex().getClassConstant().getConstant());
                        BytecodeLinkedClass theLinkedClass = aLinkerContext.resolveClass(theClass);
                        BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull(
                                theImplementingMethodRef.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                                theImplementingMethodRef.getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature()
                        );
                        if (theMethod.getAccessFlags().isStatic()) {
                            theLinkedClass.resolveStaticMethod(theImplementingMethodRef.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                                    theImplementingMethodRef.getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature());
                        } else if (theMethod.getAccessFlags().isPrivate()) {
                            theLinkedClass.resolvePrivateMethod(theImplementingMethodRef.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                                    theImplementingMethodRef.getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature());
                        } else {
                            theLinkedClass.resolveVirtualMethod(theImplementingMethodRef.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                                    theImplementingMethodRef.getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature());
                        }
                    }
                }
                break;
            }
            default:
                throw new IllegalStateException("Nut supported reference kind for invoke dynamic : " + theMethodRef.getReferenceKind());
        }

        link(aLinkerContext, theMethodRef.getReferenceKind(), theMethodRef.getReferenceIndex().getConstant());
    }
}
