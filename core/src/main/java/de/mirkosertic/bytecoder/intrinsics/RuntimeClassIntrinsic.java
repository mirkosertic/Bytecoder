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

import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESPECIAL;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.ssa.*;

import java.util.List;

public class RuntimeClassIntrinsic extends Intrinsic {

    @Override
    public boolean intrinsify(final Program aProgram, final BytecodeInstructionINVOKESPECIAL aInstruction, final String aMethodName,
                              final BytecodeObjectTypeRef aType, final List<Value> aArguments, final Variable aTarget, final RegionNode aTargetBlock, final ParsingHelper aHelper) {
        final BytecodeMethodSignature theSignature = aInstruction.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();
        if ("getClass".equals(aMethodName) && BytecodeLinkedClass.GET_CLASS_SIGNATURE
                .matchesExactlyTo(theSignature)) {
            final Variable theNewVariable = aTargetBlock
                    .newVariable(aInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), new TypeOfExpression(aProgram, aInstruction.getOpcodeAddress(), aTarget));
            aHelper.push(theNewVariable);

            return true;
        }
        return false;
    }
}
