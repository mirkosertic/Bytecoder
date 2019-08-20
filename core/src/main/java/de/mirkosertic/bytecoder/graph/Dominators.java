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
package de.mirkosertic.bytecoder.graph;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Compute Dominators of a graph, following:
 * A Simple, Fast Dominance Algorithm
 * (Cooper, Keith  D.  and Harvey, Timothy  J.  and Kennedy, Ken).
 */
public class Dominators<T extends Node<? extends Node, ? extends EdgeType>> {

    private final List<T> preOrder;
    private final Map<T, T> idom;

    public Dominators(final T rootNode, final Comparator<T> comparator) {
        this(rootNode, comparator, t -> true);
    }

    public Dominators(final T rootNode, final Comparator<T> comparator, final Predicate<Edge<EdgeType, T>> edgeFilter) {
        preOrder = new GraphDFSOrder<>(rootNode, comparator, edgeFilter).getNodesInOrder();
        idom = new HashMap<>();
        computeDominators();
    }

    public List<T> getPreOrder() {
        return preOrder;
    }

    private void computeDominators() {
        final T firstElement = preOrder.get(0);
        idom.put(firstElement, firstElement);

/*        if (!(firstElement.incomingEdges().map(t -> t.sourceNode()).count() == 0)) {
            throw new AssertionError("The entry of the flow graph is not allowed to have incoming edges");
        }*/

        boolean changed;
        do {
            changed = false;
            for (final T v : preOrder) {
                if (v.equals(firstElement))
                    continue;
                final T oldIdom = getIDom(v);
                T newIdom = null;
                final List<T> incomingNodes = v.incomingEdges().map(t -> (T) t.sourceNode()).collect(Collectors.toList());
                for (final T pre : incomingNodes) {
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

    public T getIDom(final T node) {
        return idom.get(node);
    }

    private T intersectIDoms(T v1, T v2) {
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
    public boolean dominates(final T dominator, final T dominated) {
        if(dominator.equals(dominated)) {
            return true; // Domination is reflexive ;)
        }
        T dom = getIDom(dominated);
        // as long as dominated >= dominator
        while(dom != null && preOrder.indexOf(dom) >= preOrder.indexOf(dominator) && ! dom.equals(dominator)) {
            dom = getIDom(dom);
        }
        return dominator.equals(dom);
    }

    public Set<T> getStrictDominators(final T n) {
        final Set<T> strictDoms = new HashSet<>();
        T dominated = n;
        T iDom = getIDom(n);
        while(iDom != dominated) {
            strictDoms.add(iDom);
            dominated = iDom;
            iDom = getIDom(dominated);
        }
        return strictDoms;
    }

    public Set<T> domSetOf(final T n) {
        final Set<T> theDomSet = new HashSet<>();
        addToDomSet(n, theDomSet);
        return theDomSet;
    }

    private void addToDomSet(final T n, final Set<T> domset) {
        domset.add(n);
        for (final Map.Entry<T, T> idomEntry : idom.entrySet()) {
            if (idomEntry.getValue() == n && preOrder.indexOf(idomEntry.getKey()) > preOrder.indexOf(n)) {
                addToDomSet(idomEntry.getKey(), domset);
            }
        }
    }
}
