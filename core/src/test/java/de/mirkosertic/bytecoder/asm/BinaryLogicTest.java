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
package de.mirkosertic.bytecoder.asm;

import de.mirkosertic.bytecoder.asm.test.UnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(UnitTestRunner.class)
public class BinaryLogicTest {

    @Test
    public void testCharXor() {
        char theValue = 6;
        Assert.assertEquals(4, theValue ^ 2, 0);
    }

    @Test
    public void testByteXor() {
        byte theValue = 6;
        Assert.assertEquals(4, theValue ^ 2, 0);
    }

    @Test
    public void testShortXor() {
        short theValue = 6;
        Assert.assertEquals(4, theValue ^ 2, 0);
    }

    @Test
    public void testIntXor() {
        int theValue = 6;
        Assert.assertEquals(4, theValue ^ 2, 0);
    }

    @Test
    public void testCharOr() {
        char theValue = 1;
        Assert.assertEquals(3, theValue | 2, 0);
    }

    @Test
    public void testByteOr() {
        byte theValue = 1;
        Assert.assertEquals(3, theValue | 2, 0);
    }

    @Test
    public void testShortOr() {
        short theValue = 1;
        Assert.assertEquals(3, theValue | 2, 0);
    }

    @Test
    public void testIntOr() {
        int theValue = 1;
        Assert.assertEquals(3, theValue | 2, 0);
    }

    @Test
    public void testCharAnd() {
        char theValue = 3;
        Assert.assertEquals(2, theValue & 2, 0);
    }

    @Test
    public void testByteAnd() {
        byte theValue = 3;
        Assert.assertEquals(2, theValue & 2, 0);
    }

    @Test
    public void testShortAnd() {
        short theValue = 3;
        Assert.assertEquals(2, theValue & 2, 0);
    }

    @Test
    public void testIntAnd() {
        int theValue = 3;
        Assert.assertEquals(2, theValue & 2, 0);
    }

    @Test
    public void testShiftByte() {
        byte theValue = 2 << 3;
        Assert.assertEquals(16, theValue, 0);
        theValue<<=1;
        Assert.assertEquals(32, theValue, 0);
        theValue>>=1;
        Assert.assertEquals(16, theValue, 0);
        theValue>>=2;
        Assert.assertEquals(4, theValue, 0);
    }

    @Test
    public void testShiftChar() {
        char theValue = 2 << 3;
        Assert.assertEquals(16, theValue, 0);
        theValue<<=1;
        Assert.assertEquals(32, theValue, 0);
        theValue>>=1;
        Assert.assertEquals(16, theValue, 0);
        theValue>>=2;
        Assert.assertEquals(4, theValue, 0);
    }

    @Test
    public void testShiftShort() {
        short theValue = 2 << 3;
        Assert.assertEquals(16, theValue, 0);
        theValue<<=1;
        Assert.assertEquals(32, theValue, 0);
        theValue>>=1;
        Assert.assertEquals(16, theValue, 0);
        theValue>>=2;
        Assert.assertEquals(4, theValue, 0);
    }

    @Test
    public void testShiftInt() {
        int theValue = 2 << 3;
        Assert.assertEquals(16, theValue, 0);
        theValue<<=1;
        Assert.assertEquals(32, theValue, 0);
        theValue>>=1;
        Assert.assertEquals(16, theValue, 0);
        theValue>>=2;
        Assert.assertEquals(4, theValue, 0);
    }

    @Test
    public void testAndFlags() {
        int theValue = 3;
        Assert.assertEquals(1, theValue & 1, 0);
        Assert.assertEquals(2, theValue & 2, 0);
        Assert.assertEquals(0, theValue & 4, 0);
    }

    @Test
    public void testFloatCompare() {
        float theValue = 0.01f;
        boolean thePositive = theValue > 0.0f;
        Assert.assertTrue(thePositive);
    }
}
