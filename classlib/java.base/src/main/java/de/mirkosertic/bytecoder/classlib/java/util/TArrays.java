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

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.util.ArrayList;
import java.util.List;

@SubstitutesInClass(completeReplace = true)
public class TArrays {

    public static void sort(Object[] aData, int aStart, int aEnd) {
    }

    public static <T> List<T> asList(T... aValues) {
        ArrayList<T> theResult = new ArrayList<>();
        for (T theValue : aValues) {
            theResult.add(theValue);
        }
        return theResult;
    }

    public static <T> T[] copyOf(T[] original, int newLength) {
        T[] theResult = (T[]) new Object[newLength];
        for (int i=0;i<original.length;i++) {
            theResult[i] = original[i];
        }
        return theResult;
    }

    public static <T> T[] copyOf(T[] original, int newLength, Class aType) {
        T[] theResult = (T[]) new Object[newLength];
        for (int i=0;i<original.length;i++) {
            theResult[i] = original[i];
        }
        return theResult;
    }
}
