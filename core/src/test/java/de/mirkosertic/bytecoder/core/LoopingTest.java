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

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecoderUnitTestRunner.class)
public class LoopingTest {

    @Test
    public void testLoop() {
        byte[] byteData = new byte[10];
        byte[] aOtherData = new byte[10];
        byte[] theNewData = new byte[byteData.length + aOtherData.length];

        int offset = 0;
        for (int i = 0; i< byteData.length; i++) {
            theNewData[offset++] = byteData[i];
        }

        for (int i=0;i<aOtherData.length;i++) {
            theNewData[offset++] = aOtherData[i];
        }

        byteData = theNewData;
    }
}
