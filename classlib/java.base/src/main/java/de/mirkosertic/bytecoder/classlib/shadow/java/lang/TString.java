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
package de.mirkosertic.bytecoder.classlib.shadow.java.lang;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

@SubstitutesInClass(completeReplace = true)
public class TString implements java.io.Serializable, Comparable<String> {

    private int computedHash;
    private byte[] data;

    public TString(int aSize) {
        data = new byte[aSize];
        for (int i=0;i<aSize;i++) {
            data[i] = 0;
        }
    }

    public TString(char[] aData) {
        data = new byte[aData.length];
        for (int i=0;i<aData.length;i++) {
            data[i] = (byte) aData[i];
        }
    }

    public void setCharAt(int aIndex, byte aChar) {
        data[aIndex] = aChar;
    }

    @Override
    public String toString() {
        Object a = this;
        return (String) a;
    }

    public TString(byte[] aData) {
        data = aData;
    }

    public TString(TString aOtherString) {
        data = aOtherString.data;
    }

    public TString() {
        data = new byte[0];
    }

    public byte[] getBytes() {
        return data;
    }

    public char charAt(int aIndex) {
        return (char) data[aIndex];
    }

    @Override
    public int compareTo(String o) {
        return 0;
    }

    public int length() {
        return data.length;
    }

    @Override
    public boolean equals(Object aOtherObject) {
        if (this == aOtherObject) {
            return true;
        }
        if (!(aOtherObject instanceof String)) {
            return false;
        }
        String theOtherString = (String) aOtherObject;
        if (!(theOtherString.length() == data.length)) {
            return false;
        }
        byte[] theOtherData = theOtherString.getBytes();
        for (int i=0;i<data.length;i++) {
            if (data[i] != theOtherData[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean equalsIgnoreCase(String aOtherObject) {
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
            byte[] theOtherData = ((String)aOtherObject).getBytes();
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
                h = 31 * h + data[i];
            }
            computedHash = h;
        }
        return h;
    }

    public int indexOf(int aChar) {
        for (int i=0;i<data.length;i++) {
            if (data[i] == aChar) {
                return i;
            }
        }
        return -1;
    }

    public int lastIndexOf(String aValue) {
        return -1;
    }

    public String substring(int aStart) {
        int theLength = data.length - aStart;
        byte[] theNewData = new byte[theLength];
        for (int i=0;i<theLength;i++) {
            theNewData[i] = data[i + aStart];
        }
        return new String(theNewData);
    }

    public String substring(int aStart, int aEnd) {
        int theLength = aEnd - aStart;
        byte[] theNewData = new byte[theLength];
        for (int i=0;i<theLength;i++) {
            theNewData[i] = data[i + aStart];
        }
        return new String(theNewData);
    }

    public String replace(char aOldChar, char aNewChar) {
        byte[] theNewData = new byte[data.length];
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
        char[] theResult = new char[data.length];
        for (int i=0;i<data.length;i++) {
            theResult[i] = (char) data[i];
        }
        return theResult;
    }
}