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

import de.mirkosertic.bytecoder.api.AnyTypeMatches;

public class BytecodeMethodSignature {

    private final BytecodeTypeRef returnType;
    private final BytecodeTypeRef[] arguments;

    public BytecodeMethodSignature(final BytecodeTypeRef aReturnType, final BytecodeTypeRef[] aArguments) {
        this.returnType = aReturnType;
        this.arguments = aArguments;
    }

    public BytecodeTypeRef getReturnType() {
        return returnType;
    }

    public BytecodeTypeRef[] getArguments() {
        return arguments;
    }

    public boolean matchesExactlyTo(final BytecodeMethodSignature aSignature) {
        if (arguments.length != aSignature.arguments.length) {
            return false;
        }

        if (!returnType.matchesExactlyTo(aSignature.getReturnType())) {
            return false;
        }
        for (int i=0;i<arguments.length;i++) {
            if (!arguments[i].matchesExactlyTo(aSignature.arguments[i])) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        final StringBuilder theBuilder = new StringBuilder(returnType.name());
        theBuilder.append("(");
        for (int i=0;i<arguments.length;i++) {
            if (i>0) {
                theBuilder.append(",");
            }
            theBuilder.append(arguments[i].name());
        }
        theBuilder.append(")");
        return theBuilder.toString();
    }

    public boolean containsAnyMatches() {
        if (isAnyTypeMatchesRef(returnType)) {
            return true;
        }
        for (final BytecodeTypeRef theType : arguments) {
            if (isAnyTypeMatchesRef(theType)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAnyTypeMatchesRef(final BytecodeTypeRef aType) {
        if (aType instanceof BytecodeObjectTypeRef) {
            final BytecodeObjectTypeRef theO = (BytecodeObjectTypeRef) aType;
            if (theO.name().equals(AnyTypeMatches.class.getName())) {
                return true;
            }
        }
        return false;
    }
}