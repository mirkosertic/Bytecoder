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

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;

public class BinaryExpression extends Expression {

    public enum Operator {
        EQUALS,
        LESSTHAN,
        GREATERTHAN,
        GREATEROREQUALS,
        LESSTHANOREQUALS,
        NOTEQUALS,

        ADD,
        SUB,
        DIV,
        MUL,
        REMAINDER,

        BINARYXOR,
        BINARYOR,
        BINARYAND,

        BINARYSHIFTLEFT,
        BINARYUNSIGNEDSHIFTRIGHT,
        BINARYSHIFTRIGHT,
    }

    private final Operator operator;
    private final TypeRef targetType;

    public BinaryExpression(final Program aProgram, final BytecodeOpcodeAddress aAddress, final TypeRef aTargetType, final Value aValue1, final Operator aOperator, final Value aValue2) {
        super(aProgram, aAddress);
        operator = aOperator;
        targetType = aTargetType;
        receivesDataFrom(aValue1, aValue2);
    }

    public Operator getOperator() {
        return operator;
    }

    @Override
    public TypeRef resolveType() {
        return targetType;
    }
}
