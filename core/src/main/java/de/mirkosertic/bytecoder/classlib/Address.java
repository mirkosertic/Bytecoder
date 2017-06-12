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
package de.mirkosertic.bytecoder.classlib;

public class Address {

    private int start;
    private Object[] data;

    public Address(int aStart, int aSize) {
        start = aStart;
        data = new Object[aSize];
    }

    public int getStart() {
        return start;
    }

    public int getIntValue(int aIndex) {
        return (int) data[aIndex];
    }

    public Object getObjectValue(int aIndex) {
        return (Address) data[aIndex];
    }

    public void setIntValue(int aIndex, int aValue) {
        data[aIndex] = aValue;
    }

    public void setObjectValue(int aIndex, Address aValue) {
        data[aIndex] = aValue;
    }
}