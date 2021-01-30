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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BytecodeBasicBlock {

    public enum Type {
        NORMAL,
        EXCEPTION_HANDLER,
        FINALLY
    }

    private final List<BytecodeInstruction> instructions;
    private final List<BytecodeBasicBlock> successors;
    private final Type type;
    private final Set<BytecodeUtf8Constant> catchType;

    public BytecodeBasicBlock(final Type aType) {
        instructions = new ArrayList<>();
        successors = new ArrayList<>();
        type = aType;
        catchType = null;
    }
    public BytecodeBasicBlock(final Set<BytecodeUtf8Constant> aCatchType) {
        instructions = new ArrayList<>();
        successors = new ArrayList<>();
        type = Type.EXCEPTION_HANDLER;
        catchType = aCatchType;
    }

    public Set<BytecodeUtf8Constant> getCatchType() {
        return catchType;
    }

    public void addSuccessor(final BytecodeBasicBlock aBasicBlock) {
        successors.add(aBasicBlock);
    }

    public List<BytecodeBasicBlock> getSuccessors() {
        return successors;
    }

    public Type getType() {
        return type;
    }

    public BytecodeOpcodeAddress getStartAddress() {
        return instructions.get(0).getOpcodeAddress();
    }

    public void addInstruction(final BytecodeInstruction aInstruction) {
        instructions.add(aInstruction);
    }

    public List<BytecodeInstruction> getInstructions() {
        return instructions;
    }

    public boolean endsWithReturn() {
        final BytecodeInstruction theLastInstruction = instructions.get(instructions.size() - 1);
        return theLastInstruction instanceof BytecodeInstructionRETURN ||
                theLastInstruction instanceof BytecodeInstructionGenericRETURN ||
                theLastInstruction instanceof BytecodeInstructionObjectRETURN;
    }

    public boolean endsWithGoto() {
        final BytecodeInstruction theLastInstruction = instructions.get(instructions.size() - 1);
        return theLastInstruction instanceof BytecodeInstructionGOTO;
    }

    public boolean endsWithThrow() {
        final BytecodeInstruction theLastInstruction = instructions.get(instructions.size() - 1);
        return theLastInstruction instanceof BytecodeInstructionATHROW;
    }

    public BytecodeInstruction lastInstruction() {
        return instructions.get(instructions.size() - 1);
    }
}