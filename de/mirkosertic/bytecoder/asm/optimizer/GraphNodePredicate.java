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

import de.mirkosertic.bytecoder.asm.ir.Graph;
import de.mirkosertic.bytecoder.asm.ir.Node;

public interface GraphNodePredicate {

    boolean test(final Graph graph, final Node node, final NodeContext context);

    default GraphNodePredicate and(final GraphNodePredicate p) {
        final GraphNodePredicate original = this;
        return (graph, node, context) -> original.test(graph, node, context) && p.test(graph, node, context);
    }

    default GraphNodePredicate negate() {
        final GraphNodePredicate original = this;
        return (graph, node, context) -> !original.test(graph, node, context);
    }
}
