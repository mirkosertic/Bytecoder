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
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKEINTERFACE;
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

public class Intrinsic {

    public boolean intrinsify(final Program aProgram, final BytecodeInstructionINVOKESTATIC aInstruction, final String aMethodName, final List<Value> aArguments,
                              final BytecodeObjectTypeRef aTargetClass, final RegionNode aTargetBlock, final ParsingHelper aHelper) {
        return false;
    }

    public boolean intrinsify(final Program aProgram, final BytecodeInstructionINVOKESPECIAL aInstruction, final String aMethodName,
                              final BytecodeObjectTypeRef aType, final List<Value> aArguments,
                              final Variable aTarget, final RegionNode aTargetBlock, final ParsingHelper aHelper) {
        return false;
    }

    public boolean intrinsify(final Program aProgram, final BytecodeInstructionINVOKEVIRTUAL aInstruction, final String aMethodName,
                              final List<Value> aArguments,
                              final Value aTarget, final RegionNode aTargetBlock, final ParsingHelper aHelper) {
        return false;
    }

    public boolean intrinsify(final Program aProgram, final BytecodeInstructionGETSTATIC aInstruction, final String aFieldName, final BytecodeObjectTypeRef aTtargetType, final RegionNode aTargetBlock, final ParsingHelper aHelper) {
        return false;
    }

    public boolean intrinsify(final Program aProgram, final BytecodeInstructionPUTSTATIC aInstruction, final String aFieldName, final BytecodeObjectTypeRef aTtargetType, final Value aValue, final RegionNode aTargetBlock, final ParsingHelper aHelper) {
        return false;
    }

    public boolean intrinsify(final Program aProgram, final BytecodeInstructionINVOKEINTERFACE aInstruction, final String aMethodName, final Value aInvocationTarget, final List<Value> aArguments, final BytecodeObjectTypeRef aObjectType, final RegionNode aTargetBlock, final ParsingHelper aHelper) {
        return false;
    }
}
