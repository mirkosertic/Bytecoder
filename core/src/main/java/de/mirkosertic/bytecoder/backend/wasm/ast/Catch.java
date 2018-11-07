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

public class Catch extends Container {

    Catch(final Container parent) {
        super(parent);
    }

    public void writeTo(final BinaryWriter.Writer codeWriter, final WASMValue.ExportContext context) throws IOException {
        codeWriter.writeByte((byte) 0x07);
        for (final WASMExpression e : getChildren()) {
            e.writeTo(codeWriter, context.subWith(this));
        }
    }

    public void writeTo(final TextWriter textWriter, final WASMValue.ExportContext context) throws IOException {
        textWriter.opening();
        textWriter.write("catch");
        textWriter.newLine();

        for (final WASMExpression e : getChildren()) {
            e.writeTo(textWriter, context.subWith(this));
        }

        textWriter.closing();
        textWriter.newLine();
    }
}
