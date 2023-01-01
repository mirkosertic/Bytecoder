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
import de.mirkosertic.bytecoder.asm.Node;
import de.mirkosertic.bytecoder.asm.Projection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

public class NodePatternMatcher {

    private final BiPredicate<Node, NodeContext> chainedpredicate;

    public NodePatternMatcher(final BiPredicate<Node, NodeContext>... predicates) {
        BiPredicate<Node, NodeContext> chained = null;
        for (final BiPredicate<Node, NodeContext> p : predicates) {
            if (chained == null) {
                chained = p;
            } else {
                chained = chained.and(p);
            }
        }
        this.chainedpredicate = chained;
    }

    public boolean test(final Node node) {
        final NodeContext nodeContext = new NodeContext();
        if (node instanceof ControlTokenConsumer) {
            final ControlTokenConsumer controlTokenConsumer = (ControlTokenConsumer) node;
            final List<NodeContext.ControlFlowEdge> predsToSucc = new ArrayList<>();
            for (final ControlTokenConsumer pred : controlTokenConsumer.controlComingFrom) {
                for (final Map.Entry<Projection, List<ControlTokenConsumer>> entry : pred.controlFlowsTo.entrySet()) {
                    for (final ControlTokenConsumer n : entry.getValue()) {
                        predsToSucc.add(new NodeContext.ControlFlowEdge(entry.getKey(), n));
                    }
                }
            }
            final List<NodeContext.ControlFlowEdge> nodeToSucc = new ArrayList<>();
            for (final Map.Entry<Projection, List<ControlTokenConsumer>> entry : controlTokenConsumer.controlFlowsTo.entrySet()) {
                for (final ControlTokenConsumer n : entry.getValue()) {
                    nodeToSucc.add(new NodeContext.ControlFlowEdge(entry.getKey(), n));
                }
            }
            nodeContext.predsToSucc = predsToSucc;
            nodeContext.nodeToSucc = nodeToSucc;
        }
        return chainedpredicate.test(node, nodeContext);
    }
}
