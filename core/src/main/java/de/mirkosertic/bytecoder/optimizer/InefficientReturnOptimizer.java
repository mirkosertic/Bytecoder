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
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

import java.util.List;
import java.util.stream.Collectors;

public class InefficientReturnOptimizer extends RecursiveExpressionVisitor implements Optimizer {

    @Override
    public void optimize(final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext) {
        visit(aGraph, aLinkerContext);
    }

    @Override
    protected void visit(final ControlFlowGraph aGraph, final ExpressionList aList, final Expression aExpression, final BytecodeLinkerContext aLinkerContext) {
        if (aExpression instanceof ReturnValueExpression) {
            final ReturnValueExpression theReturn = (ReturnValueExpression) aExpression;
            final Value theReturnedValue = theReturn.incomingDataFlows().get(0);
            final Expression theBefore = aList.predecessorOf(aExpression);
            if (theBefore instanceof VariableAssignmentExpression) {
                final VariableAssignmentExpression theAssignment = (VariableAssignmentExpression) theBefore;
                final Variable theVariable = theAssignment.getVariable();
                final Value theVariableValue = theAssignment.getValue();
                final List<Edge> theDataEdges = theVariable.outgoingEdges(DataFlowEdgeType.filter()).collect(Collectors.toList());
                if (theDataEdges.size() == 1) {
                    if (theReturnedValue == theVariable) {
                        // We can delete the Variable and the Variable Assignment
                        // and replace the assigned Value in the IF condition
                        aGraph.getProgram().deleteVariable(theVariable);

                        theReturn.replaceIncomingDataEdge(theReturnedValue, theVariableValue);
                        aList.remove(theAssignment);
                    }
                }
            }
        }
    }
}
