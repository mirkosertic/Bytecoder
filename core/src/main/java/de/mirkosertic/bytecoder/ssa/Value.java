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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.graph.EdgeType;
import de.mirkosertic.bytecoder.graph.Node;

public abstract class Value extends Node<Node, EdgeType> {

    private List<? extends Value> cachedIncomingFlows;

    protected Value() {
    }

    private void resetCaches() {
        cachedIncomingFlows = null;
    }

    protected void receivesDataFrom(final Value aOtherValue) {
        aOtherValue.addEdgeTo(DataFlowEdgeType.instance, this);
        resetCaches();
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
        if (cachedIncomingFlows == null) {
            cachedIncomingFlows = incomingEdges(DataFlowEdgeType.filter()).map(t -> (T) t.sourceNode()).collect(Collectors.toList());
        }
        return (List<T>) cachedIncomingFlows;
    }

    public void replaceIncomingDataEdge(final Value aOldValue, final Value aNewValue) {
        incomingEdges(DataFlowEdgeType.filter()).forEach(aEdge -> {
            if (aEdge.sourceNode() == aOldValue) {
                aEdge.newSourceIs(aNewValue);
            }
        });
        resetCaches();
    }

    public void routeIncomingDataFlowsTo(final Value aNewExpression) {
        incomingEdges(DataFlowEdgeType.filter()).forEach(aEdge -> {
            aEdge.newTargetId(aNewExpression);
            aNewExpression.addIncomingEdge(aEdge);
        });
        resetCaches();
    }

    public abstract TypeRef resolveType();

    public static TypeRef widestTypeOf(final Collection<Value> aValue, final BytecodeLinkerContext aLinkerContext) {
        if (aValue.size() == 1) {
            return aValue.iterator().next().resolveType();
        }
        final Set<TypeRef> theTypes = new HashSet<>();
        for (final Value v : aValue) {
            final TypeRef theType = v.resolveType();
            if (!(theType == TypeRef.Native.REFERENCE)) {
                theTypes.add(v.resolveType());
            }
        }
        if (theTypes.size() == 1) {
            return theTypes.iterator().next();
        }
        
        TypeRef.Native theCurrent = null;
        for (final Value theValue : aValue) {
            final TypeRef.Native theValueType = theValue.resolveType().resolve();
            if (theCurrent == null) {
                theCurrent = theValueType;
            } else {
                theCurrent = theCurrent.eventuallyPromoteTo(theValueType);
            }
        }
        return theCurrent;
    }
}