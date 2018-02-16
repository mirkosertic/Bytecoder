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

    public BytecodeVirtualMethodIdentifier identifierFor(String aMethodName, BytecodeMethodSignature aSignature) {
        String theSignature = toSignature(aMethodName, aSignature);
        BytecodeVirtualMethodIdentifier theIdentifier = knownIdentifier.get(theSignature);
        if (theIdentifier == null) {
            theIdentifier = new BytecodeVirtualMethodIdentifier(knownIdentifier.size());
            knownIdentifier.put(theSignature, theIdentifier);
        }
        return theIdentifier;
    }

    public BytecodeVirtualMethodIdentifier identifierFor(BytecodeMethod aMethod) {
        return identifierFor(aMethod.getName().stringValue(), aMethod.getSignature());
    }

    public BytecodeVirtualMethodIdentifier toIdentifier(String aMethodName, BytecodeMethodSignature aSignature) {
        return knownIdentifier.get(toSignature(aMethodName, aSignature));
    }

    private String toSignature(String aMethodName, BytecodeMethodSignature aSignature) {
        StringBuilder theStringBuilder = new StringBuilder();
        theStringBuilder.append(toSignature(aSignature.getReturnType()));
        theStringBuilder.append(aMethodName);
        for (BytecodeTypeRef theRef : aSignature.getArguments()) {
            theStringBuilder.append(toSignature(theRef));
        }
        return theStringBuilder.toString();
    }

    private String toSignature(BytecodeTypeRef aTypeRef) {
        if (aTypeRef instanceof BytecodeObjectTypeRef) {
            BytecodeObjectTypeRef theTypeRef = (BytecodeObjectTypeRef) aTypeRef;
            return theTypeRef.name().replace(".","_");
        }
        if (aTypeRef instanceof BytecodePrimitiveTypeRef) {
            BytecodePrimitiveTypeRef theRef = (BytecodePrimitiveTypeRef) aTypeRef;
            return theRef.name();
        }
        BytecodeArrayTypeRef theArrayRef = (BytecodeArrayTypeRef) aTypeRef;
        return "A" + theArrayRef.getDepth() + toSignature(theArrayRef.getType());
    }
}
