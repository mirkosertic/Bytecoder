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
package de.mirkosertic.bytecoder.core;

import java.util.ArrayList;
import java.util.List;

public class BytecodeSignatureParser {

    private void add(List<String> aTypes, String aType, boolean isArray) {
        if (isArray) {
            aTypes.add(aType + "[]");
        } else {
            aTypes.add(aType);
        }
    }

    private String[] toTypes(String aTypeList) {
        List<String> theResult = new ArrayList();
        int p = 0;
        boolean isArray = false;
        while(p<aTypeList.length()) {
            char theChar = aTypeList.charAt(p++);
            switch (theChar) {
                case 'V':
                    add(theResult, "void", isArray);
                    isArray = false;
                    break;
                case 'I':
                    add(theResult, "integer", isArray);
                    isArray = false;
                    break;
                case 'L':
                    add(theResult, "long", isArray);
                    isArray = false;
                    break;
                case 'B':
                    add(theResult, "boolean", isArray);
                    isArray = false;
                    break;
                case '[':
                    isArray = true;
                    break;
                default:
                    throw new IllegalStateException("Unexpected character " + theChar + " in typelist " + aTypeList);
            }
        }
        return theResult.toArray(new String[theResult.size()]);
    }

    public BytecodeMethodSignature toMethodSignature(BytecodeUtf8Constant aConstant) {
        StringBuilder theBuilder = new StringBuilder(aConstant.stringValue());
        int p = theBuilder.indexOf("(");
        int p2 = theBuilder.lastIndexOf(")");
        String theArguments = theBuilder.substring(p+1, p2);
        String[] theReturnValue = toTypes(theBuilder.substring(p2 + 1));
        if (theReturnValue.length != 1) {
            throw new IllegalArgumentException("Invalid method signature: missing return type : " + theBuilder);
        }
        return new BytecodeMethodSignature(theReturnValue[0], toTypes(theArguments));
    }
}
