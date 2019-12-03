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

    @Export("initMemory")
    public static void initNative() {
        initInternal(Address.getMemorySize());
    }

    private static void initInternal(final int aSize) {
        // This is the list of free blocks
        Address.setIntValue(4, 0, 36);

        // Free memory block
        Address.setIntValue(36, 0, aSize - 12);
        Address.setIntValue(36, 4, 0);
        Address.setIntValue(36, 8, 0);

        // This is the List of reserved blocks
        Address.setIntValue(8, 0, 0);

        // Current work counter for GC resumes
        Address.setIntValue(12, 0, 0);

        // Current counter for GC epochs
        Address.setIntValue(16, 0, 1);
    }

    @Export("GCEpoch")
    public static int getGCEpoch() {
        return Address.getIntValue(16, 0);
    }

    @Export("freeMem")
    public static long freeMem() {
        long theResult = 0;

        final int theFreeStartPtr = Address.getIntValue(4, 0);

        int theCurrent = theFreeStartPtr;
        while (theCurrent != 0) {
            theResult += Address.getIntValue(theCurrent, 0);
            theCurrent = Address.getIntValue(theCurrent, 4);
        }
        return theResult;
    }

    @Export("usedMem")
    public static long usedMem() {
        long theResult = 0;

        int theCurrent = Address.getIntValue(8, 0);
        while (theCurrent != 0) {
            theResult += Address.getIntValue(theCurrent, 0);
            theCurrent = Address.getIntValue(theCurrent, 4);
        }
        return theResult;
    }

    private static void internalFree(final int aPointer) {

        // Remove the block from the list of allocated blocks
        int theCurrent = Address.getIntValue(8, 0);

        int thePrevious = 0;
        while(theCurrent != 0) {

            final int theNext = Address.getIntValue(theCurrent, 4);

            if (theCurrent == aPointer) {
                // This is the block
                // Remove it from the list of allocated blocks
                if (thePrevious == 0) {
                    Address.setIntValue(8, 0, theNext);
                } else {
                    Address.setIntValue(thePrevious, 4, theNext);
                }

                // Ok, now we prepend it to the list of free blocks
                final int theFreeStartPtr = Address.getIntValue(4, 0);

                Address.setIntValue(theCurrent, 4, theFreeStartPtr);
                Address.setIntValue(4, 0, theCurrent);
                return;
            }

            thePrevious = theCurrent;
            theCurrent = theNext;
        }
    }

    public static native void logExceptionText(String aMessage);

    public static void logException(final Exception e) {
        logExceptionText(e.getMessage());
    }

    @Export("free")
    public static void free(final int aPointer) {

        int theStart = aPointer;
        theStart-=12;

        internalFree(theStart);
    }

    @Export("malloc")
    public static int malloc(int aSize) {

        // Overhead for header
        aSize+=12;

        final int theFreeStartPtr = Address.getIntValue(4, 0);

        // We search the free list for a suitable sized block
        int thePrevious = 0;
        int theCurrent = theFreeStartPtr;
        while(theCurrent != 0) {
            final int theSize = Address.getIntValue(theCurrent, 0);
            final int theNext = Address.getIntValue(theCurrent, 4);
            if (theSize >= aSize) {
                final int theRemaining = theSize - aSize;

                if (theRemaining > 12) {
                    Address.setIntValue(theCurrent, 0, aSize);

                    // Block can be safely split
                    final int theNewFreeStart = theCurrent + aSize;
                    final int theNewFreeSize = theSize - aSize;
                    Address.setIntValue(theNewFreeStart, 0, theNewFreeSize);
                    Address.setIntValue(theNewFreeStart, 4, theNext);

                    if (thePrevious == 0) {
                        Address.setIntValue(4, 0, theNewFreeStart);
                    } else {
                        Address.setIntValue(thePrevious, 4, theNewFreeStart);
                    }
                } else {
                    // Remaining size would be too small, be have to completely occupy it
                    Address.setIntValue(theCurrent, 0, theSize);

                    if (thePrevious == 0) {
                        Address.setIntValue(4, 0, theNext);
                    } else {
                        Address.setIntValue(thePrevious, 4, theNext);
                    }
                }

                // Add the current block to the allocated block ist by prepending it to the list
                final int theReservedListPtr = Address.getIntValue(8, 0);

                Address.setIntValue(theCurrent, 4, theReservedListPtr);
                Address.setIntValue(8, 0, theCurrent);

                // Reset survivor count of the block
                Address.setIntValue(theCurrent, 8, 1);

                // Wipeout data
                final int theDataStart = theCurrent + 12;

                for (int i=0;i<aSize-12;i+=4) {
                    Address.setIntValue(theDataStart, i, 0);
                }

                return theDataStart;
            }

            thePrevious = theCurrent;
            theCurrent = theNext;
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

    public static boolean isUsedByStack(final int aOwningBlock) {
        final int theOwningData = aOwningBlock + 12;
        return isUsedByStackUserSpace(theOwningData);
    }

    public static boolean isUsedByStackUserSpace(final int aPtrToObject) {
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
        return isUsedByHeapUserSpace(aAllocationPtr + 12);
    }

    public static boolean isUsedByHeapUserSpace(final int aPtrToObject) {
        final int theAllocationStart = aPtrToObject - 12;
        int theCurrent = Address.getIntValue(8, 0);
        while(theCurrent != 0) {
            // Ignore self reference
            if (theAllocationStart != theCurrent) {
                final int theSize = Address.getIntValue(theCurrent, 0);
                for (int i = 12; i < theSize; i += 4) {
                    if (Address.getIntValue(theCurrent, i) == aPtrToObject) {
                        return true;
                    }
                }
            }
            theCurrent = Address.getIntValue(theCurrent, 4);
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
        // Try to check if we can continue from a previous run
        int theCurrent = Address.getIntValue(12, 0);
        if (theCurrent == 0) {
            // No, we start at the beginning of the allocation list
            theCurrent = Address.getIntValue(8, 0);
        }

        // We have to remember the current GC epoch
        final int currentEpoch = Address.getIntValue(16, 0);

        int freeCounter = 0;
        int stepCounter = 0;
        while(theCurrent != 0) {
            final int theNext = Address.getIntValue(theCurrent, 4);
            final int theSurvivorCount = Address.getIntValue(theCurrent, 8);
            if (currentEpoch % theSurvivorCount == 0) {
                if (!isUsedByHeap(theCurrent) && !isUsedByStack(theCurrent) && (!isUsedAsCallback(theCurrent + 12))) {
                    internalFree(theCurrent);
                    freeCounter++;
                } else {
                    // Increment the survivor count of the allocation block
                    if (theSurvivorCount < 32) {
                        Address.setIntValue(theCurrent, 8, theSurvivorCount * 2);
                    }
                }
                if (stepCounter++ >= blockLimit) {
                    // We have reached the limit for the current run
                    // We save the next block to proceed and exit here
                    Address.setIntValue(12, 0, theNext);
                    return stepCounter;
                }
            }

            theCurrent = theNext;
        }
        // Increment epoch
        Address.setIntValue(16, 0, currentEpoch + 1);

        // The next run starts at the beginning
        Address.setIntValue(12, 0, 0);

        return freeCounter;
    }

    public static int newArray(final int aSize, final int aType, final int aVTableIndex) {

        // Arrays are normal objects. Their data are a length field plus n * data
        final int theObject = newObject(16 + 4 + 4 * aSize, aType, aVTableIndex);

        Address.setIntValue(theObject, 16, aSize);
        return theObject;
    }

    public static int newArray(final int aSize1, final int aSize2, final int aType, final int aVTableIndex) {
        final int theResult = newArray(aSize1, aType, aVTableIndex);
        for (int i=0;i<aSize1;i++) {
            final int theOffset = 16 + 4 + 4 * i;
            final int theSubArray = newArray(aSize2, aType, aVTableIndex);
            Address.setIntValue(theResult, theOffset, theSubArray);
        }
        return theResult;
    }

    public static int indexInAllocationList(final int aObjectPtr) {
        final int theAllocation = aObjectPtr - 12;

        final int theFreeStartPtr = Address.getIntValue(8, 0);

        int theCurrent = theFreeStartPtr;
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

    public static int indexInFreeList(final int aObjectPtr) {
        final int theAllocation = aObjectPtr - 12;

        final int theFreeStartPtr = Address.getIntValue(4, 0);

        int theCurrent = theFreeStartPtr;
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