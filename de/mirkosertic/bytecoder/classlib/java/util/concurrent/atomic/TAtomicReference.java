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
public class TAtomicReference<V> {

    private V value;

    public TAtomicReference(final V aValue) {
        value = aValue;
    }

    public TAtomicReference() {
        value = null;
    }

    public final V get() {
        return value;
    }

    public final void set(final V aValue) {
        value = aValue;
    }

    public final V getAndSet(final V aNewValue) {
        final V old = value;
        value = aNewValue;
        return old;
    }

    public boolean compareAndSet(final V expect, final V update) {
        if (value == expect) {
            value = update;
            return true;
        }
        return false;
    }
}