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

public class MethodHandleExpression extends Expression {

    public static class AdapterAnnotation  {
        private final BytecodeMethodSignature linkageSignature;
        private final BytecodeMethodSignature captureSignature;
        private final BytecodeMethodSignature samMethodType;

        public AdapterAnnotation(final BytecodeMethodSignature linkageSignature, final BytecodeMethodSignature captureSignature,
                                 final BytecodeMethodSignature samMethodType) {
            this.linkageSignature = linkageSignature;
            this.captureSignature = captureSignature;
            this.samMethodType = samMethodType;
        }

        public BytecodeMethodSignature getLinkageSignature() {
            return linkageSignature;
        }

        public BytecodeMethodSignature getCaptureSignature() {
            return captureSignature;
        }

        public BytecodeMethodSignature getSamMethodType() {
            return samMethodType;
        }
    }

    private final BytecodeObjectTypeRef className;
    private final String methodName;
    private final BytecodeMethodSignature implementationSignature;
    private final BytecodeReferenceKind referenceKind;
    private AdapterAnnotation adapterAnnotation;

    public MethodHandleExpression(final Program aProgram, final BytecodeOpcodeAddress aAddress, final BytecodeObjectTypeRef className, final String methodName,
                                  final BytecodeMethodSignature implementationSignature, final BytecodeReferenceKind referenceKind) {
        super(aProgram, aAddress);
        this.className = className;
        this.methodName = methodName;
        this.implementationSignature = implementationSignature;
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

    public BytecodeMethodSignature getImplementationSignature() {
        return implementationSignature;
    }

    public AdapterAnnotation getAdapterAnnotation() {
        return adapterAnnotation;
    }

    public void setAdapterAnnotation(final AdapterAnnotation aAdapterAnnotation) {
        adapterAnnotation = aAdapterAnnotation;
    }

    @Override
    public TypeRef resolveType() {
        return TypeRef.Native.REFERENCE;
    }
}
