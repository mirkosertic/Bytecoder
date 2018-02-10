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

import de.mirkosertic.bytecoder.api.EmulatedByRuntime;

@EmulatedByRuntime
public class Address {

    private int start;

    public Address(int aStart) {
        start = aStart;
    }

    public static int getStart(Address aAddress) {
        return aAddress.start;
    }

    public static int getIntValue(Address aAddress, int aIndex) {
        return (int) MemoryManager.data[aAddress.start + aIndex];
    }

    public static void setIntValue(Address aAddress, int aIndex, int aValue) {
        MemoryManager.data[aAddress.start + aIndex] = aValue;
    }

    public static int getStackTop() {
        return 0;
    }

    public static int getMemorySize() {
        return 0;
    }

    public static void unreachable() {
        throw new RuntimeException();
    };
}