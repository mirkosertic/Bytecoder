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

import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum Optimizations implements Optimizer {
    DISABLED(new Optimizer[] {
    }),
    DEFAULT(new Optimizer[] {
                new DeleteUnusedConstants(),
                new DeleteUnusedVariables(),
                new DeleteRedundantVariables(),
                new VariableIsConstant(),
                new VirtualToDirectInvocation(),
                new DeleteCopyToUnusedPHI(),
                new DeleteRedundantClassInitializations()
            }),
    ALL(new Optimizer[] {
            new DeleteUnusedConstants(),
            new DeleteUnusedVariables(),
            new DeleteRedundantVariables(),
            new VariableIsConstant(),
            new VirtualToDirectInvocation(),
            new DeleteCopyToUnusedPHI(),
            new DeleteRedundantClassInitializations()
    }),
    ;

    private final Optimizer[] optimizers;

    Optimizations(final Optimizer[] optimizers) {
        this.optimizers = optimizers;
    }

    public boolean optimize(final CompileUnit compileUnit, final ResolvedMethod method) {
        boolean graphchanged = false;
        final Set<GlobalOptimizer> go = Arrays.stream(optimizers).filter(t -> t instanceof GlobalOptimizer).map(t -> (GlobalOptimizer) t).collect(Collectors.toSet());
        for (final Optimizer o : optimizers) {
            if (!go.contains(o)) {
                graphchanged = graphchanged | o.optimize(compileUnit, method);
            }
        }
        if (!graphchanged) {
            for (final GlobalOptimizer o : go) {
                o.optimize(compileUnit, method);
            }
        }
        return graphchanged;
    }
}
