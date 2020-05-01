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
import de.mirkosertic.bytecoder.classlib.VM;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@SubstitutesInClass(completeReplace = false)
public class TArrays {

    public static <T> void sort(final T[] aData, final int aStart, final int aEnd) {
        Quicksort.quickSortComparable(aData, aStart, aEnd);
    }

    public static <T> void sort(final T[] aData, final int aStart, final int aEnd, final Comparator<T> c) {
        Quicksort.quickSort(aData, aStart, aEnd, c);
    }

    public static <T> void sort(final T[] aData, final Comparator<T> c) {
        Quicksort.quickSort(aData, 0, aData.length - 1, c);
    }

    public static <T> void sort(final T[] aData) {
        Quicksort.quickSortComparable(aData, 0, aData.length - 1);
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

    public static long[] copyOf(final long[] original, final int newLength) {
        final long[] theResult = new long[newLength];
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

    public static void fill(final Object[] a, final int fromIndex, final int toIndex, final Object val) {
        for (int i = fromIndex; i < toIndex; i++)
            a[i] = val;
    }

    public static void fill(final int[] a, final int val) {
        for (int i = 0, len = a.length; i < len; i++)
            a[i] = val;
    }

    public static void fill(final byte[] a, final byte val) {
        for (int i = 0, len = a.length; i < len; i++)
            a[i] = val;
    }

    public static void fill(final short[] a, final short val) {
        for (int i = 0, len = a.length; i < len; i++)
            a[i] = val;
    }

    public static void fill(final long[] a, final long val) {
        for (int i = 0, len = a.length; i < len; i++)
            a[i] = val;
    }

    public static void fill(final float[] a, final float val) {
        for (int i = 0, len = a.length; i < len; i++)
            a[i] = val;
    }

    public static void fill(final double[] a, final double val) {
        for (int i = 0, len = a.length; i < len; i++)
            a[i] = val;
    }

    public static boolean equals(final byte[] a, final byte[] b) {
        if (a == b) {
            return true;
        }
        if (a.length != b.length) {
            return false;
        }
        for (int i=0;i<a.length;i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(final long[] a, final long[] b) {
        if (a == b) {
            return true;
        }
        if (a.length != b.length) {
            return false;
        }
        for (int i=0;i<a.length;i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(final float[] a, final float[] b) {
        if (a == b) {
            return true;
        }
        if (a.length != b.length) {
            return false;
        }
        for (int i=0;i<a.length;i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(final int[] a, final int[] b) {
        if (a == b) {
            return true;
        }
        if (a.length != b.length) {
            return false;
        }
        for (int i=0;i<a.length;i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(final Object[] a, final Object[] b) {
        if (a == b) {
            return true;
        }
        if (a.length != b.length) {
            return false;
        }
        for (int i=0;i<a.length;i++) {
            if (!Objects.equals(a[i], b[i])) {
                return false;
            }
        }
        return true;
    }
}