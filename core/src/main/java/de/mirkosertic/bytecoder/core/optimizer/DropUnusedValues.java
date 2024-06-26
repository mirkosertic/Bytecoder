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
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.NodeType;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.ir.Value;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;

import java.util.stream.Collectors;

public class DropUnusedValues implements Optimizer {

    @Override
    public boolean optimize(final BackendType backendType, final CompileUnit compileUnit, final ResolvedMethod method) {
        final Graph g = method.methodBody;
        boolean changed = false;
        for (final Node value : g.nodes().stream().filter(t -> (t instanceof Value && t.outgoingDataFlows().length == 0)).collect(Collectors.toList())) {
            if (value.nodeType == NodeType.Variable || value.nodeType == NodeType.PHI || value.isConstant()) {
                if (value.incomingDataFlows.length == 0) {
                    g.deleteNode(value);
                    changed = true;
                }
            } else {
                g.deleteNode(value);
                changed = true;
            }
        }

        return changed;
    }
}
