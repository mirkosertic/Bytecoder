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

import de.mirkosertic.bytecoder.ssa.EdgeType;

public class JumpArrow {

    private final EdgeType edgeType;
    private final int head;
    private final int tail;
    private int newTail;

    public JumpArrow(final EdgeType edgeType, final int tail, final int head) {
        this.edgeType = edgeType;
        this.head = head;
        this.tail = tail;
        this.newTail = tail;
    }

    public void setNewTail(final int newTail) {
        this.newTail = newTail;
    }

    public EdgeType getEdgeType() {
        return edgeType;
    }

    public int getHead() {
        return head;
    }

    public int getTail() {
        return tail;
    }

    public int getNewTail() {
        return newTail;
    }
}