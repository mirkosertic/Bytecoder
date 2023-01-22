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

import de.mirkosertic.bytecoder.asm.ir.ControlTokenConsumer;
import de.mirkosertic.bytecoder.asm.ir.Graph;
import de.mirkosertic.bytecoder.asm.ir.Node;
import de.mirkosertic.bytecoder.asm.ir.PotentialSideeffect;
import de.mirkosertic.bytecoder.asm.ir.Projection;
import de.mirkosertic.bytecoder.asm.ir.ResolvedMethod;

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
    public boolean optimize(final ResolvedMethod method, final Graph g) {
        for (final Node node : g.nodes()) {
            if (patternMatcher.test(g, node)) {
                final ControlTokenConsumer controlTokenConsumer = (ControlTokenConsumer) node;
                final ControlTokenConsumer prevNode = controlTokenConsumer.controlComingFrom.get(0);

                // TODO: Maybe check for edge types here?
                for (final Map.Entry<Projection, ControlTokenConsumer> entry : controlTokenConsumer.controlFlowsTo.entrySet()) {
                    entry.getValue().deleteControlFlowFrom(controlTokenConsumer);
                    prevNode.addControlFlowTo(entry.getKey(), entry.getValue());
                }

                prevNode.deleteControlFlowTo(controlTokenConsumer);

                g.deleteNode(controlTokenConsumer);
                return true;
            }
        }
        return false;
    }
}
