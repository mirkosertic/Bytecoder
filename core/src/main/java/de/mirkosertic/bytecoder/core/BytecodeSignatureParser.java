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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class BytecodeSignatureParser {

    private final BytecodeReplacer replacer;

    public BytecodeSignatureParser(BytecodeReplacer aReplacer) {
        replacer = aReplacer;
    }

    private void add(List<BytecodeTypeRef> aTypes, BytecodeTypeRef aType, boolean isArray, int arrayDepth) {
        if (isArray) {
            aTypes.add(new BytecodeArrayTypeRef(aType, arrayDepth));
        } else {
            aTypes.add(aType);
        }
    }

    public BytecodeTypeRef toFieldType(BytecodeUtf8Constant aConstant) {
        BytecodeTypeRef[] theTypes = toTypes(aConstant.stringValue());
        return theTypes[0];
    }

    public BytecodeTypeRef[] toTypes(String aTypeList) {
        List<BytecodeTypeRef> theResult = new ArrayList();
        int p = 0;
        int arrayDepth = 0;
        boolean isArray = false;
        boolean isObject = false;
        StringBuilder objectName = null;
        while(p<aTypeList.length()) {
            char theChar = aTypeList.charAt(p++);
            if (isObject) {
                switch (theChar) {
                    case '[':
                        if (!isArray) {
                            isArray = true;
                            arrayDepth = 1;
                        } else {
                            arrayDepth++;
                        }
                        break;
                    case ';':
                        add(theResult, replacer.replaceTypeIn(new BytecodeObjectTypeRef(objectName.toString().replace("/","."))), isArray, arrayDepth);
                        isObject = false;
                        isArray = false;
                        objectName = null;
                        break;
                    default:
                        objectName.append(theChar);
                        break;
                }
            } else {
                switch (theChar) {
                case 'Z':
                    add(theResult, BytecodePrimitiveTypeRef.BOOLEAN, isArray, arrayDepth);
                    isArray = false;
                    break;
                case 'B':
                    add(theResult, BytecodePrimitiveTypeRef.BYTE, isArray, arrayDepth);
                    isArray = false;
                    break;
                case 'C':
                    add(theResult, BytecodePrimitiveTypeRef.CHAR, isArray, arrayDepth);
                    isArray = false;
                    break;
                case 'D':
                    add(theResult, BytecodePrimitiveTypeRef.DOUBLE, isArray, arrayDepth);
                    isArray = false;
                    break;
                case 'F':
                    add(theResult, BytecodePrimitiveTypeRef.FLOAT, isArray, arrayDepth);
                    isArray = false;
                    break;
                case 'I':
                    add(theResult, BytecodePrimitiveTypeRef.INT, isArray, arrayDepth);
                    isArray = false;
                    break;
                case 'J':
                    add(theResult, BytecodePrimitiveTypeRef.LONG, isArray, arrayDepth);
                    isArray = false;
                    break;
                case 'S':
                    add(theResult, BytecodePrimitiveTypeRef.SHORT, isArray, arrayDepth);
                    isArray = false;
                    break;
                case 'V':
                    add(theResult, BytecodePrimitiveTypeRef.VOID, isArray, arrayDepth);
                    isArray = false;
                    break;
                case 'L':
                    isObject = true;
                    objectName = new StringBuilder();
                    break;
                case '[':
                    if (!isArray) {
                        arrayDepth = 1;
                        isArray = true;
                    } else {
                        arrayDepth++;
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected character " + theChar + " in typelist " + aTypeList);
                }
            }
        }
        return theResult.toArray(new BytecodeTypeRef[theResult.size()]);
    }

    private BytecodeTypeRef toTypeRef(Class aClass) {
        if (aClass == Void.class) {
            return BytecodePrimitiveTypeRef.VOID;
        }
        if (aClass.isPrimitive()) {
            switch (aClass.getSimpleName()) {
                case "void":
                    return BytecodePrimitiveTypeRef.VOID;
                default:
                    throw new IllegalArgumentException("Unknown primitive : " + aClass.getSimpleName());
            }
        }
        if (aClass.isArray()) {
            throw new IllegalArgumentException("Unknown array type : " + aClass);
        }
        return BytecodeObjectTypeRef.fromRuntimeClass(aClass);
    }

    public BytecodeMethodSignature toMethodSignature(Method aMethod) {
        BytecodeTypeRef theReturnType = toTypeRef(aMethod.getReturnType());
        Class<?>[] theParameter = aMethod.getParameterTypes();
        BytecodeTypeRef[] theReturnValues = new BytecodeTypeRef[theParameter.length];
        for (int i=0;i<theParameter.length;i++) {
            theReturnValues[i] = toTypeRef(theParameter[i]);
        }
        return new BytecodeMethodSignature(theReturnType, theReturnValues);
    }

    public BytecodeMethodSignature toMethodSignature(BytecodeUtf8Constant aConstant) {
        StringBuilder theBuilder = new StringBuilder(aConstant.stringValue());
        int p = theBuilder.indexOf("(");
        int p2 = theBuilder.lastIndexOf(")");
        String theArguments = theBuilder.substring(p+1, p2);
        BytecodeTypeRef[] theReturnValue = toTypes(theBuilder.substring(p2 + 1));
        if (theReturnValue.length != 1) {
            throw new IllegalArgumentException("Invalid name signature: missing return type : " + theBuilder);
        }
        return new BytecodeMethodSignature(theReturnValue[0], toTypes(theArguments));
    }
}
