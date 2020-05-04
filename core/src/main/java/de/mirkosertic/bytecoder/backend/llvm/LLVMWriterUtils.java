/*
 * Copyright 2020 Mirko Sertic
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
package de.mirkosertic.bytecoder.backend.llvm;

import de.mirkosertic.bytecoder.classlib.Array;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeUtf8Constant;
import de.mirkosertic.bytecoder.ssa.TypeRef;

public class LLVMWriterUtils {

    public static String toMethodName(final String aMethodName, final BytecodeMethodSignature aSignature) {
        final StringBuilder theName = new StringBuilder(typeRefToString(aSignature.getReturnType()));
        theName.append(aMethodName.replace("<", "$").replace(">", "$").replace("/","_"));

        for (final BytecodeTypeRef theTypeRef : aSignature.getArguments()) {
            theName.append(typeRefToString(theTypeRef));
        }
        return theName.toString();
    }

    public static String toMethodName(final BytecodeObjectTypeRef aClassName, final String aMethodName, final BytecodeMethodSignature aSignature) {
        return toClassName(aClassName) + "_" + toMethodName(aMethodName, aSignature);
    }

    public static String toMethodName(final BytecodeObjectTypeRef aClassName, final BytecodeUtf8Constant aConstant, final BytecodeMethodSignature aSignature) {
        return toClassName(aClassName) + "_" + toMethodName(aConstant.stringValue(), aSignature);
    }

    public static String toClassName(final BytecodeObjectTypeRef aTypeRef) {
        if (aTypeRef.name().endsWith(";")) {
            // This seems to be an array
            return toClassName(BytecodeObjectTypeRef.fromRuntimeClass(Array.class));
        }
        return toClassNameInternal(aTypeRef.name());
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

    public static String toType(final TypeRef aType) {
        switch (aType.resolve()) {
        case DOUBLE:
            return "double";
        case FLOAT:
            return "float";
        case VOID:
            return "void";
        case LONG:
            return "i64";
        default:
            return "i32";
        }
    }

    public static String toSignature(final BytecodeMethodSignature signature) {
        final StringBuilder theResult = new StringBuilder();
        theResult.append(toType(TypeRef.toType(signature.getReturnType())));
        theResult.append("(");
        theResult.append("i32");
        for (int i = 0; i < signature.getArguments().length; i++) {
            final BytecodeTypeRef theParamType = signature.getArguments()[i];
            theResult.append(",");
            theResult.append(toType(TypeRef.toType(theParamType)));
        }
        theResult.append(")");
        return theResult.toString();
    }

    public static boolean filteredForTest(final BytecodeLinkedClass aClass) {
        /*if (!aClass.getClassName().name().contains("LLVMCompilerBackendTest")
                && !aClass.getClassName().name().equals(MemoryManager.class.getName())
                && !aClass.getClassName().name().equals(Array.class.getName())
                && !aClass.getClassName().name().equals(Enum.class.getName())) {
            return false;
        }*/
        return true;
    }

    public static String runtimeClassVariableName(final BytecodeObjectTypeRef aType) {
        return "runtimeclass_" + aType.name().replace('.', '_').replace('$','_');
    }
}
