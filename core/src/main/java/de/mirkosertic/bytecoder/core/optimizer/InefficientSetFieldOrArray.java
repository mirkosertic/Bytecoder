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
import de.mirkosertic.bytecoder.core.ir.ControlTokenConsumer;
import de.mirkosertic.bytecoder.core.ir.Copy;
import de.mirkosertic.bytecoder.core.ir.EdgeType;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.NodeType;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.ir.Variable;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;

import java.util.stream.Collectors;

public class InefficientSetFieldOrArray implements Optimizer {

    @Override
    public boolean optimize(final BackendType backendType, final CompileUnit compileUnit, final ResolvedMethod method) {
        final Graph g = method.methodBody;
        boolean changed = false;
        for (final Copy copy : g.nodes().stream().filter(t -> t.nodeType == NodeType.Copy).map(t -> (Copy) t).collect(Collectors.toList())) {
            final Node[] outgoing = copy.outgoingDataFlows();
            if (outgoing.length == 1 && outgoing[0].nodeType == NodeType.Variable && copy.controlFlowsTo.size() == 1 && copy.controlFlowsTo.keySet().stream().noneMatch(t -> t.edgeType() == EdgeType.BACK)) {
                final Variable copyTarget = (Variable) outgoing[0];
                // We are copying to a variable, and there is only one following successor control token
                final ControlTokenConsumer successor = copy.controlFlowsTo.values().iterator().next();
                if (successor.nodeType == NodeType.SetClassField || successor.nodeType == NodeType.SetInstanceField || successor.nodeType == NodeType.ArrayStore) {
                    final Node source = copy.incomingDataFlows[0];
                    // Case one : there is something written to the copyTarget
                    final Node[] succOutgoing = successor.outgoingDataFlows();
                    if (source.nodeType == NodeType.Variable && copyTarget.incomingDataFlows.length == 2 && copyTarget.incomingDataFlows[0] == copy && copyTarget.incomingDataFlows[1] == successor && succOutgoing.length == 1 && succOutgoing[0] == copyTarget) {

                        copyTarget.removeFromIncomingData(copy);

                        copyTarget.removeFromIncomingData(successor);
                        source.addIncomingData(successor);

                        copy.deleteFromControlFlow();

                        if (copyTarget.outgoingDataFlows().length == 0) {
                            g.deleteNode(copyTarget);
                        }

                        changed = true;
                    }
                }
            }
        }

        return changed;

    }
}
