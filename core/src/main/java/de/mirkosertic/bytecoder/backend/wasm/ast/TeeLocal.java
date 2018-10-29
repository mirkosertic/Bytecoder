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

public class TeeLocal implements Expression {

    private final Local local;
    private final Value value;

    TeeLocal(final Local local, final Value value) {
        this.local = local;
        this.value = value;
    }

    @Override
    public void writeTo(final TextWriter textWriter, final ExportContext context) throws IOException {
        textWriter.opening();
        textWriter.write("tee_local");
        textWriter.space();
        textWriter.writeLabel(local.getLabel());
        textWriter.space();
        value.writeTo(textWriter, context);
        textWriter.closing();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer codeWriter, final ExportContext context) throws IOException {
        value.writeTo(codeWriter, context);
        codeWriter.writeByte((byte) 0x22);
        codeWriter.writeUnsignedLeb128(context.localIndex().indexOf(local));
    }
}