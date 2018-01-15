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

import java.util.HashSet;
import java.util.Set;

public class IFExpression extends Expression implements ExpressionListContainer {

    private final BytecodeOpcodeAddress address;
    private final ExpressionList expressions;
    private final BytecodeOpcodeAddress gotoAddress;

    public IFExpression(BytecodeOpcodeAddress aAddress, BytecodeOpcodeAddress aGotoAddress, Value aBooleanValue, ExpressionList aExpressions) {
        consume(ConsumptionType.ARGUMENT, aBooleanValue);
        address = aAddress;
        expressions = aExpressions;
        gotoAddress = aGotoAddress;
    }

    public IFExpression withNewBooleanValue(Value aBooleanValue) {
        return new IFExpression(address, gotoAddress, aBooleanValue, expressions);
    }

    public BytecodeOpcodeAddress getAddress() {
        return address;
    }

    public Value getBooleanValue() {
        return resolveFirstArgument();
    }

    public ExpressionList getExpressions() {
        return expressions;
    }

    @Override
    public Set<ExpressionList> getExpressionLists() {
        Set<ExpressionList> theResult = new HashSet<>();
        theResult.add(expressions);
        return theResult;
    }

    public BytecodeOpcodeAddress getGotoAddress() {
        return gotoAddress;
    }
}