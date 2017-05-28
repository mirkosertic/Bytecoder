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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecoderUnitTestRunner.class)
public class SimpleMathTest {

    public static final float PRECISION = .00011f;
    public static final double VALUE = Math.PI * 2 / PRECISION;
    public static final int LENGTH = (int) Math.ceil(VALUE);

    public static int sum(int a, int b) {
        return a + b;
    }

    public static int sub(int a, int b) {
        return a - b;
    }

    public static int mul(int a, int b) {
        return a * b;
    }

    public static int div(int a, int b) {
        return a / b;
    }

    public static int rem(int a, int b) {
        return a % b;
    }

    public static int getInt() {
        return 240;
    }

    public static byte getByte() {
        return (byte) 127;
    }

    public static short getShort() {
        return (short) 240;
    }

    public static float getFloat() {
        return (float) 240;
    }

    @Test
    public void testGetInt() {
        Assert.assertEquals(240, getInt(), 0);
    }

    @Test
    public void testGetByte() {
        Assert.assertEquals(127, getByte(), 0);
    }

    @Test
    public void testGetShort() {
        Assert.assertEquals(240, getShort(), 0);
    }

    @Test
    public void testGetFloat() {
        Assert.assertEquals(240, getFloat(), 0);
    }

    @Test
    public void testAdd() {
        int c = sum(10, 20);
        Assert.assertEquals(30, c, 0);
    }

    @Test
    public void testSub() {
        int c = sub(10, 20);
        Assert.assertEquals(-10, c, 0);
    }

    @Test
    public void testMul() {
        int c = mul(10, 30);
        Assert.assertEquals(300, c, 0);
    }

    @Test
    public void testDiv() {
        int c = div(100, 5);
        Assert.assertEquals(20, c, 0);
    }

    @Test
    public void testRemainder() {
        int c = rem(33, 10);
        Assert.assertEquals(3, c, 0);
    }

    @Test
    public void testComputedLength() {
        // Value is 57119.86598277577
        Assert.assertEquals(57120f, LENGTH, 0);
    }
}