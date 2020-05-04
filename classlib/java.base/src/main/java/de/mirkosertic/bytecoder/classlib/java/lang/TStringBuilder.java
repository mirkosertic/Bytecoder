/*
 * Copyright 2018 Mirko Sertic
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
package de.mirkosertic.bytecoder.classlib.java.lang;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;
import de.mirkosertic.bytecoder.classlib.VM;

@SubstitutesInClass(completeReplace = false)
public class TStringBuilder {

    public StringBuilder append(final float aValue) {
        VM.appendInternal((StringBuilder) (Object) this, aValue, 1000000000);
        return (StringBuilder) (Object) this;
    }

    public StringBuilder append(final double aValue) {
        VM.appendInternal((StringBuilder) (Object) this, aValue, 1000000000);
        return (StringBuilder) (Object) this;
    }

    public StringBuilder append(long aValue) {
        boolean isNegative = false;
        if (aValue < 0) {
            isNegative = true;
            aValue=-aValue;
        }
        final char[] theCharacters = new char[20];
        int theOffset = 0;

        do {
            final int theRemainder = (int) (aValue % 10);
            theCharacters[theOffset++] = Character.forDigit(theRemainder, 10);
            aValue = aValue / 10;
        } while (aValue > 0);

        final char[] theNewData;
        final int theStart;
        if (isNegative) {
            theNewData = new char[theOffset + 1];
            theNewData[0] = '-';
            theStart = 1;
        } else {
            theNewData = new char[theOffset];
            theStart = 0;
        }
        for (int i=0;i<theOffset;i++) {
            theNewData[theStart + i] = theCharacters[theOffset - 1 - i];
        }

        final StringBuilder sb = (StringBuilder) (Object) this;
        sb.append(theNewData);
        return sb;
    }
}
