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
package de.mirkosertic.bytecoder.classlib.java.lang.invoke;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;
import de.mirkosertic.bytecoder.classlib.VM;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@SubstitutesInClass(completeReplace = true)
public class TStringConcatFactory {

    public static CallSite makeConcat(final MethodHandles.Lookup aLookup, final String aName, final MethodType aConcatType) {

        return new VM.ImplementingCallsite(null) {
            @Override
            public Object invokeExact(final Object... args) throws Throwable {
                final StringBuilder theResult = new StringBuilder();
                if (args != null) {
                    for (int i=0;i<args.length;i++) {
                        appendTo(theResult, args, i, aConcatType, i);
                    }
                }
                return theResult.toString();
            }
        };
    }

    private static void appendTo(final StringBuilder aTarget, final Object[] aArray, final int aArrayIndex,  final MethodType aType, final int aIndex) {
        if (VM.isInteger(aType, aIndex)) {
            aTarget.append(VM.arrayEntryAsInt(aArray, aArrayIndex));
        } else if (VM.isLong(aType, aIndex)) {
            aTarget.append(VM.arrayEntryAsLong(aArray, aArrayIndex));
        } else if (VM.isFloat(aType, aIndex)) {
            aTarget.append(VM.arrayEntryAsFloat(aArray, aArrayIndex));
        } else if (VM.isDouble(aType, aIndex)) {
            aTarget.append(VM.arrayEntryAsDouble(aArray, aArrayIndex));
        } else if (VM.isBoolean(aType, aIndex)) {
            aTarget.append(VM.arrayEntryAsBoolean(aArray, aArrayIndex));
        } else if (VM.isChar(aType, aIndex)) {
            aTarget.append(VM.arrayEntryAsChar(aArray, aArrayIndex));
        } else if (VM.isShort(aType, aIndex)) {
            aTarget.append(VM.arrayEntryAsShort(aArray, aArrayIndex));
        } else if (VM.isByte(aType, aIndex)) {
            aTarget.append(VM.arrayEntryAsByte(aArray, aArrayIndex));
        } else {
            aTarget.append(aArray[aArrayIndex]);
        }
    }

    public static CallSite makeConcatWithConstants(final MethodHandles.Lookup aLookup, final String aName, final MethodType aConcatType, final String aRecipe, final Object... aConstants) {

        return new VM.ImplementingCallsite(null) {
            @Override
            public Object invokeExact(final Object... args) throws Throwable {
                int theConstIndex = 0;
                int theDynIndex = 0;
                int totalIndex = 0;
                final StringBuilder theResult = new StringBuilder();
                for (int i=0;i<aRecipe.length();i++) {
                    final char theChar = aRecipe.charAt(i);
                    if (theChar == 1) {
                        appendTo(theResult, args, theDynIndex++, aConcatType, totalIndex++);
                    } else if (theChar == 2) {
                        appendTo(theResult, aConstants, theConstIndex++, aConcatType, totalIndex++);
                    } else {
                        theResult.append(theChar);
                    }
                }
                return theResult.toString();
            }
        };
    }
}