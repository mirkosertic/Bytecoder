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
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum Optimizations implements Optimizer {
    DISABLED(new Optimizer[] {
    }),
    DEFAULT(new Optimizer[] {
                new PHIorVariableIsConstant(),
                new CopyToUnusedPHI(),
                new VariableIsVariable(),
                new CopyToRedundantVariable(),
                new VirtualToDirectInvocation(),
                new DeleteRedundantClassInitializations()
            }),
    ALL(new Optimizer[] {
            new PHIorVariableIsConstant(),
            new CopyToUnusedPHI(),
            new VariableIsVariable(),
            new CopyToRedundantVariable(),
            new VirtualToDirectInvocation(),
            new DeleteRedundantClassInitializations()
    }),
    ;

    private final Optimizer[] optimizers;

    Optimizations(final Optimizer[] optimizers) {
        this.optimizers = optimizers;
    }

    public boolean optimize(final BackendType backendType, final CompileUnit compileUnit, final ResolvedMethod method) {

        //if (!method.owner.type.getClassName().contains("TailcallVarargs")) {
        //    return false;
        //}
        //if (!"eval".equals(method.methodNode.name)) {
        //    return false;
        //}

        boolean graphchanged = false;
        final Set<GlobalOptimizer> go = Arrays.stream(optimizers).filter(t -> t instanceof GlobalOptimizer).map(t -> (GlobalOptimizer) t).collect(Collectors.toSet());
        for (final Optimizer o : optimizers) {
            if (!go.contains(o)) {
                graphchanged = graphchanged | o.optimize(backendType, compileUnit, method);
            }
        }
        if (!graphchanged) {
            for (final GlobalOptimizer o : go) {
                o.optimize(backendType, compileUnit, method);
            }
        }
        return graphchanged;
    }
}
