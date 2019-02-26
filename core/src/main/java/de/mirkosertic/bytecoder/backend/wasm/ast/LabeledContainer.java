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

import de.mirkosertic.bytecoder.ssa.Expression;

public class LabeledContainer extends Container {

    private final String label;
    protected final Expression expression;

    public LabeledContainer(final Container parent, final String label, final Expression expression) {
        super(parent);
        this.label = label;
        this.expression = expression;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public LabeledContainer findByLabelInHierarchy(final String aLabel) {
        if (label.equalsIgnoreCase(aLabel)) {
            return this;
        }
        return super.findByLabelInHierarchy(aLabel);
    }
}