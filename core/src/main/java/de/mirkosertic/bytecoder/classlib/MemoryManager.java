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

import de.mirkosertic.bytecoder.annotations.Export;

public class MemoryManager {

    private static final int OFFSET_SIZE = 0;
    private static final int OFFSET_NEXT = 4;
    private static final int OFFSET_USED = 8;

    public static Object[] data;

    public static void initTestMemory(int aSize) {
        data = new Object[aSize];
    }

    public static void initWithSize(int aSize) {
        initTestMemory(aSize);
        initNative(aSize);
    }

    @Export("initMemory")
    public static void initNative(int aSize) {
        Address theFree = new Address(4);
        Address.setIntValue(theFree, OFFSET_SIZE, aSize);
        Address.setIntValue(theFree, OFFSET_NEXT, 0);
        Address.setIntValue(theFree, OFFSET_USED, 0);
    }

    @Export("freeMem")
    public static long freeMem() {
        long theResult = 0;
        Address theCurrent = new Address(4);
        while(Address.getStart(theCurrent) != 0) {
            int theUsedFlag = Address.getIntValue(theCurrent, OFFSET_USED);
            if (theUsedFlag == 0) {
                theResult += Address.getIntValue(theCurrent, OFFSET_SIZE);
            }
            int theNext = Address.getIntValue(theCurrent, OFFSET_NEXT);
            theCurrent = new Address(theNext);
        }
        return theResult;
    }

    @Export("usedMem")
    public static long usedMem() {
        long theResult = 0;
        Address theCurrent = new Address(4);
        while(Address.getStart(theCurrent) != 0) {
            int theUsedFlag = Address.getIntValue(theCurrent, OFFSET_USED);
            if (theUsedFlag == 1) {
                theResult += Address.getIntValue(theCurrent, OFFSET_SIZE);
            }
            int theNext = Address.getIntValue(theCurrent, OFFSET_NEXT);
            theCurrent = new Address(theNext);
        }
        return theResult;
    }

    @Export("free")
    public static void free(Address aPointer) {
        int theHeaderStart = Address.getStart(aPointer) - 12;
        Address theHeader = new Address(theHeaderStart);
        Address.setIntValue(theHeader, OFFSET_USED, 0);
    }

    private static Address mallocInternal(int aSize) {

        // Overhead for header
        aSize+=12;

        Address theCurrent = new Address(4);
        while(Address.getStart(theCurrent) != 0) {
            int theUsed = Address.getIntValue(theCurrent, OFFSET_USED);
            int theCurrentSize = Address.getIntValue(theCurrent, OFFSET_SIZE);
            if (theCurrentSize >= aSize && theUsed == 0) {

                // Mark block as used
                Address.setIntValue(theCurrent, OFFSET_SIZE, aSize);
                Address.setIntValue(theCurrent, OFFSET_USED, 1);

                if (theCurrentSize > aSize) {

                    int theCurrentNext = Address.getIntValue(theCurrent, OFFSET_NEXT);

                    // Mark remaining as free
                    int theNewPosition = Address.getStart(theCurrent) + aSize;

                    Address theNewFreeAdr = new Address(theNewPosition);
                    Address.setIntValue(theNewFreeAdr, OFFSET_USED, 0);
                    Address.setIntValue(theNewFreeAdr, OFFSET_SIZE, theCurrentSize - aSize);
                    Address.setIntValue(theNewFreeAdr, OFFSET_NEXT, theCurrentNext);

                    Address.setIntValue(theCurrent, OFFSET_NEXT, theNewPosition);
                }

                int theDataStart = Address.getStart(theCurrent) + 12;
                Address theNewData = new Address(theDataStart);
                // Wipeout data
                int theCleanSize = aSize;
                int thePosition = 0;
                while(theCleanSize > 12) {
                    Address.setIntValue(theNewData, thePosition, 0);
                    thePosition += 4;
                    theCleanSize -= 4;
                }

                return theNewData;
            }

            int theNext = Address.getIntValue(theCurrent, OFFSET_NEXT);
            theCurrent = new Address(theNext);
        }

        return new Address(0);
    }

    @Export("malloc")
    public static Address malloc(int aSize) {
        Address theAddress = mallocInternal(aSize);
        if (Address.getStart(theAddress) == 0) {
            GC();
            theAddress = mallocInternal(aSize);
        }
        if (Address.getStart(theAddress) == 0) {
            throw new RuntimeException();
        }
        return theAddress;
    }

    @Export("newObject")
    public static Address newObject(int aSize, int aType, int aVTableIndex) {
        Address theAddress = malloc(aSize);
        Address.setIntValue(theAddress, 0, aType);
        Address.setIntValue(theAddress, 4, aVTableIndex);
        return theAddress;
    }

    public static boolean isUsed(Address aOwningBlock) {

        int theOwningStart = Address.getStart(aOwningBlock);
        int theOwningData = theOwningStart + 12;

        // First of all we check the stack
        int theStackStart = Address.getStackTop();
        Address theStack = new Address(theStackStart);
        int theStackTop = Address.getMemorySize();
        while(theStackStart < theStackTop) {
            int theReference = Address.getIntValue(theStack, theStackStart);
            if (theReference == theOwningData) {
                return true;
            }
        }

        Address theCurrent = new Address(4);
        while(Address.getStart(theCurrent) != 0) {
            int theCurrentStart = Address.getStart(theCurrent);
            if (theOwningStart != theCurrentStart) {
                int theUsed = Address.getIntValue(theCurrent, OFFSET_USED);
                if (theUsed == 1) {
                    // Block is used, check content
                    int theStart = 12;
                    int theSize = Address.getIntValue(theCurrent, OFFSET_SIZE);
                    int thePosition = 0;
                    while(thePosition < theSize) {
                        int theReference = Address.getIntValue(theCurrent, theStart);
                        if (theReference == theOwningData) {
                            return true;
                        }
                        theStart += 4;
                        thePosition += 4;
                    }
                }
            }

            int theNext = Address.getIntValue(theCurrent, OFFSET_NEXT);
            theCurrent = new Address(theNext);
        }

        return false;
    }

    @Export("GC")
    public static void GC() {
        Address theCurrent = new Address(4);
        while(Address.getStart(theCurrent) != 0) {
            int theUsed = Address.getIntValue(theCurrent, OFFSET_USED);
            if (theUsed == 1) {
                // Block is used, check for reference
                if (!isUsed(theCurrent)) {
                    // Block is no longer used
                    Address.setIntValue(theCurrent, OFFSET_USED, 0);

                    //TODO: Check if it can be merged with the following block
                }
            }

            int theNext = Address.getIntValue(theCurrent, OFFSET_NEXT);
            theCurrent = new Address(theNext);
        }
    }

    public static Address newArray(int aSize1) {
        int theMemory = 4 + 4 * aSize1;
        Address theResult = malloc(theMemory);
        Address.setIntValue(theResult, 0, aSize1);
        return theResult;
    }

    public static Address newArray(int aSize1, int aSize2) {
        Address theResult = newArray(aSize1);
        for (int i=0;i<aSize1;i++) {
            int theOffset = 4 + 4 * i;
            Address theSubArray = newArray(aSize2);
            Address.setIntValue(theResult, theOffset, Address.getStart(theSubArray));
        }
        return theResult;
    }
}