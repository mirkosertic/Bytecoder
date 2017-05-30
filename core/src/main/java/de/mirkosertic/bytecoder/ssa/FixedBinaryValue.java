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

public class FixedBinaryValue extends Value {

    public enum Operator {
        ISNULL,
        ISZERO,
    }

    private final Variable value1;
    private final Operator operator;

    public FixedBinaryValue(Variable aValue1, Operator aOperator) {
        value1 = aValue1;
        operator = aOperator;
    }
}
