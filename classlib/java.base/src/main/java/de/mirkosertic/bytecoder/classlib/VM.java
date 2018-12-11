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

import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

public class VM {

    public static abstract class ImplementingCallsite extends ConstantCallSite {

        public ImplementingCallsite(final MethodHandle target) {
            super(target);
        }

        public abstract Object invokeExact(Object... args) throws Throwable;
    }

    public static native Object newRuntimeGeneratedType(MethodType aType, MethodHandle aHandle, Object... staticArguments);

    public static final char NEWLINE = '\n';

    public static long stringToLong(final String aString) {
        long theResult = 0;
        int theMultiplier = 1;
        for (int k=aString.length()-1;k>=0;k--) {
            final char theCharAt = aString.charAt(k);
            if (k==0 && theCharAt == '-') {
                theResult=-theResult;
            } else {
                theResult += charValue(theCharAt) * theMultiplier;
                theMultiplier *= 10;
            }
        }
        return theResult;
    }

    public static int charValue(final char aValue) {
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

    public static char toHexCharacter(final int aValue) {
        switch (aValue) {
        case 0:
            return '0';
        case 1:
            return '1';
        case 2:
            return '2';
        case 3:
            return '3';
        case 4:
            return '4';
        case 5:
            return '5';
        case 6:
            return '6';
        case 7:
            return '7';
        case 8:
            return '8';
        case 9:
            return '9';
        case 10:
            return 'a';
        case 11:
            return 'b';
        case 12:
            return 'c';
        case 13:
            return 'd';
        case 14:
            return 'e';
        case 15:
            return 'f';
        default:
            throw new IllegalArgumentException("Not supported value : " + aValue);
        }
    }

    public static String longToHex(long aValue) {
        if (aValue == 0) {
            return "0";
        }
        final StringBuilder theResult = new StringBuilder();
        while(aValue > 0) {
            final int theModulo = (int) (aValue % 16);
            theResult.append(toHexCharacter(theModulo));
            aValue = aValue >> 4;
        }
        return theResult.reverse().toString();
    }
}
