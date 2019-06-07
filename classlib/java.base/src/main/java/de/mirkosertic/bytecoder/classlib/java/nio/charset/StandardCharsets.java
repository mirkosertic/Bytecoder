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
import java.util.Iterator;

public class StandardCharsets {

    static String[] aliases_UTF_8() {
        return new String[] {"UTF8","unicode-1-1-utf-8",};
    }

    static String[] aliases_UTF_16() {
        return new String[] {"UTF_16","utf16","unicode","UnicodeBig",};
    }

    public StandardCharsets() {
    }

    public Iterator<Charset> charsets() {
        return new Iterator<Charset>() {

            int c = 0;

            @Override
            public boolean hasNext() {
                return c < 2 ;
            }

            @Override
            public Charset next() {
                c++;
                if (c == 1) {
                    return UTF_8.INSTANCE;
                }
                if (c == 2) {
                    return UTF_16.INSTANCE;
                }

                throw new IllegalStateException("EOL");
            }
        };
    }

    public Charset charsetForName(final String charsetName) {
        for (final String name : aliases_UTF_8()) {
            if (name.equalsIgnoreCase(charsetName)) {
                return UTF_8.INSTANCE;
            }
        }
        for (final String name : aliases_UTF_16()) {
            if (name.equalsIgnoreCase(charsetName)) {
                return UTF_16.INSTANCE;
            }
        }
        if (charsetName.equals(UTF_8.INSTANCE.name())) {
            return UTF_8.INSTANCE;
        }
        if (charsetName.equals(UTF_16.INSTANCE.name())) {
            return UTF_16.INSTANCE;
        }
        return null;
    }
}
