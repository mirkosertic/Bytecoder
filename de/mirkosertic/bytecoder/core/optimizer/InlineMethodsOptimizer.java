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
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.InvocationType;
import de.mirkosertic.bytecoder.core.ir.MethodArgument;
import de.mirkosertic.bytecoder.core.ir.MethodInvocation;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.NodeType;
import de.mirkosertic.bytecoder.core.ir.Projection;
import de.mirkosertic.bytecoder.core.ir.Region;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.ir.TryCatch;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class InlineMethodsOptimizer implements Optimizer {

    private int labelCounter = 0;

    private Map<Node, Node> stampInto(final Graph source, final Graph target, final Node thisRef, final Node[] arguments) {

        final Map<Node, Node> clones = new HashMap<>();
        for (final Node n : source.nodes()) {
            final Node clone;
            switch (n.nodeType) {
                case Region:
                    final Region r = (Region) n;
                    if (Graph.START_REGION_NAME.equals(r.label)) {
                        clone = target.newRegion("InlineStartProxy_" + labelCounter++);
                    } else {
                        clone = target.newRegion(r.label + "_" + labelCounter++);
                    }
                    break;
                case TryCatch:
                    final TryCatch t = (TryCatch) n;
                    clone = target.newTryCatch(t.label + "_" + labelCounter++);
                    break;
                case Return:
                    clone = target.newRegion("InlineReturnProxy_" + labelCounter++);
                    break;
                case This:
                    clone = thisRef;
                    break;
                case MethodArgument:
                    clone = arguments[((MethodArgument) n).index];
                    break;
                default:
                    clone = n.stampInto(target);
                    break;
            }
            clones.put(n, clone);
        }
        for (final Node n : source.nodes()) {
            final Node t = clones.get(n);
            for (final Node inc : n.incomingDataFlows) {
                final Node mapped = clones.get(inc);
                t.addIncomingData(mapped);
            }

            if (n instanceof ControlTokenConsumer) {
                final ControlTokenConsumer s1 = (ControlTokenConsumer) n;
                final ControlTokenConsumer t1 = (ControlTokenConsumer) t;
                for (final Map.Entry<Projection, ControlTokenConsumer> entry : s1.controlFlowsTo.entrySet()) {
                    final ControlTokenConsumer controlFlowTarget = (ControlTokenConsumer) clones.get(entry.getValue());
                    t1.addControlFlowTo(entry.getKey(), controlFlowTarget);
                }
            }
        }
        return clones;
    }

    private boolean isInliningCandidate(final ResolvedMethod rm) {
        if (Modifier.isNative(rm.methodNode.access)) {
            return false;
        }
        final long controlTokens = rm.methodBody.nodes().stream().filter(t ->
                                t.nodeType == NodeType.MonitorEnter ||
                                t.nodeType == NodeType.Goto ||
                                t.nodeType == NodeType.Unwind ||
                                t.nodeType == NodeType.TableSwitch ||
                                t.nodeType == NodeType.ReturnValue ||
                                t.nodeType == NodeType.ClassInitialization ||
                                t.nodeType == NodeType.SetInstanceField ||
                                t.nodeType == NodeType.Return ||
                                t.nodeType == NodeType.Copy ||
                                t.nodeType == NodeType.MonitorExit ||
                                t.nodeType == NodeType.Region ||
                                t.nodeType == NodeType.TryCatch ||
                                t.nodeType == NodeType.ArrayStore ||
                                t.nodeType == NodeType.Nop ||
                                t.nodeType == NodeType.If).count();
        //return controlTokens < 20;
        return "<init>".equals(rm.methodNode.name) && controlTokens < 20;
    }

    @Override
    public boolean optimize(final BackendType backendType, final CompileUnit compileUnit, final ResolvedMethod method) {
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

                final Map<Node, Node> importedNodes = stampInto(rm.methodBody, g, thisRef, arguments);
                final ControlTokenConsumer start = (ControlTokenConsumer) importedNodes.get(rm.methodBody.regionByLabel(Graph.START_REGION_NAME));

                g.replaceInControlFlow(methodInvocation, start);
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
