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
        int theResult = 0;

        final int heapBase = Address.getHeapBase();

        int current = Address.getIntValue(heapBase, 4);
        while (current != 0) {
            theResult += Address.getIntValue(current, 0);
            current = Address.getIntValue(current, 4);
        }
        return theResult;
    }

    @Export("usedMem")
    public static int usedMem() {
        int theResult = 0;

        final int heapBase = Address.getHeapBase();

        int current = Address.getIntValue(heapBase, 8);
        while (current != 0) {
            theResult += Address.getIntValue(current, 0);
            current = Address.getIntValue(current, 4);
        }
        return theResult;
    }

    private static void internalFree(final int aPointer) {

        final int heapBase = Address.getHeapBase();

        // Remove the block from the list of allocated blocks
        int current = Address.getIntValue(heapBase, 8);

        int thePrevious = 0;
        while(current != 0) {

            final int theNext = Address.getIntValue(current, 4);

            if (current == aPointer) {
                // This is the block
                // Remove it from the list of allocated blocks
                if (thePrevious == 0) {
                    Address.setIntValue(heapBase, 8, theNext);
                } else {
                    Address.setIntValue(thePrevious, 4, theNext);
                }

                // Ok, now we prepend it to the list of free blocks
                final int theFreeStartPtr = Address.getIntValue(heapBase, 4);
                Address.setIntValue(current, 4, theFreeStartPtr);

                Address.setIntValue(heapBase, 4, current);
                return;
            }

            thePrevious = current;
            current = theNext;
        }
    }

    @Export("free")
    public static void free(final int aPointer) {

        int theStart = aPointer;
        theStart-=16;

        internalFree(theStart);
    }

    @Export("malloc")
    public static int malloc(int aSize) {

        // Overhead for header
        aSize+=16;

        final int heapBase = Address.getHeapBase();

        // We search the free list for a suitable sized block
        int previous = 0;
        int current = Address.getIntValue(heapBase, 4);
        while(current != 0) {
            final int theSize = Address.getIntValue(current, 0);
            final int theNext = Address.getIntValue(current, 4);
            if (theSize >= aSize) {
                final int theRemaining = theSize - aSize;

                if (theRemaining > 16) {
                    Address.setIntValue(current, 0, aSize);

                    // Block can be safely split
                    final int theNewFreeStart = current + aSize;
                    final int theNewFreeSize = theSize - aSize;
                    Address.setIntValue(theNewFreeStart, 0, theNewFreeSize);
                    Address.setIntValue(theNewFreeStart, 4, theNext);

                    //Address.setIntValue(theNext, 12, theNewFreeStart);

                    if (previous == 0) {
                        Address.setIntValue(heapBase, 4, theNewFreeStart);
                        //Address.setIntValue(theNewFreeStart, 12, 0);
                    } else {
                        Address.setIntValue(previous, 4, theNewFreeStart);
                        //Address.setIntValue(theNewFreeStart, 12, previous);
                    }
                } else {
                    // Remaining size would be too small, be have to completely occupy it
                    Address.setIntValue(current, 0, theSize);

                    if (previous == 0) {
                        Address.setIntValue(heapBase, 4, theNext);
                        //Address.setIntValue(theNext, 12, 0);
                    } else {
                        Address.setIntValue(previous, 4, theNext);
                        //Address.setIntValue(theNext, 12, previous);
                    }
                }

                // Add the current block to the allocated block ist by prepending it to the list
                final int theReservedListPtr = Address.getIntValue(heapBase, 8);

                Address.setIntValue(current, 4, theReservedListPtr);
                //Address.setIntValue(theReservedListPtr, 12, current);

                Address.setIntValue(heapBase, 8, current);
                //Address.setIntValue(current, 12, 0);

                // Reset survivor count of the block
                Address.setIntValue(current, 8, 1);

                // Wipeout data
                final int theDataStart = current + 16;

                for (int i=0;i<aSize-16;i+=4) {
                    Address.setIntValue(theDataStart, i, 0);
                }

                return theDataStart;
            }

            previous = current;
            current = theNext;
        }
        Address.unreachable();
        return 0;
    }

    @Export("newObject")
    public static int newObject(final int aSize, final int aType, final int aVTableIndex) {

        final int theAddress = malloc(aSize);
        Address.setIntValue(theAddress, 0, aType);
        Address.setIntValue(theAddress, 4, aVTableIndex);

        return theAddress;
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
        int theDataStart = 0;
        final int theDataEnd = Address.getDataEnd();
        while(theDataStart <= theDataEnd) {
            if (Address.getIntValue(theDataStart, 0) == aPtrToObject) {
                return true;
            }
            theDataStart += 4;
        }
        return false;
    }

    public static boolean isUsedByStack(final int aOwningBlock) {
        final int theOwningData = aOwningBlock + 16;
        return isUsedByStackUserSpace(theOwningData);
    }

    public static boolean isUsedByStackUserSpace(final int aPtrToObject) {
        if (Address.systemHasStack() == 0) {
            return false;
        }
        int theStackStart = Address.getStackTop();
        final int theMemorySize = Address.getMemorySize();
        while(theStackStart + 4 < theMemorySize) {
            if (Address.getIntValue(theStackStart, 0) == aPtrToObject) {
                return true;
            }
            theStackStart += 4;
        }

        return false;
    }

    public static boolean isUsedByHeap(final int aAllocationPtr) {
        return isUsedByHeapUserSpace(aAllocationPtr + 16);
    }

    public static boolean isUsedByHeapUserSpace(final int aPtrToObject) {
        final int theAllocationStart = aPtrToObject - 16;
        final int heapBase = Address.getHeapBase();

        int current = Address.getIntValue(heapBase, 8);
        while(current != 0) {
            // Ignore self reference
            if (theAllocationStart != current) {
                final int theSize = Address.getIntValue(current, 0);
                for (int i = 16; i < theSize; i += 4) {
                    if (Address.getIntValue(current, i) == aPtrToObject) {
                        return true;
                    }
                }
            }
            current = Address.getIntValue(current, 4);
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
            final int theNext = Address.getIntValue(current, 4);
            final int theSurvivorCount = Address.getIntValue(current, 8);
            if (currentEpoch % theSurvivorCount == 0) {
                if (!isUsedByHeap(current) && !isUsedByStack(current)  && !(isUsedByStaticData(current)) && (!isUsedAsCallback(current + 12))) {
                    internalFree(current);
                    freeCounter++;
                } else {
                    // Increment the survivor count of the allocation block
                    if (theSurvivorCount < 32) {
                        Address.setIntValue(current, 8, theSurvivorCount * 2);
                    }
                }
                if (stepCounter++ >= blockLimit) {
                    // We have reached the limit for the current run
                    // We save the next block to proceed and exit here
                    Address.setIntValue(heapBase, 12, theNext);
                    return stepCounter;
                }
            }

            current = theNext;
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
        final int theObject = newObject(16 + 4 + 8 * aSize, aType, aVTableIndex);

        Address.setIntValue(theObject, 16, aSize);
        return theObject;
    }

    @Export("newArrayINTINTINTINT")
    public static int newArray(final int aSize1, final int aSize2, final int aType, final int aVTableIndex) {
        final int theResult = newArray(aSize1, aType, aVTableIndex);
        for (int i=0;i<aSize1;i++) {
            final int theOffset = 16 + 4 + 8 * i;
            final int theSubArray = newArray(aSize2, aType, aVTableIndex);
            Address.setIntValue(theResult, theOffset, theSubArray);
        }
        return theResult;
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

        int theCurrent = Address.getIntValue(heapBase, 4);
        int index = 0;
        while (theCurrent != 0) {
            if (theCurrent == theAllocation) {
                return index;
            }
            index++;
            theCurrent = Address.getIntValue(theCurrent, 4);
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