/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package de.mirkosertic.bytecoder.classlib.java.nio.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Objects;

public class ISO_8859_1 extends Charset
{
    public static final ISO_8859_1 INSTANCE = new ISO_8859_1();

    public ISO_8859_1() {
        super("ISO-8859-1", StandardCharsets.aliases_ISO_8859_1());
    }

    public String historicalName() {
        return "ISO8859_1";
    }

    public boolean contains(final Charset cs) {
        return ((cs instanceof ISO_8859_1));
    }

    public CharsetDecoder newDecoder() {
        return new Decoder(this);
    }

    public CharsetEncoder newEncoder() {
        return new Encoder(this);
    }

    private static class Decoder extends CharsetDecoder {

        private Decoder(final Charset cs) {
            super(cs, 1.0f, 1.0f);
        }

        private CoderResult decodeArrayLoop(final ByteBuffer src,
                                            final CharBuffer dst)
        {
            final byte[] sa = src.array();
            int sp = src.arrayOffset() + src.position();
            final int sl = src.arrayOffset() + src.limit();
            assert (sp <= sl);
            sp = (sp <= sl ? sp : sl);
            final char[] da = dst.array();
            int dp = dst.arrayOffset() + dst.position();
            final int dl = dst.arrayOffset() + dst.limit();
            assert (dp <= dl);
            dp = (dp <= dl ? dp : dl);

            try {
                while (sp < sl) {
                    final byte b = sa[sp];
                    if (dp >= dl)
                        return CoderResult.OVERFLOW;
                    da[dp++] = (char)(b & 0xff);
                    sp++;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(sp - src.arrayOffset());
                dst.position(dp - dst.arrayOffset());
            }
        }

        private CoderResult decodeBufferLoop(final ByteBuffer src,
                                             final CharBuffer dst)
        {
            int mark = src.position();
            try {
                while (src.hasRemaining()) {
                    final byte b = src.get();
                    if (!dst.hasRemaining())
                        return CoderResult.OVERFLOW;
                    dst.put((char)(b & 0xff));
                    mark++;
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(mark);
            }
        }

        protected CoderResult decodeLoop(final ByteBuffer src,
                                         final CharBuffer dst)
        {
            if (src.hasArray() && dst.hasArray())
                return decodeArrayLoop(src, dst);
            else
                return decodeBufferLoop(src, dst);
        }
    }

    private static class Encoder extends CharsetEncoder {

        private Encoder(final Charset cs) {
            super(cs, 1.0f, 1.0f);
        }

        public boolean canEncode(final char c) {
            return c <= '\u00FF';
        }

        public boolean isLegalReplacement(final byte[] repl) {
            return true;  // we accept any byte value
        }

        private final Surrogate.Parser sgp = new Surrogate.Parser();

        // Method possible replaced with a compiler intrinsic.
        private static int encodeISOArray(final char[] sa, final int sp,
                                          final byte[] da, final int dp, final int len) {
            if (len <= 0) {
                return 0;
            }
            encodeISOArrayCheck(sa, sp, da, dp, len);
            return implEncodeISOArray(sa, sp, da, dp, len);
        }

        private static int implEncodeISOArray(final char[] sa, int sp,
                                              final byte[] da, int dp, final int len)
        {
            int i = 0;
            for (; i < len; i++) {
                final char c = sa[sp++];
                if (c > '\u00FF')
                    break;
                da[dp++] = (byte)c;
            }
            return i;
        }

        private static void encodeISOArrayCheck(final char[] sa, final int sp,
                                                final byte[] da, final int dp, final int len) {
            Objects.requireNonNull(sa);
            Objects.requireNonNull(da);

            if (sp < 0 || sp >= sa.length) {
                throw new ArrayIndexOutOfBoundsException(sp);
            }

            if (dp < 0 || dp >= da.length) {
                throw new ArrayIndexOutOfBoundsException(dp);
            }

            final int endIndexSP = sp + len - 1;
            if (endIndexSP < 0 || endIndexSP >= sa.length) {
                throw new ArrayIndexOutOfBoundsException(endIndexSP);
            }

            final int endIndexDP = dp + len - 1;
            if (endIndexDP < 0 || endIndexDP >= da.length) {
                throw new ArrayIndexOutOfBoundsException(endIndexDP);
            }
        }

        private CoderResult encodeArrayLoop(final CharBuffer src,
                                            final ByteBuffer dst)
        {
            final char[] sa = src.array();
            final int soff = src.arrayOffset();
            int sp = soff + src.position();
            final int sl = soff + src.limit();
            assert (sp <= sl);
            sp = (sp <= sl ? sp : sl);
            final byte[] da = dst.array();
            final int doff = dst.arrayOffset();
            int dp = doff + dst.position();
            final int dl = doff + dst.limit();
            assert (dp <= dl);
            dp = (dp <= dl ? dp : dl);
            final int dlen = dl - dp;
            final int slen = sl - sp;
            final int len  = (dlen < slen) ? dlen : slen;
            try {
                final int ret = encodeISOArray(sa, sp, da, dp, len);
                sp = sp + ret;
                dp = dp + ret;
                if (ret != len) {
                    if (sgp.parse(sa[sp], sa, sp, sl) < 0)
                        return sgp.error();
                    return sgp.unmappableResult();
                }
                if (len < slen)
                    return CoderResult.OVERFLOW;
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(sp - soff);
                dst.position(dp - doff);
            }
        }

        private CoderResult encodeBufferLoop(final CharBuffer src,
                                             final ByteBuffer dst)
        {
            int mark = src.position();
            try {
                while (src.hasRemaining()) {
                    final char c = src.get();
                    if (c <= '\u00FF') {
                        if (!dst.hasRemaining())
                            return CoderResult.OVERFLOW;
                        dst.put((byte)c);
                        mark++;
                        continue;
                    }
                    if (sgp.parse(c, src) < 0)
                        return sgp.error();
                    return sgp.unmappableResult();
                }
                return CoderResult.UNDERFLOW;
            } finally {
                src.position(mark);
            }
        }

        protected CoderResult encodeLoop(final CharBuffer src,
                                         final ByteBuffer dst)
        {
            if (src.hasArray() && dst.hasArray())
                return encodeArrayLoop(src, dst);
            else
                return encodeBufferLoop(src, dst);
        }
    }
}
