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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

public class UTF_8 extends Unicode {

    public static final UTF_8 INSTANCE = new UTF_8();

    public UTF_8() {
        super("UTF-8", StandardCharsets.aliases_UTF_8());
    }

    public String historicalName() {
        return "UTF8";
    }

    public CharsetDecoder newDecoder() {
        return new Decoder(this);
    }

    public CharsetEncoder newEncoder() {
        return new Encoder(this);
    }

    static final void updatePositions(final Buffer src, final int sp,
            final Buffer dst, final int dp) {
        src.position(sp - src.arrayOffset());
        dst.position(dp - dst.arrayOffset());
    }

    private static class Decoder extends CharsetDecoder {

        private Decoder(final Charset cs) {
            super(cs, 1.0f, 1.0f);
        }

        private static boolean isNotContinuation(final int b) {
            return (b & 0xc0) != 0x80;
        }

        //  [E0]     [A0..BF] [80..BF]
        //  [E1..EF] [80..BF] [80..BF]
        private static boolean isMalformed3(final int b1, final int b2, final int b3) {
            return (b1 == (byte)0xe0 && (b2 & 0xe0) == 0x80) ||
                    (b2 & 0xc0) != 0x80 || (b3 & 0xc0) != 0x80;
        }

        // only used when there is only one byte left in src buffer
        private static boolean isMalformed3_2(final int b1, final int b2) {
            return (b1 == (byte)0xe0 && (b2 & 0xe0) == 0x80) ||
                    (b2 & 0xc0) != 0x80;
        }

        //  [F0]     [90..BF] [80..BF] [80..BF]
        //  [F1..F3] [80..BF] [80..BF] [80..BF]
        //  [F4]     [80..8F] [80..BF] [80..BF]
        //  only check 80-be range here, the [0xf0,0x80...] and [0xf4,0x90-...]
        //  will be checked by Character.isSupplementaryCodePoint(uc)
        private static boolean isMalformed4(final int b2, final int b3, final int b4) {
            return (b2 & 0xc0) != 0x80 || (b3 & 0xc0) != 0x80 ||
                    (b4 & 0xc0) != 0x80;
        }

        // only used when there is less than 4 bytes left in src buffer.
        // both b1 and b2 should be "& 0xff" before passed in.
        private static boolean isMalformed4_2(final int b1, final int b2) {
            return (b1 == 0xf0 && (b2  < 0x90 || b2 > 0xbf)) ||
                    (b1 == 0xf4 && (b2 & 0xf0) != 0x80) ||
                    (b2 & 0xc0) != 0x80;
        }

        // tests if b1 and b2 are malformed as the first 2 bytes of a
        // legal`4-byte utf-8 byte sequence.
        // only used when there is less than 4 bytes left in src buffer,
        // after isMalformed4_2 has been invoked.
        private static boolean isMalformed4_3(final int b3) {
            return (b3 & 0xc0) != 0x80;
        }

        private static CoderResult malformedN(final ByteBuffer src, final int nb) {
            switch (nb) {
            case 1:
            case 2:                    // always 1
                return CoderResult.malformedForLength(1);
            case 3:
                int b1 = src.get();
                int b2 = src.get();    // no need to lookup b3
                return CoderResult.malformedForLength(
                        ((b1 == (byte)0xe0 && (b2 & 0xe0) == 0x80) ||
                                isNotContinuation(b2)) ? 1 : 2);
            case 4:  // we don't care the speed here
                b1 = src.get() & 0xff;
                b2 = src.get() & 0xff;
                if (b1 > 0xf4 ||
                        (b1 == 0xf0 && (b2 < 0x90 || b2 > 0xbf)) ||
                        (b1 == 0xf4 && (b2 & 0xf0) != 0x80) ||
                        isNotContinuation(b2))
                    return CoderResult.malformedForLength(1);
                if (isNotContinuation(src.get()))
                    return CoderResult.malformedForLength(2);
                return CoderResult.malformedForLength(3);
            default:
                assert false;
                return null;
            }
        }

        private static CoderResult malformed(final ByteBuffer src, final int sp,
                final CharBuffer dst, final int dp,
                final int nb)
        {
            src.position(sp - src.arrayOffset());
            final CoderResult cr = malformedN(src, nb);
            updatePositions(src, sp, dst, dp);
            return cr;
        }


        private static CoderResult malformed(final ByteBuffer src,
                final int mark, final int nb)
        {
            src.position(mark);
            final CoderResult cr = malformedN(src, nb);
            src.position(mark);
            return cr;
        }

        private static CoderResult malformedForLength(final ByteBuffer src,
                final int sp,
                final CharBuffer dst,
                final int dp,
                final int malformedNB)
        {
            updatePositions(src, sp, dst, dp);
            return CoderResult.malformedForLength(malformedNB);
        }

        private static CoderResult malformedForLength(final ByteBuffer src,
                final int mark,
                final int malformedNB)
        {
            src.position(mark);
            return CoderResult.malformedForLength(malformedNB);
        }


        private static CoderResult xflow(final Buffer src, final int sp, final int sl,
                final Buffer dst, final int dp, final int nb) {
            updatePositions(src, sp, dst, dp);
            return (nb == 0 || sl - sp < nb)
                    ? CoderResult.UNDERFLOW : CoderResult.OVERFLOW;
        }

        private static CoderResult xflow(final Buffer src, final int mark, final int nb) {
            src.position(mark);
            return (nb == 0 || src.remaining() < nb)
                    ? CoderResult.UNDERFLOW : CoderResult.OVERFLOW;
        }

        private CoderResult decodeArrayLoop(final ByteBuffer src,
                final CharBuffer dst)
        {
            // This method is optimized for ASCII input.
            final byte[] sa = src.array();
            int sp = src.arrayOffset() + src.position();
            final int sl = src.arrayOffset() + src.limit();

            final char[] da = dst.array();
            int dp = dst.arrayOffset() + dst.position();
            final int dl = dst.arrayOffset() + dst.limit();
            final int dlASCII = dp + Math.min(sl - sp, dl - dp);

            // ASCII only loop
            while (dp < dlASCII && sa[sp] >= 0)
                da[dp++] = (char) sa[sp++];
            while (sp < sl) {
                int b1 = sa[sp];
                if (b1 >= 0) {
                    // 1 byte, 7 bits: 0xxxxxxx
                    if (dp >= dl)
                        return xflow(src, sp, sl, dst, dp, 1);
                    da[dp++] = (char) b1;
                    sp++;
                } else if ((b1 >> 5) == -2 && (b1 & 0x1e) != 0) {
                    // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
                    //                   [C2..DF] [80..BF]
                    if (sl - sp < 2 || dp >= dl)
                        return xflow(src, sp, sl, dst, dp, 2);
                    final int b2 = sa[sp + 1];
                    // Now we check the first byte of 2-byte sequence as
                    //     if ((b1 >> 5) == -2 && (b1 & 0x1e) != 0)
                    // no longer need to check b1 against c1 & c0 for
                    // malformed as we did in previous version
                    //   (b1 & 0x1e) == 0x0 || (b2 & 0xc0) != 0x80;
                    // only need to check the second byte b2.
                    if (isNotContinuation(b2))
                        return malformedForLength(src, sp, dst, dp, 1);
                    da[dp++] = (char) (((b1 << 6) ^ b2)
                            ^
                            (((byte) 0xC0 << 6) ^
                                    ((byte) 0x80 << 0)));
                    sp += 2;
                } else if ((b1 >> 4) == -2) {
                    // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
                    final int srcRemaining = sl - sp;
                    if (srcRemaining < 3 || dp >= dl) {
                        if (srcRemaining > 1 && isMalformed3_2(b1, sa[sp + 1]))
                            return malformedForLength(src, sp, dst, dp, 1);
                        return xflow(src, sp, sl, dst, dp, 3);
                    }
                    final int b2 = sa[sp + 1];
                    final int b3 = sa[sp + 2];
                    if (isMalformed3(b1, b2, b3))
                        return malformed(src, sp, dst, dp, 3);
                    final char c = (char)
                            ((b1 << 12) ^
                                    (b2 <<  6) ^
                                    (b3 ^
                                            (((byte) 0xE0 << 12) ^
                                                    ((byte) 0x80 <<  6) ^
                                                    ((byte) 0x80 <<  0))));
                    if (Character.isSurrogate(c))
                        return malformedForLength(src, sp, dst, dp, 3);
                    da[dp++] = c;
                    sp += 3;
                } else if ((b1 >> 3) == -2) {
                    // 4 bytes, 21 bits: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
                    final int srcRemaining = sl - sp;
                    if (srcRemaining < 4 || dl - dp < 2) {
                        b1 &= 0xff;
                        if (b1 > 0xf4 ||
                                srcRemaining > 1 && isMalformed4_2(b1, sa[sp + 1] & 0xff))
                            return malformedForLength(src, sp, dst, dp, 1);
                        if (srcRemaining > 2 && isMalformed4_3(sa[sp + 2]))
                            return malformedForLength(src, sp, dst, dp, 2);
                        return xflow(src, sp, sl, dst, dp, 4);
                    }
                    final int b2 = sa[sp + 1];
                    final int b3 = sa[sp + 2];
                    final int b4 = sa[sp + 3];
                    final int uc = ((b1 << 18) ^
                            (b2 << 12) ^
                            (b3 <<  6) ^
                            (b4 ^
                                    (((byte) 0xF0 << 18) ^
                                            ((byte) 0x80 << 12) ^
                                            ((byte) 0x80 <<  6) ^
                                            ((byte) 0x80 <<  0))));
                    if (isMalformed4(b2, b3, b4) ||
                            // shortest form check
                            !Character.isSupplementaryCodePoint(uc)) {
                        return malformed(src, sp, dst, dp, 4);
                    }
                    da[dp++] = Character.highSurrogate(uc);
                    da[dp++] = Character.lowSurrogate(uc);
                    sp += 4;
                } else
                    return malformed(src, sp, dst, dp, 1);
            }
            return xflow(src, sp, sl, dst, dp, 0);
        }

        private CoderResult decodeBufferLoop(final ByteBuffer src,
                final CharBuffer dst)
        {
            int mark = src.position();
            final int limit = src.limit();
            while (mark < limit) {
                int b1 = src.get();
                if (b1 >= 0) {
                    // 1 byte, 7 bits: 0xxxxxxx
                    if (dst.remaining() < 1)
                        return xflow(src, mark, 1); // overflow
                    dst.put((char) b1);
                    mark++;
                } else if ((b1 >> 5) == -2 && (b1 & 0x1e) != 0) {
                    // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
                    if (limit - mark < 2|| dst.remaining() < 1)
                        return xflow(src, mark, 2);
                    final int b2 = src.get();
                    if (isNotContinuation(b2))
                        return malformedForLength(src, mark, 1);
                    dst.put((char) (((b1 << 6) ^ b2)
                            ^
                            (((byte) 0xC0 << 6) ^
                                    ((byte) 0x80 << 0))));
                    mark += 2;
                } else if ((b1 >> 4) == -2) {
                    // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
                    final int srcRemaining = limit - mark;
                    if (srcRemaining < 3 || dst.remaining() < 1) {
                        if (srcRemaining > 1 && isMalformed3_2(b1, src.get()))
                            return malformedForLength(src, mark, 1);
                        return xflow(src, mark, 3);
                    }
                    final int b2 = src.get();
                    final int b3 = src.get();
                    if (isMalformed3(b1, b2, b3))
                        return malformed(src, mark, 3);
                    final char c = (char)
                            ((b1 << 12) ^
                                    (b2 <<  6) ^
                                    (b3 ^
                                            (((byte) 0xE0 << 12) ^
                                                    ((byte) 0x80 <<  6) ^
                                                    ((byte) 0x80 <<  0))));
                    if (Character.isSurrogate(c))
                        return malformedForLength(src, mark, 3);
                    dst.put(c);
                    mark += 3;
                } else if ((b1 >> 3) == -2) {
                    // 4 bytes, 21 bits: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
                    final int srcRemaining = limit - mark;
                    if (srcRemaining < 4 || dst.remaining() < 2) {
                        b1 &= 0xff;
                        if (b1 > 0xf4 ||
                                srcRemaining > 1 && isMalformed4_2(b1, src.get() & 0xff))
                            return malformedForLength(src, mark, 1);
                        if (srcRemaining > 2 && isMalformed4_3(src.get()))
                            return malformedForLength(src, mark, 2);
                        return xflow(src, mark, 4);
                    }
                    final int b2 = src.get();
                    final int b3 = src.get();
                    final int b4 = src.get();
                    final int uc = ((b1 << 18) ^
                            (b2 << 12) ^
                            (b3 <<  6) ^
                            (b4 ^
                                    (((byte) 0xF0 << 18) ^
                                            ((byte) 0x80 << 12) ^
                                            ((byte) 0x80 <<  6) ^
                                            ((byte) 0x80 <<  0))));
                    if (isMalformed4(b2, b3, b4) ||
                            // shortest form check
                            !Character.isSupplementaryCodePoint(uc)) {
                        return malformed(src, mark, 4);
                    }
                    dst.put(Character.highSurrogate(uc));
                    dst.put(Character.lowSurrogate(uc));
                    mark += 4;
                } else {
                    return malformed(src, mark, 1);
                }
            }
            return xflow(src, mark, 0);
        }

        protected CoderResult decodeLoop(final ByteBuffer src,
                final CharBuffer dst)
        {
            if (src.hasArray() && dst.hasArray())
                return decodeArrayLoop(src, dst);
            else
                return decodeBufferLoop(src, dst);
        }

        private static ByteBuffer getByteBuffer(ByteBuffer bb, final byte[] ba, final int sp)
        {
            if (bb == null)
                bb = ByteBuffer.wrap(ba);
            bb.position(sp);
            return bb;
        }
    }

    private static final class Encoder extends CharsetEncoder {

        private Encoder(final Charset cs) {
            super(cs, 1.1f, 3.0f);
        }

        public boolean canEncode(final char c) {
            return !Character.isSurrogate(c);
        }

        public boolean isLegalReplacement(final byte[] repl) {
            return ((repl.length == 1 && repl[0] >= 0) ||
                    super.isLegalReplacement(repl));
        }

        private static CoderResult overflow(final CharBuffer src, final int sp,
                final ByteBuffer dst, final int dp) {
            updatePositions(src, sp, dst, dp);
            return CoderResult.OVERFLOW;
        }

        private static CoderResult overflow(final CharBuffer src, final int mark) {
            src.position(mark);
            return CoderResult.OVERFLOW;
        }

        private Surrogate.Parser sgp;
        private CoderResult encodeArrayLoopUTF8(final CharBuffer src,
                final ByteBuffer dst)
        {
            final char[] sa = src.array();
            int sp = src.arrayOffset() + src.position();
            final int sl = src.arrayOffset() + src.limit();

            final byte[] da = dst.array();
            int dp = dst.arrayOffset() + dst.position();
            final int dl = dst.arrayOffset() + dst.limit();
            final int dlASCII = dp + Math.min(sl - sp, dl - dp);

            // ASCII only loop
            while (dp < dlASCII && sa[sp] < '\u0080')
                da[dp++] = (byte) sa[sp++];
            while (sp < sl) {
                final char c = sa[sp];
                if (c < 0x80) {
                    // Have at most seven bits
                    if (dp >= dl)
                        return overflow(src, sp, dst, dp);
                    da[dp++] = (byte)c;
                } else if (c < 0x800) {
                    // 2 bytes, 11 bits
                    if (dl - dp < 2)
                        return overflow(src, sp, dst, dp);
                    da[dp++] = (byte)(0xc0 | (c >> 6));
                    da[dp++] = (byte)(0x80 | (c & 0x3f));
                } else if (Character.isSurrogate(c)) {
                    // Have a surrogate pair
                    if (sgp == null)
                        sgp = new Surrogate.Parser();
                    final int uc = sgp.parse(c, sa, sp, sl);
                    if (uc < 0) {
                        updatePositions(src, sp, dst, dp);
                        return sgp.error();
                    }
                    if (dl - dp < 4)
                        return overflow(src, sp, dst, dp);
                    da[dp++] = (byte)(0xf0 | ((uc >> 18)));
                    da[dp++] = (byte)(0x80 | ((uc >> 12) & 0x3f));
                    da[dp++] = (byte)(0x80 | ((uc >>  6) & 0x3f));
                    da[dp++] = (byte)(0x80 | (uc & 0x3f));
                    sp++;  // 2 chars
                } else {
                    // 3 bytes, 16 bits
                    if (dl - dp < 3)
                        return overflow(src, sp, dst, dp);
                    da[dp++] = (byte)(0xe0 | ((c >> 12)));
                    da[dp++] = (byte)(0x80 | ((c >>  6) & 0x3f));
                    da[dp++] = (byte)(0x80 | (c & 0x3f));
                }
                sp++;
            }
            updatePositions(src, sp, dst, dp);
            return CoderResult.UNDERFLOW;
        }

        private CoderResult encodeBufferLoop(final CharBuffer src,
                final ByteBuffer dst)
        {
            int mark = src.position();
            while (src.hasRemaining()) {
                final char c = src.get();
                if (c < 0x80) {
                    // Have at most seven bits
                    if (!dst.hasRemaining())
                        return overflow(src, mark);
                    dst.put((byte)c);
                } else if (c < 0x800) {
                    // 2 bytes, 11 bits
                    if (dst.remaining() < 2)
                        return overflow(src, mark);
                    dst.put((byte)(0xc0 | (c >> 6)));
                    dst.put((byte)(0x80 | (c & 0x3f)));
                } else if (Character.isSurrogate(c)) {
                    // Have a surrogate pair
                    if (sgp == null)
                        sgp = new Surrogate.Parser();
                    final int uc = sgp.parse(c, src);
                    if (uc < 0) {
                        src.position(mark);
                        return sgp.error();
                    }
                    if (dst.remaining() < 4)
                        return overflow(src, mark);
                    dst.put((byte)(0xf0 | ((uc >> 18))));
                    dst.put((byte)(0x80 | ((uc >> 12) & 0x3f)));
                    dst.put((byte)(0x80 | ((uc >>  6) & 0x3f)));
                    dst.put((byte)(0x80 | (uc & 0x3f)));
                    mark++;  // 2 chars
                } else {
                    // 3 bytes, 16 bits
                    if (dst.remaining() < 3)
                        return overflow(src, mark);
                    dst.put((byte)(0xe0 | ((c >> 12))));
                    dst.put((byte)(0x80 | ((c >>  6) & 0x3f)));
                    dst.put((byte)(0x80 | (c & 0x3f)));
                }
                mark++;
            }
            src.position(mark);
            return CoderResult.UNDERFLOW;
        }

        protected final CoderResult encodeLoop(final CharBuffer src,
                final ByteBuffer dst)
        {
            if (src.hasArray() && dst.hasArray())
                return encodeArrayLoopUTF8(src, dst);
            else
                return encodeBufferLoop(src, dst);
        }
    }
}
