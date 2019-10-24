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
public class TFloat {

    @Override
    public String toString() {
        return toString(((Float) (Object) this).floatValue());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        if (((Float) o).floatValue() != ((Float) (Object) this).floatValue()) {
            return false;
        }

        return true;
    }

    public static float parseFloat(final String aValue) {
        final int p = aValue.indexOf('.');
        if (p<0) {
            return VM.stringToLong(aValue);
        }
        final String thePrefix = aValue.substring(0, p);
        final String theSuffix = aValue.substring(p + 1);
        final long theA = VM.stringToLong(thePrefix);
        final long theB = VM.stringToLong(theSuffix);
        int theMultiplier = 1;
        int theLength = Long.toString(theB).length();
        while(theLength > 0) {
            theMultiplier *= 10;
            theLength--;
        }
        if (theA > 0) {
            return theA + ((float) theB) / theMultiplier;
        }
        return theA - ((float) theB) / theMultiplier;
    }

    public static Float valueOf(final String aValue) {
        return parseFloat(aValue);
    }

    public static String toString(final float aValue) {
        final StringBuilder theBuilder = new StringBuilder();
        theBuilder.append(aValue);
        return theBuilder.toString();
    }
}