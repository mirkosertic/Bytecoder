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

import java.io.IOException;

public class Block extends LabeledContainer implements WASMExpression {

    private final PrimitiveType blockType;

    Block(final String label, final Container parent, final Expression expression, final PrimitiveType blockType) {
        super(parent, label, expression);
        this.blockType = blockType;
    }

    @Override
    public void writeTo(final TextWriter textWriter, final ExportContext context) throws IOException {
        textWriter.opening();
        textWriter.write("block");
        textWriter.space();
        textWriter.writeLabel(getLabel());
        if (blockType != PrimitiveType.empty_pseudo_block) {
            textWriter.space();
            textWriter.opening();
            textWriter.write("result");
            textWriter.space();
            blockType.writeTo(textWriter);
            textWriter.closing();
        }
        if (hasChildren()) {
            textWriter.newLine();
            for (final WASMValue child : getChildren()) {
                child.writeTo(textWriter, context);
            }
            textWriter.closing();
        } else {
            textWriter.closing();
        }
        textWriter.newLine();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer codeWriter, final ExportContext context) throws IOException {
        codeWriter.registerDebugInformationFor(expression);
        codeWriter.writeByte((byte) 0x02);
        blockType.writeTo(codeWriter);
        for (final WASMExpression e : getChildren()) {
            e.writeTo(codeWriter, context.subWith(this));
        }
        codeWriter.writeByte((byte) 0x0b);
    }
}