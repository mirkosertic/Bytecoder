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

public class GC {

    private static final int OFFSET_SIZE = 0;
    private static final int OFFSET_NEXT = 1;

    public static Object[] data;

    private static Address FREE;
    private static Address USED;

    public static void initTestMemory(int aSize) {
        data = new Object[aSize];
    }

    public static void initWithSize(int aSize) {
        initTestMemory(aSize);
        FREE = new Address(0);
        Address.setIntValue(FREE, OFFSET_SIZE, aSize);
        Address.setObjectValue(FREE, OFFSET_NEXT, null);
        USED = null;
    }

    public static long freeMem() {
        long theResult = 0;
        Address theCurrent = FREE;
        while(theCurrent != null) {
            theResult+=(int) Address.getIntValue(theCurrent, OFFSET_SIZE);
            theCurrent = (Address) Address.getObjectValue(theCurrent, OFFSET_NEXT);
        }
        return theResult;
    }

    public static long usedMem() {
        long theResult = 0;
        Address theCurrent = USED;
        while(theCurrent != null) {
            theResult+=(int) Address.getIntValue(theCurrent, OFFSET_SIZE);
            theCurrent = (Address) Address.getObjectValue(theCurrent, OFFSET_NEXT);
        }
        return theResult;
    }

    public static void free(Address aPointer) {
        Address theCurrent = USED;
        Address thePrevious = null;
        while(theCurrent != null) {
            if (Address.getStart(theCurrent) == Address.getStart(aPointer)) {
                if (thePrevious != null) {
                    Address.setIntValue(thePrevious, OFFSET_NEXT, Address.getIntValue(theCurrent, OFFSET_NEXT));
                } else {
                    USED = (Address) Address.getObjectValue(theCurrent, OFFSET_NEXT);
                }

                Address.setObjectValue(theCurrent, OFFSET_NEXT, FREE);
                FREE = theCurrent;

                return;
            }
            thePrevious = theCurrent;
            theCurrent = (Address) Address.getObjectValue(theCurrent, OFFSET_NEXT);
        }
    }

    public static Address malloc(int aSize) {
        Address thePrevious = null;
        Address theCurrent = FREE;
        while(theCurrent != null) {
            int theCurrentSize = Address.getIntValue(theCurrent, OFFSET_SIZE);
            if ((int) theCurrentSize > aSize) {

                Address theALLOCATED = new Address(Address.getStart(theCurrent));
                Address.setIntValue(theALLOCATED, OFFSET_SIZE, aSize);

                Address theNewFree = new Address(Address.getStart(theCurrent) + aSize);
                Address.setIntValue(theNewFree, OFFSET_SIZE, theCurrentSize - aSize);

                if (thePrevious != null) {
                    Address.setObjectValue(thePrevious, OFFSET_NEXT, theNewFree);
                } else {
                    FREE = theNewFree;
                }

                if (USED == null) {
                    USED = theALLOCATED;
                } else {
                    Address.setObjectValue(theALLOCATED, OFFSET_NEXT, USED);
                    USED = theALLOCATED;
                }

                return theALLOCATED;
            }

            thePrevious = theCurrent;
            theCurrent = (Address) Address.getObjectValue(theCurrent, OFFSET_NEXT);
        }
        return null;
    }
}