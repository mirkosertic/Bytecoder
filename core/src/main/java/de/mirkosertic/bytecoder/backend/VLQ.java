/*
 * Copyright 2019 Mirko Sertic
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
package de.mirkosertic.bytecoder.backend;

import java.util.ArrayList;
import java.util.List;

public class VLQ {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

    private VLQ() {
    }

    public static int[] decode(final String aData) {
        final List<Integer> theResult = new ArrayList<>();
        int theShift = 0;
        int theValue = 0;
        for (int i=0;i<aData.length();i++) {
            int theInteger = CHARACTERS.indexOf(aData.charAt(i));

            final int hasContinuationBit = theInteger & 32;

            theInteger &= 31;
            theValue += theInteger << theShift;

            if (hasContinuationBit > 0) {
                theShift += 5;
            } else {
                final int shouldNegate = theValue & 1;
                theValue >>= 1;

                theResult.add( shouldNegate > 0 ? -theValue : theValue );

                // reset
                theValue = theShift = 0;
            }
        }
        final int[] theResultArray = new int[theResult.size()];
        for (int i=0;i<theResult.size();i++) {
            theResultArray[i] = theResult.get(i);
        }
        return theResultArray;
    }

    public static String encode(final int[] aData) {
        final StringBuilder theResult = new StringBuilder();
        for (int i = 0; i < aData.length; i += 1 ) {
            theResult.append(encodeInteger(aData[i]));
        }
        return theResult.toString();
    }

    private static String encodeInteger(int aNumber) {
        final StringBuilder theResult = new StringBuilder();
        if ( aNumber < 0 ) {
            aNumber = ( -aNumber << 1 ) | 1;
        } else {
            aNumber <<= 1;
        }

        do {
            int theClamped = aNumber & 31;
            aNumber >>= 5;

            if ( aNumber > 0 ) {
                theClamped |= 32;
            }

            theResult.append(CHARACTERS.charAt(theClamped));
        } while ( aNumber > 0 );

        return theResult.toString();
    }
}
