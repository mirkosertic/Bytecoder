/*
 * Copyright 2020 Mirko Sertic
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
import de.mirkosertic.bytecoder.ssa.DirectInvokeMethodExpression;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.InvokeStaticMethodExpression;
import de.mirkosertic.bytecoder.ssa.InvokeVirtualMethodExpression;
import de.mirkosertic.bytecoder.ssa.PHIValue;
import de.mirkosertic.bytecoder.ssa.PutFieldExpression;
import de.mirkosertic.bytecoder.ssa.PutStaticExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.SetEnumConstantsExpression;
import de.mirkosertic.bytecoder.ssa.ThrowExpression;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.ValueWithEscapeCheck;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

import java.util.List;

public class EscapeAnalysisOptimizerStage implements OptimizerStage {

    @Override
    public Expression optimize(final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext, final RegionNode aCurrentNode, final ExpressionList aExpressionList, final Expression aExpression) {
        if (aExpression instanceof VariableAssignmentExpression) {
            final VariableAssignmentExpression theAssignment = (VariableAssignmentExpression) aExpression;
            final Value theAssignedValue = theAssignment.incomingDataFlows().get(0);
            if (theAssignedValue instanceof ValueWithEscapeCheck) {
                performEscapeAnalysisFor((ValueWithEscapeCheck) theAssignedValue, theAssignment.getVariable(), theAssignment.getVariable());
            }
        }
        return aExpression;
    }

    private void performEscapeAnalysisFor(final ValueWithEscapeCheck aValueToCheckEscaping, final Value aPreviousValue, final Value aCurrentValue) {

        // used in a PHIValue
        // To prevent complex analysis, we give up here and mark the value as escaping
        if (aCurrentValue instanceof PHIValue) {
            aValueToCheckEscaping.markAsEscaped();
            return;
        }

        // Value is used as a return value, it is escaping
        if (aCurrentValue instanceof ReturnValueExpression) {
            final Value v = (Value) aValueToCheckEscaping;
            aValueToCheckEscaping.markAsEscaped();
            return;
        }

        // Value is stored as enum constants, it is escaping
        if (aCurrentValue instanceof SetEnumConstantsExpression) {
            aValueToCheckEscaping.markAsEscaped();
            return;
        }

        // Used as a param in static invocation, it might be escaping
        if (aCurrentValue instanceof InvokeStaticMethodExpression) {
            final Value v = (Value) aValueToCheckEscaping;
            aValueToCheckEscaping.markAsEscaped();
            return;
        }

        if (aCurrentValue instanceof PutFieldExpression) {
            final List<Value> theValues = aCurrentValue.incomingDataFlows();
            // Value can be the receiver, but not an argument of the invocation
            if (theValues.indexOf(aPreviousValue) > 0) {
                // written to a field, it might be escaping
                aValueToCheckEscaping.markAsEscaped();
                return;
            }
        }

        if (aCurrentValue instanceof PutStaticExpression) {
            final List<Value> theValues = aCurrentValue.incomingDataFlows();
            if (theValues.contains(aPreviousValue)) {
                // written to a static field, it might be escaping
                aValueToCheckEscaping.markAsEscaped();
                return;
            }
        }

        if (aCurrentValue instanceof ArrayStoreExpression) {
            final List<Value> theValues = aCurrentValue.incomingDataFlows();
            // Value can be the receiver or index, but the value
            if (theValues.indexOf(aPreviousValue) == 2) {
                // written to an array, it might be escaping
                aValueToCheckEscaping.markAsEscaped();
                return;
            }
        }

        if (aCurrentValue instanceof InvokeVirtualMethodExpression || aCurrentValue instanceof DirectInvokeMethodExpression) {
            final List<Value> theValues = aCurrentValue.incomingDataFlows();
            // Value can be the receiver, but not an argument of the invocation
            if (theValues.indexOf(aPreviousValue) > 0) {
                // Used as a argument in Virtual or Direct invocation, it might be escaping
                aValueToCheckEscaping.markAsEscaped();
                return;
            }
        }

        if (aCurrentValue instanceof ThrowExpression) {
            // Escaping by throwing,
            aValueToCheckEscaping.markAsEscaped();
            return;
        }

        // Copied to another variable, we have to check the copy, too
        if (aCurrentValue instanceof VariableAssignmentExpression) {
            final VariableAssignmentExpression theAssignment = (VariableAssignmentExpression) aCurrentValue;
            final Variable theVariable = theAssignment.getVariable();

            theVariable.outgoingEdges().map(Edge::targetNode).forEach(
                    node -> performEscapeAnalysisFor(aValueToCheckEscaping, theVariable, (Value) node)
            );
        }

        aCurrentValue.outgoingEdges().map(Edge::targetNode).forEach(
                node -> performEscapeAnalysisFor(aValueToCheckEscaping, aCurrentValue, (Value) node)
        );
    }
}
