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

import de.mirkosertic.bytecoder.graph.Edge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

public class ControlFlowGraphSCC {

    private final List<List<RegionNode>> connectedComponentList;
    private int time;
    private final Map<RegionNode, Integer> lowLink;
    private final Set<RegionNode> visited;
    private final Stack<RegionNode> stack;
    private final List<RegionNode> nodesInOrder;

    public ControlFlowGraphSCC(final ControlFlowGraph g) {
        time = 0;
        connectedComponentList = new ArrayList<>();
        nodesInOrder = new ArrayList<>();
        lowLink = new HashMap<>();
        visited = new HashSet<>();
        stack = new Stack<>();
        for (final RegionNode node : g.dominators().getPreOrder()) {
            if (!visited.contains(node)) {
                dfs(node);
            }
        }

        for (int i = connectedComponentList.size()-1 ; i>=0; i--) {
            final List<RegionNode> l = connectedComponentList.get(i);
            for (int j = l.size() - 1; j>=0; j--) {
                nodesInOrder.add(l.get(j));
            }
        }
    }

    private void dfs(final RegionNode vertex) {
        lowLink.put(vertex, time++);
        visited.add(vertex);
        stack.push(vertex);
        boolean isComponentRoot = true;

        final List<RegionNode> successors = vertex.outgoingEdges()
                .filter(t -> t.targetNode().getType() == RegionNode.BlockType.NORMAL)
                .map(Edge::targetNode)
                .sorted((o1, o2) -> Integer.compare(o2.getStartAddress().getAddress(), o1.getStartAddress().getAddress()))
                .collect(Collectors.toList());

        for(final RegionNode v : successors) {

            if(!visited.contains(v)) {
                dfs(v);
            }

            // we have a back edge
            if(lowLink.get(vertex) > lowLink.get(v)) {
                lowLink.put(vertex, lowLink.get(v));
                isComponentRoot = false;
            }
        }

        // only for the root SCC node
        if(isComponentRoot) {

            final List<RegionNode> component = new ArrayList<>();

            while(true) {
                final RegionNode actualVertex = stack.pop();
                component.add(actualVertex);
                lowLink.put(actualVertex, Integer.MAX_VALUE);

                // Run util it hits the root SCC node
                if (actualVertex == vertex)
                    break;
            }

            connectedComponentList.add(component);
        }
    }

    public List<List<RegionNode>> getConnectedComponentList() {
        return connectedComponentList;
    }

    public List<RegionNode> getNodesInOrder() {
        return nodesInOrder;
    }
}
