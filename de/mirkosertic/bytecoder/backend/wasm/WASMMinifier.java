/*
 * Copyright 2019 Mirko Sertic
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

import de.mirkosertic.bytecoder.backend.Minifier;
import de.mirkosertic.bytecoder.classlib.Array;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeClassinfoConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;

public class WASMMinifier implements Minifier {

    @Override
    public String toClassName(final BytecodeObjectTypeRef aTypeRef) {
        if (aTypeRef.name().endsWith(";")) {
            // This seems to be an array
            return toClassName(BytecodeObjectTypeRef.fromRuntimeClass(Array.class));
        }
        return toClassNameInternal(aTypeRef.name());
    }

    @Override
    public String toClassName(final BytecodeClassinfoConstant aTypeRef) {
        return toClassNameInternal(aTypeRef.getConstant().stringValue().replace("/", "."));
    }

    private String toClassNameInternal(final String aClassName) {
        final int p = aClassName.lastIndexOf(".");
        final String theSimpleName = aClassName.substring(p + 1);
        String thePackageName = aClassName.substring(0, p);
        final StringBuilder theResult = new StringBuilder();
        while (thePackageName.length() > 0) {
            theResult.append(Character.toLowerCase(thePackageName.charAt(0)));
            final int j = thePackageName.indexOf(".");
            if (j >= 0) {
                thePackageName = thePackageName.substring(j + 1);
            } else {
                thePackageName = "";
            }
        }

        return theResult.append(theSimpleName).toString();
    }

    @Override
    public String toMethodName(final String aMethodName, final BytecodeMethodSignature aSignature) {
        String theName = typeRefToString(aSignature.getReturnType());
        theName += aMethodName.replace("<", "").replace(">", "");

        for (final BytecodeTypeRef theTypeRef : aSignature.getArguments()) {
            theName += typeRefToString(theTypeRef);
        }
        return theName;
    }

    @Override
    public String typeRefToString(final BytecodeTypeRef aTypeRef) {
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

    @Override
    public String toVariableName(final String aVariable) {
        return aVariable;
    }
}
