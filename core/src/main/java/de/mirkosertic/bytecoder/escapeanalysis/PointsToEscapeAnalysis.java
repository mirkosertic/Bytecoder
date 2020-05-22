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
package de.mirkosertic.bytecoder.escapeanalysis;

import de.mirkosertic.bytecoder.api.Logger;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.graph.Edge;
import de.mirkosertic.bytecoder.graph.EdgeType;
import de.mirkosertic.bytecoder.graph.Node;
import de.mirkosertic.bytecoder.ssa.ArrayEntryExpression;
import de.mirkosertic.bytecoder.ssa.ArrayStoreExpression;
import de.mirkosertic.bytecoder.ssa.BlockState;
import de.mirkosertic.bytecoder.ssa.ClassReferenceValue;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.CurrentExceptionExpression;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.GetFieldExpression;
import de.mirkosertic.bytecoder.ssa.GetStaticExpression;
import de.mirkosertic.bytecoder.ssa.InvocationExpression;
import de.mirkosertic.bytecoder.ssa.InvokeDirectMethodExpression;
import de.mirkosertic.bytecoder.ssa.InvokeStaticMethodExpression;
import de.mirkosertic.bytecoder.ssa.InvokeVirtualMethodExpression;
import de.mirkosertic.bytecoder.ssa.LambdaConstructorReferenceExpression;
import de.mirkosertic.bytecoder.ssa.LambdaInterfaceReferenceExpression;
import de.mirkosertic.bytecoder.ssa.LambdaSpecialReferenceExpression;
import de.mirkosertic.bytecoder.ssa.LambdaVirtualReferenceExpression;
import de.mirkosertic.bytecoder.ssa.LambdaWithStaticImplExpression;
import de.mirkosertic.bytecoder.ssa.MethodParameterValue;
import de.mirkosertic.bytecoder.ssa.NewArrayExpression;
import de.mirkosertic.bytecoder.ssa.NewInstanceFromDefaultConstructorExpression;
import de.mirkosertic.bytecoder.ssa.NewMultiArrayExpression;
import de.mirkosertic.bytecoder.ssa.NewInstanceAndConstructExpression;
import de.mirkosertic.bytecoder.ssa.NewInstanceExpression;
import de.mirkosertic.bytecoder.ssa.NullValue;
import de.mirkosertic.bytecoder.ssa.PHIValue;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.PutFieldExpression;
import de.mirkosertic.bytecoder.ssa.PutStaticExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.ResolveCallsiteInstanceExpression;
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.SelfReferenceParameterValue;
import de.mirkosertic.bytecoder.ssa.SetEnumConstantsExpression;
import de.mirkosertic.bytecoder.ssa.StringValue;
import de.mirkosertic.bytecoder.ssa.ThrowExpression;
import de.mirkosertic.bytecoder.ssa.TypeOfExpression;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.ValueWithEscapeCheck;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

public class PointsToEscapeAnalysis {

    enum PointsTo implements EdgeType {
        to
    }

    static class GraphNode extends Node<GraphNode, PointsTo> {

        private final Value value;

        public GraphNode(final Value value) {
            this.value = value;
        }
    }

    static class Scope {

        final Set<Scope> flowsInto;

        Scope() {
            flowsInto = new HashSet<>();
        }

        public void flowsInto(final Scope otherScope) {
            flowsInto.add(otherScope);
            otherScope.receivesFrom(this);
        }

        public void receivesFrom(final Scope otherScope) {
        }
    }

    static class LocalScope extends Scope {

    }

    static class StaticScope extends Scope {

    }

    static class ReturnScope extends Scope {

    }

    static class InvocationResultScope extends Scope {

    }

    static class PHIScope extends Scope {

        private final Set<Scope> mergingScopes;

        public PHIScope(final Set<Scope> mergingScopes) {
            this.mergingScopes = mergingScopes;
        }

        @Override
        public void receivesFrom(final Scope otherScope) {
            for (final Scope merged : mergingScopes) {
                otherScope.flowsInto(merged);
            }
        }
    }

    static class ThisScope extends Scope {
        public ThisScope() {
        }
    }

    static class MethodParameterScope extends Scope {
        private final MethodParameterValue methodParameterValue;

        public MethodParameterScope(final MethodParameterValue methodParameterValue) {
            this.methodParameterValue = methodParameterValue;
        }

        public int parameterIndex() {
            return methodParameterValue.getParameterIndex();
        }
    }

    public static class AnalysisResult {
        private final Map<Value, GraphNode> nodes;
        private final Set<Value> escapedValues;
        private final Map<Variable, Set<Scope>> argumentFlows;
        private final Program program;
        private final Map<Value, Scope> scopes;
        private final Set<Scope> returnFlows;

        AnalysisResult(final Program aProgram) {
            program = aProgram;
            scopes = new HashMap<>();
            nodes = new HashMap<>();
            escapedValues = new HashSet<>();
            argumentFlows = new HashMap<>();
            returnFlows = new HashSet<>();
        }

        public Program program() {
            return program;
        }

        private GraphNode nodeFor(final Value v) {
            return nodes.computeIfAbsent(v, GraphNode::new);
        }

        void escapedValue(final Value v) {
            escapedValues.add(v);
            if (v instanceof ValueWithEscapeCheck) {
                ((ValueWithEscapeCheck) v).markAsEscaped();
            }
        }

        void setArgumentFlows(final Variable argument, final Set<Scope> flowsInto) {
            argumentFlows.put(argument, flowsInto);
        }

        public Set<Value> escapedValues() {
            return escapedValues;
        }

        public Set<Scope> returnFlows() {
            return returnFlows;
        }

        public Set<Scope> argumentsFlowsFor(final Variable v) {
            return argumentFlows.get(v);
        }

        private String toScopeDebugLabel(final Scope scope) {
            return toScopeDebugLabel(scope, true);
        }

        private String toScopeDebugLabel(final Scope scope, final boolean includeFlow) {
            String label;
            if (scope instanceof MethodParameterScope) {
                final MethodParameterScope mp = (MethodParameterScope) scope;
                label =  "MethodParameterScope #" + mp.methodParameterValue.getParameterIndex();
            }else if (scope instanceof LocalScope) {
                label =  "LocalScope #" + System.identityHashCode(scope);
            } else if (scope == null) {
                label = "Unknown Scope";
            } else {
                label = scope.getClass().getSimpleName();
            }
            if (scope != null && includeFlow) {
                for (final Scope f : scope.flowsInto) {
                    label += "\\nFlows into " + toScopeDebugLabel(f, false);
                }
            }
            return label;
        }

        public void printDebugDotTree() {
            System.out.println("digraph refflow {");
            for (final GraphNode v: nodes.values()) {
                if (v.value instanceof MethodParameterValue || v.value instanceof SelfReferenceParameterValue) {
                    String label;
                    if (v.value instanceof MethodParameterValue) {
                        label = "arg" + ((MethodParameterValue) v.value).getParameterIndex();
                    } else {
                        label = "this";
                    }
                    label+="\\n\\n" + toScopeDebugLabel(scopes.get(v.value));
                    System.out.println(" n_" + System.identityHashCode(v) + "[color=blue fontcolor=blue shape=octagon label=\"" + label + "\"];");
                } else if (v.value instanceof Variable) {
                    String label = ((Variable) v.value).getName();
                    label+="\\n\\n" + toScopeDebugLabel(scopes.get(v.value));
                    if (program.getArguments().contains(v.value)) {
                        if (escapedValues.contains(v.value)) {
                            System.out.println(" n_" + System.identityHashCode(v) + "[color=red fontcolor=white style=filled fillcolor=red shape=octagon label=\"" + label + "\"];");
                        } else {
                            System.out.println(" n_" + System.identityHashCode(v) + "[shape=octagon label=\"" + label + "\"];");
                        }

                    } else {
                        System.out.println(" n_" + System.identityHashCode(v) + "[label=\"" + label + "\"];");
                    }
                } else {
                    String label = v.value.getClass().getSimpleName();
                    if (v.value instanceof PHIValue) {
                        label += " " + System.identityHashCode(v.value);
                    }
                    label+="\\n\\n" + toScopeDebugLabel(scopes.get(v.value));
                    if (escapedValues.contains(v.value)) {
                        System.out.println(" n_" + System.identityHashCode(v) + "[color=red shape=box fontcolor=white style=filled fillcolor=red label=\"" + label + "\"];");
                    } else {
                        System.out.println(" n_" + System.identityHashCode(v) + "[shape=box label=\"" + label + "\"];");
                    }
                }
                v.outgoingEdges().map(Edge::targetNode).forEach(t -> {
                    System.out.print(" n_" + System.identityHashCode(v) + " -> ");
                    System.out.println(" n_" + System.identityHashCode(t));
                });
            }
            System.out.println("}");
        }
    }

    static class CachedAnalysisResult {
        final ProgramDescriptor programDescriptor;
        final AnalysisResult analysisResult;

        public CachedAnalysisResult(final ProgramDescriptor programDescriptor, final AnalysisResult analysisResult) {
            this.programDescriptor = programDescriptor;
            this.analysisResult = analysisResult;
        }
    }

    private final ProgramDescriptorProvider programDescriptorProvider;
    private final Stack<CachedAnalysisResult> analysisStack;
    private final Map<BytecodeLinkedClass, List<CachedAnalysisResult>> cache;
    private final Logger logger;

    public PointsToEscapeAnalysis(final ProgramDescriptorProvider aProgramDescriptorProvider, final Logger aLogger) {
        programDescriptorProvider = aProgramDescriptorProvider;
        analysisStack = new Stack<>();
        cache = new HashMap<>();
        logger = aLogger;
    }

    public AnalysisResult analyze(final ProgramDescriptor aProgramDescriptor) {

        // We search in the cache
        final List<CachedAnalysisResult> cachedForCurrentClass = cache.computeIfAbsent(aProgramDescriptor.linkedClass, t -> new ArrayList<>());
        for (final CachedAnalysisResult entry : cachedForCurrentClass) {
            if (entry.programDescriptor.linkedClass == aProgramDescriptor.linkedClass && entry.programDescriptor.method == aProgramDescriptor.method) {
                // We have it already computed
                return entry.analysisResult;
            }
        }

        // We check for recursion in the analysis
        for (final CachedAnalysisResult stackEntry : analysisStack) {
            if (stackEntry.programDescriptor.linkedClass == aProgramDescriptor.linkedClass && stackEntry.programDescriptor.method == aProgramDescriptor.method) {
                // Recursion cannot be analyzed here
                // We assume that every argument escapes to the static flow
                final AnalysisResult recursionResult = new AnalysisResult(stackEntry.programDescriptor.program);
                final StaticScope staticScope = new StaticScope();
                for (final Variable v : stackEntry.programDescriptor.program.getArguments()) {
                    final TypeRef t = v.resolveType();
                    if (t.isArray() || t.isObject()) {
                        recursionResult.escapedValue(v);
                        recursionResult.setArgumentFlows(v, Collections.singleton(staticScope));
                    }
                }
                return recursionResult;
            }
        }

        logger.info(" Analyzing {}.{} {}", aProgramDescriptor.linkedClass.getClassName().name(), aProgramDescriptor.method.getName().stringValue(), aProgramDescriptor.method.getSignature());

        final AnalysisResult analysisResult = new AnalysisResult(aProgramDescriptor.program);
        analysisStack.push(new CachedAnalysisResult(aProgramDescriptor, analysisResult));

        final StaticScope staticScope = new StaticScope();
        final ReturnScope returnScope = new ReturnScope();

        // Step 1 : Build a reference flow graph
        for (final Variable v : aProgramDescriptor.program.getArguments()) {
            final TypeRef theType = v.resolveType();
            if (theType.isArray() || theType.isObject()) {
                final GraphNode varNode = analysisResult.nodeFor(v);
                final GraphNode initValue = analysisResult.nodeFor(v.incomingDataFlows().get(0));
                initValue.addEdgeTo(PointsTo.to, varNode);
            }
        }

        final ControlFlowGraph g = aProgramDescriptor.program.getControlFlowGraph();
        final Set<PHIValue> alreadyKnownPHIvalues = new HashSet<>();
        for (final RegionNode theNode : g.dominators().getPreOrder()) {
            final BlockState theLiveIn = theNode.liveIn();
            theLiveIn.getPorts().values().stream()
                    .filter(t -> t instanceof PHIValue)
                    .map(t -> (PHIValue) t)
                    .filter(t -> t.resolveType().isObject() || t.resolveType().isArray())
                    .filter(alreadyKnownPHIvalues::add).forEach(t -> {

                final GraphNode phiNode = analysisResult.nodeFor(t);
                for (final RegionNode pred : theNode.getPredecessors()) {
                    if (pred != theNode) {
                        final BlockState theOut = pred.liveOut();
                        final Value incomingValue = theOut.getPorts().get(t.getDescription());
                        final GraphNode incomingNode = analysisResult.nodeFor(incomingValue);

                        if (incomingValue instanceof PHIValue && alreadyKnownPHIvalues.contains((incomingValue))) {
                            continue;
                        }

                        if (incomingNode != phiNode) {
                            if (incomingNode.outgoingEdges().map(Edge::targetNode).noneMatch(x -> x != phiNode)) {
                                incomingNode.addEdgeTo(PointsTo.to, phiNode);
                            }
                        }
                    }
                }
            });
            analyze(theNode.getExpressions(), analysisResult);
        }

        // Step 2: we compute the scope flow
        final Queue<GraphNode> workingQueue = analysisResult.nodes.values().stream().filter(t -> t.incomingEdges().count() == 0).distinct().collect(Collectors.toCollection(LinkedList::new));
        while (!workingQueue.isEmpty()) {
            final GraphNode currentEntry = workingQueue.poll();
            final Set<GraphNode> theOutgoing = currentEntry.outgoingEdges().map(Edge::targetNode).filter(t -> !analysisResult.scopes.containsKey(t.value)).collect(Collectors.toSet());
            if (currentEntry.value instanceof NewArrayExpression) {
                analysisResult.scopes.put(currentEntry.value, new LocalScope());
                addNotExisting(workingQueue, theOutgoing);
            } else if (currentEntry.value instanceof NewMultiArrayExpression) {
                analysisResult.scopes.put(currentEntry.value, new LocalScope());
                addNotExisting(workingQueue, theOutgoing);
            } else if (currentEntry.value instanceof NewInstanceExpression) {
                analysisResult.scopes.put(currentEntry.value, new LocalScope());
                addNotExisting(workingQueue, theOutgoing);
            } else if (currentEntry.value instanceof GetStaticExpression) {
                analysisResult.scopes.put(currentEntry.value, staticScope);
                addNotExisting(workingQueue, theOutgoing);
            } else if (currentEntry.value instanceof CurrentExceptionExpression) {
                analysisResult.scopes.put(currentEntry.value, staticScope);
                addNotExisting(workingQueue, theOutgoing);
            } else if (currentEntry.value instanceof TypeOfExpression) {
                analysisResult.scopes.put(currentEntry.value, staticScope);
                addNotExisting(workingQueue, theOutgoing);
            } else if (currentEntry.value instanceof ResolveCallsiteInstanceExpression) {
                analysisResult.scopes.put(currentEntry.value, staticScope);
                addNotExisting(workingQueue, theOutgoing);
            } else if (currentEntry.value instanceof StringValue) {
                analysisResult.scopes.put(currentEntry.value, staticScope);
                addNotExisting(workingQueue, theOutgoing);
            } else if (currentEntry.value instanceof SelfReferenceParameterValue) {
                analysisResult.scopes.put(currentEntry.value, new ThisScope());
                addNotExisting(workingQueue, theOutgoing);
            } else if (currentEntry.value instanceof MethodParameterValue) {
                analysisResult.scopes.put(currentEntry.value, new MethodParameterScope(((MethodParameterValue) currentEntry.value)));
                addNotExisting(workingQueue, theOutgoing);
            } else {
                final Set<GraphNode> theIncoming = currentEntry.incomingEdges().map(t -> (GraphNode) t.sourceNode()).collect(Collectors.toSet());
                final Set<GraphNode> theIncomingWithScope = theIncoming.stream().filter(t -> analysisResult.scopes.containsKey(t.value)).collect(Collectors.toSet());
                if (theIncoming.size() == theIncomingWithScope.size()) {
                    // We have all predecessor scopes, so we can continue and compute the scope flow
                    // for the current node

                    if (currentEntry.value instanceof ReturnValueExpression || currentEntry.value instanceof ThrowExpression) {
                        // Terminal instruction
                        analysisResult.scopes.put(currentEntry.value, returnScope);
                        theIncomingWithScope.stream().map(t -> analysisResult.scopes.get(t.value)).forEach(scope -> scope.flowsInto(returnScope));
                    } else if (currentEntry.value instanceof PutStaticExpression) {
                        // Terminal instruction
                        analysisResult.scopes.put(currentEntry.value, staticScope);
                        theIncomingWithScope.stream().map(t -> analysisResult.scopes.get(t.value)).forEach(scope -> scope.flowsInto(staticScope));
                    } else if (currentEntry.value instanceof SetEnumConstantsExpression) {
                        // Terminal instruction
                        analysisResult.scopes.put(currentEntry.value, staticScope);
                        theIncomingWithScope.stream().map(t -> analysisResult.scopes.get(t.value)).forEach(scope -> scope.flowsInto(staticScope));
                    } else if (currentEntry.value instanceof ClassReferenceValue) {
                        final Scope newScope = new StaticScope();
                        analysisResult.scopes.put(currentEntry.value, newScope);
                        theIncoming.stream().map(t -> analysisResult.scopes.get(t.value)).forEach(t -> t.flowsInto(newScope));
                        addNotExisting(workingQueue, theOutgoing);
                    } else if (currentEntry.value instanceof NullValue) {
                        final Scope newScope = new LocalScope();
                        analysisResult.scopes.put(currentEntry.value, newScope);
                        theIncoming.stream().map(t -> analysisResult.scopes.get(t.value)).forEach(t -> t.flowsInto(newScope));
                        addNotExisting(workingQueue, theOutgoing);
                    } else {

                        final Set<GraphNode> theArrayWrites = theIncoming.stream().filter(t -> t.value instanceof ArrayStoreExpression).collect(Collectors.toSet());
                        final Set<GraphNode> thePutFields = theIncoming.stream().filter(t -> t.value instanceof PutFieldExpression).collect(Collectors.toSet());
                        if (!theArrayWrites.isEmpty() && theIncomingWithScope.size() >= 2) {

                            final GraphNode theValue = theArrayWrites.iterator().next();
                            theIncoming.stream().filter(t -> t != theValue).forEach(array -> {
                                if (!analysisResult.scopes.containsKey(currentEntry.value)) {
                                    analysisResult.scopes.put(currentEntry.value, analysisResult.scopes.get(array.value));
                                }
                                analysisResult.scopes.get(theValue.value).flowsInto(analysisResult.scopes.get(array.value));
                            });

                        } else if (!thePutFields.isEmpty() && theIncomingWithScope.size() >= 2) {

                            final GraphNode theValue = thePutFields.iterator().next();
                            theIncoming.stream().filter(t -> t != theValue).forEach(instance -> {
                                if (!analysisResult.scopes.containsKey(currentEntry.value)) {
                                    analysisResult.scopes.put(currentEntry.value, analysisResult.scopes.get(instance.value));
                                }
                                analysisResult.scopes.get(theValue.value).flowsInto(analysisResult.scopes.get(instance.value));
                            });

                            analysisResult.scopes.get(theValue.value).flowsInto(analysisResult.scopes.get(currentEntry.value));

                        } else if (currentEntry.value instanceof VariableAssignmentExpression) {

                            analysisResult.scopes.put(currentEntry.value, analysisResult.scopes.get(theIncomingWithScope.iterator().next().value));
                            addNotExisting(workingQueue, theOutgoing);

                        } else if (currentEntry.value instanceof NewInstanceFromDefaultConstructorExpression) {

                            final InvocationResultScope invocationScope = new InvocationResultScope();

                            final NewInstanceFromDefaultConstructorExpression x = (NewInstanceFromDefaultConstructorExpression) currentEntry.value;
                            // We are pretty sure the runtime implementation does not let escape anything, so we are safe here

                            // All dataflows are now defined, we can continue with the new scope from here
                            analysisResult.scopes.put(currentEntry.value, invocationScope);

                            addNotExisting(workingQueue, theOutgoing);

                        } else if (currentEntry.value instanceof NewInstanceAndConstructExpression) {

                            final InvocationResultScope invocationScope = new InvocationResultScope();

                            final NewInstanceAndConstructExpression x = (NewInstanceAndConstructExpression) currentEntry.value;
                            // We analyze the constructor invocation
                            final AnalysisResult constructorAnalysis = analyze(programDescriptorProvider.resolveConstructorInvocation(x.getClazz(), x.getSignature()));
                            final List<Value> constructorArguments = currentEntry.value.incomingDataFlows();

                            // Now, for every argument, we check if it escapes to other scopes
                            for (int i = 0; i < constructorAnalysis.program.getArguments().size(); i++) {
                                final Variable programArgument = constructorAnalysis.program.argumentAt(i);
                                final TypeRef argumentType = programArgument.resolveType();
                                if (argumentType.isArray() || argumentType.isObject()) {
                                    final Scope incomingScope;
                                    if (i == 0) {
                                        incomingScope = invocationScope;
                                    } else {
                                        incomingScope = analysisResult.scopes.get(constructorArguments.get(i - 1));
                                    }
                                    for (final Scope s : constructorAnalysis.argumentsFlowsFor(programArgument)) {
                                        if (s instanceof StaticScope) {
                                            incomingScope.flowsInto(staticScope);
                                        } else if (s instanceof ReturnScope) {
                                            throw new IllegalArgumentException("Constructors must not have a return scope!");
                                        } else if (s instanceof ThisScope) {
                                            incomingScope.flowsInto(invocationScope);
                                        } else if (s instanceof MethodParameterScope) {
                                            final MethodParameterScope mp = (MethodParameterScope) s;
                                            final Scope mpScope = analysisResult.scopes.get(constructorArguments.get(mp.parameterIndex() - 1));
                                            incomingScope.flowsInto(mpScope);
                                        }
                                    }
                                }
                            }

                            // All dataflows are now defined, we can continue with the new scope from here
                            analysisResult.scopes.put(currentEntry.value, invocationScope);

                            addNotExisting(workingQueue, theOutgoing);

                        } else if (currentEntry.value instanceof InvokeDirectMethodExpression) {

                            final InvocationResultScope invocationScope = new InvocationResultScope();

                            final InvokeDirectMethodExpression x = (InvokeDirectMethodExpression) currentEntry.value;

                            // We analyze the method invocation
                            final ProgramDescriptor pd = programDescriptorProvider.resolveDirectInvocation(x.getClazz(), x.getMethodName(), x.getSignature());
                            if (pd == null || pd.method.getAccessFlags().isNative()) {
                                // We assume everything escapes for native methods
                                for (final Value v : currentEntry.value.incomingDataFlows()) {
                                    final TypeRef argumentType = v.resolveType();
                                    if (argumentType.isArray() || argumentType.isObject()) {
                                        final Scope incomingScope = analysisResult.scopes.get(v);
                                        incomingScope.flowsInto(staticScope);
                                    }
                                }

                                // All dataflows are now defined, we can continue with the new scope from here
                                analysisResult.scopes.put(currentEntry.value, invocationScope);

                                addNotExisting(workingQueue, theOutgoing);

                            } else {

                                final AnalysisResult methodAnalysisAnalysis = analyze(pd);
                                final List<Value> methodArguments = currentEntry.value.incomingDataFlows();

                                // Now, for every argument, we check if it escapes to other scopes
                                for (int i = 0; i < methodAnalysisAnalysis.program.getArguments().size(); i++) {
                                    final Variable programArgument = methodAnalysisAnalysis.program.argumentAt(i);
                                    final TypeRef argumentType = programArgument.resolveType();
                                    if (argumentType.isArray() || argumentType.isObject()) {
                                        final Scope incomingScope = analysisResult.scopes.get(methodArguments.get(i));
                                        for (final Scope s : methodAnalysisAnalysis.argumentsFlowsFor(programArgument)) {
                                            if (s instanceof StaticScope) {
                                                incomingScope.flowsInto(staticScope);
                                            } else if (s instanceof ReturnScope) {
                                                incomingScope.flowsInto(invocationScope);
                                            } else if (s instanceof ThisScope) {
                                                incomingScope.flowsInto(analysisResult.scopes.get(methodArguments.get(0)));
                                            } else if (s instanceof MethodParameterScope) {
                                                final MethodParameterScope mp = (MethodParameterScope) s;
                                                final Scope mpScope = analysisResult.scopes.get(methodArguments.get(mp.parameterIndex()));
                                                incomingScope.flowsInto(mpScope);
                                            }
                                        }
                                    }
                                }

                                // All dataflows are now defined, we can continue with the new scope from here
                                analysisResult.scopes.put(currentEntry.value, invocationScope);

                                addNotExisting(workingQueue, theOutgoing);
                            }

                        } else if (currentEntry.value instanceof LambdaWithStaticImplExpression ||
                                   currentEntry.value instanceof LambdaInterfaceReferenceExpression ||
                                   currentEntry.value instanceof LambdaVirtualReferenceExpression ||
                                   currentEntry.value instanceof LambdaConstructorReferenceExpression ||
                                   currentEntry.value instanceof LambdaSpecialReferenceExpression) {

                            // Lambda factories are tricky.
                            // We might analyze the code for most of them excluding the interface and virtual invocations
                            // but for now we assume everything is escaping to static scope here

                            final InvocationResultScope invocationScope = new InvocationResultScope();

                            for (final Value v : currentEntry.value.incomingDataFlows()) {
                                final TypeRef argumentType = v.resolveType();
                                if (argumentType.isArray() || argumentType.isObject()) {
                                    final Scope incomingScope = analysisResult.scopes.get(v);
                                    incomingScope.flowsInto(staticScope);
                                }
                            }

                            // All dataflows are now defined, we can continue with the new scope from here
                            analysisResult.scopes.put(currentEntry.value, invocationScope);

                            addNotExisting(workingQueue, theOutgoing);

                        } else if (currentEntry.value instanceof InvokeVirtualMethodExpression) {

                            // Virtual invocations are tricky, as we might also call a lambda with
                            // a delegating static implementation. This is currently impossible to figure out
                            // so we go the way to think all arguments escape to static scope here

                            // TODO: Check which classes are used in lambdas here to make a better guess

                            final InvocationResultScope invocationScope = new InvocationResultScope();

                            final InvokeVirtualMethodExpression x = (InvokeVirtualMethodExpression) currentEntry.value;
                            for (final Value v : x.incomingDataFlows()) {
                                final TypeRef argumentType = v.resolveType();
                                if (argumentType.isArray() || argumentType.isObject()) {
                                    final Scope incomingScope = analysisResult.scopes.get(v);
                                    incomingScope.flowsInto(staticScope);
                                }
                            }

                            // All dataflows are now defined, we can continue with the new scope from here
                            analysisResult.scopes.put(currentEntry.value, invocationScope);

                            addNotExisting(workingQueue, theOutgoing);

                        } else if (currentEntry.value instanceof InvokeStaticMethodExpression) {

                            final InvocationResultScope invocationScope = new InvocationResultScope();
                            final InvokeStaticMethodExpression x = (InvokeStaticMethodExpression) currentEntry.value;
                            final ProgramDescriptor pd = programDescriptorProvider.resolveStaticInvocation(x.getClassName(), x.getMethodName(), x.getSignature());
                            if (pd == null || pd.method.getAccessFlags().isNative()) {
                                // We assume everything escapes for native methods
                                for (final Value v : currentEntry.value.incomingDataFlows()) {
                                    final TypeRef argumentType = v.resolveType();
                                    if (argumentType.isArray() || argumentType.isObject()) {
                                        final Scope incomingScope = analysisResult.scopes.get(v);
                                        incomingScope.flowsInto(staticScope);
                                    }
                                }

                                // All dataflows are now defined, we can continue with the new scope from here
                                analysisResult.scopes.put(currentEntry.value, invocationScope);

                                addNotExisting(workingQueue, theOutgoing);

                            } else {
                                final AnalysisResult staticAnalysis = analyze(pd);
                                final List<Value> staticArguments = currentEntry.value.incomingDataFlows();

                                // Now, for every argument, we check if it escapes to other scopes
                                for (int i = 0; i < staticAnalysis.program.getArguments().size(); i++) {
                                    final Variable programArgument = staticAnalysis.program.argumentAt(i);
                                    final TypeRef argumentType = programArgument.resolveType();
                                    if (argumentType.isArray() || argumentType.isObject()) {
                                        final Scope incomingScope = analysisResult.scopes.get(staticArguments.get(i));
                                        for (final Scope s : staticAnalysis.argumentsFlowsFor(programArgument)) {
                                            if (s instanceof StaticScope) {
                                                incomingScope.flowsInto(staticScope);
                                            } else if (s instanceof ReturnScope) {
                                                incomingScope.flowsInto(invocationScope);
                                            } else if (s instanceof ThisScope) {
                                                throw new IllegalArgumentException("this cannot be used in a static context!");
                                            } else if (s instanceof MethodParameterScope) {
                                                final MethodParameterScope mp = (MethodParameterScope) s;
                                                final Scope mpScope = analysisResult.scopes.get(staticArguments.get(mp.parameterIndex() - 1));
                                                incomingScope.flowsInto(mpScope);
                                            }
                                        }
                                    }
                                }

                                // All dataflows are now defined, we can continue with the new scope from here
                                analysisResult.scopes.put(currentEntry.value, invocationScope);

                                addNotExisting(workingQueue, theOutgoing);
                            }
                        } else {

                            if (theIncomingWithScope.size() == 1) {
                                final Scope newScope = analysisResult.scopes.get(theIncomingWithScope.iterator().next().value);
                                analysisResult.scopes.put(currentEntry.value, newScope);
                            } else if (currentEntry.value instanceof PHIValue) {
                                final Scope newScope = analysisResult.scopes.computeIfAbsent(currentEntry.value, key -> new PHIScope(theIncomingWithScope.stream().map(t -> analysisResult.scopes.get(t.value)).collect(Collectors.toSet())));
                                analysisResult.scopes.put(currentEntry.value, newScope);
                                theIncoming.stream().map(t -> analysisResult.scopes.get(t.value)).filter(t -> t != newScope).forEach(t -> t.flowsInto(newScope));
                            } else if (theIncomingWithScope.size() > 1) {
                                // Should not happen due to SSA form
                                throw new IllegalArgumentException("Don't know how to handle multiple flow assignments for " + currentEntry.value);
                            }

                            addNotExisting(workingQueue, theOutgoing);
                        }
                    }
                } else {
                    // Condition not met, we try again later
                    workingQueue.add(currentEntry);
                }
            }
        }

        // Step 3: Compute escaping allocations of flows for the arguments
        for (final Variable v : aProgramDescriptor.program.getArguments()) {
            final TypeRef theType = v.resolveType();
            if (theType.isObject() || theType.isArray()) {
                final Set<Scope> targetScopes = new HashSet<>();
                final Stack<Scope> workingStack = new Stack<>();
                final Set<Scope> alreadySeen = new HashSet<>();

                final Value selfValue = v.incomingDataFlows().get(0);
                final Scope selfScope = analysisResult.scopes.get(selfValue);
                workingStack.push(selfScope);

                while (!workingStack.isEmpty()) {
                    final Scope current = workingStack.pop();
                    if (alreadySeen.add(current)) {
                        if (current != selfScope && (current instanceof ThisScope || current instanceof MethodParameterScope || current instanceof StaticScope || current instanceof ReturnScope)) {
                            targetScopes.add(current);
                            analysisResult.escapedValue(v);
                        }

                        workingStack.addAll(current.flowsInto);
                    }
                }

                analysisResult.setArgumentFlows(v, targetScopes);
            }
        }

        // Step 4 : Compute escapes for allocations
        analysisResult.scopes.entrySet().stream().filter(t ->
                t.getKey() instanceof NewArrayExpression ||
                t.getKey() instanceof NewMultiArrayExpression ||
                t.getKey() instanceof NewInstanceAndConstructExpression).forEach(entry -> {

            final Stack<Scope> workingStack = new Stack<>();
            final Set<Scope> alreadySeen = new HashSet<>();
            workingStack.push(entry.getValue());
            loop: while (!workingStack.isEmpty()) {
                final Scope current = workingStack.pop();
                if (alreadySeen.add(current)) {
                    if (current instanceof ReturnScope ||
                        current instanceof ThisScope ||
                        current instanceof StaticScope ||
                        current instanceof MethodParameterScope) {

                        analysisResult.escapedValue(entry.getKey());
                        break loop;
                    }
                    workingStack.addAll(current.flowsInto);
                }
            }
        });

        // Step 5 : Compute returning flows
        analysisResult.scopes.values().stream().filter(t -> !(t instanceof ReturnScope) && !(t instanceof PHIScope)).forEach(t -> {
            final Stack<Scope> workingStack = new Stack<>();
            final Set<Scope> alreadySeen = new HashSet<>();
            workingStack.push(t);
            while (!workingStack.isEmpty()) {
                final Scope s = workingStack.pop();
                if (alreadySeen.add(s)) {
                    if (s instanceof ReturnScope) {
                        analysisResult.returnFlows.add(t);
                        return;
                    }
                    workingStack.addAll(s.flowsInto);
                }
            }
        });

        // We put it into the cache
        analysisStack.pop();
        cachedForCurrentClass.add(new CachedAnalysisResult(aProgramDescriptor, analysisResult));

        return analysisResult;
    }

    private void analyze(final ExpressionList aExpressionList, final AnalysisResult analysisResult) {
        for (final Expression theExpression : aExpressionList.toList()) {
            if (theExpression instanceof ExpressionListContainer) {
                final ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                for (final ExpressionList theList : theContainer.getExpressionLists()) {
                    analyze(theList, analysisResult);
                }
            }

            analyze(theExpression, analysisResult);
        }
    }

    private void analyze(final Value aExpression, final AnalysisResult analysisResult) {
        if (aExpression instanceof Variable) {
            // Nothing to do
        } else if (aExpression instanceof VariableAssignmentExpression) {

            final VariableAssignmentExpression v = (VariableAssignmentExpression) aExpression;
            final List<Value> theIncoming = v.incomingDataFlows();
            if (theIncoming.size() != 1) {
                throw new IllegalArgumentException("Exact one incoming flow expected for " + v.getVariable());
            }

            final TypeRef theType = v.getVariable().resolveType();
            if (theType.isArray() || theType.isObject()) {
                final GraphNode varNode = analysisResult.nodeFor(v.getVariable());
                final GraphNode valueNode = analysisResult.nodeFor(theIncoming.get(0));

                valueNode.addEdgeTo(PointsTo.to, varNode);

                analyze(valueNode.value, analysisResult);
            }

        } else if (aExpression instanceof NewInstanceAndConstructExpression) {

            final NewInstanceAndConstructExpression n = (NewInstanceAndConstructExpression) aExpression;
            final GraphNode newNode = analysisResult.nodeFor(n);

            for (final Value v : n.incomingDataFlows()) {
                final TypeRef type = v.resolveType();
                if (type.isObject() || type.isArray()) {
                    final GraphNode arg = analysisResult.nodeFor(v);
                    arg.addEdgeTo(PointsTo.to, newNode);
                }
            }

        } else if (aExpression instanceof NewInstanceFromDefaultConstructorExpression) {

            final NewInstanceFromDefaultConstructorExpression n = (NewInstanceFromDefaultConstructorExpression) aExpression;
            final GraphNode newNode = analysisResult.nodeFor(n);

            for (final Value v : n.incomingDataFlows()) {
                final TypeRef type = v.resolveType();
                if (type.isObject() || type.isArray()) {
                    final GraphNode arg = analysisResult.nodeFor(v);
                    arg.addEdgeTo(PointsTo.to, newNode);
                }
            }

        } else if (aExpression instanceof InvocationExpression) {

            final InvocationExpression n = (InvocationExpression) aExpression;
            final GraphNode newNode = analysisResult.nodeFor(n);

            for (final Value v : n.incomingDataFlows()) {
                final TypeRef type = v.resolveType();
                if (type.isObject() || type.isArray()) {
                    final GraphNode arg = analysisResult.nodeFor(v);
                    arg.addEdgeTo(PointsTo.to, newNode);
                }
            }

        } else if (aExpression instanceof LambdaWithStaticImplExpression ||
                   aExpression instanceof LambdaInterfaceReferenceExpression ||
                   aExpression instanceof LambdaVirtualReferenceExpression ||
                   aExpression instanceof LambdaConstructorReferenceExpression ||
                   aExpression instanceof LambdaSpecialReferenceExpression) {

            final GraphNode newNode = analysisResult.nodeFor(aExpression);

            for (final Value v : aExpression.incomingDataFlows()) {
                final TypeRef type = v.resolveType();
                if (type.isObject() || type.isArray()) {
                    final GraphNode arg = analysisResult.nodeFor(v);
                    arg.addEdgeTo(PointsTo.to, newNode);
                }
            }

        } else if (aExpression instanceof ThrowExpression) {

            final ThrowExpression n = (ThrowExpression) aExpression;
            final GraphNode newNode = analysisResult.nodeFor(n);

            for (final Value v : n.incomingDataFlows()) {
                final TypeRef type = v.resolveType();
                if (type.isObject() || type.isArray()) {
                    final GraphNode arg = analysisResult.nodeFor(v);
                    arg.addEdgeTo(PointsTo.to, newNode);
                }
            }

        } else if (aExpression instanceof TypeOfExpression) {

            final TypeOfExpression n = (TypeOfExpression) aExpression;
            final GraphNode newNode = analysisResult.nodeFor(n);

            for (final Value v : n.incomingDataFlows()) {
                final TypeRef type = v.resolveType();
                if (type.isObject() || type.isArray()) {
                    final GraphNode arg = analysisResult.nodeFor(v);
                    arg.addEdgeTo(PointsTo.to, newNode);
                }
            }

        } else if (aExpression instanceof ReturnValueExpression) {

            final ReturnValueExpression r = (ReturnValueExpression) aExpression;
            final List<Value> theIncoming = r.incomingDataFlows();
            if (theIncoming.size() != 1) {
                throw new IllegalArgumentException("Exact one incoming flow expected for " + r);
            }

            final TypeRef theType = theIncoming.get(0).resolveType();
            if (theType.isArray() || theType.isObject()) {

                final GraphNode rNode = analysisResult.nodeFor(r);
                final GraphNode argNode = analysisResult.nodeFor(theIncoming.get(0));

                argNode.addEdgeTo(PointsTo.to, rNode);
            }

        } else if (aExpression instanceof PutFieldExpression) {

            final PutFieldExpression p = (PutFieldExpression) aExpression;
            final TypeRef theType = TypeRef.toType(p.getField().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().fieldType());
            if (theType.isArray() || theType.isObject()) {
                final List<Value> theIncoming = p.incomingDataFlows();
                final GraphNode instanceNode = analysisResult.nodeFor(theIncoming.get(0));
                final GraphNode valueNode = analysisResult.nodeFor(theIncoming.get(1));

                final GraphNode thePut = analysisResult.nodeFor(p);
                valueNode.addEdgeTo(PointsTo.to, thePut);
                thePut.addEdgeTo(PointsTo.to, instanceNode);

            }

        } else if (aExpression instanceof ResolveCallsiteInstanceExpression) {

            final GraphNode node = analysisResult.nodeFor(aExpression);

        } else if (aExpression instanceof GetStaticExpression) {

            final GraphNode node = analysisResult.nodeFor(aExpression);

        } else if (aExpression instanceof PutStaticExpression) {

            final PutStaticExpression p = (PutStaticExpression) aExpression;
            final TypeRef theType = TypeRef.toType(p.getField().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().fieldType());
            if (theType.isArray() || theType.isObject()) {
                final GraphNode putNode = analysisResult.nodeFor(p);

                final List<Value> theIncoming = p.incomingDataFlows();
                final GraphNode theValueNode = analysisResult.nodeFor(theIncoming.get(0));

                theValueNode.addEdgeTo(PointsTo.to, putNode);
            }

        } else if (aExpression instanceof SetEnumConstantsExpression) {

            final SetEnumConstantsExpression p = (SetEnumConstantsExpression) aExpression;
            final GraphNode putNode = analysisResult.nodeFor(p);

            for (final Value v : p.incomingDataFlows()) {
                final GraphNode theValueNode = analysisResult.nodeFor(v);
                theValueNode.addEdgeTo(PointsTo.to, putNode);
            }

        } else if (aExpression instanceof GetFieldExpression) {

            final GetFieldExpression p = (GetFieldExpression) aExpression;
            final TypeRef theType = TypeRef.toType(p.getField().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().fieldType());
            if (theType.isArray() || theType.isObject()) {
                final List<Value> theIncoming = p.incomingDataFlows();

                final GraphNode instanceNode = analysisResult.nodeFor(p);
                final GraphNode valueNode = analysisResult.nodeFor(theIncoming.get(0));

                valueNode.addEdgeTo(PointsTo.to, instanceNode);
            }

        } else if (aExpression instanceof NewArrayExpression) {

            // Nothing to do, no references are incoming

        } else if (aExpression instanceof NewMultiArrayExpression) {

            // Nothing to do, no references are incoming

        } else if (aExpression instanceof ArrayStoreExpression) {

            final ArrayStoreExpression x = (ArrayStoreExpression) aExpression;
            final List<Value> theIncoming = x.incomingDataFlows();

            final TypeRef theType = theIncoming.get(2).resolveType();
            if (theType.isArray() || theType.isObject()) {

                final GraphNode rNode = analysisResult.nodeFor(x);
                final GraphNode arrayNode = analysisResult.nodeFor(theIncoming.get(0));
                final GraphNode valueNode = analysisResult.nodeFor(theIncoming.get(2));

                rNode.addEdgeTo(PointsTo.to, arrayNode);
                valueNode.addEdgeTo(PointsTo.to, rNode);
            }
        } else if (aExpression instanceof ArrayEntryExpression) {

            final ArrayEntryExpression x = (ArrayEntryExpression) aExpression;
            final List<Value> theIncoming = x.incomingDataFlows();

            final TypeRef theType = x.resolveType();
            if (theType.isArray() || theType.isObject()) {

                final GraphNode rNode = analysisResult.nodeFor(x);
                final GraphNode arrayNode = analysisResult.nodeFor(theIncoming.get(0));

                arrayNode.addEdgeTo(PointsTo.to, rNode);
            }
        }
    }

    private static <T>  void addNotExisting(final Queue<T> aQueue, final Collection<T> aValues) {
        for (final T value : aValues) {
            if (!aQueue.contains(value)) {
                aQueue.add(value);
            }
        }
    }
}