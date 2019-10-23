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
public class TAtomicBoolean {

    private boolean value;

    public TAtomicBoolean(final boolean aValue) {
        value = aValue;
    }

    public TAtomicBoolean() {
        value = false;
    }

    public final boolean get() {
        return value;
    }

    public final void set(final boolean aValue) {
        value = aValue;
    }

    public final boolean getAndSet(final boolean aNewValue) {
        final boolean old = value;
        value = aNewValue;
        return old;
    }

    public final boolean compareAndSet(final boolean expected, final boolean newValue) {
        if (value == expected) {
            value = newValue;
            return true;
        }
        return false;
    }
}