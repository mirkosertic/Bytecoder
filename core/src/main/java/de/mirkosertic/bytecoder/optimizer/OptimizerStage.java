/*
 * Copyright 2019 Mirko Sertic
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
import de.mirkosertic.bytecoder.core.AnalysisStack;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.RegionNode;

public interface OptimizerStage {

    Expression optimize(CompileBackend aBackend,
                        ControlFlowGraph aGraph,
                        BytecodeLinkerContext aLinkerContext,
                        RegionNode aCurrentNode,
                        ExpressionList aExpressionList,
                        Expression aExpression,
                        AnalysisStack analysisStack);

    class NullOptimizerStage implements OptimizerStage {
        @Override
        public Expression optimize(final CompileBackend aBackend,
                                   final ControlFlowGraph aGraph,
                                   final BytecodeLinkerContext aLinkerContext,
                                   final RegionNode aCurrentNode,
                                   final ExpressionList aExpressionList,
                                   final Expression aExpression,
                                   final AnalysisStack analysisStack) {
            return aExpression;
        }
    }
}