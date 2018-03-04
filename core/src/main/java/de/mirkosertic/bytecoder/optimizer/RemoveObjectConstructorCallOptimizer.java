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
package de.mirkosertic.bytecoder.optimizer;

import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.DirectInvokeMethodExpression;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.RecursiveExpressionVisitor;

import java.util.Objects;

public class RemoveObjectConstructorCallOptimizer extends RecursiveExpressionVisitor implements Optimizer {

    @Override
    public void optimize(ControlFlowGraph aGraph, BytecodeLinkerContext aLinkerContext) {
        visit(aGraph, aLinkerContext);
    }

    @Override
    protected void visit(ControlFlowGraph aGraph, ExpressionList aList, Expression aExpression,
            BytecodeLinkerContext aLinkerContext) {
        if (aExpression instanceof DirectInvokeMethodExpression) {
            DirectInvokeMethodExpression theInvoke = (DirectInvokeMethodExpression) aExpression;
            if ("<init>".equals(theInvoke.getMethodName()) && Objects.equals(theInvoke.getClazz().name(), Object.class.getName())) {
                aList.remove(theInvoke);
            }
        }
    }
}
