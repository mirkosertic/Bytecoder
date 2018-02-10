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

public class TDouble extends TNumber {

    private final double doubleValue;

    public TDouble(double aDoubleValue) {
        doubleValue = aDoubleValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        TDouble tDouble = (TDouble) o;

        if (Double.compare(tDouble.doubleValue, doubleValue) != 0)
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

    public static int compare(double d1, double d2) {
        if(d1 < d2) {
            return -1;
        }
        if(d1 > d2) {
            return 1;
        }
        return 0;
    }

    public static double parseDouble(String aValue) {
        int p = aValue.indexOf('.');
        if (p<0) {
            return TNumber.stringToLong(aValue);
        }
        String thePrefix = aValue.substring(0, p);
        String theSuffix = aValue.substring(p + 1);
        long theA = TNumber.stringToLong(thePrefix);
        long theB = TNumber.stringToLong(theSuffix);
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

    public static TDouble valueOf(String aValue) {
        return new TDouble(parseDouble(aValue));
    }

    public static TDouble valueOf(double aValue) {
        return new TDouble(aValue);
    }

    public static String toString(double aValue) {
        TStringBuilder theBuffer = new TStringBuilder();
        theBuffer.append(aValue);
        return theBuffer.toString();
    }
}