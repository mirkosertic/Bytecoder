/*
 * Copyright 2019 Mirko Sertic
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
package de.mirkosertic.bytecoder.optimizer;

import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.EdgeType;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;

import java.util.Map;

public class InlineDominatedNodesOptimizer implements Optimizer {

    @Override
    public void optimize(final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext) {
        endless: while (true) {
            for (final RegionNode node : aGraph.getKnownNodes()) {
                for (final Map.Entry<RegionNode.Edge, RegionNode> entry : node.getSuccessors().entrySet()) {
                    if (entry.getKey().getType() == EdgeType.forward) {
                        final RegionNode succ = entry.getValue();
                        if (succ.getType() == RegionNode.BlockType.NORMAL && succ.isStrictlyDominatedBy(node)) {
                            System.out.println(String.format("%s strictly dominates %s", node.getStartAddress(), succ.getStartAddress()));
                            inline(node.getExpressions(), succ);
                            aGraph.inlinedTo(succ, node);
                            continue endless;
                        }
                    }
                }
            }
            return;
        }
    }

    private boolean inline(final ExpressionList list, final RegionNode l) {
        boolean changed = false;
        for (final Expression e : list.toList()) {
            if (e instanceof ExpressionListContainer) {
                final ExpressionListContainer c = (ExpressionListContainer) e;
                for (final ExpressionList l1 : c.getExpressionLists()) {
                    changed = changed | inline(l1, l);
                }
            }
            if (e instanceof GotoExpression) {
                final GotoExpression g = (GotoExpression) e;
                if (g.jumpTarget().equals(l.getStartAddress())) {
                    System.out.println(String.format("Replacing goto to %s with contents of %s", g.jumpTarget(), l.getStartAddress()));
                    list.replace(g, l.getExpressions());
                    changed = true;
                }
            }
        }
        return changed;
    }
}
