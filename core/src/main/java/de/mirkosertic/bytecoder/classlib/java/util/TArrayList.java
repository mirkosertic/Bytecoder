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

import de.mirkosertic.bytecoder.classlib.java.lang.TArrayIndexOutOfBoundsException;
import de.mirkosertic.bytecoder.classlib.java.lang.TSystem;

public class TArrayList<T> implements TList<T> {

    private static final int INITIAL_CAPACITY = 10;

    private Object[] data;
    private int currentLength;

    public TArrayList() {
        data = new Object[INITIAL_CAPACITY];
        currentLength =0;
    }

    @Override
    public Object[] toArray() {
        Object[] theNewArray = new Object[currentLength];
        System.arraycopy(data, 0, theNewArray, 0, currentLength);
        return theNewArray;
    }

    @Override
    public Object[] toArray(Object[] aTarget) {
        System.arraycopy(data, 0, aTarget, 0, currentLength);
        return aTarget;
    }

    @Override
    public boolean add(T aObject) {
        currentLength++;
        if (currentLength >= data.length) {
            Object[] theNewData = new Object[data.length + INITIAL_CAPACITY];
            TSystem.arraycopy(data, 0, theNewData, 0, data.length);
            data = theNewData;
        }
        data[currentLength-1] = aObject;
        return true;
    }

    @Override
    public T get(int aIndex) throws TArrayIndexOutOfBoundsException {
        if (aIndex >=currentLength || aIndex < 0) {
            throw new TArrayIndexOutOfBoundsException();
        }
        return (T) data[aIndex];
    }

    @Override
    public void clear() {
        currentLength = 0;
        data = new Object[INITIAL_CAPACITY];
    }

    @Override
    public boolean contains(T aObject) {
        for (int i = 0; i<currentLength; i++) {
            Object theData = data[i];
            if (theData != null) {
                if (data[i].equals(aObject)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean remove(Object aObject) {
        for (int i = 0; i < currentLength ; i++) {
            Object theData = data[i];
            if (theData != null) {
                if (data[i].equals(aObject)) {
                    if (i!= currentLength -1) {
                        for (int k = i + 1; k < currentLength; k++) {
                            data[k - 1] = data[k];
                        }
                    }
                    currentLength--;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int size() {
        return currentLength;
    }
}
