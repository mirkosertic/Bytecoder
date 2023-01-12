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
package de.mirkosertic.bytecoder.asm.optimizer;

import de.mirkosertic.bytecoder.asm.ControlTokenConsumer;
import de.mirkosertic.bytecoder.asm.Graph;
import de.mirkosertic.bytecoder.asm.Node;
import de.mirkosertic.bytecoder.asm.Projection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NodePatternMatcher {

    private final GraphNodePredicate chainedpredicate;

    public NodePatternMatcher(final GraphNodePredicate... predicates) {
        GraphNodePredicate chained = null;
        for (final GraphNodePredicate p : predicates) {
            if (chained == null) {
                chained = p;
            } else {
                chained = chained.and(p);
            }
        }
        this.chainedpredicate = chained;
    }

    public boolean test(final Graph g, final Node node) {
        final NodeContext nodeContext = new NodeContext();
        if (node instanceof ControlTokenConsumer) {
            final ControlTokenConsumer controlTokenConsumer = (ControlTokenConsumer) node;
            final List<NodeContext.ControlFlowEdge> predsToSucc = new ArrayList<>();
            for (final ControlTokenConsumer pred : controlTokenConsumer.controlComingFrom) {
                for (final Map.Entry<Projection, ControlTokenConsumer> entry : pred.controlFlowsTo.entrySet()) {
                    predsToSucc.add(new NodeContext.ControlFlowEdge(entry.getKey(), entry.getValue()));
                }
            }
            final List<NodeContext.ControlFlowEdge> nodeToSucc = new ArrayList<>();
            for (final Map.Entry<Projection, ControlTokenConsumer> entry : controlTokenConsumer.controlFlowsTo.entrySet()) {
                nodeToSucc.add(new NodeContext.ControlFlowEdge(entry.getKey(), entry.getValue()));
            }
            nodeContext.predsToSucc = predsToSucc;
            nodeContext.nodeToSucc = nodeToSucc;
        }
        return chainedpredicate.test(g, node, nodeContext);
    }
}
