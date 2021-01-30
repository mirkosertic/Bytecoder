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
package de.mirkosertic.bytecoder.stackifier;

import de.mirkosertic.bytecoder.ssa.ControlFlowEdgeType;

public class JumpArrow<T> {

    private final ControlFlowEdgeType edgeType;
    private final T head;
    private final T tail;
    private T newTail;

    public JumpArrow(final ControlFlowEdgeType edgeType, final T tail, final T head) {
        this.edgeType = edgeType;
        this.head = head;
        this.tail = tail;
        this.newTail = tail;
    }

    void setNewTail(final T newTail) {
        this.newTail = newTail;
    }

    public ControlFlowEdgeType getEdgeType() {
        return edgeType;
    }

    public T getHead() {
        return head;
    }

    public T getTail() {
        return tail;
    }

    public T getNewTail() {
        return newTail;
    }
}