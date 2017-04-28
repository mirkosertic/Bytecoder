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

public class BytecodeInstructionANEWARRAY implements BytecodeInstruction {

    private final byte indexbyte1;
    private final byte indexbyte2;

    public BytecodeInstructionANEWARRAY(byte indebyte1, byte indexbyte2) {
        this.indexbyte1 = indebyte1;
        this.indexbyte2 = indexbyte2;
    }
}
