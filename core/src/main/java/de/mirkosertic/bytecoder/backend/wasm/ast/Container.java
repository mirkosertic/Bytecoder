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
package de.mirkosertic.bytecoder.backend.wasm.ast;

import java.util.ArrayList;
import java.util.List;

public abstract class Container {

    public final Expressions flow;

    private final Container parent;
    private final List<WASMExpression> children;

    protected Container(final Container parent) {
        this.parent = parent;
        this.children = new ArrayList<>();
        this.flow = new Expressions(this);
    }

    protected Container() {
        this.parent = null;
        this.children = new ArrayList<>();
        this.flow = new Expressions(this);
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public List<WASMExpression> getChildren() {
        return children;
    }

    public void addChild(final WASMExpression e) {
        children.add(e);
    }

    public int relativeDepthTo(final LabeledContainer outerBlock) {
        return relativeDepthTo(outerBlock, 0);
    }

    public int relativeDepthTo(final LabeledContainer outerBlock, final int offset) {
        if (this == outerBlock) {
            return offset;
        }
        if (parent != null) {
            return parent.relativeDepthTo(outerBlock, offset + 1);
        }
        throw new IllegalArgumentException("Cannot find block " + outerBlock.getLabel());
    }

    public LabeledContainer findByLabelInHierarchy(final String aLabel) {
        if (parent != null) {
            return parent.findByLabelInHierarchy(aLabel);
        }
        throw new IllegalArgumentException("No such parent container : " + aLabel);
    }

    public Container end() {
        return parent;
    }
}
