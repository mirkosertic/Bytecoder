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

import de.mirkosertic.bytecoder.core.BytecodeInstructionIFICMP;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;

public class ASTIFIntegerCompare extends ASTValue {

    private final ASTValue value1;
    private final ASTValue value2;
    private final BytecodeInstructionIFICMP.Type type;
    private final BytecodeOpcodeAddress targetAddress;

    public ASTIFIntegerCompare(ASTValue aValue2, ASTValue aValue1, BytecodeInstructionIFICMP.Type aType, BytecodeOpcodeAddress aTargetAddress) {
        value1 = aValue1;
        value2 = aValue2;
        type = aType;
        targetAddress = aTargetAddress;
    }

    public ASTValue getValue1() {
        return value1;
    }

    public ASTValue getValue2() {
        return value2;
    }

    public BytecodeInstructionIFICMP.Type getType() {
        return type;
    }

    public BytecodeOpcodeAddress getTargetAddress() {
        return targetAddress;
    }
}
