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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BytecodeProgramJumpsTest {

    @Test
    public void testSingleCase() {
        BytecodeProgramJumps theJumps = new BytecodeProgramJumps();
        theJumps.registerJumpFromAToB(new BytecodeOpcodeAddress(10), new BytecodeOpcodeAddress(20));
        theJumps.registerJumpFromAToB(new BytecodeOpcodeAddress(30), new BytecodeOpcodeAddress(25));
        assertEquals(1,theJumps.startRangesAt(new BytecodeOpcodeAddress(10)).size());
        assertEquals(1,theJumps.endRangesAt(new BytecodeOpcodeAddress(20)).size());
        assertEquals(1,theJumps.startRangesAt(new BytecodeOpcodeAddress(25)).size());
        assertEquals(1,theJumps.endRangesAt(new BytecodeOpcodeAddress(30)).size());
    }

    @Test
    public void testSinpleOverlap() {
        BytecodeProgramJumps theJumps = new BytecodeProgramJumps();
        theJumps.registerJumpFromAToB(new BytecodeOpcodeAddress(2), new BytecodeOpcodeAddress(13));
        theJumps.registerJumpFromAToB(new BytecodeOpcodeAddress(10), new BytecodeOpcodeAddress(33));
        theJumps.tryToOptimize();
        assertEquals(2,theJumps.startRangesAt(new BytecodeOpcodeAddress(2)).size());
        assertEquals(1,theJumps.endRangesAt(new BytecodeOpcodeAddress(13)).size());
        assertEquals(1,theJumps.endRangesAt(new BytecodeOpcodeAddress(33)).size());
    }
}