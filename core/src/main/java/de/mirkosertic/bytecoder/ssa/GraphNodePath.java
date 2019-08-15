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
import java.util.Arrays;
import java.util.List;

public class GraphNodePath {

    private RegionNode[] nodes;
    private boolean regularFlow;

    protected GraphNodePath(final RegionNode[] aNodes) {
        nodes = aNodes;
    }

    public RegionNode lastElement() {
        return nodes[nodes.length - 1];
    }

    public boolean contains(final RegionNode aNode) {
        for (final RegionNode n : nodes) {
            if (n == aNode) {
                return true;
            }
        }
        return false;
    }

    public List<RegionNode> getNodes() {
        return Arrays.asList(nodes);
    }

    public boolean isRegularFlow() {
        for (final RegionNode node : nodes) {
            if (node.getType() == RegionNode.BlockType.EXCEPTION_HANDLER) {
                return false;
            }
        }
        return true;
    }

    public void replace(final RegionNode aNode, final RegionNode aTarget) {
        final List<RegionNode> list = new ArrayList<>(getNodes());
        final int p = list.indexOf(aNode);
        if (p>=0) {
            list.add(p, aTarget);
            list.remove(aNode);

            nodes = list.toArray(new RegionNode[0]);
        }
    }
}
