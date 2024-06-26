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
package de.mirkosertic.bytecoder.core.backend.wasm.ast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Iff extends LabeledContainer implements WasmExpression {

    public final Expressions falseFlow;

    private final WasmValue condition;
    private final List<WasmExpression> falseChildren;

    Iff(final Container parent, final String label, final WasmValue condition) {
        super(parent, label);
        this.condition = condition;

        this.falseChildren = new ArrayList<>();
        this.falseFlow = new Expressions(new Container() {
            @Override
            public boolean hasChildren() {
                return !falseChildren.isEmpty();
            }

            @Override
            public List<WasmExpression> getChildren() {
                return falseChildren;
            }

            @Override
            public void addChild(final WasmExpression e) {
                falseChildren.add(e);
            }

            @Override
            public int relativeDepthTo(final LabeledContainer outerBlock) {
                return Iff.super.relativeDepthTo(outerBlock);
            }

            @Override
            public int relativeDepthTo(final LabeledContainer outerBlock, final int offset) {
                return Iff.super.relativeDepthTo(outerBlock, offset);
            }

            @Override
            public LabeledContainer findByLabelInHierarchy(final String aLabel) {
                return Iff.super.findByLabelInHierarchy(aLabel);
            }

            @Override
            public Container end() {
                return Iff.super.end();
            }
        });
    }

    @Override
    public void writeTo(final TextWriter textWriter, final ExportContext context) throws IOException {
        textWriter.opening();
        textWriter.write("if");
        textWriter.space();
        textWriter.writeLabel(getLabel());
        textWriter.space();
        condition.writeTo(textWriter, context);
        textWriter.newLine();

        textWriter.opening();
        textWriter.write("then");
        textWriter.newLine();
        if (hasChildren()) {
            for (final WasmValue child : getChildren()) {
                child.writeTo(textWriter, context);
            }
            textWriter.closing();
        } else {
            textWriter.closing();
        }

        if (!falseChildren.isEmpty()) {
            textWriter.opening();
            textWriter.write("else");
            textWriter.newLine();
            for (final WasmValue child : falseChildren) {
                child.writeTo(textWriter, context);
            }
            textWriter.closing();
        }

        textWriter.closing();
        textWriter.newLine();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer codeWriter, final ExportContext context) throws IOException {
        condition.writeTo(codeWriter, context);

        codeWriter.writeByte((byte) 0x04);
        PrimitiveType.empty_block.writeTo(codeWriter);
        for (final WasmExpression e : getChildren()) {
            e.writeTo(codeWriter, context.subWith(this));
        }
        if (!falseChildren.isEmpty()) {
            codeWriter.writeByte((byte) 0x05);
            for (final WasmExpression e : falseChildren) {
                e.writeTo(codeWriter, context.subWith(this));
            }
        }
        codeWriter.writeByte((byte) 0x0b);
    }
}
