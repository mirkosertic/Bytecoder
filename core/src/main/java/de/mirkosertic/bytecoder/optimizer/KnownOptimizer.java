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

import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;

import java.util.ArrayList;
import java.util.List;

public enum KnownOptimizer implements Optimizer {

    NONE {
        @Override
        public void optimize(final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext) {
        }
    },

    ALL {
        @Override
        public void optimize(final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext) {
            final List<Optimizer> theOptimizer = new ArrayList<>();
            theOptimizer.add(new SinglePassOptimizer(new OptimizerStage[] {
                    new RemoveObjectConstructorCallOptimizerStage(),
                    new DropUnusedReturnValuesOptimizerStage(),
                    new InefficientCompareOptimizerStage(),
                    new RedundantVariablesOptimizerStage(),
                    new InvokeVirtualOptimizerStage(),
                    new DirectAssignmentOptimizerStage(),
                    new RedundantVariablesForIfOptimizerStage(),
            }));
            run(aGraph, aLinkerContext, theOptimizer);
        }
    },

    EXPERIMENTAL {
        @Override
        public void optimize(final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext) {
            final List<Optimizer> theOptimizer = new ArrayList<>();
            theOptimizer.add(ALL);
            run(aGraph, aLinkerContext, theOptimizer);
        }
    };

    private static void run(final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext, final List<Optimizer> aList) {
        try {
            for (final Optimizer theOptimizer : aList) {
                theOptimizer.optimize(aGraph, aLinkerContext);
            }
        } catch (final RuntimeException e) {
            throw new RuntimeException("Error optimizing cfg : " + aGraph.toDOT(), e);
        }
    }
}