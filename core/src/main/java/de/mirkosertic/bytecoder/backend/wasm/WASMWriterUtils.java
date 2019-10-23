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
package de.mirkosertic.bytecoder.backend.wasm;

import de.mirkosertic.bytecoder.classlib.Array;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeUtf8Constant;
import de.mirkosertic.bytecoder.ssa.TypeRef;

public class WASMWriterUtils {

    private static String typeRefToString(final BytecodeTypeRef aTypeRef) {
        if (aTypeRef.isPrimitive()) {
            final BytecodePrimitiveTypeRef thePrimitive = (BytecodePrimitiveTypeRef) aTypeRef;
            return thePrimitive.name();
        }
        if (aTypeRef.isArray()) {
            final BytecodeArrayTypeRef theRef = (BytecodeArrayTypeRef) aTypeRef;
            return "A" + theRef.getDepth() + typeRefToString(theRef.getType());
        }
        final BytecodeObjectTypeRef theObjectRef = (BytecodeObjectTypeRef) aTypeRef;
        return toClassName(theObjectRef);
    }

    public static String toMethodName(final String aMethodName, final BytecodeMethodSignature aSignature) {
        String theName = typeRefToString(aSignature.getReturnType());
        theName += aMethodName.replace("<", "$").replace(">", "$");

        for (final BytecodeTypeRef theTypeRef : aSignature.getArguments()) {
            theName += typeRefToString(theTypeRef);
        }
        return theName;
    }

    public static String toMethodName(final BytecodeObjectTypeRef aClassName, final String aMethodName, final BytecodeMethodSignature aSignature) {
        return toClassName(aClassName) + "_" + toMethodName(aMethodName, aSignature);
    }

    public static String toMethodName(final BytecodeObjectTypeRef aClassName, final BytecodeUtf8Constant aConstant, final BytecodeMethodSignature aSignature) {
        return toClassName(aClassName) + "_" + toMethodName(aConstant.stringValue(), aSignature);
    }

    private static String toClassNameInternal(final String aClassName) {
        final int p = aClassName.lastIndexOf(".");
        final String theSimpleName = aClassName.substring(p + 1);
        String thePackageName = aClassName.substring(0, p);
        final StringBuilder theResult = new StringBuilder();
        while(thePackageName.length() > 0) {
            theResult.append(Character.toLowerCase(thePackageName.charAt(0)));
            final int j = thePackageName.indexOf(".");
            if (j>=0) {
                thePackageName = thePackageName.substring(j + 1);
            } else {
                thePackageName = "";
            }
        }

        return theResult.append(theSimpleName).toString();
    }

    public static String toClassName(final BytecodeObjectTypeRef aTypeRef) {
        if (aTypeRef.name().endsWith(";")) {
            // This seems to be an array
            return toClassName(BytecodeObjectTypeRef.fromRuntimeClass(Array.class));
        }
        return toClassNameInternal(aTypeRef.name());
    }

    public static String toType(final TypeRef aType) {
        switch (aType.resolve()) {
            case DOUBLE:
                return "f32";
            case FLOAT:
                return "f32";
            default:
                return "i32";
        }
    }
}
