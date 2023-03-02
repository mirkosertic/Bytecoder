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
package de.mirkosertic.bytecoder.asm.optimizer;

import de.mirkosertic.bytecoder.asm.ir.ResolvedMethod;

public enum Optimizations implements Optimizer {
    DISABLED(new Optimizer[] {
        //new DeleteCopyToUnusedVariable(),
    }),
    DEFAULT(new Optimizer[] {
                new DeleteUnusedConstants(),
                new DeleteUnusedVariables(),
                //new DeleteRedundantControlTokenWithoutDataFlow(),
                //new PromoteVariableToConstant(),
                //new DeleteCopyToUnusedVariable(),
                //new DeleteRedundantVariableCopy(),
                //new VariableShouldBePHI()
            }),
    ALL(new Optimizer[] {
        //new DeleteCopyToUnusedVariable(),
    }),
    ;

    private final Optimizer[] optimizers;

    Optimizations(final Optimizer[] optimizers) {
        this.optimizers = optimizers;
    }

    public boolean optimize(final ResolvedMethod method) {
        boolean graphchanged = false;
        for (final Optimizer o : optimizers) {
            graphchanged = graphchanged | o.optimize(method);
        }
        return graphchanged;
    }
}