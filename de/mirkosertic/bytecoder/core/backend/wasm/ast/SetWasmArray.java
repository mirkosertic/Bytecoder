/*
 * Copyright 2023 Mirko Sertic
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

public class SetWasmArray implements WasmExpression {

    private final WasmType type;

    private final WasmValue array;

    private final WasmValue index;

    private final WasmValue value;

    SetWasmArray(final WasmType type, final WasmValue array, final WasmValue index, final WasmValue value) {
        this.type = type;
        this.array = array;
        this.index = index;
        this.value = value;
    }

    @Override
    public void writeTo(final TextWriter writer, final ExportContext context) throws IOException {
        writer.opening();
        writer.write("array.set ");
        type.writeRefTo(writer);
        writer.space();
        array.writeTo(writer, context);
        writer.space();
        index.writeTo(writer, context);
        writer.space();
        value.writeTo(writer, context);
        writer.closing();
        writer.newLine();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer binaryWriter, final ExportContext context) throws IOException {
        array.writeTo(binaryWriter, context);
        index.writeTo(binaryWriter, context);
        value.writeTo(binaryWriter, context);
        binaryWriter.writeByte((byte) 0xfb);
        binaryWriter.writeByte((byte) 0x0e);
        binaryWriter.writeUnsignedLeb128(type.index());
    }
}
