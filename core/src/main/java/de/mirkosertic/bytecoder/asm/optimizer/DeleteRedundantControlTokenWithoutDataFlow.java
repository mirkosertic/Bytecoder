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

import de.mirkosertic.bytecoder.asm.ControlTokenConsumer;
import de.mirkosertic.bytecoder.asm.Graph;
import de.mirkosertic.bytecoder.asm.Node;
import de.mirkosertic.bytecoder.asm.PotentialSideeffect;
import de.mirkosertic.bytecoder.asm.Projection;

import java.util.List;
import java.util.Map;

public class DeleteRedundantControlTokenWithoutDataFlow implements Optimizer {

    private final NodePatternMatcher patternMatcher;

    public DeleteRedundantControlTokenWithoutDataFlow() {
        patternMatcher = new NodePatternMatcher(
                NodePredicates.ofType(ControlTokenConsumer.class),
                NodePredicates.ofType(PotentialSideeffect.class).negate(),
                NodePredicates.incomingDataFlows(NodePredicates.empty()),
                NodePredicates.outgoingDataFlows(NodePredicates.empty()),
                NodePredicates.singlePredWithForwardEdge(),
                NodePredicates.singleSuccWithForwardEdge()
        );
    }

    @Override
    public boolean optimize(final Graph g) {
        for (final Node node : g.nodes()) {
            if (patternMatcher.test(node)) {
                final ControlTokenConsumer controlTokenConsumer = (ControlTokenConsumer) node;
                final ControlTokenConsumer prevNode = controlTokenConsumer.controlComingFrom.get(0);

                // TODO: Maybe check for edge types here?
                for (final Map.Entry<Projection, List<ControlTokenConsumer>> entry : controlTokenConsumer.controlFlowsTo.entrySet()) {
                    for (final ControlTokenConsumer targetnode : entry.getValue()) {
                        prevNode.addControlFlowTo(entry.getKey(), targetnode);
                        targetnode.deleteControlFlowFrom(controlTokenConsumer);
                    }
                }

                prevNode.deleteControlFlowTo(controlTokenConsumer);

                g.deleteNode(controlTokenConsumer);
                return true;
            }
        }
        return false;
    }
}
