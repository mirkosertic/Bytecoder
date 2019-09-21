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
import de.mirkosertic.bytecoder.api.Import;
import de.mirkosertic.bytecoder.classlib.java.lang.TString;

/**
 * A simple Memory Manager.
 *
 * It basically holds zwo linked lists. One for the free memory blocks,
 * and one for the reserved blocks.
 */
public class MemoryManager {

    @Export("initMemory")
    public static void initNative() {
        initInternal(Address.getMemorySize());
    }

    @Import(module = "profiler", name = "logMemoryLayoutBlock")
    public static native void logMemoryLayoutBlock(int aStart, int aUsed, int aNext);

    private static void initInternal(final int aSize) {
        // This is the list of free blocks
        Address.setIntValue(4, 0, 28);

        // Free memory block
        Address.setIntValue(28, 0, aSize);
        Address.setIntValue(28, 4, 0);

        // This is the List of reserved blocks
        Address.setIntValue(8, 0, 0);
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

        final int theUsedStartPtr = Address.getIntValue(8, 0);

        int theCurrent = theUsedStartPtr;
        while (theCurrent != 0) {
            theResult += Address.getIntValue(theCurrent, 0);
            theCurrent = Address.getIntValue(theCurrent, 4);
        }
        return theResult;
    }

    @Export("logMemoryLayout")
    public static void logMemoryLayout() {

        final int theFreeStart = 4;
        final int theFreeStartPtr = Address.getIntValue(theFreeStart, 0);

        int theCurrent = theFreeStartPtr;
        while (theCurrent != 0) {
            final int theNext = Address.getIntValue(theCurrent, 4);
            final int theStart = theCurrent;

            logMemoryLayoutBlock(theStart, 0, theNext);

            theCurrent = theNext;
        }

        final int theUsedStart = 8;
        final int theUsedStartPtr = Address.getIntValue(theUsedStart, 0);

        theCurrent = theUsedStartPtr;
        while (theCurrent != 0) {
            final int theStart = theCurrent;
            final int theNext = Address.getIntValue(theCurrent, 4);

            logMemoryLayoutBlock(theStart, 1, theNext);

            theCurrent = theNext;
        }
   }

    private static void internalFree(final int aPointer) {

        final int theStart = aPointer;

        // Remove the block from the list of allocated blocks
        final int theAllocatedStart = 8;
        final int theAllocatedStartPtr = Address.getIntValue(theAllocatedStart, 0);

        int theCurrent = theAllocatedStartPtr;
        int thePrevious = 0;
        while(theCurrent != 0) {

            final int theCurrentStart = theCurrent;
            final int theNext = Address.getIntValue(theCurrent, 4);

            if (theCurrentStart == theStart) {
                // This is the block
                // Remove it from the list of allocated blocks
                if (thePrevious == 0) {
                    Address.setIntValue(theAllocatedStart, 0, theNext);
                } else {
                    final int thePrevPtr = thePrevious;
                    Address.setIntValue(thePrevPtr, 4, theNext);
                }

                // Ok, now we prepend it to the list of free blocks
                final int theFreeStart = 4;
                final int theFreeStartPtr = Address.getIntValue(theFreeStart, 0);

                Address.setIntValue(theCurrent, 4, theFreeStartPtr);
                Address.setIntValue(theFreeStart, 0, theCurrentStart);
                return;
            }

            thePrevious = theCurrentStart;
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
        theStart-=8;

        internalFree(theStart);
    }

    @Export("malloc")
    public static int malloc(int aSize) {

        // Overhead for header
        aSize+=8;

        final int theFreeStart = 4;
        final int theFreeStartPtr = Address.getIntValue(theFreeStart, 0);

        // We search the free list for a suitable sized block
        int thePrevious = 0;
        int theCurrent = theFreeStartPtr;
        while(theCurrent != 0) {
            final int theSize = Address.getIntValue(theCurrent, 0);
            final int theNext = Address.getIntValue(theCurrent, 4);
            if (theSize >= aSize) {
                final int theRemaining = theSize - aSize;


                if (theRemaining > 8) {
                    Address.setIntValue(theCurrent, 0, aSize);

                    // Block can be safely split
                    final int theNewFreeStart = theCurrent + aSize;
                    final int theNewFreeSize = theSize - aSize;
                    final int theNewFree = theNewFreeStart;
                    Address.setIntValue(theNewFree, 0, theNewFreeSize);
                    Address.setIntValue(theNewFree, 4, theNext);

                    if (thePrevious == 0) {
                        Address.setIntValue(theFreeStart, 0, theNewFreeStart);
                    } else {
                        Address.setIntValue(thePrevious, 4, theNewFreeStart);
                    }
                } else {
                    // Remaining size would be too small, be have to completely occupy it
                    Address.setIntValue(theCurrent, 0, theSize);

                    if (thePrevious == 0) {
                        Address.setIntValue(theFreeStart, 0, theNext);
                    } else {
                        Address.setIntValue(thePrevious, 4, theNext);
                    }
                }

                // Add the current block to the allocated block ist by prepending it to the list
                final int theReservedListStart = 8;

                final int theReservedListPtr = Address.getIntValue(theReservedListStart, 0);
                final int theCurrentStart = theCurrent;

                Address.setIntValue(theCurrent, 4, theReservedListPtr);
                Address.setIntValue(theReservedListStart, 0, theCurrentStart);

                // Wipeout data
                final int theDataStart = theCurrentStart + 8;
                final int theNewData = theDataStart;

                for (int i=0;i<aSize-16;i+=4) {
                    Address.setIntValue(theNewData, i, 0);
                }

                return theNewData;
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

    private static boolean isUsed(final int aOwningBlock) {

        final int theOwningStart = aOwningBlock;
        final int theOwningData = theOwningStart + 8;

        // First of all we check the stack
        int theStackStart = Address.getStackTop();
        final int theStackTop = Address.getMemorySize();
        while(theStackStart < theStackTop) {
            final int theCurrent = theStackStart;
            final int theReference = Address.getIntValue(theCurrent, 0);
            if (theReference == theOwningData) {
                return true;
            }
            theStackStart += 4;
        }

        // Nothing on the stack, we check the allocated memory blovks
        final int theAllocatedStart= 8;
        final int theAllocatedStartPtr = Address.getIntValue(theAllocatedStart, 0);

        int theCurrent = theAllocatedStartPtr;
        while(theCurrent != 0) {

            final int theCurrentStart = theCurrent;
            if (theOwningStart != theCurrentStart) {
                final int theSize = Address.getIntValue(theCurrent, 0) - 8;
                int thePosition = 8;
                while(thePosition < theSize) {
                    final int theReference = Address.getIntValue(theCurrent, thePosition);
                    if (theReference == theOwningData) {
                        return true;
                    }
                    thePosition += 4;
                }
            }

            final int theNext = Address.getIntValue(theCurrent, 4);
            theCurrent = theNext;
        }

        return false;
    }

    @Export("GC")
    public static void GC() {
        final int theUsedStart = 8;

        final int theUsedStartPtr = Address.getIntValue(theUsedStart, 0);
        int theCurrent = theUsedStartPtr;
        while(theCurrent != 0) {

            final int theNext = Address.getIntValue(theCurrent, 4);

            if (!isUsed(theCurrent)) {
                internalFree(theCurrent);
            }

            theCurrent = theNext;
        }
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

    @Export("newString")
    public static TString newString(final char[] aData) {
        return new TString(aData);
    }

    @Export("newCharArray")
    public static char[] newCharArray(final int length) {
        return new char[length];
    }

    @Export("setCharArrayEntry")
    public static void setCharArrayEntry(final char[] aArray, final int aIndex, final char aValue) {
        aArray[aIndex] = aValue;
    }
}