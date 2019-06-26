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

import de.mirkosertic.bytecoder.classlib.Array;

public class BytecodeInstructionGenericLDC extends BytecodeInstruction {

    private final int constantIndex;
    private final BytecodeConstantPool constantPool;

    public BytecodeInstructionGenericLDC(final BytecodeOpcodeAddress aOpcodeIndex, final int aConstantIndex, final BytecodeConstantPool aConstantPool) {
        super(aOpcodeIndex);
        constantIndex = aConstantIndex;
        constantPool = aConstantPool;
    }

    public BytecodeConstant constant() {
        return constantPool.constantByIndex(constantIndex - 1);
    }

    @Override
    public void performLinking(final BytecodeClass aOwningClass, final BytecodeLinkerContext aLinkerContext) {
        final BytecodeConstant theConstant = constant();
        if (theConstant instanceof BytecodeStringConstant) {
            aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Array.class));

            final BytecodeObjectTypeRef theObjectTypeRef = BytecodeObjectTypeRef.fromRuntimeClass(String.class);
            aLinkerContext.resolveClass(theObjectTypeRef)
                    .resolveConstructorInvocation(new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID,
                            new BytecodeTypeRef[] {new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.BYTE, 1)}));
        }
        if (theConstant instanceof BytecodeClassinfoConstant) {
            final BytecodeClassinfoConstant theClassInfo = (BytecodeClassinfoConstant) theConstant;
            if (theClassInfo.getConstant().stringValue().startsWith("[")) {
                final BytecodeTypeRef theType = aLinkerContext.getSignatureParser().toFieldType(theClassInfo.getConstant());
                aLinkerContext.resolveTypeRef(theType);
            } else {
                aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(theClassInfo.getConstant()));
            }
        }
        if (theConstant instanceof BytecodeMethodTypeConstant) {
            final BytecodeMethodTypeConstant m = (BytecodeMethodTypeConstant) theConstant;
            final BytecodeMethodSignature theSignature = m.getDescriptorIndex().methodSignature();
            aLinkerContext.resolveTypeRef(theSignature.getReturnType());
            for (final BytecodeTypeRef ref : theSignature.getArguments()) {
                aLinkerContext.resolveTypeRef(ref);
            }
        }
    }
}
