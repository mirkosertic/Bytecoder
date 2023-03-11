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

import de.mirkosertic.bytecoder.core.test.UnitTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(UnitTestRunner.class)
public class ByteTest {

    @Test
    public void testEquals() {
        final Byte theByte = new Byte((byte) 10);
        assertEquals(theByte,theByte);
        assertNotEquals(theByte, new Short((short) 11));
        assertNotEquals(theByte, null);
        assertNotEquals(theByte, "");
    }

    @Test
    public void testHashCode() {
        assertEquals(new Byte((byte) 10).hashCode(), 10, 0);
    }

    @Test
    public void testIntValue() {
        assertEquals(10, new Byte((byte) 10).intValue(), 0);
    }

    @Test
    public void testByteValue() {
        assertEquals(10, new Byte((byte) 10).byteValue(), 0);
    }

    @Test
    public void testShortValue() {
        assertEquals(10, new Byte((byte) 10).shortValue(), 0);
    }

    @Test
    public void testFloatValue() {
        assertEquals(10, new Byte((byte) 10).floatValue(), 0);
    }

    @Test
    public void testLongValue() {
        assertEquals(10, new Byte((byte) 10).longValue(), 0);
    }

    @Test
    public void testDoubleValue() {
        assertEquals(10, new Byte((byte) 10).doubleValue(), 0);
    }

    @Test
    public void testToString() {
        assertEquals("123", new Byte((byte) 123).toString());
    }

    @Test
    public void testToString2() {
        final String str = Byte.toString((byte) 123);
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
