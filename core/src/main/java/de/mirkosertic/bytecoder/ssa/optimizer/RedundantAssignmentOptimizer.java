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
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.GraphNode;
import de.mirkosertic.bytecoder.ssa.InitVariableExpression;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;

import java.util.List;

public class RedundantAssignmentOptimizer implements Optimizer {

    @Override
    public void optimize(ControlFlowGraph aGraph, BytecodeLinkerContext aLinkerContext) {
        for (GraphNode theNode : aGraph.getDominatedNodes()) {
            while (optimizeExpressionList(aGraph, theNode.getExpressions(), aLinkerContext));
        }
    }

    private boolean optimizeExpressionList(ControlFlowGraph aGraph, ExpressionList aExpressions, BytecodeLinkerContext aLinkerContext) {
        List<Expression> theList = aExpressions.toList();
        for (int i=0; i<theList.size(); i++) {
            Expression theExpression = theList.get(i);
            if (theExpression instanceof ExpressionListContainer) {
                ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                for (ExpressionList theSubList : theContainer.getExpressionLists()) {
                    if (optimizeExpressionList(aGraph, theSubList, aLinkerContext)) {
                        return true;
                    }
                }
            }
            if (theExpression instanceof InitVariableExpression) {
                InitVariableExpression theInit = (InitVariableExpression) theExpression;
                Value theValue = theInit.getValue();

                if (theValue instanceof Variable) {
                    Expression thePredecessor = aExpressions.predecessorOf(theInit);
                    if (thePredecessor instanceof InitVariableExpression) {
                        InitVariableExpression thePred = (InitVariableExpression) thePredecessor;
                        if (thePred.getVariable() == theValue) {
                            theInit.replaceInConsumedValues(theValue, thePred.getValue());
                            aExpressions.remove(thePred);
                            return true;
                        }
                    }
                }

                /*if (theValue instanceof ArrayEntryValue) {
                    ArrayEntryValue theArrayEntry = (ArrayEntryValue) theValue;
                    Expression thePredecessor = aExpressions.predecessorOf(theInit);
                    if (thePredecessor instanceof InitVariableExpression) {
                        InitVariableExpression thePred = (InitVariableExpression) thePredecessor;
                        if (thePred.getVariable() == theArrayEntry.resolveFirstArgument()) {
                            theArrayEntry.replaceInConsumedValues(thePred.getVariable(), thePred.getValue());
                            aExpressions.remove(thePred);
                            return true;
                        }
                    }
                }*/
            }
        }
        return false;
    }
}