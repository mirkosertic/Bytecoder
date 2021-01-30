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

public abstract class UnaryExpression implements WASMExpression {

    private final WASMValue value;
    private final String textCode;
    private final byte binaryCode;
    private final Expression expression;

    protected UnaryExpression(final WASMValue value, final String textCode, final byte binaryCode, final Expression expression) {
        this.value = value;
        this.textCode = textCode;
        this.binaryCode = binaryCode;
        this.expression = expression;
    }

    @Override
    public void writeTo(final TextWriter textWriter, final ExportContext context) throws IOException {
        textWriter.opening();
        textWriter.write(textCode);
        textWriter.space();
        value.writeTo(textWriter, context);
        textWriter.closing();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer codeWriter, final ExportContext context) throws IOException {
        value.writeTo(codeWriter, context);
        codeWriter.registerDebugInformationFor(expression);
        codeWriter.writeByte(binaryCode);
    }
}
