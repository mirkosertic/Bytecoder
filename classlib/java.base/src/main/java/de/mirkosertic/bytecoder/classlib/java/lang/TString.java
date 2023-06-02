/*
 * Copyright 2019 Mirko Sertic
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
package de.mirkosertic.bytecoder.classlib.java.lang;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.util.Locale;

@SubstitutesInClass(completeReplace = true)
public class TString implements CharSequence, Comparable<String> {

    public TString() {
    }

    public TString(final String value) {
        this();
        initializeWith(value);
    }

    public TString(final byte[] data, final byte coder) {
        this();
        initializeWith(data, 0, data.length, coder);
    }

    public TString(final byte[] data) {
        this();
        initializeWith(data, 0, data.length, (byte) 0);
    }

    public TString(final byte[] data, final int offset, final int count) {
        this();
        initializeWith(data, offset, count, (byte) 0);
    }

    public TString(final char[] data, final int offset, final int count) {
        this();
        initializeWith(data, offset, count);
    }

    public TString(final char[] data) {
        this();
        initializeWith(data, 0, data.length);
    }

    public TString(final int[] data, final int offset, final int count) {
        this();
        initializeWith(data, offset, count);
    }

    native void initializeWith(byte[] data, int offset, int count, byte coder);

    native void initializeWith(final String value);

    native void initializeWith(char[] data, int offset, int count);

    native void initializeWith(int[] data, int offset, int count);

    public String toString() {
        return (String) (Object) this;
    }

    public native boolean matches(String regex);

    static String valueOf(final Object obj) {
        if (obj == null) {
            return "null";
        }
        return obj.toString();
    }

    public static String valueOf(final byte b) {
        return Byte.toString(b);
    }

    public static String valueOf(final char c) {
        return Character.toString(c);
    }

    public static String valueOf(final short s) {
        return Short.toString(s);
    }

    public static String valueOf(final int i) {
        return Integer.toString(i);
    }

    public static String valueOf(final long l) {
        return Long.toString(l);
    }

    public static String valueOf(final float f) {
        return Float.toString(f);
    }

    public static String valueOf(final double d) {
        return Double.toString(d);
    }

    public static String valueOf(final boolean b) {
        return b ? "true" : "false";
    }

    public native char[] toCharArray();

    public native void getChars(int srcBegin, int srcEnd, char dst[], int dstBegin);

    public static native String format(final String pattern, Object[] values);

    public native boolean equalsIgnoreCase(final String str);

    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        return equals0(o.toString());
    }

    public native boolean equals0(final String str);

    public native int indexOf(final int c);

    public native int lastIndexOf(final int c);

    public native int lastIndexOf(final String str);

    public int compareTo(final String anotherString) {
        final char[] currentvalues = toCharArray();
        final char[] othervalues = anotherString.toCharArray();

        final int len1 = currentvalues.length;
        final int len2 = othervalues.length;
        final int lim = Math.min(len1, len2);
        final char[] v1 = currentvalues;
        final char[] v2 = othervalues;

        int k = 0;
        while (k < lim) {
            final char c1 = v1[k];
            final char c2 = v2[k];
            if (c1 != c2) {
                return c1 - c2;
            }
            k++;
        }
        return len1 - len2;
    }

    public native String repeat(int amount);

    public native String substring(int pos, int len);

    public native String substring(int pos);

    public native boolean startsWith(final String value);

    public native boolean endsWith(final String value);

    public native String replaceAll(final String regex, final String replacement);

    public int codePointCount(final int beginIndex, final int endIndex) {
        return endIndex - beginIndex;
    }

    public native String trim();

    public native int length();

    @Override
    public native char charAt(int index);

    @Override
    public CharSequence subSequence(final int start, final int end) {
        return substring(start, end - start);
    }

    public native byte[] getBytes();

    @Override
    public int hashCode() {
        final byte[] value = getBytes();
        int h = 0;
        for (int i = 0; i < value.length; i++) {
            h = 31 * h + value[i];
        }
        return h;
    }

    public native String toUpperCase();

    public native String toUpperCase(final Locale locale);

    public native String toLowerCase();

    public native String toLowerCase(final Locale locale);
}
