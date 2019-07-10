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
import de.mirkosertic.bytecoder.ssa.RegionNode;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Stackifier {

    public Sequence stackify(final ControlFlowGraph controlFlowGraph) {
        final Sequence sequence = new Sequence();
        final BlockStack currentStack = new BlockStack();
        sequence.append(currentStack);
        stackify(controlFlowGraph, sequence, currentStack, controlFlowGraph.startNode());
        return sequence;
    }

    private void stackify(
            final ControlFlowGraph controlFlowGraph, final Sequence sequence, final BlockStack currentStack, final RegionNode currentNode) {
        final Map<RegionNode.Edge, RegionNode> theSuccessors = currentNode.getSuccessors();
        final List<RegionNode> theImmediateDominated = theSuccessors.entrySet().stream()
                .filter(t -> t.getKey().getType() == RegionNode.EdgeType.NORMAL)
                .filter(t -> t.getValue().isStrictlyDominatedBy(currentNode))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        currentStack.append(new RegionNodeElement(currentNode));
        if (theImmediateDominated.size() == 1) {
            stackify(controlFlowGraph, sequence, currentStack, theImmediateDominated.get(0));
        }
    }
}
