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

import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.core.BytecodeReferenceKind;

public class MethodRefExpression extends Expression {

    private final BytecodeObjectTypeRef className;
    private String methodName;
    private BytecodeMethodSignature signature;
    private final BytecodeReferenceKind referenceKind;

    public MethodRefExpression(final Program aProgram, final BytecodeOpcodeAddress aAddress, final BytecodeObjectTypeRef className, final String methodName,
            final BytecodeMethodSignature signature, final BytecodeReferenceKind referenceKind) {
        super(aProgram, aAddress);
        this.className = className;
        this.methodName = methodName;
        this.signature = signature;
        this.referenceKind = referenceKind;
    }

    public BytecodeReferenceKind getReferenceKind() {
        return referenceKind;
    }

    public BytecodeObjectTypeRef getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public BytecodeMethodSignature getSignature() {
        return signature;
    }

    public void retargetToMethodName(final String aNewMethodName) {
        methodName = aNewMethodName;
    }

    public void retargetToSignature(final BytecodeMethodSignature aSignature) {
        signature = aSignature;
    }

    @Override
    public TypeRef resolveType() {
        return TypeRef.Native.REFERENCE;
    }
}
