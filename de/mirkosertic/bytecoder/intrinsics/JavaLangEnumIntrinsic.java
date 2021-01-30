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

import de.mirkosertic.bytecoder.core.*;
import de.mirkosertic.bytecoder.ssa.*;

import java.util.List;

public class JavaLangEnumIntrinsic extends Intrinsic {

    @Override
    public boolean intrinsify(final Program aProgram, final BytecodeInstructionINVOKEVIRTUAL aInstruction, final String aMethodName, final List<Value> aArguments, final Value aTarget, final RegionNode aTargetBlock, final ParsingHelper aHelper) {
        final BytecodeMethodSignature theSignature = aInstruction.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();
        if ("getEnumConstants".equals(aMethodName) && theSignature.matchesExactlyTo(BytecodeLinkedClass.GET_ENUM_CONSTANTS_SIGNATURE)) {
            final Value theValue = new EnumConstantsExpression(aProgram, aInstruction.getOpcodeAddress(), aTarget);
            final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), theValue);
            aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);
            return true;
        }
        return false;
    }

    @Override
    public boolean intrinsify(final Program aProgram, final BytecodeInstructionGETSTATIC aInstruction, final String aFieldName, final BytecodeObjectTypeRef aTtargetType, final RegionNode aTargetBlock, final ParsingHelper aHelper) {
        if ("$VALUES".equals(aFieldName)) {
            final Value theValue = new EnumConstantsExpression(aProgram, aInstruction.getOpcodeAddress(), new ClassReferenceValue(aTtargetType));
            aHelper.push(aInstruction.getOpcodeAddress(), theValue);
            return true;
        }
        return false;
    }

    @Override
    public boolean intrinsify(final Program aProgram, final BytecodeInstructionPUTSTATIC aInstruction, final String aFieldName, final BytecodeObjectTypeRef aTtargetType, final Value aValue, final RegionNode aTargetBlock, final ParsingHelper aHelper) {
        if ("$VALUES".equals(aFieldName)) {
            aTargetBlock.getExpressions().add(new SetEnumConstantsExpression(aProgram, aInstruction.getOpcodeAddress(), new ClassReferenceValue(aTtargetType), aValue));
            return true;
        }
        return false;
    }
}
