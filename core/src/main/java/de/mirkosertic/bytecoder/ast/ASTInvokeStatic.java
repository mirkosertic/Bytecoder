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
package de.mirkosertic.bytecoder.ast;

import java.util.List;

import de.mirkosertic.bytecoder.core.BytecodeClassinfoConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethodRefConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeUtf8Constant;

public class ASTInvokeStatic extends ASTComputationResult {

    private final List<ASTValue> arguments;
    private final BytecodeMethodRefConstant methodRef;

    public ASTInvokeStatic(List<ASTValue> aArguments, BytecodeMethodRefConstant aMethodRef) {
        arguments = aArguments;
        methodRef = aMethodRef;
    }

    public List<ASTValue> getArguments() {
        return arguments;
    }

    public BytecodeClassinfoConstant getClassname() {
        return methodRef.getClassIndex().getClassConstant();
    }

    public BytecodeUtf8Constant getMethodName() {
        return methodRef.getNameAndTypeIndex().getNameAndType().getNameIndex().getName();
    }

    public BytecodeMethodSignature getSignature() {
        return methodRef.getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();
    }
}