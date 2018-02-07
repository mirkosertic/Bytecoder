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

import java.util.List;
import java.util.stream.Collectors;

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

    public abstract TypeRef resolveType();
}