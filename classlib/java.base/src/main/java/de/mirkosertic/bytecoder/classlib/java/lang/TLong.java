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

import de.mirkosertic.bytecoder.api.SubstitutesInClass;
import de.mirkosertic.bytecoder.classlib.VM;

@SubstitutesInClass(completeReplace = true)
public class TLong extends Number {

    private final long longValue;

    public TLong(final long aLongValue) {
        longValue = aLongValue;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final Long tLong = (Long) o;

        if (longValue != tLong.longValue())
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) longValue;
    }

    @Override
    public int intValue() {
        return (int) longValue;
    }

    @Override
    public float floatValue() {
        return (float) longValue;
    }

    @Override
    public long longValue() {
        return longValue;
    }

    @Override
    public double doubleValue() {
        return longValue;
    }

    @Override
    public String toString() {
        return toString(longValue);
    }

    public static Long valueOf(final long aValue) {
        return new Long(aValue);
    }

    public static Long valueOf(final String aValue) {
        return new Long(VM.stringToLong(aValue));
    }

    public static long parseLong(final String aString) {
        return VM.stringToLong(aString);
    }

    public static String toString(final long aValue) {
        final StringBuilder theBuffer = new StringBuilder();
        theBuffer.append(aValue);
        return theBuffer.toString();
    }

    public static String toString(final long aValue, final int aBase) {
        final StringBuilder theBuffer = new StringBuilder();
        theBuffer.append(aValue);
        return theBuffer.toString();
    }

    public static String toHexString(final long aValue) {
        return VM.longToHex(aValue);
    }

    public static int numberOfLeadingZeros(final long i) {
        final int x = (int)(i >>> 32);
        return x == 0 ? 32 + Integer.numberOfLeadingZeros((int)i) : 32 + Integer.numberOfLeadingZeros(x);
    }

    public static int numberOfTrailingZeros(final long i) {
        final int x = (int)i;
        return x == 0 ? 32 + Integer.numberOfTrailingZeros((int)(i >>> 32)) : 32 + Integer.numberOfTrailingZeros(x);
    }

    public static int bitCount(long i) {
        // HD, Figure 5-2
        i = i - ((i >>> 1) & 0x5555555555555555L);
        i = (i & 0x3333333333333333L) + ((i >>> 2) & 0x3333333333333333L);
        i = (i + (i >>> 4)) & 0x0f0f0f0f0f0f0f0fL;
        i = i + (i >>> 8);
        i = i + (i >>> 16);
        i = i + (i >>> 32);
        return (int)i & 0x7f;
    }

    public static int compare(final long a, final long b) {
        if (a > b) {
            return 1;
        } else if (a < b) {
            return -1;
        }
        return 0;
    }
}