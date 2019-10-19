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
package de.mirkosertic.bytecoder.classlib.java.util.concurrent.atomic;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

@SubstitutesInClass(completeReplace = true)
public class TAtomicLong extends Number {

    private long value;

    public TAtomicLong() {
        value = 0L;
    }

    public TAtomicLong(final long aValue) {
        value = aValue;
    }

    @Override
    public int intValue() {
        return (int) value;
    }

    @Override
    public byte byteValue() {
        return (byte) value;
    }

    @Override
    public short shortValue() {
        return (short) value;
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }

    public final long incrementAndGet() {
        value = ++value;
        return value;
    }

    public final long get() {
        return value;
    }

    public final void set(final long aValue) {
        value = aValue;
    }

    public final long getAndSet(final long aNewValue) {
        final long old = value;
        value = aNewValue;
        return old;
    }

    public final long addAndGet(final long aValue) {
        value+= aValue;
        return value;
    }

    public final long decrementAndGet() {
        value--;
        return value;
    }

    public final long getAndIncrement() {
        final long old = value;
        value++;
        return old;
    }

    public final boolean compareAndSet(final long expected, final long newValue) {
        if (value == expected) {
            value = newValue;
            return true;
        }
        return false;
    }
}