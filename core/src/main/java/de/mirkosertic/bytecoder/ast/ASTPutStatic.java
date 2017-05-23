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

import de.mirkosertic.bytecoder.core.BytecodeClassinfoConstant;
import de.mirkosertic.bytecoder.core.BytecodeFieldRefConstant;
import de.mirkosertic.bytecoder.core.BytecodeUtf8Constant;

public class ASTPutStatic extends ASTValue {

    private final ASTValue argument;
    private final BytecodeFieldRefConstant fieldRef;

    public ASTPutStatic(ASTValue aArgument, BytecodeFieldRefConstant aFieldRef) {
        argument = aArgument;
        fieldRef = aFieldRef;
    }

    public ASTValue getArgument() {
        return argument;
    }

    public BytecodeClassinfoConstant getClassName() {
        return fieldRef.getClassIndex().getClassConstant();
    }

    public BytecodeUtf8Constant getFieldName() {
        return fieldRef.getNameAndTypeIndex().getNameAndType().getNameIndex().getName();
    }
}
