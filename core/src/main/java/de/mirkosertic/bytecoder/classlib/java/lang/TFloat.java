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

public class TFloat extends TNumber implements TComparable<TFloat> {

    private float floatValue;

    public TFloat(float aValue) {
        floatValue = aValue;
    }

    @Override
    public int compareTo(TFloat o) {
        return 0;
    }

    public static int compare(float f1, float f2) {
        if(f1 < f2) {
            return -1;
        }
        if(f1 > f2) {
            return 1;
        }
        return 0;
    }

    @Override
    public float floatValue() {
        return floatValue;
    }

    @Override
    public int intValue() {
        return (int) floatValue;
    }

    @Override
    public byte byteValue() {
        return (byte) floatValue;
    }

    @Override
    public short shortValue() {
        return (short) floatValue;
    }
}