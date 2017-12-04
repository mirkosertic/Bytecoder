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
package de.mirkosertic.bytecoder.ssa;

import de.mirkosertic.bytecoder.classlib.Address;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;

public interface TypeRef {

    Native resolve();

    enum Native implements TypeRef {
        UNKNOWN,FLOAT,SHORT,LONG,CHAR,BOOLEAN,BYTE,INT,DOUBLE,REFERENCE,VOID,CALLSITE,MEMORYLOCATION;
        @Override
        public Native resolve() {
            return this;
        }
    }

    public static TypeRef toType(BytecodeTypeRef aTypeRef) {
        if (aTypeRef.isVoid()) {
            return Native.VOID;
        }
        if (aTypeRef.isArray()) {
            return Native.REFERENCE;
        }
        if (aTypeRef.isPrimitive()) {
            BytecodePrimitiveTypeRef thePrimitive = (BytecodePrimitiveTypeRef) aTypeRef;
            switch (thePrimitive) {
            case INT:
                return Native.INT;
            case LONG:
                return Native.LONG;
            case FLOAT:
                return Native.FLOAT;
            case DOUBLE:
                return Native.DOUBLE;
            case BYTE:
                return Native.BYTE;
            case VOID:
                return Native.VOID;
            case CHAR:
                return Native.CHAR;
            case SHORT:
                return Native.SHORT;
            case BOOLEAN:
                return Native.BOOLEAN;
            default:
                throw new IllegalStateException("Not supported : " + aTypeRef);
            }
        }
        if (BytecodeObjectTypeRef.fromRuntimeClass(Address.class).equals(aTypeRef)) {
            return TypeRef.Native.MEMORYLOCATION;
        }
        return Native.REFERENCE;
    }
}
