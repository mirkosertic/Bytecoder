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

public class BytecodeInstructionTABLESWITCH extends BytecodeInstruction {

    private final long defaultValue;
    private final long lowValue;
    private final long highValue;
    private final long offsets[];

    public BytecodeInstructionTABLESWITCH(BytecodeOpcodeAddress aOpcodeIndex, long aDefaultValue, long aLowValue, long aHighValue, long[] aOffsets) {
        super(aOpcodeIndex);
        defaultValue = aDefaultValue;
        lowValue = aLowValue;
        highValue = aHighValue;
        offsets = aOffsets;
    }

    public long getLowValue() {
        return lowValue;
    }

    public long getHighValue() {
        return highValue;
    }

    public long[] getOffsets() {
        return offsets;
    }

    @Override
    public BytecodeOpcodeAddress[] getPotentialJumpTargets() {
        Set<BytecodeOpcodeAddress> theResult = new HashSet<>();
        for (long theOffset : getOffsets()) {
            theResult.add(getOpcodeAddress().add((int) theOffset));
        }
        theResult.add(getDefaultJumpTarget());
        return theResult.toArray(new BytecodeOpcodeAddress[theResult.size()]);
    }

    @Override
    public boolean isJumpSource() {
        return true;
    }

    public BytecodeOpcodeAddress getDefaultJumpTarget() {
        return getOpcodeAddress().add((int) defaultValue);
    }
}