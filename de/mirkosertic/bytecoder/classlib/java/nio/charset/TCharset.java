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

import de.mirkosertic.bytecoder.api.SubstitutesInClass;
import de.mirkosertic.bytecoder.classlib.BytecoderCharset;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

@SubstitutesInClass(completeReplace = true)
public abstract class TCharset implements Comparable<java.nio.charset.Charset> {

    private static Charset defaultCharset;

    private final String canonicalName;

    private final String[] aliases;

    public static Charset defaultCharset() {
        if (defaultCharset == null) {
            defaultCharset = new BytecoderCharset("UTF-8", new String[0]);
        }
        return defaultCharset;
    }

    public static Charset forName(final String name) {
        return new BytecoderCharset(name, new String[0]);
    }

    public TCharset(final String canonicalName, final String[] aliases) {
        this.canonicalName = canonicalName;
        this.aliases = aliases;
    }

    public String name() {
        return this.canonicalName;
    }

    public abstract CharsetEncoder newEncoder();

    public abstract CharsetDecoder newDecoder();

    public static boolean isSupported(final String charactersetName) {
        return "UTF-8".equals(charactersetName);
    }

    public static boolean atBugLevel(final String level) {
        return true;
    }
}
