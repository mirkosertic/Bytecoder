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
public class TDouble extends Number {

    public static final double NaN = 0.0d / 0.0;

    private final double doubleValue;

    public TDouble(final double aDoubleValue) {
        doubleValue = aDoubleValue;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final Double aDouble = (Double) o;

        if (Double.compare(aDouble.doubleValue(), doubleValue) != 0)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) doubleValue;
    }

    @Override
    public int intValue() {
        return (int) doubleValue;
    }

    @Override
    public byte byteValue() {
        return (byte) doubleValue;
    }

    @Override
    public short shortValue() {
        return (short) doubleValue;
    }

    @Override
    public float floatValue() {
        return (float) doubleValue;
    }

    @Override
    public long longValue() {
        return (long) doubleValue;
    }

    @Override
    public double doubleValue() {
        return doubleValue;
    }

    public static int compare(final double d1, final double d2) {
        if(d1 < d2) {
            return -1;
        }
        if(d1 > d2) {
            return 1;
        }
        return 0;
    }

    public static double parseDouble(final String aValue) {
        final int p = aValue.indexOf('.');
        if (p<0) {
            return VM.stringToLong(aValue);
        }
        final String thePrefix = aValue.substring(0, p);
        final String theSuffix = aValue.substring(p + 1);
        final long theA = VM.stringToLong(thePrefix);
        final long theB = VM.stringToLong(theSuffix);
        int theMultiplier = 1;
        int theLength = Long.toString(theB).length();
        while(theLength > 0) {
            theMultiplier *= 10;
            theLength--;
        }
        if (theA > 0) {
            return theA + ((double) theB) / theMultiplier;
        }
        return theA - ((double) theB) / theMultiplier;
    }

    @Override
    public String toString() {
        return toString(doubleValue);
    }

    public static Double valueOf(final String aValue) {
        return new Double(parseDouble(aValue));
    }

    public static Double valueOf(final double aValue) {
        return new Double(aValue);
    }

    public static boolean isNaN(final double aValue) {
        return !(aValue == aValue);
    }

    public static String toString(final double aValue) {
        final StringBuilder theBuffer = new StringBuilder();
        theBuffer.append(aValue);
        return theBuffer.toString();
    }

    public static long doubleToLongBits(final double aValue) {
        return 0;
    }

    public static boolean isInfinite(final double aValue) {
        return false;
    }

    public static double longBitsToDouble(final long aValue) {
        return 0d;
    }
}