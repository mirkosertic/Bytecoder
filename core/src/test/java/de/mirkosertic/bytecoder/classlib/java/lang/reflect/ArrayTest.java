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
package de.mirkosertic.bytecoder.classlib.java.lang.reflect;

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Array;

@RunWith(BytecoderUnitTestRunner.class)
public class ArrayTest {

    @Test
    public void booleanArrayTest() {
        final boolean[] array = new boolean[1];
        Array.setBoolean(array, 0, true);
        Assert.assertTrue(Array.getBoolean(array, 0));
        Assert.assertEquals(1, Array.getLength(array));
    }

    @Test
    public void byteArrayTest() {
        final byte[] array = new byte[1];
        Array.setByte(array, 0, (byte) 42);
        Assert.assertEquals((byte) 42, Array.getByte(array, 0));
        Assert.assertEquals(1, Array.getLength(array));
    }

    @Test
    public void charArrayTest() {
        final char[] array = new char[1];
        Array.setChar(array, 0, 'A');
        Assert.assertEquals('A', Array.getChar(array, 0));
        Assert.assertEquals(1, Array.getLength(array));
    }

    @Test
    public void doubleArrayTest() {
        final double[] array = new double[1];
        Array.setDouble(array, 0, 10d);
        Assert.assertEquals(10d, Array.getDouble(array, 0), 0);
        Assert.assertEquals(1, Array.getLength(array));
    }

    @Test
    public void floatArrayTest() {
        final float[] array = new float[1];
        Array.setFloat(array, 0, 10f);
        Assert.assertEquals(10f, Array.getFloat(array, 0), 0);
        Assert.assertEquals(1, Array.getLength(array));
    }

    @Test
    public void intArrayTest() {
        final int[] array = new int[1];
        Array.setInt(array, 0, 42);
        Assert.assertEquals(42, Array.getInt(array, 0));
        Assert.assertEquals(1, Array.getLength(array));
    }

    @Test
    public void longArrayTest() {
        final long[] array = new long[1];
        Array.setLong(array, 0, 42L);
        Assert.assertEquals(42L, Array.getLong(array, 0));
        Assert.assertEquals(1, Array.getLength(array));
    }

    @Test
    public void shortArrayTest() {
        final short[] array = new short[1];
        Array.setShort(array, 0, (short) 42);
        Assert.assertEquals((short) 42, Array.getShort(array, 0));
        Assert.assertEquals(1, Array.getLength(array));
    }

    @Test
    public void objectArrayTest() {
        final Object[] array = new Object[1];
        Array.set(array, 0, 42);
        Assert.assertEquals(42, Array.get(array, 0));
        Assert.assertEquals(1, Array.getLength(array));
    }

    @Test
    public void testCreateArrayTest() {
        final Object[] array = (Object[]) Array.newInstance(Object.class, 10);
        Assert.assertEquals(10, Array.getLength(array));
    }
}
