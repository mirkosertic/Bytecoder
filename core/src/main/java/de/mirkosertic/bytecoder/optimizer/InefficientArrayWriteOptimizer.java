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

import java.util.List;
import java.util.stream.Collectors;

import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.graph.Edge;
import de.mirkosertic.bytecoder.ssa.ArrayStoreExpression;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.DataFlowEdgeType;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.PutFieldExpression;
import de.mirkosertic.bytecoder.ssa.PutStaticExpression;
import de.mirkosertic.bytecoder.ssa.RecursiveExpressionVisitor;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

public class InefficientArrayWriteOptimizer extends RecursiveExpressionVisitor implements Optimizer {

    @Override
    public void optimize(final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext) {
        visit(aGraph, aLinkerContext);
    }

    @Override
    protected void visit(final ControlFlowGraph aGraph, final ExpressionList aList, final Expression aExpression, final BytecodeLinkerContext aLinkerContext) {
        if (aExpression instanceof ArrayStoreExpression) {
            final ArrayStoreExpression theArrayStore = (ArrayStoreExpression) aExpression;
            final List<Value> theIncoming = theArrayStore.incomingDataFlows();
            boolean repeat = true;
            while(repeat) {
                repeat = false;
                final Expression theBefore = aList.predecessorOf(aExpression);
                if (theBefore != null) {
                    final Value theTarget = theIncoming.get(0);
                    final Value theValue = theIncoming.get(1);

                    if (theBefore instanceof VariableAssignmentExpression) {
                        final VariableAssignmentExpression theAssignment = (VariableAssignmentExpression) theBefore;
                        final Variable theVariable = theAssignment.getVariable();
                        final List<Edge> theDataEdges = theVariable.outgoingEdges(DataFlowEdgeType.filter())
                                .collect(Collectors.toList());
                        if (theDataEdges.size() == 1 && theVariable == theValue && !theVariable.getName().startsWith("local_")) {
                            theArrayStore.replaceIncomingDataEdge(theValue, theAssignment.getValue());
                            aList.remove(theAssignment);
                            aGraph.getProgram().deleteVariable(theVariable);
                            repeat = true;
                        }
                        if (theDataEdges.size() == 1 && theVariable == theTarget && !theVariable.getName().startsWith("local_")) {
                            theArrayStore.replaceIncomingDataEdge(theTarget, theAssignment.getValue());
                            aList.remove(theAssignment);
                            aGraph.getProgram().deleteVariable(theVariable);
                            repeat = true;
                        }
                    }
                }
            }
        }
    }
}