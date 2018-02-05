/*
 * Copyright 2018 Mirko Sertic
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
package de.mirkosertic.bytecoder.core;

import de.mirkosertic.bytecoder.graph.EdgeType;

import java.util.Objects;
import java.util.function.Predicate;

public class BytecodeLinkedClassEdgeType implements EdgeType {

    public static Predicate<EdgeType> filter() {
        return edgeType -> edgeType instanceof BytecodeLinkedClassEdgeType;
    };

    public static Predicate<EdgeType> filter(BytecodeObjectTypeRef aTypeRef) {
        return edgeType -> {
            if (!(edgeType instanceof BytecodeLinkedClassEdgeType)) {
                return false;
            }
            BytecodeLinkedClassEdgeType theType = (BytecodeLinkedClassEdgeType) edgeType;
            return Objects.equals(aTypeRef, theType.objectTypeRef);
        };
    };

    private final BytecodeObjectTypeRef objectTypeRef;

    public BytecodeLinkedClassEdgeType(BytecodeObjectTypeRef aObjectTypeRef) {
        objectTypeRef = aObjectTypeRef;
    }

    public BytecodeObjectTypeRef objectTypeRef() {
        return objectTypeRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BytecodeLinkedClassEdgeType that = (BytecodeLinkedClassEdgeType) o;
        return Objects.equals(objectTypeRef, that.objectTypeRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectTypeRef);
    }
}