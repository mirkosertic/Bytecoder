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

    public BytecodeInstructionINVOKEDYNAMIC(final BytecodeOpcodeAddress aIndex, final int aConstantIndex, final BytecodeConstantPool aConstantPool) {
        super(aIndex);
        index = aConstantIndex;
        constantPool = aConstantPool;
    }

    public BytecodeInvokeDynamicConstant getCallSite() {
        return (BytecodeInvokeDynamicConstant) constantPool.constantByIndex(index - 1);
    }

    private void link(final BytecodeLinkerContext aLinkerContext, final BytecodeReferenceKind aKind, final BytecodeConstant aReference, final AnalysisStack analysisStack) {
        switch (aKind) {
            case REF_invokeStatic:
                final BytecodeMethodRefConstant theStaticReference = (BytecodeMethodRefConstant) aReference;

                final BytecodeObjectTypeRef className = BytecodeObjectTypeRef.fromUtf8Constant(theStaticReference.getClassIndex().getClassConstant().getConstant());
                final BytecodeNameAndTypeConstant theNameAndType = theStaticReference.getNameAndTypeIndex().getNameAndType();
                final String methodName = theNameAndType.getNameIndex().getName().stringValue();
                final BytecodeMethodSignature signature = theNameAndType.getDescriptorIndex().methodSignature();
                final BytecodeLinkedClass theLinkedClass = aLinkerContext.resolveClass(className, analysisStack);
                if (!theLinkedClass.resolveVirtualMethod(methodName,
                        theNameAndType.getDescriptorIndex().methodSignature(), analysisStack)) {
                    throw new MissingLinkException("Cannot find static invoke dynamic bootstrap method " + className.name() + "." + methodName + "(" +signature + ") . Analysis stack is \n" + analysisStack.toDebugOutput());
                }
                break;
            default:
                throw new IllegalStateException("Not implemented refkind for invokedynamic : " + aKind);
        }

    }

    private void linkSignature(final BytecodeMethodSignature signature, final BytecodeLinkerContext context, final AnalysisStack analysisStack) {
        context.resolveTypeRef(signature.getReturnType(), analysisStack);
        for (final BytecodeTypeRef ref : signature.getArguments()) {
            context.resolveTypeRef(ref, analysisStack);
        }
    }

    @Override
    public void performLinking(final BytecodeClass aOwningClass, final BytecodeLinkerContext aLinkerContext, final AnalysisStack analysisStack) {

        final BytecodeInvokeDynamicConstant theConstant = getCallSite();

        final BytecodeBootstrapMethodsAttributeInfo theBootStrapMethods = aOwningClass.getAttributes().getByType(BytecodeBootstrapMethodsAttributeInfo.class);
        final BytecodeBootstrapMethod theBootstrapMethod = theBootStrapMethods.methodByIndex(theConstant.getBootstrapMethodAttributeIndex().getIndex());
        for (final BytecodeConstant constant : theBootstrapMethod.getArguments()) {
            if (constant instanceof BytecodeMethodTypeConstant) {
                final BytecodeMethodTypeConstant m = (BytecodeMethodTypeConstant) constant;
                final BytecodeMethodSignature theSignature = m.getDescriptorIndex().methodSignature();
                linkSignature(theSignature, aLinkerContext, analysisStack);
            }
        }

        final BytecodeMethodSignature theInitSignature = theConstant.getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();
        linkSignature(theInitSignature, aLinkerContext, analysisStack);

        final BytecodeMethodHandleConstant theMethodRef = theBootstrapMethod.getMethodRef();
        final BytecodeMethodRefConstant theBootstrapMethodToInvoke = (BytecodeMethodRefConstant) theMethodRef.getReferenceIndex().getConstant();

        switch (theMethodRef.getReferenceKind()) {
            case REF_invokeStatic: {
                // Link the static method
                aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(theBootstrapMethodToInvoke.getClassIndex().getClassConstant().getConstant()), analysisStack)
                        .resolveStaticMethod(theBootstrapMethodToInvoke.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                                theBootstrapMethodToInvoke.getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature(), analysisStack);

                // in this case we assume that the invoke dynamic can be replaced by an invokestatic
                // to the implementing method, but only indirectly using a callsite object aka function pointer
                for (final BytecodeConstant theBootstrapArgument : theBootstrapMethod.getArguments()) {
                    if (theBootstrapArgument instanceof BytecodeMethodHandleConstant) {
                        final BytecodeMethodHandleConstant theHandle = (BytecodeMethodHandleConstant) theBootstrapArgument;
                        final BytecodeMethodRefConstant theImplementingMethodRef = (BytecodeMethodRefConstant) theHandle.getReferenceIndex().getConstant();

                        final BytecodeObjectTypeRef theClass = BytecodeObjectTypeRef.fromUtf8Constant(theImplementingMethodRef.getClassIndex().getClassConstant().getConstant());
                        final BytecodeLinkedClass theLinkedClass = aLinkerContext.resolveClass(theClass, analysisStack);
                        theLinkedClass.tagWith(BytecodeLinkedClass.Tag.POSSIBLE_USE_IN_LAMBDA);
                        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull(
                                theImplementingMethodRef.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                                theImplementingMethodRef.getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature()
                        );
                        if ("<init>".equals(theMethod.getName().stringValue())) {
                            theLinkedClass.tagWith(BytecodeLinkedClass.Tag.INSTANTIATED);
                        }
                        if (theMethod.getAccessFlags().isStatic()) {
                            theLinkedClass.resolveStaticMethod(theImplementingMethodRef.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                                    theImplementingMethodRef.getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature(), analysisStack);
                        } else if (theMethod.getAccessFlags().isPrivate()) {
                            theLinkedClass.resolvePrivateMethod(theImplementingMethodRef.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                                    theImplementingMethodRef.getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature(), analysisStack);
                        } else {
                            theLinkedClass.resolveVirtualMethod(theImplementingMethodRef.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                                    theImplementingMethodRef.getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature(), analysisStack);
                        }
                    }
                }
                break;
            }
            default:
                throw new IllegalStateException("Nut supported reference kind for invoke dynamic : " + theMethodRef.getReferenceKind());
        }

        link(aLinkerContext, theMethodRef.getReferenceKind(), theMethodRef.getReferenceIndex().getConstant(), analysisStack);
    }
}
