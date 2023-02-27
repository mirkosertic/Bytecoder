/*
 * Copyright 2023 Mirko Sertic
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
package de.mirkosertic.bytecoder.classlib;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

public class BytecoderCharsetEncoder extends CharsetEncoder {

    public BytecoderCharsetEncoder(final Charset cs) {
        super(cs, 1.1f, 3.0f);
    }

    @Override
    protected CoderResult encodeLoop(final CharBuffer in, final ByteBuffer out) {
        final Charset cs = charset();
        if (in.hasRemaining()) {
            final int size = in.remaining();
            final char[] chardata = new char[size];

            in.get(chardata, 0, size);

            final byte[] converted = encodeToBytes(cs.name(), chardata);
            out.put(converted);
        }
        return CoderResult.UNDERFLOW;
    }

    native byte[] encodeToBytes(final String charsetName, char[] characters);
}
