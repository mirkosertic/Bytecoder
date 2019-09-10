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

import de.mirkosertic.bytecoder.classlib.VM;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESTATIC;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.ssa.*;

import java.util.List;
import java.util.Objects;

public class VMIntrinsic extends Intrinsic {

    @Override
    public boolean intrinsify(final Program aProgram, final BytecodeInstructionINVOKESTATIC aInstruction, final String aMethodName, final List<Value> aArguments, final BytecodeObjectTypeRef aTargetClass, final RegionNode aTargetBlock, final ParsingHelper aHelper) {
        if (Objects.equals(aTargetClass.name(), VM.class.getName()) && "newRuntimeGeneratedType".equals(aMethodName)) {
            final RuntimeGeneratedTypeExpression theValue = new RuntimeGeneratedTypeExpression(aProgram, aInstruction.getOpcodeAddress(), aArguments.get(1), aArguments.get(2), aArguments.get(3), aArguments.get(0));
            final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, theValue);
            aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);

            return true;
        }
        return false;
    }
}
