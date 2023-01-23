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

import java.text.DecimalFormatSymbols;

public class StringHelper {

    public static final DecimalFormatSymbols FORMAT_SYMBOLS = DecimalFormatSymbols.getInstance();

    public static void appendInternal(final StringBuilder sb, final double aValue, final long aMultiplier) {
        final long theA;
        final long theB;
        if (aValue < 0) {
            theA = (long) Math.ceil(aValue);
            theB = - (long) Math.ceil((aValue % 1) * 10000);
        } else {
            theA = (long) Math.floor(aValue);
            theB = (long) Math.floor((aValue % 1) * 10000);
        }
        sb.append(theA);

        final StringBuilder theTemp = new StringBuilder();
        theTemp.append(theB);

        for (int i=theTemp.length()-1;i>=0;i--) {
            final char theChar = theTemp.charAt(i);
            if (theChar != '0') {
                sb.append(FORMAT_SYMBOLS.getDecimalSeparator());
                for (int j=0;j<=i;j++) {
                    sb.append(theTemp.charAt(j));
                }
                return;
            }
        }

        sb.append(FORMAT_SYMBOLS.getDecimalSeparator());
        sb.append('0');
    }

    public static final byte[] DigitTens = {
            '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
            '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
            '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
            '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
            '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
            '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
            '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
            '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
            '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
            '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
    } ;

    public static final byte[] DigitOnes = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    };
}
