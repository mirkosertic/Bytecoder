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

public class SetGlobal implements WasmExpression {

    private final Global global;
    private final WasmValue value;

    SetGlobal(final Global global, final WasmValue value) {
        this.global = global;
        this.value = value;
    }

    @Override
    public void writeTo(final TextWriter textWriter, final ExportContext context) throws IOException {
        textWriter.opening();
        textWriter.write("global.set");
        textWriter.space();
        textWriter.writeLabel(global.getLabel());
        textWriter.space();
        value.writeTo(textWriter, context);
        textWriter.closing();
        textWriter.newLine();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer codeWriter, final ExportContext context) throws IOException {
        value.writeTo(codeWriter, context);
        codeWriter.writeByte((byte) 0x24);
        codeWriter.writeUnsignedLeb128(context.globalsIndex().indexOf(global));
    }
}
