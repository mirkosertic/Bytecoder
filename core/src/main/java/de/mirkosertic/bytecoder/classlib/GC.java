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

    private static Address FREE;
    private static Address USED;

    public static void initWithSize(int aSize) {
        FREE = new Address(0, aSize);
        FREE.setIntValue(OFFSET_SIZE, aSize);
        FREE.setObjectValue(OFFSET_NEXT, null);
        USED = null;
    }

    public static long freeMem() {
        long theResult = 0;
        Address theCurrent = FREE;
        while(theCurrent != null) {
            theResult+=(int) theCurrent.getIntValue(OFFSET_SIZE);
            theCurrent = (Address) theCurrent.getObjectValue(OFFSET_NEXT);
        }
        return theResult;
    }

    public static long usedMem() {
        long theResult = 0;
        Address theCurrent = USED;
        while(theCurrent != null) {
            theResult+=(int) theCurrent.getIntValue(OFFSET_SIZE);
            theCurrent = (Address) theCurrent.getObjectValue(OFFSET_NEXT);
        }
        return theResult;
    }

    public static void free(Address aPointer) {
        Address theCurrent = USED;
        Address thePrevious = null;
        while(theCurrent != null) {
            if (theCurrent.getStart() == aPointer.getStart()) {
                if (thePrevious != null) {
                    thePrevious.setIntValue(OFFSET_NEXT, theCurrent.getIntValue(OFFSET_NEXT));
                } else {
                    USED = (Address) theCurrent.getObjectValue(OFFSET_NEXT);
                }

                theCurrent.setObjectValue(OFFSET_NEXT, FREE);
                FREE = theCurrent;

                return;
            }
            thePrevious = theCurrent;
            theCurrent = (Address) theCurrent.getObjectValue(OFFSET_NEXT);
        }
    }

    public static Address malloc(int aSize) {
        Address thePrevious = null;
        Address theCurrent = FREE;
        while(theCurrent != null) {
            if ((int) theCurrent.getIntValue(OFFSET_SIZE) > aSize) {

                Address theALLOCATED = new Address(theCurrent.getStart(), aSize);
                theALLOCATED.setIntValue(OFFSET_SIZE, aSize);

                Address theNewFree = new Address(theCurrent.getStart() + aSize, aSize);
                theNewFree.setIntValue(OFFSET_SIZE, (int) theCurrent.getIntValue(OFFSET_SIZE) - aSize);

                if (thePrevious != null) {
                    thePrevious.setObjectValue(OFFSET_NEXT, theNewFree);
                } else {
                    FREE = theNewFree;
                }

                if (USED == null) {
                    USED = theALLOCATED;
                } else {
                    theALLOCATED.setObjectValue(OFFSET_NEXT, USED);
                    USED = theALLOCATED;
                }

                return theALLOCATED;
            }

            thePrevious = theCurrent;
            theCurrent = (Address) theCurrent.getObjectValue(OFFSET_NEXT);
        }
        return null;
    }
}