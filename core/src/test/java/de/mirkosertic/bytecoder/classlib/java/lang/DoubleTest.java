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
public class DoubleTest {

    @Test
    public void testCompare() {
        Assert.assertEquals(Double.compare(10d, 20d), -1, 0);
        Assert.assertEquals(Double.compare(10d, 10d), 0, 0);
        Assert.assertEquals(Double.compare(20d, 10d), 1, 0);
    }

    @Test
    public void testEquals() throws Exception {
        final Double theDouble = 10d;
        assertEquals(theDouble,theDouble);
        assertNotEquals(theDouble, 11d);
        assertNotEquals(theDouble, null);
        assertNotEquals(theDouble, "");
    }

    @Test
    public void testHashCode() throws Exception {
        assertEquals(10d, 10d, 0);
    }

    @Test
    public void testIntValue() throws Exception {
        assertEquals(10, new Double(10d).intValue(), 0);
    }

    @Test
    public void testByteValue() throws Exception {
        assertEquals(10, new Double(10d).byteValue(), 0);
    }

    @Test
    public void testShortValue() throws Exception {
        assertEquals(10, new Double(10d).shortValue(), 0);
    }

    @Test
    public void testFloatValue() throws Exception {
        assertEquals(10, new Double(10d).floatValue(), 0);
    }

    @Test
    public void testLongValue() throws Exception {
        assertEquals(10, new Double(10d).longValue(), 0);
    }

    @Test
    public void testDoubleValue() throws Exception {
        assertEquals(10, new Double(10d).doubleValue(), 0);
    }

    @Test
    public void testToStringPositive() {
        assertEquals("123.5", new Double(123.5d).toString());
    }

    @Test
    public void testToString() {
        assertEquals("123.0", new Double(123.00d).toString());
    }

    @Test
    public void testToStringNegative() {
        assertEquals("-123.5", new Double(-123.5d).toString());
    }


    @Test
    public void testValueOfInt() {
        assertEquals(123, Double.valueOf(123).intValue(), 0);
    }

    @Test
    public void testValueOfString1() {
        assertEquals(123.25d, Double.valueOf("123.25").doubleValue(), 0);
    }

    @Test
    public void testValueOfString2() {
        assertEquals(-123.25, Double.valueOf("-123.25").doubleValue(), 0);
    }

    @Test
    public void testValueOfString() {
        assertEquals(123.25d, Double.valueOf("123.25").doubleValue(), 0);
    }

    @Test
    public void testValueOfNegativeString() {
        assertEquals(-123.25, Double.valueOf("-123.25").doubleValue(), 0);
    }

    @Test
    public void testParseDouble() {
        assertEquals(-123, Double.parseDouble("-123"), 0);
    }
}