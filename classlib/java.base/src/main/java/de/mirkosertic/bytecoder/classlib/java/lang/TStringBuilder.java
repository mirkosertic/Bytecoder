/*
 * Copyright 2018 Mirko Sertic
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

import java.text.DecimalFormatSymbols;

@SubstitutesInClass(completeReplace = true)
public class TStringBuilder {

    private static final DecimalFormatSymbols FORMAT_SYMBOLS = new DecimalFormatSymbols();

    private static char[] toCharArray(final CharSequence seq) {
        final int len = seq.length();
        final char[] charData = new char[len];
        for (int i=0;i<len;i++) {
            charData[i] = seq.charAt(i);
        }
        return charData;
    }

    private char[] charData;

    public TStringBuilder() {
        charData = new char[0];
    }

    public TStringBuilder(final int capacity) {
        charData = new char[0];
    }

    public TStringBuilder(final String aOtherString) {
        charData = toCharArray(aOtherString);
    }

    public TStringBuilder(final CharSequence aOtherString) {
        charData = toCharArray(aOtherString);
    }

    public int length() {
        return charData.length;
    }

    public char charAt(final int aIndex) {
        return charData[aIndex];
    }

    public void internalAdd(final char[] aOtherData) {
        final char[] theNewData = new char[charData.length + aOtherData.length];
        int offset = 0;
        for (int i = 0; i< charData.length; i++) {
            theNewData[offset++] = charData[i];
        }
        for (int i=0;i<aOtherData.length;i++) {
            theNewData[offset++] = aOtherData[i];
        }
        charData = theNewData;
    }

    public TStringBuilder reverse() {
        final char[] theReversed = new char[charData.length];
        for (int i=0;i<charData.length;i++) {
            theReversed[charData.length -1 -i] = charData[i];
        }
        charData = theReversed;
        return this;
    }

    public TStringBuilder append(final CharSequence aCharSequence) {
        if (aCharSequence == null) {
            internalAdd(toCharArray("null"));
            return this;
        }
        internalAdd(toCharArray(aCharSequence));
        return this;
    }

    public TStringBuilder append(final String aString) {
        if (aString == null) {
            internalAdd(toCharArray("null"));
            return this;
        }
        internalAdd(toCharArray(aString));
        return this;
    }

    public TStringBuilder append(final char aValue) {
        internalAdd(new char[] {aValue});
        return this;
    }

    public TStringBuilder append(final float aValue) {
        appendInternal(aValue, 1000000000);
        return this;
    }

    public TStringBuilder append(final double aValue) {
        appendInternal(aValue, 1000000000);
        return this;
    }

    public void appendInternal(final double aValue, final long aMultiplier) {
        final long theA;
        final long theB;
        if (aValue < 0) {
            theA = (long) Math.ceil(aValue);
            theB = - (long) Math.ceil((aValue % 1) * 10000);
        } else {
            theA = (long) Math.floor(aValue);
            theB = (long) Math.floor((aValue % 1) * 10000);
        }
        append(theA);

        final StringBuilder theTemp = new StringBuilder();
        theTemp.append(theB);

        for (int i=theTemp.length()-1;i>=0;i--) {
            final char theChar = theTemp.charAt(i);
            if (theChar != '0') {
                append(FORMAT_SYMBOLS.getDecimalSeparator());
                for (int j=0;j<=i;j++) {
                    append(theTemp.charAt(j));
                }
                return;
            }
        }

        append(FORMAT_SYMBOLS.getDecimalSeparator());
        append('0');
    }

    public TStringBuilder append(long aValue) {
        boolean isNegative = false;
        if (aValue < 0) {
            isNegative = true;
            aValue=-aValue;
        }
        final char[] theCharacters = new char[20];
        int theOffset = 0;
        do {
            final int theRemainder = (int) aValue % 10;
            theCharacters[theOffset++] = Character.forDigit(theRemainder, 10);
            aValue = aValue / 10;
        } while (aValue > 0);

        final char[] theNewData;
        final int theStart;
        if (isNegative) {
            theNewData = new char[theOffset + 1];
            theNewData[0] = '-';
            theStart = 1;
        } else {
            theNewData = new char[theOffset];
            theStart = 0;
        }
        for (int i=0;i<theOffset;i++) {
            theNewData[theStart + i] = theCharacters[theOffset - 1 - i];
        }

        internalAdd(theNewData);
        return this;
    }

    public TStringBuilder append(final int aValue) {
        return append((long) aValue);
    }

    public TStringBuilder append(final Object aObject) {
        if (aObject == null) {
            internalAdd(toCharArray("null"));
            return this;
        }
        if (aObject instanceof String) {
            internalAdd(toCharArray((String) aObject));
            return this;
        }
        if (aObject instanceof Long) {
            append(((Long) aObject).longValue());
            return this;
        }
        if (aObject instanceof Integer) {
            append(((Integer) aObject).intValue());
            return this;
        }
        if (aObject instanceof Float) {
            append(((Float) aObject).floatValue());
            return this;
        }
        if (aObject instanceof Double) {
            append(((Double) aObject).doubleValue());
            return this;
        }

        internalAdd(toCharArray(aObject.toString()));
        return this;
    }

    public byte[] getBytes() {
        return toString().getBytes();
    }

    public String toString() {
        return new String(charData);
    }

    public int indexOf(final String aOtherString) {
        return -1;
    }

    public int indexOf(final String aOtherString, final int aIndex) {
        return -1;
    }

    public void setLength(final int aLength) {
    }

    public void setCharAt(final int aIndex, final char aChar) {
    }

    public StringBuilder delete(final int aFrom, final int aTo) {
        return (StringBuilder) (Object) this;
    }

    public StringBuilder replace(final int aFrom, final int aTo, final String replacement) {
        return (StringBuilder) (Object) this;
    }

    public StringBuilder append(final boolean aValue) {
        return (StringBuilder) (Object) this;
    }

    public StringBuilder insert(final int aIndex, final char aChar) {
        return (StringBuilder) (Object) this;
    }

    public TStringBuilder append(final CharSequence aCharSequence, final int a, final int b) {
        if (aCharSequence == null) {
            internalAdd(toCharArray("null"));
            return this;
        }
        internalAdd(toCharArray(aCharSequence));
        return this;
    }

    public StringBuilder append(final char[] aValue, final int a, final int b) {
        return (StringBuilder) (Object) this;
    }

    public StringBuilder append(final char[] aValue) {
        return (StringBuilder) (Object) this;
    }

    public StringBuilder appendCodePoint(final int codePoint) {
        return (StringBuilder) (Object) this;
    }

    public StringBuilder deleteCharAt(final int aIndex) {
        return (StringBuilder) (Object) this;
    }

    public int codePointBefore(final int aIndex) {
        return 0;
    }

    public void ensureCapacity(final int aCapacity) {
    }
}
