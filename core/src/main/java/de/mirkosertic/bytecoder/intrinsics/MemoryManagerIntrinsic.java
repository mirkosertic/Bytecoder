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

import de.mirkosertic.bytecoder.classlib.Address;
import de.mirkosertic.bytecoder.classlib.MemoryManager;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESPECIAL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESTATIC;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.ssa.*;

import java.util.List;
import java.util.Objects;

public class MemoryManagerIntrinsic extends Intrinsic {

    @Override
    public boolean intrinsify(final Program aProgram, final BytecodeInstructionINVOKESTATIC aInstruction, final String aMethodName, final List<Value> aArguments, final BytecodeObjectTypeRef aTargetClass, final RegionNode aTargetBlock, final ParsingHelper aHelper) {

        if (Objects.equals(aTargetClass.name(), MemoryManager.class.getName()) && "initTestMemory".equals(aMethodName)) {
            // This invocation can be skipped!!!
            return true;
        }

        if (Objects.equals(aTargetClass.name(), Address.class.getName())) {
            switch (aMethodName) {
                case "setIntValue": {

                    final Value theTarget = aArguments.get(0);
                    final Value theOffset = aArguments.get(1);
                    final Value theNewValue = aArguments.get(2);

                    final ComputedMemoryLocationWriteExpression theLocation = new ComputedMemoryLocationWriteExpression(aProgram, aInstruction.getOpcodeAddress(), theTarget, theOffset);
                    final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.Native.INT, theLocation);
                    aTargetBlock.getExpressions().add(new SetMemoryLocationExpression(aProgram, aInstruction.getOpcodeAddress(), theNewVariable, theNewValue));

                    return true;
                }
                case "getStart": {

                    final Value theTarget = aArguments.get(0);
                    final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.Native.INT, theTarget);

                    aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);
                    return true;
                }
                case "getStackTop": {

                    final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.Native.INT, new StackTopExpression(aProgram, aInstruction.getOpcodeAddress()));

                    aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);
                    return true;
                }
                case "getMemorySize": {

                    final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.Native.INT, new MemorySizeExpression(aProgram, aInstruction.getOpcodeAddress()));

                    aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);
                    return true;
                }
                case "getIntValue": {

                    final Value theTarget = aArguments.get(0);
                    final Value theOffset = aArguments.get(1);

                    final ComputedMemoryLocationReadExpression theLocation = new ComputedMemoryLocationReadExpression(aProgram, aInstruction.getOpcodeAddress(), theTarget, theOffset);
                    final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.Native.INT, theLocation);
                    aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);

                    return true;
                }
                case "unreachable": {
                    aTargetBlock.getExpressions().add(new UnreachableExpression(aProgram, aInstruction.getOpcodeAddress()));
                    return true;
                }
                default:
                    throw new IllegalStateException("Not implemented : " + aMethodName);
            }
        }
        return false;
    }

    @Override
    public boolean intrinsify(final Program aProgram, final BytecodeInstructionINVOKESPECIAL aInstruction, final String aMethodName, final BytecodeObjectTypeRef aType, final List<Value> aArguments, final Variable aTarget, final RegionNode aTargetBlock, final ParsingHelper aHelper) {

        if (Objects.equals(aType, BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
            aTarget.initializeWith(aArguments.get(0), aProgram.getAnalysisTime());
            aTargetBlock.getExpressions().add(new VariableAssignmentExpression(aProgram, aInstruction.getOpcodeAddress(), aTarget, aArguments.get(0)));
            return true;
        }
        return false;
    }
}
