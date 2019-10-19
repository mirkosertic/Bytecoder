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
public class TAtomicMarkableReference<V> {

    private V reference;

    public TAtomicMarkableReference(final V initialRef, final boolean initialMark) {
        reference = initialRef;
    }

    public boolean compareAndSet(final V       expectedReference,
                                 final V       newReference,
                                 final boolean expectedMark,
                                 final boolean newMark) {
        if (reference == expectedReference) {
            reference = newReference;
            return true;
        }
        return false;
    }

    public V get(final boolean[] markHolder) {
        return reference;
    }

    public V getReference() {
        return reference;
    }

    public void set(final V newReference, final boolean newMark) {
        reference = newReference;
    }

    public boolean isMarked() {
        return false;
    }
}
