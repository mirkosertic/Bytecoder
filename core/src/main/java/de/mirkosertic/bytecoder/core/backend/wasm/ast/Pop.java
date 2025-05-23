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

public class Pop implements WasmExpression {

    private final WasmType type;

    Pop(final WasmType type) {
        this.type = type;
    }

    @Override
    public void writeTo(final TextWriter textWriter, final ExportContext context) {
        textWriter.opening();
        textWriter.write("pop");
        textWriter.space();
        type.writeRefTo(textWriter);
        textWriter.closing();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer codeWriter, final ExportContext context) {
        // codeWriter.writeByte((byte) 0x20);
        // codeWriter.writeUnsignedLeb128(context.localIndex().indexOf(local));
    }
}
