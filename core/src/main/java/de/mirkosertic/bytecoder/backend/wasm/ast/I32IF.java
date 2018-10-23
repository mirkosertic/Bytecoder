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

public class I32IF extends Container implements Expression {

    private final I32Condition condition;

    I32IF(final Container parent, final I32Condition condition) {
        super(parent);
        this.condition = condition;
    }

    @Override
    public void writeTo(final TextWriter textWriter, final ExportableFunction exportableFunction) throws IOException {
        textWriter.opening();
        textWriter.write("if");
        textWriter.newLine();
        condition.writeTo(textWriter, exportableFunction);
        if (hasChildren()) {
            for (final Value child : getChildren()) {
                textWriter.newLine();
                child.writeTo(textWriter, exportableFunction);
            }
            textWriter.closing();
        } else {
            textWriter.closing();
        }
    }

    @Override
    public void writeTo(final BinaryWriter.Writer codeWriter, final Container owningContainer, final ExportableFunction exportableFunction) throws IOException {
        condition.writeTo(codeWriter, owningContainer, exportableFunction);
        codeWriter.writeByte((byte) 0x04);
        PrimitiveType.empty_pseudo_block.writeTo(codeWriter);
        for (final Expression e : getChildren()) {
            e.writeTo(codeWriter, this, exportableFunction);
        }
        codeWriter.writeByte((byte) 0x0b);

    }
}