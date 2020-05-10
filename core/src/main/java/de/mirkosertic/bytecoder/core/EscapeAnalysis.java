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
package de.mirkosertic.bytecoder.core;

import de.mirkosertic.bytecoder.graph.Edge;
import de.mirkosertic.bytecoder.ssa.ArrayStoreExpression;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.DirectInvokeMethodExpression;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.GetFieldExpression;
import de.mirkosertic.bytecoder.ssa.InvokeStaticMethodExpression;
import de.mirkosertic.bytecoder.ssa.InvokeVirtualMethodExpression;
import de.mirkosertic.bytecoder.ssa.NewObjectAndConstructExpression;
import de.mirkosertic.bytecoder.ssa.PHIValue;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.PutFieldExpression;
import de.mirkosertic.bytecoder.ssa.PutStaticExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.SetEnumConstantsExpression;
import de.mirkosertic.bytecoder.ssa.ThrowExpression;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.ValueWithEscapeCheck;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class EscapeAnalysis {

    public static class AnalysisResult {

        private final BytecodeMethod method;
        private final Program program;
        private final Set<Value> escapingValues;

        public AnalysisResult(final BytecodeMethod method, final Program program) {
            this.method = method;
            this.program = program;
            this.escapingValues = new HashSet<>();
        }

        void escaping(final Value value) {
            escapingValues.add(value);
            if (value instanceof ValueWithEscapeCheck) {
                ((ValueWithEscapeCheck) value).markAsEscaped();
            }
        }

        Set<Value> getEscapingValues() {
            return escapingValues;
        }

        public boolean isMethodArgumentEscaping(final int aIndex) {
            return escapingValues.contains(program.getArguments().get(aIndex));
        }
    }

    public interface ProgramSupplier {
        List<AnalysisResult> provideFor(final EscapeAnalysis aAnalysis, final BytecodeObjectTypeRef aLinkedClass, final String aMethodName, final BytecodeMethodSignature aSignature);
    }

    private final Map<BytecodeLinkedClass, Map<BytecodeMethod, AnalysisResult>> analysisResults;
    private final Stack<AnalysisResult> workingStack;
    private final ProgramSupplier programSupplier;

    public EscapeAnalysis(final ProgramSupplier aProgramSupplier) {
        analysisResults = new HashMap<>();
        workingStack = new Stack<>();
        programSupplier = aProgramSupplier;
    }

    public AnalysisResult analyze(final BytecodeLinkedClass aLinkedClass, final BytecodeMethod aMethod, final Program aProgram) {

        // Check if the requested method is already on the working stack
        // In this case, the method seems to be recursive and we have to prevent duplicate work
        for (final AnalysisResult theStackEntry : workingStack) {
            if (theStackEntry.method == aMethod) {
                // We make sure all arguments are escaping
                for (final Variable v : theStackEntry.program.getArguments()) {
                    theStackEntry.escaping(v);
                }
                return theStackEntry;
            }
        }

        // Return a cached result or perform a new analysis
        final Map<BytecodeMethod, AnalysisResult> theCached = analysisResults.computeIfAbsent(aLinkedClass, t -> new HashMap<>());
        AnalysisResult theResult = theCached.get(aMethod);
        if (theResult == null) {
            theResult = new AnalysisResult(aMethod, aProgram);

            workingStack.push(theResult);

            final ControlFlowGraph aGraph = aProgram.getControlFlowGraph();

            for (final Variable argument : aProgram.getArguments()) {
                final TypeRef theType = argument.resolveType();
                if (theType.isArray() || theType.isObject()) {
                    performEscapeAnalysisFor(theResult, argument, argument, argument, new HashSet<>());
                }
            }
            for (final RegionNode theNode : aGraph.dominators().getPreOrder()) {
                analyze(theResult, theNode.getExpressions());
            }

            workingStack.pop();

            theCached.put(aMethod, theResult);
        }
        return theResult;
    }

    private void analyze(final AnalysisResult aResult, final ExpressionList aExpressionList) {
        for (final Expression theExpression : aExpressionList.toList()) {
            if (theExpression instanceof ExpressionListContainer) {
                final ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                for (final ExpressionList theList : theContainer.getExpressionLists()) {
                    analyze(aResult, theList);
                }
            }

            analyze(aResult, theExpression);
        }
    }

    private void analyze(final AnalysisResult aResult, final Expression aExpression) {
        if (aExpression instanceof VariableAssignmentExpression) {
            final VariableAssignmentExpression theAssignment = (VariableAssignmentExpression) aExpression;
            final Value theAssignedValue = theAssignment.incomingDataFlows().get(0);
            if (theAssignedValue instanceof ValueWithEscapeCheck) {
                performEscapeAnalysisFor(aResult, theAssignedValue, theAssignment.getVariable(), theAssignment.getVariable(), new HashSet<>());
            }
        }
    }

    private void performEscapeAnalysisFor(final AnalysisResult aResult, final Value aValueToCheckEscaping, final Value aPreviousValue, final Value aCurrentValue, final Set<Value> aAlreadySeenPHIs) {

        if (aCurrentValue instanceof PHIValue && !aAlreadySeenPHIs.add(aCurrentValue)) {
            return;
        }

        // Value is used as a return value, it is escaping
        if (aCurrentValue instanceof ReturnValueExpression) {
            aResult.escaping(aValueToCheckEscaping);
            return;
        }

        // Value is stored as enum constants, it is escaping
        if (aCurrentValue instanceof SetEnumConstantsExpression) {
            aResult.escaping(aValueToCheckEscaping);
            return;
        }

        // Used as a param in static invocation, it might be escaping
        if (aCurrentValue instanceof InvokeStaticMethodExpression) {
            final InvokeStaticMethodExpression theExpression = (InvokeStaticMethodExpression) aCurrentValue;
            final List<Value> theValues = aCurrentValue.incomingDataFlows();
            final int theIndex = theValues.indexOf(aPreviousValue);
            for (final AnalysisResult theResult : programSupplier.provideFor(this,
                    theExpression.getClassName(),
                    theExpression.getMethodName(),
                    theExpression.getSignature())) {
                if (theResult.isMethodArgumentEscaping(theIndex)) {
                    // It is escaping!
                    aResult.escaping(aValueToCheckEscaping);
                    return;
                }
            }
        }

        if (aCurrentValue instanceof PutFieldExpression) {
            final List<Value> theValues = aCurrentValue.incomingDataFlows();
            // Value can be the receiver, but not an argument of the invocation
            if (theValues.indexOf(aPreviousValue) > 0) {
                // written to a field, it might be escaping
                aResult.escaping(aValueToCheckEscaping);
                return;
            }
        }

        if (aCurrentValue instanceof PutStaticExpression) {
            final List<Value> theValues = aCurrentValue.incomingDataFlows();
            if (theValues.contains(aPreviousValue)) {
                // written to a static field, it might be escaping
                aResult.escaping(aValueToCheckEscaping);
                return;
            }
        }

        if (aCurrentValue instanceof ArrayStoreExpression) {
            final List<Value> theValues = aCurrentValue.incomingDataFlows();
            // Value can be the receiver or index, but the value
            if (theValues.indexOf(aPreviousValue) == 2) {
                // written to an array, it might be escaping
                aResult.escaping(aValueToCheckEscaping);
                return;
            }
        }

        if (aCurrentValue instanceof NewObjectAndConstructExpression) {
            final List<Value> theArguments = aCurrentValue.incomingDataFlows();
            if (theArguments.contains(aPreviousValue)) {
                // Value used as constructor argument, it might be escaping
                aResult.escaping(aValueToCheckEscaping);
                return;
            }
        }

        if (aCurrentValue instanceof InvokeVirtualMethodExpression || aCurrentValue instanceof DirectInvokeMethodExpression) {
            final List<Value> theValues = aCurrentValue.incomingDataFlows();
            // Value can be the receiver, but not an argument of the invocation
            if (theValues.indexOf(aPreviousValue) > 0) {
                // Used as a argument in Virtual or Direct invocation, it might be escaping
                aResult.escaping(aValueToCheckEscaping);
                return;
            }
        }

        if (aCurrentValue instanceof GetFieldExpression) {
            // Field is read from the instance, we think it is escaping
            aResult.escaping(aValueToCheckEscaping);
            return;
        }

        if (aCurrentValue instanceof ThrowExpression) {
            // Escaping by throwing,
            aResult.escaping(aValueToCheckEscaping);
            return;
        }

        // Copied to another variable, we have to check the copy, too
        if (aCurrentValue instanceof VariableAssignmentExpression) {
            final VariableAssignmentExpression theAssignment = (VariableAssignmentExpression) aCurrentValue;
            final Variable theVariable = theAssignment.getVariable();

            theVariable.outgoingEdges().map(Edge::targetNode).forEach(
                    node -> performEscapeAnalysisFor(aResult, aValueToCheckEscaping, theVariable, (Value) node, aAlreadySeenPHIs)
            );
        }

        aCurrentValue.outgoingEdges().map(Edge::targetNode).forEach(
                node -> performEscapeAnalysisFor(aResult, aValueToCheckEscaping, aCurrentValue, (Value) node, aAlreadySeenPHIs)
        );
    }
}
