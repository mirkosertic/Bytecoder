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
package de.mirkosertic.bytecoder.classlib.java.util;

import de.mirkosertic.bytecoder.classlib.java.io.TSerializable;
import de.mirkosertic.bytecoder.classlib.java.lang.TArrayIndexOutOfBoundsException;
import de.mirkosertic.bytecoder.classlib.java.lang.TCloneable;

public class THashMap<K, V> extends TAbstractMap<K, V> implements TSerializable, TCloneable {

    private static class Bucket<K, V> {

        private final int hashCode;
        private final TArrayList<Entry<K, V>> values;

        public Bucket(int aHashCode) {
            hashCode = aHashCode;
            values = new TArrayList<>();
        }

        public boolean containsKey(K aKey) throws TArrayIndexOutOfBoundsException {
            for (int i=0;i<values.size();i++) {
                Entry<K, V> theEntry = values.get(i);
                if (theEntry.key.equals(aKey)) {
                    return true;
                }
            }
            return false;
        }

        public V put(K aKey, V aValue) throws TArrayIndexOutOfBoundsException {
            for (int i=0;i<values.size();i++) {
                Entry<K, V> theEntry = values.get(i);
                if (theEntry.key.equals(aKey)) {
                    V theOldValue = theEntry.value;
                    theEntry.value = aValue;
                    return theOldValue;
                }
            }
            Entry<K, V> theNewEntry = new Entry<>(aKey, aValue);
            values.add(theNewEntry);
            return null;
        }

        public V get(K aKey) throws TArrayIndexOutOfBoundsException {
            for (int i=0;i<values.size();i++) {
                Entry<K, V> theEntry = values.get(i);
                if (theEntry.key.equals(aKey)) {
                    return theEntry.value;
                }
            }
            return null;
        }
    }

    private final TArrayList<Bucket> buckets;

    public THashMap() {
        buckets = new TArrayList<>();
    }

    private Bucket<K, V> findByHashCode(int aHashCode) throws TArrayIndexOutOfBoundsException {
        for (int i=0;i<buckets.size();i++) {
            Bucket<K, V> theBuckets = buckets.get(i);
            if (theBuckets.hashCode == aHashCode) {
                return theBuckets;
            }
        }
        return null;
    }

    @Override
    public boolean containsKey(K aKey) throws TArrayIndexOutOfBoundsException {
        Bucket<K, V> theBucket = findByHashCode(aKey.hashCode());
        if (theBucket != null) {
            return theBucket.containsKey(aKey);
        }
        return false;
    }

    @Override
    public V put(K aKey, V aValue) throws TArrayIndexOutOfBoundsException {
        int theHashCode = aKey.hashCode();
        Bucket<K, V> theBucket = findByHashCode(theHashCode);
        if (theBucket == null) {
            theBucket = new Bucket<>(theHashCode);
            buckets.add(theBucket);
        }
        return theBucket.put(aKey, aValue);
    }

    @Override
    public V get(K aKey) throws TArrayIndexOutOfBoundsException {
        Bucket<K, V> theBucket = findByHashCode(aKey.hashCode());
        if (theBucket != null) {
            return theBucket.get(aKey);
        }
        return null;
    }
}
