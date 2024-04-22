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
import de.mirkosertic.bytecoder.core.backend.sequencer.DominatorTree;
import de.mirkosertic.bytecoder.core.ir.ControlTokenConsumer;
import de.mirkosertic.bytecoder.core.ir.EdgeType;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.If;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.NodeType;
import de.mirkosertic.bytecoder.core.ir.PrimitiveInt;
import de.mirkosertic.bytecoder.core.ir.Projection;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;

import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class IfOnConstantOptimizer implements Optimizer {

    @Override
    public boolean optimize(final BackendType backendType, final CompileUnit compileUnit, final ResolvedMethod method) {
        boolean changed = false;

        final Graph g = method.methodBody;

        // Variable and Constant propagation
        final Stack<If> workingQueue = new Stack<>();

        // We search Copy operation to an unused PHI or Variable, e.g. the PHI or Variable has no outgoing data usage
        // The source of the copy must not be an operation with side effects, to prevent program flow corruption
        g.nodes().stream().filter(t -> t.nodeType == NodeType.If && (t.incomingDataFlows[0].nodeType == NodeType.PrimitiveInt)).map(t -> (If) t).forEach(workingQueue::push);

        // We perform a recursive search across the invocation graph
        while (!workingQueue.isEmpty()) {
            final If workingItem = workingQueue.pop();
            final PrimitiveInt condition = (PrimitiveInt) workingItem.incomingDataFlows[0];

            ControlTokenConsumer trueBranch = null;
            ControlTokenConsumer falseBranch = null;
            for (final Map.Entry<Projection, ControlTokenConsumer> entry : workingItem.controlFlowsTo.entrySet()) {
                if (entry.getKey() instanceof Projection.TrueProjection && entry.getKey().edgeType() != EdgeType.BACK) {
                    trueBranch = entry.getValue();
                }
                if (entry.getKey() instanceof Projection.FalseProjection && entry.getKey().edgeType() != EdgeType.BACK) {
                    falseBranch = entry.getValue();
                }
            }

            if (trueBranch != null && falseBranch != null && trueBranch != falseBranch) {
                final DominatorTree dt = new DominatorTree(g);

                final Set<ControlTokenConsumer> branchToBeDeleted;
                final ControlTokenConsumer replacementForIf;
                if (condition.value == 1) {
                    // Always true
                    branchToBeDeleted = dt.domSetOf(falseBranch);
                    replacementForIf = trueBranch;
                } else {
                    // Always false
                    branchToBeDeleted = dt.domSetOf(trueBranch);
                    replacementForIf = falseBranch;
                }
                for (final ControlTokenConsumer token : branchToBeDeleted) {
                    for (final ControlTokenConsumer flowsTo : token.controlFlowsTo.values()) {
                        flowsTo.controlComingFrom.remove(token);
                    }
                    for (final Node dataFlowsTo : token.outgoingDataFlows()) {
                        dataFlowsTo.removeFromIncomingData(token);
                    }
                    g.deleteNode(token);
                }

                replacementForIf.controlComingFrom.remove(workingItem);
                g.replaceInControlFlow(workingItem, replacementForIf);
                g.deleteNode(workingItem);

                changed = true;
            }
        }

        return changed;
    }
}
