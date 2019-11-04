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

import java.util.ArrayList;
import java.util.List;

import de.mirkosertic.bytecoder.core.BytecodeInstructionGETSTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESPECIAL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKEVIRTUAL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPUTSTATIC;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.ssa.ParsingHelper;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;

public class Intrinsics {

    private final List<Intrinsic> intrinsics;

    public Intrinsics() {
        intrinsics = new ArrayList<>();
        intrinsics.add(new MemoryManagerIntrinsic());
        intrinsics.add(new JavaUtilArrayIntrinsic());
        intrinsics.add(new VMIntrinsic());
        intrinsics.add(new JavaLangStrictMathIntrinsic());
        intrinsics.add(new JavaLangMathIntrinsic());
        intrinsics.add(new JavaLangClassIntrinsic());
        intrinsics.add(new ObjectConstructorCallIntrinsic());
        intrinsics.add(new JavaLangEnumIntrinsic());
        intrinsics.add(new JavaLangFloatIntrinsic());
        intrinsics.add(new JavaLangDoubleIntrinsic());
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

    public boolean intrinsify(final Program aProgram, final BytecodeInstructionGETSTATIC aInstruction, final RegionNode aTargetBlock, final ParsingHelper aHelper) {
        final String theFieldName = aInstruction.getConstant().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue();
        final BytecodeObjectTypeRef theTargetType = BytecodeObjectTypeRef.fromUtf8Constant(aInstruction.getConstant().getClassIndex().getClassConstant().getConstant());
        for (final Intrinsic intrinsic : intrinsics) {
            if (intrinsic.intrinsify(aProgram, aInstruction, theFieldName, theTargetType, aTargetBlock, aHelper)) {
                return true;
            }
        }
        return false;
    }

    public boolean intrinsify(final Program aProgram, final BytecodeInstructionPUTSTATIC aInstruction, final Value aValue, final RegionNode aTargetBlock, final ParsingHelper aHelper) {
        final String theFieldName = aInstruction.getConstant().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue();
        final BytecodeObjectTypeRef theTargetType = BytecodeObjectTypeRef.fromUtf8Constant(aInstruction.getConstant().getClassIndex().getClassConstant().getConstant());
        for (final Intrinsic intrinsic : intrinsics) {
            if (intrinsic.intrinsify(aProgram, aInstruction, theFieldName, theTargetType, aValue, aTargetBlock, aHelper)) {
                return true;
            }
        }
        return false;
    }
}