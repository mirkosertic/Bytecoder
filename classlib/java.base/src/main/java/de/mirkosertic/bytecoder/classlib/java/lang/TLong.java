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

    public static final Class<Long> TYPE = (Class<Long>) TClass.getPrimitiveClass("long");

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

    public static long parseLong(final String aString, final int radix) {
        return VM.stringToLong(aString, radix);
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

    static int stringSize(long x) {
        int d = 1;
        if (x >= 0) {
            d = 0;
            x = -x;
        }
        long p = -10;
        for (int i = 1; i < 19; i++) {
            if (x > p)
                return i + d;
            p = 10 * p;
        }
        return 19 + d;
    }

    static int getChars(long i, final int index, final byte[] buf) {
        long q;
        int r;
        int charPos = index;

        final boolean negative = (i < 0);
        if (!negative) {
            i = -i;
        }

        // Get 2 digits/iteration using longs until quotient fits into an int
        while (i <= Integer.MIN_VALUE) {
            q = i / 100;
            r = (int)((q * 100) - i);
            i = q;
            buf[--charPos] = VM.DigitOnes[r];
            buf[--charPos] = VM.DigitTens[r];
        }

        // Get 2 digits/iteration using ints
        int q2;
        int i2 = (int)i;
        while (i2 <= -100) {
            q2 = i2 / 100;
            r  = (q2 * 100) - i2;
            i2 = q2;
            buf[--charPos] = VM.DigitOnes[r];
            buf[--charPos] = VM.DigitTens[r];
        }

        // We know there are at most two digits left at this point.
        q2 = i2 / 10;
        r  = (q2 * 10) - i2;
        buf[--charPos] = (byte)('0' + r);

        // Whatever left is the remaining digit.
        if (q2 < 0) {
            buf[--charPos] = (byte)('0' - q2);
        }

        if (negative) {
            buf[--charPos] = (byte)'-';
        }
        return charPos;
    }
}