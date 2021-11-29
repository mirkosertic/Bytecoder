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

public class BytecodeInstructionGETFIELD extends BytecodeInstruction {

    private final int poolIndex;
    private final BytecodeConstantPool constantPool;

    public BytecodeInstructionGETFIELD(final BytecodeOpcodeAddress aOpcodeIndex, final int aConstantPoolIndex, final BytecodeConstantPool aConstantPool) {
        super(aOpcodeIndex);
        poolIndex = aConstantPoolIndex;
        constantPool = aConstantPool;
    }

    public BytecodeFieldRefConstant getFieldRefConstant() {
        return (BytecodeFieldRefConstant) constantPool.constantByIndex(poolIndex - 1);
    }

    @Override
    public void performLinking(final BytecodeClass aOwningClass, final BytecodeLinkerContext aLinkerContext, final AnalysisStack analysisStack) {
        final BytecodeFieldRefConstant theFieldRef = getFieldRefConstant();

        final BytecodeClassinfoConstant theClass = theFieldRef.getClassIndex().getClassConstant();
        final BytecodeNameIndex theName = theFieldRef.getNameAndTypeIndex().getNameAndType().getNameIndex();

        final BytecodeObjectTypeRef className = BytecodeObjectTypeRef.fromUtf8Constant(theClass.getConstant());
        final AnalysisStack.Frame currentFrame = analysisStack.fieldAccess(className, theName.getName().stringValue());

        try {
            final BytecodeLinkedClass theLinkedClass = aLinkerContext.resolveClass(className, analysisStack);
            if (!theLinkedClass.resolveInstanceField(theName.getName(), analysisStack)) {
                throw new MissingLinkException("Cannot find instance field. Analysis stack is \n" + analysisStack.toDebugOutput());
            }
        } finally {
            currentFrame.close();
        }
    }
}
