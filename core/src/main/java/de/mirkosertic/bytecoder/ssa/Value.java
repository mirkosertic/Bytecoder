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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import de.mirkosertic.bytecoder.graph.Edge;
import de.mirkosertic.bytecoder.graph.Node;

public abstract class Value extends Node {

    protected Value() {
    }

    protected void receivesDataFrom(Value aOtherValue) {
        aOtherValue.addEdgeTo(new DataFlowEdgeType(), this);
    }

    protected void receivesDataFrom(List<Value> aValues) {
        for (Value aValue : aValues) {
            receivesDataFrom(aValue);
        }
    }

    protected void receivesDataFrom(Value... aValues) {
        for (Value aValue : aValues) {
            receivesDataFrom(aValue);
        }
    }

    public <T extends Value> List<T> incomingDataFlows() {
        return incomingEdges(DataFlowEdgeType.filter()).map(t -> (T) t.sourceNode()).collect(Collectors.toList());
    }

    public List<Edge> incomingDataFlowEdges() {
        return incomingEdges(DataFlowEdgeType.filter()).collect(Collectors.toList());
    }

    public List<Edge> incomingDataFlowEdgesRecursive() {
        return incomingDataFlowEdgesRecursive(new HashSet<>());
    }

    private List<Edge> incomingDataFlowEdgesRecursive(Set<Value> aAlreadyVisited) {
        List<Edge> theResult = new ArrayList<>();
        if (aAlreadyVisited.add(this)) {
            for (Edge theEdge : incomingDataFlowEdges()) {
                theResult.add(theEdge);
                Value theSource = (Value) theEdge.sourceNode();
                theResult.addAll(theSource.incomingDataFlowEdgesRecursive(aAlreadyVisited));
            }
        }
        return theResult;
    }

    public void replaceIncomingDataEdge(Value aOldValue, Value aNewValue) {
        incomingEdges(DataFlowEdgeType.filter()).forEach(aEdge -> {
            if (aEdge.sourceNode() == aOldValue) {
                aEdge.newSourceIs(aNewValue);
            }
        });
    }

    public void routeIncomingDataFlowsTo(Value aNewExpression) {
        incomingEdges(DataFlowEdgeType.filter()).forEach(aEdge -> {
            aEdge.newTargetId(aNewExpression);
            aNewExpression.addIncomingEdge(aEdge);
        });
    }

    public abstract TypeRef resolveType();
}