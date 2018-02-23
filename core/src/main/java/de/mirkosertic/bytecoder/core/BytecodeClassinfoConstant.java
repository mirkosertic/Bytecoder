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

public class BytecodeClassinfoConstant implements BytecodeConstant {

    public static final BytecodeClassinfoConstant OBJECT_CLASS = new BytecodeClassinfoConstant(-1, null, null);

    private final int nameIndex;
    private final BytecodeConstantPool constantPool;
    private final BytecodeReplacer bytecodeReplacer;

    public BytecodeClassinfoConstant(int aNameIndex, BytecodeConstantPool aConstantPool, BytecodeReplacer aReplacer) {
        nameIndex = aNameIndex;
        constantPool = aConstantPool;
        bytecodeReplacer = aReplacer;
    }

    public BytecodeUtf8Constant getConstant() {
        BytecodeUtf8Constant theClassNameConstant = (BytecodeUtf8Constant) constantPool.constantByIndex(nameIndex - 1);
        if (bytecodeReplacer != null) {
            return bytecodeReplacer.replaceTypeIn(theClassNameConstant);
        }
        return theClassNameConstant;
    }
}
