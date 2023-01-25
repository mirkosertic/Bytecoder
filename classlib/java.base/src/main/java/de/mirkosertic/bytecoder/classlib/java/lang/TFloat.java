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

    public static final Class<Float> TYPE = (Class<Float>) VM.floatPrimitiveClass();

    public static final float NaN = 0.0f / 0.0f;
    public static final float POSITIVE_INFINITY = 1 / 0.0f;
    public static final float NEGATIVE_INFINITY = -POSITIVE_INFINITY;

    private final float value;

    public TFloat(final float value) {
        this.value = value;
    }

    public TFloat(final double value) {
        this((float) value);
    }

    @Override
    public int intValue() {
        return (int) value;
    }

    @Override
    public long longValue() {
        return (long) value;
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }

    @Override
    public String toString() {
        return toString(((Float) (Object) this).floatValue());
    }

    public static int compare(final float d1, final float d2) {
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

        final Float obj = (Float) o;

        return value == obj.floatValue();
    }

    public static native float parseFloat(final String aValue);

    public static Float valueOf(final String aValue) {
        return parseFloat(aValue);
    }

    public static Float valueOf(final float aValue) {
        return new Float(aValue);
    }

    public native static String toString(final float aValue);

    public static native boolean isNaN(final float aValue);

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
        final float abs = Math.abs(value);
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

    public static int signum(final float value) {
        if (value < 0) {
            return -1;
        }
        if (value > 0) {
            return 1;
        }
        return 0;
    }

}
