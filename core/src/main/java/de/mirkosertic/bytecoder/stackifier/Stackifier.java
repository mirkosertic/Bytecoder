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
package de.mirkosertic.bytecoder.stackifier;

import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraphRegionNodeTopologicOrder;
import de.mirkosertic.bytecoder.ssa.EdgeType;
import de.mirkosertic.bytecoder.ssa.RegionNode;

import java.util.List;
import java.util.Map;

public class Stackifier {

    public StructuredControlFlow<RegionNode> stackify(final ControlFlowGraph controlFlowGraph) {
        final List<RegionNode> sorted = new ControlFlowGraphRegionNodeTopologicOrder(controlFlowGraph).getNodesInOrder();
        final StructuredControlFlowBuilder<RegionNode> builder = new StructuredControlFlowBuilder<>(sorted);
        for (final RegionNode node : sorted) {
            for (final Map.Entry<RegionNode.Edge, RegionNode> succ : node.getSuccessors().entrySet()) {
                switch (succ.getKey().getType()) {
                    case forward:
                        if (sorted.contains(succ.getValue())) {
                            builder.add(EdgeType.forward, node, succ.getValue());
                        }
                        break;
                    case back:
                        if (sorted.contains(succ.getValue())) {
                            builder.add(EdgeType.back, node, succ.getValue());
                        }
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }
        }
        return builder.build();
    }
}
