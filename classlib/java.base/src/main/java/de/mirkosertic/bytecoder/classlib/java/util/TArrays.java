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
import java.util.List;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

@SubstitutesInClass(completeReplace = true)
public class TArrays {

    public static void sort(final Object[] aData, final int aStart, final int aEnd) {
    }

    public static <T> List<T> asList(final T... aValues) {
        final ArrayList<T> theResult = new ArrayList<>();
        for (final T theValue : aValues) {
            theResult.add(theValue);
        }
        return theResult;
    }

    public static <T> T[] copyOf(final T[] original, final int newLength) {
        return copyOf(original, newLength, null);
    }

    public static <T> T[] copyOf(final T[] original, final int newLength, final Class aType) {
        int theMax = original.length;
        if (newLength < theMax) {
            theMax = newLength;
        }
        final T[] theResult = (T[]) new Object[newLength];
        for (int i=0;i<theMax;i++) {
            theResult[i] = original[i];
        }
        return theResult;
    }

    public static <T> T[] copyOfRange(final T[] original, final int from, final int to, final Class aType) {
        final T[] theResult = (T[]) new Object[to - from];
        for (int i=from;i<to;i++) {
            theResult[i-from] = original[i];
        }
        return theResult;
    }
}