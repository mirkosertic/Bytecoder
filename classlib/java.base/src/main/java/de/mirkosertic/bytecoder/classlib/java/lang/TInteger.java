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

import de.mirkosertic.bytecoder.api.NoExceptionCheck;
import de.mirkosertic.bytecoder.api.SubstitutesInClass;
import de.mirkosertic.bytecoder.classlib.VM;

@SubstitutesInClass(completeReplace = true)
public class TInteger extends Number {

    public static final Class<Integer> TYPE = Integer.class;

    private final int integerValue;

    @NoExceptionCheck
    public TInteger(final int aIntegerValue) {
        integerValue = aIntegerValue;
    }

    @Override
    public int intValue() {
        return integerValue;
    }

    @Override
    public float floatValue() {
        return integerValue;
    }

    @Override
    public long longValue() {
        return integerValue;
    }

    @Override
    public double doubleValue() {
        return integerValue;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TInteger)) {
            return false;
        }

        final TInteger theOtherInteger = (TInteger) o;

        return integerValue == theOtherInteger.integerValue;
    }

    @Override
    public int hashCode() {
        return integerValue;
    }

    @Override
    public String toString() {
        return toString(integerValue);
    }

    public static Integer valueOf(final int aValue) {
        return new Integer(aValue);
    }

    public static Integer valueOf(final String aValue) {
        return new Integer((int) VM.stringToLong(aValue));
    }

    public static int parseInt(final String aString) {
        return (int) VM.stringToLong(aString);
    }

    public static String toString(final int aValue) {
        final StringBuilder theBuffer = new StringBuilder();
        theBuffer.append(aValue);
        return theBuffer.toString();
    }

    public static String toHexString(final int aValue) {
        return VM.longToHex(aValue);
    }

    public static int numberOfLeadingZeros(final int aValue) {
        return 0;
    }

    public static int parseInt(final String s, final int radix)
            throws NumberFormatException
    {
        /*
         * WARNING: This method may be invoked early during VM initialization
         * before IntegerCache is initialized. Care must be taken to not use
         * the valueOf method.
         */

        if (s == null) {
            throw new NumberFormatException("null");
        }

        if (radix < Character.MIN_RADIX) {
            throw new NumberFormatException("radix " + radix +
                    " less than Character.MIN_RADIX");
        }

        if (radix > Character.MAX_RADIX) {
            throw new NumberFormatException("radix " + radix +
                    " greater than Character.MAX_RADIX");
        }

        boolean negative = false;
        int i = 0;
        final int len = s.length();
        int limit = -Integer.MAX_VALUE;

        if (len > 0) {
            final char firstChar = s.charAt(0);
            if (firstChar < '0') { // Possible leading "+" or "-"
                if (firstChar == '-') {
                    negative = true;
                    limit = Integer.MIN_VALUE;
                } else if (firstChar != '+') {
                    throw new NumberFormatException(s);
                }

                if (len == 1) { // Cannot have lone "+" or "-"
                    throw new NumberFormatException(s);
                }
                i++;
            }
            final int multmin = limit / radix;
            int result = 0;
            while (i < len) {
                // Accumulating negatively avoids surprises near MAX_VALUE
                final int digit = Character.digit(s.charAt(i++), radix);
                if (digit < 0 || result < multmin) {
                    throw new NumberFormatException(s);
                }
                result *= radix;
                if (result < limit + digit) {
                    throw new NumberFormatException(s);
                }
                result -= digit;
            }
            return negative ? result : -result;
        } else {
            throw new NumberFormatException(s);
        }
    }

}