/*
 * Copyright 2017 Mirko Sertic
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

import java.util.ArrayList;
import java.util.List;

public class GraphNodePath {

    private final List<GraphNode> nodes;

    protected GraphNodePath(List<GraphNode> aNodes) {
        nodes = aNodes;
    }

    public GraphNodePath() {
        this(new ArrayList<>());
    }

    public GraphNodePath clone() {
        return new GraphNodePath(new ArrayList<>(nodes));
    }

    public boolean addToPath(GraphNode aNode) {
        if (!nodes.contains(aNode)) {
            nodes.add(aNode);
            return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    public GraphNode lastElement() {
        return nodes.get(nodes.size() - 1);
    }

    public List<GraphNode> nodes() {
        return nodes;
    }

    public boolean contains(GraphNode aNode) {
        return nodes.contains(aNode);
    }
}
