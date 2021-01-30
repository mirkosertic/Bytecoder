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
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.PutFieldExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

public class MemberFieldWriteOptimizerStage implements OptimizerStage{

    @Override
    public Expression optimize(final CompileBackend aBackend, final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext, final RegionNode aCurrentNode,
                               final ExpressionList aExpressionList, final Expression aExpression) {
        loop:
        while (true) {
            if (aExpression instanceof PutFieldExpression) {
                final PutFieldExpression theFieldWrite = (PutFieldExpression) aExpression;
                final Value theTarget = theFieldWrite.incomingDataFlows().get(0);
                final Value theValue = theFieldWrite.incomingDataFlows().get(1);
                final Expression theBefore = aExpressionList.predecessorOf(theFieldWrite);
                if (theBefore instanceof VariableAssignmentExpression) {
                    final VariableAssignmentExpression theBeforeAssignment = (VariableAssignmentExpression) theBefore;
                    if (theBeforeAssignment.getVariable() == theTarget &&
                            theTarget.outgoingEdges().count() == 1 &&
                            !aCurrentNode.liveOut().contains(theBeforeAssignment.getVariable())) {

                        theFieldWrite.replaceIncomingDataEdge(theTarget, theBeforeAssignment.incomingDataFlows().get(0));
                        aGraph.getProgram().deleteVariable(theBeforeAssignment.getVariable());
                        aExpressionList.remove(theBeforeAssignment);
                        continue loop;
                    }
                    if (theBeforeAssignment.getVariable() == theValue &&
                            theValue.outgoingEdges().count() == 1 &&
                            !aCurrentNode.liveOut().contains(theBeforeAssignment.getVariable())) {

                        theFieldWrite.replaceIncomingDataEdge(theValue, theBeforeAssignment.incomingDataFlows().get(0));
                        aGraph.getProgram().deleteVariable(theBeforeAssignment.getVariable());
                        aExpressionList.remove(theBeforeAssignment);
                        continue loop;
                    }
                }
            }
            return aExpression;
        }
    }
}