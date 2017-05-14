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

import de.mirkosertic.bytecoder.classlib.io.TIOException;

public class TStringBuilder extends TAbstractStringBuilder implements TSerializable {

    private byte[] data;

    public TStringBuilder() {
        data = new byte[0];
    }

    @Override
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
        for (int i=0;i<theNewData.length;i++) {
            theNewData[offset++] = theNewData[i];
        }
        data = theNewData;
        return this;
    }

    public TStringBuilder append(TString aString) throws TIOException {
        byte[] theOtherData = aString.getBytes();
        byte[] theNewData = new byte[data.length + theOtherData.length];
        int offset = 0;
        for (int i=0;i<data.length;i++) {
            theNewData[offset++] = data[i];
        }
        for (int i=0;i<theNewData.length;i++) {
            theNewData[offset++] = theNewData[i];
        }
        data = theNewData;
        return this;
    }

    @Override
    public byte[] getBytes() {
        return data;
    }

    @Override
    public String toString() {
        return new String(data);
    }
}
