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

public class BytecodeMethodSignature {

    private final BytecodeTypeRef returnType;
    private final BytecodeTypeRef[] arguments;

    public BytecodeMethodSignature(BytecodeTypeRef aReturnType, BytecodeTypeRef[] aArguments) {
        this.returnType = aReturnType;
        this.arguments = aArguments;
    }

    public BytecodeTypeRef getReturnType() {
        return returnType;
    }

    public BytecodeTypeRef[] getArguments() {
        return arguments;
    }

    public boolean matchesExactlyTo(BytecodeMethodSignature aSignature) {
        if (arguments.length != aSignature.arguments.length) {
            return false;
        }

        boolean theMatch = returnType.matchesExactlyTo(aSignature.getReturnType());
        if (!theMatch) {
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
        StringBuilder theBuilder = new StringBuilder(returnType.name());
        theBuilder.append("(");
        for (BytecodeTypeRef theArgument : arguments) {
            theBuilder.append(theArgument.name());
        }
        theBuilder.append(")");
        return theBuilder.toString();
    }
}