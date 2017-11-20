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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import de.mirkosertic.bytecoder.classlib.java.lang.TArray;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeClassinfoConstant;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeUtf8Constant;
import de.mirkosertic.bytecoder.ssa.Type;

public class WASMWriterUtils {

    public static final int CLASS_HEADER_SIZE = 4;
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

    public static String toMethodSignature(BytecodeMethodSignature aSignature) {
        String theName = typeRefToString(aSignature.getReturnType()) + "_";

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

    public static String toType(BytecodeTypeRef aType) {
        return "i32";
    }

    public static String toType(Type aType) {
        switch (aType) {
            case DOUBLE:
                return "f32";
            case FLOAT:
                return "f32";
            default:
                return "i32";
        }
    }

    public static int computeObjectSizeFor(BytecodeLinkedClass aClass) {
        AtomicInteger theSize = new AtomicInteger(OBJECT_HEADER_SIZE); // 32 Bits Header for the type + 32 Bits Header for VTableOffset
        aClass.forEachMemberField( t -> {
            theSize.addAndGet(OBJECT_FIELDSIZE); // Every member is a pointer to another object or a primitive of 32 bits
        });
        return theSize.intValue();
    }

    public static int computeClassSizeFor(BytecodeLinkedClass aClass) {
        AtomicInteger theSize = new AtomicInteger(CLASS_HEADER_SIZE); // 32 Bits Header
        aClass.forEachStaticField( t -> {
            theSize.addAndGet(OBJECT_FIELDSIZE); // Every member is a pointer to another object or a primitive of 32 bits
        });
        return theSize.intValue();
    }

    public static int computeStaticFieldOffsetOf(String aFieldName, BytecodeLinkedClass aClass) {
        int theOffset = CLASS_HEADER_SIZE;
        List<Map.Entry<String, BytecodeLinkedClass.LinkedField>> theFields = new ArrayList<>();
        aClass.forEachStaticField(theFields::add);
        for (int i=0;i<theFields.size();i++) {
            Map.Entry<String, BytecodeLinkedClass.LinkedField> theField = theFields.get(i);
            if (theField.getKey().equals(aFieldName)) {
                return theOffset;
            }
            theOffset += OBJECT_FIELDSIZE;
        }
        throw new IllegalStateException("Unknown field " + aFieldName + " in " + aClass.getClassName().name());
    }

    public static int computeFieldOffsetOf(String aFieldName, BytecodeLinkedClass aClass) {
        int theOffset = CLASS_HEADER_SIZE;
        List<Map.Entry<String, BytecodeLinkedClass.LinkedField>> theFields = new ArrayList<>();
        aClass.forEachMemberField(theFields::add);
        for (int i=0;i<theFields.size();i++) {
            Map.Entry<String, BytecodeLinkedClass.LinkedField> theField = theFields.get(i);
            if (theField.getKey().equals(aFieldName)) {
                return theOffset;
            }
            theOffset += OBJECT_FIELDSIZE;
        }
        throw new IllegalStateException("Unknown field " + aFieldName + " in " + aClass.getClassName().name());
    }
}
