/*
 * Copyright 2018 Mirko Sertic
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
public class TFloat extends Number {

    public static final float POSITIVE_INFINITY = 1 / 0.0f;
    public static final float NEGATIVE_INFINITY = -POSITIVE_INFINITY;
    public static final float NaN = 0.0f / 0.0f;

    private final float floatValue;

    public TFloat(final float aValue) {
        floatValue = aValue;
    }

    public TFloat(final double aValue) {
        floatValue = (float) aValue;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final TFloat tFloat = (TFloat) o;

        if (Float.compare(tFloat.floatValue, floatValue) != 0)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) floatValue;
    }

    public int compareTo(final Float o) {
        final float f = floatValue;
        final float k = o.floatValue();
        if (f == k) {
            return 0;
        }
        if (f > k) {
            return 1;
        }
        return -1;
    }

    public static int compare(final float f1, final float f2) {
        if(f1 < f2) {
            return -1;
        }
        if(f1 > f2) {
            return 1;
        }
        return 0;
    }

    public static boolean isNaN(final float aValue) {
        return !(aValue == aValue);
    }

    public static boolean isInfinite(final float aFloat) {
        return false;
    }

    @Override
    public float floatValue() {
        return floatValue;
    }

    @Override
    public int intValue() {
        return (int) floatValue;
    }

    @Override
    public long longValue() {
        return (long) floatValue;
    }

    @Override
    public double doubleValue() {
        return floatValue;
    }

    @Override
    public String toString() {
        return toString(floatValue);
    }

    private static float binaryExponent(int n) {
        float result = 1;
        if (n >= 0) {
            float d = 2;
            while (n != 0) {
                if (n % 2 != 0) {
                    result *= d;
                }
                n /= 2;
                d *= d;
            }
        } else {
            n = -n;
            float d = 0.5f;
            while (n != 0) {
                if (n % 2 != 0) {
                    result *= d;
                }
                n /= 2;
                d *= d;
            }
        }
        return result;
    }

    public static int floatToRawIntBits(final float value) {
        return floatToIntBits(value);
    }

    public static int floatToIntBits(final float value) {
        if (value == POSITIVE_INFINITY) {
            return 0x7F800000;
        } else if (value == NEGATIVE_INFINITY) {
            return 0xFF800000;
        } else if (isNaN(value)) {
            return 0x7FC00000;
        }
        final float abs = de.mirkosertic.bytecoder.classlib.java.lang.TMath.abs(value);
        int exp = Math.getExponent(abs);
        int negExp = -exp + 23;
        if (exp < -126) {
            exp = -127;
            negExp = 126 + 23;
        }
        final float doubleMantissa;
        if (negExp <= 126) {
            doubleMantissa = abs * binaryExponent(negExp);
        } else {
            doubleMantissa = abs * 0x1p126f * binaryExponent(negExp - 126);
        }
        final int mantissa = (int) (doubleMantissa + 0.5f) & 0x7FFFFF;
        return mantissa | ((exp + 127) << 23) | (value < 0 || 1 / value == NEGATIVE_INFINITY  ? (1 << 31) : 0);
    }

    public static float intBitsToFloat(final int bits) {
        if ((bits & 0x7F800000) == 0x7F800000) {
            if (bits == 0x7F800000) {
                return POSITIVE_INFINITY;
            } else if (bits == 0xFF800000) {
                return NEGATIVE_INFINITY;
            } else {
                return NaN;
            }
        }
        final boolean negative = (bits & (1 << 31)) != 0;
        final int rawExp = (bits >> 23) & 0xFF;
        int mantissa = bits & 0x7FFFFF;
        if (rawExp == 0) {
            mantissa <<= 1;
        } else {
            mantissa |= 1L << 23;
        }
        final float value = mantissa * binaryExponent(rawExp - 127 - 23);
        return !negative ? value : -value;
    }

    public static float parseFloat(final String aValue) {
        final int p = aValue.indexOf('.');
        if (p<0) {
            return VM.stringToLong(aValue);
        }
        final String thePrefix = aValue.substring(0, p);
        final String theSuffix = aValue.substring(p + 1);
        final long theA = VM.stringToLong(thePrefix);
        final long theB = VM.stringToLong(theSuffix);
        int theMultiplier = 1;
        int theLength = Long.toString(theB).length();
        while(theLength > 0) {
            theMultiplier *= 10;
            theLength--;
        }
        if (theA > 0) {
            return theA + ((float) theB) / theMultiplier;
        }
        return theA - ((float) theB) / theMultiplier;
    }

    public static Float valueOf(final float aValue) {
        return new Float(aValue);
    }

    public static Float valueOf(final String aValue) {
        return parseFloat(aValue);
    }

    public static String toString(final float aValue) {
        final StringBuilder theBuilder = new StringBuilder();
        theBuilder.append(aValue);
        return theBuilder.toString();
    }
}