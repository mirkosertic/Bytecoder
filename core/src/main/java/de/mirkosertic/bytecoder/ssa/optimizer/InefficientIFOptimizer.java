/*
 * Copyright 2018 Mirko Sertic
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
package de.mirkosertic.bytecoder.ssa.optimizer;

import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.ssa.BinaryValue;
import de.mirkosertic.bytecoder.ssa.CompareValue;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.GraphNode;
import de.mirkosertic.bytecoder.ssa.IFExpression;
import de.mirkosertic.bytecoder.ssa.IntegerValue;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;

import java.util.List;

public class InefficientIFOptimizer implements Optimizer {

    @Override
    public void optimize(ControlFlowGraph aGraph, BytecodeLinkerContext aLinkerContext) {
        for (GraphNode theNode : aGraph.getKnownNodes()) {
            checkExpressions(aGraph, theNode, theNode.getExpressions());
        }
    }

    private void checkExpressions(ControlFlowGraph aGraph, GraphNode aNode, ExpressionList aList) {
        for (Expression theExpression : aList.toList()) {
            if (theExpression instanceof IFExpression) {
                IFExpression theIF = (IFExpression) theExpression;
                Value theBooleanValue = theIF.getBooleanValue();
                if (theBooleanValue instanceof BinaryValue) {
                    BinaryValue theBinary = (BinaryValue) theBooleanValue;
                    Value theFirst = theBinary.resolveFirstArgument();
                    Value theSecond = theBinary.resolveSecondArgument();

                    if ((theFirst instanceof Variable) && (theSecond instanceof IntegerValue)){
                        List<Value> theInits = theFirst.consumedValues(Value.ConsumptionType.INITIALIZATION);
                        if (theInits.size() == 1) {
                            Value theFirstValue = theInits.get(0);
                            if (theFirstValue instanceof CompareValue) {
                                CompareValue theCompare = (CompareValue) theFirstValue;
                                IntegerValue theInteger = (IntegerValue) theSecond;
                                // We have a candidate
                                if (theInteger.getIntValue() == 0) {
                                    switch (theBinary.getOperator()) {
                                        default:
                                            break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
