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

import java.util.ArrayList;
import java.util.List;

import de.mirkosertic.bytecoder.backend.CompileBackend;
import de.mirkosertic.bytecoder.backend.js.JSSSACompilerBackend;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;

public enum KnownOptimizer implements Optimizer {

    NONE {
        @Override
        public void optimize(final CompileBackend aBackend, final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext) {
        }
    },

    ALL {
        @Override
        public void optimize(final CompileBackend aBackend, final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext) {
            final List<Optimizer> theOptimizer = new ArrayList<>();
            theOptimizer.add(new InlineMethodParameterOptimizer());
            theOptimizer.add(new InlineConstVariablesOptimizer());
            theOptimizer.add(new SinglePassOptimizer(new OptimizerStage[] {
                    aBackend instanceof JSSSACompilerBackend ? new OptimizerStage.NullOptimizerStage() : new InvokeVirtualOptimizerStage(),
                    new InefficientCompareOptimizerStage(),
                    new InlineCallArgumentsOptimizerStage(),
                    new MemberFieldReadOptimizerStage(),
                    new MemberFieldWriteOptimizerStage(),
                    new ArrayEntryReadOptimizerStage(),
                    new ArrayEntryWriteOptimizerStage(),
                    new ArrayReadLengthOptimizerStage(),
                    new BinaryExpressionOptimizerStage()
            }));
            run(aBackend, aGraph, aLinkerContext, theOptimizer);
        }
    },

    EXPERIMENTAL {
        @Override
        public void optimize(final CompileBackend aBackend, final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext) {
            final List<Optimizer> theOptimizer = new ArrayList<>();
            theOptimizer.add(ALL);
            run(aBackend, aGraph, aLinkerContext, theOptimizer);
        }
    },

    LLVM {
        @Override
        public void optimize(final CompileBackend aBackend, final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext) {
            final List<Optimizer> theOptimizer = new ArrayList<>();
            theOptimizer.add(new SinglePassOptimizer(new OptimizerStage[] {
                    new InvokeVirtualOptimizerStage(),
                    new InefficientCompareOptimizerStage(),
            }));
            run(aBackend, aGraph, aLinkerContext, theOptimizer);
        }
    };

    private static void run(final CompileBackend aBackend, final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext, final List<Optimizer> aList) {
        try {
            for (final Optimizer theOptimizer : aList) {
                theOptimizer.optimize(aBackend, aGraph, aLinkerContext);
            }
        } catch (final RuntimeException e) {
            throw new RuntimeException("Error optimizing cfg : " + aGraph.toDOT(), e);
        }
    }
}