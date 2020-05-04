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

import de.mirkosertic.bytecoder.api.EmulatedByRuntime;
import de.mirkosertic.bytecoder.api.SubstitutesInClass;

@SubstitutesInClass(completeReplace = true)
public class TMath {

    public static final double E = 2.7182818284590452354;
    public static final double PI = 3.14159265358979323846;

    private static class FloatExponents {
        public static final float[] exponents = { 0x1p1f, 0x1p2f, 0x1p4f, 0x1p8f, 0x1p16f, 0x1p32f, 0x1p64f };
        public static final float[] negativeExponents = { 0x1p-1f, 0x1p-2f, 0x1p-4f, 0x1p-8f, 0x1p-16f, 0x1p-32f,
                0x1p-64f };
        public static final float[] negativeExponents2 = { 0x1p-0f, 0x1p-1f, 0x1p-3f, 0x1p-7f, 0x1p-15f, 0x1p-31f,
                0x1p-63f };
    }

    public static float abs(final float a) {
        if (a<0) {
            return -a;
        }
        return a;
    }

    public static double abs(final double a) {
        if (a<0) {
            return -a;
        }
        return a;
    }

    public static int abs(final int a) {
        if (a<0) {
            return -a;
        }
        return a;
    }

    @EmulatedByRuntime
    public static native double sqrt(double aValue);

    public static native double cbrt(double aValue);

    @EmulatedByRuntime
    public static native double ceil(double aValue);

    @EmulatedByRuntime
    public static native double floor(double aValue);

    public static native double sin(double aValue);

    public static native double cos(double aValue);

    public static native double random();

    public static native double toRadians(double aValue);

    public static native double toDegrees(double aValue);

    public static native double tan(double aValue);

    @EmulatedByRuntime
    public static native long max(long aValue1, long aValue2);

    @EmulatedByRuntime
    public static native int max(int aValue1, int aValue2);

    @EmulatedByRuntime
    public static native float max(float aValue1, float aValue2);

    @EmulatedByRuntime
    public static native double max(double aValue1, double aValue2);

    @EmulatedByRuntime
    public static native int min(int aValue1, int aValue2);

    @EmulatedByRuntime
    public static native long min(long aValue1, long aValue2);

    @EmulatedByRuntime
    public static native float min(float aValue1, float aValue2);

    @EmulatedByRuntime
    public static native double min(double aValue1, double aValue2);

    public static int getExponent(float f) {
        f = abs(f);
        int exp = 0;
        final float[] exponents = FloatExponents.exponents;
        final float[] negativeExponents = FloatExponents.negativeExponents;
        final float[] negativeExponents2 = FloatExponents.negativeExponents2;
        if (f > 1) {
            int expBit = 1 << (exponents.length - 1);
            for (int i = exponents.length - 1; i >= 0; --i) {
                if (f >= exponents[i]) {
                    f *= negativeExponents[i];
                    exp |= expBit;
                }
                expBit >>>= 1;
            }
        } else if (f < 1) {
            int expBit = 1 << (negativeExponents.length - 1);
            int offset = 0;
            if (f < 0x1p-126) {
                f *= 0x1p23f;
                offset = 23;
            }
            for (int i = negativeExponents2.length - 1; i >= 0; --i) {
                if (f < negativeExponents2[i]) {
                    f *= exponents[i];
                    exp |= expBit;
                }
                expBit >>>= 1;
            }
            exp = -(exp + offset);
        }
        return exp;
    }

    public static native double log(double aValue1);

    public static int floorMod(final int x, final int y) {
        return x - floorDiv(x, y) * y;
    }

    public static int floorDiv(final int x, final int y) {
        int r = x / y;
        // if the signs are different and modulo not zero, round down
        if ((x ^ y) < 0 && (r * y != x)) {
            r--;
        }
        return r;
    }

    public static long floorDiv(final long x, final long y) {
        long r = x / y;
        // if the signs are different and modulo not zero, round down
        if ((x ^ y) < 0 && (r * y != x)) {
            r--;
        }
        return r;
    }

    public static long floorDiv(final long x, final int y) {
        return floorDiv(x, (long)y);
    }

    public static int floorMod(final long x, final int y) {
        // Result cannot overflow the range of int.
        return (int)(x - floorDiv(x, y) * y);
    }

    public static long floorMod(final long x, final long y) {
        return x - floorDiv(x, y) * y;
    }

    public static int addExact(final int x, final int y) {
        final int r = x + y;
        // HD 2-12 Overflow iff both arguments have the opposite sign of the result
        if (((x ^ r) & (y ^ r)) < 0) {
            throw new ArithmeticException("integer overflow");
        }
        return r;
    }

    public static long addExact(final long x, final long y) {
        final long r = x + y;
        // HD 2-12 Overflow iff both arguments have the opposite sign of the result
        if (((x ^ r) & (y ^ r)) < 0) {
            throw new ArithmeticException("long overflow");
        }
        return r;
    }

    public static int multiplyExact(final int a, final int b) {
        return a * b;
    }

    public static native double pow(double a, double b);

    public static int round(final float value) {
        return (int) value;
    }

    public static long round(final double value) {
        return (int) value;
    }

    public static double rint(final double value) {
        return (int) value;
    }

    public static double hypot(final double a, final double b) {
        return sqrt(a*a + b*b);
    }

    public static native double acos(final double value);

    public static long abs(final long value) {
        if (value < 0) {
            return -value;
        }
        return value;
    }

    public static double IEEEremainder(final double a, final double b) {
        return 0;
    }

    public static native double atan2(final double a, final double b);

    public static double ulp(final double d) {
        int exp = getExponent((float) d);

        switch(exp) {
            case Double.MAX_EXPONENT + 1:       // NaN or infinity
                return Math.abs(d);

            case Double.MIN_EXPONENT - 1:       // zero or subnormal
                return Double.MIN_VALUE;

            default:
                assert exp <= Double.MAX_EXPONENT && exp >= Double.MIN_EXPONENT;

                // ulp(x) is usually 2^(SIGNIFICAND_WIDTH-1)*(2^ilogb(x))
                exp = exp - (53-1);
                if (exp >= Double.MIN_EXPONENT) {
                    return powerOfTwoD(exp);
                }
                else {
                    // return a subnormal result; left shift integer
                    // representation of Double.MIN_VALUE appropriate
                    // number of positions
                    return Double.longBitsToDouble(1L <<
                            (exp - (Double.MIN_EXPONENT - (53-1)) ));
                }
        }
    }

    public static float ulp(final float f) {
        int exp = getExponent(f);

        switch(exp) {
            case Float.MAX_EXPONENT+1:        // NaN or infinity
                return Math.abs(f);

            case Float.MIN_EXPONENT-1:        // zero or subnormal
                return Float.MIN_VALUE;

            default:
                assert exp <= Float.MAX_EXPONENT && exp >= Float.MIN_EXPONENT;

                // ulp(x) is usually 2^(SIGNIFICAND_WIDTH-1)*(2^ilogb(x))
                exp = exp - (24-1);
                if (exp >= Float.MIN_EXPONENT) {
                    return powerOfTwoF(exp);
                } else {
                    // return a subnormal result; left shift integer
                    // representation of FloatConsts.MIN_VALUE appropriate
                    // number of positions
                    return Float.intBitsToFloat(1 <<
                            (exp - (Float.MIN_EXPONENT - (24-1)) ));
                }
        }
    }

    static double powerOfTwoD(final int n) {
        assert(n >= Double.MIN_EXPONENT && n <= Double.MAX_EXPONENT);
        return Double.longBitsToDouble((((long)n + (long)1023) <<
                (53-1))
                & 9218868437227405312L);
    }

    static float powerOfTwoF(final int n) {
        assert(n >= Float.MIN_EXPONENT && n <= Float.MAX_EXPONENT);
        return Float.intBitsToFloat(((n + 127) <<
                (42-1))
                & 2139095040);
    }
}