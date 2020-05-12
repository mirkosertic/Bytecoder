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

import de.mirkosertic.bytecoder.ssa.ArrayStoreExpression;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.DirectInvokeMethodExpression;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.GetFieldExpression;
import de.mirkosertic.bytecoder.ssa.InvocationExpression;
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
import java.util.stream.Collectors;

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

    public static class ProgramDescriptor {

        private final BytecodeLinkedClass linkedClass;
        private final BytecodeMethod method;
        private final Program program;

        public ProgramDescriptor(final BytecodeLinkedClass linkedClass, final BytecodeMethod method, final Program program) {
            this.linkedClass = linkedClass;
            this.method = method;
            this.program = program;
        }
    }

    public interface ProgramDescriptorProvider {
        ProgramDescriptor resolveStaticInvocation(final BytecodeObjectTypeRef aClass, final String aMethodName, final BytecodeMethodSignature aSignature);
        ProgramDescriptor resolveConstructorInvocation(final BytecodeObjectTypeRef aClass, final BytecodeMethodSignature aSignature);
        ProgramDescriptor resolveDirectInvocation(final BytecodeObjectTypeRef aClazz, final String  aMethodName, final BytecodeMethodSignature aSignature);
    }

    private final Map<BytecodeLinkedClass, Map<BytecodeMethod, AnalysisResult>> analysisResults;
    private final Stack<AnalysisResult> workingStack;
    private final ProgramDescriptorProvider programDescriptorProvider;
    private final Statistics statistics;

    public EscapeAnalysis(final ProgramDescriptorProvider aProgramDescriptorProvider, final Statistics aStatistics) {
        analysisResults = new HashMap<>();
        workingStack = new Stack<>();
        programDescriptorProvider = aProgramDescriptorProvider;
        statistics = aStatistics;
    }

    public AnalysisResult analyze(final ProgramDescriptor aProgramDescriptor) {

        final BytecodeLinkedClass theLinkedClass = aProgramDescriptor.linkedClass;
        final BytecodeMethod theMethod = aProgramDescriptor.method;
        final Program theProgram = aProgramDescriptor.program;

        // Check if the requested method is already on the working stack
        // In this case, the method seems to be recursive and we have to prevent duplicate work
        for (final AnalysisResult theStackEntry : workingStack) {
            if (theStackEntry.method == theMethod) {
                // We make sure all arguments are escaping
                for (final Variable v : theStackEntry.program.getArguments()) {
                    theStackEntry.escaping(v);
                }
                return theStackEntry;
            }
        }

        // Return a cached result or perform a new analysis
        final Map<BytecodeMethod, AnalysisResult> theCached = analysisResults.computeIfAbsent(theLinkedClass, t -> new HashMap<>());
        AnalysisResult theResult = theCached.get(theMethod);
        if (theResult == null) {
            theResult = new AnalysisResult(theMethod, theProgram);

            workingStack.push(theResult);

            final ControlFlowGraph aGraph = theProgram.getControlFlowGraph();

            for (final Variable argument : theProgram.getArguments()) {
                final TypeRef theType = argument.resolveType();
                if (theType.isArray() || theType.isObject()) {
                    performEscapeAnalysisFor(theResult, argument, argument, argument, new HashSet<>(), false, new HashSet<>());
                }
            }
            for (final RegionNode theNode : aGraph.dominators().getPreOrder()) {
                analyze(theResult, theNode.getExpressions(), true);
            }

            workingStack.pop();

            theCached.put(theMethod, theResult);
        }
        return theResult;
    }

    private void analyze(final AnalysisResult aResult, final ExpressionList aExpressionList, final boolean aIncludeReturn) {
        for (final Expression theExpression : aExpressionList.toList()) {
            if (theExpression instanceof ExpressionListContainer) {
                final ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                for (final ExpressionList theList : theContainer.getExpressionLists()) {
                    analyze(aResult, theList, aIncludeReturn);
                }
            }

            analyze(aResult, theExpression, aIncludeReturn);
        }
    }

    private void analyze(final AnalysisResult aResult, final Expression aExpression, final boolean aIncludeReturn) {
        if (aExpression instanceof VariableAssignmentExpression) {
            final VariableAssignmentExpression theAssignment = (VariableAssignmentExpression) aExpression;
            final Value theAssignedValue = theAssignment.incomingDataFlows().get(0);
            if (theAssignedValue instanceof ValueWithEscapeCheck) {
                performEscapeAnalysisFor(aResult, theAssignedValue, theAssignment.getVariable(), theAssignment.getVariable(), new HashSet<>(), aIncludeReturn, new HashSet<>());
            }
        }
    }

    private boolean performEscapeAnalysisFor(final AnalysisResult aResult, final Value aValueToCheckEscaping, final Value aPreviousValue, final Value aCurrentValue, final Set<Value> aAlreadySeenPHIs, final boolean aIncludeReturn, final Set<Value> alreadyAnalyzed) {

        if (aCurrentValue instanceof PHIValue && !aAlreadySeenPHIs.add(aCurrentValue)) {
            return true;
        }

        if (!(aCurrentValue instanceof InvocationExpression)) {
            alreadyAnalyzed.add(aCurrentValue);
        }

        // Value is used as a return value, it is escaping
        if (aCurrentValue instanceof ReturnValueExpression && aIncludeReturn) {
            aResult.escaping(aValueToCheckEscaping);
            statistics.context("EscapeAnalysis")
                    .counter("EscapeByReturn").increment();
            return true;
        }

        // Value is stored as enum constants, it is escaping
        if (aCurrentValue instanceof SetEnumConstantsExpression) {
            aResult.escaping(aValueToCheckEscaping);
            statistics.context("EscapeAnalysis")
                    .counter("EscapeBySetEnumConstant").increment();
            return true;
        }

        // Used as a param in static invocation, it might be escaping
        if (aCurrentValue instanceof InvokeStaticMethodExpression) {
            final InvokeStaticMethodExpression theExpression = (InvokeStaticMethodExpression) aCurrentValue;
            final List<Value> theArguments = aCurrentValue.incomingDataFlows();

            final ProgramDescriptor theDescriptor = programDescriptorProvider.resolveStaticInvocation(
                    theExpression.getClassName(),
                    theExpression.getMethodName(),
                    theExpression.getSignature());

            if (theDescriptor != null && !theDescriptor.method.getAccessFlags().isNative()) {
                final AnalysisResult theResult = analyze(theDescriptor);
                for (int i = 0; i < theArguments.size(); i++) {
                    final Value v = theArguments.get(i);
                    if (v == aPreviousValue && theResult.isMethodArgumentEscaping(i)) {
                        aResult.escaping(aValueToCheckEscaping);
                        statistics.context("EscapeAnalysis")
                                .counter("EscapeByStaticInvocation").increment();
                        return true;
                    }
                }
            } else {
                // Calls to native methods are always escaping
                aResult.escaping(aValueToCheckEscaping);
                statistics.context("EscapeAnalysis")
                        .counter("EscapeByStaticNativeInvocation").increment();
                return true;
            }
        }

        if (aCurrentValue instanceof PutFieldExpression) {
            final List<Value> theValues = aCurrentValue.incomingDataFlows();
            // Value can be the receiver, but not an argument of the invocation
            if (theValues.indexOf(aPreviousValue) > 0) {
                // written to a field, it might be escaping
                aResult.escaping(aValueToCheckEscaping);
                statistics.context("EscapeAnalysis")
                        .counter("EscapeByPutField").increment();
                return true;
            }
        }

        if (aCurrentValue instanceof PutStaticExpression) {
            final List<Value> theValues = aCurrentValue.incomingDataFlows();
            if (theValues.contains(aPreviousValue)) {
                // written to a static field, it is escaping
                aResult.escaping(aValueToCheckEscaping);
                statistics.context("EscapeAnalysis")
                        .counter("EscapeByPutStatic").increment();
                return true;
            }
        }

        if (aCurrentValue instanceof ArrayStoreExpression) {
            final List<Value> theValues = aCurrentValue.incomingDataFlows();
            // Value can be the receiver or index
            if (theValues.indexOf(aPreviousValue) == 2) {
                // written to an array, it might be escaping
                aResult.escaping(aValueToCheckEscaping);
                statistics.context("EscapeAnalysis")
                        .counter("EscapeByArrayStore").increment();
                return true;
            }
        }

        if (aCurrentValue instanceof NewObjectAndConstructExpression) {
            final NewObjectAndConstructExpression newObjectAndConstructExpression = (NewObjectAndConstructExpression) aCurrentValue;
            final List<Value> theArguments = newObjectAndConstructExpression.incomingDataFlows();
            final ProgramDescriptor theDescriptor = programDescriptorProvider.resolveConstructorInvocation(
                    newObjectAndConstructExpression.getClazz(), newObjectAndConstructExpression.getSignature()
            );

            final AnalysisResult theResult = analyze(theDescriptor);
            for (int i=0;i<theArguments.size();i++) {
                final Value v = theArguments.get(i);
                if (v == aPreviousValue && theResult.isMethodArgumentEscaping(i)) {
                    aResult.escaping(aValueToCheckEscaping);
                    statistics.context("EscapeAnalysis")
                            .counter("EscapeByConstructorArgument").increment();
                    return true;
                }
            }
        }

        if (aCurrentValue instanceof DirectInvokeMethodExpression) {
            final DirectInvokeMethodExpression directInvokeMethodExpression = (DirectInvokeMethodExpression) aCurrentValue;
            final List<Value> theArguments = directInvokeMethodExpression.incomingDataFlows();
            final ProgramDescriptor theDescriptor = programDescriptorProvider.resolveDirectInvocation(
                    directInvokeMethodExpression.getClazz(),
                    directInvokeMethodExpression.getMethodName(),
                    directInvokeMethodExpression.getSignature()
            );
            if (theDescriptor != null && !theDescriptor.method.getAccessFlags().isNative()) {
                final AnalysisResult theResult = analyze(theDescriptor);
                // We start with 1, because we ignore the target
                for (int i = 1; i < theArguments.size(); i++) {
                    final Value v = theArguments.get(i);
                    if (v == aPreviousValue && theResult.isMethodArgumentEscaping(i)) {
                        aResult.escaping(aValueToCheckEscaping);
                        statistics.context("EscapeAnalysis")
                                .counter("EscapeByDirectInvocation").increment();
                        return true;
                    }
                }
            } else {
                // Calls to native methods are always escaping
                aResult.escaping(aValueToCheckEscaping);
                statistics.context("EscapeAnalysis")
                        .counter("EscapeByDirectNative").increment();
                return true;
            }
        }

        if (aCurrentValue instanceof InvokeVirtualMethodExpression) {
            // Virtual invocations always escape
            // Is is quite hard to get this right. We need a new IR instruction for
            // the whole InvokeDynamic handling to get this right.
            aResult.escaping(aValueToCheckEscaping);
            statistics.context("EscapeAnalysis")
                    .counter("EscapeByVirtual").increment();
            return true;
        }

        if (aCurrentValue instanceof GetFieldExpression) {
            final GetFieldExpression theGetField = (GetFieldExpression) aCurrentValue;
            final TypeRef theType = theGetField.resolveType();
            if (theType.isArray() || theType.isObject()) {
                // Field of reference type is read from the instance, we think it is escaping

                statistics.context("EscapeAnalysis")
                        .counter("EscapeByGetField").increment();
                aResult.escaping(aValueToCheckEscaping);
                return true;
            }
        }

        if (aCurrentValue instanceof ThrowExpression) {
            // Escaping by throwing,
            aResult.escaping(aValueToCheckEscaping);
            statistics.context("EscapeAnalysis")
                    .counter("EscapeByThrow").increment();
            return true;
        }

        // Copied to another variable, we have to check the copy, too
        if (aCurrentValue instanceof VariableAssignmentExpression) {
            final VariableAssignmentExpression theAssignment = (VariableAssignmentExpression) aCurrentValue;
            final Variable theVariable = theAssignment.getVariable();

            final List<Value> theOutgoingValues = theVariable.outgoingEdges().map(t -> (Value)t.targetNode()).collect(Collectors.toList());
            boolean theResult = false;
            for (final Value node : theOutgoingValues) {
                if (!alreadyAnalyzed.contains(node)) {
                    theResult = theResult | performEscapeAnalysisFor(aResult, aValueToCheckEscaping, theVariable, node, aAlreadySeenPHIs, aIncludeReturn, alreadyAnalyzed);
                }
            }

            if (theResult) {
                return true;
            }
        }

        // Continue to follow the dataflow
        boolean result = false;
        final List<Value> theOutgoingValues = aCurrentValue.outgoingEdges().map(t -> (Value)t.targetNode()).collect(Collectors.toList());
        for (final Value node : theOutgoingValues) {
            if (!alreadyAnalyzed.contains(node)) {
                result = result | performEscapeAnalysisFor(aResult, aValueToCheckEscaping, aCurrentValue, node, aAlreadySeenPHIs, aIncludeReturn, alreadyAnalyzed);
            }
        }
        return result;
    }
}
