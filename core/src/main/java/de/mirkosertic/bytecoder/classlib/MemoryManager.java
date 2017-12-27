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
import de.mirkosertic.bytecoder.annotations.Import;

public class MemoryManager {

    public static Object[] data;

    public static void initTestMemory(int aSize) {
        data = new Object[aSize];
    }

    public static void initWithSize(int aSize) {
        initTestMemory(aSize);
        initInternal(aSize);
    }

    @Export("initMemory")
    public static void initNative() {
        initInternal(Address.getMemorySize());
    }

    @Import(module = "profiler", name = "logMemoryLayoutBlock")
    public static native void logMemoryLayoutBlock(int aStart, int aUsed, int aNext);

    private static void initInternal(int aSize) {
        Address theFree = new Address(4);
        Address.setIntValue(theFree, 0, aSize);
        Address.setIntValue(theFree, 4, 0);
        Address.setIntValue(theFree, 8, 0);
    }

    @Export("freeMem")
    public static long freeMem() {
        long theResult = 0;
        Address theCurrent = new Address(4);
        while(Address.getStart(theCurrent) != 0) {
            int theUsedFlag = Address.getIntValue(theCurrent, 8);
            if (theUsedFlag == 0) {
                theResult += Address.getIntValue(theCurrent, 0);
            }
            int theNext = Address.getIntValue(theCurrent, 4);
            theCurrent = new Address(theNext);
        }
        return theResult;
    }

    @Export("usedMem")
    public static long usedMem() {
        long theResult = 0;
        Address theCurrent = new Address(4);
        while(Address.getStart(theCurrent) != 0) {
            int theUsedFlag = Address.getIntValue(theCurrent, 8);
            if (theUsedFlag == 1) {
                theResult += Address.getIntValue(theCurrent, 0);
            }
            int theNext = Address.getIntValue(theCurrent, 4);
            theCurrent = new Address(theNext);
        }
        return theResult;
    }

    @Export("logMemoryLayout")
    public static void logMemoryLayout() {
        Address theCurrent = new Address(4);
        while(Address.getStart(theCurrent) != 0) {
            int theStart = Address.getStart(theCurrent);
            int theUsedFlag = Address.getIntValue(theCurrent, 8);
            int theNext = Address.getIntValue(theCurrent, 4);

            logMemoryLayoutBlock(theStart, theUsedFlag, theNext);

            theCurrent = new Address(theNext);
        }
    }

    @Export("free")
    public static void free(Address aPointer) {
        int theHeaderStart = Address.getStart(aPointer) - 12;
        Address theHeader = new Address(theHeaderStart);
        Address.setIntValue(theHeader, 8, 0);
    }

    private static Address mallocInternal(int aSize) {

        // Overhead for header
        aSize+=12;

        Address theCurrent = new Address(4);
        while(Address.getStart(theCurrent) != 0) {

            int theUsed = Address.getIntValue(theCurrent, 8);
            int theCurrentSize = Address.getIntValue(theCurrent, 0);
            if (theCurrentSize >= aSize && theUsed == 0) {

                // Mark block as used
                Address.setIntValue(theCurrent, 0, aSize);
                Address.setIntValue(theCurrent, 8, 1);

                if (theCurrentSize > aSize) {

                    int theCurrentNext = Address.getIntValue(theCurrent, 4);

                    // Mark remaining as free
                    int theNewPosition = Address.getStart(theCurrent);
                    theNewPosition+= aSize;
                    int theNewSize = theCurrentSize - aSize;

                    Address theNewFreeAdr = new Address(theNewPosition);
                    Address.setIntValue(theNewFreeAdr, 0, theNewSize);
                    Address.setIntValue(theNewFreeAdr, 8, 0);
                    Address.setIntValue(theNewFreeAdr, 4, theCurrentNext);

                    Address.setIntValue(theCurrent, 4, theNewPosition);
                }

                int theDataStart = Address.getStart(theCurrent) + 12;
                Address theNewData = new Address(theDataStart);
                // Wipeout data
                for (int i=0;i<aSize-16;i+=4) {
                    Address.setIntValue(theNewData, i, 0);
                }

                return theNewData;
            }

            int theNext = Address.getIntValue(theCurrent, 4);
            theCurrent = new Address(theNext);
        }

        return new Address(0);
    }

    @Export("malloc")
    public static Address malloc(int aSize) {
        Address theAddress = mallocInternal(aSize);
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
                int theUsed = Address.getIntValue(theCurrent, 8);
                if (theUsed == 1) {
                    // Block is used, check content
                    int theStart = 12;
                    int theSize = Address.getIntValue(theCurrent, 0);
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

            int theNext = Address.getIntValue(theCurrent, 4);
            theCurrent = new Address(theNext);
        }

        return false;
    }

    @Export("GC")
    public static void GC() {
        Address theCurrent = new Address(4);
        while(Address.getStart(theCurrent) != 0) {
            int theUsed = Address.getIntValue(theCurrent, 8);
            if (theUsed == 1) {
                // Block is used, check for reference
                if (!isUsed(theCurrent)) {
                    // Block is no longer used
                    Address.setIntValue(theCurrent, 8, 0);

                    //TODO: Check if it can be merged with the following block
                }
            }

            int theNext = Address.getIntValue(theCurrent, 4);
            theCurrent = new Address(theNext);
        }
    }

    public static Address newArray(int aSize, int aType, int aVTableIndex) {

        // Arrays are normal objects. Their data are a length field plus n * data
        Address theObject = newObject(16 + 4 + 4 * aSize, aType, aVTableIndex);

        Address.setIntValue(theObject, 16, aSize);
        return theObject;
    }

    public static Address newArray(int aSize1, int aSize2, int aType, int aVTableIndex) {
        Address theResult = newArray(aSize1, aType, aVTableIndex);
        for (int i=0;i<aSize1;i++) {
            int theOffset = 16 + 4 + 4 * i;
            Address theSubArray = newArray(aSize2, aType, aVTableIndex);
            Address.setIntValue(theResult, theOffset, Address.getStart(theSubArray));
        }
        return theResult;
    }
}