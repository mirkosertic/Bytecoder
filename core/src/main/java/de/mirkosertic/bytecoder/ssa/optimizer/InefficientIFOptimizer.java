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
import de.mirkosertic.bytecoder.ssa.*;

public class InefficientIFOptimizer implements Optimizer {

    @Override
    public void optimize(Program aProgram, BytecodeLinkerContext aLinkerContext) {
        for (Block theBlock : aProgram.getBlocks()) {
            optimizeExpressionList(theBlock.getExpressions(), aLinkerContext);
        }
    }

    private void optimizeExpressionList(ExpressionList aExpressions, BytecodeLinkerContext aLinkerContext) {
        for (Expression theExpression : aExpressions.toList()) {

            if (theExpression instanceof IFExpression) {
                IFExpression theIf = (IFExpression) theExpression;

                Variable theBoolean = theIf.getBooleanExpression();
                if (theBoolean.getValue() instanceof BinaryValue) {
                    BinaryValue theBinary = (BinaryValue) theBoolean.getValue();
                    if (theBinary.getValue1().getValue() instanceof CompareValue && theBinary.getValue2().getValue() instanceof PrimitiveValue) {
                        CompareValue theComare = (CompareValue) theBinary.getValue1().getValue();
                        PrimitiveValue thePrimitive = (PrimitiveValue) theBinary.getValue1().getValue();
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
