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

public class Loop extends LabeledContainer implements WasmExpression {

    Loop(final String label, final Container parent) {
        super(parent, label);
    }

    @Override
    public void writeTo(final TextWriter textWriter, final ExportContext context) throws IOException {
        textWriter.opening();
        textWriter.write("loop");
        textWriter.space();
        textWriter.writeLabel(getLabel());
        if (hasChildren()) {
            textWriter.newLine();
            for (final WasmValue child : getChildren()) {
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
        codeWriter.writeByte((byte) 0x03);
        PrimitiveType.empty_block.writeTo(codeWriter);
        for (final WasmExpression e : getChildren()) {
            e.writeTo(codeWriter, context.subWith(this));
        }
        codeWriter.writeByte((byte) 0x0b);
    }
}
