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
package de.mirkosertic.bytecoder.ssa;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ControlFlowGraphRegionNodeTopologicOrder {

    private final List<RegionNode> nodes;
    private final ControlFlowGraph controlFlowGraph;

    public ControlFlowGraphRegionNodeTopologicOrder(final ControlFlowGraph graph) {
        this.nodes = new ArrayList<>();
        this.controlFlowGraph = graph;
        visit(graph.startNode());
        nodes.sort(Comparator.comparingInt(this::topo));
    }

    private void visit(final RegionNode regionNode) {
        if (regionNode.getType() == RegionNode.BlockType.NORMAL) {
            if (!nodes.contains(regionNode)) {
                nodes.add(regionNode);
                for (final Map.Entry<RegionNode.Edge, RegionNode> entry : regionNode.getSuccessors().entrySet()) {
                    if (entry.getKey().getType() == EdgeType.forward) {
                        visit(entry.getValue());
                    }
                }
            }
        }
    }

    private int topo(final RegionNode regionNode) {
        if (regionNode == controlFlowGraph.startNode()) {
            return 0;
        }
        int max = 0;
        RegionNode dominatedBy = null;
        for (final RegionNode pre : regionNode.getPredecessorsIgnoringBackEdges()) {
            if (regionNode.isStrictlyDominatedBy(pre)) {
                dominatedBy = pre;
            }
            max = Math.max(max, topo(pre));
        }
        if (dominatedBy != null && dominatedBy.getSuccessors().size() > 1) {
            final List<RegionNode> dominatedSuccessors = new ArrayList<>(dominatedBy.getSuccessors().values());
            dominatedSuccessors.sort(Comparator.comparingInt(o -> o.getStartAddress().getAddress()));
            return (max + 1) * 10000 * (dominatedSuccessors.indexOf(regionNode) + 1);
        }
        return max + 1;
    }

    public List<RegionNode> getNodesInOrder() {
        return nodes;
    }

    public void printDebug(final PrintStream ps) {
        System.out.println("Topologic order:");
        for (final RegionNode node : nodes) {
            ps.print("    ");
            ps.print(node.getStartAddress());
            ps.print(" -> ");
            ps.print(topo(node));
            ps.println();
        }
    }
}
