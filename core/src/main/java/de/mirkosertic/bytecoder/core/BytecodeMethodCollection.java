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


import java.util.HashMap;
import java.util.Map;

public class BytecodeMethodCollection {

    private final Map<String, BytecodeVirtualMethodIdentifier> knownIdentifier;

    public BytecodeMethodCollection() {
        knownIdentifier = new HashMap<>();
    }

    public BytecodeVirtualMethodIdentifier identifierFor(BytecodeMethod aMethod) {
        String theSignature = toSignature(aMethod);
        BytecodeVirtualMethodIdentifier theIdentifier = knownIdentifier.get(theSignature);
        if (theIdentifier == null) {
            theIdentifier = new BytecodeVirtualMethodIdentifier(knownIdentifier.size());
            knownIdentifier.put(theSignature, theIdentifier);
        }
        return theIdentifier;
    }

    private String toSignature(BytecodeMethod aMethod) {
        StringBuilder theStringBuilder = new StringBuilder();
        BytecodeMethodSignature theSignature = aMethod.getSignature();
        theStringBuilder.append(toSignature(theSignature.getReturnType()));
        theStringBuilder.append(aMethod.getName());
        for (BytecodeTypeRef theRef : theSignature.getArguments()) {
            theStringBuilder.append(toSignature(theRef));
        }
        return theStringBuilder.toString();
    }

    private String toSignature(BytecodeTypeRef aTypeRef) {
        if (aTypeRef instanceof BytecodeObjectTypeRef) {
            // We dont care for concrete type here!
            return "Object";
        }
        if (aTypeRef instanceof BytecodePrimitiveTypeRef) {
            BytecodePrimitiveTypeRef theRef = (BytecodePrimitiveTypeRef) aTypeRef;
            return theRef.name();
        }
        BytecodeArrayTypeRef theArrayRef = (BytecodeArrayTypeRef) aTypeRef;
        return "A" + theArrayRef.getDepth() + toSignature(theArrayRef.getType());
    }
}
