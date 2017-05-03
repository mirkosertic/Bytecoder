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

public class BytecodeInstructionJSR extends BytecodeInstruction {

    private final byte banchbyte1;
    private final byte banchbyte2;

    public BytecodeInstructionJSR(BytecodeOpcodeAddress aOpcodeIndex, byte aBanchbyte1, byte aBanchbyte2) {
        super(aOpcodeIndex);
        banchbyte1 = aBanchbyte1;
        banchbyte2 = aBanchbyte2;
    }
}
