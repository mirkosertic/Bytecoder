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

import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.ssa.BlockState;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.InvocationExpression;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UnusedReturnValueOptimizer implements Optimizer {

    @Override
    public void optimize(final ControlFlowGraph aGraph, final BytecodeLinkerContext aLinkerContext) {
        final Program theProgram = aGraph.getProgram();
        final Map<Variable, InvocationExpression> theMappings = new HashMap<>();

        final Set<Value> theLiveOuts = new HashSet<>();
        for (final RegionNode n : theProgram.getControlFlowGraph().dominators().getPreOrder()) {
            final BlockState theLiveout = n.liveOut();
            theLiveOuts.addAll(theLiveout.getPorts().values());
        }

        for (final Variable v : theProgram.getVariables()) {
            if (!v.isSynthetic() && !theLiveOuts.contains(v)) {
                final List<Value> theIncoming = v.incomingDataFlows();
                if (theIncoming.size() == 1) {
                    final Value theSingleValue = theIncoming.get(0);
                    // Search for invocation results that have no usages
                    if (theSingleValue instanceof InvocationExpression && v.outgoingEdges().count() == 0) {
                        // We found something
                        theMappings.put(v, (InvocationExpression) theSingleValue);
                    }
                }
            }
        }

        // Now, we have to delete the no longer needed assignments
        if (!theMappings.isEmpty()) {
            final SinglePassOptimizer theSinglePass = new SinglePassOptimizer(new OptimizerStage[]{
                    (aGraph1, aLinkerContext1, aCurrentNode, aExpressionList, aExpression) -> {
                        if (aExpression instanceof VariableAssignmentExpression) {
                            final VariableAssignmentExpression theVarAssign = (VariableAssignmentExpression) aExpression;
                            final Variable theVar = theVarAssign.getVariable();
                            final InvocationExpression inv = theMappings.get(theVar);
                            if (inv != null) {
                                aExpressionList.replace(theVarAssign, inv);
                                theProgram.deleteVariable(theVar);
                            }
                            return inv;
                        }
                        return aExpression;
                    }
            });
            theSinglePass.optimize(aGraph, aLinkerContext);
        }
    }
}
