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

import java.io.IOException;

public class Iff extends LabeledContainer implements WASMExpression {

    private final WASMValue condition;

    Iff(final Container parent, final String label, final WASMValue condition) {
        super(parent, label);
        this.condition = condition;
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
            for (final WASMValue child : getChildren()) {
                child.writeTo(textWriter, context);
            }
            textWriter.closing();
        } else {
            textWriter.closing();
        }
        textWriter.closing();
        textWriter.newLine();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer codeWriter, final ExportContext context) throws IOException {
        condition.writeTo(codeWriter, context);
        codeWriter.writeByte((byte) 0x04);
        PrimitiveType.empty_pseudo_block.writeTo(codeWriter);
        for (final WASMExpression e : getChildren()) {
            e.writeTo(codeWriter, context.subWith(this));
        }
        codeWriter.writeByte((byte) 0x0b);

    }
}