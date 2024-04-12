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
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.NodeType;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.ir.Variable;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class CopyToRedundantVariable implements Optimizer {

    private List<Node> evaluationOrderOf(final Node node) {
        final List<Node> result = new ArrayList<>();

        // We do a DFS here
        final Set<Node> visited = new HashSet<>();
        final Stack<Node> workingQueue = new Stack<>();

        for (int i = node.incomingDataFlows.length - 1; i >= 0; i--) {
            workingQueue.push(node.incomingDataFlows[i]);
        }
        visited.add(node);

        while (!workingQueue.isEmpty()) {
            final Node workingItem = workingQueue.pop();
            if (visited.add(workingItem)) {
                if (!(workingItem instanceof ControlTokenConsumer)) {
                    if (workingItem.hasSideSideEffect() || workingItem.nodeType == NodeType.Variable) {
                        result.add(workingItem);
                    } else {
                        for (int i = workingItem.incomingDataFlows.length - 1; i >= 0; i--) {
                            workingQueue.push(workingItem.incomingDataFlows[i]);
                        }
                    }
                }
            }
        }

        return result;
    }

    @Override
    public boolean optimize(final BackendType backendType, final CompileUnit compileUnit, final ResolvedMethod method) {

        boolean changed = false;

        final Graph g = method.methodBody;

        // Variable and Constant propagation
        final Stack<Copy> workingQueue = new Stack<>();

        // We search Copy operation to a variable. The Copy is followed by a single successor, and the target variable is only used
        // by this successor. The variable can be replaced by the copy source if it is at the first place of the evaluation order
        // of the successors incoming data flows.
        g.nodes().stream().filter(t -> t.nodeType == NodeType.Copy).map(t -> (Copy) t).filter(t -> t.controlFlowsTo.size() == 1).forEach(workingQueue::push);

        // We perform a recursive search across the invocation graph
        while (!workingQueue.isEmpty()) {

            final Copy workingItem = workingQueue.pop();

            final Node source = workingItem.incomingDataFlows[0];

            final Node[] outgoing = workingItem.outgoingDataFlows();
            if (outgoing.length == 1 && outgoing[0].nodeType == NodeType.Variable) {
                final Variable variableToCheck = (Variable) outgoing[0];
                if (variableToCheck.incomingDataFlows.length == 1 && variableToCheck.outgoingDataFlows().length == 1) {
                    final ControlTokenConsumer successor = workingItem.controlFlowsTo.values().stream().findFirst().get();

                    final List<Node> evaluationOrder = evaluationOrderOf(successor);

                    if (!evaluationOrder.isEmpty() && evaluationOrder.get(0) == variableToCheck) {
                        // We found a candidate

                        g.remapDataFlow(variableToCheck, source);

                        workingItem.deleteFromControlFlow();

                        g.deleteNode(variableToCheck);

                        changed = true;
                    }
                }
            }
        }

        return changed;
    }

}
