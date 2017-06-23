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

import de.mirkosertic.bytecoder.core.BytecodeFieldRefConstant;

public class PutFieldExpression extends Expression {

    private final BytecodeFieldRefConstant field;
    private final Variable target;
    private final Variable value;

    public PutFieldExpression(BytecodeFieldRefConstant aField, Variable aTarget, Variable aValue) {
        field = aField;
        target = aTarget.usedBy(this);
        value = aValue.usedBy(this);
    }

    public BytecodeFieldRefConstant getField() {
        return field;
    }

    public Variable getTarget() {
        return target;
    }

    public Variable getValue() {
        return value;
    }
}
