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
import de.mirkosertic.bytecoder.api.SubstitutesInClass;
import de.mirkosertic.bytecoder.classlib.VM;

@SubstitutesInClass(completeReplace = true)
public class TByte extends Number {

    private byte byteValue;

    @NoExceptionCheck
    public TByte(byte aByteValue) {
        byteValue = aByteValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        TByte tByte = (TByte) o;

        if (byteValue != tByte.byteValue)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return byteValue;
    }

    @Override
    public int intValue() {
        return (int) byteValue;
    }

    @Override
    public float floatValue() {
        return (float) byteValue;
    }

    @Override
    public long longValue() {
        return byteValue;
    }

    @Override
    public double doubleValue() {
        return byteValue;
    }

    @Override
    public String toString() {
        return toString(byteValue);
    }

    public static TByte valueOf(byte aValue) {
        return new TByte(aValue);
    }

    public static TByte valueOf(String aValue) {
        return new TByte((byte) VM.stringToLong(aValue));
    }

    public static byte parseByte(String aString) {
        return (byte) VM.stringToLong(aString);
    }

    public static String toString(byte aValue) {
        StringBuilder theBuffer = new StringBuilder();
        theBuffer.append(aValue);
        return theBuffer.toString();
    }
}
