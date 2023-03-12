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
package de.mirkosertic.bytecoder.core.optimizer;

import de.mirkosertic.bytecoder.core.ir.Constant;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;

public class DeleteUnusedConstants implements Optimizer {

    private final NodePatternMatcher patternMatcher;

    public DeleteUnusedConstants() {
        patternMatcher = new NodePatternMatcher(
            NodePredicates.ofType(Constant.class),
            NodePredicates.outgoingDataFlows(NodePredicates.empty())
        );
    }

    @Override
    public boolean optimize(final ResolvedMethod method) {
        final Graph g = method.methodBody;
        boolean changed = false;
        for (final Node node : g.nodes()) {
            if (patternMatcher.test(g, node)) {
                changed = true;
                g.deleteNode(node);
            }
        }
        return changed;
    }
}
