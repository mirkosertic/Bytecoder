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

    public static native int getIntValue(int aAddress, int aIndex);

    public static native void setIntValue(int aAddress, int aIndex, int aValue);

    public static native int getStackTop();

    public static native int getMemorySize();

    public static native int getHeapBase();

    public static native int getDataEnd();

    public static native void unreachable();

    public static native int ptrOf(final Object o);

    public static native int systemHasStack();
}