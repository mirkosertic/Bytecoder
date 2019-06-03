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
package de.mirkosertic.bytecoder.classlib.java.nio.charset;

import java.nio.CharBuffer;
import java.nio.charset.CoderResult;

public class Surrogate {

    private Surrogate() { }

    // TODO: Deprecate/remove the following redundant definitions
    public static final char MIN_HIGH = Character.MIN_HIGH_SURROGATE;
    public static final char MAX_HIGH = Character.MAX_HIGH_SURROGATE;
    public static final char MIN_LOW  = Character.MIN_LOW_SURROGATE;
    public static final char MAX_LOW  = Character.MAX_LOW_SURROGATE;
    public static final char MIN      = Character.MIN_SURROGATE;
    public static final char MAX      = Character.MAX_SURROGATE;
    public static final int UCS4_MIN  = Character.MIN_SUPPLEMENTARY_CODE_POINT;
    public static final int UCS4_MAX  = Character.MAX_CODE_POINT;

    /**
     * Tells whether or not the given value is in the high surrogate range.
     * Use of {@link Character#isHighSurrogate} is generally preferred.
     */
    public static boolean isHigh(final int c) {
        return (MIN_HIGH <= c) && (c <= MAX_HIGH);
    }

    /**
     * Tells whether or not the given value is in the low surrogate range.
     * Use of {@link Character#isLowSurrogate} is generally preferred.
     */
    public static boolean isLow(final int c) {
        return (MIN_LOW <= c) && (c <= MAX_LOW);
    }

    /**
     * Tells whether or not the given value is in the surrogate range.
     * Use of {@link Character#isSurrogate} is generally preferred.
     */
    public static boolean is(final int c) {
        return (MIN <= c) && (c <= MAX);
    }

    /**
     * Tells whether or not the given UCS-4 character must be represented as a
     * surrogate pair in UTF-16.
     * Use of {@link Character#isSupplementaryCodePoint} is generally preferred.
     */
    public static boolean neededFor(final int uc) {
        return Character.isSupplementaryCodePoint(uc);
    }

    /**
     * Returns the high UTF-16 surrogate for the given supplementary UCS-4 character.
     * Use of {@link Character#highSurrogate} is generally preferred.
     */
    public static char high(final int uc) {
        assert Character.isSupplementaryCodePoint(uc);
        return Character.highSurrogate(uc);
    }

    /**
     * Returns the low UTF-16 surrogate for the given supplementary UCS-4 character.
     * Use of {@link Character#lowSurrogate} is generally preferred.
     */
    public static char low(final int uc) {
        assert Character.isSupplementaryCodePoint(uc);
        return Character.lowSurrogate(uc);
    }

    /**
     * Converts the given surrogate pair into a 32-bit UCS-4 character.
     * Use of {@link Character#toCodePoint} is generally preferred.
     */
    public static int toUCS4(final char c, final char d) {
        assert Character.isHighSurrogate(c) && Character.isLowSurrogate(d);
        return Character.toCodePoint(c, d);
    }

    /**
     * Surrogate parsing support.  Charset implementations may use instances of
     * this class to handle the details of parsing UTF-16 surrogate pairs.
     */
    public static class Parser {

        public Parser() { }

        private int character;          // UCS-4
        private CoderResult error = CoderResult.UNDERFLOW;
        private boolean isPair;

        /**
         * Returns the UCS-4 character previously parsed.
         */
        public int character() {
            assert (error == null);
            return character;
        }

        /**
         * Tells whether or not the previously-parsed UCS-4 character was
         * originally represented by a surrogate pair.
         */
        public boolean isPair() {
            assert (error == null);
            return isPair;
        }

        /**
         * Returns the number of UTF-16 characters consumed by the previous
         * parse.
         */
        public int increment() {
            assert (error == null);
            return isPair ? 2 : 1;
        }

        /**
         * If the previous parse operation detected an error, return the object
         * describing that error.
         */
        public CoderResult error() {
            assert (error != null);
            return error;
        }

        /**
         * Returns an unmappable-input result object, with the appropriate
         * input length, for the previously-parsed character.
         */
        public CoderResult unmappableResult() {
            assert (error == null);
            return CoderResult.unmappableForLength(isPair ? 2 : 1);
        }

        /**
         * Parses a UCS-4 character from the given source buffer, handling
         * surrogates.
         *
         * @param  c    The first character
         * @param  in   The source buffer, from which one more character
         *              will be consumed if c is a high surrogate
         *
         * @return  Either a parsed UCS-4 character, in which case the isPair()
         *          and increment() methods will return meaningful values, or
         *          -1, in which case error() will return a descriptive result
         *          object
         */
        public int parse(final char c, final CharBuffer in) {
            if (Character.isHighSurrogate(c)) {
                if (!in.hasRemaining()) {
                    error = CoderResult.UNDERFLOW;
                    return -1;
                }
                final char d = in.get();
                if (Character.isLowSurrogate(d)) {
                    character = Character.toCodePoint(c, d);
                    isPair = true;
                    error = null;
                    return character;
                }
                error = CoderResult.malformedForLength(1);
                return -1;
            }
            if (Character.isLowSurrogate(c)) {
                error = CoderResult.malformedForLength(1);
                return -1;
            }
            character = c;
            isPair = false;
            error = null;
            return character;
        }

        /**
         * Parses a UCS-4 character from the given source buffer, handling
         * surrogates.
         *
         * @param  c    The first character
         * @param  ia   The input array, from which one more character
         *              will be consumed if c is a high surrogate
         * @param  ip   The input index
         * @param  il   The input limit
         *
         * @return  Either a parsed UCS-4 character, in which case the isPair()
         *          and increment() methods will return meaningful values, or
         *          -1, in which case error() will return a descriptive result
         *          object
         */
        public int parse(final char c, final char[] ia, final int ip, final int il) {
            assert (ia[ip] == c);
            if (Character.isHighSurrogate(c)) {
                if (il - ip < 2) {
                    error = CoderResult.UNDERFLOW;
                    return -1;
                }
                final char d = ia[ip + 1];
                if (Character.isLowSurrogate(d)) {
                    character = Character.toCodePoint(c, d);
                    isPair = true;
                    error = null;
                    return character;
                }
                error = CoderResult.malformedForLength(1);
                return -1;
            }
            if (Character.isLowSurrogate(c)) {
                error = CoderResult.malformedForLength(1);
                return -1;
            }
            character = c;
            isPair = false;
            error = null;
            return character;
        }

    }

    /**
     * Surrogate generation support.  Charset implementations may use instances
     * of this class to handle the details of generating UTF-16 surrogate
     * pairs.
     */
    public static class Generator {

        public Generator() { }

        private CoderResult error = CoderResult.OVERFLOW;

        /**
         * If the previous generation operation detected an error, return the
         * object describing that error.
         */
        public CoderResult error() {
            assert error != null;
            return error;
        }

        /**
         * Generates one or two UTF-16 characters to represent the given UCS-4
         * character.
         *
         * @param  uc   The UCS-4 character
         * @param  len  The number of input bytes from which the UCS-4 value
         *              was constructed (used when creating result objects)
         * @param  dst  The destination buffer, to which one or two UTF-16
         *              characters will be written
         *
         * @return  Either a positive count of the number of UTF-16 characters
         *          written to the destination buffer, or -1, in which case
         *          error() will return a descriptive result object
         */
        public int generate(final int uc, final int len, final CharBuffer dst) {
            if (Character.isBmpCodePoint(uc)) {
                final char c = (char) uc;
                if (Character.isSurrogate(c)) {
                    error = CoderResult.malformedForLength(len);
                    return -1;
                }
                if (dst.remaining() < 1) {
                    error = CoderResult.OVERFLOW;
                    return -1;
                }
                dst.put(c);
                error = null;
                return 1;
            } else if (Character.isValidCodePoint(uc)) {
                if (dst.remaining() < 2) {
                    error = CoderResult.OVERFLOW;
                    return -1;
                }
                dst.put(Character.highSurrogate(uc));
                dst.put(Character.lowSurrogate(uc));
                error = null;
                return 2;
            } else {
                error = CoderResult.unmappableForLength(len);
                return -1;
            }
        }

        /**
         * Generates one or two UTF-16 characters to represent the given UCS-4
         * character.
         *
         * @param  uc   The UCS-4 character
         * @param  len  The number of input bytes from which the UCS-4 value
         *              was constructed (used when creating result objects)
         * @param  da   The destination array, to which one or two UTF-16
         *              characters will be written
         * @param  dp   The destination position
         * @param  dl   The destination limit
         *
         * @return  Either a positive count of the number of UTF-16 characters
         *          written to the destination buffer, or -1, in which case
         *          error() will return a descriptive result object
         */
        public int generate(final int uc, final int len, final char[] da, final int dp, final int dl) {
            if (Character.isBmpCodePoint(uc)) {
                final char c = (char) uc;
                if (Character.isSurrogate(c)) {
                    error = CoderResult.malformedForLength(len);
                    return -1;
                }
                if (dl - dp < 1) {
                    error = CoderResult.OVERFLOW;
                    return -1;
                }
                da[dp] = c;
                error = null;
                return 1;
            } else if (Character.isValidCodePoint(uc)) {
                if (dl - dp < 2) {
                    error = CoderResult.OVERFLOW;
                    return -1;
                }
                da[dp] = Character.highSurrogate(uc);
                da[dp + 1] = Character.lowSurrogate(uc);
                error = null;
                return 2;
            } else {
                error = CoderResult.unmappableForLength(len);
                return -1;
            }
        }
    }
}
