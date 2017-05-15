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
import org.junit.runner.RunWith;

import de.mirkosertic.bytecoder.classlib.java.lang.TRuntimeException;
import de.mirkosertic.bytecoder.classlib.org.junit.TAssert;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;

@RunWith(BytecoderUnitTestRunner.class)
public class SimpleMathTest {

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
    public void testGetInt() throws TRuntimeException {
        TAssert.assertEquals(220, getInt(), 0);
    }

    @Test
    public void testGetByte() throws TRuntimeException {
        TAssert.assertEquals(127, getByte(), 0);
    }

    @Test
    public void testGetShort() throws TRuntimeException {
        TAssert.assertEquals(240, getShort(), 0);
    }

    @Test
    public void testGetFloat() throws TRuntimeException {
        TAssert.assertEquals(240, getFloat(), 0);
    }

    @Test
    public void testAdd() throws TRuntimeException {
        int c = sum(10, 20);
        TAssert.assertEquals(30, c, 0);
    }

    @Test
    public void testSub() throws TRuntimeException {
        int c = sub(10, 20);
        TAssert.assertEquals(-10, c, 0);
    }

    @Test
    public void testMul() throws TRuntimeException {
        int c = mul(10, 30);
        TAssert.assertEquals(300, c, 0);
    }

    @Test
    public void testDiv() throws TRuntimeException {
        int c = div(100, 5);
        TAssert.assertEquals(20, c, 0);
    }

    @Test
    public void testRemainder() throws TRuntimeException {
        int c = rem(33, 10);
        TAssert.assertEquals(3, c, 0);
    }
}