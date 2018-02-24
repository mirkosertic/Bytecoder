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

import de.mirkosertic.bytecoder.api.Substitutes;
import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.nio.charset.Charset;

@SubstitutesInClass(String.class)
public class TString {

    @Substitutes("<clinit>")
    private static void emptyClassInit() {
    }

    private byte[] data;

    @Substitutes("<init>")
    void defaultConstructor(byte[] aData) {
        data = aData;
    }

    @Substitutes("<init>")
    void defaultConstructor(String aData) {
        byte[] theOtherData = aData.getBytes();
        data = new byte[theOtherData.length];
        for (int i=0;i<theOtherData.length;i++) {
            data[i] = theOtherData[i];
        }
    }

    @Substitutes("getBytes")
    public byte[] getBytes(String charsetName) {
        return data;
    }

    @Substitutes("getBytes")
    public byte[] getBytes(Charset charset) {
        return data;
    }

    @Substitutes("getBytes")
    public byte[] getBytes() {
        return data;
    }

    @Substitutes("length")
    public int length() {
        return data.length;
    }

    @Substitutes("equals")
    public boolean equals(Object aOtherObject) {
        if (aOtherObject == null) {
            return false;
        }
        if (!(aOtherObject instanceof String)) {
            return false;
        }
        String theOtherString = (String) aOtherObject;
        byte[] theData = theOtherString.getBytes();
        byte[] theThisData = getBytes();
        if (theData.length != theThisData.length) {
            return false;
        }

        for (int i=0;i<theData.length;i++) {
            if (!(theData[i] == theThisData[i])) {
                return false;
            }
        }

        return true;
    }
}