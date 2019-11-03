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

    static final String[] aliases_UTF_8() {
        return new String[] {"UTF8","unicode-1-1-utf-8",};
    }

    static final String[] aliases_UTF_16() {
        return new String[] {"UTF_16","utf16","unicode","UnicodeBig",};
    }

    static final String[] aliases_ISO_8859_1() { return new String[]{"iso-ir-100", "ISO_8859-1", "latin1", "l1", "IBM819", "cp819", "csISOLatin1", "819", "IBM-819", "ISO8859_1", "ISO_8859-1:1987", "ISO_8859_1", "8859_1", "ISO8859-1"};}

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
                try {
                    if (c == 1) {
                        return (Charset) Class.forName("sun.nio.cs.UTF_8").newInstance();
                    }
                    if (c == 2) {
                        return (Charset) Class.forName("sun.nio.cs.UTF_16").newInstance();
                    }
                    if (c == 3) {
                        return (Charset) Class.forName("sun.nio.cs.ISO_88591_1").newInstance();
                    }
                } catch (final Exception e) {
                    throw new IllegalStateException("Instantiation erroe", e);
                }
                throw new IllegalStateException("EOL");
            }
        };
    }

    public Charset charsetForName(final String charsetName) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        for (final String name : aliases_UTF_8()) {
            if (name.equalsIgnoreCase(charsetName)) {
                return (Charset) Class.forName("sun.nio.cs.UTF_8").newInstance();
            }
        }
        for (final String name : aliases_UTF_16()) {
            if (name.equalsIgnoreCase(charsetName)) {
                return (Charset) Class.forName("sun.nio.cs.UTF_16").newInstance();
            }
        }
        for (final String name : aliases_ISO_8859_1()) {
            if (name.equalsIgnoreCase(charsetName)) {
                return (Charset) Class.forName("sun.nio.cs.ISO_8859_1").newInstance();
            }
        }
        if (charsetName.equals("UTF-8")) {
            return (Charset) Class.forName("sun.nio.cs.UTF_8").newInstance();
        }
        if (charsetName.equals("UTF-16")) {
            return (Charset) Class.forName("sun.nio.cs.UTF_16").newInstance();
        }
        if (charsetName.equals("ISO-8859-1")) {
            return (Charset) Class.forName("sun.nio.cs.ISO_8859_1").newInstance();
        }
        return null;
    }
}
