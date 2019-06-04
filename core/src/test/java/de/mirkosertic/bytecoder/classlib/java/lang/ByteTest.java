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
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(BytecoderUnitTestRunner.class)
public class ByteTest {

    @Test
    public void testEquals() throws Exception {
        final Byte theByte = new Byte((byte) 10);
        assertEquals(theByte,theByte);
        assertNotEquals(theByte, new Short((short) 11));
        assertNotEquals(theByte, null);
        assertNotEquals(theByte, "");
    }

    @Test
    public void testHashCode() throws Exception {
        System.out.println(new Byte((byte) 10).hashCode());
        assertEquals(new Byte((byte) 10).hashCode(), 10, 0);
    }

    @Test
    public void testIntValue() throws Exception {
        assertEquals(10, new Byte((byte) 10).intValue(), 0);
    }

    @Test
    public void testByteValue() throws Exception {
        assertEquals(10, new Byte((byte) 10).byteValue(), 0);
    }

    @Test
    public void testShortValue() throws Exception {
        assertEquals(10, new Byte((byte) 10).shortValue(), 0);
    }

    @Test
    public void testFloatValue() throws Exception {
        assertEquals(10, new Byte((byte) 10).floatValue(), 0);
    }

    @Test
    public void testLongValue() throws Exception {
        assertEquals(10, new Byte((byte) 10).longValue(), 0);
    }

    @Test
    public void testDoubleValue() throws Exception {
        assertEquals(10, new Byte((byte) 10).doubleValue(), 0);
    }

    @Test
    public void testToString() {
        System.out.println(new Byte((byte) 123).toString());
        assertEquals("123", new Byte((byte) 123).toString());
    }

    @Test
    public void testToString2() {
        final String str = Byte.toString((byte) 123);
        System.out.println(str);
        assertEquals("123", str);
    }

    @Test
    public void testValueOfInt() {
        assertEquals(123, Byte.valueOf((byte) 123).intValue(), 0);
    }

    @Test
    public void testValueOfString() {
        assertEquals(123, Byte.valueOf("123").intValue(), 0);
    }

    @Test
    public void testValueOfNegativeString() {
        assertEquals(-123, Byte.valueOf("-123").intValue(), 0);
    }

    @Test
    public void testParseByte() {
        assertEquals(-123, Byte.parseByte("-123"), 0);
    }
}