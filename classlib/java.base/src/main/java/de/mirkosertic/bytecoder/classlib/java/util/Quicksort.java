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

import java.util.Comparator;

// Code taken from https://stackabuse.com/sorting-algorithms-in-java/#quicksort
public class Quicksort {

    static <T> int partition(final T[] array, final int begin, final int end, final Comparator<T> comparator) {
        final int pivot = end;

        int counter = begin;
        for (int i = begin; i < end; i++) {
            if (comparator.compare(array[i], array[pivot]) < 0) {
                final T temp = array[counter];
                array[counter] = array[i];
                array[i] = temp;
                counter++;
            }
        }
        final T temp = array[pivot];
        array[pivot] = array[counter];
        array[counter] = temp;

        return counter;
    }

    static <T> int partitionComparable(final T[] array, final int begin, final int end) {
        final int pivot = end;

        int counter = begin;
        for (int i = begin; i < end; i++) {
            if (((Comparable) array[i]).compareTo(array[pivot]) < 0) {
                final T temp = array[counter];
                array[counter] = array[i];
                array[i] = temp;
                counter++;
            }
        }
        final T temp = array[pivot];
        array[pivot] = array[counter];
        array[counter] = temp;

        return counter;
    }

    public static <T> void quickSort(final T[] array, final int begin, final int end, final Comparator<T> comparator) {
        if (end <= begin) return;
        final int pivot = partition(array, begin, end, comparator);
        quickSort(array, begin, pivot-1, comparator);
        quickSort(array, pivot+1, end, comparator);
    }

    public static <T> void quickSortComparable(final T[] array, final int begin, final int end) {
        if (end <= begin) return;
        final int pivot = partitionComparable(array, begin, end);
        quickSortComparable(array, begin, pivot-1);
        quickSortComparable(array, pivot+1, end);
    }

}
