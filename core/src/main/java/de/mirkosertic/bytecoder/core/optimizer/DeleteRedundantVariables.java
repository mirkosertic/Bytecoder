/*
 * Copyright 2023 Mirko Sertic
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

import de.mirkosertic.bytecoder.core.ir.ControlTokenConsumer;
import de.mirkosertic.bytecoder.core.ir.Copy;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.ir.Variable;

public class DeleteRedundantVariables implements Optimizer {

    private final NodePatternMatcher patternMatcher;

    public DeleteRedundantVariables() {
        patternMatcher = new NodePatternMatcher(
                NodePredicates.ofType(Copy.class),
                NodePredicates.singlePredWithForwardEdge(),
                NodePredicates.singleSuccWithForwardEdge(),
                NodePredicates.incomingDataFlows(NodePredicates.item(0, NodePredicates.itemOfType(Variable.class))),
                NodePredicates.outgoingDataFlows(NodePredicates.item(0, NodePredicates.itemOfType(Variable.class)))
        );
    }

    @Override
    public boolean optimize(final ResolvedMethod method) {

        final Graph g = method.methodBody;

        for (final Node node : g.nodes()) {
            if (patternMatcher.test(g, node)) {
                final ControlTokenConsumer ct = (ControlTokenConsumer) node;
                final Variable incoming = (Variable) node.incomingDataFlows[0];
                final Variable outgoing = (Variable) node.outgoingFlows[0];

                g.remapDataFlow(outgoing, incoming);

                outgoing.clearIncomingData();

                final ControlTokenConsumer pred = ct.controlComingFrom.get(0);
                final ControlTokenConsumer succ = ct.controlFlowsTo.values().iterator().next();

                pred.remapControlFlowTo(ct, succ);

                g.deleteNode(node);

                return true;
            }
        }

        return false;
    }
}
