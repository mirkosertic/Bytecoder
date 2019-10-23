/*
 * Copyright 2019 Mirko Sertic
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
package de.mirkosertic.bytecoder.classlib.java.lang.ref;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;

@SubstitutesInClass(completeReplace = true)
public class TReference<T> {

    private T referent;
    private ReferenceQueue queue;
    private Reference<T> next;

    TReference(final T referent) {
        this.referent = referent;
    }

    TReference(final T referent, final ReferenceQueue queue) {
        this.referent = referent;
        this.queue = queue;
    }

    public T get() {
        return referent;
    }

    public void clear() {
        referent = null;
    }

    public static void reachabilityFence(final Object value) {
    }
}
