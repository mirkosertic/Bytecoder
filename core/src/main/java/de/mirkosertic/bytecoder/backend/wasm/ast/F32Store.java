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

public class F32Store implements WASMExpression {

    private final Alignment alignment;
    private final int offset;
    private final WASMValue ptr;
    private final WASMValue value;
    private final Expression expression;

    F32Store(final int offset, final WASMValue ptr, final WASMValue value, final Expression expression) {
        this(Alignment.FOUR, offset, ptr, value, expression);
    }

    F32Store(final Alignment alignment, final int offset, final WASMValue ptr, final WASMValue value, final Expression expression) {
        this.alignment = alignment;
        this.offset = offset;
        this.ptr = ptr;
        this.value = value;
        this.expression = expression;
    }

    @Override
    public void writeTo(final TextWriter textWriter, final ExportContext context) throws IOException {
        textWriter.opening();
        textWriter.write("f32.store");
        textWriter.space();
        textWriter.writeAttribute("offset", offset);
        textWriter.space();
        textWriter.writeAttribute("align", alignment.value());
        textWriter.space();
        ptr.writeTo(textWriter, context);
        textWriter.space();
        value.writeTo(textWriter, context);
        textWriter.closing();
        textWriter.newLine();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer codeWriter, final ExportContext context) throws IOException {
        ptr.writeTo(codeWriter, context);
        value.writeTo(codeWriter, context);
        codeWriter.registerDebugInformationFor(expression);
        codeWriter.writeByte((byte) 0x38);
        codeWriter.writeUnsignedLeb128(alignment.log2Value());
        codeWriter.writeUnsignedLeb128(offset);
    }
}
