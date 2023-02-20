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
package de.mirkosertic.bytecoder.classlib;

public class Array {

    @Override
    public Object clone() {
        return this;
    }

    public static Object newObjectArray2Dim(final int dim1, final int dim2) {
        final Object[] a = new Object[dim1];
        for (int i = 0; i < dim1; i++) {
            a[i] = new Object[dim2];
        }
        return a;
    }

    public static Object newBooleanArray2Dim(final int dim1, final int dim2) {
        final Object[] a = new Object[dim1];
        for (int i = 0; i < dim1; i++) {
            a[i] = new boolean[dim2];
        }
        return a;
    }

    public static Object newShortArray2Dim(final int dim1, final int dim2) {
        final Object[] a = new Object[dim1];
        for (int i = 0; i < dim1; i++) {
            a[i] = new short[dim2];
        }
        return a;
    }

    public static Object newByteArray2Dim(final int dim1, final int dim2) {
        final Object[] a = new Object[dim1];
        for (int i = 0; i < dim1; i++) {
            a[i] = new byte[dim2];
        }
        return a;
    }

    public static Object newCharArray2Dim(final int dim1, final int dim2) {
        final Object[] a = new Object[dim1];
        for (int i = 0; i < dim1; i++) {
            a[i] = new char[dim2];
        }
        return a;
    }

    public static Object newIntArray2Dim(final int dim1, final int dim2) {
        final Object[] a = new Object[dim1];
        for (int i = 0; i < dim1; i++) {
            a[i] = new int[dim2];
        }
        return a;
    }

    public static Object newLongArray2Dim(final int dim1, final int dim2) {
        final Object[] a = new Object[dim1];
        for (int i = 0; i < dim1; i++) {
            a[i] = new long[dim2];
        }
        return a;
    }

    public static Object newFloatArray2Dim(final int dim1, final int dim2) {
        final Object[] a = new Object[dim1];
        for (int i = 0; i < dim1; i++) {
            a[i] = new float[dim2];
        }
        return a;
    }

    public static Object newDoubleArray2Dim(final int dim1, final int dim2) {
        final Object[] a = new Object[dim1];
        for (int i = 0; i < dim1; i++) {
            a[i] = new double[dim2];
        }
        return a;
    }
}
