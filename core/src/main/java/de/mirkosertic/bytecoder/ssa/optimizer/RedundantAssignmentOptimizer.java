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
import de.mirkosertic.bytecoder.ssa.ArrayEntryValue;
import de.mirkosertic.bytecoder.ssa.ArrayStoreExpression;
import de.mirkosertic.bytecoder.ssa.BinaryValue;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.GetFieldValue;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.IFExpression;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;
import de.mirkosertic.bytecoder.ssa.InvocationValue;
import de.mirkosertic.bytecoder.ssa.PutFieldExpression;
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;

import java.util.List;

public class RedundantAssignmentOptimizer implements Optimizer {

    @Override
    public void optimize(ControlFlowGraph aGraph, BytecodeLinkerContext aLinkerContext) {
        for (RegionNode theNode : aGraph.getDominatedNodes()) {
            while (optimizeExpressionList(aGraph, theNode.getExpressions(), aLinkerContext));
        }
    }

    private boolean optimizeExpressionList(ControlFlowGraph aGraph, ExpressionList aExpressions, BytecodeLinkerContext aLinkerContext) {
        List<Expression> theList = aExpressions.toList();
        for (Expression theExpression : theList) {
            if (theExpression instanceof ExpressionListContainer) {
                ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                for (ExpressionList theSubList : theContainer.getExpressionLists()) {
                    if (optimizeExpressionList(aGraph, theSubList, aLinkerContext)) {
                        return true;
                    }
                }
            }

            if (theExpression instanceof IFExpression) {
                IFExpression theIF = (IFExpression) theExpression;
                Value theBooleanValue = theIF.getBooleanValue();

                List<Value> theArguments = theBooleanValue.consumedValues(Value.ConsumptionType.ARGUMENT);
                Expression thePredecessor = aExpressions.predecessorOf(theIF);
                if (thePredecessor instanceof VariableAssignmentExpression) {
                    VariableAssignmentExpression thePred = (VariableAssignmentExpression) thePredecessor;
                    for (Value theArgument : theArguments) {
                        if (thePred.getVariable() == theArgument && thePred.getVariable().getUsageCount() == 2) {
                            theBooleanValue.replaceInConsumedValues(theArgument, thePred.getValue());
                            aExpressions.remove(thePred);

                            return true;
                        }
                    }
                }
            }

            if (theExpression instanceof ArrayStoreExpression) {
                ArrayStoreExpression theStore = (ArrayStoreExpression) theExpression;
                Value theTarget = theStore.getArray();
                Value theIndex = theStore.getIndex();
                Value theValueToPut = theStore.getValue();
                Expression thePredecessor = aExpressions.predecessorOf(theStore);
                if (thePredecessor instanceof VariableAssignmentExpression) {
                    VariableAssignmentExpression thePred = (VariableAssignmentExpression) thePredecessor;
                    if (thePred.getVariable() == theValueToPut && thePred.getVariable().getUsageCount() == 2) {
                        theStore.replaceInConsumedValues(theValueToPut, thePred.getValue());
                        aExpressions.remove(thePred);
                        return true;
                    }
                    if (thePred.getVariable() == theIndex && thePred.getVariable().getUsageCount() == 2) {
                        theStore.replaceInConsumedValues(theIndex, thePred.getValue());
                        aExpressions.remove(thePred);
                        return true;
                    }
                    if (thePred.getVariable() == theTarget && thePred.getVariable().getUsageCount() == 2) {
                        theStore.replaceInConsumedValues(theTarget, thePred.getValue());
                        aExpressions.remove(thePred);
                        return true;
                    }
                }
            }

            if (theExpression instanceof PutFieldExpression) {
                PutFieldExpression thePut = (PutFieldExpression) theExpression;
                Value theTarget = thePut.getTarget();
                Value theValueToPut = thePut.getValue();
                Expression thePredecessor = aExpressions.predecessorOf(thePut);
                if (thePredecessor instanceof VariableAssignmentExpression) {
                    VariableAssignmentExpression thePred = (VariableAssignmentExpression) thePredecessor;
                    if (thePred.getVariable() == theValueToPut && thePred.getVariable().getUsageCount() == 2) {
                        thePut.replaceInConsumedValues(theValueToPut, thePred.getValue());
                        aExpressions.remove(thePred);
                        return true;
                    }
                    if (thePred.getVariable() == theTarget && thePred.getVariable().getUsageCount() == 2) {
                        thePut.replaceInConsumedValues(theTarget, thePred.getValue());
                        aExpressions.remove(thePred);
                        return true;
                    }
                }
            }

            if (theExpression instanceof ReturnValueExpression) {
                ReturnValueExpression theReturn = (ReturnValueExpression) theExpression;
                Value theValueToReturn = theReturn.getValue();
                Expression thePredecessor = aExpressions.predecessorOf(theReturn);
                if (thePredecessor instanceof VariableAssignmentExpression) {
                    VariableAssignmentExpression thePred = (VariableAssignmentExpression) thePredecessor;
                    if (thePred.getVariable() == theValueToReturn && thePred.getVariable().getUsageCount() == 2) {
                        theReturn.replaceInConsumedValues(theValueToReturn, thePred.getValue());
                        aExpressions.remove(thePred);
                        return true;
                    }
                }
            }

            if (theExpression instanceof VariableAssignmentExpression) {
                VariableAssignmentExpression theInit = (VariableAssignmentExpression) theExpression;
                Value theValue = theInit.getValue();

                if (theValue instanceof InvocationValue) {
                    InvocationValue theInvocation = (InvocationValue) theValue;
                    List<Value> theArguments = theInvocation.consumedValues(Value.ConsumptionType.ARGUMENT);
                    Expression thePredecessor = aExpressions.predecessorOf(theInit);
                    if (thePredecessor instanceof VariableAssignmentExpression) {
                        VariableAssignmentExpression thePred = (VariableAssignmentExpression) thePredecessor;
                        for (Value theArgument : theArguments) {
                            if (thePred.getVariable() == theArgument && thePred.getVariable().getUsageCount() == 2) {
                                theInvocation.replaceInConsumedValues(theArgument, thePred.getValue());
                                aExpressions.remove(thePred);
                                return true;
                            }
                        }
                    }
                }

                if (theValue instanceof Variable) {
                    Expression thePredecessor = aExpressions.predecessorOf(theInit);
                    if (thePredecessor instanceof VariableAssignmentExpression) {
                        VariableAssignmentExpression thePred = (VariableAssignmentExpression) thePredecessor;
                        if (thePred.getVariable() == theValue) {
                            theInit.replaceInConsumedValues(theValue, thePred.getValue());
                            aExpressions.remove(thePred);
                            return true;
                        }
                    }
                }

                if (theValue instanceof BinaryValue) {
                    BinaryValue theBinary = (BinaryValue) theValue;
                    Expression thePredecessor = aExpressions.predecessorOf(theInit);
                    if (thePredecessor instanceof VariableAssignmentExpression) {
                        VariableAssignmentExpression thePred = (VariableAssignmentExpression) thePredecessor;
                        if (theBinary.resolveFirstArgument() == thePred.getVariable()
                                && thePred.getVariable().getUsageCount() == 2) {
                            theBinary.replaceInConsumedValues(thePred.getVariable(), thePred.getValue());
                            aExpressions.remove(thePred);

                            return true;
                        }
                        if (theBinary.resolveSecondArgument() == thePred.getVariable()
                                && thePred.getVariable().getUsageCount() == 2) {
                            theBinary.replaceInConsumedValues(thePred.getVariable(), thePred.getValue());
                            aExpressions.remove(thePred);

                            return true;
                        }
                    }
                }

                if (theValue instanceof ArrayEntryValue) {
                    ArrayEntryValue theArrayEntry = (ArrayEntryValue) theValue;
                    Expression thePredecessor = aExpressions.predecessorOf(theInit);
                    if (thePredecessor instanceof VariableAssignmentExpression) {
                        VariableAssignmentExpression thePred = (VariableAssignmentExpression) thePredecessor;
                        if (thePred.getVariable() == theArrayEntry.resolveFirstArgument()
                                && thePred.getVariable().getUsageCount() == 2) {
                            theArrayEntry.replaceInConsumedValues(thePred.getVariable(), thePred.getValue());
                            aExpressions.remove(thePred);

                            return true;
                        }
                        if (thePred.getVariable() == theArrayEntry.resolveSecondArgument()
                                && thePred.getVariable().getUsageCount() == 2) {
                            theArrayEntry.replaceInConsumedValues(thePred.getVariable(), thePred.getValue());
                            aExpressions.remove(thePred);

                            return true;
                        }
                    }
                }

                if (theValue instanceof GetFieldValue) {
                    GetFieldValue theGetVield = (GetFieldValue) theValue;
                    Expression thePredecessor = aExpressions.predecessorOf(theInit);
                    if (thePredecessor instanceof VariableAssignmentExpression) {
                        VariableAssignmentExpression thePred = (VariableAssignmentExpression) thePredecessor;
                        if (thePred.getVariable() == theGetVield.resolveFirstArgument()
                                && thePred.getVariable().getUsageCount() == 2) {
                            theGetVield.replaceInConsumedValues(thePred.getVariable(), thePred.getValue());
                            aExpressions.remove(thePred);

                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}