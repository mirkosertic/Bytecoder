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
package de.mirkosertic.bytecoder.optimizer;

import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.RegionNode;

public class InefficientIFOptimizer implements Optimizer {

    @Override
    public void optimize(ControlFlowGraph aGraph, BytecodeLinkerContext aLinkerContext) {
        for (RegionNode theNode : aGraph.getKnownNodes()) {
            checkExpressions(aGraph, theNode, theNode.getExpressions());
        }
    }

    private void checkExpressions(ControlFlowGraph aGraph, RegionNode aNode, ExpressionList aList) {
/*        for (Expression theExpression : aList.toList()) {
            if (theExpression instanceof IFExpression) {
                IFExpression theIF = (IFExpression) theExpression;
                Value theBooleanValue = theIF.getBooleanValue();
                if (theBooleanValue instanceof BinaryExpression) {
                    BinaryExpression theBinary = (BinaryExpression) theBooleanValue;
                    Value theFirst = theBinary.resolveFirstArgument();
                    Value theSecond = theBinary.resolveSecondArgument();

                    if ((theFirst instanceof Variable) && (theSecond instanceof IntegerValue)){
                        List<Value> theInits = theFirst.consumedValues(Value.ConsumptionType.INITIALIZATION);
                        if (theInits.size() == 1) {
                            Value theFirstValue = theInits.get(0);
                            if (theFirstValue instanceof CompareExpression) {
                                CompareExpression theCompare = (CompareExpression) theFirstValue;
                                Value theCompareA = theCompare.resolveFirstArgument();
                                Value theCompareB = theCompare.resolveSecondArgument();
                                IntegerValue theInteger = (IntegerValue) theSecond;
                                // We have a candidate

                                // Compare follows this logic:
                                // a == b -> 0
                                // a >= b -> 1
                                // a < b -> -1
                                if (theInteger.getIntValue() == 0) {
                                    switch (theBinary.getOperator()) {
                                        case EQUALS:
                                        case GREATEROREQUALS:
                                        case LESSTHAN:
                                        case LESSTHANOREQUALS:
                                        case GREATERTHAN: {
                                            // Unbind all
                                            theCompare.unbind();
                                            theBinary.unbind();
                                            // The new boolean expression and the new if
                                            BinaryExpression theNewBooleanValue = new BinaryExpression(theBinary.resolveType(), theCompareA, theBinary.getOperator(), theCompareB);
                                            IFExpression theNewIf = theIF.withNewBooleanValue(theNewBooleanValue);
                                            aList.replace(theIF, theNewIf);
                                            // Finally, get rid of the removed variable
                                            aGraph.getProgram().deleteVariable((Variable) theFirst);
                                            break;
                                        }
                                        default:
                                            break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }*/
    }
}
