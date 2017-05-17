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

import de.mirkosertic.bytecoder.annotations.NoExceptionCheck;
import de.mirkosertic.bytecoder.classlib.java.io.TIOException;

public class TStringBuilder extends TAbstractStringBuilder implements TSerializable {

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

    @Override
    public TStringBuilder append(TCharSequence aCharSequence) throws TIOException {
        byte[] theOtherData = aCharSequence.getBytes();
        internalAdd(theOtherData);
        return this;
    }

    public TStringBuilder append(TString aString) throws TIOException {
        byte[] theOtherData = aString.getBytes();
        internalAdd(theOtherData);
        return this;
    }

    public TStringBuilder append(Object aObject) {
        String theOtherObject = aObject != null ? aObject.toString() : "null";
        byte[] theOtherData = theOtherObject.getBytes();

        internalAdd(theOtherData);
        return this;
    }

    public TStringBuilder append(int aValue) {
        boolean isNegative = false;
        if (aValue < 0) {
            isNegative = true;
            aValue=-aValue;
        }
        byte[] theBytes = new byte[20];
        int theOffset = 0;
        do {
            int theRemainder = aValue % 10;
            theBytes[theOffset++] = (byte) theRemainder;
            aValue = aValue / 10;
        } while (aValue > 0);

        byte[] theNewData;
        int theStart = 0;
        if (isNegative) {
            theNewData = new byte[theOffset + 1];
            theNewData[0] = '-';
            theStart = 1;
        } else {
            theNewData = new byte[theOffset];
            for (int i=0;i<theOffset;i++) {
                theNewData[theStart + i] = (byte) (48 + theBytes[theOffset - 1 - i]);
            }
        }

        internalAdd(theNewData);
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