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
package de.mirkosertic.bytecoder.classlib.java.lang;

public abstract class TNumber extends TObject {

    public abstract int intValue();

    public abstract byte byteValue();

    public abstract short shortValue();

    public abstract float floatValue();

    public abstract long longValue();

    public abstract double doubleValue();

    public static long stringToLong(String aString) {
        long theResult = 0;
        int theMultiplier = 1;
        for (int k=aString.length()-1;k>=0;k--) {
            char theCharAt = aString.charAt(k);
            if (k==0 && theCharAt == '-') {
                theResult=-theResult;
            } else {
                theResult += charValue(theCharAt) * theMultiplier;
                theMultiplier *= 10;
            }
        }
        return theResult;
    }

    public static int charValue(char aValue) {
        switch (aValue) {
            case '0':
                return 0;
            case '1':
                return 1;
            case '2':
                return 2;
            case '3':
                return 3;
            case '4':
                return 4;
            case '5':
                return 5;
            case'6':
                return 6;
            case '7':
                return 7;
            case '8':
                return 8;
            case '9':
                return 9;
            default:
                throw new IllegalStateException("Not supported character value for " + aValue);
        }
    }
}
