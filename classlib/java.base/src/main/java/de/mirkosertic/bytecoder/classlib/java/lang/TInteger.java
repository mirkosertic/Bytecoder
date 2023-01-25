/*
 * Copyright 2023 Mirko Sertic
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

import de.mirkosertic.bytecoder.api.SubstitutesInClass;
import de.mirkosertic.bytecoder.classlib.VM;

import java.lang.annotation.Native;

@SubstitutesInClass(completeReplace = true)
public class TInteger extends Number {

    public static final Class<Integer> TYPE = (Class<Integer>) VM.intPrimitiveClass();

    @Native
    private final int value;

    public TInteger(final int value) {
        this.value = value;
    }

    @Override
    public int intValue() {
        return value;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }

    public static int compare(final int d1, final int d2) {
        if (d1 < d2) {
            return -1;
        }
        if (d1 > d2) {
            return 1;
        }
        return 0;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        final Integer obj = (Integer) o;

        return value == obj.intValue();
    }

    @Override
    public native String toString();

    public static native int numberOfLeadingZeros(final int i);

    public static native int numberOfTrailingZeros(final int i);

    public static native int bitCount(final int i);

    public static Integer valueOf(final int i) {
        return new Integer(i);
    }

    public static Integer valueOf(final String str) {
        return new Integer(parseInt(str));
    }

    public static String toString(final int i) {
        return toString(i, 10);
    }
    public static native String toString(int i, int radix);

    public static native String toHexString(int i);

    public static int parseInt(final String s) {
        return parseInt(s, 10);
    }

    public static native int parseInt(String s, int radix);

    public static int signum(final int value) {
        if (value < 0) {
            return -1;
        }
        if (value > 0) {
            return 1;
        }
        return 0;
    }
}
