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
package de.mirkosertic.bytecoder.classlib.java.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecoderUnitTestRunner.class)
public class IntegerTest {

    @Test
    public void testEquals() {
        final Integer theInteger = new Integer((int) 10);
        assertEquals(theInteger,theInteger);
        assertNotEquals(theInteger, new Integer((int) 11));
        assertNotEquals(theInteger, null);
        assertNotEquals(theInteger, "");
    }

    @Test
    public void testHashCode() {
        assertEquals(new Integer((int) 10).hashCode(), new Integer((int) 10).hashCode(), 0);
    }

    @Test
    public void testIntValue() {
        assertEquals(10, new Integer((int) 10).intValue(), 0);
    }

    @Test
    public void testByteValue() {
        assertEquals(10, new Integer((int) 10).byteValue(), 0);
    }

    @Test
    public void testShortValue() {
        assertEquals(10, new Integer((int) 10).shortValue(), 0);
    }

    @Test
    public void testFloatValue() {
        assertEquals(10, new Integer((int) 10).floatValue(), 0);
    }

    @Test
    public void testLongValue() {
        assertEquals(10, new Integer((int) 10).longValue(), 0);
    }

    @Test
    public void testDoubleValue() {
        assertEquals(10, new Integer((int) 10).doubleValue(), 0);
    }

    @Test
    public void testToString() {
        assertEquals("123", new Integer(123).toString());
    }

    @Test
    public void testValueOfInt() {
        assertEquals(123, Integer.valueOf(123).intValue(), 0);
    }

    @Test
    public void testValueOfString() {
        assertEquals(123, Integer.valueOf("123").intValue(), 0);
    }

    @Test
    public void testValueOfNegativeString() {
        assertEquals(-123, Integer.valueOf("-123").intValue(), 0);
    }

    @Test
    public void testParseInt() {
        assertEquals(-123, Integer.parseInt("-123"), 0);
    }

    @Test
    public void testToHexString() {
        assertEquals("3039", Integer.toHexString(12345));
    }

    @Test
    public void parseIntRadix16() {
        final int value = Integer.parseInt("1F", 16);
        assertEquals(31, value);
    }

    @Test
    public void parseIntRadix10() {
        final int value = Integer.parseInt("10", 10);
        assertEquals(10, value);
    }

    @Test
    public void testBitCount() {
        final int count = Integer.bitCount(1234);
        Assert.assertEquals(5, count, 0);
    }

    @Test
    public void testNumberOfTrailingZeros() {
        final int count = Integer.numberOfTrailingZeros(1234);
        Assert.assertEquals(1, count, 0);
    }

    @Test
    public void testNumberOfLeadingZeros() {
        final int count = Integer.numberOfLeadingZeros(1234);
        Assert.assertEquals(21, count, 0);
    }
}