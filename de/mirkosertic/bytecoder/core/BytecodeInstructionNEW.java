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

public class BytecodeInstructionNEW extends BytecodeInstruction {

    private final int index;
    private final BytecodeConstantPool constantPool;

    public BytecodeInstructionNEW(BytecodeOpcodeAddress aOpcodeIndex, int aIndex, BytecodeConstantPool aConstantPool) {
        super(aOpcodeIndex);
        index = aIndex;
        constantPool = aConstantPool;
    }

    public BytecodeClassinfoConstant getClassInfoForObjectToCreate() {
        return (BytecodeClassinfoConstant) constantPool.constantByIndex(index - 1);
    }

    @Override
    public void performLinking(BytecodeClass aOwningClass, BytecodeLinkerContext aLinkerContext) {
        BytecodeObjectTypeRef theObjectType = BytecodeObjectTypeRef.fromUtf8Constant(getClassInfoForObjectToCreate().getConstant());
        aLinkerContext.resolveClass(theObjectType);
    }
}
