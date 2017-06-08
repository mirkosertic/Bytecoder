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
import de.mirkosertic.bytecoder.core.BytecodeNameAndTypeConstant;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeVirtualMethodIdentifier;
import de.mirkosertic.bytecoder.ssa.Block;
import de.mirkosertic.bytecoder.ssa.DirectInvokeMethodExpression;
import de.mirkosertic.bytecoder.ssa.DirectInvokeMethodValue;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.InitVariableExpression;
import de.mirkosertic.bytecoder.ssa.InvokeVirtualMethodExpression;
import de.mirkosertic.bytecoder.ssa.InvokeVirtualMethodValue;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;

public class InvokeVirtualOptimizer implements Optimizer {

    @Override
    public void optimize(Program aProgram, BytecodeLinkerContext aLinkerContext) {
        for (Block theBlock : aProgram.getBlocks()) {
            optimizeExpressionList(theBlock.getExpressions(), aLinkerContext);
        }
    }

    private void optimizeExpressionList(ExpressionList aExpressions, BytecodeLinkerContext aLinkerContext) {
        for (Expression theExpression : aExpressions.toList()) {

            if (theExpression instanceof InvokeVirtualMethodExpression) {
                InvokeVirtualMethodExpression theInvokeVirtual = (InvokeVirtualMethodExpression) theExpression;
                InvokeVirtualMethodValue theValue = theInvokeVirtual.getValue();

                BytecodeNameAndTypeConstant theNameAndType = theValue.getMethod();
                String theMethodName = theNameAndType.getNameIndex().getName().stringValue();
                BytecodeMethodSignature theSignature = theNameAndType.getDescriptorIndex().methodSignature();

                BytecodeVirtualMethodIdentifier theIdentifier = aLinkerContext.getMethodCollection().toIdentifier(theMethodName, theSignature);
                List<BytecodeLinkedClass> theLinkedClasses = aLinkerContext.getClassesImplementingVirtualMethod(theIdentifier);
                if (theLinkedClasses.size() == 1) {
                    // There is only one class implementing this method, so we can make a direct call
                    BytecodeLinkedClass theLinked = theLinkedClasses.get(0);
                    if (!theLinked.emulatedByRuntime()) {
                        BytecodeObjectTypeRef theClazz = theLinked.getClassName();
                        DirectInvokeMethodValue theNewValue = new DirectInvokeMethodValue(theClazz, theMethodName, theSignature,
                                theValue.getTarget(), theValue.getArguments());
                        DirectInvokeMethodExpression theNewExpression = new DirectInvokeMethodExpression(theNewValue);

                        aExpressions.replace(theExpression, theNewExpression);
                    }
                }
            }

            if (theExpression instanceof InitVariableExpression) {
                InitVariableExpression theInit = (InitVariableExpression) theExpression;
                Variable theVariable = theInit.getVariable();
                Value theValue = theVariable.getValue();
                if (theValue instanceof InvokeVirtualMethodValue) {

                    InvokeVirtualMethodValue theInvokeVirtualValue = (InvokeVirtualMethodValue) theValue;

                    BytecodeNameAndTypeConstant theNameAndType = theInvokeVirtualValue.getMethod();
                    String theMethodName = theNameAndType.getNameIndex().getName().stringValue();
                    BytecodeMethodSignature theSignature = theNameAndType.getDescriptorIndex().methodSignature();

                    BytecodeVirtualMethodIdentifier theIdentifier = aLinkerContext.getMethodCollection().toIdentifier(theMethodName, theSignature);
                    List<BytecodeLinkedClass> theLinkedClasses = aLinkerContext.getClassesImplementingVirtualMethod(theIdentifier);
                    if (theLinkedClasses.size() == 1) {
                        // There is only one class implementing this method, so we can make a direct call
                        BytecodeLinkedClass theLinked = theLinkedClasses.get(0);
                        if (!theLinked.emulatedByRuntime()) {
                            BytecodeObjectTypeRef theClazz = theLinked.getClassName();
                            DirectInvokeMethodValue theNewValue = new DirectInvokeMethodValue(theClazz, theMethodName,
                                    theSignature, theInvokeVirtualValue.getTarget(), theInvokeVirtualValue.getArguments());
                            theVariable.setValue(theNewValue);
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
