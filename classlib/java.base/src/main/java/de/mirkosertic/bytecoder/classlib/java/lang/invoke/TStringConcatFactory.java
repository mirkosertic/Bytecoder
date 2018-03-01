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

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;
import de.mirkosertic.bytecoder.classlib.VM;

@SubstitutesInClass(completeReplace = true)
public class TStringConcatFactory {

    public static CallSite makeConcat(MethodHandles.Lookup aLookup, String aName, MethodType aConcatType) {

        return new VM.ImplementingCallsite(null) {
            @Override
            public Object invokeExact(Object... args) throws Throwable {
                StringBuilder theResult = new StringBuilder();
                if (args != null) {
                    for (int i=0;i<args.length;i++) {
                        theResult.append(args[i]);
                    }
                }
                return theResult.toString();
            }
        };
    }

    public static CallSite makeConcatWithConstants(MethodHandles.Lookup aLookup, String aName, MethodType aConcatType, String aRecipe, Object... aConstants) {

        return new VM.ImplementingCallsite(null) {
            @Override
            public Object invokeExact(Object... args) throws Throwable {
                int theConstIndex = 0;
                int theDynIndex = 0;
                StringBuilder theResult = new StringBuilder();
                for (int i=0;i<aRecipe.length();i++) {
                    char theChar = aRecipe.charAt(i);
                    if (theChar == 1) {
                        theResult.append(args[theDynIndex++]);
                    } else if (theChar == 2) {
                        theResult.append(aConstants[theConstIndex++]);
                    } else {
                        theResult.append(theChar);
                    }
                }
                return theResult.toString();
            }
        };
    }
}