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
import de.mirkosertic.bytecoder.ssa.ArrayStoreExpression;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.DataFlowEdgeType;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.InvocationExpression;
import de.mirkosertic.bytecoder.ssa.PutFieldExpression;
import de.mirkosertic.bytecoder.ssa.PutStaticExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

import java.util.List;
import java.util.stream.Collectors;

public class RedundantVariablesOptimizerStage implements OptimizerStage {

    @Override
    public Expression optimize(final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext, final RegionNode aCurrentNode,
            final ExpressionList aExpressionList, final Expression aExpression) {

        if ((aExpression instanceof VariableAssignmentExpression) ||
                (aExpression instanceof InvocationExpression) ||
                (aExpression instanceof ReturnValueExpression) ||
                (aExpression instanceof PutFieldExpression) ||
                (aExpression instanceof PutStaticExpression) ||
                (aExpression instanceof ArrayStoreExpression)) {
            boolean modified = true;
            while(modified) {
                modified = false;
                final List<Value> theIncoming = aExpression.incomingDataFlowsRecursive();
                final Expression theBefore = aExpressionList.predecessorOf(aExpression);
                if (theBefore instanceof VariableAssignmentExpression) {
                    final VariableAssignmentExpression theAssignment = (VariableAssignmentExpression) theBefore;
                    final Variable theVariable = theAssignment.getVariable();
                    final List<Edge> theDataEdges = theVariable.outgoingEdges(DataFlowEdgeType.filter()).collect(Collectors.toList());
                    if (theDataEdges.size() == 1 && theIncoming.contains(theVariable) && !theVariable.isLocal()) {
                        // Variable is only used once and is used as an incoming data flow
                        final Value theVariableValue = theAssignment.getValue();
                        if (theVariableValue.isTrulyFunctional()) {
                            // We can replace the variable with its assigned value
                            // We variable can also be deleted
                            // We variable assignment is also no longer used
                            aExpressionList.remove(theBefore);

                            aExpression.replaceIncomingDataEdgeRecursive(theVariable, theVariableValue);
                            aGraph.getProgram().deleteVariable(theVariable);

                            modified = true;
                        }
                    }
                }
            }
        }
        return aExpression;
    }
}
