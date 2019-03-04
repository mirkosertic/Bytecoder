/*
 * Copyright 2019 Mirko Sertic
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
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

import java.util.List;
import java.util.stream.Collectors;

public class DirectAssignmentOptimizerStage implements OptimizerStage {

    @Override
    public Expression optimize(final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext, final RegionNode aCurrentNode,
            final ExpressionList aExpressionList, final Expression aExpression) {

        if (aExpression instanceof VariableAssignmentExpression) {
            final Expression theBefore = aExpressionList.predecessorOf(aExpression);
            if (theBefore instanceof VariableAssignmentExpression) {
                final VariableAssignmentExpression theVarBefore = (VariableAssignmentExpression) theBefore;
                final VariableAssignmentExpression theVarLocal = (VariableAssignmentExpression) aExpression;

                final List<Edge> theDataEdges = theVarBefore.getVariable().outgoingEdges(DataFlowEdgeType.filter()).collect(Collectors.toList());
                if ((!theVarBefore.getVariable().isLocal()) && theVarLocal.getValue() == theVarBefore.getVariable() && theDataEdges.size() == 1) {

                    aExpression.replaceIncomingDataEdgeRecursive(theVarLocal.getValue(), theVarBefore.getValue());

                    aExpressionList.remove(theBefore);
                    aGraph.getProgram().deleteVariable(theVarBefore.getVariable());
                }
            }
        }
        return aExpression;
    }
}
