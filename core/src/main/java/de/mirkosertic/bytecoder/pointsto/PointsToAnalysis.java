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
import de.mirkosertic.bytecoder.ssa.CurrentExceptionExpression;
import de.mirkosertic.bytecoder.ssa.EnumConstantsExpression;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.GetFieldExpression;
import de.mirkosertic.bytecoder.ssa.GetStaticExpression;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.IFExpression;
import de.mirkosertic.bytecoder.ssa.InvokeDirectMethodExpression;
import de.mirkosertic.bytecoder.ssa.InvokeStaticMethodExpression;
import de.mirkosertic.bytecoder.ssa.InvokeVirtualMethodExpression;
import de.mirkosertic.bytecoder.ssa.LambdaConstructorReferenceExpression;
import de.mirkosertic.bytecoder.ssa.LambdaInterfaceReferenceExpression;
import de.mirkosertic.bytecoder.ssa.LambdaSpecialReferenceExpression;
import de.mirkosertic.bytecoder.ssa.LambdaVirtualReferenceExpression;
import de.mirkosertic.bytecoder.ssa.LambdaWithStaticImplExpression;
import de.mirkosertic.bytecoder.ssa.LookupSwitchExpression;
import de.mirkosertic.bytecoder.ssa.MethodHandleExpression;
import de.mirkosertic.bytecoder.ssa.MethodHandlesGeneratedLookupExpression;
import de.mirkosertic.bytecoder.ssa.MethodParameterValue;
import de.mirkosertic.bytecoder.ssa.MethodTypeExpression;
import de.mirkosertic.bytecoder.ssa.NewArrayExpression;
import de.mirkosertic.bytecoder.ssa.NewInstanceAndConstructExpression;
import de.mirkosertic.bytecoder.ssa.NewInstanceExpression;
import de.mirkosertic.bytecoder.ssa.NewInstanceFromDefaultConstructorExpression;
import de.mirkosertic.bytecoder.ssa.NewMultiArrayExpression;
import de.mirkosertic.bytecoder.ssa.NullValue;
import de.mirkosertic.bytecoder.ssa.PHIValue;
import de.mirkosertic.bytecoder.ssa.ProgramDescriptor;
import de.mirkosertic.bytecoder.ssa.ProgramDescriptorProvider;
import de.mirkosertic.bytecoder.ssa.PtrOfExpression;
import de.mirkosertic.bytecoder.ssa.PutFieldExpression;
import de.mirkosertic.bytecoder.ssa.PutStaticExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.ResolveCallsiteInstanceExpression;
import de.mirkosertic.bytecoder.ssa.ReturnExpression;
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.SelfReferenceParameterValue;
import de.mirkosertic.bytecoder.ssa.SetEnumConstantsExpression;
import de.mirkosertic.bytecoder.ssa.SetMemoryLocationExpression;
import de.mirkosertic.bytecoder.ssa.StringValue;
import de.mirkosertic.bytecoder.ssa.SuperTypeOfExpression;
import de.mirkosertic.bytecoder.ssa.TableSwitchExpression;
import de.mirkosertic.bytecoder.ssa.ThrowExpression;
import de.mirkosertic.bytecoder.ssa.TypeOfExpression;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.UnreachableExpression;
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

        // We check for recursion in the analysis
        for (final CachedAnalysisResult stackEntry : analysisStack) {
            if (stackEntry.programDescriptor.linkedClass() == aProgramDescriptor.linkedClass() && stackEntry.programDescriptor.method() == aProgramDescriptor.method()) {
                // Recursion cannot be analyzed here
                // We assume that every argument escapes to the static flow
                final SymbolCache symbolCache = new SymbolCache();
                final PointsToAnalysisResult recursionResult = new PointsToAnalysisResult();
                for (final Variable v : stackEntry.programDescriptor.program().getArguments()) {
                    final TypeRef t = v.resolveType();
                    if (t.isArray() || t.isObject()) {
                        recursionResult.assign(GlobalSymbols.staticScope, resolve(v, symbolCache));
                    }
                }
                return recursionResult;
            }
        }

        logger.debug("Analyzing {}.{} {}", aProgramDescriptor.linkedClass().getClassName().name(), aProgramDescriptor.method().getName().stringValue(), aProgramDescriptor.method().getSignature());

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

    private Symbol resolve(final Value value, final SymbolCache aSymbolCache) {
        if (value instanceof Variable) {
            return aSymbolCache.variableSymbolForVariable((Variable) value);
        } else if (value instanceof PHIValue) {
            return aSymbolCache.variableSymbolForPHI((PHIValue) value);
        } else if (value instanceof LambdaWithStaticImplExpression ||
                   value instanceof LambdaSpecialReferenceExpression ||
                   value instanceof LambdaVirtualReferenceExpression ||
                   value instanceof LambdaConstructorReferenceExpression ||
                   value instanceof LambdaInterfaceReferenceExpression) {
            return GlobalSymbols.staticScope;
        } else {
            throw new IllegalArgumentException("Don't know how to handle " + value);
        }
    }

    private void analyze(final Value aExpression, final PointsToAnalysisResult aAnalysisResult, final SymbolCache aSymbolCache) {
        if (aExpression instanceof VariableAssignmentExpression) {
            final VariableAssignmentExpression exp = (VariableAssignmentExpression) aExpression;
            final Variable var = exp.getVariable();
            final TypeRef varType = var.resolveType();
            final Value value = exp.incomingDataFlows().get(0);
            if (varType.isObject() || varType.isArray()) {
                final Symbol varSymbol = aSymbolCache.variableSymbolForVariable(var);
                if (value instanceof Variable) {
                    final Symbol valueSymbol = aSymbolCache.variableSymbolForVariable((Variable) value);
                    aAnalysisResult.alias(varSymbol, valueSymbol);
                } else if (value instanceof PHIValue) {
                    final Symbol valueSymbol = aSymbolCache.variableSymbolForPHI((PHIValue) value);
                    aAnalysisResult.alias(varSymbol, valueSymbol);
                } else if (value instanceof NullValue) {
                    // Null-Constant are in static scope
                    final Symbol valueSymbol = GlobalSymbols.localScope;
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
                } else if (value instanceof CurrentExceptionExpression) {
                    // Exceptions are always in static scope
                    final Symbol valueSymbol = GlobalSymbols.staticScope;
                    aAnalysisResult.alias(varSymbol, valueSymbol);
                } else if (value instanceof PtrOfExpression) {
                    // Special case, we assume everything is in static scope here
                    final Symbol valueSymbol = GlobalSymbols.staticScope;
                    aAnalysisResult.alias(varSymbol, valueSymbol);
                } else if (value instanceof MethodHandlesGeneratedLookupExpression) {
                    // The MethodHandles lookup are always in static scope
                    final Symbol valueSymbol = GlobalSymbols.staticScope;
                    aAnalysisResult.alias(varSymbol, valueSymbol);
                } else if (value instanceof MethodTypeExpression) {
                    // The MethodTypes lookup are always in static scope
                    final Symbol valueSymbol = GlobalSymbols.staticScope;
                    aAnalysisResult.alias(varSymbol, valueSymbol);
                } else if (value instanceof MethodHandleExpression) {
                    // The Methodhandles are always in static scope
                    final Symbol valueSymbol = GlobalSymbols.staticScope;
                    aAnalysisResult.alias(varSymbol, valueSymbol);
                } else if (value instanceof ResolveCallsiteInstanceExpression) {
                    // Callsites always in static scope
                    final Symbol valueSymbol = GlobalSymbols.staticScope;
                    aAnalysisResult.alias(varSymbol, valueSymbol);
                } else if (value instanceof SuperTypeOfExpression) {
                    // The Superclass of a a class reference is in static scope, too
                    final Symbol valueSymbol = GlobalSymbols.staticScope;
                    aAnalysisResult.alias(varSymbol, valueSymbol);
                } else if (value instanceof TypeOfExpression) {
                    // The runtime class of an object is in static scope, too
                    final Symbol valueSymbol = GlobalSymbols.staticScope;
                    aAnalysisResult.alias(varSymbol, valueSymbol);
                } else if (value instanceof ArrayEntryExpression) {
                    // We inherit the symbol from the array we read from
                    final Symbol arraySymbol = resolve(value.incomingDataFlows().get(0), aSymbolCache);
                    aAnalysisResult.readFrom(varSymbol, arraySymbol);
                } else if (value instanceof GetFieldExpression) {
                    // We inherit the symbol from the object we read from
                    final Symbol arraySymbol = resolve(value.incomingDataFlows().get(0), aSymbolCache);
                    aAnalysisResult.readFrom(varSymbol, arraySymbol);
                } else if (value instanceof NewInstanceAndConstructExpression) {
                    final NewInstanceAndConstructExpression newInstance = (NewInstanceAndConstructExpression) value;
                    final List<Value> params = newInstance.incomingDataFlows();
                    final PointsToAnalysisResult analysisResult = analyze(programDescriptorProvider.resolveConstructorInvocation(newInstance.getClazz(), newInstance.getSignature()));
                    final Map<Symbol, Set<Symbol>> flows = analysisResult.computeMergingFlows();
                    final AllocationSymbol alloc = aAnalysisResult.allocation(newInstance);
                    for (final Map.Entry<Symbol, Set<Symbol>> entry : flows.entrySet()) {
                        Symbol source = null;
                        if (entry.getKey() == GlobalSymbols.thisScope) {
                            source = alloc;
                        } else if (entry.getKey() instanceof ParamRef) {
                            final ParamRef p = (ParamRef) entry.getKey();
                            source = resolve(params.get(p.index() -1), aSymbolCache);
                        }
                        if (source != null) {
                            // We check where the source if flowing to
                            for (final Symbol target : entry.getValue()) {
                                if (target == GlobalSymbols.thisScope) {
                                    aAnalysisResult.writeInto(alloc, source);
                                } else if (target == GlobalSymbols.staticScope) {
                                    aAnalysisResult.writeInto(GlobalSymbols.staticScope, source);
                                } else if (target instanceof ParamRef) {
                                    final ParamRef p = (ParamRef) target;
                                    aAnalysisResult.writeInto(resolve(params.get(p.index() - 1), aSymbolCache), source);
                                }
                            }
                        }
                    }
                    aAnalysisResult.assign(varSymbol, alloc);
                } else if (value instanceof NewArrayExpression) {
                    final AllocationSymbol alloc = aAnalysisResult.allocation(value);
                    aAnalysisResult.assign(varSymbol, alloc);
                } else if (value instanceof NewMultiArrayExpression) {
                    final AllocationSymbol alloc = aAnalysisResult.allocation(value);
                    aAnalysisResult.assign(varSymbol, alloc);
                } else if (value instanceof NewInstanceExpression) {
                    final AllocationSymbol alloc = aAnalysisResult.allocation(value);
                    aAnalysisResult.assign(varSymbol, alloc);
                } else if (value instanceof InvokeDirectMethodExpression) {
                    final InvocationResultSymbol s = toSymbol(aAnalysisResult, (InvokeDirectMethodExpression) value, aSymbolCache);
                    aAnalysisResult.assign(varSymbol, s);
                } else if (value instanceof InvokeVirtualMethodExpression) {
                    final InvocationResultSymbol s = toSymbol(aAnalysisResult, (InvokeVirtualMethodExpression) value, aSymbolCache);
                    aAnalysisResult.assign(varSymbol, s);
                } else if (value instanceof InvokeStaticMethodExpression) {
                    final InvocationResultSymbol s = toSymbol(aAnalysisResult, (InvokeStaticMethodExpression) value, aSymbolCache);
                    aAnalysisResult.assign(varSymbol, s);
                } else if (value instanceof NewInstanceFromDefaultConstructorExpression) {
                    final AllocationSymbol alloc = aAnalysisResult.allocation(value);
                    aAnalysisResult.assign(varSymbol, alloc);
                } else if (value instanceof LambdaWithStaticImplExpression ||
                           value instanceof LambdaConstructorReferenceExpression ||
                           value instanceof LambdaInterfaceReferenceExpression ||
                           value instanceof LambdaVirtualReferenceExpression ||
                           value instanceof LambdaSpecialReferenceExpression) {
                    for (final Value v : aExpression.incomingDataFlows()) {
                        final TypeRef t = v.resolveType();
                        if (t.isObject() || t.isArray()) {
                            aAnalysisResult.writeInto(GlobalSymbols.staticScope, resolve(v, aSymbolCache));
                        }
                    }
                    final InvocationResultSymbol result = new InvocationResultSymbol();
                    aAnalysisResult.assign(varSymbol, result);
                } else {
                    // Assignment
                    throw new IllegalArgumentException("Unknown :" + value);
                }
            } else {
                if (value instanceof InvokeDirectMethodExpression) {
                    // Invocation result is not an object, but has to be analyzed
                    // the result can be ignored
                    toSymbol(aAnalysisResult, (InvokeDirectMethodExpression) value, aSymbolCache);
                } else if (value instanceof InvokeVirtualMethodExpression) {
                    // Invocation result is not an object, but has to be analyzed
                    // the result can be ignored
                    toSymbol(aAnalysisResult, (InvokeVirtualMethodExpression) value, aSymbolCache);
                } else if (value instanceof InvokeStaticMethodExpression) {
                    // Invocation result is not an object, but has to be analyzed
                    // the result can be ignored
                    toSymbol(aAnalysisResult, (InvokeStaticMethodExpression) value, aSymbolCache);
                }
            }
        } else if (aExpression instanceof ReturnValueExpression) {
            final ReturnValueExpression exp = (ReturnValueExpression) aExpression;
            final Value returnValue = exp.incomingDataFlows().get(0);
            final TypeRef returnValueType = returnValue.resolveType();
            if (returnValueType.isArray() || returnValueType.isObject()) {
                final Symbol returnSymbol = resolve(returnValue, aSymbolCache);
                aAnalysisResult.returns(returnSymbol);
            }
        } else if (aExpression instanceof ThrowExpression) {
            final ThrowExpression exp = (ThrowExpression) aExpression;
            final Value returnValue = exp.incomingDataFlows().get(0);
            final Symbol returnSymbol = resolve(returnValue, aSymbolCache);
            aAnalysisResult.returns(returnSymbol);
        } else if (aExpression instanceof ArrayStoreExpression) {
            final List<Value> incoming = aExpression.incomingDataFlows();
            final Symbol arraySymbol = resolve(incoming.get(0), aSymbolCache);
            final Symbol valueSymbol = resolve(incoming.get(2), aSymbolCache);
            aAnalysisResult.writeInto(arraySymbol, valueSymbol);
        } else if (aExpression instanceof PutFieldExpression) {
            final List<Value> incoming = aExpression.incomingDataFlows();
            final Symbol instanceSymbol = resolve(incoming.get(0), aSymbolCache);
            final Symbol valueSymbol = resolve(incoming.get(1), aSymbolCache);
            aAnalysisResult.writeInto(instanceSymbol, valueSymbol);
        } else if (aExpression instanceof PutStaticExpression) {
            final List<Value> incoming = aExpression.incomingDataFlows();
            final Symbol valueSymbol = resolve(incoming.get(0), aSymbolCache);
            aAnalysisResult.writeInto(GlobalSymbols.staticScope, valueSymbol);
        } else if (aExpression instanceof SetEnumConstantsExpression) {
            final List<Value> incoming = aExpression.incomingDataFlows();
            final Symbol valueSymbol = resolve(incoming.get(1), aSymbolCache);
            aAnalysisResult.writeInto(GlobalSymbols.staticScope, valueSymbol);
        } else if (aExpression instanceof InvokeDirectMethodExpression) {
            toSymbol(aAnalysisResult, (InvokeDirectMethodExpression) aExpression, aSymbolCache);
        } else if (aExpression instanceof InvokeVirtualMethodExpression) {
            toSymbol(aAnalysisResult, (InvokeVirtualMethodExpression) aExpression, aSymbolCache);
        } else if (aExpression instanceof InvokeStaticMethodExpression) {
            toSymbol(aAnalysisResult, (InvokeStaticMethodExpression) aExpression, aSymbolCache);
        } else if (aExpression instanceof GotoExpression ||
                   aExpression instanceof IFExpression ||
                   aExpression instanceof ReturnExpression ||
                   aExpression instanceof TableSwitchExpression ||
                   aExpression instanceof LookupSwitchExpression ||
                   aExpression instanceof SetMemoryLocationExpression ||
                   aExpression instanceof UnreachableExpression) {
            // Not relevant for analysis
        } else {
            throw new IllegalArgumentException("Not supported expression : " + aExpression);
        }
    }

    private InvocationResultSymbol toSymbol(final PointsToAnalysisResult aAnalysisResult, final InvokeDirectMethodExpression aExpression, final SymbolCache aSymbolCache) {
        final List<Value> params = aExpression.incomingDataFlows();
        final ProgramDescriptor descriptor = programDescriptorProvider.resolveDirectInvocation(aExpression.getInvokedClass(), aExpression.getMethodName(), aExpression.getSignature());

        if (descriptor == null || descriptor.method().getAccessFlags().isNative() || descriptor.program() == null) {
            // We assume everything is flowing into static scope here
            for (final Value v : aExpression.incomingDataFlows()) {
                final TypeRef t = v.resolveType();
                if (t.isObject() || t.isArray()) {
                    aAnalysisResult.writeInto(GlobalSymbols.staticScope, resolve(v, aSymbolCache));
                }
            }
            return new InvocationResultSymbol();
        }

        final PointsToAnalysisResult analysisResult = analyze(descriptor);
        final Map<Symbol, Set<Symbol>> flows = analysisResult.computeMergingFlows();
        final InvocationResultSymbol invocationResult = new InvocationResultSymbol();
        for (final Map.Entry<Symbol, Set<Symbol>> entry : flows.entrySet()) {
            Symbol source = null;
            if (entry.getKey() == GlobalSymbols.thisScope) {
                source = resolve(params.get(0), aSymbolCache);
            } else if (entry.getKey() instanceof ParamRef) {
                final ParamRef p = (ParamRef) entry.getKey();
                source = resolve(params.get(p.index()), aSymbolCache);
            }
            if (source != null) {
                // We check where the source if flowing to
                for (final Symbol target : entry.getValue()) {
                    if (target == GlobalSymbols.thisScope) {
                        aAnalysisResult.writeInto(resolve(params.get(0), aSymbolCache), source);
                    } else if (target == GlobalSymbols.staticScope) {
                        aAnalysisResult.writeInto(GlobalSymbols.staticScope, source);
                    } else if (target == GlobalSymbols.returnScope) {
                        aAnalysisResult.writeInto(invocationResult, source);
                    } else if (target instanceof ParamRef) {
                        final ParamRef p = (ParamRef) target;
                        aAnalysisResult.writeInto(resolve(params.get(p.index()), aSymbolCache), source);
                    }
                }
            }
        }
        return invocationResult;
    }

    private InvocationResultSymbol toSymbol(final PointsToAnalysisResult aAnalysisResult, final InvokeStaticMethodExpression aExpression, final SymbolCache aSymbolCache) {
        final List<Value> params = aExpression.incomingDataFlows();

        final ProgramDescriptor descriptor = programDescriptorProvider.resolveStaticInvocation(aExpression.getInvokedClass(), aExpression.getMethodName(), aExpression.getSignature());
        if (descriptor == null || descriptor.method().getAccessFlags().isNative() || descriptor.program() == null) {
            // We assume everything is flowing into static scope here
            for (final Value v : aExpression.incomingDataFlows()) {
                final TypeRef t = v.resolveType();
                if (t.isObject() || t.isArray()) {
                    aAnalysisResult.writeInto(GlobalSymbols.staticScope, resolve(v, aSymbolCache));
                }
            }
            return new InvocationResultSymbol();
        }

        final PointsToAnalysisResult analysisResult = analyze(descriptor);
        final Map<Symbol, Set<Symbol>> flows = analysisResult.computeMergingFlows();

        final InvocationResultSymbol invocationResult = new InvocationResultSymbol();
        for (final Map.Entry<Symbol, Set<Symbol>> entry : flows.entrySet()) {
            Symbol source = null;
            if (entry.getKey() == GlobalSymbols.thisScope) {
                throw new IllegalArgumentException("There should be no this scope in static invocations");
            } else if (entry.getKey() instanceof ParamRef) {
                final ParamRef p = (ParamRef) entry.getKey();
                source = resolve(params.get(p.index() - 1), aSymbolCache);
            }
            if (source != null) {
                // We check where the source if flowing to
                for (final Symbol target : entry.getValue()) {
                    if (target == GlobalSymbols.thisScope) {
                        throw new IllegalArgumentException("There should be no this scope in static invocations");
                    } else if (target == GlobalSymbols.staticScope) {
                        aAnalysisResult.writeInto(GlobalSymbols.staticScope, source);
                    } else if (target == GlobalSymbols.returnScope) {
                        aAnalysisResult.writeInto(invocationResult, source);
                    } else if (target instanceof ParamRef) {
                        final ParamRef p = (ParamRef) target;
                        aAnalysisResult.writeInto(resolve(params.get(p.index() - 1), aSymbolCache), source);
                    }
                }
            }
        }
        return invocationResult;
    }

    private InvocationResultSymbol toSymbol(final PointsToAnalysisResult aAnalysisResult, final InvokeVirtualMethodExpression aExpression, final SymbolCache aSymbolCache) {
        // All arguments escape to static context, because we do not know
        for (final Value v : aExpression.incomingDataFlows()) {
            final TypeRef t = v.resolveType();
            if (t.isObject() || t.isArray()) {
                aAnalysisResult.writeInto(GlobalSymbols.staticScope, resolve(v, aSymbolCache));
            }
        }
        return new InvocationResultSymbol();
    }
}
