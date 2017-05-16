/*
 * Copyright 2017 Mirko Sertic
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
package de.mirkosertic.bytecoder.core;

import java.util.ArrayList;
import java.util.List;

public class BytecodeProgram {

    private final List<BytecodeInstruction> instructions;

    public BytecodeProgram() {
        instructions = new ArrayList<>();
    }

    public void addInstruction(BytecodeInstruction aInstruction) {
        instructions.add(aInstruction);
    }

    public List<BytecodeInstruction> getInstructions() {
        return instructions;
    }

    public BytecodeProgramJumps buildJumps(BytecodeExceptionTableEntry[] aExceptionHandlerEntries) {
        BytecodeProgramJumps theJumps = new BytecodeProgramJumps();
        for (BytecodeInstruction theInstruction : instructions) {
            if (theInstruction.isJumpSource()) {
                for (BytecodeOpcodeAddress theAddress : theInstruction.getPotentialJumpTargets()) {
                    theJumps.registerJumpFromAToB(theInstruction.getOpcodeAddress(), theAddress);
                }
            }
        }
        for (BytecodeExceptionTableEntry aEntry : aExceptionHandlerEntries) {
            theJumps.registerJumpFromAToB(aEntry.getStartPC(), aEntry.getHandlerPc());
        }
        theJumps.tryToOptimize();
        return theJumps;
    }

    public BytecodeExceptionTableEntry[] getActiveExceptionHandlers(BytecodeOpcodeAddress aAddress, BytecodeExceptionTableEntry[] aExceptionHandlerEntries) {
        List<BytecodeExceptionTableEntry> theResult = new ArrayList<>();
        for (BytecodeExceptionTableEntry aEntry : aExceptionHandlerEntries) {
            if (!aEntry.isFinally()) {
                if (aEntry.getStartPC().isBefore(aAddress) && aEntry.getEndPc().isAfter(aAddress)) {
                    theResult.add(aEntry);
                }
            }
        }
        return theResult.toArray(new BytecodeExceptionTableEntry[theResult.size()]);
    }

    public BytecodeInstruction instructionAt(BytecodeOpcodeAddress aAddress) {
        for (BytecodeInstruction theInstruction : instructions) {
            if (theInstruction.getOpcodeAddress().equals(aAddress)) {
                return theInstruction;
            }
        }
        throw new IllegalArgumentException("No instruction found at " + aAddress.getAddress());
    }

    public boolean containsBackJump(BytecodeProgramJumps.Range aRange) {

        BytecodeInstruction theInstructionAtEnd = instructionAt(aRange.getEnd());
        if (theInstructionAtEnd.isJumpSource()) {
            BytecodeOpcodeAddress[] theAddresses = theInstructionAtEnd.getPotentialJumpTargets();
            for (BytecodeOpcodeAddress theAddress : theAddresses) {
                if (theAddress.equals(aRange.getStart())) {
                    return true;
                }
            }
            return false;
        }
        return false;

        /*for (BytecodeInstruction theInstruction : instructions) {
            BytecodeOpcodeAddress theAddress = theInstruction.getOpcodeAddress();
            if (theAddress.getAddress() >= aRange.getStart().getAddress() &&
                theAddress.getAddress() <= aRange.getEnd().getAddress()) {
                // Instruction is in Range
                if (theInstruction.isJumpSource()) {
                    for (BytecodeOpcodeAddress theTarget : theInstruction.getPotentialJumpTargets()) {
                        if (theTarget.getAddress() == aRange.getStart().getAddress()) {
                            // Jump to top of the block, so it is a backjump
                            return true;
                        }
                    }
                }
            }
        }
        return false;*/
    }
}