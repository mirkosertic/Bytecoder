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
package de.mirkosertic.bytecoder.ssa.optimizer;

import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.ssa.BinaryValue;
import de.mirkosertic.bytecoder.ssa.CompareValue;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.DoubleValue;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.FloatValue;
import de.mirkosertic.bytecoder.ssa.GraphNode;
import de.mirkosertic.bytecoder.ssa.IFExpression;
import de.mirkosertic.bytecoder.ssa.InitVariableExpression;
import de.mirkosertic.bytecoder.ssa.IntegerValue;
import de.mirkosertic.bytecoder.ssa.PrimitiveValue;
import de.mirkosertic.bytecoder.ssa.Variable;

public class InefficientIFOptimizer implements Optimizer {

    @Override
    public void optimize(ControlFlowGraph aGraph, BytecodeLinkerContext aLinkerContext) {
        for (GraphNode theNode : aGraph.getDominatedNodes()) {
            optimizeExpressionList(theNode.getExpressions(), aLinkerContext);
        }
    }

    private void optimizeExpressionList(ExpressionList aExpressions, BytecodeLinkerContext aLinkerContext) {
        for (Expression theExpression : aExpressions.toList()) {

            if (theExpression instanceof IFExpression) {
                IFExpression theIf = (IFExpression) theExpression;
                if (!(theIf.getBooleanValue() instanceof Variable)) {
                    continue;
                }
                Variable theBoolean = (Variable) theIf.getBooleanValue();
                if (theBoolean.getValue() instanceof BinaryValue) {
                    BinaryValue theBinary = (BinaryValue) theBoolean.getValue();
                    Variable theBValue1 = theBinary.resolveFirstArgument();
                    Variable theBValue2 = theBinary.resolveSecondArgument();
                    if (theBValue1.getValue() instanceof CompareValue && theBValue2.getValue() instanceof PrimitiveValue) {
                        CompareValue theCompare = (CompareValue) theBValue1.getValue();
                        PrimitiveValue thePrimitive = (PrimitiveValue) theBValue2.getValue();

                        boolean isZero = false;
                        if (thePrimitive instanceof IntegerValue) {
                            isZero = ((IntegerValue) thePrimitive).getIntValue() == 0;
                        }
                        if (thePrimitive instanceof FloatValue) {
                            isZero = ((FloatValue) thePrimitive).getFloatValue() == 0;
                        }
                        if (thePrimitive instanceof DoubleValue) {
                            isZero = ((DoubleValue) thePrimitive).getDoubleValue() == 0;
                        }

                        if (isZero) {
                            Variable theValue1 = theCompare.resolveFirstArgument();
                            Variable theValue2 = theCompare.resolveSecondArgument();
                            switch (theBinary.getOperator()) {
                                case GREATEROREQUALS: {
                                    // Means value1 == value2 || value2 > value2
                                    // hence value1>=value2;
                                    theBoolean.setValue(
                                            new BinaryValue(theValue1, BinaryValue.Operator.GREATEROREQUALS,
                                                    theValue2));

                                    for (Expression theE : aExpressions.toList()) {
                                        if (theE instanceof InitVariableExpression) {
                                            InitVariableExpression theI = (InitVariableExpression) theE;
                                            if (theI.getVariable().getValue() == theCompare) {
                                                aExpressions.remove(theE);
                                            }
                                        }
                                    }
                                    break;
                                }
                                case LESSTHANOREQUALS: {
                                    // Means value1 == value2 || value1 < value2
                                    // hence value1<=value2
                                    theBoolean.setValue(
                                            new BinaryValue(theValue1, BinaryValue.Operator.LESSTHANOREQUALS,
                                                    theValue2));

                                    for (Expression theE : aExpressions.toList()) {
                                        if (theE instanceof InitVariableExpression) {
                                            InitVariableExpression theI = (InitVariableExpression) theE;
                                            if (theI.getVariable().getValue() == theCompare) {
                                                aExpressions.remove(theE);
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (theExpression instanceof ExpressionListContainer) {
                ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                for (ExpressionList theSub : theContainer.getExpressionLists()) {
                    optimizeExpressionList(theSub, aLinkerContext);
                }
            }
        }
    }
}
