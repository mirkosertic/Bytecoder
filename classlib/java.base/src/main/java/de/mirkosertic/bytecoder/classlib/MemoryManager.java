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
package de.mirkosertic.bytecoder.classlib;

import de.mirkosertic.bytecoder.api.Export;

/**
 * A simple Memory Manager.
 *
 * It basically holds two linked lists. One for the free memory blocks,
 * and one for the reserved blocks.
 */
public class MemoryManager {

    public static native int log(int amount);

    @Export("initMemory")
    public static void initNative() {
        initInternal(Address.getMemorySize());
    }

    private static void initInternal(final int aSize) {
        // This is the list of free blocks
        final int heapBase = Address.getHeapBase();
        final int initialFreeBlock = heapBase + 36;

        Address.setIntValue(heapBase, 4, initialFreeBlock);

        // Free memory block
        Address.setIntValue(initialFreeBlock, 0, aSize - 48 - heapBase);
        Address.setIntValue(initialFreeBlock, 4, 0);
        Address.setIntValue(initialFreeBlock, 8, 0);

        // This is the List of reserved blocks
        Address.setIntValue(heapBase, 8, 0);

        // Current work counter for GC resumes
        Address.setIntValue(heapBase, 12, 0);

        // Current counter for GC epochs
        Address.setIntValue(heapBase, 16, 1);
    }

    @Export("GCEpoch")
    public static int getGCEpoch() {

        final int heapBase = Address.getHeapBase();

        return Address.getIntValue(heapBase, 16);
    }

    @Export("freeMem")
    public static int freeMem() {
        int resultSize = 0;

        final int heapBase = Address.getHeapBase();

        int current = Address.getIntValue(heapBase, 4);
        while (current != 0) {
            resultSize += Address.getIntValue(current, 0);
            current = Address.getIntValue(current, 4);
        }
        return resultSize;
    }

    @Export("usedMem")
    public static int usedMem() {
        int resultSize = 0;

        final int heapBase = Address.getHeapBase();

        int current = Address.getIntValue(heapBase, 8);
        while (current != 0) {
            resultSize += Address.getIntValue(current, 0);
            current = Address.getIntValue(current, 4);
        }
        return resultSize;
    }

    private static void internalFree(final int current) {

        final int heapBase = Address.getHeapBase();

        final int currentPrev = Address.getIntValue(current, 12);
        final int currentNext = Address.getIntValue(current, 4);

        if (currentPrev != 0) {
            Address.setIntValue(currentPrev, 4, currentNext);
        } else {
            Address.setIntValue(heapBase, 8, currentNext);
        }

        if (currentNext != 0) {
            Address.setIntValue(currentNext, 12, currentPrev);
        }

        // Prepend to the list of free blocks
        final int freeListHead = Address.getIntValue(heapBase, 4);

        Address.setIntValue(current, 4, freeListHead);
        Address.setIntValue(freeListHead, 12, current);
        Address.setIntValue(current, 12, 0);

        Address.setIntValue(heapBase, 4, current);
    }

    @Export("free")
    public static void free(final int aPointer) {

        final int pointerToBlock = aPointer - 16;

        internalFree(pointerToBlock);
    }

    @Export("malloc")
    public static int malloc(int aSize) {

        // Overhead for header
        aSize+=16;

        final int heapBase = Address.getHeapBase();

        int current = Address.getIntValue(heapBase, 4);
        while (current != 0) {
            final int currentSize = Address.getIntValue(current, 0);
            final int currentPrev = Address.getIntValue(current, 12);
            final int currentNext = Address.getIntValue(current, 4);
            if (currentSize >= aSize) {
                final int remaining = currentSize - aSize;
                if (remaining < 16) {
                    // We have an exact match
                    final int allocatedListHead = Address.getIntValue(heapBase, 8);
                    final int allocated = current;
                    Address.setIntValue(allocated, 0, currentSize);
                    Address.setIntValue(allocated, 4, allocatedListHead);
                    Address.setIntValue(allocated, 8, 1);
                    Address.setIntValue(allocated,12, 0);
                    if (allocatedListHead != 0) {
                        Address.setIntValue(allocatedListHead, 12, allocated);
                    }
                    Address.setIntValue(heapBase, 8, allocated);

                    if (currentPrev != 0) {
                        Address.setIntValue(currentPrev, 4, currentNext);
                    } else {
                        Address.setIntValue(heapBase, 4, currentNext);
                    }

                    if (currentNext != 0) {
                        Address.setIntValue(currentNext,12, currentPrev);
                    }

                    // Wipeout data
                    final int dataStart = allocated + 16;
                    final int fillSize = aSize - 16;
                    if (fillSize == 0) {
                        /// Nothing to do
                    } else if (fillSize == 4) {
                        Address.setIntValue(dataStart, 0, 0);
                    } else if (fillSize == 8) {
                        Address.setIntValue(dataStart, 0, 0);
                        Address.setIntValue(dataStart, 4, 0);
                    } else if (fillSize == 12) {
                        Address.setIntValue(dataStart, 0, 0);
                        Address.setIntValue(dataStart, 4, 0);
                        Address.setIntValue(dataStart, 8, 0);
                    } else if (fillSize == 16) {
                        Address.setIntValue(dataStart, 0, 0);
                        Address.setIntValue(dataStart, 4, 0);
                        Address.setIntValue(dataStart, 8, 0);
                        Address.setIntValue(dataStart, 12, 0);
                    } else if (fillSize == 20) {
                        Address.setIntValue(dataStart, 0, 0);
                        Address.setIntValue(dataStart, 4, 0);
                        Address.setIntValue(dataStart, 8, 0);
                        Address.setIntValue(dataStart, 12, 0);
                        Address.setIntValue(dataStart, 16, 0);
                    } else if (fillSize == 24) {
                        Address.setIntValue(dataStart, 0, 0);
                        Address.setIntValue(dataStart, 4, 0);
                        Address.setIntValue(dataStart, 8, 0);
                        Address.setIntValue(dataStart, 12, 0);
                        Address.setIntValue(dataStart, 16, 0);
                        Address.setIntValue(dataStart, 20, 0);
                    } else if (fillSize == 28) {
                        Address.setIntValue(dataStart, 0, 0);
                        Address.setIntValue(dataStart, 4, 0);
                        Address.setIntValue(dataStart, 8, 0);
                        Address.setIntValue(dataStart, 12, 0);
                        Address.setIntValue(dataStart, 16, 0);
                        Address.setIntValue(dataStart, 20, 0);
                        Address.setIntValue(dataStart, 24, 0);
                    } else {
                        for (int i = 0; i < fillSize; i += 4) {
                            Address.setIntValue(dataStart, i, 0);
                        }
                    }

                    return dataStart;
                }
                if (remaining > 16) {
                    // We can use split this chunk

                    // We create the allocated chunk by appending it to the list of
                    // allocated blocks
                    final int allocatedListHead = Address.getIntValue(heapBase, 8);
                    final int allocated = current;
                    Address.setIntValue(allocated, 0, aSize);
                    Address.setIntValue(allocated, 4, allocatedListHead);
                    Address.setIntValue(allocated, 8, 1);
                    Address.setIntValue(allocated, 12, 0);
                    if (allocatedListHead != 0) {
                        Address.setIntValue(allocatedListHead, 12, allocated);
                    }
                    Address.setIntValue(heapBase, 8, allocated);

                    // Now we split the original block
                    if (currentPrev != 0) {
                        final int newFree = current + aSize;
                        Address.setIntValue(newFree, 0, remaining);
                        Address.setIntValue(newFree, 4, currentNext);
                        Address.setIntValue(newFree, 8, 1);
                        Address.setIntValue(newFree, 12, currentPrev);
                        if (currentPrev != 0) {
                            Address.setIntValue(currentPrev, 4, newFree);
                        }
                        if (currentNext != 0) {
                            Address.setIntValue(currentNext, 12, newFree);
                        }
                    } else {
                        final int newFree = current + aSize;
                        Address.setIntValue(newFree, 0, remaining);
                        Address.setIntValue(newFree, 4, currentNext);
                        Address.setIntValue(newFree, 8, 1);
                        Address.setIntValue(newFree, 12, 0);
                        if (currentNext !=  0) {
                            Address.setIntValue(currentNext, 12, newFree);
                        }
                        Address.setIntValue(heapBase, 4, newFree);
                    }

                    // Wipeout data
                    final int dataStart = allocated + 16;
                    final int fillSize = aSize - 16;
                    if (fillSize == 0) {
                        /// Nothing to do
                    } else if (fillSize == 4) {
                        Address.setIntValue(dataStart, 0, 0);
                    } else if (fillSize == 8) {
                        Address.setIntValue(dataStart, 0, 0);
                        Address.setIntValue(dataStart, 4, 0);
                    } else if (fillSize == 12) {
                        Address.setIntValue(dataStart, 0, 0);
                        Address.setIntValue(dataStart, 4, 0);
                        Address.setIntValue(dataStart, 8, 0);
                    } else if (fillSize == 16) {
                        Address.setIntValue(dataStart, 0, 0);
                        Address.setIntValue(dataStart, 4, 0);
                        Address.setIntValue(dataStart, 8, 0);
                        Address.setIntValue(dataStart, 12, 0);
                    } else if (fillSize == 20) {
                        Address.setIntValue(dataStart, 0, 0);
                        Address.setIntValue(dataStart, 4, 0);
                        Address.setIntValue(dataStart, 8, 0);
                        Address.setIntValue(dataStart, 12, 0);
                        Address.setIntValue(dataStart, 16, 0);
                    } else if (fillSize == 24) {
                        Address.setIntValue(dataStart, 0, 0);
                        Address.setIntValue(dataStart, 4, 0);
                        Address.setIntValue(dataStart, 8, 0);
                        Address.setIntValue(dataStart, 12, 0);
                        Address.setIntValue(dataStart, 16, 0);
                        Address.setIntValue(dataStart, 20, 0);
                    } else if (fillSize == 28) {
                        Address.setIntValue(dataStart, 0, 0);
                        Address.setIntValue(dataStart, 4, 0);
                        Address.setIntValue(dataStart, 8, 0);
                        Address.setIntValue(dataStart, 12, 0);
                        Address.setIntValue(dataStart, 16, 0);
                        Address.setIntValue(dataStart, 20, 0);
                        Address.setIntValue(dataStart, 24, 0);
                    } else {
                        for (int i = 0; i < fillSize; i += 4) {
                            Address.setIntValue(dataStart, i, 0);
                        }
                    }
                    return dataStart;
                }
            }
            current = Address.getIntValue(current, 4);
        }
        Address.unreachable();
        return -1;
    }

    @Export("newObject")
    public static int newObject(final int aSize, final int aType, final int aVTableIndex) {

        final int allocated = malloc(aSize);
        Address.setIntValue(allocated, 0, aType);
        Address.setIntValue(allocated, 4, aVTableIndex);

        return allocated;
    }

    public static void initStackObject(final int aPtr, final int aSize, final int aType, final int aVTableIndex) {
        // Wipeout data
        for (int i=0;i<aSize;i+=4) {
            Address.setIntValue(aPtr, i, 0);
        }

        Address.setIntValue(aPtr, 0, aType);
        Address.setIntValue(aPtr, 4, aVTableIndex);
    }

    public static void initStackArray(final int aPtr, final int aSize, final int aType, final int aVTableIndex) {
        initStackObject(aPtr, 16 + 4 + 8 * aSize, aType, aVTableIndex);
        Address.setIntValue(aPtr, 16, aSize);
    }

    public static boolean isUsedByStaticData(final int aOwningBlock) {
        final int aPtrToObject = aOwningBlock + 16;
        return isUsedByStaticDataUserSpace(aPtrToObject);
    }

    public static boolean isUsedByStaticDataUserSpace(final int aPtrToObject) {
        int dataStart = 0;
        final int theDataEnd = Address.getDataEnd();

        while(dataStart <= theDataEnd) {
            if (Address.getIntValue(dataStart, 0) == aPtrToObject) {
                return true;
            }
            dataStart += 4;
        }

        return false;
    }

    public static boolean isUsedByStack(final int aOwningBlock) {
        final int objectPtr = aOwningBlock + 16;
        return isUsedByStackUserSpace(objectPtr);
    }

    public static boolean isUsedByStackUserSpace(final int aPtrToObject) {
        if (Address.systemHasStack() == 0) {
            return false;
        }

        int stackStart = Address.getStackTop();
        final int memorySize = Address.getMemorySize();
        while(stackStart + 4 < memorySize) {
            if (Address.getIntValue(stackStart, 0) == aPtrToObject) {
                return true;
            }
            stackStart += 4;
        }

        return false;
    }

    public static boolean isUsedByHeap(final int aAllocationPtr) {
        return isUsedByHeapUserSpace(aAllocationPtr + 16);
    }

    public static boolean isUsedByHeapUserSpace(final int aPtrToObject) {
        final int allocationPtr = aPtrToObject - 16;
        final int heapBase = Address.getHeapBase();

        int current = Address.getIntValue(heapBase, 8);
        while(current != 0) {
            final int next = Address.getIntValue(current, 4);

            // Ignore self reference
            if (allocationPtr != current) {
                final int theSize = Address.getIntValue(current, 0);
                for (int i = 16; i < theSize; i += 4) {
                    if (Address.getIntValue(current, i) == aPtrToObject) {
                        return true;
                    }
                }
            }
            current = next;
        }

        return false;
    }

    @Export("isUsedAsCallback")
    public static native boolean isUsedAsCallback(final int aPtr);

    @Export("GC")
    public static int GC() {
        return IncrementalGC(Integer.MAX_VALUE);
    }

    @Export("IncrementalGC")
    public static int IncrementalGC(final int blockLimit) {

        final int heapBase = Address.getHeapBase();

        // Try to check if we can continue from a previous run
        int current = Address.getIntValue(heapBase, 12);
        if (current == 0) {
            // No, we start at the beginning of the allocation list
            current = Address.getIntValue(heapBase, 8);
        }

        // We have to remember the current GC epoch
        final int currentEpoch = Address.getIntValue(heapBase, 16);

        int freeCounter = 0;
        int stepCounter = 0;
        while(current != 0) {
            final int next = Address.getIntValue(current, 4);
            final int survivorCount = Address.getIntValue(current, 8);
            if (currentEpoch % survivorCount == 0) {
                if (!isUsedByHeap(current) && !isUsedByStack(current)  && !(isUsedByStaticData(current)) && (!isUsedAsCallback(current + 16))) {
                    internalFree(current);
                    freeCounter++;
                } else {
                    // Increment the survivor count of the allocation block
                    if (survivorCount < 32) {
                        Address.setIntValue(current, 8, survivorCount * 2);
                    }
                }
                if (stepCounter++ >= blockLimit) {
                    // We have reached the limit for the current run
                    // We save the next block to proceed and exit here
                    Address.setIntValue(heapBase, 12, next);

                    return stepCounter;
                }
            }

            current = next;
        }
        // Increment epoch
        Address.setIntValue(heapBase, 16, currentEpoch + 1);

        // The next run starts at the beginning
        Address.setIntValue(heapBase, 12, 0);

        return freeCounter;
    }

    @Export("newArrayINTINTINT")
    public static int newArray(final int aSize, final int aType, final int aVTableIndex) {

        // Arrays are normal objects. Their data are a length field plus n * data
        final int newArray = newObject(16 + 4 + 8 * aSize, aType, aVTableIndex);

        Address.setIntValue(newArray, 16, aSize);
        return newArray;
    }

    @Export("newArrayINTINTINTINT")
    public static int newArray(final int aSize1, final int aSize2, final int aType, final int aVTableIndex) {
        final int newArray = newArray(aSize1, aType, aVTableIndex);
        for (int i=0;i<aSize1;i++) {
            final int theOffset = 16 + 4 + 8 * i;
            final int theSubArray = newArray(aSize2, aType, aVTableIndex);
            Address.setIntValue(newArray, theOffset, theSubArray);
        }
        return newArray;
    }

    @Export("logAllocations")
    public static void logAllocations() {
        final int heapBase = Address.getHeapBase();

        int current = Address.getIntValue(heapBase, 8);
        while (current != 0) {
            final int prev = Address.getIntValue(current, 12);
            final int next = Address.getIntValue(current, 4);

            logAllocationDetails(current, prev, next);

            current = next;
        }
    }

    public static native void logAllocationDetails(final int start, final int prev, final int next);

    public static int indexInAllocationList(final int aObjectPtr) {
        final int theAllocation = aObjectPtr - 16;
        final int heapBase = Address.getHeapBase();

        int current = Address.getIntValue(heapBase, 8);
        int index = 0;
        while (current != 0) {
            if (current == theAllocation) {
                return index;
            }
            index++;
            current = Address.getIntValue(current, 4);
        }
        return -1;
    }

    public static int indexInFreeList(final int aObjectPtr) {
        final int theAllocation = aObjectPtr - 16;
        final int heapBase = Address.getHeapBase();

        int current = Address.getIntValue(heapBase, 4);
        int index = 0;
        while (current != 0) {
            if (current == theAllocation) {
                return index;
            }
            index++;
            current = Address.getIntValue(current, 4);
        }
        return -1;
    }

    public static void printObjectDebug(final Object o) {
        final int ptr = Address.ptrOf(o);
        final int indexAllocation = indexInAllocationList(ptr);
        final int indexFree = indexInFreeList(ptr);
        final boolean usedByStack = isUsedByStackUserSpace(ptr);
        final boolean usedByHeap = isUsedByHeapUserSpace(ptr);
        printObjectDebugInternal(o, indexAllocation,
                indexFree,
                usedByStack,
                usedByHeap);
    }

    public static native void printObjectDebugInternal(final Object o, int indexAlloc, int indexFree,
                                                       boolean usedByStack, boolean usedByHeap);
}