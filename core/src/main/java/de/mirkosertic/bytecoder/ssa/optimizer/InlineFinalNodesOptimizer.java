/*
 * Copyright 2018 Mirko Sertic
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
package de.mirkosertic.bytecoder.ssa.optimizer;

import java.util.HashMap;
import java.util.Map;

import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.GraphNode;

public class InlineFinalNodesOptimizer implements Optimizer {

    @Override
    public void optimize(ControlFlowGraph aGraph, BytecodeLinkerContext aLinkerContext) {
        if (aGraph.getKnownNodes().size() == 1) {
            return;
        }

        Map<BytecodeOpcodeAddress, GraphNode> theFinalNodes = new HashMap<>();
        for (GraphNode theNode : aGraph.getKnownNodes()) {
            if (!theNode.getPredecessors().isEmpty() &&
                 theNode.endsWithReturn() &&
                 theNode.getExpressions().toList().size() <= 1) {
                theFinalNodes.put(theNode.getStartAddress(), theNode);
            }
        }

        for (GraphNode theNode : aGraph.getKnownNodes()) {
            inline(theFinalNodes, theNode.getExpressions());
        }

        for (GraphNode theNode : theFinalNodes.values()) {
            aGraph.delete(theNode);
        }
    }

    private void inline(Map<BytecodeOpcodeAddress, GraphNode> aFinalNodes, ExpressionList aList) {
        for (Expression theExpression : aList.toList()) {
            if (theExpression instanceof ExpressionListContainer) {
                ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                for (ExpressionList theList : theContainer.getExpressionLists()) {
                    inline(aFinalNodes, theList);
                }
            }
            if (theExpression instanceof GotoExpression) {
                GotoExpression theGoto = (GotoExpression) theExpression;
                GraphNode thePotentialFinal = aFinalNodes.get(theGoto.getJumpTarget());
                if (thePotentialFinal != null) {
                    aList.replace(theExpression, thePotentialFinal.getExpressions());
                }
            }
        }
    }
}
