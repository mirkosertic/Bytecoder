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

public class Cast implements WasmValue {

    private final StructType structType;

    private final WasmValue source;

    Cast(final StructType structType, final WasmValue source) {
        this.structType = structType;
        this.source = source;
    }

    @Override
    public void writeTo(final TextWriter writer, final ExportContext context) throws IOException {
        writer.opening();
        writer.write("ref.cast_static $");
        writer.write(structType.getName());
        writer.space();
        source.writeTo(writer, context);
        writer.closing();

    }

    @Override
    public void writeTo(final BinaryWriter.Writer binaryWriter, final ExportContext context) throws IOException {
        source.writeTo(binaryWriter, context);
        binaryWriter.writeByte((byte) 0xfb);
        binaryWriter.writeByte((byte) 0x45);
        binaryWriter.writeSignedLeb128(structType.index());
    }
}
