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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

@SubstitutesInClass(completeReplace = true)
public abstract class TCharset
        implements Comparable<java.nio.charset.Charset>
{

    private static void checkName(final String s) {
        final int n = s.length();
        if (n == 0) {
            throw new IllegalCharsetNameException(s);
        }
        for (int i = 0; i < n; i++) {
            final char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') continue;
            if (c >= 'a' && c <= 'z') continue;
            if (c >= '0' && c <= '9') continue;
            if (c == '-' && i != 0) continue;
            if (c == '+' && i != 0) continue;
            if (c == ':' && i != 0) continue;
            if (c == '_' && i != 0) continue;
            if (c == '.' && i != 0) continue;
            throw new IllegalCharsetNameException(s);
        }
    }

    private static final StandardCharsets standardProvider
            = new StandardCharsets();

    private static final String[] zeroAliases = new String[0];

    private static volatile Object[] cache1; // "Level 1" cache
    private static volatile Object[] cache2; // "Level 2" cache

    private static void cache(final String charsetName, final java.nio.charset.Charset cs) {
        cache2 = cache1;
        cache1 = new Object[] { charsetName, cs };
    }

    private static java.nio.charset.Charset lookup(final String charsetName) {
        if (charsetName == null)
            throw new IllegalArgumentException("Null charset name");
        final Object[] a;
        if ((a = cache1) != null && charsetName.equals(a[0]))
            return (java.nio.charset.Charset)a[1];
        // We expect most programs to use one Charset repeatedly.
        // We convey a hint to this effect to the VM by putting the
        // level 1 cache miss code in a separate method.
        return lookup2(charsetName);
    }

    private static java.nio.charset.Charset lookup2(final String charsetName) {
        final Object[] a;
        if ((a = cache2) != null && charsetName.equals(a[0])) {
            cache2 = cache1;
            cache1 = a;
            return (java.nio.charset.Charset)a[1];
        }
        final java.nio.charset.Charset cs;
        try {
            if ((cs = standardProvider.charsetForName(charsetName)) != null) {
                cache(charsetName, cs);
                return cs;
            }
        } catch (final Exception e) {
            throw new IllegalArgumentException("Instantiation error", e);
        }
        /* Only need to check the name if we didn't find a charset for it */
        checkName(charsetName);
        return null;
    }

    public static boolean isSupported(final String charsetName) {
        return (lookup(charsetName) != null);
    }

    public static java.nio.charset.Charset forName(final String charsetName) {
        final java.nio.charset.Charset cs = lookup(charsetName);
        if (cs != null)
            return cs;
        throw new UnsupportedCharsetException(charsetName);
    }

    // Fold charsets from the given iterator into the given map, ignoring
    // charsets whose names already have entries in the map.
    //
    private static void put(final Iterator<java.nio.charset.Charset> i, final Map<String, java.nio.charset.Charset> m) {
        while (i.hasNext()) {
            final java.nio.charset.Charset cs = i.next();
            if (!m.containsKey(cs.name()))
                m.put(cs.name(), cs);
        }
    }

    public static SortedMap<String, java.nio.charset.Charset> availableCharsets() {
        final TreeMap<String, java.nio.charset.Charset> m =
                new TreeMap<>(
                        String.CASE_INSENSITIVE_ORDER);
        put(standardProvider.charsets(), m);
        return Collections.unmodifiableSortedMap(m);
    }

    private static volatile java.nio.charset.Charset defaultCharset;

    public static java.nio.charset.Charset defaultCharset() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (defaultCharset == null) {
            defaultCharset = (Charset) Class.forName("sun.nio.cs.UTF_8").newInstance();
        }
        return defaultCharset;
    }


    /* -- Instance fields and methods -- */

    private final String name;          // tickles a bug in oldjavac
    private final String[] aliases;     // tickles a bug in oldjavac
    private Set<String> aliasSet = null;

    protected TCharset(final String canonicalName, final String[] aliases) {
        final String[] as = aliases != null ? aliases : zeroAliases;

        // Skip checks for the standard, built-in Charsets we always load
        // during initialization.
        if (canonicalName != "ISO-8859-1"
                && canonicalName != "US-ASCII"
                && canonicalName != "UTF-8") {
            checkName(canonicalName);
            for (int i = 0; i < as.length; i++) {
                checkName(as[i]);
            }
        }
        this.name = canonicalName;
        this.aliases = as;
    }

    public final String name() {
        return name;
    }

    public final Set<String> aliases() {
        if (aliasSet != null)
            return aliasSet;
        final int n = aliases.length;
        final HashSet<String> hs = new HashSet<>(n);
        for (int i = 0; i < n; i++)
            hs.add(aliases[i]);
        aliasSet = Collections.unmodifiableSet(hs);
        return aliasSet;
    }

    public String displayName() {
        return name;
    }

    public final boolean isRegistered() {
        return !name.startsWith("X-") && !name.startsWith("x-");
    }

    public String displayName(final Locale locale) {
        return name;
    }

    public abstract boolean contains(java.nio.charset.Charset cs);

    public abstract CharsetDecoder newDecoder();

    public abstract CharsetEncoder newEncoder();

    public boolean canEncode() {
        return true;
    }

    public final CharBuffer decode(final ByteBuffer bb) {
        try {
            return CharsetEncoderDecoderCache.decoderFor((Charset) (Object)this )
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE)
                    .decode(bb);
        } catch (final CharacterCodingException x) {
            throw new Error(x);         // Can't happen
        }
    }

    public final ByteBuffer encode(final CharBuffer cb) {
        try {
            return CharsetEncoderDecoderCache.encoderFor((Charset) (Object) this)
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE)
                    .encode(cb);
        } catch (final CharacterCodingException x) {
            throw new Error(x);         // Can't happen
        }
    }

    public final ByteBuffer encode(final String str) {
        return encode(CharBuffer.wrap(str));
    }

    public final int compareTo(final java.nio.charset.Charset that) {
        return (name().compareToIgnoreCase(that.name()));
    }

    public final int hashCode() {
        return name().hashCode();
    }

    public final boolean equals(final Object ob) {
        if (!(ob instanceof java.nio.charset.Charset))
            return false;
        if (this == ob)
            return true;
        return name.equals(((java.nio.charset.Charset)ob).name());
    }

    public final String toString() {
        return name();
    }
}