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
public class FloatTest {

    @Test
    public void testRawBitsFromFloat() {
        final int raw = Float.floatToIntBits(1.1999967098236084f);
        System.out.println(raw);
        Assert.assertEquals(1067030910, raw,0);
    }

    @Test
    public void testRawBitsToFloat() {
        final float fl = Float.intBitsToFloat(1067030910);
        System.out.println(fl);
        Assert.assertEquals(1.1999967098236084f, fl,0.000005);
    }

    @Test
    public void testCompare() {
        Assert.assertEquals(Float.compare(10f, 20f), -1, 0);
        Assert.assertEquals(Float.compare(10f, 10f), 0, 0);
        Assert.assertEquals(Float.compare(20f, 10f), 1, 0);
    }

    @Test
    public void testEquals() throws Exception {
        final Float theFloat = 10f;
        assertEquals(theFloat,theFloat);
        assertNotEquals(theFloat, (float) (int) 11);
        assertNotEquals(theFloat, null);
        assertNotEquals(theFloat, "");
    }

    @Test
    public void testHashCode() throws Exception {
        assertEquals(new Float((int) 10), new Float((int) 10));
    }

    @Test
    public void testIntValue() throws Exception {
        assertEquals(10, new Float((int) 10).intValue(), 0);
    }

    @Test
    public void testByteValue() throws Exception {
        assertEquals(10, new Float((int) 10).byteValue(), 0);
    }

    @Test
    public void testShortValue() throws Exception {
        assertEquals(10, new Float((int) 10).shortValue(), 0);
    }

    @Test
    public void testFloatValue() throws Exception {
        assertEquals(10, new Float((int) 10).floatValue(), 0);
    }

    @Test
    public void testLongValue() throws Exception {
        assertEquals(10, new Float((int) 10).longValue(), 0);
    }

    @Test
    public void testDoubleValue() throws Exception {
        assertEquals(10, new Float((int) 10).doubleValue(), 0);
    }

    @Test
    public void testToString() {
        assertEquals("123.5", new Float(123.5).toString());
    }

    @Test
    public void testValueOfInt() {
        assertEquals(123, Float.valueOf(123).intValue(), 0);
    }

    @Test
    public void testValueOfString1() {
        assertEquals(123.25, Float.valueOf("123.25").floatValue(), 0);
    }

    @Test
    public void testValueOfString2() {
        assertEquals(-123.25, Float.valueOf("-123.25").floatValue(), 0);
    }

    @Test
    public void testValueOfString() {
        assertEquals(123.25, Float.valueOf("123.25").floatValue(), 0);
    }

    @Test
    public void testValueOfNegativeString() {
        assertEquals(-123.25, Float.valueOf("-123.25").floatValue(), 0);
    }

    @Test
    public void testParseFloat() {
        assertEquals(-123, Float.parseFloat("-123"), 0);
    }

}