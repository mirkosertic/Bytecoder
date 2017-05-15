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
import de.mirkosertic.bytecoder.classlib.io.TIOException;

public class TStringBuilder extends TAbstractStringBuilder implements TSerializable {

    private byte[] data;

    @NoExceptionCheck
    public TStringBuilder() {
        data = new byte[0];
    }

    @Override
    @NoExceptionCheck
    public int length() {
        return data.length;
    }

    @Override
    public TStringBuilder append(TCharSequence aCharSequence) throws TIOException {
        byte[] theOtherData = aCharSequence.getBytes();
        byte[] theNewData = new byte[data.length + theOtherData.length];
        int offset = 0;
        for (int i=0;i<data.length;i++) {
            theNewData[offset++] = data[i];
        }
        for (int i=0;i<theOtherData.length;i++) {
            theNewData[offset++] = theOtherData[i];
        }
        data = theNewData;
        return this;
    }

    /*public TStringBuilder append(TString aString) throws TIOException {
        byte[] theOtherData = aString.getBytes();
        byte[] theNewData = new byte[data.length + theOtherData.length];
        int offset = 0;
        for (int i=0;i<data.length;i++) {
            theNewData[offset++] = data[i];
        }
        for (int i=0;i<theOtherData.length;i++) {
            theNewData[offset++] = theOtherData[i];
        }
        data = theNewData;
        return this;
    }*/

    public TStringBuilder append(Object aObject) {
        String theOtherObject = aObject != null ? aObject.toString() : "null";
        byte[] theOtherData = theOtherObject.getBytes();

        byte[] theNewData = new byte[data.length + theOtherData.length];
        int offset = 0;
        for (int i=0;i<data.length;i++) {
            theNewData[offset++] = data[i];
        }
        for (int i=0;i<theOtherData.length;i++) {
            theNewData[offset++] = theOtherData[i];
        }
        data = theNewData;
        return this;
    }

    @Override
    @NoExceptionCheck
    public byte[] getBytes() {
        return data;
    }

    @Override
    @NoExceptionCheck
    public String toString() {
        return new String(data);
    }
}