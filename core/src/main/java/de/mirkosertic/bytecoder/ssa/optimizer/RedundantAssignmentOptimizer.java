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
import de.mirkosertic.bytecoder.graph.Edge;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.DataFlowEdgeType;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

import java.util.List;
import java.util.stream.Collectors;

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

            // Check if a variable assignment is before the current expression
            Expression theBefore = aExpressions.predecessorOf(theExpression);
            if (theBefore instanceof VariableAssignmentExpression) {
                VariableAssignmentExpression theAssignment = (VariableAssignmentExpression) theBefore;
                List<Value> theConsumedValues = theAssignment.incomingDataFlows();
                Variable theVariable = (Variable) theConsumedValues.get(0);
                Value theValue = theConsumedValues.get(1);

                // Check if there is only one data flow
                List<Edge> theDataEdges = theVariable.outgoingEdges(DataFlowEdgeType.filter()).collect(Collectors.toList());
                if (theDataEdges.size() == 1) {
                    List<Value> theIncomingData = theExpression.incomingDataFlows();
                    if (theIncomingData.contains(theVariable)) {
                        aExpressions.remove(theAssignment);
                        theDataEdges.get(0).newSourceIs(theValue);
                        aGraph.getProgram().deleteVariable(theVariable);

                        return true;
                    }
                }
            }
        }
        return false;
    }
}