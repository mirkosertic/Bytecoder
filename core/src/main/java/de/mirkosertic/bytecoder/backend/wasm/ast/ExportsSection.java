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
import java.util.HashMap;
import java.util.Map;

public class ExportsSection implements ModuleSection {

    private static final byte EXTERNAL_KIND_FUNCTION = (byte) 0;
    private static final byte EXTERNAL_KIND_TABLE = (byte) 1;
    private static final byte EXTERNAL_KIND_MEMORY = (byte) 2;
    private static final byte EXTERNAL_KIND_GLOBAL = (byte) 3;

    private final Map<String, Exportable> exports;

    public ExportsSection() {
        exports = new HashMap<>();
    }

    public void export(final Exportable exportable, final String name) {
        exports.put(name, exportable);
    }

    @Override
    public void writeTo(final TextWriter textWriter) {
        for (final Map.Entry<String, Exportable> entry : exports.entrySet()) {
            textWriter.opening();
            textWriter.write("export");
            textWriter.space();
            textWriter.writeText(entry.getKey());
            textWriter.space();
            entry.getValue().writeRefTo(textWriter);
            textWriter.closing();
            textWriter.newLine();
        }
    }

    public void writeTo(final BinaryWriter binaryWriter, FunctionIndex functionIndex) throws IOException {
        try (final BinaryWriter.SectionWriter exportWriter = binaryWriter.exportsSection()) {
            exportWriter.writeUnsignedLeb128(exports.size());
            for (Map.Entry<String, Exportable> entry : exports.entrySet()) {
                exportWriter.writeUTF8(entry.getKey());
                Exportable value = entry.getValue();
                if (value instanceof ExportableFunction) {
                    exportWriter.writeByte(EXTERNAL_KIND_FUNCTION);
                    exportWriter.writeUnsignedLeb128(functionIndex.indexOf(value));
                } else {
                    throw new IllegalStateException("Not Implemented yet for " + value);
                }
            }
        }
    }
}
