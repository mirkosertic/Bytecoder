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

import java.util.HashSet;
import java.util.Set;

public class BytecodeInstructionLOOKUPSWITCH extends BytecodeInstruction {

    public static class Pair {

        private final long match;
        private final long offset;

        public Pair(long match, long offset) {
            this.match = match;
            this.offset = offset;
        }

        public long getMatch() {
            return match;
        }

        public long getOffset() {
            return offset;
        }
    }

    private final long defaultValue;
    private final Pair[] pairs;

    public BytecodeInstructionLOOKUPSWITCH(BytecodeOpcodeAddress aIndex, long aDefaultValue, Pair[] aPairs) {
        super(aIndex);
        defaultValue = aDefaultValue;
        pairs = aPairs;
    }

    public Pair[] getPairs() {
        return pairs;
    }

    public BytecodeOpcodeAddress getDefaultJumpTarget() {
        return getOpcodeAddress().add((int ) defaultValue);
    }

    @Override
    public BytecodeOpcodeAddress[] getPotentialJumpTargets() {
        Set<BytecodeOpcodeAddress> theResult = new HashSet<>();
        for (Pair thePair : pairs) {
            theResult.add(getOpcodeAddress().add((int) thePair.offset));
        }
        theResult.add(getDefaultJumpTarget());
        return theResult.toArray(new BytecodeOpcodeAddress[theResult.size()]);
    }

    @Override
    public boolean isJumpSource() {
        return true;
    }
}
