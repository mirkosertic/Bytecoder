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

public class BinaryValue extends Value {

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

    private final Variable value1;
    private final Operator operator;
    private final Variable value2;

    public BinaryValue(Variable aValue1, Operator aOperator, Variable aValue2) {
        value1 = aValue1.usedBy(this);
        operator = aOperator;
        value2 = aValue2.usedBy(this);
    }

    public Variable getValue1() {
        return value1;
    }

    public Operator getOperator() {
        return operator;
    }

    public Variable getValue2() {
        return value2;
    }
}
