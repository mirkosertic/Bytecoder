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
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InlineMethodParameterOptimizer implements Optimizer {

    @Override
    public void optimize(final CompileBackend aBackend, final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext) {
        final Program theProgram = aGraph.getProgram();

        // We need a list of variables that are only initialized once with method parameters
        final Map<Variable, Value> theOnceInitialized = new HashMap<>();

        for (final Variable v : theProgram.getVariables()) {
            if (!v.isSynthetic()) {
                final List<Value> theIncoming = v.incomingDataFlows();
                if (theIncoming.size() == 1) {
                    final Value theSingleValue = theIncoming.get(0);
                    if ((theSingleValue instanceof Variable) && ((Variable) theSingleValue).isSynthetic()) {
                        // We found something
                        theOnceInitialized.put(v, theSingleValue);
                    }
                }
            }
        }

        // Now we iterate over variables that are initialized with one of the once initialized
        // those inits are replaced by the method parameter values
        if (!theOnceInitialized.isEmpty()) {
            final SinglePassOptimizer theSinglePass = new SinglePassOptimizer(new OptimizerStage[]{
                    (aCompilBackend1, aGraph1, aLinkerContext1, aCurrentNode, aExpressionList, aExpression) -> {
                        if (aExpression instanceof VariableAssignmentExpression) {
                            final VariableAssignmentExpression theVarAssign = (VariableAssignmentExpression) aExpression;
                            final Value theValue = theVarAssign.incomingDataFlows().get(0);
                            if (theValue instanceof Variable) {
                                final Value theReplacement = theOnceInitialized.get(theValue);
                                if (theReplacement != null) {
                                    theVarAssign.replaceIncomingDataEdge(theValue, theReplacement);
                                }
                            }
                        }
                        return aExpression;
                    }
            });
            theSinglePass.optimize(aBackend, aGraph, aLinkerContext);

            // Todo: Wipeout from livein/out and phi value propagation
        }
    }
}
