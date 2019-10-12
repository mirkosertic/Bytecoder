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
package de.mirkosertic.bytecoder.classlib.java.util;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SubstitutesInClass(completeReplace = true)
public class TArrays {

    public static void sort(final long[] aData) {
    }

    public static void sort(final Object[] aData, final int aStart, final int aEnd) {
    }

    public static void sort(final Object[] aData, final Comparator<Object> c) {
    }

    public static int binarySearch(final int[] aData, final int value) {
        return -1;
    }

    public static int binarySearch(final Object[] aData, final Object aValue, final Comparator<Object> c) {
        return -1;
    }

    public static <T> List<T> asList(final T... aValues) {
        final ArrayList<T> theResult = new ArrayList<>();
        for (final T theValue : aValues) {
            theResult.add(theValue);
        }
        return theResult;
    }

    public static <T> T[] copyOf(final T[] original, final int newLength) {
        return copyOf(original, newLength, null);
    }

    public static <T> T[] copyOf(final T[] original, final int newLength, final Class aType) {
        final T[] theResult = (T[]) new Object[newLength];
        for (int i=0;i<Math.min(newLength, original.length);i++) {
            theResult[i] = original[i];
        }
        return theResult;
    }

    public static int[] copyOf(final int[] original, final int newLength) {
        final int[] theResult = new int[newLength];
        for (int i=0;i<Math.min(newLength, original.length);i++) {
            theResult[i] = original[i];
        }
        return theResult;
    }

    public static byte[] copyOf(final byte[] original, final int newLength) {
        final byte[] theResult = new byte[newLength];
        for (int i=0;i<Math.min(newLength, original.length);i++) {
            theResult[i] = original[i];
        }
        return theResult;
    }

    public static float[] copyOf(final float[] original, final int newLength) {
        final float[] theResult = new float[newLength];
        for (int i=0;i<Math.min(newLength, original.length);i++) {
            theResult[i] = original[i];
        }
        return theResult;
    }

    public static double[] copyOf(final double[] original, final int newLength) {
        final double[] theResult = new double[newLength];
        for (int i=0;i<Math.min(newLength, original.length);i++) {
            theResult[i] = original[i];
        }
        return theResult;
    }

    public static char[] copyOf(final char[] original, final int newLength) {
        final char[] theResult = new char[newLength];
        for (int i=0;i<Math.min(newLength, original.length);i++) {
            theResult[i] = original[i];
        }
        return theResult;
    }

    public static <T> T[] copyOfRange(final T[] original, final int from, final int to, final Class aType) {
        final T[] theResult = (T[]) new Object[to - from];
        for (int i=from;i<to;i++) {
            theResult[i-from] = original[i];
        }
        return theResult;
    }

    public static Object[] copyOfRange(final Object[] original, final int from, final int to) {
        final Object[] theResult = new Object[to - from];
        for (int i=from;i<to;i++) {
            theResult[i-from] = original[i];
        }
        return theResult;
    }

    public static int[] copyOfRange(final int[] original, final int from, final int to) {
        final int[] theResult = new int[to - from];
        for (int i=from;i<to;i++) {
            theResult[i-from] = original[i];
        }
        return theResult;
    }

    public static byte[] copyOfRange(final byte[] original, final int from, final int to) {
        final byte[] theResult = new byte[to - from];
        for (int i=from;i<to;i++) {
            theResult[i-from] = original[i];
        }
        return theResult;
    }

    public static void fill(final char[] a, final int fromIndex, final int toIndex, final char val) {
        for (int i = fromIndex; i < toIndex; i++)
            a[i] = val;
    }

    public static void fill(final int[] a, final int fromIndex, final int toIndex, final int val) {
        for (int i = fromIndex; i < toIndex; i++)
            a[i] = val;
    }

    public static void fill(final byte[] a, final int fromIndex, final int toIndex, final byte val) {
        for (int i = fromIndex; i < toIndex; i++)
            a[i] = val;
    }

    public static void fill(final int[] a, final int val) {
        for (int i = 0, len = a.length; i < len; i++)
            a[i] = val;
    }
}