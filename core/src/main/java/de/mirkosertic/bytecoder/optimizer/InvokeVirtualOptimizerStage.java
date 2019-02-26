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

import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeVirtualMethodIdentifier;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.DirectInvokeMethodExpression;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.InvokeVirtualMethodExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

import java.util.List;
import java.util.Optional;

public class InvokeVirtualOptimizerStage implements OptimizerStage {

    @Override
    public Expression optimize(final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext, final RegionNode aCurrentNode,
            final ExpressionList aExpressionList, final Expression aExpression) {

        if (aExpression instanceof InvokeVirtualMethodExpression) {
            return visit(aExpressionList, (InvokeVirtualMethodExpression) aExpression, aLinkerContext);
        }

        if (aExpression instanceof VariableAssignmentExpression) {
            return visit((VariableAssignmentExpression) aExpression, aLinkerContext);
        }

        return aExpression;
    }

    private Expression visit(final VariableAssignmentExpression aExpression, final BytecodeLinkerContext aLinkerContext) {
        final Value theValue = aExpression.getValue();
        if (theValue instanceof InvokeVirtualMethodExpression) {
            final Optional<DirectInvokeMethodExpression> theNewExpression = visit((InvokeVirtualMethodExpression) theValue, aLinkerContext);
            theNewExpression.ifPresent(
                    directInvokeMethodExpression -> aExpression.replaceIncomingDataEdge(theValue, directInvokeMethodExpression));
        }
        return aExpression;
    }

    private Expression visit(final ExpressionList aExpressions, final InvokeVirtualMethodExpression aExpression, final BytecodeLinkerContext aLinkerContext) {
        final Optional<DirectInvokeMethodExpression> theNewExpression = visit(aExpression, aLinkerContext);
        if (theNewExpression.isPresent()) {
            final Expression theNew = theNewExpression.get();
            aExpressions.replace(aExpression, theNew);
            return theNew;
        }
        return aExpression;
    }

    private Optional<DirectInvokeMethodExpression> visit(final InvokeVirtualMethodExpression aExpression, final BytecodeLinkerContext aLinkerContext) {
        // Do not optimize interface invocation as they might be used for lambda expressions
        if (aExpression.isInterfaceInvocation()) {
            return Optional.empty();
        }

        final String theMethodName = aExpression.getMethodName();
        final BytecodeMethodSignature theSignature = aExpression.getSignature();

        final BytecodeVirtualMethodIdentifier theIdentifier = aLinkerContext.getMethodCollection().toIdentifier(theMethodName, theSignature);
        final List<BytecodeLinkedClass> theLinkedClasses = aLinkerContext.getClassesImplementingVirtualMethod(theIdentifier);
        if (theLinkedClasses.size() == 1) {
            // There is only one class implementing this method, so we can make a direct call
            final BytecodeLinkedClass theLinked = theLinkedClasses.get(0);
            if (!theLinked.emulatedByRuntime()) {

                final BytecodeMethod theMethod = theLinked.getBytecodeClass().methodByNameAndSignatureOrNull(theMethodName, theSignature);
                if (!theMethod.getAccessFlags().isAbstract()) {
                    final BytecodeObjectTypeRef theClazz = theLinked.getClassName();

                    final DirectInvokeMethodExpression theNewExpression = new DirectInvokeMethodExpression(aExpression.getProgram(), aExpression.getAddress(), theClazz, theMethodName,
                            theSignature);
                    aExpression.routeIncomingDataFlowsTo(theNewExpression);

                    return Optional.of(theNewExpression);
                }
            }
        }
        return Optional.empty();
    }
}