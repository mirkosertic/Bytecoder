/*
 * Copyright 2024 Mirko Sertic
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
import de.mirkosertic.bytecoder.core.ir.Copy;
import de.mirkosertic.bytecoder.core.ir.EdgeType;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.NodeType;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.ir.Value;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class SingularPHIOrVariable implements Optimizer {

    @Override
    public boolean optimize(final BackendType backendType, final CompileUnit compileUnit, final ResolvedMethod method) {

        boolean modified = false;

        final Graph g = method.methodBody;

        // We search for all PHI values or variables in the graph
        for (final Value phiOrVariable : g.nodes().stream().filter(t -> t.nodeType == NodeType.PHI || t.nodeType == NodeType.Variable).map(t -> (Value) t).collect(Collectors.toList())) {

            // Now, we get all sources from all copy operations targeting this PHI value
            final Set<Copy> affectedCopyOperations = Arrays.stream(phiOrVariable.incomingDataFlows).filter(t -> t.nodeType == NodeType.Copy).map(t -> (Copy) t).collect(Collectors.toSet());
            final Set<Node> sourceFromCopies = affectedCopyOperations.stream().map(t -> t.incomingDataFlows[0]).collect(Collectors.toSet());
            if (sourceFromCopies.size() == 1 && affectedCopyOperations.size() == phiOrVariable.incomingDataFlows.length) {
                // We found exactly one source!!
                final Node singleSource = sourceFromCopies.iterator().next();

                // The source must not be a PHI node as this node might be initialized at different times in the control flow
                // Also the copy sources must not be back edges
                if ((singleSource.nodeType == NodeType.Variable || singleSource.isConstant()) && affectedCopyOperations.stream().noneMatch(t -> t.controlFlowsTo.keySet().stream().anyMatch(x -> x.edgeType() == EdgeType.BACK))) {
                    // It is safe, now the usage of the phi can be replaced with the source

                    g.remapDataFlow(phiOrVariable, singleSource);
                    for (final Copy c : affectedCopyOperations) {
                        c.deleteFromControlFlow();
                    }
                    g.deleteNode(phiOrVariable);

                    modified = true;
                }
            }
        }

        return modified;
    }
}
