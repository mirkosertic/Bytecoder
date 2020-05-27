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
package de.mirkosertic.bytecoder.pointsto;

import de.mirkosertic.bytecoder.api.Logger;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.ssa.BlockState;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.IFExpression;
import de.mirkosertic.bytecoder.ssa.MethodParameterValue;
import de.mirkosertic.bytecoder.ssa.PHIValue;
import de.mirkosertic.bytecoder.ssa.ProgramDescriptor;
import de.mirkosertic.bytecoder.ssa.ProgramDescriptorProvider;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.SelfReferenceParameterValue;
import de.mirkosertic.bytecoder.ssa.ThrowExpression;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class PointsToAnalysis {

    static class CachedAnalysisResult {
        final ProgramDescriptor programDescriptor;
        final PointsToAnalysisResult analysisResult;

        public CachedAnalysisResult(final ProgramDescriptor programDescriptor, final PointsToAnalysisResult analysisResult) {
            this.programDescriptor = programDescriptor;
            this.analysisResult = analysisResult;
        }
    }

    private final ProgramDescriptorProvider programDescriptorProvider;
    private final Stack<CachedAnalysisResult> analysisStack;
    private final Logger logger;
    private final Map<BytecodeLinkedClass, List<CachedAnalysisResult>> cache;

    public PointsToAnalysis(final ProgramDescriptorProvider aProgramDescriptorProvider, final Logger aLogger) {
        programDescriptorProvider = aProgramDescriptorProvider;
        logger = aLogger;
        analysisStack = new Stack<>();
        cache = new HashMap<>();
    }

    public PointsToAnalysisResult analyze(final ProgramDescriptor aProgramDescriptor) {

        // We search in the cache
        final List<CachedAnalysisResult> cachedForCurrentClass = cache.computeIfAbsent(aProgramDescriptor.linkedClass(), t -> new ArrayList<>());
        for (final CachedAnalysisResult entry : cachedForCurrentClass) {
            if (entry.programDescriptor.linkedClass() == aProgramDescriptor.linkedClass() && entry.programDescriptor.method() == aProgramDescriptor.method()) {
                // We have it already computed
                return entry.analysisResult;
            }
        }

        final PointsToAnalysisResult analysisResult = new PointsToAnalysisResult();
        analysisStack.push(new CachedAnalysisResult(aProgramDescriptor, analysisResult));

        final SymbolCache symbolCache = new SymbolCache();

        for (final Variable v : aProgramDescriptor.program().getArguments()) {
            final TypeRef theType = v.resolveType();
            if (theType.isArray() || theType.isObject()) {
                final VariableSymbol var = symbolCache.variableSymbolForVariable(v);
                final List<Value> incoming = v.incomingDataFlows();
                if (incoming.size() != 1) {
                    throw new IllegalArgumentException("Unexpected number of initialization values for " + v + ", got " + incoming.size() + ", expected 1");
                }
                final Value theSingleValue = incoming.get(0);
                if (theSingleValue instanceof SelfReferenceParameterValue) {
                    analysisResult.alias(var, GlobalSymbols.thisScope);
                } else if (theSingleValue instanceof MethodParameterValue) {
                    analysisResult.alias(var, symbolCache.symbolForMethodParameter((MethodParameterValue) theSingleValue));
                } else {
                    throw new IllegalStateException("Not supported initialization type for " + v + " : " + theSingleValue);
                }
            }
        }

        final ControlFlowGraph g = aProgramDescriptor.program().getControlFlowGraph();
        final Set<PHIValue> alreadyKnownPHIvalues = new HashSet<>();
        // We iterate over the control flow graph
        for (final RegionNode theNode : g.dominators().getPreOrder()) {
            final BlockState theIn = theNode.liveIn();
            // We set the phi values accordingly for the current node
            theIn.getPorts().values().stream().filter(t -> t instanceof PHIValue).map(t -> (PHIValue) t).filter(alreadyKnownPHIvalues::add).forEach(t -> {
                final VariableSymbol thePHI = symbolCache.variableSymbolForPHI(t);
                for (final Value v : t.incomingDataFlows()) {
                    if (v instanceof PHIValue) {
                        final Symbol theIncomingSymbol = symbolCache.variableSymbolForPHI((PHIValue) v);
                        analysisResult.assign(thePHI, theIncomingSymbol);
                    } else if (v instanceof Variable) {
                        final Symbol theIncomingSymbol = symbolCache.variableSymbolForVariable((Variable) v);
                        analysisResult.assign(thePHI, theIncomingSymbol);
                    } else {
                        throw new IllegalArgumentException("Not supported incoming dataflow value for PHI : " + v);
                    }
                }
            });
            // And now we iterate over the expressions and run the pointsto analysis while searching for relevant patterns
            analyze(theNode.getExpressions(), analysisResult, symbolCache);
        }

        analysisStack.pop();
        cachedForCurrentClass.add(new CachedAnalysisResult(aProgramDescriptor, analysisResult));
        return analysisResult;
    }

    private void analyze(final ExpressionList aExpressionList, final PointsToAnalysisResult aAnalysisResult, final SymbolCache aSymbolCache) {
        for (final Expression theExpression : aExpressionList.toList()) {
            if (theExpression instanceof ExpressionListContainer) {
                final ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                for (final ExpressionList theList : theContainer.getExpressionLists()) {
                    analyze(theList, aAnalysisResult, aSymbolCache);
                }
            }

            analyze(theExpression, aAnalysisResult, aSymbolCache);
        }
    }

    private void analyze(final Value aExpression, final PointsToAnalysisResult aAnalysisResult, final SymbolCache aSymbolCache) {
        if (aExpression instanceof VariableAssignmentExpression) {
            final VariableAssignmentExpression exp = (VariableAssignmentExpression) aExpression;
            final Variable var = exp.getVariable();
            final TypeRef varType = var.resolveType();
            if (varType.isObject() || varType.isArray()) {
                final Value value = exp.incomingDataFlows().get(0);

                final Symbol varSymbol = aSymbolCache.variableSymbolForVariable(var);
                if (value instanceof Variable) {
                    final Symbol valueSymbol = aSymbolCache.variableSymbolForVariable((Variable) value);
                    aAnalysisResult.alias(varSymbol, valueSymbol);
                } else if (value instanceof PHIValue) {
                    final Symbol valueSymbol = aSymbolCache.variableSymbolForPHI((PHIValue) value);
                    aAnalysisResult.alias(varSymbol, valueSymbol);
                } else {
                    // Assignment
                    throw new IllegalArgumentException("Unknown :" + value);
                }
            }
        } else if (aExpression instanceof ReturnValueExpression) {
            final ReturnValueExpression exp = (ReturnValueExpression) aExpression;
            final Value returnValue = exp.incomingDataFlows().get(0);
            final TypeRef returnValueType = returnValue.resolveType();
            if (returnValueType.isArray() || returnValueType.isObject()) {
                if (returnValue instanceof Variable) {
                    final VariableSymbol returningSymbol = aSymbolCache.variableSymbolForVariable((Variable) returnValue);
                    aAnalysisResult.returns(returningSymbol);
                } else if (returnValue instanceof PHIValue) {
                    final VariableSymbol returningSymbol = aSymbolCache.variableSymbolForPHI((PHIValue) returnValue);
                    aAnalysisResult.returns(returningSymbol);
                } else {
                    throw new IllegalArgumentException("Don't know how to handle return of " + returnValue);
                }
            }
        } else if (aExpression instanceof ThrowExpression) {
            final ThrowExpression exp = (ThrowExpression) aExpression;
            final Value returnValue = exp.incomingDataFlows().get(0);
            if (returnValue instanceof Variable) {
                final VariableSymbol returningSymbol = aSymbolCache.variableSymbolForVariable((Variable) returnValue);
                aAnalysisResult.returns(returningSymbol);
            } else if (returnValue instanceof PHIValue) {
                final VariableSymbol returningSymbol = aSymbolCache.variableSymbolForPHI((PHIValue) returnValue);
                aAnalysisResult.returns(returningSymbol);
            } else {
                throw new IllegalArgumentException("Don't know how to handle return of " + returnValue);
            }
        } else if (aExpression instanceof GotoExpression ||
                   aExpression instanceof IFExpression) {
            // Not relevant for analysis
        } else {
            throw new IllegalArgumentException("Not supported expression : " + aExpression);
        }
    }
}
