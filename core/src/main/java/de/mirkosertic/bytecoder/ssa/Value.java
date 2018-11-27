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

import de.mirkosertic.bytecoder.graph.Node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class Value extends Node {

    protected Value() {
    }

    protected void receivesDataFrom(final Value aOtherValue) {
        aOtherValue.addEdgeTo(new DataFlowEdgeType(), this);
    }

    protected void receivesDataFrom(final List<Value> aValues) {
        for (final Value aValue : aValues) {
            receivesDataFrom(aValue);
        }
    }

    protected void receivesDataFrom(final Value... aValues) {
        for (final Value aValue : aValues) {
            receivesDataFrom(aValue);
        }
    }

    public <T extends Value> List<T> incomingDataFlows() {
        return incomingEdges(DataFlowEdgeType.filter()).map(t -> (T) t.sourceNode()).collect(Collectors.toList());
    }

    public <T extends Value> List<T> incomingDataFlowsRecursive() {
        return incomingDataFlowsRecursive(new ArrayList<>());
    }

    private <T extends Value> List<T> incomingDataFlowsRecursive(final List<T> aValues) {
        if (!aValues.contains(this)) {
            aValues.add((T) this);
            for (final Value theValue : incomingDataFlows()) {
                theValue.incomingDataFlowsRecursive(aValues);
            }
        }
        return aValues;
    }

    public boolean isTrulyFunctional() {
        return isTrulyFunctional(new HashSet<>());
    }

    private boolean isTrulyFunctional(final Set<Value> aVisited) {
        if (aVisited.add(this)) {
            for (final Value theIncoming : incomingDataFlows()) {
                if (!theIncoming.isTrulyFunctional(aVisited)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void replaceIncomingDataEdge(final Value aOldValue, final Value aNewValue) {
        incomingEdges(DataFlowEdgeType.filter()).forEach(aEdge -> {
            if (aEdge.sourceNode() == aOldValue) {
                aEdge.newSourceIs(aNewValue);
            }
        });
    }

    public void replaceIncomingDataEdgeRecursive(final Value aOldValue, final Value aNewValue) {
        for (final Value theValue : incomingDataFlowsRecursive()) {
            theValue.replaceIncomingDataEdge(aOldValue, aNewValue);
        }
    }

    public void routeIncomingDataFlowsTo(final Value aNewExpression) {
        incomingEdges(DataFlowEdgeType.filter()).forEach(aEdge -> {
            aEdge.newTargetId(aNewExpression);
            aNewExpression.addIncomingEdge(aEdge);
        });
    }

    public abstract TypeRef resolveType();
}