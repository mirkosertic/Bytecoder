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

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(BytecoderUnitTestRunner.class)
public class LongTest {

    @Test
    public void testEquals() {
        final Long theLong = new Long((int) 10);
        assertEquals(theLong,theLong);
        assertNotEquals(theLong, new Long((int) 11));
        assertNotEquals(theLong, null);
        assertNotEquals(theLong, "");
    }

    @Test
    public void testHashCode() {
        assertEquals(new Long((int) 10), new Long((int) 10));
    }

    @Test
    public void testIntValue() {
        assertEquals(10, new Long((int) 10).intValue(), 0);
    }

    @Test
    public void testByteValue() {
        assertEquals(10, new Long((int) 10).byteValue(), 0);
    }

    @Test
    public void testShortValue() {
        assertEquals(10, new Long((int) 10).shortValue(), 0);
    }

    @Test
    public void testFloatValue() {
        assertEquals(10, new Long((int) 10).floatValue(), 0);
    }

    @Test
    public void testLongValue() {
        assertEquals(10, new Long((int) 10).longValue(), 0);
    }

    @Test
    public void testDoubleValue() {
        assertEquals(10, new Long((int) 10).doubleValue(), 0);
    }

    @Test
    public void testToString() {
        assertEquals("123", new Long(123).toString());
    }

    @Test
    public void testValueOfInt() {
        assertEquals(123, Long.valueOf(123).intValue(), 0);
    }

    @Test
    public void testValueOfString() {
        final Long theValue = Long.valueOf("123");
        System.out.println(theValue.intValue());
        assertEquals(123, theValue.intValue(), 0);
    }

    @Test
    public void testValueOfNegativeString() {
        final Long theValue = Long.valueOf("-123");
        System.out.println(theValue.intValue());
        assertEquals(-123, theValue.intValue(), 0);
    }

    @Test
    public void testParseLong() {
        assertEquals(-123, Long.parseLong("-123"), 0);
    }

    @Test
    public void testToHexString() {
        final String theHexString = Long.toHexString(12345L);
        System.out.println(theHexString);
        assertEquals("3039", theHexString);
    }

    @Test
    public void testPrintLong() {
        final long l = 1000L;
        System.out.println("Long value");
        System.out.println(l);
    }

    /*
    @Test
    public void testBitCount() {
        final int count = Long.bitCount(1234L);
        Assert.assertEquals(5, count, 0);
    }

    @Test
    public void testNumberOfTrailingZeros() {
        final int count = Long.numberOfTrailingZeros(1234L);
        Assert.assertEquals(1, count, 0);
    }
    */

    @Test
    public void testNumberOfLeadingZeros() {
        final int count = Long.numberOfLeadingZeros(1234L);
        Assert.assertEquals(53, count, 0);
    }

    @Test
    public void testCompare() {
        assertEquals(0, Long.compare(10L, 10L));
        assertEquals(1, Long.compare(20L, 10L));
        assertEquals(-1, Long.compare(10L, 20L));
    }
}