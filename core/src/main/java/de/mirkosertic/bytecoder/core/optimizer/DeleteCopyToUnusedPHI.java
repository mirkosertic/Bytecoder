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

import de.mirkosertic.bytecoder.core.ir.Constant;
import de.mirkosertic.bytecoder.core.ir.ControlTokenConsumer;
import de.mirkosertic.bytecoder.core.ir.Copy;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.PHI;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.ir.Variable;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;

public class DeleteCopyToUnusedPHI implements Optimizer {

    private final NodePatternMatcher patternMatcher;

    public DeleteCopyToUnusedPHI() {
        patternMatcher = new NodePatternMatcher(
                NodePredicates.ofType(Copy.class),
                NodePredicates.singlePredWithForwardEdge(),
                NodePredicates.singleSuccWithForwardEdge(),
                NodePredicates.incomingDataFlows(NodePredicates.item(0, NodePredicates.itemOfType(Variable.class).or(NodePredicates.itemOfType(Constant.class)))),
                NodePredicates.outgoingDataFlows(NodePredicates.item(0, NodePredicates.itemOfType(PHI.class)))
        );
    }

    @Override
    public boolean optimize(final CompileUnit compileUnit, final ResolvedMethod method) {

        final Graph g = method.methodBody;

        for (final Node node : g.nodes()) {
            if (patternMatcher.test(g, node)) {
                final ControlTokenConsumer ct = (ControlTokenConsumer) node;
                final Node incoming = node.incomingDataFlows[0];
                final Node outgoing = node.outgoingFlows[0];

                if (outgoing.outgoingFlows.length == 0 && outgoing.incomingDataFlows.length == 1) {

                    incoming.removeFromIncomingData(node);
                    outgoing.clearIncomingData();

                    final ControlTokenConsumer pred = ct.controlComingFrom.iterator().next();
                    final ControlTokenConsumer succ = ct.controlFlowsTo.values().iterator().next();

                    pred.remapControlFlowTo(ct, succ);

                    g.deleteNode(node);

                    return true;
                }
            }
        }

        return false;
    }
}
