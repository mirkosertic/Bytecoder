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

import de.mirkosertic.bytecoder.asm.ir.AbstractVar;
import de.mirkosertic.bytecoder.asm.ir.Graph;
import de.mirkosertic.bytecoder.asm.ir.Node;
import de.mirkosertic.bytecoder.asm.ir.ResolvedMethod;

public class DeleteUnusedVariables implements Optimizer {

    private final NodePatternMatcher patternMatcher;

    public DeleteUnusedVariables() {
        patternMatcher = new NodePatternMatcher(
            NodePredicates.ofType(AbstractVar.class),
            NodePredicates.incomingDataFlows(NodePredicates.empty())
        );
    }

    @Override
    public boolean optimize(final ResolvedMethod method, final Graph g) {
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