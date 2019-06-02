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

import de.mirkosertic.bytecoder.api.SubstitutesInClass;
import de.mirkosertic.bytecoder.classlib.VM;

@SubstitutesInClass(completeReplace = true)
public class TLong extends Number {

    private final long longValue;

    public TLong(long aLongValue) {
        longValue = aLongValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Long tLong = (Long) o;

        if (longValue != tLong.longValue())
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) longValue;
    }

    @Override
    public int intValue() {
        return (int) longValue;
    }

    @Override
    public float floatValue() {
        return (float) longValue;
    }

    @Override
    public long longValue() {
        return longValue;
    }

    @Override
    public double doubleValue() {
        return longValue;
    }

    @Override
    public String toString() {
        return toString(longValue);
    }

    public static Long valueOf(long aValue) {
        return new Long(aValue);
    }

    public static Long valueOf(String aValue) {
        return new Long(VM.stringToLong(aValue));
    }

    public static long parseLong(String aString) {
        return VM.stringToLong(aString);
    }

    public static String toString(long aValue) {
        StringBuilder theBuffer = new StringBuilder();
        theBuffer.append(aValue);
        return theBuffer.toString();
    }

    public static String toString(long aValue, int aBase) {
        StringBuilder theBuffer = new StringBuilder();
        theBuffer.append(aValue);
        return theBuffer.toString();
    }

    public static String toHexString(long aValue) {
        return VM.longToHex(aValue);
    }
}