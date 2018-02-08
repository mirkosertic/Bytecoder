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
package de.mirkosertic.bytecoder.ssa;

import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.RegionNode;

public abstract class RecursiveExpressionVisitor {

    public void visit(ControlFlowGraph aGraph, BytecodeLinkerContext aLinkerContext) {
        for (RegionNode theNode : aGraph.getDominatedNodes()) {
            visit(aGraph, theNode.getExpressions(), aLinkerContext);
        }
    }

    private void visit(ControlFlowGraph aGraph, ExpressionList aList, BytecodeLinkerContext aLinkerContext) {
        for (Expression theExpression : aList.toList()) {
            if (theExpression instanceof ExpressionListContainer) {
                ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                for (ExpressionList theList : theContainer.getExpressionLists()) {
                    visit(aGraph, theList, aLinkerContext);
                }
            }
            visit(aGraph, aList, theExpression, aLinkerContext);
        }
    }

    protected abstract void visit(ControlFlowGraph aGraph, ExpressionList aList, Expression aExpression, BytecodeLinkerContext aLinkerContext);
}
