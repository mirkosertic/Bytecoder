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

import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.graph.Edge;
import de.mirkosertic.bytecoder.graph.EdgeType;
import de.mirkosertic.bytecoder.graph.Node;
import de.mirkosertic.bytecoder.ssa.ArrayStoreExpression;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.GetFieldExpression;
import de.mirkosertic.bytecoder.ssa.MethodParameterValue;
import de.mirkosertic.bytecoder.ssa.NewArrayExpression;
import de.mirkosertic.bytecoder.ssa.NewMultiArrayExpression;
import de.mirkosertic.bytecoder.ssa.NewObjectAndConstructExpression;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.PutFieldExpression;
import de.mirkosertic.bytecoder.ssa.PutStaticExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.SelfReferenceParameterValue;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.ValueWithEscapeCheck;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

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

    private final Map<Value, GraphNode> nodes;
    private final Set<Value> escapedValues;

    public PointsToEscapeAnalysis(final BytecodeLinkedClass aClass, final BytecodeMethod aMethod, final Program aProgram) {
        nodes = new HashMap<>();
        escapedValues = new HashSet<>();

        // Step 1 : Build a reference flow graph
        for (final Variable v : aProgram.getArguments()) {
            final TypeRef theType = v.resolveType();
            if (theType.isArray() || theType.isObject()) {
                final GraphNode varNode = nodeFor(v);
                final GraphNode initValue = nodeFor(v.incomingDataFlows().get(0));
                initValue.addEdgeTo(PointsTo.to, varNode);
            }
        }

        final ControlFlowGraph g = aProgram.getControlFlowGraph();
        for (final RegionNode theNode : g.dominators().getPreOrder()) {
            analyze(theNode.getExpressions());
        }

        // Step 2: we build graph partitions
        final List<Set<GraphNode>> partitions = new ArrayList<>();
        final List<GraphNode> searchSpace = new ArrayList(nodes.values());
        while (!searchSpace.isEmpty()) {
            final GraphNode anyNode = searchSpace.get(0);
            final Set<GraphNode> partition = interconnectedNodesFor(anyNode, searchSpace);
            partitions.add(partition);
            searchSpace.removeAll(partition);
        }

        // Step 3: we check all partitions if they contain something
        // that might cause an escape. If so, we mark potential escaping objects
        for (final Set<GraphNode> partition : partitions) {

            final long returnOrStaticEscapeCount = partition.stream().map(t -> t.value).filter(t -> {
                if (t instanceof ReturnValueExpression) {
                    return true;
                }
                if (t instanceof PutStaticExpression) {
                    return true;
                }
                return false;
            }).count();

            // Brute force escape
            if (returnOrStaticEscapeCount > 0) {

                partition.stream().map(t -> t.value).forEach(t -> {
                    if (t instanceof MethodParameterValue) {
                        escapedValues.add(t);
                    }
                    if (t instanceof SelfReferenceParameterValue) {
                        escapedValues.add(t);
                    }
                    if (t instanceof NewObjectAndConstructExpression) {
                        escapedValues.add(t);
                    }
                    if (t instanceof NewArrayExpression) {
                        escapedValues.add(t);
                    }
                    if (t instanceof NewMultiArrayExpression) {
                        escapedValues.add(t);
                    }
                });

            } else{

                // TODO: If there are any instantiations
                // or PutField or ArrayStore scope changes
                // we mark the datasources as escaping

            }
        }

        // Step 4 : Propagate the escaped status
        for (final Value v : escapedValues) {
            if (v instanceof ValueWithEscapeCheck) {
                ((ValueWithEscapeCheck) v).markAsEscaped();
            }
        }

        System.out.println("digraph refflow {");
        for (final GraphNode v: nodes.values()) {
            if (v.value instanceof MethodParameterValue || v.value instanceof SelfReferenceParameterValue) {
                final String label;
                if (v.value instanceof MethodParameterValue) {
                    label = "arg" + ((MethodParameterValue) v.value).getParameterIndex();
                } else {
                    label = "this";
                }
                if (escapedValues.contains(v.value)) {
                    System.out.println(" n_" + System.identityHashCode(v) + "[color=blue fontcolor=white style=filled fillcolor=blue shape=octagon label=\"" + label + "\"];");
                } else {
                    System.out.println(" n_" + System.identityHashCode(v) + "[color=blue fontcolor=blue shape=octagon label=\"" + label + "\"];");
                }
            } else if (v.value instanceof Variable) {
                System.out.println(" n_" + System.identityHashCode(v) + "[label=\"" + ((Variable) v.value).getName() + "\"];");
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

    public Set<Value> escapedValues() {
        return escapedValues;
    }

    private GraphNode nodeFor(final Value v) {
        return nodes.computeIfAbsent(v, GraphNode::new);
    }

    private void analyze(final ExpressionList aExpressionList) {
        for (final Expression theExpression : aExpressionList.toList()) {
            if (theExpression instanceof ExpressionListContainer) {
                final ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                for (final ExpressionList theList : theContainer.getExpressionLists()) {
                    analyze(theList);
                }
            }

            analyze(theExpression);
        }
    }

    private void analyze(final Value aExpression) {
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
                final GraphNode varNode = nodeFor(v.getVariable());
                final GraphNode valueNode = nodeFor(theIncoming.get(0));

                valueNode.addEdgeTo(PointsTo.to, varNode);

                analyze(valueNode.value);
            }

        } else if (aExpression instanceof NewObjectAndConstructExpression) {

            final NewObjectAndConstructExpression n = (NewObjectAndConstructExpression) aExpression;
            final GraphNode newNode = nodeFor(n);

            for (final Value v : n.incomingDataFlows()) {
                final TypeRef type = v.resolveType();
                if (type.isObject() || type.isArray()) {
                    final GraphNode arg = nodeFor(v);
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

                final GraphNode rNode = nodeFor(r);
                final GraphNode argNode = nodeFor(theIncoming.get(0));

                argNode.addEdgeTo(PointsTo.to, rNode);
            }

        } else if (aExpression instanceof PutFieldExpression) {

            final PutFieldExpression p = (PutFieldExpression) aExpression;
            final TypeRef theType = TypeRef.toType(p.getField().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().fieldType());
            if (theType.isArray() || theType.isObject()) {
                final List<Value> theIncoming = p.incomingDataFlows();
                final GraphNode instanceNode = nodeFor(theIncoming.get(0));
                final GraphNode valueNode = nodeFor(theIncoming.get(1));

                final GraphNode thePut = nodeFor(p);
                instanceNode.addEdgeTo(PointsTo.to, thePut);
                valueNode.addEdgeTo(PointsTo.to, thePut);


            }

        } else if (aExpression instanceof PutStaticExpression) {

            final PutStaticExpression p = (PutStaticExpression) aExpression;
            final TypeRef theType = TypeRef.toType(p.getField().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().fieldType());
            if (theType.isArray() || theType.isObject()) {
                final GraphNode putNode = nodeFor(p);

                final List<Value> theIncoming = p.incomingDataFlows();
                final GraphNode theValueNode = nodeFor(theIncoming.get(0));

                theValueNode.addEdgeTo(PointsTo.to, putNode);
            }

        } else if (aExpression instanceof GetFieldExpression) {

            final GetFieldExpression p = (GetFieldExpression) aExpression;
            final TypeRef theType = TypeRef.toType(p.getField().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().fieldType());
            if (theType.isArray() || theType.isObject()) {
                final List<Value> theIncoming = p.incomingDataFlows();

                final GraphNode instanceNode = nodeFor(p);
                final GraphNode valueNode = nodeFor(theIncoming.get(0));

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

                final GraphNode rNode = nodeFor(x);
                final GraphNode arrayNode = nodeFor(theIncoming.get(0));
                final GraphNode valueNode = nodeFor(theIncoming.get(2));

                arrayNode.addEdgeTo(PointsTo.to, rNode);
                valueNode.addEdgeTo(PointsTo.to, rNode);
            }

        } else {
            System.out.println(aExpression.getClass().getName());
        }
    }

    static Set<GraphNode> interconnectedNodesFor(final GraphNode aNode, final Collection<GraphNode> aSearchSpace) {
        final Set<GraphNode> result = new HashSet<>();
        final Stack<GraphNode> workqueue = new Stack<>();
        workqueue.push(aNode);
        while (!workqueue.isEmpty()) {
            final GraphNode current = workqueue.pop();
            current.outgoingEdges().map(Edge::targetNode).filter(aSearchSpace::contains).forEach(t -> {
                if (result.add(t)) {
                    workqueue.push(t);
                }
            });
            current.incomingEdges().map(t -> (GraphNode) t.sourceNode()).filter(aSearchSpace::contains).forEach(t -> {
                if (result.add(t)) {
                    workqueue.push(t);
                }
            });
        }
        return result;
    }
}
