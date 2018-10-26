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

public class I32Mul implements Expression {

    private final Value left;
    private final Value right;

    I32Mul(final Value left, final Value right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public void writeTo(final TextWriter textWriter, final ExportContext context) throws IOException {
        textWriter.opening();
        textWriter.write("i32.mul");
        textWriter.space();
        left.writeTo(textWriter, context);
        textWriter.space();
        right.writeTo(textWriter, context);
        textWriter.closing();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer codeWriter, final ExportContext context) throws IOException {
        left.writeTo(codeWriter, context);
        right.writeTo(codeWriter, context);
        codeWriter.writeByte((byte) 0x6c);
    }
}
