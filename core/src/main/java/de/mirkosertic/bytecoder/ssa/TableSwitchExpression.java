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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TableSwitchExpression extends Expression implements ExpressionListContainer {

    private final long lowValue;
    private final long highValue;
    private final ExpressionList defaultExpressions;
    private final BytecodeOpcodeAddress defaultJumpTarget;
    private final Map<Long, ExpressionList> offsets;

    public TableSwitchExpression(final Program aProgram, final BytecodeOpcodeAddress aAddress, final Value aValue, final long aLowValue, final long aHighValue,
            final ExpressionList aDefaultPath, final BytecodeOpcodeAddress aDefaultJumpTarget, final Map<Long, ExpressionList> aPathPerOffset) {
        super(aProgram, aAddress);
        lowValue = aLowValue;
        highValue = aHighValue;
        defaultExpressions = aDefaultPath;
        defaultJumpTarget = aDefaultJumpTarget;
        offsets = aPathPerOffset;
        receivesDataFrom(aValue);
    }

    public long getLowValue() {
        return lowValue;
    }

    public long getHighValue() {
        return highValue;
    }

    public ExpressionList getDefaultExpressions() {
        return defaultExpressions;
    }

    public Map<Long, ExpressionList> getOffsets() {
        return offsets;
    }

    @Override
    public Set<ExpressionList> getExpressionLists() {
        final Set<ExpressionList> theResult = new HashSet<>();
        theResult.add(defaultExpressions);
        theResult.addAll(offsets.values());
        return theResult;
    }

    public BytecodeOpcodeAddress getDefaultJumpTarget() {
        return defaultJumpTarget;
    }

    @Override
    public Expression deepCopy() {
        final Map<Long, ExpressionList> theHandler = new HashMap<>();
        for (final Map.Entry<Long, ExpressionList> theEntry : offsets.entrySet()) {
            theHandler.put(theEntry.getKey(), theEntry.getValue().deepCopy());
        }
        return new TableSwitchExpression(getProgram(), getAddress(), incomingDataFlows().get(0),
                lowValue, highValue, defaultExpressions.deepCopy(), defaultJumpTarget, theHandler);
    }
}