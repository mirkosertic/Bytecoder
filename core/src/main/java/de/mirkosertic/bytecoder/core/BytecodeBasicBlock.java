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

public class BytecodeBasicBlock {

    private final List<BytecodeInstruction> instructions;
    private final List<BytecodeBasicBlock> successors;

    public BytecodeBasicBlock() {
        instructions = new ArrayList<>();
        successors = new ArrayList<>();
    }

    public BytecodeOpcodeAddress getStartAddress() {
        return instructions.get(0).getOpcodeAddress();
    }

    public void addInstruction(BytecodeInstruction aInstruction) {
        instructions.add(aInstruction);
    }

    public List<BytecodeInstruction> getInstructions() {
        return instructions;
    }

    public BytecodeInstruction getLastInstruction() {
        return instructions.get(instructions.size() - 1);
    }
}
