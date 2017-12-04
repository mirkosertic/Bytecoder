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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.mirkosertic.bytecoder.classlib.java.lang.TArray;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeClassinfoConstant;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeUtf8Constant;
import de.mirkosertic.bytecoder.ssa.TypeRef;

public class WASMWriterUtils {

    public static final int CLASS_HEADER_SIZE = 16; // Object header plus initialization status + enum values offset
    public static final int OBJECT_HEADER_SIZE = 8;
    public static final int OBJECT_FIELDSIZE = 4;

    public static String typeRefToString(BytecodeTypeRef aTypeRef) {
        if (aTypeRef.isPrimitive()) {
            BytecodePrimitiveTypeRef thePrimitive = (BytecodePrimitiveTypeRef) aTypeRef;
            return thePrimitive.name();
        }
        if (aTypeRef.isArray()) {
            BytecodeArrayTypeRef theRef = (BytecodeArrayTypeRef) aTypeRef;
            return "A" + theRef.getDepth() + typeRefToString(theRef.getType());
        }
        BytecodeObjectTypeRef theObjectRef = (BytecodeObjectTypeRef) aTypeRef;
        return toClassName(theObjectRef);
    }

    public static String toMethodSignature(BytecodeMethodSignature aSignature, boolean aIsStatic) {
        String theName = typeRefToString(aSignature.getReturnType()) + "_";
        if (!aIsStatic) {
            theName += "THISREF";
        }

        for (BytecodeTypeRef theTypeRef : aSignature.getArguments()) {
            theName += typeRefToString(theTypeRef);
        }
        return theName.replace("(", "").replace(")", "");
    }


    public static String toMethodName(String aMethodName, BytecodeMethodSignature aSignature) {
        String theName = typeRefToString(aSignature.getReturnType());
        theName += aMethodName.replace("<", "").replace(">", "");

        for (BytecodeTypeRef theTypeRef : aSignature.getArguments()) {
            theName += typeRefToString(theTypeRef);
        }
        return theName;
    }

    public static String toMethodName(BytecodeObjectTypeRef aClassName, String aMethodName, BytecodeMethodSignature aSignature) {
        return toClassName(aClassName) + "_" + toMethodName(aMethodName, aSignature);
    }

    public static String toMethodName(BytecodeObjectTypeRef aClassName, BytecodeUtf8Constant aConstant, BytecodeMethodSignature aSignature) {
        return toClassName(aClassName) + "_" + toMethodName(aConstant.stringValue(), aSignature);
    }

    public static String toClassNameInternal(String aClassName) {
        int p = aClassName.lastIndexOf(".");
        return aClassName.substring(p + 1);
    }

    public static String toClassName(BytecodeObjectTypeRef aTypeRef) {
        if (aTypeRef.name().endsWith(";")) {
            // This seems to be an array
            return toClassName(BytecodeObjectTypeRef.fromRuntimeClass(TArray.class));
        }
        return toClassNameInternal(aTypeRef.name());
    }

    public static String toClassName(BytecodeClassinfoConstant aTypeRef) {
        return toClassNameInternal(aTypeRef.getConstant().stringValue().replace("/","."));
    }

    public static String toType(TypeRef aType) {
        switch (aType.resolve()) {
            case DOUBLE:
                return "f32";
            case FLOAT:
                return "f32";
            default:
                return "i32";
        }
    }

    private static List<String> memberFieldNamesOf(BytecodeLinkedClass aClass) {
        Set<String> theFields = new HashSet<>();
        BytecodeLinkedClass theCurrent = aClass;
        while(theCurrent != null) {
            theCurrent.forEachMemberField(t -> theFields.add(t.getKey()));
            theCurrent = theCurrent.getSuperClass();
        }
        List<String> theSortedFields = new ArrayList<>(theFields);
        Collections.sort(theSortedFields);
        return theSortedFields;
    }

    private static List<String> classFieldNamesOf(BytecodeLinkedClass aClass) {
        Set<String> theFields = new HashSet<>();
        BytecodeLinkedClass theCurrent = aClass;
        while(theCurrent != null) {
            theCurrent.forEachStaticField(t -> theFields.add(t.getKey()));
            theCurrent = theCurrent.getSuperClass();
        }
        List<String> theSortedFields = new ArrayList<>(theFields);
        Collections.sort(theSortedFields);
        return theSortedFields;
    }

    public static int computeObjectSizeFor(BytecodeLinkedClass aClass) {
        List<String> theFieldNames = memberFieldNamesOf(aClass);
        return OBJECT_HEADER_SIZE + theFieldNames.size() * OBJECT_FIELDSIZE;
    }

    public static int computeClassSizeFor(BytecodeLinkedClass aClass) {
        List<String> theFieldNames = classFieldNamesOf(aClass);
        return CLASS_HEADER_SIZE + theFieldNames.size() * OBJECT_FIELDSIZE;
    }

    public static int computeStaticFieldOffsetOf(String aFieldName, BytecodeLinkedClass aClass) {
        List<String> theFieldNames = classFieldNamesOf(aClass);
        int p = theFieldNames.indexOf(aFieldName);
        if (p<0) {
            throw new IllegalStateException("Unknown field " + aFieldName + " in " + aClass.getClassName().name());
        }
        return CLASS_HEADER_SIZE + p * OBJECT_FIELDSIZE;
    }

    public static int computeFieldOffsetOf(String aFieldName, BytecodeLinkedClass aClass) {
        List<String> theFieldNames = memberFieldNamesOf(aClass);
        int p = theFieldNames.indexOf(aFieldName);
        if (p<0) {
            throw new IllegalStateException("Unknown field " + aFieldName + " in " + aClass.getClassName().name());
        }
        return OBJECT_HEADER_SIZE + p * OBJECT_FIELDSIZE;
    }

    public static String toWASMMethodSignature(BytecodeMethodSignature aSignatutre, boolean aIsStatic) {
        String theTypeDefinition = "(func";

        if (!aIsStatic) {
            theTypeDefinition+= " (param ";
            theTypeDefinition+= WASMWriterUtils.toType(TypeRef.Native.REFERENCE);
            theTypeDefinition+=")";
        }

        for (int i=0;i<aSignatutre.getArguments().length;i++) {
            BytecodeTypeRef theParamType = aSignatutre.getArguments()[i];
            theTypeDefinition+= " (param ";
            theTypeDefinition+=WASMWriterUtils.toType(TypeRef.toType(theParamType));
            theTypeDefinition+= ")";
        }

        if (!aSignatutre.getReturnType().isVoid()) {
            theTypeDefinition+= " (result "; // result
            theTypeDefinition+=WASMWriterUtils.toType(TypeRef.toType(aSignatutre.getReturnType()));
            theTypeDefinition+=")";
        }
        theTypeDefinition+= ")";
        return theTypeDefinition;
    }
}
