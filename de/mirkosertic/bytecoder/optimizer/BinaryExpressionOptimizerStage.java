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
import de.mirkosertic.bytecoder.ssa.BinaryExpression;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

public class BinaryExpressionOptimizerStage implements OptimizerStage {

    private boolean isRegularVariable(final Value value) {
        return ((value instanceof Variable) && !((Variable) value).isSynthetic());
    }

    @Override
    public Expression optimize(final CompileBackend aBackend, final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext, final RegionNode aCurrentNode,
                               final ExpressionList aExpressionList, final Expression aExpression) {
        loop:
        while (true) {
            if (aExpression instanceof VariableAssignmentExpression) {
                final VariableAssignmentExpression theAssignment = (VariableAssignmentExpression) aExpression;
                final Value theAssignedValue = theAssignment.incomingDataFlows().get(0);
                if (theAssignedValue instanceof BinaryExpression) {
                    // We have a candidate

                    final Value theValue1 = theAssignedValue.incomingDataFlows().get(0);
                    final Value theValue2 = theAssignedValue.incomingDataFlows().get(1);

                    final Expression theBefore = aExpressionList.predecessorOf(aExpression);
                    if (theBefore instanceof VariableAssignmentExpression) {
                        final VariableAssignmentExpression theBeforeAssignment = (VariableAssignmentExpression) theBefore;

                        if (theBeforeAssignment.getVariable() == theValue1 && isRegularVariable(theValue1)
                                && theBeforeAssignment.getVariable().outgoingEdges().count() == 1 &&
                                !aCurrentNode.liveOut().contains(theBeforeAssignment.getVariable())) {

                            theAssignedValue.replaceIncomingDataEdge(theValue1, theBeforeAssignment.incomingDataFlows().get(0));
                            aGraph.getProgram().deleteVariable(theBeforeAssignment.getVariable());
                            aExpressionList.remove(theBeforeAssignment);

                            continue loop;
                        }

                        if (theBeforeAssignment.getVariable() == theValue2 && isRegularVariable(theValue2)
                                && theBeforeAssignment.getVariable().outgoingEdges().count() == 1 &&
                                !aCurrentNode.liveOut().contains(theBeforeAssignment.getVariable())) {

                            theAssignedValue.replaceIncomingDataEdge(theValue2, theBeforeAssignment.incomingDataFlows().get(0));
                            aGraph.getProgram().deleteVariable(theBeforeAssignment.getVariable());
                            aExpressionList.remove(theBeforeAssignment);

                            continue loop;
                        }
                    }
                }
            }
            return aExpression;
        }
    }
}