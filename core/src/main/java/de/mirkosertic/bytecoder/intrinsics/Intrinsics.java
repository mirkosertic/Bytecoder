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
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKEVIRTUAL;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.ssa.*;

import java.util.ArrayList;
import java.util.List;

public class Intrinsics {

    private final List<Intrinsic> intrinsics;

    public Intrinsics() {
        intrinsics = new ArrayList<>();
        intrinsics.add(new MemoryManagerIntrinsic());
        intrinsics.add(new JavaUtilArrayIntrinsic());
        intrinsics.add(new VMIntrinsic());
        intrinsics.add(new JavaLangStrictMathIntrinsic());
        intrinsics.add(new JavaLangMathIntrinsic());
        intrinsics.add(new RuntimeClassIntrinsic());
        intrinsics.add(new ObjectConstructorCallIntrinsic());
    }

    public void add(final Intrinsic aIntrinsic) {
        intrinsics.add(aIntrinsic);
    }

    public boolean intrinsify(final Program aProgram, final BytecodeInstructionINVOKESTATIC aInstruction, final List<Value> aArguments,
                              final BytecodeObjectTypeRef aObjectType, final RegionNode aTargetBlock, final ParsingHelper aHelper) {
        final String theMethodName = aInstruction.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue();
        for (final Intrinsic intrinsic : intrinsics) {
            if (intrinsic.intrinsify(aProgram, aInstruction, theMethodName, aArguments, aObjectType, aTargetBlock, aHelper)) {
                return true;
            }
        }
        return false;
    }

    public boolean intrinsify(final Program aProgram, final BytecodeInstructionINVOKESPECIAL aInstruction,
                              final BytecodeObjectTypeRef aType, final List<Value> aArguments,
                              final Variable aTarget, final RegionNode aTargetBlock, final ParsingHelper aHelper) {
        final String theMethodName = aInstruction.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue();
        for (final Intrinsic intrinsic : intrinsics) {
            if (intrinsic.intrinsify(aProgram, aInstruction, theMethodName, aType, aArguments, aTarget, aTargetBlock, aHelper)) {
                return true;
            }
        }
        return false;
    }

    public boolean intrinsify(final Program aProgram, final BytecodeInstructionINVOKEVIRTUAL aInstruction,
                              final List<Value> aArguments,
                              final Value aTarget, final RegionNode aTargetBlock, final ParsingHelper aHelper) {
        final String theMethodName = aInstruction.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue();
        for (final Intrinsic intrinsic : intrinsics) {
            if (intrinsic.intrinsify(aProgram, aInstruction, theMethodName, aArguments, aTarget, aTargetBlock, aHelper)) {
                return true;
            }
        }
        return false;
    }

}
