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

    public static native Object newRuntimeGeneratedType(String methodName, MethodType aType, MethodHandle aHandle, Object... staticArguments);

    public static final char NEWLINE = '\n';

    public static long stringToLong(final String aString) {
        long theResult = 0;
        int theMultiplier = 1;
        for (int k=aString.length()-1;k>=0;k--) {
            final char theCharAt = aString.charAt(k);
            if (k==0 && theCharAt == '-') {
                theResult=-theResult;
            } else {
                theResult += Character.getNumericValue(theCharAt) * theMultiplier;
                theMultiplier *= 10;
            }
        }
        return theResult;
    }

    public static String longToHex(long aValue) {
        if (aValue == 0) {
            return "0";
        }
        final StringBuilder theResult = new StringBuilder();
        while(aValue > 0) {
            final int theModulo = (int) (aValue % 16);
            theResult.append(Character.forDigit(theModulo, 16));
            aValue = aValue >> 4;
        }
        return theResult.reverse().toString();
    }
}
