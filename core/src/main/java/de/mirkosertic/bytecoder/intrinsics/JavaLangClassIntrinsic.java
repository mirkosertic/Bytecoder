/*
 * Copyright 2019 Mirko Sertic
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
package de.mirkosertic.bytecoder.intrinsics;

import java.util.List;

import de.mirkosertic.bytecoder.core.BytecodeInstructionGETSTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESPECIAL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKEVIRTUAL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPUTSTATIC;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.ssa.IntegerValue;
import de.mirkosertic.bytecoder.ssa.NewInstanceFromDefaultConstructorExpression;
import de.mirkosertic.bytecoder.ssa.ParsingHelper;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.StringValue;
import de.mirkosertic.bytecoder.ssa.SuperTypeOfExpression;
import de.mirkosertic.bytecoder.ssa.TypeOfExpression;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;

public class JavaLangClassIntrinsic extends Intrinsic {

    @Override
    public boolean intrinsify(final Program aProgram, final BytecodeInstructionINVOKESPECIAL aInstruction, final String aMethodName,
                              final BytecodeObjectTypeRef aType, final List<Value> aArguments, final Variable aTarget, final RegionNode aTargetBlock, final ParsingHelper aHelper) {
        final BytecodeMethodSignature theSignature = aInstruction.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();
        final BytecodeObjectTypeRef theCalledClass = BytecodeObjectTypeRef.fromUtf8Constant(aInstruction.getMethodReference().getClassIndex().getClassConstant().getConstant());

        if ("getClass".equals(aMethodName) && BytecodeLinkedClass.GET_CLASS_SIGNATURE
                .matchesExactlyTo(theSignature)) {
            final Variable theNewVariable = aTargetBlock
                    .newVariable(aInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), new TypeOfExpression(aProgram, aInstruction.getOpcodeAddress(), aTarget));
            aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);

            return true;
        }
        if ("getSuperclass".equals(aMethodName) && BytecodeLinkedClass.GET_SUPERCLASS_SIGNATURE
                .matchesExactlyTo(theSignature)) {
            final Variable theNewVariable = aTargetBlock
                    .newVariable(aInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), new SuperTypeOfExpression(aProgram, aInstruction.getOpcodeAddress(), aTarget));
            aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);

            return true;
        }

        if (theCalledClass.name().equals(Class.class.getName())) {
            if ("newInstance".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new NewInstanceFromDefaultConstructorExpression(aProgram, aInstruction.getOpcodeAddress(), aTarget));
                return true;
            }
            if ("desiredAssertionStatus".equals(aMethodName) && theSignature.matchesExactlyTo(BytecodeLinkedClass.DESIRED_ASSERTION_STATUS_SIGNATURE)) {
                // Status is always false
                aHelper.push(aInstruction.getOpcodeAddress(), new IntegerValue(0));
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean intrinsify(final Program aProgram, final BytecodeInstructionINVOKESTATIC aInstruction,
            final String aMethodName, final List<Value> aArguments, final BytecodeObjectTypeRef aTargetClass,
            final RegionNode aTargetBlock, final ParsingHelper aHelper) {
        final BytecodeMethodSignature theSignature = aInstruction.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();
        final BytecodeObjectTypeRef theCalledClass = BytecodeObjectTypeRef.fromUtf8Constant(aInstruction.getMethodReference().getClassIndex().getClassConstant().getConstant());

        if (theCalledClass.name().equals(Class.class.getName())) {
            if ("forName".equals(aMethodName)) {
                for (int i=0;i<theSignature.getArguments().length;i++) {
                    if (theSignature.getArguments()[i].equals(BytecodeObjectTypeRef.fromRuntimeClass(String.class))) {
                        final Value theArgumentValue = aArguments.get(i);
                        checkblock:
                        {
                            if (theArgumentValue instanceof StringValue) {
                                // We found something directly
                                final String theClassName = ((StringValue) theArgumentValue).getStringValue();
                                if (aProgram.getLinkerContext() != null) {
                                    aProgram.getLinkerContext().getLogger().warn("Class {} is used by reflection!", theClassName);
                                }

                                break checkblock;
                            } else {
                                final List<Value> theIncomingFlows = theArgumentValue.incomingDataFlows();
                                for (final Value theValue : theIncomingFlows) {
                                    if (theValue instanceof StringValue) {
                                        // We found something as a variable
                                        final String theClassName = ((StringValue) theValue).getStringValue();
                                        if (aProgram.getLinkerContext() != null) {
                                            aProgram.getLinkerContext().getLogger().warn("Class {} is used by reflection!", theClassName);
                                        }

                                        break checkblock;
                                    }
                                }
                            }

                            if (aProgram.getLinkerContext() != null) {
                                aProgram.getLinkerContext().getLogger().warn("Class.forName usage detected with unknown class name");
                            }
                        }
                    }
                }
            }
        }

        return super.intrinsify(aProgram, aInstruction, aMethodName, aArguments, aTargetClass, aTargetBlock, aHelper);
    }

    @Override
    public boolean intrinsify(final Program aProgram, final BytecodeInstructionINVOKEVIRTUAL aInstruction, final String aMethodName, final List<Value> aArguments, final Value aTarget, final RegionNode aTargetBlock, final ParsingHelper aHelper) {
        final BytecodeMethodSignature theSignature = aInstruction.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();
        final BytecodeObjectTypeRef theCalledClass = BytecodeObjectTypeRef.fromUtf8Constant(aInstruction.getMethodReference().getClassIndex().getClassConstant().getConstant());

        if ("getClass".equals(aMethodName) && theSignature.matchesExactlyTo(BytecodeLinkedClass.GET_CLASS_SIGNATURE)) {
            final Value theValue = new TypeOfExpression(aProgram, aInstruction.getOpcodeAddress(), aTarget);
            final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), theValue);
            aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);
            return true;
        }
        if ("getSuperclass".equals(aMethodName) && BytecodeLinkedClass.GET_SUPERCLASS_SIGNATURE
                .matchesExactlyTo(theSignature)) {
            final Variable theNewVariable = aTargetBlock
                    .newVariable(aInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), new SuperTypeOfExpression(aProgram, aInstruction.getOpcodeAddress(), aTarget));
            aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);

            return true;
        }

        if (theCalledClass.name().equals(Class.class.getName())) {
            if ("newInstance".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new NewInstanceFromDefaultConstructorExpression(aProgram, aInstruction.getOpcodeAddress(), aTarget));
                return true;
            }
            if ("desiredAssertionStatus".equals(aMethodName) && theSignature.matchesExactlyTo(BytecodeLinkedClass.DESIRED_ASSERTION_STATUS_SIGNATURE)) {
                // Status is always false
                aHelper.push(aInstruction.getOpcodeAddress(), new IntegerValue(0));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean intrinsify(final Program aProgram, final BytecodeInstructionGETSTATIC aInstruction, final String aFieldName, final BytecodeObjectTypeRef aTtargetType, final RegionNode aTargetBlock, final ParsingHelper aHelper) {
        if ("$assertionsDisabled".equals(aFieldName)) {
            aHelper.push(aInstruction.getOpcodeAddress(), new IntegerValue(1));
            return true;
        }
        return false;
    }

    @Override
    public boolean intrinsify(final Program aProgram, final BytecodeInstructionPUTSTATIC aInstruction, final String aFieldName, final BytecodeObjectTypeRef aTtargetType, final Value aValue, final RegionNode aTargetBlock, final ParsingHelper aHelper) {
        if ("$assertionsDisabled".equals(aFieldName)) {
            return true;
        }
        return false;
    }
}
