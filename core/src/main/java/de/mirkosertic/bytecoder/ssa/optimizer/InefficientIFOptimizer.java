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
import de.mirkosertic.bytecoder.ssa.Block;
import de.mirkosertic.bytecoder.ssa.CompareValue;
import de.mirkosertic.bytecoder.ssa.DoubleValue;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.FloatValue;
import de.mirkosertic.bytecoder.ssa.IFExpression;
import de.mirkosertic.bytecoder.ssa.InitVariableExpression;
import de.mirkosertic.bytecoder.ssa.IntegerValue;
import de.mirkosertic.bytecoder.ssa.PrimitiveValue;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.Variable;

public class InefficientIFOptimizer implements Optimizer {

    @Override
    public void optimize(Program aProgram, BytecodeLinkerContext aLinkerContext) {
        for (Block theBlock : aProgram.getBlocks()) {
            optimizeExpressionList(theBlock.getExpressions(), aLinkerContext);
        }
    }

    private void optimizeExpressionList(ExpressionList aExpressions, BytecodeLinkerContext aLinkerContext) {
        for (Expression theExpression : aExpressions.toList()) {

            if (theExpression instanceof IFExpression) {
                IFExpression theIf = (IFExpression) theExpression;

                Variable theBoolean = theIf.getBooleanExpression();
                if (theBoolean.getValue() instanceof BinaryValue) {
                    BinaryValue theBinary = (BinaryValue) theBoolean.getValue();
                    if (theBinary.getValue1().getValue() instanceof CompareValue && theBinary.getValue2().getValue() instanceof PrimitiveValue) {
                        CompareValue theCompare = (CompareValue) theBinary.getValue1().getValue();
                        PrimitiveValue thePrimitive = (PrimitiveValue) theBinary.getValue2().getValue();

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
                            switch (theBinary.getOperator()) {
                                case LESSTHANOREQUALS: {
                                    // Means value1 == value2 || value1 < value2
                                    // hence value1<=value2
                                    theBoolean.setValue(
                                            new BinaryValue(theCompare.getValue1(), BinaryValue.Operator.LESSTHANOREQUALS,
                                                    theCompare.getValue2()));

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
