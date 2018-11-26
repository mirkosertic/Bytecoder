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
import de.mirkosertic.bytecoder.graph.Edge;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.DataFlowEdgeType;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.RecursiveExpressionVisitor;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

import java.util.List;
import java.util.stream.Collectors;

public class RedundantAssignmentOptimizer extends RecursiveExpressionVisitor implements Optimizer {

    @Override
    public void optimize(final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext) {
        visit(aGraph, aLinkerContext);
    }

    @Override
    protected void visit(final ControlFlowGraph aGraph, final ExpressionList aList, final Expression aExpression, final BytecodeLinkerContext aLinkerContext) {
        // Check if a variable assignment is before the current expression
        if (aExpression instanceof VariableAssignmentExpression) {
            final Expression theBefore = aList.predecessorOf(aExpression);
            if (theBefore instanceof VariableAssignmentExpression) {
                final VariableAssignmentExpression thePrevAssignment = (VariableAssignmentExpression) theBefore;
                final Variable theVariable = thePrevAssignment.getVariable();
                final Value theValue = thePrevAssignment.getValue();

                // Check if there is only one data flow
                final List<Edge> theDataEdges = theVariable.outgoingEdges(DataFlowEdgeType.filter()).collect(Collectors.toList());
                if (theDataEdges.size() == 1) {
                    final List<Value> theIncomingData = aExpression.incomingDataFlows();
                    if (theIncomingData.contains(theVariable)) {
                        aExpression.replaceIncomingDataEdge(theVariable, theValue);
                        aList.remove(thePrevAssignment);
                        aGraph.getProgram().deleteVariable(theVariable);
                    }
                }
            }
        }
    }
}