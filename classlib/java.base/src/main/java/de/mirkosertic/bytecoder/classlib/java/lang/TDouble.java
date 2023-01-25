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

import java.lang.annotation.Native;

@SubstitutesInClass(completeReplace = true)
public class TDouble extends Number {

    public static final Class<Double> TYPE = (Class<Double>) VM.doublePrimitiveClass();

    @Native
    private final double value;

    public TDouble(final double value) {
        this.value = value;
    }

    @Override
    public int intValue() {
        return (int) value;
    }

    @Override
    public long longValue() {
        return (long) value;
    }

    @Override
    public float floatValue() {
        return (float) value;
    }

    @Override
    public double doubleValue() {
        return value;
    }

    public static int compare(final double d1, final double d2) {
        if (d1 < d2) {
            return -1;
        }
        if (d1 > d2) {
            return 1;
        }
        return 0;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        final Double obj = (Double) o;

        return value == obj.doubleValue();
    }

    public static native double parseDouble(final String aValue);

    @Override
    public native String toString();

    public static Double valueOf(final String aValue) {
        return parseDouble(aValue);
    }

    public static Double valueOf(final double aValue) {
        return new Double(aValue);
    }

    public static native boolean isNaN(final double aValue);

    public native static String toString(final double aValue);

    public static int signum(final double value) {
        if (value < 0) {
            return -1;
        }
        if (value > 0) {
            return 1;
        }
        return 0;
    }

}
