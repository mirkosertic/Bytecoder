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

import de.mirkosertic.bytecoder.classlib.java.lang.TRuntimeException;
import de.mirkosertic.bytecoder.classlib.org.junit.TAssert;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecoderUnitTestRunner.class)
public class BinaryLogicTest {

    @Test
    public void testShiftByte() throws TRuntimeException {
        byte theValue = 2 << 3;
        TAssert.assertEquals(16, theValue, 0);
        theValue<<=1;
        TAssert.assertEquals(32, theValue, 0);
        theValue>>=1;
        TAssert.assertEquals(16, theValue, 0);
        theValue>>=2;
        TAssert.assertEquals(4, theValue, 0);
    }

    @Test
    public void testShiftChar() throws TRuntimeException {
        char theValue = 2 << 3;
        TAssert.assertEquals(16, theValue, 0);
        theValue<<=1;
        TAssert.assertEquals(32, theValue, 0);
        theValue>>=1;
        TAssert.assertEquals(16, theValue, 0);
        theValue>>=2;
        TAssert.assertEquals(4, theValue, 0);
    }

    @Test
    public void testShiftShort() throws TRuntimeException {
        short theValue = 2 << 3;
        TAssert.assertEquals(16, theValue, 0);
        theValue<<=1;
        TAssert.assertEquals(32, theValue, 0);
        theValue>>=1;
        TAssert.assertEquals(16, theValue, 0);
        theValue>>=2;
        TAssert.assertEquals(4, theValue, 0);
    }

    @Test
    public void testShiftInt() throws TRuntimeException {
        int theValue = 2 << 3;
        TAssert.assertEquals(16, theValue, 0);
        theValue<<=1;
        TAssert.assertEquals(32, theValue, 0);
        theValue>>=1;
        TAssert.assertEquals(16, theValue, 0);
        theValue>>=2;
        TAssert.assertEquals(4, theValue, 0);
    }
}
