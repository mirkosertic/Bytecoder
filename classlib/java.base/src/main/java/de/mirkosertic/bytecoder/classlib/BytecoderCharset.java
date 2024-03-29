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

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public class BytecoderCharset extends Charset {

    public BytecoderCharset(final String canonicalName, final String[] aliases) {
        super(canonicalName, aliases);
    }

    @Override
    public boolean contains(final Charset cs) {
        if (cs == this) {
            return true;
        }
        return false;
    }

    @Override
    public CharsetDecoder newDecoder() {
        return new BytecoderCharsetDecoder(this);
    }

    @Override
    public CharsetEncoder newEncoder() {
        return new BytecoderCharsetEncoder(this);
    }
}
