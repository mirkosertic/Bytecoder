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

import de.mirkosertic.bytecoder.graph.Edge;
import de.mirkosertic.bytecoder.graph.EdgeType;
import de.mirkosertic.bytecoder.graph.Node;
import de.mirkosertic.bytecoder.ssa.ArrayStoreExpression;
import de.mirkosertic.bytecoder.ssa.BlockState;
import de.mirkosertic.bytecoder.ssa.ClassReferenceValue;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.GetFieldExpression;
import de.mirkosertic.bytecoder.ssa.GetStaticExpression;
import de.mirkosertic.bytecoder.ssa.MethodParameterValue;
import de.mirkosertic.bytecoder.ssa.NewArrayExpression;
import de.mirkosertic.bytecoder.ssa.NewMultiArrayExpression;
import de.mirkosertic.bytecoder.ssa.NewObjectAndConstructExpression;
import de.mirkosertic.bytecoder.ssa.NullValue;
import de.mirkosertic.bytecoder.ssa.PHIValue;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.PutFieldExpression;
import de.mirkosertic.bytecoder.ssa.PutStaticExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.SelfReferenceParameterValue;
import de.mirkosertic.bytecoder.ssa.ThrowExpression;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.ValueWithEscapeCheck;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

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
        }
    }

    static class LocalScope extends Scope {

    }

    static class StaticScope extends Scope {

    }

    static class ReturnScope extends Scope {

    }

    static class ForeignScope extends Scope {

    }

    static class ThisScope extends Scope {
        private final SelfReferenceParameterValue selfReferenceParameterValue;

        public ThisScope(final SelfReferenceParameterValue selfReferenceParameterValue) {
            this.selfReferenceParameterValue = selfReferenceParameterValue;
        }
    }

    static class MethodParameterScope extends Scope {
        private final MethodParameterValue methodParameterValue;

        public MethodParameterScope(final MethodParameterValue methodParameterValue) {
            this.methodParameterValue = methodParameterValue;
        }
    }

    public static class AnalysisResult {
        private final Map<Value, GraphNode> nodes;
        private final Set<Value> escapedValues;
        private final Map<Variable, Set<Scope>> argumentFlows;
        private final Program program;

        AnalysisResult(final Program aProgram) {
            program = aProgram;
            nodes = new HashMap<>();
            escapedValues = new HashSet<>();
            argumentFlows = new HashMap<>();
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

        public void printDebugDotTree() {
            System.out.println("digraph refflow {");
            for (final GraphNode v: nodes.values()) {
                if (v.value instanceof MethodParameterValue || v.value instanceof SelfReferenceParameterValue) {
                    final String label;
                    if (v.value instanceof MethodParameterValue) {
                        label = "arg" + ((MethodParameterValue) v.value).getParameterIndex();
                    } else {
                        label = "this";
                    }
                    System.out.println(" n_" + System.identityHashCode(v) + "[color=blue fontcolor=blue shape=octagon label=\"" + label + "\"];");
                } else if (v.value instanceof Variable) {
                    if (program.getArguments().contains(v.value)) {
                        final String label = ((Variable) v.value).getName();
                        if (escapedValues.contains(v.value)) {
                            System.out.println(" n_" + System.identityHashCode(v) + "[color=red fontcolor=white style=filled fillcolor=red shape=octagon label=\"" + label + "\"];");
                        } else {
                            System.out.println(" n_" + System.identityHashCode(v) + "[shape=octagon label=\"" + label + "\"];");
                        }

                    } else {
                        System.out.println(" n_" + System.identityHashCode(v) + "[label=\"" + ((Variable) v.value).getName() + "\"];");
                    }
                } else {
                    if (escapedValues.contains(v.value)) {
                        System.out.println(" n_" + System.identityHashCode(v) + "[color=red shape=box fontcolor=white style=filled fillcolor=red label=\"" + v.value.getClass().getSimpleName() + "\"];");
                    } else {
                        System.out.println(" n_" + System.identityHashCode(v) + "[shape=box label=\"" + v.value.getClass().getSimpleName() + "\"];");
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


    private final ProgramDescriptorProvider programDescriptorProvider;

    public PointsToEscapeAnalysis(final ProgramDescriptorProvider programDescriptorProvider) {
        this.programDescriptorProvider = programDescriptorProvider;
    }

    public AnalysisResult analyze(final ProgramDescriptor aProgramDescriptor) {

        final AnalysisResult analysisResult = new AnalysisResult(aProgramDescriptor.program);

        final Map<Value, Scope> scopes = new HashMap<>();
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
            BlockState theLiveIn = theNode.liveIn();
            theLiveIn.getPorts().values().stream()
                    .filter(t -> t instanceof PHIValue)
                    .map(t -> (PHIValue) t)
                    .filter(t -> t.resolveType().isObject() || t.resolveType().isArray())
                    .filter(t -> alreadyKnownPHIvalues.add(t)).forEach(t -> {

                final GraphNode phiNode = analysisResult.nodeFor(t);
                for (final Value incoming : t.incomingDataFlows()) {
                    final GraphNode incomingNode = analysisResult.nodeFor(incoming);
                    incomingNode.addEdgeTo(PointsTo.to, phiNode);
                }
            });
            analyze(theNode.getExpressions(), analysisResult);
        }

        // Step 2: we compute the scope flow
        final Queue<GraphNode> workingQueue = analysisResult.nodes.values().stream().filter(t -> t.incomingEdges().count() == 0).distinct().collect(Collectors.toCollection(LinkedList::new));
        while (!workingQueue.isEmpty()) {
            final GraphNode currentEntry = workingQueue.poll();
            System.out.println(currentEntry.value);

            final Set<GraphNode> theOutgoing = currentEntry.outgoingEdges().map(Edge::targetNode).filter(t -> !scopes.containsKey(t)).collect(Collectors.toSet());
            if (currentEntry.value instanceof NewArrayExpression) {
                scopes.put(currentEntry.value, new LocalScope());
                workingQueue.addAll(theOutgoing);
            } else if (currentEntry.value instanceof NewMultiArrayExpression) {
                scopes.put(currentEntry.value, new LocalScope());
                workingQueue.addAll(theOutgoing);
            } else if (currentEntry.value instanceof GetStaticExpression) {
                scopes.put(currentEntry.value, staticScope);
                workingQueue.addAll(theOutgoing);
            } else if (currentEntry.value instanceof SelfReferenceParameterValue) {
                scopes.put(currentEntry.value, new ThisScope(((SelfReferenceParameterValue) currentEntry.value)));
                workingQueue.addAll(theOutgoing);
            } else if (currentEntry.value instanceof MethodParameterValue) {
                scopes.put(currentEntry.value, new MethodParameterScope(((MethodParameterValue) currentEntry.value)));
                workingQueue.addAll(theOutgoing);
            } else {
                final Set<GraphNode> theIncoming = currentEntry.incomingEdges().map(t -> (GraphNode) t.sourceNode()).collect(Collectors.toSet());
                final Set<GraphNode> theIncomingWithScope = theIncoming.stream().filter(t -> scopes.containsKey(t.value)).collect(Collectors.toSet());
                if (theIncoming.size() == theIncomingWithScope.size()) {
                    // We have all predecessor scopes, so we can continue and compute the scope flow
                    // for the current node

                    if (currentEntry.value instanceof ReturnValueExpression || currentEntry.value instanceof ThrowExpression) {
                        // Terminal instruction
                        scopes.put(currentEntry.value, returnScope);
                        theIncomingWithScope.stream().map(t -> scopes.get(t.value)).forEach(scope -> scope.flowsInto(returnScope));
                    } else if (currentEntry.value instanceof PutStaticExpression) {
                        // Terminal instruction
                        scopes.put(currentEntry.value, staticScope);
                        theIncomingWithScope.stream().map(t -> scopes.get(t.value)).forEach(scope -> scope.flowsInto(returnScope));
                    } else {

                        final Set<GraphNode> theArrayWrites = theIncoming.stream().filter(t -> t.value instanceof ArrayStoreExpression).collect(Collectors.toSet());
                        final Set<GraphNode> thePutFields = theIncoming.stream().filter(t -> t.value instanceof PutFieldExpression).collect(Collectors.toSet());
                        if (!theArrayWrites.isEmpty()) {
                            if (theIncomingWithScope.size() != 2) {
                                throw new IllegalArgumentException("Expected 2 incoming values for ArrayStore, got " + theIncomingWithScope.size());
                            }
                            final GraphNode theValue = theArrayWrites.iterator().next();
                            theIncoming.stream().filter(t -> t != theValue).forEach(array -> scopes.get(theValue.value).flowsInto(scopes.get(array.value)));

                        } else if (!thePutFields.isEmpty()) {

                            if (theIncomingWithScope.size() != 2) {
                                throw new IllegalArgumentException("Expected 2 incoming values for PutField, got " + theIncomingWithScope.size());
                            }
                            final GraphNode theValue = thePutFields.iterator().next();
                            theIncoming.stream().filter(t -> t != theValue).forEach(instance -> scopes.get(theValue.value).flowsInto(scopes.get(instance.value)));

                        } else if (currentEntry.value instanceof VariableAssignmentExpression) {

                            scopes.put(currentEntry.value, scopes.get(theIncomingWithScope.iterator().next().value));
                            workingQueue.addAll(theOutgoing);

                        } else if (currentEntry.value instanceof NewObjectAndConstructExpression) {

                            final Scope newScope = new ForeignScope();
                            scopes.put(currentEntry.value, newScope);
                            theIncoming.stream().map(t -> scopes.get(t.value)).forEach(t -> t.flowsInto(newScope));

                            workingQueue.addAll(theOutgoing);

                        } else if (currentEntry.value instanceof GetStaticExpression || currentEntry.value instanceof ClassReferenceValue) {

                            final Scope newScope = new StaticScope();
                            scopes.put(currentEntry.value, newScope);
                            theIncoming.stream().map(t -> scopes.get(t.value)).forEach(t -> t.flowsInto(newScope));

                            workingQueue.addAll(theOutgoing);

                        } else if (currentEntry.value instanceof NullValue) {

                            final Scope newScope = new LocalScope();
                            scopes.put(currentEntry.value, newScope);
                            theIncoming.stream().map(t -> scopes.get(t.value)).forEach(t -> t.flowsInto(newScope));

                            workingQueue.addAll(theOutgoing);

                        } else {

                            if (theIncomingWithScope.size() == 1) {
                                final Scope newScope = scopes.get(theIncomingWithScope.iterator().next().value);
                                scopes.put(currentEntry.value, newScope);
                                theIncoming.stream().map(t -> scopes.get(t.value)).forEach(t -> t.flowsInto(newScope));
                            } else if (currentEntry.value instanceof PHIValue) {
                                Scope newScope = scopes.computeIfAbsent(currentEntry.value, key -> new LocalScope());
                                scopes.put(currentEntry.value, newScope);
                                theIncoming.stream().map(t -> scopes.get(t.value)).forEach(t -> t.flowsInto(newScope));
                            } else {
                                // Should not happen due to SSA form
                                throw new IllegalArgumentException("Don't know how to handle multiple flow assignments for " + currentEntry.value);
                            }

                            workingQueue.addAll(theOutgoing);
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
                final Set<Scope> nonLocalScopes = new HashSet<>();
                final Stack<Scope> workingStack = new Stack<>();
                final Set<Scope> alreadySeen = new HashSet<>();

                final Value selfValue = v.incomingDataFlows().get(0);

                workingStack.push(scopes.get(selfValue));
                while (!workingStack.isEmpty()) {
                    final Scope current = workingStack.pop();
                    if (alreadySeen.add(current)) {
                        if (!(current instanceof LocalScope)) {
                            nonLocalScopes.add(current);
                        }
                        workingStack.addAll(current.flowsInto);
                    }
                }
                // Ignore self scope
                nonLocalScopes.remove(scopes.get(selfValue));

                if (!nonLocalScopes.isEmpty()) {
                    analysisResult.escapedValue(v);
                }

                analysisResult.setArgumentFlows(v, nonLocalScopes);
            }
        }

        // Step 4 : Compute escapes for allocations
        scopes.entrySet().stream().filter(t ->
                t.getKey() instanceof NewArrayExpression ||
                t.getKey() instanceof NewMultiArrayExpression ||
                t.getKey() instanceof NewObjectAndConstructExpression).forEach(entry -> {

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

        } else if (aExpression instanceof NewObjectAndConstructExpression) {

            final NewObjectAndConstructExpression n = (NewObjectAndConstructExpression) aExpression;
            final GraphNode newNode = analysisResult.nodeFor(n);

            for (final Value v : n.incomingDataFlows()) {
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

        } else if (aExpression instanceof PutStaticExpression) {

            final PutStaticExpression p = (PutStaticExpression) aExpression;
            final TypeRef theType = TypeRef.toType(p.getField().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().fieldType());
            if (theType.isArray() || theType.isObject()) {
                final GraphNode putNode = analysisResult.nodeFor(p);

                final List<Value> theIncoming = p.incomingDataFlows();
                final GraphNode theValueNode = analysisResult.nodeFor(theIncoming.get(0));

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
        }
    }
}
