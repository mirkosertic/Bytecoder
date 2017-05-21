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

import java.util.*;

public class BytecodeProgram {

    private final List<BytecodeInstruction> instructions;
    private final List<BytecodeExceptionTableEntry> exceptionHandlers;

    public BytecodeProgram() {
        instructions = new ArrayList<>();
        exceptionHandlers = new ArrayList<>();
    }

    public void addInstruction(BytecodeInstruction aInstruction) {
        instructions.add(aInstruction);
    }

    public void addExceptionHandler(BytecodeExceptionTableEntry aHandler) {
        exceptionHandlers.add(aHandler);
    }

    public List<BytecodeInstruction> getInstructions() {
        return instructions;
    }

    public BytecodeExceptionTableEntry[] getActiveExceptionHandlers(BytecodeOpcodeAddress aAddress, List<BytecodeExceptionTableEntry> aExceptionHandlerEntries) {
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

    public int getNextInstructionAddress(BytecodeInstruction aInstruction) {
        int p = instructions.indexOf(aInstruction);
        if (p== instructions.size() -1) {
            return -1;
        }
        return instructions.get(p + 1).getOpcodeAddress().getAddress();
    }

    public boolean isStartOfTryBlock(BytecodeOpcodeAddress aAddress) {
        for (BytecodeExceptionTableEntry aEntry : exceptionHandlers) {
            if (aAddress.equals(aEntry.getStartPC())) {
                return true;
            }
        }
        return false;
    }

    public Set<BytecodeOpcodeAddress> getJumpTargets() {
        Set<BytecodeOpcodeAddress> theJumpTarget = new HashSet();
        for (BytecodeInstruction theInstruction : instructions) {
            if (theInstruction.isJumpSource()) {
                theJumpTarget.addAll(Arrays.asList(theInstruction.getPotentialJumpTargets()));
            }
        }
        for (BytecodeExceptionTableEntry aEntry: exceptionHandlers) {
            theJumpTarget.add(aEntry.getHandlerPc());
        }
        return theJumpTarget;
    }

    public List<BytecodeExceptionTableEntry> getExceptionHandlers() {
        return exceptionHandlers;
    }
}