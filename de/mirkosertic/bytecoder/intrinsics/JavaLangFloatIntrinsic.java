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

import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESTATIC;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.ssa.IsNaNExpression;
import de.mirkosertic.bytecoder.ssa.ParsingHelper;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;

import java.util.List;

public class JavaLangFloatIntrinsic extends Intrinsic {

    @Override
    public boolean intrinsify(final Program aProgram, final BytecodeInstructionINVOKESTATIC aInstruction, final String aMethodName, final List<Value> aArguments, final BytecodeObjectTypeRef aTargetClass, final RegionNode aTargetBlock, final ParsingHelper aHelper) {
        if (Float.class.getName().equals(aTargetClass.name())) {
            final BytecodeMethodSignature theSignature = aInstruction.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();
            if ("isNaN".equals(aMethodName)) {
                final Value theValue = new IsNaNExpression(aProgram, aInstruction.getOpcodeAddress(), TypeRef.Native.FLOAT,
                        aArguments.get(0));
                final Variable theNewVariable = aTargetBlock
                        .newVariable(aInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), theValue);
                aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);

                return true;
            }
        }
        return false;
    }
}
