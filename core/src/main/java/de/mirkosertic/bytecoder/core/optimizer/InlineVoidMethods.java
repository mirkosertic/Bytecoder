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
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.NodeType;
import de.mirkosertic.bytecoder.core.ir.Projection;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.ir.StandardProjections;
import de.mirkosertic.bytecoder.core.ir.Value;
import de.mirkosertic.bytecoder.core.ir.Variable;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

public class InlineVoidMethods implements Optimizer {

    private int labelCounter = 0;

    private final int maxInlineSourceSize;

    private final int maxInlineTargetSize;

    public InlineVoidMethods() {
        this.maxInlineSourceSize = Utils.maxInlineSourceSize();
        this.maxInlineTargetSize = Utils.maxInlineTargetSize();
    }

    private boolean isInliningCandidate(final ResolvedMethod rm) {
        if (Modifier.isNative(rm.methodNode.access)) {
            return false;
        }

        final long controlTokens = Utils.methodSize(rm);

        // Important point here: only if the method returns a value at a point, we can inline them. If it just throws an exception, we can't inline it because it would cause an invalid control flow
        if (controlTokens < maxInlineSourceSize && rm.methodBody.nodes().stream().anyMatch(t -> t.nodeType == NodeType.Return)) {
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
        final Stack<MethodInvocation> workingQueue = new Stack<>();

        // We search for static and direct method invocations
        g.nodes().stream().filter(t -> t.nodeType == NodeType.MethodInvocation).map(t -> (MethodInvocation) t).filter(t -> t.invocationType == InvocationType.DIRECT || t.invocationType == InvocationType.STATIC).forEach(workingQueue::push);

        // We perform a recursive search across the invocation graph
        while (!workingQueue.isEmpty()) {
            final MethodInvocation methodInvocation = workingQueue.pop();
            final ResolvedMethod rm = methodInvocation.method;
            // Check for recursion here!
            if (isInliningCandidate(rm) && rm != method) {

                rm.inlined = true;

                Node thisRef;
                final Node[] arguments;
                final Node[] incomingDataFlows = methodInvocation.incomingDataFlows;

                final ControlTokenConsumer preambleStart = g.newRegion("InlinePreamble_" + labelCounter++);
                ControlTokenConsumer preambleEnd = preambleStart;

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

                // Convert all incoming data flows into variables
                if (thisRef != null) {
                    if (!Utils.isVariableOrConstant(thisRef)) {
                        // Convert this into a variable
                        final Variable newThisRef = g.newVariable(((Value) thisRef).type);
                        final Copy c = g.newCopy();
                        c.addIncomingData(thisRef);
                        newThisRef.addIncomingData(c);
                        thisRef = newThisRef;

                        preambleEnd.addControlFlowTo(StandardProjections.DEFAULT, c);
                        preambleEnd = c;
                    }
                }
                for (int i = 0; i < arguments.length; i++) {
                    final Node argument = arguments[i];
                    if (!Utils.isVariableOrConstant(argument)) {
                        // Convert this into a variable
                        final Variable newArgument = g.newVariable(((Value) argument).type);
                        final Copy c = g.newCopy();
                        c.addIncomingData(argument);
                        newArgument.addIncomingData(c);
                        arguments[i] = newArgument;

                        preambleEnd.addControlFlowTo(StandardProjections.DEFAULT, c);
                        preambleEnd = c;
                    }
                }

                final Map<Node, Node> importedNodes = g.stampFrom(rm.methodBody, thisRef, arguments);
                final ControlTokenConsumer start = (ControlTokenConsumer) importedNodes.get(rm.methodBody.regionByLabel(Graph.START_REGION_NAME));

                preambleEnd.addControlFlowTo(StandardProjections.DEFAULT, start);

                g.replaceInControlFlow(methodInvocation, preambleStart);
                for (final Map.Entry<Projection, ControlTokenConsumer> entry : methodInvocation.controlFlowsTo.entrySet()) {
                    entry.getValue().controlComingFrom.remove(methodInvocation);
                }

                // Remerge the control flow
                for (final Map.Entry<Node, Node> imp : importedNodes.entrySet()) {
                    final Node source = imp.getKey();
                    if (source.nodeType == NodeType.Return) {
                        final ControlTokenConsumer target = (ControlTokenConsumer) imp.getValue();

                        for (final Map.Entry<Projection, ControlTokenConsumer> entry : methodInvocation.controlFlowsTo.entrySet()) {
                            target.addControlFlowTo(entry.getKey(), entry.getValue());
                        }
                    }
                }

                changed = true;
            }
        }

        return changed;
    }
}
