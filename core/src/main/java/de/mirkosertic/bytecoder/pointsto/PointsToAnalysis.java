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
import de.mirkosertic.bytecoder.ssa.ArrayEntryExpression;
import de.mirkosertic.bytecoder.ssa.ArrayStoreExpression;
import de.mirkosertic.bytecoder.ssa.BlockState;
import de.mirkosertic.bytecoder.ssa.ClassReferenceValue;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.EnumConstantsExpression;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.GetFieldExpression;
import de.mirkosertic.bytecoder.ssa.GetStaticExpression;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.IFExpression;
import de.mirkosertic.bytecoder.ssa.MethodParameterValue;
import de.mirkosertic.bytecoder.ssa.NewArrayExpression;
import de.mirkosertic.bytecoder.ssa.NewInstanceAndConstructExpression;
import de.mirkosertic.bytecoder.ssa.NewMultiArrayExpression;
import de.mirkosertic.bytecoder.ssa.NullValue;
import de.mirkosertic.bytecoder.ssa.PHIValue;
import de.mirkosertic.bytecoder.ssa.ProgramDescriptor;
import de.mirkosertic.bytecoder.ssa.ProgramDescriptorProvider;
import de.mirkosertic.bytecoder.ssa.PutFieldExpression;
import de.mirkosertic.bytecoder.ssa.PutStaticExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.ReturnExpression;
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.SelfReferenceParameterValue;
import de.mirkosertic.bytecoder.ssa.SetEnumConstantsExpression;
import de.mirkosertic.bytecoder.ssa.StringValue;
import de.mirkosertic.bytecoder.ssa.SuperTypeOfExpression;
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
import java.util.stream.Collectors;

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

    private VariableSymbol resolveVariable(final Value value, final SymbolCache aSymbolCache) {
        if (value instanceof Variable) {
            return aSymbolCache.variableSymbolForVariable((Variable) value);
        } else if (value instanceof PHIValue) {
            return aSymbolCache.variableSymbolForPHI((PHIValue) value);
        } else {
            throw new IllegalArgumentException("Don't know how to handle return of " + value);
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
                } else if (value instanceof NullValue) {
                    // Null-Constant are in static scope
                    final Symbol valueSymbol = GlobalSymbols.staticScope;
                    aAnalysisResult.alias(varSymbol, valueSymbol);
                } else if (value instanceof StringValue) {
                    // String constants are also in static scope
                    final Symbol valueSymbol = GlobalSymbols.staticScope;
                    aAnalysisResult.alias(varSymbol, valueSymbol);
                } else if (value instanceof GetStaticExpression) {
                    // String constants are also in static scope
                    final Symbol valueSymbol = GlobalSymbols.staticScope;
                    aAnalysisResult.alias(varSymbol, valueSymbol);
                } else if (value instanceof ClassReferenceValue) {
                    // Class references are always in static scope
                    final Symbol valueSymbol = GlobalSymbols.staticScope;
                    aAnalysisResult.alias(varSymbol, valueSymbol);
                } else if (value instanceof EnumConstantsExpression) {
                    // Enum constants are always in static scope
                    final Symbol valueSymbol = GlobalSymbols.staticScope;
                    aAnalysisResult.alias(varSymbol, valueSymbol);
                } else if (value instanceof SuperTypeOfExpression) {
                    // The Superclass of a a class reference is in static scope, too
                    final Symbol valueSymbol = GlobalSymbols.staticScope;
                    aAnalysisResult.alias(varSymbol, valueSymbol);
                } else if (value instanceof ArrayEntryExpression) {
                    // We inherit the symbol from the array we read from
                    final VariableSymbol arraySymbol = resolveVariable(value.incomingDataFlows().get(0), aSymbolCache);
                    aAnalysisResult.readFrom(varSymbol, arraySymbol);
                } else if (value instanceof GetFieldExpression) {
                    // We inherit the symbol from the object we read from
                    final VariableSymbol arraySymbol = resolveVariable(value.incomingDataFlows().get(0), aSymbolCache);
                    aAnalysisResult.readFrom(varSymbol, arraySymbol);
                } else if (value instanceof NewInstanceAndConstructExpression) {
                    final List<Symbol> incomingSymbols = value.incomingDataFlows().stream().map(t -> resolveVariable(t, aSymbolCache)).collect(Collectors.toList());
                    // TODO: Compute escaping flows here
                    final AllocationSymbol alloc = aAnalysisResult.allocation();
                    aAnalysisResult.alias(varSymbol, alloc);
                } else if (value instanceof NewArrayExpression) {
                    final AllocationSymbol alloc = aAnalysisResult.allocation();
                    aAnalysisResult.alias(varSymbol, alloc);
                } else if (value instanceof NewMultiArrayExpression) {
                    final AllocationSymbol alloc = aAnalysisResult.allocation();
                    aAnalysisResult.alias(varSymbol, alloc);
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
                final VariableSymbol returnSymbol = resolveVariable(returnValue, aSymbolCache);
                aAnalysisResult.returns(returnSymbol);
            }
        } else if (aExpression instanceof ThrowExpression) {
            final ThrowExpression exp = (ThrowExpression) aExpression;
            final Value returnValue = exp.incomingDataFlows().get(0);
            final VariableSymbol returnSymbol = resolveVariable(returnValue, aSymbolCache);
            aAnalysisResult.returns(returnSymbol);
        } else if (aExpression instanceof ArrayStoreExpression) {
            final List<Value> incoming = aExpression.incomingDataFlows();
            final VariableSymbol arraySymbol = resolveVariable(incoming.get(0), aSymbolCache);
            final VariableSymbol valueSymbol = resolveVariable(incoming.get(2), aSymbolCache);
            aAnalysisResult.writeInto(arraySymbol, valueSymbol);
        } else if (aExpression instanceof PutFieldExpression) {
            final List<Value> incoming = aExpression.incomingDataFlows();
            final VariableSymbol instanceSymbol = resolveVariable(incoming.get(0), aSymbolCache);
            final VariableSymbol valueSymbol = resolveVariable(incoming.get(1), aSymbolCache);
            aAnalysisResult.writeInto(instanceSymbol, valueSymbol);
        } else if (aExpression instanceof PutStaticExpression) {
            final List<Value> incoming = aExpression.incomingDataFlows();
            final VariableSymbol valueSymbol = resolveVariable(incoming.get(0), aSymbolCache);
            aAnalysisResult.writeInto(GlobalSymbols.staticScope, valueSymbol);
        } else if (aExpression instanceof SetEnumConstantsExpression) {
            final List<Value> incoming = aExpression.incomingDataFlows();
            final VariableSymbol valueSymbol = resolveVariable(incoming.get(1), aSymbolCache);
            aAnalysisResult.writeInto(GlobalSymbols.staticScope, valueSymbol);
        } else if (aExpression instanceof GotoExpression ||
                   aExpression instanceof IFExpression ||
                   aExpression instanceof ReturnExpression) {
            // Not relevant for analysis
        } else {
            throw new IllegalArgumentException("Not supported expression : " + aExpression);
        }
    }
}
