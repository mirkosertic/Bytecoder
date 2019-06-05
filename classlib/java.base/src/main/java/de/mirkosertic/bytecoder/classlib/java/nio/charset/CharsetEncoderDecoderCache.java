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

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.Map;

public class CharsetEncoderDecoderCache {

    private static final Map<String, CharsetEncoder> encoders = new HashMap<>();
    private static final Map<String, CharsetDecoder> decoders = new HashMap<>();

    public static CharsetEncoder encoderFor(final Charset charset) {
        CharsetEncoder e = encoders.get(charset.name());
        if (e == null) {
            e = charset.newEncoder();
            encoders.put(charset.name(), e);
        }
        return e;
    }

    public static CharsetDecoder decoderFor(final Charset charset) {
        CharsetDecoder e = decoders.get(charset.name());
        if (e == null) {
            e = charset.newDecoder();
            decoders.put(charset.name(), e);
        }
        return e;
    }

}
