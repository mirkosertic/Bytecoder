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

import java.util.Stack;

public class CopyToUnusedPHI implements Optimizer {

    @Override
    public boolean optimize(final BackendType backendType, final CompileUnit compileUnit, final ResolvedMethod method) {
        boolean changed = false;

        final Graph g = method.methodBody;

        // Variable and Constant propagation
        final Stack<Copy> workingQueue = new Stack<>();

        // We search Copy operation to an unused PHI, e.g. the PHI has no outgoing data usage
        // The source of the copy must not be an operation with side effects, to prevent program flow corruption
        g.nodes().stream().filter(t -> t.nodeType == NodeType.Copy && !(t.incomingDataFlows[0].hasSideSideEffectRecursive())).map(t -> (Copy) t).forEach(workingQueue::push);

        // We perform a recursive search across the invocation graph
        while (!workingQueue.isEmpty()) {

            final Copy workingItem = workingQueue.pop();

            final Node[] outgoingDataFlows = workingItem.outgoingDataFlows();
            if (outgoingDataFlows[0].nodeType == NodeType.PHI) {
                final Node phi = outgoingDataFlows[0];
                final Node[] phiOutgoingDataFlows = phi.outgoingDataFlows();
                if (phiOutgoingDataFlows.length == 0) {
                    // This phi is unused
                    if (phi.incomingDataFlows.length == 1) {
                        // And it is assigned exactly once, hence we can delete the phi and the assignment

                        // We remap all control flow from the predecessor of the working item to the successor
                        // of the working item. After this the successor is no longer part of the control flow
                        workingItem.deleteFromControlFlow();

                        g.deleteNode(phi);

                        changed = true;
                    }
                }
            }
        }

        return changed;
    }
}