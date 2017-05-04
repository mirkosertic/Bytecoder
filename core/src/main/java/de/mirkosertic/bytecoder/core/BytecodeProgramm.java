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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class BytecodeProgramm {

    private final List<BytecodeInstruction> instructions;

    public BytecodeProgramm() {
        instructions = new ArrayList<>();
    }

    public void addInstruction(BytecodeInstruction aInstruction) {
        instructions.add(aInstruction);
    }

    public List<BytecodeInstruction> getInstructions() {
        return instructions;
    }

    private List<BytecodeOpcodeAddress> unifyAndSort(List<BytecodeOpcodeAddress> aResult) {
        HashSet<BytecodeOpcodeAddress> theSet = new HashSet<>();
        theSet.addAll(aResult);
        List<BytecodeOpcodeAddress> theResult = new ArrayList<>();
        theResult.addAll(theSet);
        Collections.sort(theResult, Comparator.comparingInt(BytecodeOpcodeAddress::getAddress));
        return theResult;
    }

    public List<BytecodeOpcodeAddress> getJumpSources() {
        List<BytecodeOpcodeAddress> theResult = new ArrayList<>();
        for (BytecodeInstruction theInstruction : instructions) {
            if (theInstruction.isJumpSource()) {
                theResult.add(theInstruction.getOpcodeAddress());
            }
        }
        return unifyAndSort(theResult);
    }

    public List<BytecodeOpcodeAddress> getPotentialJumpTargets() {
        List<BytecodeOpcodeAddress> theResult = new ArrayList<>();
        for (BytecodeInstruction theInstruction : instructions) {
            for (BytecodeOpcodeAddress thetarget : theInstruction.getPotentialJumpTargets()) {
                theResult.add(thetarget);
            }
        }
        return unifyAndSort(theResult);
    }
}
