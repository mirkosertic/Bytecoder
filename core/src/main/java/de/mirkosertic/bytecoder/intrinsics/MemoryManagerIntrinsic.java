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
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESTATIC;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.ssa.ComputedMemoryLocationReadExpression;
import de.mirkosertic.bytecoder.ssa.ComputedMemoryLocationWriteExpression;
import de.mirkosertic.bytecoder.ssa.DataEndExpression;
import de.mirkosertic.bytecoder.ssa.HeapBaseExpression;
import de.mirkosertic.bytecoder.ssa.MemorySizeExpression;
import de.mirkosertic.bytecoder.ssa.ParsingHelper;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.PtrOfExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.SetMemoryLocationExpression;
import de.mirkosertic.bytecoder.ssa.StackTopExpression;
import de.mirkosertic.bytecoder.ssa.SystemHasStackExpression;
import de.mirkosertic.bytecoder.ssa.UnreachableExpression;
import de.mirkosertic.bytecoder.ssa.Value;

import java.util.List;
import java.util.Objects;

public class MemoryManagerIntrinsic extends Intrinsic {

    @Override
    public boolean intrinsify(final Program aProgram, final BytecodeInstructionINVOKESTATIC aInstruction, final String aMethodName, final List<Value> aArguments, final BytecodeObjectTypeRef aTargetClass, final RegionNode aTargetBlock, final ParsingHelper aHelper) {

        if (Objects.equals(aTargetClass.name(), Address.class.getName())) {
            switch (aMethodName) {
                case "setIntValue": {

                    final Value theTarget = aArguments.get(0);
                    final Value theOffset = aArguments.get(1);
                    final Value theNewValue = aArguments.get(2);

                    final ComputedMemoryLocationWriteExpression theLocation = new ComputedMemoryLocationWriteExpression(aProgram, aInstruction.getOpcodeAddress(), theTarget, theOffset);
                    aTargetBlock.getExpressions().add(new SetMemoryLocationExpression(aProgram, aInstruction.getOpcodeAddress(), theLocation, theNewValue));

                    return true;
                }
                case "getStackTop": {
                    aHelper.push(aInstruction.getOpcodeAddress(), new StackTopExpression(aProgram, aInstruction.getOpcodeAddress()));
                    return true;
                }
                case "getMemorySize": {
                    aHelper.push(aInstruction.getOpcodeAddress(), new MemorySizeExpression(aProgram, aInstruction.getOpcodeAddress()));
                    return true;
                }
                case "getHeapBase": {
                    aHelper.push(aInstruction.getOpcodeAddress(), new HeapBaseExpression(aProgram, aInstruction.getOpcodeAddress()));
                    return true;
                }
                case "getDataEnd": {
                    aHelper.push(aInstruction.getOpcodeAddress(), new DataEndExpression(aProgram, aInstruction.getOpcodeAddress()));
                    return true;
                }
                case "systemHasStack": {
                    aHelper.push(aInstruction.getOpcodeAddress(), new SystemHasStackExpression(aProgram, aInstruction.getOpcodeAddress()));
                    return true;
                }
                case "getIntValue": {

                    final Value theTarget = aArguments.get(0);
                    final Value theOffset = aArguments.get(1);

                    aHelper.push(aInstruction.getOpcodeAddress(), new ComputedMemoryLocationReadExpression(aProgram, aInstruction.getOpcodeAddress(), theTarget, theOffset));

                    return true;
                }
                case "unreachable": {
                    aTargetBlock.getExpressions().add(new UnreachableExpression(aProgram, aInstruction.getOpcodeAddress()));
                    return true;
                }
                case "ptrOf": {
                    final Value theTarget = aArguments.get(0);

                    aHelper.push(aInstruction.getOpcodeAddress(), new PtrOfExpression(aProgram, aInstruction.getOpcodeAddress(), theTarget));
                    return true;
                }
                default:
                    throw new IllegalStateException("Not implemented : " + aMethodName);
            }
        }
        return false;
    }
}
