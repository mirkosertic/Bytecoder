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
package de.mirkosertic.bytecoder.classlib.jdk.internal.misc;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

@SubstitutesInClass(completeReplace = true)
public class TUnsafe {

    private final static TUnsafe INSTANCE = new TUnsafe();

    public static TUnsafe getUnsafe() {
        return INSTANCE;
    }

    public static final int ARRAY_BOOLEAN_INDEX_SCALE = 0;

    public static final int ARRAY_BYTE_INDEX_SCALE = 0;

    public static final int ARRAY_CHAR_INDEX_SCALE = 0;

    public static final int ARRAY_SHORT_INDEX_SCALE = 0;

    public static final int ARRAY_INT_INDEX_SCALE = 0;

    public static final int ARRAY_LONG_INDEX_SCALE = 0;

    public static final int ARRAY_FLOAT_INDEX_SCALE = 0;

    public static final int ARRAY_DOUBLE_INDEX_SCALE = 0;

    public int arrayBaseOffset(final Class clazz) {
        return 0;
    }


    public int arrayIndexScale(final Class clazz) {
        return 0;
    }

    public long objectFieldOffset(final Class clazz, final String fieldName) {
        return 0;
    }

    public void storeFence() {
    }

    public void ensureClassInitialized(final Class<?> c) {
    }

    public boolean isBigEndian() {
        return false;
    }

    public long getLongUnaligned(final Object o, final long a) {
        return a;
    }

    public long getLongUnaligned(final Object o, final long a, final boolean b) {
        return a;
    }

    public int getIntUnaligned(final Object o, final long a) {
        return (int) a;
    }

    public int getIntUnaligned(final Object o, final long a, final boolean b) {
        return (int) a;
    }

    public char getCharUnaligned(final Object o, final long a) {
        return (char) a;
    }

    public char getCharUnaligned(final Object o, final long a, final boolean b) {
        return (char) a;
    }

    public short getShortUnaligned(final Object o, final long a, final boolean b) {
        return (short) a;
    }

    public int getAndAddInt(final Object ol, final long a, final int b) {
        return b;
    }

    public boolean compareAndSetReference(final Object o1, final long l, final Object o2, final Object o3) {
        return false;
    }

    public boolean unalignedAccess() {
        return false;
    }

    public int pageSize() {
        return 8192;
    }

    public long allocateMemory(final long aAmount) {
        return 0;
    }

    public void setMemory(final long a, final long b, final byte c) {
    }

    public float getFloat(final long a) {
        return 0f;
    }

    public void putFloat(final long a, final float b) {
    }

    public byte getByte(final long a) {
        return 0;
    }

    public void putByte(final long a, final byte b) {
    }

    public int getInt(final long a) {
        return 0;
    }

    public int getInt(final Object o, final long a) {
        return 0;
    }

    public void putInt(final long a, final int b) {
    }

    public short getShort(final long a) {
        return 0;
    }

    public void putShort(final long a, final short b) {
    }

    public void putChar(final long a, final char b) {
    }

    public char getChar(final long a) {
        return 0;
    }

    public void putCharUnaligned(final Object a, final long b, final char c, final boolean d) {
    }

    public void copyMemory(final Object o, final long a, final Object b, final long c, final long d) {
    }

    public void copyMemory(final long a, final long b, final long c) {
    }

    public void copySwapMemory(final Object a, final long b, final Object c, final long d, final long e, final long f) {
    }

    public long getAddress(final long a) {
        return a;
    }

    public Object getReference(final Object a, final long b) {
        return a;
    }

    public void putReference(final Object a, final long b, final Object c) {
    }

    public void putReferenceRelease(final Object a, final long b, final Object c) {
    }

    public void freeMemory(final long a) {
    }

    public long reallocateMemory(final long a, final long b) {
        return a;
    }
}
