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

import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.ssa.ContinueExpression;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.GraphNode;

import java.util.Map;
import java.util.Set;

public class ControlFlowOptimizer implements Optimizer {

    @Override
    public void optimize(ControlFlowGraph aGraph, BytecodeLinkerContext aLinkerContext) {
        GraphNode theStartNode = aGraph.startNode();
        recoverFrom(aGraph, theStartNode);
    }

    private void recoverFrom(ControlFlowGraph aGraph, GraphNode aNode) {
        Set<GraphNode> theDominatedNodes = aGraph.dominatedNodesOf(aNode);

        // Check if this is a loop
        // If is detected if there is a back - edge from any dominated nodes to the loop header
        for (GraphNode theNode : theDominatedNodes) {
            for (Map.Entry<GraphNode.Edge, GraphNode> theEntry : theNode.getSuccessors().entrySet()) {
                if (theEntry.getKey().getType() == GraphNode.EdgeType.BACK && theEntry.getValue() == aNode) {
                    // Back edge found!, this is clearly a loop
                    aNode.markAsLoop();

                    // Now, for every dominate node we check if it branches to the loop header
                    // this goto is replaced with a continue expression
                    ContinueExpression theContinue = new ContinueExpression(aNode);
                    for (GraphNode theDominated : theDominatedNodes) {
                        replaceGotoWith(theDominated.getExpressions(), aNode.getStartAddress(), theContinue);
                    }

                    break;
                }
            }
        }


        // Continue to resolve the successors
        Map<GraphNode.Edge, GraphNode> theSuccessors = aNode.getSuccessors();
        for (Map.Entry<GraphNode.Edge, GraphNode> theEntry : theSuccessors.entrySet()) {
            GraphNode theSuccessor = theEntry.getValue();
            if (theEntry.getKey().getType() == GraphNode.EdgeType.NORMAL) {
                replaceGotoWith(aNode.getExpressions(), theSuccessor.getStartAddress(), theSuccessor);
                aGraph.removeDominatedNode(theSuccessor);
                recoverFrom(aGraph, theSuccessor);
            } else {
                replaceGotoWith(aNode.getExpressions(), theSuccessor.getStartAddress(), new ContinueExpression(theSuccessor));
            }
        }
    }

    private void replaceGotoWith(ExpressionList aList, BytecodeOpcodeAddress aAddress, Expression aExpression) {
        for (Expression theExpression : aList.toList()) {
            if (theExpression instanceof GotoExpression) {
                GotoExpression theGoto = (GotoExpression) theExpression;
                if (theGoto.getJumpTarget().equals(aAddress)) {
                    aList.replace(theGoto, aExpression);
                }
            }
            if (theExpression instanceof ExpressionListContainer) {
                ExpressionListContainer theList = (ExpressionListContainer) theExpression;
                for (ExpressionList theSinglle : theList.getExpressionLists()) {
                    replaceGotoWith(theSinglle, aAddress, aExpression);
                }
            }
        }
    }
}
