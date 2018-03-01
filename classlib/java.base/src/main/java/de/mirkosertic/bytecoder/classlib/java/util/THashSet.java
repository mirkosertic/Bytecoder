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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

@SubstitutesInClass(completeReplace = true)
public class THashSet<T>  {

    private static class Bucket<V> {

        private final int hashCode;
        private final ArrayList<V> values;

        public Bucket(int aHashCode) {
            hashCode = aHashCode;
            values = new ArrayList<>();
        }

        public boolean contains(V aValue) {
            for (int i=0;i<values.size();i++) {
                V theValue = values.get(i);
                if (theValue.equals(aValue)) {
                    return true;
                }
            }
            return false;
        }

        public void add(V aValue) {
            values.add(aValue);
        }

        public boolean remove(V aObject) {
            return values.remove(aObject);
        }
    }

    private final ArrayList<Bucket<T>> buckets;

    public THashSet() {
        buckets = new ArrayList<>();
    }

    public Bucket<T> findByHashCode(int aHashCode) {
        for (int i=0;i<buckets.size();i++) {
            Bucket<T> theBucket = buckets.get(i);
            if (theBucket.hashCode == aHashCode) {
                return theBucket;
            }
        }
        return null;
    }

    public Iterator<T> iterator() {
        return new Iterator<T>() {

            int index = -1;
            Object[] data = toArray();

            @Override
            public T next() {
                return (T) data[index++];
            }

            @Override
            public boolean hasNext() {
                return index < data.length;
            }
        };
    }

    public boolean add(T aObject) {
        int theHashCode = aObject.hashCode();
        Bucket<T> theBucket = findByHashCode(theHashCode);
        if (theBucket == null) {
            theBucket = new Bucket<>(theHashCode);
            theBucket.add(aObject);
            buckets.add(theBucket);
            return true;
        }
        if (!theBucket.contains(aObject)) {
            theBucket.add(aObject);
            return true;
        }
        return false;
    }

    public void clear() {
        buckets.clear();
    }

    public boolean contains(T aObject) {
        int theHashCode = aObject.hashCode();
        Bucket<T> theBucket = findByHashCode(theHashCode);
        if (theBucket == null) {
            return false;
        }
        return theBucket.contains(aObject);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean remove(T aObject) {
        int theHashcode = aObject.hashCode();
        Bucket<T> theBucket = findByHashCode(theHashcode);
        if (theBucket == null) {
            return false;
        }
        return theBucket.remove(aObject);
    }

    public int size() {
        int theSize = 0;
        for (int i=0;i<buckets.size();i++) {
            Bucket<T> theBucket = buckets.get(i);
            theSize+=theBucket.values.size();
        }
        return theSize;
    }

    public Object[] toArray() {
        int theSize = size();
        int theIndex = 0;
        Object[] theResult = new Object[theSize];
        for (int i=0;i<buckets.size();i++) {
            Bucket<T> theBucket = buckets.get(i);
            for (int j=0;j<theBucket.values.size();j++) {
                theResult[theIndex++] = theBucket.values.get(j);
            }
        }
        return theResult;
    }

    public Object[] toArray(Object[] aTarget) {
        int theIndex = 0;
        Object[] theResult = aTarget;
        for (int i=0;i<buckets.size();i++) {
            Bucket<T> theBucket = buckets.get(i);
            for (int j=0;j<theBucket.values.size();j++) {
                theResult[theIndex++] = theBucket.values.get(j);
            }
        }
        return theResult;
    }

    public boolean addAll(Collection<T> aOtherCollection) {
        boolean theChanged = false;
        for (T aValue : aOtherCollection) {
            if (add(aValue)) {
                theChanged = true;
            }
        }
        return theChanged;
    }

    public boolean removeAll(Collection<T> aOtherCollection) {
        boolean theChanged = false;
        for (T aValue : aOtherCollection) {
            if (remove(aValue)) {
                theChanged = true;
            }
        }
        return theChanged;
    }
}