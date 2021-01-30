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

import java.util.ArrayList;
import java.util.List;

public class StructuredControlFlowBuilder<T> {

    private final List<JumpArrow<T>> knownJumpArrows;
    private final List<T> nodesInOrder;

    public StructuredControlFlowBuilder(final List<T> nodesInOrder) {
        this.nodesInOrder = nodesInOrder;
        this.knownJumpArrows = new ArrayList<>();
    }

    public void add(final ControlFlowEdgeType edgeType, final T source, final T destination) {
        knownJumpArrows.add(new JumpArrow<>(edgeType, source, destination));
    }

    public StructuredControlFlow<T> build() throws HeadToHeadControlFlowException {
        final StructuredControlFlow<T> stack = new StructuredControlFlow<>(knownJumpArrows, nodesInOrder);
        stack.stackify();
        return stack;
    }
}
