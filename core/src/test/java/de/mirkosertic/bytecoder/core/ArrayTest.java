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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

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
    private static boolean[] booleans = new boolean[10];
    private static float[] floats = new float[10];
    private static Entry[] entries = new Entry[10];

    @Test
    public void testLength() {
        Assert.assertEquals(10, bytes.length, 0);
    }

    @Test
    public void testBytes() {
        bytes[4] = (byte) 10;
        Assert.assertEquals(10, bytes[4], 0);
        Assert.assertEquals(0, bytes[0], 0);
    }

    @Test
    public void testShorts() {
        shorts[4] = (short) 10;
        Assert.assertEquals(10, shorts[4], 0);
        Assert.assertEquals(0, shorts[0], 0);
    }

    @Test
    public void testChars() {
        chars[4] = (char) 10;
        Assert.assertEquals(10, chars[4], 0);
        Assert.assertEquals(0, chars[0], 0);
    }

    @Test
    public void testInts() {
        ints[4] = (int) 10;
        Assert.assertEquals(10, ints[4], 0);
        Assert.assertEquals(0, ints[0], 0);
    }

    @Test
    public void testFloats() {
        floats[4] = (float) 10;
        Assert.assertEquals(10, floats[4], 0);
        Assert.assertEquals(0, floats[0], 0);
    }

    @Test
    public void testBooleans() {
        booleans[4] = true;
        Assert.assertTrue(booleans[4]);
        Assert.assertFalse(booleans[0]);
    }

    @Test
    public void testEntries() {
        Entry theEntry = new Entry();
        Assert.assertNull(entries[0]);
        theEntry.test = 10;
        entries[4] = theEntry;
        Assert.assertEquals(10, entries[4].test, 0);

        Object[] theEmpty = new Object[100];

        Object theEntry0 = theEmpty[12];
        Assert.assertNull(theEntry0);
    }

    @Test
    public void testCObjectArray() {
        String theA = "AB";
        String theC = "ABC";
        String[] theStrings = new String[3];
        theStrings[0] = theA;
        theStrings[2] = theC;
        Assert.assertNotNull(theStrings[0]);
        Assert.assertNull(theStrings[1]);
        Assert.assertNotNull(theStrings[2]);

        Assert.assertSame(theA, theStrings[0]);
        Assert.assertSame(theC, theStrings[2]);
        Assert.assertEquals(2, theStrings[0].length(), 0);
        Assert.assertEquals(3, theStrings[2].length(), 0);
    }
}
