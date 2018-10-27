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

public class Iff extends Container implements Expression {

    private final Value condition;

    Iff(final Container parent, final Value condition) {
        super(parent);
        this.condition = condition;
    }

    @Override
    public void writeTo(final TextWriter textWriter, final ExportContext context) throws IOException {
        textWriter.opening();
        textWriter.write("if");
        textWriter.newLine();
        condition.writeTo(textWriter, context);
        if (hasChildren()) {
            for (final Value child : getChildren()) {
                textWriter.newLine();
                child.writeTo(textWriter, context);
            }
            textWriter.closing();
        } else {
            textWriter.closing();
        }
    }

    @Override
    public void writeTo(final BinaryWriter.Writer codeWriter, final ExportContext context) throws IOException {
        condition.writeTo(codeWriter, context);
        codeWriter.writeByte((byte) 0x04);
        PrimitiveType.empty_pseudo_block.writeTo(codeWriter);
        for (final Expression e : getChildren()) {
            e.writeTo(codeWriter, context.subWith(this));
        }
        codeWriter.writeByte((byte) 0x0b);

    }
}