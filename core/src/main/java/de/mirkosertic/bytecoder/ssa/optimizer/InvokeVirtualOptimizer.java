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

import java.util.List;

import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeVirtualMethodIdentifier;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.DirectInvokeMethodExpression;
import de.mirkosertic.bytecoder.ssa.DirectInvokeMethodValue;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;
import de.mirkosertic.bytecoder.ssa.InvokeVirtualMethodExpression;
import de.mirkosertic.bytecoder.ssa.InvokeVirtualMethodValue;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;

public class InvokeVirtualOptimizer implements Optimizer {

    @Override
    public void optimize(ControlFlowGraph aGraph, BytecodeLinkerContext aLinkerContext) {
        for (RegionNode theNode : aGraph.getDominatedNodes()) {
            optimizeExpressionList(theNode.getExpressions(), aLinkerContext);
        }
    }

    private void optimizeExpressionList(ExpressionList aExpressions, BytecodeLinkerContext aLinkerContext) {
        for (Expression theExpression : aExpressions.toList()) {

            if (theExpression instanceof InvokeVirtualMethodExpression) {
                InvokeVirtualMethodExpression theInvokeVirtual = (InvokeVirtualMethodExpression) theExpression;
                InvokeVirtualMethodValue theValue = theInvokeVirtual.getValue();

                String theMethodName = theValue.getMethodName();
                BytecodeMethodSignature theSignature = theValue.getSignature();

                BytecodeVirtualMethodIdentifier theIdentifier = aLinkerContext.getMethodCollection().toIdentifier(theMethodName, theSignature);
                List<BytecodeLinkedClass> theLinkedClasses = aLinkerContext.getClassesImplementingVirtualMethod(theIdentifier);
                if (theLinkedClasses.size() == 1) {
                    // There is only one class implementing this method, so we can make a direct call
                    BytecodeLinkedClass theLinked = theLinkedClasses.get(0);
                    if (!theLinked.emulatedByRuntime()) {
                        BytecodeObjectTypeRef theClazz = theLinked.getClassName();

                        Value theTarget = theValue.consumedValues(Value.ConsumptionType.INVOCATIONTARGET).get(0);
                        List<Value> theVariables = theValue.consumedValues(Value.ConsumptionType.ARGUMENT);

                        DirectInvokeMethodValue theNewValue = new DirectInvokeMethodValue(theClazz, theMethodName, theSignature,
                                theTarget, theVariables);
                        DirectInvokeMethodExpression theNewExpression = new DirectInvokeMethodExpression(theNewValue);
                        aExpressions.replace(theExpression, theNewExpression);

                        theValue.unbind();
                    }
                }
            }

            if (theExpression instanceof VariableAssignmentExpression) {
                VariableAssignmentExpression theInit = (VariableAssignmentExpression) theExpression;
                Variable theVariable = theInit.getVariable();
                Value theValue = theInit.getValue();
                if (theValue instanceof InvokeVirtualMethodValue) {

                    InvokeVirtualMethodValue theInvokeVirtualValue = (InvokeVirtualMethodValue) theValue;

                    String theMethodName = theInvokeVirtualValue.getMethodName();
                    BytecodeMethodSignature theSignature = theInvokeVirtualValue.getSignature();

                    Value theTarget = theValue.consumedValues(Value.ConsumptionType.INVOCATIONTARGET).get(0);
                    List<Value> theVariables = theValue.consumedValues(Value.ConsumptionType.ARGUMENT);

                    BytecodeVirtualMethodIdentifier theIdentifier = aLinkerContext.getMethodCollection().toIdentifier(theMethodName, theSignature);
                    List<BytecodeLinkedClass> theLinkedClasses = aLinkerContext.getClassesImplementingVirtualMethod(theIdentifier);
                    if (theLinkedClasses.size() == 1) {
                        // There is only one class implementing this method, so we can make a direct call
                        BytecodeLinkedClass theLinked = theLinkedClasses.get(0);
                        if (!theLinked.emulatedByRuntime()) {
                            BytecodeObjectTypeRef theClazz = theLinked.getClassName();
                            DirectInvokeMethodValue theNewValue = new DirectInvokeMethodValue(theClazz, theMethodName,
                                    theSignature, theTarget, theVariables);

                            theInvokeVirtualValue.unbind();
                            theVariable.initializeWith(theNewValue);

                            theValue.unbind();

                            VariableAssignmentExpression theNewInit = new VariableAssignmentExpression(theVariable, theNewValue);
                            aExpressions.replace(theInit, theNewInit);
                        }
                    }
                }
            }

            if (theExpression instanceof ExpressionListContainer) {
                ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                for (ExpressionList theSub : theContainer.getExpressionLists()) {
                    optimizeExpressionList(theSub, aLinkerContext);
                }
            }
        }
    }
}