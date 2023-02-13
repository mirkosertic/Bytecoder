/*
 * Copyright 2023 Mirko Sertic
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
package de.mirkosertic.bytecoder.asm.backend.sequencer;

import de.mirkosertic.bytecoder.asm.ir.ControlTokenConsumer;
import de.mirkosertic.bytecoder.asm.ir.EdgeType;
import de.mirkosertic.bytecoder.asm.ir.Graph;
import de.mirkosertic.bytecoder.asm.ir.Node;
import de.mirkosertic.bytecoder.asm.ir.Projection;
import de.mirkosertic.bytecoder.asm.ir.Region;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DominatorTree {

    private final Graph graph;
    private final List<ControlTokenConsumer> preOrder;
    private final Map<ControlTokenConsumer, ControlTokenConsumer> idom;

    private final List<ControlTokenConsumer> rpo;

    public DominatorTree(final Graph graph) {
        this.graph = graph;
        final Region start = graph.regionByLabel(Graph.START_REGION_NAME);
        preOrder = new DFS(start).getTopoligicalOrder();
        idom = new HashMap<>();
        rpo = new ArrayList<>();
        computeDominators();
        computeRPO(start);
    }

    private void computeRPO(final ControlTokenConsumer consumer) {
        final List<ControlTokenConsumer> finished = new ArrayList<>();
        final Set<ControlTokenConsumer> visited = new HashSet<>();
        computeRPO(consumer, finished, visited);

        Collections.reverse(finished);
        rpo.addAll(finished);
    }

    private void computeRPO(final ControlTokenConsumer current, final List<ControlTokenConsumer> finished, final Set<ControlTokenConsumer> visited) {
        if (visited.add(current)) {
            for (final Map.Entry<Projection, ControlTokenConsumer> entry : current.controlFlowsTo.entrySet()) {
                if (entry.getKey().edgeType() == EdgeType.FORWARD) {
                    computeRPO(entry.getValue(), finished, visited);
                }
            }
            finished.add(current);
        }
    }

    public List<ControlTokenConsumer> getPreOrder() {
        return preOrder;
    }

    public List<ControlTokenConsumer> getRpo() {
        return rpo;
    }

    private void computeDominators() {
        final ControlTokenConsumer firstElement = preOrder.get(0);
        idom.put(firstElement, firstElement);

        boolean changed;
        do {
            changed = false;
            for (final ControlTokenConsumer v : preOrder) {
                if (v.equals(firstElement))
                    continue;
                final ControlTokenConsumer oldIdom = getIDom(v);
                ControlTokenConsumer newIdom = null;
                for (final ControlTokenConsumer pre : v.controlComingFrom) {
                    if (getIDom(pre) == null)
                        /* not yet analyzed */ continue;
                    if (newIdom == null) {
                        /* If we only have one (defined) predecessor pre, IDom(v) = pre */
                        newIdom = pre;
                    } else {
                        /* compute the intersection of all defined predecessors of v */
                        newIdom = intersectIDoms(pre, newIdom);
                    }
                }
                if (newIdom == null) {
                    throw new AssertionError("newIDom == null !, for " + v);
                }
                if (!newIdom.equals(oldIdom)) {
                    changed = true;
                    this.idom.put(v, newIdom);
                }
            }
        } while (changed);
    }

    public ControlTokenConsumer getIDom(final ControlTokenConsumer node) {
        return idom.get(node);
    }

    private ControlTokenConsumer intersectIDoms(ControlTokenConsumer v1, ControlTokenConsumer v2) {
        while (v1 != v2) {
            if (preOrder.indexOf(v1) < preOrder.indexOf(v2)) {
                v2 = getIDom(v2);
            } else {
                v1 = getIDom(v1);
            }
        }
        return v1;
    }

    /**
     * Check wheter a node dominates another one.
     * @param dominator
     * @param dominated
     * @return true, if <code>dominator</code> dominates <code>dominated</code> w.r.t to the entry node
     */
    public boolean dominates(final ControlTokenConsumer dominator, final ControlTokenConsumer dominated) {
        if(dominator.equals(dominated)) {
            return true; // Domination is reflexive ;)
        }
        ControlTokenConsumer dom = getIDom(dominated);
        // as long as dominated >= dominator
        while(dom != null && preOrder.indexOf(dom) >= preOrder.indexOf(dominator) && ! dom.equals(dominator)) {
            dom = getIDom(dom);
        }
        return dominator.equals(dom);
    }

    public Set<ControlTokenConsumer> getStrictDominators(final ControlTokenConsumer n) {
        final Set<ControlTokenConsumer> strictDoms = new HashSet<>();
        ControlTokenConsumer dominated = n;
        ControlTokenConsumer iDom = getIDom(n);
        while(iDom != dominated) {
            strictDoms.add(iDom);
            dominated = iDom;
            iDom = getIDom(dominated);
        }
        return strictDoms;
    }

    public Set<ControlTokenConsumer> immediatelyDominatedNodesOf(final ControlTokenConsumer n) {
        final Set<ControlTokenConsumer> result = new HashSet<>();
        for (final Map.Entry<ControlTokenConsumer, ControlTokenConsumer> entry : idom.entrySet()) {
            if (entry.getValue() == n) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public Set<ControlTokenConsumer> domSetOf(final ControlTokenConsumer n) {
        final Set<ControlTokenConsumer> theDomSet = new HashSet<>();
        addToDomSet(n, theDomSet);
        return theDomSet;
    }

    private void addToDomSet(final ControlTokenConsumer n, final Set<ControlTokenConsumer> domset) {
        domset.add(n);
        for (final Map.Entry<ControlTokenConsumer, ControlTokenConsumer> idomEntry : idom.entrySet()) {
            if (idomEntry.getValue() == n && preOrder.indexOf(idomEntry.getKey()) > preOrder.indexOf(n)) {
                addToDomSet(idomEntry.getKey(), domset);
            }
        }
    }

    public void writeDebugTo(final OutputStream fileOutputStream) {
        writeDebugTo(fileOutputStream, null, null);
    }

    public void writeDebugTo(final OutputStream fileOutputStream, final ControlTokenConsumer sourceHighlight, final Set<ControlTokenConsumer> destHighlight) {
        final PrintWriter pw = new PrintWriter(fileOutputStream);
        final List<Node> graphNodes = graph.nodes();
        pw.println("digraph debugoutput {");
        for (final ControlTokenConsumer n : preOrder) {
            final String label = graphNodes.indexOf(n) + " " + n.getClass().getSimpleName() + " " + n.additionalDebugInfo() + " Order : " + rpo.indexOf(n);

            pw.print(" node_" + graphNodes.indexOf(n) + "[label=\"" + label + "\" ");
            if (n == sourceHighlight) {
                pw.print("shape=\"box\" fillcolor=\"green\" style=\"filled\"");
            } else {
                if (destHighlight != null && destHighlight.contains(n)) {
                    pw.print("shape=\"box\" fillcolor=\"blue\" style=\"filled\"");
                } else {
                    pw.print("shape=\"box\" fillcolor=\"orangered\" style=\"filled\"");
                }
            }
            pw.println("];");

            final ControlTokenConsumer id = idom.get(n);
            if (id != n) {
                pw.print(" node_" + graphNodes.indexOf(n) + " -> node_" + graphNodes.indexOf(id) + "[dir=\"forward\"");
                pw.print(" color=\"black\" penwidth=\"2\"");
                pw.println("];");
            }

            for (final Map.Entry<Projection, ControlTokenConsumer> entry : n.controlFlowsTo.entrySet()) {
                pw.print(" node_" + graphNodes.indexOf(n) + " -> node_" + graphNodes.indexOf(entry.getValue()) + "[dir=\"forward\"");
                if (entry.getKey().isControlFlow()) {
                    pw.print(" color=\"red\" penwidth=\"1\"");
                } else {
                    pw.print(" color=\"azure4\" penwidth=\"1\"");
                }
                pw.print(" label=\"");
                pw.print(entry.getKey().additionalDebugInfo());
                pw.print("\"");
                if (entry.getKey().edgeType() == EdgeType.BACK) {
                    pw.print(" style=\"dashed\"");
                }
                pw.println("];");
            }
        }
        pw.println("}");
        pw.flush();
    }
}
