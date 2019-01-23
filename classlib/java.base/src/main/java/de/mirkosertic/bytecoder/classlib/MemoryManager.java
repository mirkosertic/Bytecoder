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

    public static Object[] data;

    public static void initTestMemory(final int aSize) {
        data = new Object[aSize];
    }

    public static void initWithSize(final int aSize) {
        initTestMemory(aSize);
        initInternal(aSize);
    }

    @Export("initMemory")
    public static void initNative() {
        initInternal(Address.getMemorySize());
    }

    @Import(module = "profiler", name = "logMemoryLayoutBlock")
    public static native void logMemoryLayoutBlock(int aStart, int aUsed, int aNext);

    private static void initInternal(final int aSize) {
        // This is the list of free blocks
        final Address theFreeStart = new Address(4); // At the beginning, we have one giant free block
        Address.setIntValue(theFreeStart, 0, 28);

        // Free memory block
        final Address theFree = new Address(28);
        Address.setIntValue(theFree, 0, aSize);
        Address.setIntValue(theFree, 4, 0);

        // This is the List of reserved blocks
        final Address theReserved = new Address(8);
        Address.setIntValue(theReserved, 0, 0);
    }

    @Export("freeMem")
    public static long freeMem() {
        long theResult = 0;

        final Address theFreeStart = new Address(4);
        final int theFreeStartPtr = Address.getIntValue(theFreeStart, 0);

        Address theCurrent = new Address(theFreeStartPtr);
        while (Address.getStart(theCurrent) != 0) {
            final int theSize = Address.getIntValue(theCurrent, 0);
            theResult += theSize;
            final int theNext = Address.getIntValue(theCurrent, 4);
            theCurrent = new Address(theNext);
        }
        return theResult;
    }

    @Export("usedMem")
    public static long usedMem() {
        long theResult = 0;

        final Address theUsedStart = new Address(8);
        final int theUsedStartPtr = Address.getIntValue(theUsedStart, 0);

        Address theCurrent = new Address(theUsedStartPtr);
        while (Address.getStart(theCurrent) != 0) {
            final int theSize = Address.getIntValue(theCurrent, 0);
            theResult += theSize;
            final int theNext = Address.getIntValue(theCurrent, 4);
            theCurrent = new Address(theNext);
        }
        return theResult;
    }

    @Export("logMemoryLayout")
    public static void logMemoryLayout() {

        final Address theFreeStart = new Address(4);
        final int theFreeStartPtr = Address.getIntValue(theFreeStart, 0);

        Address theCurrent = new Address(theFreeStartPtr);
        while (Address.getStart(theCurrent) != 0) {
            final int theNext = Address.getIntValue(theCurrent, 4);
            final int theStart = Address.getStart(theCurrent);

            logMemoryLayoutBlock(theStart, 0, theNext);

            theCurrent = new Address(theNext);
        }

        final Address theUsedStart = new Address(8);
        final int theUsedStartPtr = Address.getIntValue(theUsedStart, 0);

        theCurrent = new Address(theUsedStartPtr);
        while (Address.getStart(theCurrent) != 0) {
            final int theStart = Address.getStart(theCurrent);
            final int theNext = Address.getIntValue(theCurrent, 4);

            logMemoryLayoutBlock(theStart, 1, theNext);

            theCurrent = new Address(theNext);
        }
   }

    private static void internalFree(final Address aPointer) {

        final int theStart = Address.getStart(aPointer);

        // Remove the block from the list of allocated blocks
        final Address theAllocatedStart = new Address(8);
        final int theAllocatedStartPtr = Address.getIntValue(theAllocatedStart, 0);

        Address theCurrent = new Address(theAllocatedStartPtr);
        int thePrevious = 0;
        while(Address.getStart(theCurrent) != 0) {

            final int theCurrentStart = Address.getStart(theCurrent);
            final int theNext = Address.getIntValue(theCurrent, 4);

            if (theCurrentStart == theStart) {
                // This is the block
                // Remove it from the list of allocated blocks
                if (thePrevious == 0) {
                    Address.setIntValue(theAllocatedStart, 0, theNext);
                } else {
                    final Address thePrevPtr = new Address(thePrevious);
                    Address.setIntValue(thePrevPtr, 4, theNext);
                }

                // Ok, now we prepend it to the list of free blocks
                final Address theFreeStart = new Address(4);
                final int theFreeStartPtr = Address.getIntValue(theFreeStart, 0);

                Address.setIntValue(theCurrent, 4, theFreeStartPtr);
                Address.setIntValue(theFreeStart, 0, theCurrentStart);
                return;
            }

            thePrevious = theCurrentStart;
            theCurrent = new Address(theNext);
        }
    }

    public static native void logExceptionText(String aMessage);

    public static void logException(final Exception e) throws Exception {
        logExceptionText(e.getMessage());
    }

    @Export("free")
    public static void free(final Address aPointer) {

        int theStart = Address.getStart(aPointer);
        theStart-=8;

        internalFree(new Address(theStart));
    }

    @Export("malloc")
    public static Address malloc(int aSize) {

        // Overhead for header
        aSize+=8;

        final Address theFreeStart = new Address(4);
        final int theFreeStartPtr = Address.getIntValue(theFreeStart, 0);

        // We search the free list for a suitable sized block
        int thePrevious = 0;
        Address theCurrent = new Address(theFreeStartPtr);
        while(Address.getStart(theCurrent) != 0) {
            final int theSize = Address.getIntValue(theCurrent, 0);
            final int theNext = Address.getIntValue(theCurrent, 4);
            if (theSize >= aSize) {
                final int theRemaining = theSize - aSize;


                if (theRemaining > 8) {
                    Address.setIntValue(theCurrent, 0, aSize);

                    // Block can be safely split
                    final int theNewFreeStart = Address.getStart(theCurrent) + aSize;
                    final int theNewFreeSize = theSize - aSize;
                    final Address theNewFree = new Address(theNewFreeStart);
                    Address.setIntValue(theNewFree, 0, theNewFreeSize);
                    Address.setIntValue(theNewFree, 4, theNext);

                    if (thePrevious == 0) {
                        Address.setIntValue(theFreeStart, 0, theNewFreeStart);
                    } else {
                        final Address thePrev = new Address(thePrevious);
                        Address.setIntValue(thePrev, 4, theNewFreeStart);
                    }
                } else {
                    // Remaining size would be too small, be have to completely occupy it
                    Address.setIntValue(theCurrent, 0, theSize);

                    if (thePrevious == 0) {
                        Address.setIntValue(theFreeStart, 0, theNext);
                    } else {
                        final Address thePrev = new Address(thePrevious);
                        Address.setIntValue(thePrev, 4, theNext);
                    }
                }

                // Add the current block to the allocated block ist by prepending it to the list
                final Address theReservedListStart = new Address(8);

                final int theReservedListPtr = Address.getIntValue(theReservedListStart, 0);
                final int theCurrentStart = Address.getStart(theCurrent);

                Address.setIntValue(theCurrent, 4, theReservedListPtr);
                Address.setIntValue(theReservedListStart, 0, theCurrentStart);

                // Wipeout data
                final int theDataStart = theCurrentStart + 8;
                final Address theNewData = new Address(theDataStart);

                for (int i=0;i<aSize-16;i+=4) {
                    Address.setIntValue(theNewData, i, 0);
                }

                return theNewData;
            }

            thePrevious = Address.getStart(theCurrent);
            theCurrent = new Address(theNext);
        }
        Address.unreachable();
        return new Address(0);
    }

    @Export("newObject")
    public static Address newObject(final int aSize, final int aType, final int aVTableIndex) {

        final Address theAddress = malloc(aSize);
        Address.setIntValue(theAddress, 0, aType);
        Address.setIntValue(theAddress, 4, aVTableIndex);

        return theAddress;
    }

    private static boolean isUsed(final Address aOwningBlock) {

        final int theOwningStart = Address.getStart(aOwningBlock);
        final int theOwningData = theOwningStart + 8;

        // First of all we check the stack
        int theStackStart = Address.getStackTop();
        final int theStackTop = Address.getMemorySize();
        while(theStackStart < theStackTop) {
            final Address theCurrent = new Address(theStackStart);
            final int theReference = Address.getIntValue(theCurrent, 0);
            if (theReference == theOwningData) {
                return true;
            }
            theStackStart += 4;
        }

        // Nothing on the stack, we check the allocated memory blovks
        final Address theAllocatedStart= new Address(8);
        final int theAllocatedStartPtr = Address.getIntValue(theAllocatedStart, 0);

        Address theCurrent = new Address(theAllocatedStartPtr);
        while(Address.getStart(theCurrent) != 0) {

            final int theCurrentStart = Address.getStart(theCurrent);
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
            theCurrent = new Address(theNext);
        }

        return false;
    }

    @Export("GC")
    public static void GC() {
        final Address theUsedStart = new Address(8);

        final int theUsedStartPtr = Address.getIntValue(theUsedStart, 0);
        Address theCurrent = new Address(theUsedStartPtr);
        while(Address.getStart(theCurrent) != 0) {

            final int theNext = Address.getIntValue(theCurrent, 4);

            if (!isUsed(theCurrent)) {
                internalFree(theCurrent);
            }

            theCurrent = new Address(theNext);
        }
    }

    public static Address newArray(final int aSize, final int aType, final int aVTableIndex) {

        // Arrays are normal objects. Their data are a length field plus n * data
        final Address theObject = newObject(16 + 4 + 4 * aSize, aType, aVTableIndex);

        Address.setIntValue(theObject, 16, aSize);
        return theObject;
    }

    public static Address newArray(final int aSize1, final int aSize2, final int aType, final int aVTableIndex) {
        final Address theResult = newArray(aSize1, aType, aVTableIndex);
        for (int i=0;i<aSize1;i++) {
            final int theOffset = 16 + 4 + 4 * i;
            final Address theSubArray = newArray(aSize2, aType, aVTableIndex);
            Address.setIntValue(theResult, theOffset, Address.getStart(theSubArray));
        }
        return theResult;
    }

    @Export("newString")
    public static TString newString(final byte[] aData) {
        return new TString(aData);
    }

    @Export("newByteArray")
    public static byte[] newByteArray(final int length) {
        return new byte[length];
    }

    @Export("setByteArrayEntry")
    public static void setByteArrayEntry(final byte[] aArray, final int aIndex, final byte aValue) {
        aArray[aIndex] = aValue;
    }
}