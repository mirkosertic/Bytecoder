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
package de.mirkosertic.bytecoder.classlib.shadow.java.lang;

import de.mirkosertic.bytecoder.api.Substitutes;
import de.mirkosertic.bytecoder.api.SubstitutesInClass;
import de.mirkosertic.bytecoder.classlib.java.lang.TMath;

@SubstitutesInClass(Float.class)
public class TFloat {

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

    @Substitutes("floatToRawIntBits")
    public static int floatToRawIntBits(float value) {
        return floatToIntBits(value);
    }

    public static int floatToIntBits(float value) {
        if (value == Float.POSITIVE_INFINITY) {
            return 0x7F800000;
        } else if (value == Float.NEGATIVE_INFINITY) {
            return 0xFF800000;
        } else if (Float.isNaN(value)) {
            return 0x7FC00000;
        }
        float abs = de.mirkosertic.bytecoder.classlib.java.lang.TMath.abs(value);
        int exp = TMath.getExponent(abs);
        int negExp = -exp + 23;
        if (exp < -126) {
            exp = -127;
            negExp = 126 + 23;
        }
        float doubleMantissa;
        if (negExp <= 126) {
            doubleMantissa = abs * binaryExponent(negExp);
        } else {
            doubleMantissa = abs * 0x1p126f * binaryExponent(negExp - 126);
        }
        int mantissa = (int) (doubleMantissa + 0.5f) & 0x7FFFFF;
        return mantissa | ((exp + 127) << 23) | (value < 0 || 1 / value == Float.NEGATIVE_INFINITY  ? (1 << 31) : 0);
    }

    @Substitutes("intBitsToFloat")
    public static float intBitsToFloat(int bits) {
        if ((bits & 0x7F800000) == 0x7F800000) {
            if (bits == 0x7F800000) {
                return Float.POSITIVE_INFINITY;
            } else if (bits == 0xFF800000) {
                return Float.NEGATIVE_INFINITY;
            } else {
                return Float.NaN;
            }
        }
        boolean negative = (bits & (1 << 31)) != 0;
        int rawExp = (bits >> 23) & 0xFF;
        int mantissa = bits & 0x7FFFFF;
        if (rawExp == 0) {
            mantissa <<= 1;
        } else {
            mantissa |= 1L << 23;
        }
        float value = mantissa * binaryExponent(rawExp - 127 - 23);
        return !negative ? value : -value;
    }

}
