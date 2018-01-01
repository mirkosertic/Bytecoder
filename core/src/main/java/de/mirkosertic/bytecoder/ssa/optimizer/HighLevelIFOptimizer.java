/*
 * Copyright 2017 Mirko Sertic
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
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExtendedIFExpression;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.GraphNode;
import de.mirkosertic.bytecoder.ssa.IFExpression;

import java.util.List;

public class HighLevelIFOptimizer implements Optimizer {

    @Override
    public void optimize(ControlFlowGraph aGraph, BytecodeLinkerContext aLinkerContext) {
        for (GraphNode theNode : aGraph.getKnownNodes()) {
            ExpressionList theExpressions = theNode.getExpressions();
            for (Expression theExpression : theExpressions.toList()) {
                if (theExpression instanceof IFExpression) {
                    IFExpression theIf = (IFExpression) theExpression;
                    GraphNode theTrueNode = aGraph.nodeStartingAt(theIf.getGotoAddress());
                    if (theTrueNode.isStrictlyDominatedBy(theNode) && (theTrueNode !=  theNode)) {
                        // True branch is dominated by node, we need to check if the false branch is also
                        Expression theLastExpression = theExpressions.lastExpression();
                        if (theLastExpression instanceof GotoExpression) {
                            GotoExpression theGoto = (GotoExpression) theLastExpression;
                            GraphNode theFalseNode = aGraph.nodeStartingAt(theGoto.getJumpTarget());
                            if (theFalseNode.isStrictlyDominatedBy(theNode) && (theFalseNode != theNode)) {
                                // We found something!
                                // Inlining of nodes will be done in other optimization step
                                ExtendedIFExpression theExtended = new ExtendedIFExpression(theIf.getAddress(), theIf.getBooleanValue());
                                theExtended.getTrueBranch().addAll(theIf.getExpressions());

                                List<Expression> theRest = theExpressions.toList();
                                int p = theRest.indexOf(theIf);
                                for (int j=p+1;j<theRest.size();j++) {
                                    Expression theR = theRest.get(j);
                                    theExtended.getFalseBranch().add(theR);
                                    theExpressions.remove(theR);
                                }
                                theExpressions.replace(theIf, theExtended);
                            }
                        }
                    }
                }
            }
        }
    }
}
