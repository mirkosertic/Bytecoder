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
public class ArrayTest {

    public static class Entry {

        public int test;
    }

    private static byte[] bytes = new byte[10];
    private static short[] shorts = new short[10];
    private static char[] chars = new char[10];
    private static int[] ints = new int[10];
    private static float[] floats = new float[10];
    private static Entry[] entries = new Entry[10];

    @Test
    public void testBytes() throws TRuntimeException {
        bytes[4] = (byte) 10;
        TAssert.assertEquals(10, bytes[4], 0);
    }

    @Test
    public void testShorts() throws TRuntimeException {
        shorts[4] = (short) 10;
        TAssert.assertEquals(10, shorts[4], 0);
    }

    @Test
    public void testChars() throws TRuntimeException {
        chars[4] = (char) 10;
        TAssert.assertEquals(10, chars[4], 0);
    }

    @Test
    public void testInts() throws TRuntimeException {
        ints[4] = (int) 10;
        TAssert.assertEquals(10, ints[4], 0);
    }

    @Test
    public void testFloats() throws TRuntimeException {
        floats[4] = (float) 10;
        TAssert.assertEquals(10, floats[4], 0);
    }

    @Test
    public void testEntries() throws TRuntimeException {
        Entry theEntry = new Entry();
        theEntry.test = 10;
        entries[4] = theEntry;
        TAssert.assertEquals(10, entries[4].test, 0);
    }
}
