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

    private final Operator operator;

    public BinaryValue(Value aValue1, Operator aOperator, Value aValue2) {
        operator = aOperator;
        consume(ConsumptionType.ARGUMENT, aValue1);
        consume(ConsumptionType.ARGUMENT, aValue2);
    }

    public Operator getOperator() {
        return operator;
    }

    @Override
    public TypeRef resolveType() {
        // In case of comparison and bit-operation, the resulting type is clear
        switch (operator) {
            case EQUALS:
                return TypeRef.Native.BOOLEAN;
            case LESSTHAN:
                return TypeRef.Native.BOOLEAN;
            case GREATERTHAN:
                return TypeRef.Native.BOOLEAN;
            case GREATEROREQUALS:
                return TypeRef.Native.BOOLEAN;
            case LESSTHANOREQUALS:
                return TypeRef.Native.BOOLEAN;
            case NOTEQUALS:
                return TypeRef.Native.BOOLEAN;
            case BINARYXOR:
                return TypeRef.Native.INT;
            case BINARYAND:
                return TypeRef.Native.INT;
            case BINARYSHIFTLEFT:
                return TypeRef.Native.INT;
            case BINARYUNSIGNEDSHIFTRIGHT:
                return TypeRef.Native.INT;
            case BINARYSHIFTRIGHT:
                return TypeRef.Native.INT;
        }
        TypeRef.Native theTypeOf1 = resolveFirstArgument().resolveType().resolve();
        TypeRef.Native theTypeOf2 = resolveSecondArgument().resolveType().resolve();
        switch (theTypeOf1) {
            case BYTE:
                switch (theTypeOf2) {
                    case BYTE:
                        return TypeRef.Native.BYTE;
                    case SHORT:
                        return TypeRef.Native.SHORT;
                    case INT:
                        return TypeRef.Native.INT;
                    case LONG:
                        return TypeRef.Native.LONG;
                    case FLOAT:
                        return TypeRef.Native.FLOAT;
                    case DOUBLE:
                        return TypeRef.Native.DOUBLE;
                    default:
                        throw new IllegalStateException("Not supported second type " + theTypeOf2);
                }
            case SHORT:
                switch (theTypeOf2) {
                    case BYTE:
                        return TypeRef.Native.SHORT;
                    case SHORT:
                        return TypeRef.Native.SHORT;
                    case INT:
                        return TypeRef.Native.INT;
                    case LONG:
                        return TypeRef.Native.LONG;
                    case FLOAT:
                        return TypeRef.Native.FLOAT;
                    case DOUBLE:
                        return TypeRef.Native.DOUBLE;
                    default:
                        throw new IllegalStateException("Not supported second type " + theTypeOf2);
                }
            case INT:
                switch (theTypeOf2) {
                    case BYTE:
                        return TypeRef.Native.INT;
                    case SHORT:
                        return TypeRef.Native.INT;
                    case INT:
                        return TypeRef.Native.INT;
                    case LONG:
                        return TypeRef.Native.LONG;
                    case FLOAT:
                        return TypeRef.Native.FLOAT;
                    case DOUBLE:
                        return TypeRef.Native.DOUBLE;
                    default:
                        throw new IllegalStateException("Not supported second type " + theTypeOf2);
                }
            case LONG:
                switch (theTypeOf2) {
                    case BYTE:
                        return TypeRef.Native.LONG;
                    case SHORT:
                        return TypeRef.Native.LONG;
                    case INT:
                        return TypeRef.Native.LONG;
                    case LONG:
                        return TypeRef.Native.LONG;
                    case FLOAT:
                        return TypeRef.Native.FLOAT;
                    case DOUBLE:
                        return TypeRef.Native.DOUBLE;
                    default:
                        throw new IllegalStateException("Not supported second type " + theTypeOf2);
                }
            case FLOAT:
                switch (theTypeOf2) {
                    case BYTE:
                        return TypeRef.Native.FLOAT;
                    case SHORT:
                        return TypeRef.Native.FLOAT;
                    case INT:
                        return TypeRef.Native.FLOAT;
                    case LONG:
                        return TypeRef.Native.FLOAT;
                    case FLOAT:
                        return TypeRef.Native.FLOAT;
                    case DOUBLE:
                        return TypeRef.Native.DOUBLE;
                    default:
                        throw new IllegalStateException("Not supported second type " + theTypeOf2);
                }
            case DOUBLE:
                switch (theTypeOf2) {
                    case BYTE:
                        return TypeRef.Native.DOUBLE;
                    case SHORT:
                        return TypeRef.Native.DOUBLE;
                    case INT:
                        return TypeRef.Native.DOUBLE;
                    case LONG:
                        return TypeRef.Native.DOUBLE;
                    case FLOAT:
                        return TypeRef.Native.DOUBLE;
                    case DOUBLE:
                        return TypeRef.Native.DOUBLE;
                    default:
                        throw new IllegalStateException("Not supported second type " + theTypeOf2);
                }
            default:
                throw new IllegalStateException("Not supported first type : " + theTypeOf1);
        }
    }
}
