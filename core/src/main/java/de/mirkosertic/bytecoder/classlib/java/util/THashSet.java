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

public class THashSet<T> implements TSet<T> {

    @Override
    public TIterator<T> iterator() {
        return null;
    }

    @Override
    public boolean add(T aObject) {
        return false;
    }

    @Override
    public void clear() {
    }

    @Override
    public boolean contains(T aObject) {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean remove(T aObject) {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public Object[] toArray(Object[] aTarget) {
        return new Object[0];
    }

    @Override
    public boolean addAll(TCollection<T> aOtherCollection) {
        return false;
    }

    @Override
    public boolean removeAll(TCollection<T> aOtherCollection) {
        return false;
    }
}