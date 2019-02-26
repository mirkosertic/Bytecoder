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

public class Try extends LabeledContainer implements WASMExpression {

    public final Catch catchBlock;

    Try(final Container parent, final String label, final Expression expression) {
        super(parent, label, expression);
        catchBlock = new Catch(this);
    }

    @Override
    public void writeTo(final TextWriter textWriter, final ExportContext context) throws IOException {
        textWriter.opening();
        textWriter.write("try");
        textWriter.space();
        textWriter.writeLabel(getLabel());
        textWriter.newLine();

        for (final WASMExpression e : getChildren()) {
            e.writeTo(textWriter, context.subWith(this));
        }

        catchBlock.writeTo(textWriter, context);
        textWriter.closing();
        textWriter.newLine();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer codeWriter, final ExportContext context) throws IOException {
        codeWriter.registerDebugInformationFor(expression);
        codeWriter.writeByte((byte) 0x06);
        PrimitiveType.empty_pseudo_block.writeTo(codeWriter);
        for (final WASMExpression e : getChildren()) {
            e.writeTo(codeWriter, context.subWith(this));
        }
        catchBlock.writeTo(codeWriter, context);
        codeWriter.writeByte((byte) 0x0b);
    }
}
