/*
 * Copyright 2022 Mirko Sertic
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
package de.mirkosertic.bytecoder.asm.optimizer;

import de.mirkosertic.bytecoder.asm.CaughtException;
import de.mirkosertic.bytecoder.asm.Constant;
import de.mirkosertic.bytecoder.asm.ControlTokenConsumer;
import de.mirkosertic.bytecoder.asm.Copy;
import de.mirkosertic.bytecoder.asm.Graph;
import de.mirkosertic.bytecoder.asm.Node;
import de.mirkosertic.bytecoder.asm.Projection;
import de.mirkosertic.bytecoder.asm.Variable;

import java.util.List;
import java.util.Map;

public class PromoteVariableToConstant implements Optimizer {

    private final NodePatternMatcher patternMatcher;

    public PromoteVariableToConstant() {
        patternMatcher = new NodePatternMatcher(
                NodePredicates.ofType(Copy.class),
                NodePredicates.incomingDataFlows(NodePredicates.length(1)),
                NodePredicates.outgoingDataFlows(NodePredicates.length(1)),
                NodePredicates.singlePredWithForwardEdge(),
                NodePredicates.singleSuccWithForwardEdge()
        );
    }

    @Override
    public boolean optimize(final Graph g) {
        for (final Node node : g.nodes()) {
            if (patternMatcher.test(node)) {
                final Copy copy = (Copy) node;
                final Node incoming = copy.incomingDataFlows[0];
                final Node outgoing = copy.outgoingFlows[0];
                if (incoming instanceof Constant && !(incoming instanceof CaughtException) && outgoing instanceof Variable) {

                    incoming.removeFromOutgoingData(copy);
                    outgoing.clearIncomingData();

                    for (final Node target : outgoing.outgoingFlows) {
                        target.replaceIncomingDataFlowsWith(outgoing, incoming);
                    }

                    final ControlTokenConsumer prevNode = copy.controlComingFrom.get(0);

                    // TODO: Maybe check for edge types here?

                    for (final Map.Entry<Projection, List<ControlTokenConsumer>> entry : copy.controlFlowsTo.entrySet()) {
                        for (final ControlTokenConsumer targetnode : entry.getValue()) {
                            prevNode.addControlFlowTo(entry.getKey(), targetnode);
                            targetnode.deleteControlFlowFrom(copy);
                        }
                    }

                    prevNode.deleteControlFlowTo(copy);
                    g.deleteNode(copy);

                    return true;
                }
            }
        }
        return false;
    }
}
