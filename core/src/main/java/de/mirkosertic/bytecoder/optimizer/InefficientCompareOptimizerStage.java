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

import de.mirkosertic.bytecoder.backend.CompileBackend;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.graph.Edge;
import de.mirkosertic.bytecoder.ssa.BinaryExpression;
import de.mirkosertic.bytecoder.ssa.BlockState;
import de.mirkosertic.bytecoder.ssa.CompareExpression;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.DataFlowEdgeType;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.FixedBinaryExpression;
import de.mirkosertic.bytecoder.ssa.IFExpression;
import de.mirkosertic.bytecoder.ssa.IntegerValue;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

import java.util.List;
import java.util.stream.Collectors;

public class InefficientCompareOptimizerStage implements OptimizerStage{

    @Override
    public Expression optimize(final CompileBackend aBackend, final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext, final RegionNode aCurrentNode,
                               final ExpressionList aExpressionList, final Expression aExpression) {
        if (aExpression instanceof IFExpression) {
            final IFExpression theIf = (IFExpression) aExpression;
            final Value theCondition = theIf.incomingDataFlows().get(0);
            if (theCondition instanceof FixedBinaryExpression) {
                final FixedBinaryExpression theBinary = (FixedBinaryExpression) theCondition;
                final Value theFirst = theBinary.incomingDataFlows().get(0);
                final Expression theBefore = aExpressionList.predecessorOf(aExpression);
                if (theBefore instanceof VariableAssignmentExpression) {
                    final VariableAssignmentExpression theAssignment = (VariableAssignmentExpression) theBefore;
                    final Variable theVariable = theAssignment.getVariable();
                    final List<Edge> theDataEdges = theVariable.outgoingEdges(DataFlowEdgeType.filter())
                            .collect(Collectors.toList());
                    final BlockState theLiveOut = aCurrentNode.liveOut();
                    if ((theDataEdges.size() == 1) && (theFirst == theDataEdges.get(0).sourceNode()) && !theLiveOut.contains(theVariable)) {
                        aExpressionList.remove(theAssignment);
                        theBinary.replaceIncomingDataEdge(theVariable, theAssignment.incomingDataFlows().get(0));
                        aGraph.getProgram().deleteVariable(theVariable);
                        return aExpression;
                    }
                }
            }
            if (theCondition instanceof BinaryExpression) {
                final BinaryExpression theBinary = (BinaryExpression) theCondition;
                final Value theFirst = theBinary.incomingDataFlows().get(0);
                final Value theSecond = theBinary.incomingDataFlows().get(1);
                if (theSecond instanceof IntegerValue) {
                    final Expression theBefore = aExpressionList.predecessorOf(aExpression);
                    if (theBefore instanceof VariableAssignmentExpression) {
                        final VariableAssignmentExpression theAssignment = (VariableAssignmentExpression) theBefore;
                        final Variable theVariable = theAssignment.getVariable();
                        final List<Edge> theDataEdges = theVariable.outgoingEdges(DataFlowEdgeType.filter()).collect(Collectors.toList());
                        if (theDataEdges.size() == 1 && theFirst == theVariable) {
                            final Value theValue = theAssignment.incomingDataFlows().get(0);
                            if (theValue instanceof CompareExpression) {
                                final CompareExpression theCompare = (CompareExpression) theValue;
                                final Value theA = theCompare.incomingDataFlows().get(0);
                                final Value theB = theCompare.incomingDataFlows().get(1);
                                final IntegerValue theIntegerValue = (IntegerValue) theSecond;
                                if (theIntegerValue.getIntValue() == 0) {
                                    // A > B : 1
                                    // A < B : -1
                                    // A = B : 0
                                    switch (theBinary.getOperator()) {
                                        case GREATEROREQUALS: {
                                            // compareResult >= 0      -> A >= B
                                            final BinaryExpression theNewCondition = new BinaryExpression(theAssignment.getProgram(), theAssignment.getAddress(), theCompare.resolveType(), theA, BinaryExpression.Operator.GREATEROREQUALS, theB);
                                            theIf.replaceIncomingDataEdge(theCondition, theNewCondition);
                                            aExpressionList.remove(theAssignment);
                                            aGraph.getProgram().deleteVariable(theVariable);
                                            return aExpression;
                                        }
                                        case GREATERTHAN: {
                                            // compareResult >0        -> A > B
                                            final BinaryExpression theNewCondition = new BinaryExpression(theAssignment.getProgram(), theAssignment.getAddress(), theCompare.resolveType(), theA, BinaryExpression.Operator.GREATERTHAN, theB);
                                            theIf.replaceIncomingDataEdge(theCondition, theNewCondition);
                                            aExpressionList.remove(theAssignment);
                                            aGraph.getProgram().deleteVariable(theVariable);
                                            return aExpression;
                                        }
                                        case LESSTHANOREQUALS: {
                                            // compareResult <=0       -> A <= B
                                            final BinaryExpression theNewCondition = new BinaryExpression(theAssignment.getProgram(), theAssignment.getAddress(), theCompare.resolveType(), theA, BinaryExpression.Operator.LESSTHANOREQUALS, theB);
                                            theIf.replaceIncomingDataEdge(theCondition, theNewCondition);
                                            aExpressionList.remove(theAssignment);
                                            aGraph.getProgram().deleteVariable(theVariable);
                                            return aExpression;
                                        }
                                        case LESSTHAN: {
                                            // compareResult <0        -> A < B
                                            final BinaryExpression theNewCondition = new BinaryExpression(theAssignment.getProgram(), theAssignment.getAddress(), theCompare.resolveType(), theA, BinaryExpression.Operator.LESSTHAN, theB);
                                            theIf.replaceIncomingDataEdge(theCondition, theNewCondition);
                                            aExpressionList.remove(theAssignment);
                                            aGraph.getProgram().deleteVariable(theVariable);
                                            return aExpression;
                                        }
                                        case EQUALS: {
                                            // compareResult == 0        -> A == B
                                            final BinaryExpression theNewCondition = new BinaryExpression(theAssignment.getProgram(), theAssignment.getAddress(), theCompare.resolveType(), theA, BinaryExpression.Operator.EQUALS, theB);
                                            theIf.replaceIncomingDataEdge(theCondition, theNewCondition);
                                            aExpressionList.remove(theAssignment);
                                            aGraph.getProgram().deleteVariable(theVariable);
                                            return aExpression;
                                        }
                                        case NOTEQUALS: {
                                            // compareResult != 0        -> A != B
                                            final BinaryExpression theNewCondition = new BinaryExpression(theAssignment.getProgram(), theAssignment.getAddress(), theCompare.resolveType(), theA, BinaryExpression.Operator.NOTEQUALS, theB);
                                            theIf.replaceIncomingDataEdge(theCondition, theNewCondition);
                                            aExpressionList.remove(theAssignment);
                                            aGraph.getProgram().deleteVariable(theVariable);
                                            return aExpression;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return aExpression;
    }
}