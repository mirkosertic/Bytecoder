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
import de.mirkosertic.bytecoder.ssa.IFElseExpression;
import de.mirkosertic.bytecoder.ssa.IFExpression;
import de.mirkosertic.bytecoder.ssa.InvocationExpression;
import de.mirkosertic.bytecoder.ssa.PutStaticExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

import java.util.List;
import java.util.stream.Collectors;

public class InlineCallArgumentsOptimizerStage implements OptimizerStage{

    @Override
    public Expression optimize(final CompileBackend aBackend, final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext, final RegionNode aCurrentNode,
                               final ExpressionList aExpressionList, final Expression aExpression) {

        Value theValueToObserve = null;
        if (aExpression instanceof InvocationExpression) {
            theValueToObserve = aExpression;
        } else if (aExpression instanceof VariableAssignmentExpression) {
            final VariableAssignmentExpression theAssignment = (VariableAssignmentExpression) aExpression;
            final Value theValue = theAssignment.incomingDataFlows().get(0);
            if (theValue instanceof InvocationExpression) {
                theValueToObserve = theValue;
            } else {
                final List<Value> theIncoming = theValue.incomingDataFlows();
                if (theIncoming.size() > 0) {
                    if (theIncoming.get(0) instanceof InvocationExpression) {
                        theValueToObserve = theIncoming.get(0);
                    }
                }
            }
        } else if (aExpression instanceof IFExpression) {
            theValueToObserve = aExpression.incomingDataFlows().get(0);
        } else if (aExpression instanceof IFElseExpression) {
            theValueToObserve = aExpression.incomingDataFlows().get(0);
        } else if (aExpression instanceof ReturnValueExpression) {
            theValueToObserve = aExpression;
        } else if (aExpression instanceof PutStaticExpression) {
            theValueToObserve = aExpression;
        }

        if (theValueToObserve != null) {
            // Try to find all required variables
            final List<Variable> theVars = theValueToObserve.incomingDataFlows().stream().filter(t -> t instanceof Variable).map(t -> (Variable) t).collect(Collectors.toList());
            loop: while (true) {
                if (!theVars.isEmpty()) {
                    final Expression theBefore = aExpressionList.predecessorOf(aExpression);
                    if (theBefore instanceof VariableAssignmentExpression) {
                        final VariableAssignmentExpression theBeforeAss = (VariableAssignmentExpression) theBefore;
                        final Variable theVar = theBeforeAss.getVariable();
                        if (!aCurrentNode.liveOut().contains(theVar) && (theVar == theVars.get(theVars.size() - 1)) && theVar.outgoingEdges().count() == 1) {
                            theValueToObserve.replaceIncomingDataEdge(theVar, theBeforeAss.incomingDataFlows().get(0));
                            aExpressionList.remove(theBeforeAss);
                            aGraph.getProgram().deleteVariable(theVar);

                            theVars.remove(theVar);
                            continue loop;
                        }
                    }
                }
                break loop;
            }
        }
        return aExpression;
    }
}