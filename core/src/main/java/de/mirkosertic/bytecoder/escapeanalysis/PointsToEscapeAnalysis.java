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
import de.mirkosertic.bytecoder.ssa.EnumConstantsExpression;
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
import de.mirkosertic.bytecoder.ssa.NewInstanceAndConstructExpression;
import de.mirkosertic.bytecoder.ssa.NewInstanceExpression;
import de.mirkosertic.bytecoder.ssa.NewInstanceFromDefaultConstructorExpression;
import de.mirkosertic.bytecoder.ssa.NewMultiArrayExpression;
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
import java.util.Arrays;
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

    enum Flowdirection {
        forward, backward
    }

    static class PointsTo implements EdgeType {

        private Flowdirection flowdirection;

        public PointsTo() {
            flowdirection = Flowdirection.forward;
        }

        public PointsTo(final Flowdirection flowdirection) {
            this.flowdirection = flowdirection;
        }
    }

    static class GraphNode extends Node<GraphNode, PointsTo> {

        private final Value value;
        private String comment;

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
                    String label = ((Variable) v.value).getName() + " " + System.identityHashCode(v.value);
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
                        if (v.comment != null) {
                            label += " " + v.comment;
                        }
                    }
                    label+="\\n\\n" + toScopeDebugLabel(scopes.get(v.value));
                    if (escapedValues.contains(v.value)) {
                        System.out.println(" n_" + System.identityHashCode(v) + "[color=red shape=box fontcolor=white style=filled fillcolor=red label=\"" + label + "\"];");
                    } else {
                        System.out.println(" n_" + System.identityHashCode(v) + "[shape=box label=\"" + label + "\"];");
                    }
                }
                v.outgoingEdges().forEach(edge -> {
                    System.out.print(" n_" + System.identityHashCode(edge.sourceNode()) + " -> ");
                    if (edge.edgeType().flowdirection == Flowdirection.forward) {
                        System.out.println(" n_" + System.identityHashCode(edge.targetNode()));
                    } else {
                        System.out.println(" n_" + System.identityHashCode(edge.targetNode()) + "[style=dashed]");
                    }
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
                initValue.addEdgeTo(new PointsTo(), varNode);
            }
        }

        final ControlFlowGraph g = aProgramDescriptor.program.getControlFlowGraph();
        final Set<PHIValue> alreadyKnownPHIvalues = new HashSet<>();
        final List<RegionNode> thePreOrder = g.dominators().getPreOrder();
        for (final RegionNode theNode : thePreOrder) {
            final BlockState theLiveIn = theNode.liveIn();
            final int theNodeIndex = thePreOrder.indexOf(theNode);
            theLiveIn.getPorts().entrySet().stream()
                    .filter(t -> t.getValue() instanceof PHIValue)
                    .filter(t -> t.getValue().resolveType().isObject() || t.getValue().resolveType().isArray())
                    .filter(t -> alreadyKnownPHIvalues.add((PHIValue) t.getValue())).forEach(t -> {

                final PHIValue phiValue = (PHIValue) t.getValue();
                final GraphNode phiNode = analysisResult.nodeFor(phiValue);
                phiNode.comment = "LiveIn #" + theNode.getStartAddress().getAddress() + " @ " + t.getKey();

                for (final Value v : phiValue.incomingDataFlows()) {
                    final GraphNode s = analysisResult.nodeFor(v);
                    if (s.outgoingEdges().map(Edge::targetNode).noneMatch(x -> x == phiNode)) {
                        if (alreadyKnownPHIvalues.contains(v)) {
                            s.addEdgeTo(new PointsTo(Flowdirection.backward), phiNode);
                        } else {
                            s.addEdgeTo(new PointsTo(), phiNode);
                        }
                    }
                }
            });
            analyze(theNode.getExpressions(), analysisResult);
        }

        // Step 2: find back edges in the graph
        final Set<GraphNode> rootNodes = analysisResult.nodes.values().stream().filter(t -> t.incomingEdges().filter(x -> x.edgeType().flowdirection == Flowdirection.forward).count() == 0).collect(Collectors.toSet());
        for (final GraphNode rootNode : rootNodes) {
            findBackEdgesFor(rootNode, new Stack<>());
        }

        // Step 3: we compute the scope flow
        final Queue<GraphNode> workingQueue = new LinkedList<>(rootNodes);
        while (!workingQueue.isEmpty()) {
            final GraphNode currentEntry = workingQueue.poll();
            if (analysisResult.scopes.containsKey(currentEntry.value)) {
                continue;
            }
            final Set<GraphNode> theOutgoing = currentEntry.outgoingEdges().map(Edge::targetNode).filter(t -> !analysisResult.scopes.containsKey(t.value)).collect(Collectors.toSet());
            if (currentEntry.value instanceof NullValue) {
                // The null constant. It is always local scope
                analysisResult.scopes.put(currentEntry.value, new LocalScope());
                addNotExisting(workingQueue, theOutgoing);
            } else if (currentEntry.value instanceof NewArrayExpression) {
                // We are creating a new array here, there are no incoming references
                analysisResult.scopes.put(currentEntry.value, new LocalScope());
                addNotExisting(workingQueue, theOutgoing);
            } else if (currentEntry.value instanceof NewMultiArrayExpression) {
                // We are creating a new multidimensional array here, there are no incoming references
                analysisResult.scopes.put(currentEntry.value, new LocalScope());
                addNotExisting(workingQueue, theOutgoing);
            } else if (currentEntry.value instanceof NewInstanceExpression) {
                // We are creating a new instance of a given type, there are no incoming references
                analysisResult.scopes.put(currentEntry.value, new LocalScope());
                addNotExisting(workingQueue, theOutgoing);
            } else if (currentEntry.value instanceof GetStaticExpression) {
                // We are reading a field from a static class, there are no in imcoming references
                analysisResult.scopes.put(currentEntry.value, staticScope);
                addNotExisting(workingQueue, theOutgoing);
            } else if (currentEntry.value instanceof CurrentExceptionExpression) {
                // We are accessing the caught exception in an exception handler, there are no incoming references
                analysisResult.scopes.put(currentEntry.value, staticScope);
                addNotExisting(workingQueue, theOutgoing);
            } else if (currentEntry.value instanceof TypeOfExpression) {
                // We are accessing the runtime class of a reference
                // there is an incoming reference, but we don't care about its scope
                // as the result of this operation is always static scope
                analysisResult.scopes.put(currentEntry.value, staticScope);
                addNotExisting(workingQueue, theOutgoing);
            } else if (currentEntry.value instanceof ResolveCallsiteInstanceExpression) {
                // We are resolving the callsite for a lambda
                // Callsites are cached in static scope once created, so the result is always static
                analysisResult.scopes.put(currentEntry.value, staticScope);
                addNotExisting(workingQueue, theOutgoing);
            } else if (currentEntry.value instanceof EnumConstantsExpression) {
                // We are getting the enum constants from a runtime class
                // there are no incoming references
                analysisResult.scopes.put(currentEntry.value, staticScope);
                addNotExisting(workingQueue, theOutgoing);
            } else if (currentEntry.value instanceof StringValue) {
                // We are accessing a String constant from constant pool
                analysisResult.scopes.put(currentEntry.value, staticScope);
                addNotExisting(workingQueue, theOutgoing);
            } else if (currentEntry.value instanceof ClassReferenceValue) {
                // We are accessing a runtime class
                analysisResult.scopes.put(currentEntry.value, staticScope);
                addNotExisting(workingQueue, theOutgoing);
            } else if (currentEntry.value instanceof SelfReferenceParameterValue) {
                // We are accessing the this reference within a non-static method
                analysisResult.scopes.put(currentEntry.value, new ThisScope());
                addNotExisting(workingQueue, theOutgoing);
            } else if (currentEntry.value instanceof MethodParameterValue) {
                // We are accessing a method parameter
                analysisResult.scopes.put(currentEntry.value, new MethodParameterScope(((MethodParameterValue) currentEntry.value)));
                addNotExisting(workingQueue, theOutgoing);
            } else if (currentEntry.value instanceof ReturnValueExpression) {
                // We are returning something
                // There is always one incoming reference
                if (isComplete(analysisResult, currentEntry, 1)) {
                    final List<Value> incomingValues = currentEntry.value.incomingDataFlows();
                    final Scope theIncomingScope = analysisResult.scopes.get(incomingValues.get(0));
                    theIncomingScope.flowsInto(returnScope);
                    analysisResult.scopes.put(currentEntry.value, returnScope);
                } else {
                    // Incoming value is not resolved yet, we put it back onto the workingqueue
                    workingQueue.add(currentEntry);
                }
                // Terminal instruction
            } else if (currentEntry.value instanceof ThrowExpression) {
                // We are throwing something
                // There is always one incoming reference
                if (isComplete(analysisResult, currentEntry, 1)) {
                    final List<Value> incomingValues = currentEntry.value.incomingDataFlows();
                    final Scope theIncomingScope = analysisResult.scopes.get(incomingValues.get(0));
                    theIncomingScope.flowsInto(returnScope);
                    analysisResult.scopes.put(currentEntry.value, returnScope);
                } else {
                    // Incoming value is not resolved yet, we put it back onto the workingqueue
                    workingQueue.add(currentEntry);
                }
                // Terminal instruction
            } else if (currentEntry.value instanceof NewInstanceFromDefaultConstructorExpression) {
                // We are creating an instance from a type only known at runtime
                // There is always one incoming reference
                if (isComplete(analysisResult, currentEntry, 1)) {
                    final InvocationResultScope scope = new InvocationResultScope();
                    analysisResult.scopes.put(currentEntry.value, scope);
                    addNotExisting(workingQueue, theOutgoing);
                } else {
                    // Incoming value is not resolved yet, we put it back onto the workingqueue
                    workingQueue.add(currentEntry);
                }
            } else if (currentEntry.value instanceof PutStaticExpression) {
                // We are writing something into a static field
                // There is always one incoming reference
                if (isComplete(analysisResult, currentEntry, 1)) {
                    final List<Value> incomingValues = currentEntry.value.incomingDataFlows();
                    final Scope theIncomingScope = analysisResult.scopes.get(incomingValues.get(0));
                    theIncomingScope.flowsInto(staticScope);
                    analysisResult.scopes.put(currentEntry.value, staticScope);
                } else {
                    // Incoming value is not resolved yet, we put it back onto the workingqueue
                    workingQueue.add(currentEntry);
                }
                // Terminal instruction
            } else if (currentEntry.value instanceof SetEnumConstantsExpression) {
                // We are setting the enum constants within a runtime class
                // There is always one incoming reference
                if (isComplete(analysisResult, currentEntry, 2)) {
                    final List<Value> incomingValues = currentEntry.value.incomingDataFlows();
                    final Value theValue = incomingValues.get(1);
                    analysisResult.scopes.get(theValue).flowsInto(staticScope);
                } else {
                    // Not all incoming values are resolved yet, we put it back onto the workingqueue
                    workingQueue.add(currentEntry);
                }
                // Terminal instruction
            } else if (currentEntry.value instanceof NewInstanceAndConstructExpression) {
                // We are creating a new instance here
                // Check if all arguments are set
                final NewInstanceAndConstructExpression exp = (NewInstanceAndConstructExpression) currentEntry.value;
                final long requiredArgs = Arrays.stream(exp.getSignature().getArguments()).filter(t -> t.isArray() || !t.isPrimitive()).count();
                if (isComplete(analysisResult, currentEntry, requiredArgs)) {

                    final InvocationResultScope invocationScope = new InvocationResultScope();
                    final AnalysisResult analysis = analyze(programDescriptorProvider.resolveConstructorInvocation(exp.getClazz(), exp.getSignature()));
                    final List<Value> constructorArguments = currentEntry.value.incomingDataFlows();

                    // Now, for every argument, we check if it escapes to other scopes
                    for (int i = 0; i < analysis.program.getArguments().size(); i++) {
                        final Variable programArgument = analysis.program.argumentAt(i);
                        final TypeRef argumentType = programArgument.resolveType();
                        if (argumentType.isArray() || argumentType.isObject()) {
                            final Scope incomingScope;
                            if (i == 0) {
                                incomingScope = invocationScope;
                            } else {
                                incomingScope = analysisResult.scopes.get(constructorArguments.get(i - 1));
                            }
                            for (final Scope s : analysis.argumentsFlowsFor(programArgument)) {
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

                    analysisResult.scopes.put(currentEntry.value, invocationScope);

                    // Continue with all following nodes
                    addNotExisting(workingQueue, theOutgoing);

                } else {
                    // Not all incoming values are resolved yet, we put it back into the workingqueue
                    workingQueue.add(currentEntry);
                }
            } else if (currentEntry.value instanceof InvokeStaticMethodExpression) {
                // We are invoking a static method ere
                // Check if all arguments are set
                final InvokeStaticMethodExpression exp = (InvokeStaticMethodExpression) currentEntry.value;
                final long requiredArgs = Arrays.stream(exp.getSignature().getArguments()).filter(t -> t.isArray() || !t.isPrimitive()).count();
                if (isComplete(analysisResult, currentEntry, requiredArgs)) {
                    final InvocationResultScope invocationScope = new InvocationResultScope();
                    final ProgramDescriptor pd = programDescriptorProvider.resolveStaticInvocation(exp.getClassName(), exp.getMethodName(), exp.getSignature());
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
                        analysisResult.scopes.put(currentEntry.value, invocationScope);
                    }

                    addNotExisting(workingQueue, theOutgoing);

                } else {
                    // Not all incoming values are resolved yet, we put it back into the workingqueue
                    workingQueue.add(currentEntry);
                }
            } else if (currentEntry.value instanceof InvokeDirectMethodExpression) {
                // We are invoking a instance method ere
                // Check if all arguments are set
                final InvokeDirectMethodExpression exp = (InvokeDirectMethodExpression) currentEntry.value;
                final long requiredArgs = Arrays.stream(exp.getSignature().getArguments()).filter(t -> t.isArray() || !t.isPrimitive()).count() + 1;
                if (isComplete(analysisResult, currentEntry, requiredArgs)) {

                    final InvocationResultScope invocationScope = new InvocationResultScope();

                    // We analyze the method invocation
                    final ProgramDescriptor pd = programDescriptorProvider.resolveDirectInvocation(exp.getClazz(), exp.getMethodName(), exp.getSignature());
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
                    }

                    addNotExisting(workingQueue, theOutgoing);

                } else {
                    // Not all incoming values are resolved yet, we put it back into the workingqueue
                    workingQueue.add(currentEntry);

                }

            } else if (currentEntry.value instanceof InvokeVirtualMethodExpression) {
                // We are invoking a virtual method here
                final InvokeVirtualMethodExpression exp = (InvokeVirtualMethodExpression) currentEntry.value;
                final long requiredArgs = Arrays.stream(exp.getSignature().getArguments()).filter(t -> t.isArray() || !t.isPrimitive()).count() + 1;
                if (isComplete(analysisResult, currentEntry, requiredArgs)) {
                    final InvocationResultScope invocationScope = new InvocationResultScope();

                    // Virtual invocations are tricky, as we might also call a lambda with
                    // a delegating static implementation. This is currently impossible to figure out
                    // so we go the way to think all arguments escape to static scope here

                    // TODO: Check which classes are used in lambdas here to make a better guess
                    for (final Value v : exp.incomingDataFlows()) {
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
                    // Not all incoming values are resolved yet, we put it back into the workingqueue
                    workingQueue.add(currentEntry);
                }
            } else if (currentEntry.value instanceof LambdaWithStaticImplExpression) {
                if (isComplete(analysisResult, currentEntry, 4)) {
                    // Lambda factories are tricky.
                    // Arguments to not escape on construction, but maybe on invocation
                    // of the lambda. As we do not know, we assume everything escapes to
                    // static scope
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

                } else {
                    // Not all incoming values are resolved yet, we put it back into the workingqueue
                    workingQueue.add(currentEntry);
                }
            } else if (currentEntry.value instanceof LambdaInterfaceReferenceExpression) {
                if (isComplete(analysisResult, currentEntry, 3)) {
                    // Lambda factories are tricky.
                    // Arguments to not escape on construction, but maybe on invocation
                    // of the lambda. As we do not know, we assume everything escapes to
                    // static scope
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

                } else {
                    // Not all incoming values are resolved yet, we put it back into the workingqueue
                    workingQueue.add(currentEntry);
                }
            } else if (currentEntry.value instanceof LambdaVirtualReferenceExpression) {
                if (isComplete(analysisResult, currentEntry, 3)) {
                    // Lambda factories are tricky.
                    // Arguments to not escape on construction, but maybe on invocation
                    // of the lambda. As we do not know, we assume everything escapes to
                    // static scope
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

                } else {
                    // Not all incoming values are resolved yet, we put it back into the workingqueue
                    workingQueue.add(currentEntry);
                }
            } else if (currentEntry.value instanceof LambdaConstructorReferenceExpression) {
                if (isComplete(analysisResult, currentEntry, 3)) {
                    // Lambda factories are tricky.
                    // Arguments to not escape on construction, but maybe on invocation
                    // of the lambda. As we do not know, we assume everything escapes to
                    // static scope
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

                } else {
                    // Not all incoming values are resolved yet, we put it back into the workingqueue
                    workingQueue.add(currentEntry);
                }
            } else if (currentEntry.value instanceof LambdaSpecialReferenceExpression) {
                if (isComplete(analysisResult, currentEntry, 3)) {
                    // Lambda factories are tricky.
                    // Arguments to not escape on construction, but maybe on invocation
                    // of the lambda. As we do not know, we assume everything escapes to
                    // static scope
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

                } else {
                    // Not all incoming values are resolved yet, we put it back into the workingqueue
                    workingQueue.add(currentEntry);
                }
            } else if (currentEntry.value instanceof ArrayStoreExpression) {
                if (isComplete(analysisResult, currentEntry, 1)) {
                    final Set<GraphNode> theIncoming = currentEntry.incomingEdges().map(t -> (GraphNode) t.sourceNode()).collect(Collectors.toSet());
                    final Scope scope = analysisResult.scopes.get(theIncoming.iterator().next().value);
                    analysisResult.scopes.put(currentEntry.value, scope);

                    addNotExisting(workingQueue, theOutgoing);
                } else {
                    // Not all incoming values are resolved yet, we put it back into the workingqueue
                    workingQueue.add(currentEntry);
                }
            } else if (currentEntry.value instanceof PutFieldExpression) {
                if (isComplete(analysisResult, currentEntry, 1)) {
                    final Set<GraphNode> theIncoming = currentEntry.incomingEdges().map(t -> (GraphNode) t.sourceNode()).collect(Collectors.toSet());
                    final Scope scope = analysisResult.scopes.get(theIncoming.iterator().next().value);
                    analysisResult.scopes.put(currentEntry.value, scope);

                    addNotExisting(workingQueue, theOutgoing);
                } else {
                    // Not all incoming values are resolved yet, we put it back into the workingqueue
                    workingQueue.add(currentEntry);
                }
            } else if (currentEntry.value instanceof GetFieldExpression) {
                // We are accessing a field of something
                // There is always one incoming reference
                if (isComplete(analysisResult, currentEntry, 1)) {
                    final List<Value> incomingValues = currentEntry.value.incomingDataFlows();
                    final Scope theIncomingScope = analysisResult.scopes.get(incomingValues.get(0));
                    analysisResult.scopes.put(currentEntry.value, theIncomingScope);

                    addNotExisting(workingQueue, theOutgoing);

                } else {
                    // Incoming value is not resolved yet, we put it back onto the workingqueue
                    workingQueue.add(currentEntry);
                }
            } else if (currentEntry.value instanceof ArrayEntryExpression) {
                // We are reading an array element of something
                // There is always one incoming reference
                if (isComplete(analysisResult, currentEntry, 1)) {
                    final List<Value> incomingValues = currentEntry.value.incomingDataFlows();
                    final Scope theIncomingScope = analysisResult.scopes.get(incomingValues.get(0));
                    analysisResult.scopes.put(currentEntry.value, theIncomingScope);

                    addNotExisting(workingQueue, theOutgoing);

                } else {
                    // Incoming value is not resolved yet, we put it back onto the workingqueue
                    workingQueue.add(currentEntry);
                }
            } else if (currentEntry.value instanceof Variable) {
                final long theIncomingCount = currentEntry.incomingEdges().filter(edge -> edge.edgeType().flowdirection == Flowdirection.forward).map(t -> (GraphNode) t.sourceNode()).count();
                if (isComplete(analysisResult, currentEntry, theIncomingCount)) {
                    // First of all, we need the scope for the current variable
                    // This os derived from the incoming value which is not a PutPield or ArrayStore expression
                    final Set<GraphNode> theInitializer = currentEntry.incomingEdges()
                            .filter(edge -> edge.edgeType().flowdirection == Flowdirection.forward)
                            .map(t -> (GraphNode) t.sourceNode())
                            .filter(t -> !(t.value instanceof ArrayStoreExpression) && !(t.value instanceof PutFieldExpression))
                            .collect(Collectors.toSet());
                    if (theInitializer.size() != 1) {
                        throw new IllegalArgumentException("Expected a single initializer for " + currentEntry.value + ", got " + theInitializer.size());
                    }
                    final Scope scope = analysisResult.scopes.get(theInitializer.iterator().next().value);
                    analysisResult.scopes.put(currentEntry.value, scope);

                    // Now we check for incoming ArrayStore or PutField Expressions
                    currentEntry.incomingEdges()
                            .filter(edge -> edge.edgeType().flowdirection == Flowdirection.forward)
                            .map(t -> ((GraphNode) t.sourceNode()).value)
                            .filter(t -> (t instanceof PutFieldExpression) || (t instanceof ArrayStoreExpression))
                            .forEach(t -> {
                                final Scope incoming = analysisResult.scopes.get(t);
                                incoming.flowsInto(scope);
                            });

                    // And we are done
                    addNotExisting(workingQueue, theOutgoing);
                } else {
                    // Not all incoming values are resolved yet, we put it back into the workingqueue
                    workingQueue.add(currentEntry);
                }
            } else if (currentEntry.value instanceof PHIValue) {
                // All incoming forward edges must be resolved
                final long theIncomingCount = currentEntry.incomingEdges().filter(edge -> edge.edgeType().flowdirection == Flowdirection.forward).map(t -> (GraphNode) t.sourceNode()).count();
                if (isComplete(analysisResult, currentEntry, theIncomingCount)) {

                    final Set<Scope> mergingScopes = currentEntry.incomingEdges()
                            .filter(edge -> edge.edgeType().flowdirection == Flowdirection.forward)
                            .map(t -> ((GraphNode) t.sourceNode()).value)
                            .map(analysisResult.scopes::get).collect(Collectors.toSet());

                    final PHIScope scope = new PHIScope(mergingScopes);
                    for (final Scope s : mergingScopes) {
                        s.flowsInto(scope);
                    }

                    analysisResult.scopes.put(currentEntry.value, scope);

                    addNotExisting(workingQueue, theOutgoing);

                } else {
                    // Not all incoming values are resolved yet, we put it back into the workingqueue
                    workingQueue.add(currentEntry);
                }
            } else {
                throw new IllegalArgumentException("Not supported : " + currentEntry.value);
            }
        }

        // Step 4: Compute escaping allocations of flows for the arguments
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

        // Step 5 : Compute escapes for allocations
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

        // Step 6 : Compute returning flows
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

    private void findBackEdgesFor(final GraphNode aCurrentNode, final Stack<Object> aTraversalStack) {
        aTraversalStack.push(aCurrentNode);
        for (final Edge<PointsTo, GraphNode> outgoing : aCurrentNode.outgoingEdges().collect(Collectors.toSet())) {
            if (outgoing.edgeType().flowdirection == Flowdirection.forward) {
                if (aTraversalStack.contains(outgoing.targetNode())) {
                    // Back edge found
                    outgoing.edgeType().flowdirection = Flowdirection.backward;
                } else {
                    findBackEdgesFor(outgoing.targetNode(), aTraversalStack);
                }
            }
        }
        aTraversalStack.pop();
    }

    private boolean isComplete(final AnalysisResult aResult, final GraphNode aNode, final long aExpectedIncomingValues) {
        final List<GraphNode> theIncoming = aNode.incomingEdges().filter(t -> t.edgeType().flowdirection == Flowdirection.forward).map(t -> (GraphNode) t.sourceNode()).collect(Collectors.toList());
        if (theIncoming.size() != aExpectedIncomingValues) {
            throw new IllegalArgumentException(aNode.value.getClass().getSimpleName() + " with unexpected number of incoming edges : " + theIncoming.size()+ ", expected " + aExpectedIncomingValues);
        }
        final long theIncomingWithScope = theIncoming.stream().filter(t -> aResult.scopes.get(t.value) != null).count();
        return theIncomingWithScope == aExpectedIncomingValues;
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

                valueNode.addEdgeTo(new PointsTo(), varNode);

                analyze(valueNode.value, analysisResult);
            }

        } else if (aExpression instanceof NewInstanceAndConstructExpression) {

            final NewInstanceAndConstructExpression n = (NewInstanceAndConstructExpression) aExpression;
            final GraphNode newNode = analysisResult.nodeFor(n);

            for (final Value v : n.incomingDataFlows()) {
                final TypeRef type = v.resolveType();
                if (type.isObject() || type.isArray()) {
                    final GraphNode arg = analysisResult.nodeFor(v);
                    arg.addEdgeTo(new PointsTo(), newNode);
                }
            }

        } else if (aExpression instanceof NewInstanceFromDefaultConstructorExpression) {

            final NewInstanceFromDefaultConstructorExpression n = (NewInstanceFromDefaultConstructorExpression) aExpression;
            final GraphNode newNode = analysisResult.nodeFor(n);

            for (final Value v : n.incomingDataFlows()) {
                final TypeRef type = v.resolveType();
                if (type.isObject() || type.isArray()) {
                    final GraphNode arg = analysisResult.nodeFor(v);
                    arg.addEdgeTo(new PointsTo(), newNode);
                }
            }

        } else if (aExpression instanceof InvocationExpression) {

            final InvocationExpression n = (InvocationExpression) aExpression;
            final GraphNode newNode = analysisResult.nodeFor(n);

            for (final Value v : n.incomingDataFlows()) {
                final TypeRef type = v.resolveType();
                if (type.isObject() || type.isArray()) {
                    final GraphNode arg = analysisResult.nodeFor(v);
                    arg.addEdgeTo(new PointsTo(), newNode);
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
                    arg.addEdgeTo(new PointsTo(), newNode);
                }
            }

        } else if (aExpression instanceof ThrowExpression) {

            final ThrowExpression n = (ThrowExpression) aExpression;
            final GraphNode newNode = analysisResult.nodeFor(n);

            for (final Value v : n.incomingDataFlows()) {
                final TypeRef type = v.resolveType();
                if (type.isObject() || type.isArray()) {
                    final GraphNode arg = analysisResult.nodeFor(v);
                    arg.addEdgeTo(new PointsTo(), newNode);
                }
            }

        } else if (aExpression instanceof TypeOfExpression) {

            final TypeOfExpression n = (TypeOfExpression) aExpression;
            final GraphNode newNode = analysisResult.nodeFor(n);

            for (final Value v : n.incomingDataFlows()) {
                final TypeRef type = v.resolveType();
                if (type.isObject() || type.isArray()) {
                    final GraphNode arg = analysisResult.nodeFor(v);
                    arg.addEdgeTo(new PointsTo(), newNode);
                }
            }

        } else if (aExpression instanceof PHIValue) {

            final PHIValue phi = (PHIValue) aExpression;
            final GraphNode phiNode = analysisResult.nodeFor(phi);

            for (final Value v : phi.incomingDataFlows()) {
                final TypeRef type = v.resolveType();
                if (type.isObject() || type.isArray()) {
                    final GraphNode arg = analysisResult.nodeFor(v);

                    if (arg.outgoingEdges().map(Edge::targetNode).noneMatch(x -> x == phiNode)) {
                        arg.addEdgeTo(new PointsTo(), phiNode);
                    }
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

                argNode.addEdgeTo(new PointsTo(), rNode);
            }

        } else if (aExpression instanceof PutFieldExpression) {

            final PutFieldExpression p = (PutFieldExpression) aExpression;
            final TypeRef theType = TypeRef.toType(p.getField().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().fieldType());
            if (theType.isArray() || theType.isObject()) {
                final List<Value> theIncoming = p.incomingDataFlows();
                final GraphNode instanceNode = analysisResult.nodeFor(theIncoming.get(0));
                final GraphNode valueNode = analysisResult.nodeFor(theIncoming.get(1));

                final GraphNode thePut = analysisResult.nodeFor(p);
                valueNode.addEdgeTo(new PointsTo(), thePut);
                thePut.addEdgeTo(new PointsTo(), instanceNode);

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

                theValueNode.addEdgeTo(new PointsTo(), putNode);
            }

        } else if (aExpression instanceof SetEnumConstantsExpression) {

            final SetEnumConstantsExpression p = (SetEnumConstantsExpression) aExpression;
            final GraphNode putNode = analysisResult.nodeFor(p);

            for (final Value v : p.incomingDataFlows()) {
                final GraphNode theValueNode = analysisResult.nodeFor(v);
                theValueNode.addEdgeTo(new PointsTo(), putNode);
            }

        } else if (aExpression instanceof GetFieldExpression) {

            final GetFieldExpression p = (GetFieldExpression) aExpression;
            final TypeRef theType = TypeRef.toType(p.getField().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().fieldType());
            if (theType.isArray() || theType.isObject()) {
                final List<Value> theIncoming = p.incomingDataFlows();

                final GraphNode instanceNode = analysisResult.nodeFor(p);
                final GraphNode valueNode = analysisResult.nodeFor(theIncoming.get(0));

                valueNode.addEdgeTo(new PointsTo(), instanceNode);
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

                rNode.addEdgeTo(new PointsTo(), arrayNode);
                valueNode.addEdgeTo(new PointsTo(), rNode);
            }
        } else if (aExpression instanceof ArrayEntryExpression) {

            final ArrayEntryExpression x = (ArrayEntryExpression) aExpression;
            final List<Value> theIncoming = x.incomingDataFlows();

            final TypeRef theType = x.resolveType();
            if (theType.isArray() || theType.isObject()) {

                final GraphNode rNode = analysisResult.nodeFor(x);
                final GraphNode arrayNode = analysisResult.nodeFor(theIncoming.get(0));

                arrayNode.addEdgeTo(new PointsTo(), rNode);
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