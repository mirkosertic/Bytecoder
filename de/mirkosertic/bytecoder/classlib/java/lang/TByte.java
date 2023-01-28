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
public class TByte extends Number implements Comparable<Byte> {

    public static final Class<Byte> TYPE = (Class<Byte>) VM.bytePrimitiveClass();

    @Native
    private final byte value;

    public TByte(final byte value) {
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

        final Byte obj = (Byte) o;

        return value == obj.byteValue();
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return toString(value, 10);
    }

    public static Byte valueOf(final byte b) {
        return new Byte(b);
    }

    public static Byte valueOf(final String str) {
        return new Byte(parseByte(str));
    }

    public static native byte parseByte(final String aString);

    public static String toString(final byte b) {
        return toString(b, 10);
    }

    public static native String toString(byte b, int radix);

    public static int signum(final byte value) {
        if (value < 0) {
            return -1;
        }
        if (value > 0) {
            return 1;
        }
        return 0;
    }

    @Override
    public int compareTo(final Byte o) {
        return compare(this.value, o.byteValue());
    }

    public static int compare(final byte x, final byte y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }
}
