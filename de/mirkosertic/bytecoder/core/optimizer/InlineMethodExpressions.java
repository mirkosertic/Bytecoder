/*
 * Copyright 2024 Mirko Sertic
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
package de.mirkosertic.bytecoder.core.optimizer;

import de.mirkosertic.bytecoder.core.backend.BackendType;
import de.mirkosertic.bytecoder.core.ir.ControlTokenConsumer;
import de.mirkosertic.bytecoder.core.ir.Copy;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.InvocationType;
import de.mirkosertic.bytecoder.core.ir.MethodInvocation;
import de.mirkosertic.bytecoder.core.ir.MethodInvocationExpression;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.NodeType;
import de.mirkosertic.bytecoder.core.ir.PHI;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.ir.StandardProjections;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

public class InlineMethodExpressions implements Optimizer {

    private final int maxInlineSourceSize;

    private final int maxInlineTargetSize;

    public InlineMethodExpressions() {
        this.maxInlineSourceSize = Utils.maxInlineSourceSize();
        this.maxInlineTargetSize = Utils.maxInlineTargetSize();
    }

    private boolean isInliningCandidate(final ResolvedMethod rm) {
        if (Modifier.isNative(rm.methodNode.access)) {
            return false;
        }

        if (!rm.owner.type.getClassName().startsWith("org.luaj.vm2.compiler.Func")) {
            return false;
        }
        if (!rm.methodNode.name.equals("storevar")) {
            return false;
        }

        final long controlTokens = Utils.methodSize(rm);

        // Important point here: only if the method returns a value at a point, we can inline them. If it just throws an exception, we can't inline it because it would cause an invalid control flow
        if (controlTokens < maxInlineSourceSize && rm.methodBody.nodes().stream().anyMatch(t -> t.nodeType == NodeType.ReturnValue)) {
            // Check if recursive
            for (final MethodInvocation methodInvocation : rm.methodBody.nodes().stream().filter(t -> t.nodeType == NodeType.MethodInvocation).map(t -> (MethodInvocation) t).filter(t -> t.invocationType == InvocationType.DIRECT || t.invocationType == InvocationType.STATIC).collect(Collectors.toList())) {
                if (methodInvocation.method == rm) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public List<ControlTokenConsumer> finalControlFlowsFor(final Node source) {
        final ArrayList<ControlTokenConsumer> result = new ArrayList<>();
        final Stack<Node> workingQueue = new Stack<>();
        workingQueue.add(source);
        final Set<Node> visited = new HashSet<>();
        visited.add(source);
        while (!workingQueue.isEmpty()) {
            final Node n = workingQueue.pop();
            if (n instanceof ControlTokenConsumer) {
                result.add((ControlTokenConsumer) n);
            } else {
                for (final Node flowsTo : n.outgoingDataFlows()) {
                    if (visited.add(flowsTo)) {
                        workingQueue.add(flowsTo);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public boolean optimize(final BackendType backendType, final CompileUnit compileUnit, final ResolvedMethod method) {

        if (backendType == BackendType.OpenCL) {
            return false;
        }

        if (Utils.methodSize(method) > maxInlineTargetSize) {
            return false;
        }

        boolean changed = false;

        final Graph g = method.methodBody;

        // Variable and Constant propagation
        final Stack<MethodInvocationExpression> workingQueue = new Stack<>();

        // We search for static and direct method invocations
        g.nodes().stream().filter(t -> t.nodeType == NodeType.MethodInvocationExpression).map(t -> (MethodInvocationExpression) t).filter(t -> t.invocationType == InvocationType.DIRECT || t.invocationType == InvocationType.STATIC).forEach(workingQueue::push);

        // We perform a recursive search across the invocation graph
        while (!workingQueue.isEmpty()) {
            final MethodInvocationExpression methodInvocation = workingQueue.pop();
            final ResolvedMethod rm = methodInvocation.method;
            // Check for recursion here!
            if (isInliningCandidate(rm) && rm != method) {

                final Node thisRef;
                final Node[] arguments;
                final Node[] incomingDataFlows = methodInvocation.incomingDataFlows;

                if (Modifier.isStatic(rm.methodNode.access)) {
                    thisRef = null;
                } else {
                    thisRef = incomingDataFlows[0];
                }
                if (incomingDataFlows.length > 1) {
                    arguments = new Node[incomingDataFlows.length - 1];
                    System.arraycopy(incomingDataFlows, 1, arguments, 0, incomingDataFlows.length - 1);
                } else {
                    arguments = new Node[0];
                }

                // thisRef and arguments must be variable, phi or constant to do a valid transformation
                boolean valid = true;
                if (thisRef != null) {
                    if (!Utils.isVariableOrConstant(thisRef) && !thisRef.hasSideSideEffectRecursive()) {
                        valid = false;
                    }
                }
                for (final Node argument : arguments) {
                    if (!Utils.isVariableOrConstant(argument) && !argument.hasSideSideEffectRecursive()) {
                        valid = false;
                    }
                }

                if (valid) {
                    final Node[] invocationDataFlowTargets = methodInvocation.outgoingDataFlows();
                    final List<ControlTokenConsumer> finalFlowsFor = finalControlFlowsFor(methodInvocation);
                    if (finalFlowsFor.size() == 1 && finalFlowsFor.get(0).nodeType == NodeType.Copy && invocationDataFlowTargets.length == 1) {
                        final Copy rootCopy = (Copy) finalFlowsFor.get(0);
                        if (rootCopy.controlComingFrom.size() == 1) {
                            final Map<Node, Node> importedNodes = g.stampFrom(rm.methodBody, thisRef, arguments);
                            final ControlTokenConsumer start = (ControlTokenConsumer) importedNodes.get(rm.methodBody.regionByLabel(Graph.START_REGION_NAME));

                            final ControlTokenConsumer pred = rootCopy.controlComingFrom.iterator().next();
                            pred.replaceInControlFlow(rootCopy, start);

                            rootCopy.controlComingFrom.clear();
                            final PHI methodResult = g.newPHI(rm.methodType.getReturnType());

                            for (final Map.Entry<Node, Node> entry : importedNodes.entrySet()) {
                                if (entry.getKey().nodeType == NodeType.ReturnValue) {
                                    final Copy replacement = (Copy) entry.getValue();
                                    replacement.addControlFlowTo(StandardProjections.DEFAULT, rootCopy);
                                    methodResult.addIncomingData(replacement);
                                }
                            }

                            final Node resultConsumer = invocationDataFlowTargets[0];
                            resultConsumer.remapDataFlow(methodInvocation, methodResult);
                            g.deleteNode(methodInvocation);

                            changed = true;
                            method.inlined = true;
                        }
                    }
                }
            }
        }

        return changed;
    }
}
