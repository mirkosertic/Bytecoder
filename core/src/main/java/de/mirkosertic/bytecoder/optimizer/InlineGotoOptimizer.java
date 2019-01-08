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
package de.mirkosertic.bytecoder.optimizer;

import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class InlineGotoOptimizer implements Optimizer {

    @Override
    public void optimize(ControlFlowGraph aGraph, BytecodeLinkerContext aLinkerContext) {

        while (true) {
            boolean theChanged = false;
            for (RegionNode theNode : aGraph.getKnownNodes()) {
                List<BytecodeOpcodeAddress> theJumpTargets = theNode.getExpressions().jumpTargets();
                if (performNodeInlining(aGraph, theNode, theNode.getExpressions(), theJumpTargets)) {
                    theChanged = true;
                    break;
                }
            }
            if (!theChanged) {
                return;
            }
        }
    }

    private boolean performNodeInlining(ControlFlowGraph aGraph, RegionNode aNode, ExpressionList aList, List<BytecodeOpcodeAddress> aJumpTargets) {
        for (Expression theExpression : aList.toList()) {
            if (theExpression instanceof ExpressionListContainer) {
                ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                for (ExpressionList theList : theContainer.getExpressionLists()) {
                    if (performNodeInlining(aGraph, aNode, theList, aJumpTargets)) {
                        return true;
                    }
                }
            }
            if (theExpression instanceof GotoExpression) {
                GotoExpression theGOTO = (GotoExpression) theExpression;
                RegionNode theTargetNode = aGraph.nodeStartingAt(theGOTO.getJumpTarget());

                if (theTargetNode.isStrictlyDominatedBy(aNode)) {

                    BytecodeOpcodeAddress theJumpTarget = theGOTO.getJumpTarget();
                    int theCount = 0;
                    for (BytecodeOpcodeAddress theEntry : aJumpTargets) {
                        if (theEntry.equals(theJumpTarget)) {
                            theCount++;
                        }
                    }

                    if (theCount == 1 && theTargetNode.getStartAddress().getAddress() != 0 && isSafeToInline(aGraph, aNode, theTargetNode)) {
                        // Node can be inlined
                        aGraph.delete(theTargetNode);
                        aList.replace(theGOTO, theTargetNode.getExpressions());

                        aNode.inheritSuccessorsOf(theTargetNode);

                        for (RegionNode theNode : aGraph.getKnownNodes()) {
                            recomputeGotos(theNode.getExpressions(), theTargetNode.getStartAddress(), aNode.getStartAddress());
                        }

                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void recomputeGotos(ExpressionList aList, BytecodeOpcodeAddress aOriginal, BytecodeOpcodeAddress aNew) {
        for (Expression theExpression : aList.toList()) {
            if (theExpression instanceof ExpressionListContainer) {
                ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                for (ExpressionList theList : theContainer.getExpressionLists()) {
                    recomputeGotos(theList, aOriginal, aNew);
                }
            }
            if (theExpression instanceof GotoExpression) {
                GotoExpression theGoto = (GotoExpression) theExpression;
                if (Objects.equals(theGoto.getJumpTarget(), aOriginal)) {
                    GotoExpression theNewGoto = new GotoExpression(aNew);
                    aList.replace(theGoto, theNewGoto);
                }
            }
        }
    }

    public boolean isSafeToInline(ControlFlowGraph aGraph, RegionNode aSourceNode, RegionNode aTargetNode) {
        List<RegionNode.ExceptionHandler> theHandler = aGraph.exceptionHandlersStartingAt(aTargetNode.getStartAddress());
        if (theHandler.isEmpty()) {
            // No exception handlers allocated to this block,
            // to it can be inlined
            return true;
        }
        List<RegionNode.ExceptionHandler> theActiveHandlers = aGraph.exceptionHandlersActiveAt(aSourceNode.getStartAddress());
        if (theActiveHandlers.size() == theHandler.size()) {
            // We collect the targets
            if (theActiveHandlers.size() != 1) {
                throw new IllegalStateException("Nested try blocks not supported yet!");
            }
            RegionNode.ExceptionHandler theA = theHandler.get(0);
            RegionNode.ExceptionHandler theB = theActiveHandlers.get(0);
            return theA.sameCatchBlockAs(theB);
        }
        return false;
    }
}
