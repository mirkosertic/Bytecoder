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
import java.util.Optional;

public abstract class UnaryExpression implements Expression {

    private final Optional<Value> value;
    private final String textCode;
    private final byte binaryCode;

    protected UnaryExpression(Optional<Value> value, String textCode, byte binaryCode) {
        this.value = value;
        this.textCode = textCode;
        this.binaryCode = binaryCode;
    }

    @Override
    public final void writeTo(final TextWriter textWriter, final ExportContext context) throws IOException {
        textWriter.opening();
        textWriter.write(textCode);
        if (value.isPresent()) {
            textWriter.space();
            value.get().writeTo(textWriter, context);
        }
        textWriter.closing();
    }

    @Override
    public final void writeTo(final BinaryWriter.Writer codeWriter, final ExportContext context) throws IOException {
        if (value.isPresent()) {
            value.get().writeTo(codeWriter, context);
        }
        codeWriter.writeByte(binaryCode);
    }
}
