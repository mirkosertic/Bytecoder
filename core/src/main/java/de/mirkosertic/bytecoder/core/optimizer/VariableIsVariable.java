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
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.NodeType;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;

import java.util.Arrays;
import java.util.Stack;

public class VariableIsVariable implements Optimizer {

    @Override
    public boolean optimize(final BackendType backendType, final CompileUnit compileUnit, final ResolvedMethod method) {
        boolean changed = false;

        final Graph g = method.methodBody;

        // Variable and Constant propagation
        final Stack<Copy> workingQueue = new Stack<>();

        // We search for Constants and Variables A and check if they are copied to a variable B.
        // In this case, the variable B is redundant and can be replaced with A.
        g.nodes().stream().filter(t -> (t.nodeType == NodeType.Copy) &&
                t.incomingDataFlows[0].nodeType == NodeType.Variable).map(t -> (Copy) t).forEach(workingQueue::push);

        // We perform a recursive search across the invocation graph
        while (!workingQueue.isEmpty()) {

            final Copy workingItem = workingQueue.pop();

            final Node[] outgoingDataFlows = workingItem.outgoingDataFlows();
            if ((outgoingDataFlows[0].nodeType == NodeType.Variable) &&
                // Target node is written exactly once!
                Arrays.stream(outgoingDataFlows[0].incomingDataFlows).filter(t -> t.nodeType == NodeType.Copy).count() == 1) {

                // A Copy token has one incoming dataflow
                final Node source = workingItem.incomingDataFlows[0];
                // And only one outgoing dataflow
                // At this point we are sure it is a variable or phi
                final Node target = outgoingDataFlows[0];

                g.remapDataFlow(target, source);

                // Reroute all other incoming data flows from this variable
                // to the source. This might be setInstanceField, ArrayStore and what ever...
                for (final Node incoming : target.incomingDataFlows) {
                    if (incoming != workingItem) {
                        source.addIncomingData(incoming);
                    }
                }

                // We remap all control flow from the predecessor of the working item to the successor
                // of the working item. After this the successor is no longer part of the control flow
                workingItem.deleteFromControlFlow();

                g.deleteNode(target);

                changed = true;
            }
        }

        //if (!workingQueue.isEmpty()) {
        //    compileUnit.getLogger().info("Next working item would be copy #{}", g.nodes().indexOf(workingQueue.peek()));
        //}

        return changed;
    }
}
