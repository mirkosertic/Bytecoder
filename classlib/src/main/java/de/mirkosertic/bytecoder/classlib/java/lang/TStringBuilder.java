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
package de.mirkosertic.bytecoder.classlib.java.lang;

import de.mirkosertic.bytecoder.api.NoExceptionCheck;
import de.mirkosertic.bytecoder.classlib.java.text.TDecimalFormatSymbols;

public class TStringBuilder extends TAbstractStringBuilder implements TSerializable {

    private static final TDecimalFormatSymbols FORMAT_SYMBOLS = new TDecimalFormatSymbols();

    private byte[] byteData;

    @NoExceptionCheck
    public TStringBuilder() {
        byteData = new byte[0];
    }

    @NoExceptionCheck
    public TStringBuilder(byte[] aData) {
        byteData = aData;
    }

    @Override
    @NoExceptionCheck
    public int length() {
        return byteData.length;
    }

    @Override
    public char charAt(int aIndex) {
        return (char) byteData[aIndex];
    }

    public void internalAdd(byte[] aOtherData) {
        byte[] theNewData = new byte[byteData.length + aOtherData.length];
        int offset = 0;
        for (int i = 0; i< byteData.length; i++) {
            theNewData[offset++] = byteData[i];
        }
        for (int i=0;i<aOtherData.length;i++) {
            theNewData[offset++] = aOtherData[i];
        }
        byteData = theNewData;
    }

    public TStringBuilder reverse() {
        byte[] theReversed = new byte[byteData.length];
        for (int i=0;i<byteData.length;i++) {
            theReversed[byteData.length -1 -i] = byteData[i];
        }
        byteData = theReversed;
        return this;
    }

    @Override
    public TStringBuilder append(TCharSequence aCharSequence) {
        byte[] theOtherData = aCharSequence.getBytes();
        internalAdd(theOtherData);
        return this;
    }

    public TStringBuilder append(TString aString) {
        byte[] theOtherData = aString.getBytes();
        internalAdd(theOtherData);
        return this;
    }

    public TStringBuilder append(char aValue) {
        internalAdd(new byte[] {(byte) aValue});
        return this;
    }

    public TStringBuilder append(float aValue) {
        appendInternal(aValue, 1000000000);
        return this;
    }

    public TStringBuilder append(double aValue) {
        appendInternal(aValue, 1000000000);
        return this;
    }

    public void appendInternal(double aValue, long aMultiplier) {
        long theA;
        long theB;
        if (aValue < 0) {
            theA = (long) Math.ceil(aValue);
            theB = - (long) Math.ceil((aValue % 1) * 10000);
        } else {
            theA = (long) Math.floor(aValue);
            theB = (long) Math.floor((aValue % 1) * 10000);
        }
        append(theA);

        StringBuilder theTemp = new StringBuilder();
        theTemp.append(theB);

        for (int i=theTemp.length()-1;i>=0;i--) {
            char theChar = theTemp.charAt(i);
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
        byte[] theBytes = new byte[20];
        int theOffset = 0;
        do {
            int theRemainder = (int) aValue % 10;
            theBytes[theOffset++] = (byte) theRemainder;
            aValue = aValue / 10;
        } while (aValue > 0);

        byte[] theNewData;
        int theStart;
        if (isNegative) {
            theNewData = new byte[theOffset + 1];
            theNewData[0] = '-';
            theStart = 1;
        } else {
            theNewData = new byte[theOffset];
            theStart = 0;
        }
        for (int i=0;i<theOffset;i++) {
            theNewData[theStart + i] = (byte) (48 + theBytes[theOffset - 1 - i]);
        }

        internalAdd(theNewData);
        return this;
    }

    public TStringBuilder append(int aValue) {
        return append((long) aValue);
    }

    public TStringBuilder append(Object aObject) {
        if (aObject == null) {
            internalAdd("null".getBytes());
            return this;
        }
        if (aObject instanceof String) {
            internalAdd(((String) aObject).getBytes());
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

        byte[] theOtherData = aObject.toString().getBytes();
        internalAdd(theOtherData);
        return this;
    }

    @Override
    @NoExceptionCheck
    public byte[] getBytes() {
        return byteData;
    }

    @Override
    @NoExceptionCheck
    public String toString() {
        return new String(byteData);
    }
}