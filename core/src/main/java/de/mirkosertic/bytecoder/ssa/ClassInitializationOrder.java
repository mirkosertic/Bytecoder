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
package de.mirkosertic.bytecoder.ssa;

import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.graph.EdgeType;
import de.mirkosertic.bytecoder.graph.GraphDFSOrder;
import de.mirkosertic.bytecoder.graph.Node;

import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClassInitializationOrder {

    private enum EdgeTypes implements EdgeType {
        usedBy
    }

    private static class ClassNode extends Node<ClassNode,EdgeTypes> {

        private final BytecodeLinkedClass clazz;
        private final Map<BytecodeMethod, Program> methods;

        public ClassNode(final BytecodeLinkedClass clazz) {
            this.clazz = clazz;
            this.methods = new HashMap<>();
        }

        @Override
        public String toString() {
            return clazz.getClassName().name();
        }
    }

    private final Map<BytecodeLinkedClass, ClassNode> nodes;

    public ClassInitializationOrder() {
        nodes = new HashMap<>();
    }

    private ClassNode nodeFor(final BytecodeLinkedClass aLinkedClass) {
        return nodes.computeIfAbsent(aLinkedClass, ClassNode::new);
    }

    public void usedBy(final BytecodeLinkedClass aUserClass, final BytecodeLinkedClass aUsedClass) {
        final ClassNode userClassNode = nodeFor(aUserClass);
        final ClassNode usedClassNode = nodeFor(aUsedClass);
        if (userClassNode != usedClassNode) {
            if (usedClassNode.incomingEdges().noneMatch(t -> t.targetNode() == userClassNode)) {
                //System.out.println(" -- edge from " + usedClassNode.clazz.getClassName().name() + " to " + userClassNode.clazz.getClassName().name());
                usedClassNode.addEdgeTo(EdgeTypes.usedBy, userClassNode);
            }
        }
    }

    public void registerCodeForDependencyAnalysis(final BytecodeLinkedClass aClass, final BytecodeMethod aMethod, final Program aSSAProgram) {
        final ClassNode node = nodeFor(aClass);
        node.methods.put(aMethod, aSSAProgram);
    }

    private void finalizeAnalysis() {
        for (final Map.Entry<BytecodeLinkedClass, ClassNode> entry : nodes.entrySet()) {
            analyzeMethod(entry.getValue(), "<clinit>", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[0]));
        }
    }

    private void analyzeMethod(final ClassNode aNode, final String aMethodName, final BytecodeMethodSignature aSignature) {
        for (final Map.Entry<BytecodeMethod, Program> entry : aNode.methods.entrySet()) {
            final BytecodeMethod m = entry.getKey();
            if (aMethodName.equals(m.getName().stringValue()) && aSignature.matchesExactlyTo(m.getSignature())) {
                analyzeDependencies(aNode.clazz, entry.getValue());
            }
        }
    }

    private void analyzeDependencies(final BytecodeLinkedClass aOwner, final Program aProgram) {
        new DependencyAnalysis(aProgram, new DependencyAnalysis.DependencyVisitor() {
            @Override
            public void staticInvocation(final BytecodeLinkedClass aClass, final String aMethodName, final BytecodeMethodSignature aSignature) {
                //System.out.println(aOwner.getClassName().name() +" depends on static invocation to " + aClass.getClassName().name());
                usedBy(aOwner, aClass);
                analyzeMethod(nodeFor(aClass), aMethodName, aSignature);
            }

            @Override
            public void staticFieldAccess(final BytecodeLinkedClass aClass, final String aFieldName, final BytecodeTypeRef aFieldType) {
                //System.out.println(aOwner.getClassName().name() +" depends on static field access to " + aClass.getClassName().name());
                usedBy(aOwner, aClass);
            }

            @Override
            public void classReference(final BytecodeLinkedClass aClass) {
                //System.out.println(aOwner.getClassName().name() +" depends on class reference to " + aClass.getClassName().name());
                usedBy(aOwner, aClass);
            }
        });
    }

    public List<BytecodeLinkedClass> computeInitializationOrder() {
        finalizeAnalysis();
        for (final ClassNode node : nodes.values()) {
            if (node.clazz.getClassName().name().equals(Object.class.getName())) {
                // We found the startnode,
                // Now we have to compute the right order

                // This is wrong
                final GraphDFSOrder<ClassNode> theOrder = new GraphDFSOrder<>(node,
                        Comparator.comparing(o -> o.clazz.getClassName().name()),
                        edge -> edge.edgeType() == EdgeTypes.usedBy);
                theOrder.printDebug(new PrintWriter(System.out));
                return theOrder.getNodesInOrder().stream().map(t -> t.clazz).collect(Collectors.toList());
            }
        }
        throw new IllegalArgumentException("Cannot find java.lang.Object root for dependency analysis!");
    }
}
