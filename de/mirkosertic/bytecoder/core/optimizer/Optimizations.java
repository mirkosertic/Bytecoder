/*
 * Copyright 2022 Mirko Sertic
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
package de.mirkosertic.bytecoder.core.optimizer;

import de.mirkosertic.bytecoder.core.backend.BackendType;
import de.mirkosertic.bytecoder.core.backend.CodeGenerationFailure;
import de.mirkosertic.bytecoder.core.backend.sequencer.DominatorTree;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;

public enum Optimizations implements Optimizer {
    DISABLED(new Optimizer[] {
    }),
    DEFAULT(new Optimizer[] {
                new DropRedundantRegions(),
                new MathWithConstants(),
                new IfOnConstant(),
                new CopyToRedundantVariable(),
                new VirtualToDirectInvocation(),
                new DeleteRedundantClassInitializations(),
                //new InlineVoidMethods(),
                //new InlineMethodExpressions(),
                new CMPInNumericalTest(),
                new DropUnusedValues(),
                new CopyToUnusedPHIOrVariable(),
                new SingularPHIOrVariable(),
                //new InefficientSetFieldOrArray(),
            }),
    ALL(new Optimizer[] {
            new DropDebugData(),
            new DropRedundantRegions(),
            new MathWithConstants(),
            new IfOnConstant(),
            new CopyToRedundantVariable(),
            new VirtualToDirectInvocation(),
            new DeleteRedundantClassInitializations(),
            //new InlineVoidMethods(),
            //new InlineMethodExpressions(),
            new CMPInNumericalTest(),
            new DropUnusedValues(),
            new CopyToUnusedPHIOrVariable(),
            new SingularPHIOrVariable(),
            //new InefficientSetFieldOrArray(),
    }),
    ;

    private final Optimizer[] optimizers;

    Optimizations(final Optimizer[] optimizers) {
        this.optimizers = optimizers;
    }

    public boolean optimize(final BackendType backendType, final CompileUnit compileUnit, final ResolvedMethod method) {

        try {
            boolean graphchanged = false;
            for (final Optimizer o : optimizers) {
                graphchanged = graphchanged | o.optimize(backendType, compileUnit, method);
                if (Utils.doSanityCheck()) {
                    method.methodBody.sanityCheck();
                }
            }
            return graphchanged;
        } catch (final RuntimeException e) {
            throw new CodeGenerationFailure(method, new DominatorTree(method.methodBody), e);
        }
    }
}
