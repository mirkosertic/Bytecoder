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

import de.mirkosertic.bytecoder.core.ir.EdgeType;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.Projection;

import java.util.function.Predicate;

public class NodePredicates {

    public static Predicate<Node> itemOfType(final Class nodeClass) {
        return item -> nodeClass.isAssignableFrom(item.getClass());
    }

    public static GraphNodePredicate ofType(final Class nodeClass) {
        return (graph, node, context) -> nodeClass.isAssignableFrom(node.getClass());
    }

    public static Predicate<Node[]> empty() {
        return length(0);
    }

    public static Predicate<Node[]> length(final int expected) {
        return nodes -> nodes.length == expected;
    }

    public static Predicate<Node[]> item(final int index, final Predicate<Node> pred) {
        return nodes -> pred.test(nodes[index]);
    }

    public static GraphNodePredicate incomingDataFlows(final Predicate<Node[]> pred) {
        return (graph, node, context) -> pred.test(node.incomingDataFlows);
    }

    public static GraphNodePredicate outgoingDataFlows(final Predicate<Node[]> pred) {
        return (graph, node, context) -> pred.test(node.outgoingFlows);
    }

    public static GraphNodePredicate singlePredWithForwardEdge() {
        return (graph, node, nodeContext) -> nodeContext.predsToSucc != null && nodeContext.predsToSucc.size() == 1 &&
                (nodeContext.predsToSucc.get(0).projection instanceof Projection.DefaultProjection) &&
                nodeContext.predsToSucc.get(0).projection.edgeType() == EdgeType.FORWARD;
    }

    public static GraphNodePredicate singleSuccWithForwardEdge() {
        return (graph, node, nodeContext) -> nodeContext.nodeToSucc != null && nodeContext.nodeToSucc.size() == 1 &&
                (nodeContext.nodeToSucc.get(0).projection instanceof Projection.DefaultProjection) &&
                nodeContext.nodeToSucc.get(0).projection.edgeType() == EdgeType.FORWARD;
    }
}
