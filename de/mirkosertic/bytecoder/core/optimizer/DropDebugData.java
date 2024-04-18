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
import de.mirkosertic.bytecoder.core.ir.EdgeType;
import de.mirkosertic.bytecoder.core.ir.FrameDebugInfo;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.LineNumberDebugInfo;
import de.mirkosertic.bytecoder.core.ir.NodeType;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;

import java.util.stream.Collectors;

public class DropDebugData implements Optimizer {

    @Override
    public boolean optimize(final BackendType backendType, final CompileUnit compileUnit, final ResolvedMethod method) {
        final Graph g = method.methodBody;
        boolean changed = false;
        for (final ControlTokenConsumer t : g.nodes().stream().filter(t -> t.nodeType == NodeType.FrameDebugInfo || t.nodeType == NodeType.LineNumberDebugInfo).map(t -> (ControlTokenConsumer) t).collect(Collectors.toList())) {
            if (t.controlComingFrom.size() == 1 && t.controlFlowsTo.size() == 1 && t.controlFlowsTo.keySet().iterator().next().edgeType() == EdgeType.FORWARD) {
                if (t.nodeType == NodeType.FrameDebugInfo) {
                    ((FrameDebugInfo) t).deleteFromControlFlow();
                    changed = true;
                }
                if (t.nodeType == NodeType.LineNumberDebugInfo) {
                    ((LineNumberDebugInfo) t).deleteFromControlFlow();
                    changed = true;
                }
            }
        }

        return changed;
    }
}
