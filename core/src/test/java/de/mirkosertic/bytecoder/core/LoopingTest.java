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
        final byte[] aOtherData = new byte[10];
        final byte[] theNewData = new byte[byteData.length + aOtherData.length];

        int offset = 0;
        for (int i = 0; i< byteData.length; i++) {
            theNewData[offset++] = byteData[i];
        }

        for (int i=0;i<aOtherData.length;i++) {
            theNewData[offset++] = aOtherData[i];
        }

        byteData = theNewData;
    }

    @Test
    public void testSimpleSum() {
        int theSum = 0;
        final int theCount = 20;
        for (int i=0;i<theCount;i++) {
            theSum+=i;
        }
        final int z = theSum * 2;
    }

    @Test
    public void testSimpleLoop() {
        for (int i=0;i<10;i++) {

        }
    }

    @Test
    public void testComplexFlow() {
        int theIndex = 0;
        while (true) {

            System.out.println(theIndex);

            theIndex++;
            if (theIndex > 10) {
                break;
            }
        }
        System.out.println(theIndex);
    }

    @Test
    public void testComplexer() {
        int a = 0;
        final int b = 10;
        System.out.println("Starting");
        while(true) {
            System.out.println(a);
            System.out.println(b);
            a++;
            if (a==b) {
                break;
            }
        }
        System.out.println("Finished");
    }
}
