/*
 * Copyright 2019 Mirko Sertic
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

import java.util.Collections;
import java.util.Set;

public class IFElseExpression extends Expression implements ExpressionListContainer {

    private final IFExpression condition;
    private final ExpressionList elsePart;

    public IFElseExpression(final Program program, final BytecodeOpcodeAddress address,
            final IFExpression condition, final ExpressionList elsePart) {
        super(program, address);
        this.condition = condition;
        this.elsePart = elsePart;
    }

    public IFExpression getCondition() {
        return condition;
    }

    public ExpressionList getElsePart() {
        return elsePart;
    }

    @Override
    public Set<ExpressionList> getExpressionLists() {
        return Collections.singleton(elsePart);
    }

    @Override
    public Expression deepCopy() {
        return new IFElseExpression(getProgram(), getAddress(), (IFExpression) condition.deepCopy(), elsePart.deepCopy());
    }
}
