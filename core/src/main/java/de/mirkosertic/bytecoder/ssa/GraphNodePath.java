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

    private final List<RegionNode> nodes;

    protected GraphNodePath(final List<RegionNode> aNodes) {
        nodes = aNodes;
    }

    public GraphNodePath() {
        this(new ArrayList<>());
    }

    public GraphNodePath(final GraphNodePath aOther) {
        this(new ArrayList<>(aOther.nodes));
    }

    public void addToPath(final RegionNode aNode) {
        if (!nodes.contains(aNode)) {
            nodes.add(aNode);
        }
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    public RegionNode lastElement() {
        return nodes.get(nodes.size() - 1);
    }

    public boolean contains(final RegionNode aNode) {
        return nodes.contains(aNode);
    }
}
