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

import de.mirkosertic.bytecoder.backend.CompileBackend;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.Statistics;
import de.mirkosertic.bytecoder.ssa.ClassHierarchyAnalysis;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.InvokeDirectMethodExpression;
import de.mirkosertic.bytecoder.ssa.InvokeVirtualMethodExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

import java.util.Optional;

public class InvokeVirtualOptimizerStage implements OptimizerStage {

    @Override
    public Expression optimize(final CompileBackend aBackend, final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext, final RegionNode aCurrentNode,
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
        final Value theValue = aExpression.incomingDataFlows().get(0);
        if (theValue instanceof InvokeVirtualMethodExpression) {
            final Optional<InvokeDirectMethodExpression> theNewExpression = visit((InvokeVirtualMethodExpression) theValue, aLinkerContext);
            theNewExpression.ifPresent(
                    directInvokeMethodExpression -> aExpression.replaceIncomingDataEdge(theValue, directInvokeMethodExpression));
        }
        return aExpression;
    }

    private Expression visit(final ExpressionList aExpressions, final InvokeVirtualMethodExpression aExpression, final BytecodeLinkerContext aLinkerContext) {
        final Optional<InvokeDirectMethodExpression> theNewExpression = visit(aExpression, aLinkerContext);
        if (theNewExpression.isPresent()) {
            final Expression theNew = theNewExpression.get();
            aExpressions.replace(aExpression, theNew);
            return theNew;
        }
        return aExpression;
    }

    private Optional<InvokeDirectMethodExpression> visit(final InvokeVirtualMethodExpression aExpression, final BytecodeLinkerContext aLinkerContext) {
        // Do not optimize interface invocation as they might be used for lambda expressions
        if (aExpression.isInterfaceInvocation()) {
            return Optional.empty();
        }

        final Statistics.Context context = aLinkerContext.getStatistics().context("InvokeVirtualOptimizer");

        context.counter("Total number of invocations").increment();

        final String theMethodName = aExpression.getMethodName();
        final BytecodeMethodSignature theSignature = aExpression.getSignature();

        final ClassHierarchyAnalysis theAnalysis = new ClassHierarchyAnalysis(aLinkerContext);
        final Optional<BytecodeLinkedClass> theClassToInvoke = theAnalysis.classProvidingInvokableMethod(theMethodName, theSignature, aExpression.getInvokedClass(),
                aExpression.incomingDataFlows().get(0),
                aClass -> !aClass.emulatedByRuntime() && !aClass.getClassName().name().equals(Class.class.getName()),
                aMethod -> !aMethod.getAccessFlags().isAbstract() && !aMethod.getAccessFlags().isStatic());

        if (theClassToInvoke.isPresent()) {

            // There is only one class implementing this method and reachable in the hierarchy, so we can make a direct call
            final BytecodeLinkedClass theLinked = theClassToInvoke.get();
            final BytecodeObjectTypeRef theClazz = theLinked.getClassName();

            // Due to method substitution in the JDK emulation layer we might get another method implementation
            // as seen by the wait vs. waitInternal thing in TObject. This is strange, but has to be this way
            // as wait is final and cannot be overwritten anyhow.
            final BytecodeMethod theMethodToInvoke = theLinked.getBytecodeClass().methodByNameAndSignatureOrNull(theMethodName, theSignature);

            final InvokeDirectMethodExpression theNewExpression = new InvokeDirectMethodExpression(aExpression.getProgram(), aExpression.getAddress(), theClazz, theMethodToInvoke.getName().stringValue(),
                    theSignature);
            aExpression.routeIncomingDataFlowsTo(theNewExpression);

            context.counter("Optimized invocations").increment();

            return Optional.of(theNewExpression);
        }
        return Optional.empty();
    }
}