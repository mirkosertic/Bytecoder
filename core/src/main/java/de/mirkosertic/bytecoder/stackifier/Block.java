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

import de.mirkosertic.bytecoder.ssa.Label;

import java.util.Objects;

public class Block<T> {
    private final Label label;
    final JumpArrow<T> arrow;

    public Block(final Label label, final JumpArrow<T> arrow) {
        this.label = label;
        this.arrow = arrow;
    }

    public Label getLabel() {
        return label;
    }

    public JumpArrow<T> getArrow() {
        return arrow;
    }

    public T getEnding() {
        switch (arrow.getEdgeType()) {
            case forward:
                return arrow.getHead();
            case back:
                return arrow.getNewTail();
        }
        throw new IllegalStateException();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Block<?> block = (Block<?>) o;
        return label.equals(block.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label);
    }
}
