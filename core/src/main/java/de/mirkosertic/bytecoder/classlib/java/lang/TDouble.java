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
}