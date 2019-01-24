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

@SubstitutesInClass(completeReplace = true)
public class TString implements java.io.Serializable, Comparable<String> {

    private int computedHash;
    private final byte[] data;

    public TString(final int aSize) {
        data = new byte[aSize];
        for (int i=0;i<aSize;i++) {
            data[i] = 0;
        }
    }

    public TString(final char[] aData) {
        data = new byte[aData.length];
        for (int i=0;i<aData.length;i++) {
            data[i] = (byte) aData[i];
        }
    }

    public TString(final char[] value, final int offset, final int count) {
        data = new byte[count];
        for (int i=0;i<count;i++) {
            data[i] = (byte) value[offset + i];
        }
    }

    public void setCharAt(final int aIndex, final byte aChar) {
        data[aIndex] = aChar;
    }

    @Override
    public String toString() {
        final Object a = this;
        return (String) a;
    }

    public TString(final byte[] aData) {
        data = aData;
    }

    public TString(final TString aOtherString) {
        data = aOtherString.data;
    }

    public TString() {
        data = new byte[0];
    }

    public byte[] getBytes() {
        return data;
    }

    public char charAt(final int aIndex) {
        return (char) data[aIndex];
    }

    @Override
    public int compareTo(final String o) {
        return 0;
    }

    public int length() {
        return data.length;
    }

    @Override
    public boolean equals(final Object aOtherObject) {
        if (this == aOtherObject) {
            return true;
        }
        if (!(aOtherObject instanceof String)) {
            return false;
        }
        final String theOtherString = (String) aOtherObject;
        if (!(theOtherString.length() == data.length)) {
            return false;
        }
        final byte[] theOtherData = theOtherString.getBytes();
        for (int i=0;i<data.length;i++) {
            if (data[i] != theOtherData[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean equalsIgnoreCase(final String aOtherObject) {
        if ((Object) this == aOtherObject) {
            return true;
        }
        if (aOtherObject == null) {
            return false;
        }
        if (!(aOtherObject.length() == data.length)) {
            return false;
        }
        for (int i=0;i<data.length;i++) {
            final byte[] theOtherData = ((String)aOtherObject).getBytes();
            if (Character.toLowerCase((char)data[i]) != Character.toLowerCase((char) theOtherData[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = computedHash;
        if (h == 0 && data.length > 0) {
            for (int i = 0; i < data.length; i++) {
                h = 31 * h + (data[i] & 0xff);
            }
            computedHash = h;
        }
        return h;
    }

    public int indexOf(final int aChar) {
        for (int i=0;i<data.length;i++) {
            if (data[i] == aChar) {
                return i;
            }
        }
        return -1;
    }

    public int lastIndexOf(final String aValue) {
        return -1;
    }

    public String substring(final int aStart) {
        final int theLength = data.length - aStart;
        final byte[] theNewData = new byte[theLength];
        for (int i=0;i<theLength;i++) {
            theNewData[i] = data[i + aStart];
        }
        return new String(theNewData);
    }

    public String substring(final int aStart, final int aEnd) {
        final int theLength = aEnd - aStart;
        final byte[] theNewData = new byte[theLength];
        for (int i=0;i<theLength;i++) {
            theNewData[i] = data[i + aStart];
        }
        return new String(theNewData);
    }

    public String replace(final char aOldChar, final char aNewChar) {
        final byte[] theNewData = new byte[data.length];
        for (int i=0;i<data.length;i++) {
            byte theData = data[i];
            if (theData == aOldChar) {
                theData = (byte) aNewChar;
            }
            theNewData[i] = theData;
        }
        return new String(theNewData);
    }

    public char[] toCharArray() {
        final char[] theResult = new char[data.length];
        for (int i=0;i<data.length;i++) {
            theResult[i] = (char) data[i];
        }
        return theResult;
    }

    public static String valueOf(final Object obj) {
        return (obj == null) ? "null" : obj.toString();
    }

    public static String valueOf(final int aValue) {
        return Integer.toString(aValue);
    }

    public static String valueOf(final double aValue) {
        return Double.toString(aValue);
    }

    public static String valueOf(final char aValue) {
        return new String(new byte[] {(byte) aValue});
    }

    public static String format(final String aPattern, final Object[] aValues) {
        return aPattern;
    }

    public String trim() {
        int len = data.length;
        int st = 0;
        while ((st < len) && ((data[st] == ' ' || data[st] == '\u0000'))) {
            st++;
        }
        while ((st < len) && ((data[len - 1] == ' ' || data[len - 1] == '\u0000'))) {
            len--;
        }
        if (st > 0 || len < data.length) {
            final char[] newData = new char[len - st];
            for (int i=0;i<len-st;i++) {
                newData[i] = (char) data[st + i];
            }
            return new String(newData);
        }
        final Object t = (this);
        return (String) t;
    }

    public boolean startsWith(final String aOtherString) {
        final int len = length();
        final int otherlength = aOtherString.length();
        if (otherlength == 0) {
            return false;
        }
        if (len < otherlength) {
            return false;
        }
        final byte[] theOtherData = aOtherString.getBytes();
        for (int i=0;i<otherlength;i++) {
            if (theOtherData[i] != data[i]) {
                return false;
            }
        }
        return true;
    }

    public void getChars(final int srcBegin, final int srcEnd, final char[] dst, int dstBegin) {
        for (int i=srcBegin;i<srcEnd;i++) {
            dst[dstBegin++]=(char) data[i];
        }
    }
}