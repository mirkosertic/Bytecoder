/*
 * Copyright 2023 Mirko Sertic
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
package de.mirkosertic.bytecoder.core.backend.opencl;

import org.objectweb.asm.Type;

public class OpenCLHelpers {

    public static String toType(final Type type) {
        /*if (aType.isArray()) {
            final TypeRef.ArrayTypeRef theArray = (TypeRef.ArrayTypeRef) aType;
            return toType(TypeRef.toType(theArray.arrayType().getType()));
        }
        if (aType instanceof TypeRef.ObjectTypeRef) {
            final TypeRef.ObjectTypeRef theObject = (TypeRef.ObjectTypeRef) aType;
            return toStructName(theObject.objectType());
        }*/
        switch (type.getSort()) {
            case Type.ARRAY:
                return toType(type.getElementType());
            case Type.INT:
                return "int";
            case Type.FLOAT:
                return "float";
            case Type.LONG:
                return "long";
            case Type.DOUBLE:
                return "double";
            case Type.SHORT:
                return "short";
            case Type.BYTE:
                return "byte";
            case Type.OBJECT:
                return "void*";
            default:
                throw new IllegalArgumentException("Not supported : " + type);
        }
    }
}
