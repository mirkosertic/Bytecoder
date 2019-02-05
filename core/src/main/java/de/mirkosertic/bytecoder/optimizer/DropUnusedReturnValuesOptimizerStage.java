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
import de.mirkosertic.bytecoder.ssa.InvocationExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

import java.util.List;
import java.util.stream.Collectors;

public class DropUnusedReturnValuesOptimizerStage implements OptimizerStage{

    @Override
    public Expression optimize(final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext, final RegionNode aCurrentNode,
            final ExpressionList aExpressionList, final Expression aExpression) {
        if (aExpression instanceof VariableAssignmentExpression) {
            final VariableAssignmentExpression theAssignment = (VariableAssignmentExpression) aExpression;
            final Variable theVariable = theAssignment.getVariable();
            final Value theValue = theAssignment.getValue();
            if (theValue instanceof InvocationExpression) {
                final List<Edge> theDataEdges = theVariable.outgoingEdges(DataFlowEdgeType.filter()).collect(Collectors.toList());
                if (theDataEdges.isEmpty()) {
                    aExpressionList.replace(aExpression, (InvocationExpression) theValue);
                    aGraph.getProgram().deleteVariable(theVariable);
                    return (InvocationExpression) theValue;
                }
            }
        }
        return aExpression;
    }
}