/*
 * Copyright 2023 Mirko Sertic
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

import java.lang.annotation.Native;

@SubstitutesInClass(completeReplace = true)
public class TShort extends Number implements Comparable<Short> {

    public static final Class<Short> TYPE = (Class<Short>) VM.shortPrimitiveClass();

    @Native
    private final short value;

    public TShort(final short value) {
        this.value = value;
    }

    @Override
    public int intValue() {
        return value;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        final Short obj = (Short) o;

        return value == obj.shortValue();
    }

    @Override
    public int hashCode() {
        return (int) value;
    }

    public static Short valueOf(final short s) {
        return new Short(s);
    }

    public static Short valueOf(final String str) {
        return new Short(parseShort(str));
    }

    public static short parseShort(final String aString) {
        return parseShort(aString, 10);
    }

    public static native short parseShort(final String aString, int radix);

    @Override
    public String toString() {
        return toString(value, 10);
    }

    public static String toString(final short b) {
        return toString(b, 10);
    }

    public static native String toString(short b, int radix);

    public static int signum(final short value) {
        if (value < 0) {
            return -1;
        }
        if (value > 0) {
            return 1;
        }
        return 0;
    }

    @Override
    public int compareTo(final Short o) {
        return compare(this.value, o.shortValue());
    }

    public static int compare(final short x, final short y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

}
