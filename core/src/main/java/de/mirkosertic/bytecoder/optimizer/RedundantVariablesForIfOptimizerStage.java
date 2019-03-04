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
import de.mirkosertic.bytecoder.ssa.BinaryExpression;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.DataFlowEdgeType;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.IFExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

import java.util.List;
import java.util.stream.Collectors;

public class RedundantVariablesForIfOptimizerStage implements OptimizerStage {

    @Override
    public Expression optimize(final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext, final RegionNode aCurrentNode,
            final ExpressionList aExpressionList, final Expression aExpression) {
        if (aExpression instanceof IFExpression) {
            final IFExpression theIf = (IFExpression) aExpression;
            final Value theCondition = theIf.incomingDataFlows().get(0);
            if (theCondition instanceof BinaryExpression) {
                final BinaryExpression theBinary = (BinaryExpression) theCondition;
                final Value theLeft = theBinary.incomingDataFlows().get(0);
                final Value theRight = theBinary.incomingDataFlows().get(1);
                if (theLeft instanceof Variable && theRight instanceof Variable) {

                    final Variable theLeftVar = (Variable) theLeft;
                    final Variable theRightVar = (Variable) theRight;
                    if (!theLeftVar.isLocal() && !theRightVar.isLocal()) {
                        final Expression theRightBefore = aExpressionList.predecessorOf(aExpression);
                        final Expression theLeftBefore = aExpressionList.predecessorOf(theRightBefore);
                        if (theLeftBefore instanceof VariableAssignmentExpression
                                && theRightBefore instanceof VariableAssignmentExpression) {
                            final VariableAssignmentExpression theLeftAssignment = (VariableAssignmentExpression )theLeftBefore;
                            final VariableAssignmentExpression theRightAssignment = (VariableAssignmentExpression) theRightBefore;

                            final List<Edge> theLeftEdges = theLeftAssignment.getVariable().outgoingEdges(DataFlowEdgeType.filter()).collect(Collectors.toList());
                            final List<Edge> theRightEdges = theRightAssignment.getVariable().outgoingEdges(DataFlowEdgeType.filter()).collect(Collectors.toList());

                            if (theLeftEdges.size() == 1 && theRightEdges.size() == 1 && theLeftVar == theLeftAssignment.getVariable() && theRightVar == theRightAssignment.getVariable()) {
                                aExpression.replaceIncomingDataEdgeRecursive(theLeftVar, theLeftAssignment.getValue());
                                aExpression.replaceIncomingDataEdgeRecursive(theRightVar, theRightAssignment.getValue());

                                aExpressionList.remove(theLeftAssignment);
                                aExpressionList.remove(theRightAssignment);
                                aGraph.getProgram().deleteVariable(theLeftAssignment.getVariable());
                                aGraph.getProgram().deleteVariable(theRightAssignment.getVariable());
                            }
                        }
                    }

                } else if (theLeft instanceof Variable) {
                    final Variable theLeftVar = (Variable) theLeft;
                    if (!theLeftVar.isLocal()) {
                        final Expression theLeftBefore = aExpressionList.predecessorOf(aExpression);
                        if (theLeftBefore instanceof VariableAssignmentExpression) {
                            final VariableAssignmentExpression theLeftAssignment = (VariableAssignmentExpression )theLeftBefore;

                            final List<Edge> theLeftEdges = theLeftAssignment.getVariable().outgoingEdges(DataFlowEdgeType.filter()).collect(Collectors.toList());

                            if (theLeftEdges.size() == 1 && theLeftVar == theLeftAssignment.getVariable()) {
                                aExpression.replaceIncomingDataEdgeRecursive(theLeftVar, theLeftAssignment.getValue());

                                aExpressionList.remove(theLeftAssignment);
                                aGraph.getProgram().deleteVariable(theLeftAssignment.getVariable());
                            }
                        }
                    }
                }
            }
        }
        return aExpression;
    }
}
