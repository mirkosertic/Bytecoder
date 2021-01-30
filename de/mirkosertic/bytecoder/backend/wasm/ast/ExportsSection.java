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
import java.util.List;
import java.util.Map;

public class ExportsSection extends ModuleSection {

    private final Map<String, Exportable> exports;

    ExportsSection(final Module aModule) {
        super(aModule);
        exports = new HashMap<>();
    }

    public void export(final Exportable exportable, final String name) {
        exports.put(name, exportable);
    }

    public void writeTo(final TextWriter textWriter) throws IOException {
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

    public void writeTo(final BinaryWriter binaryWriter, final List<Memory> memoryIndex) throws IOException {
        final FunctionIndex functionIndex = getModule().functionIndex();
        final EventIndex eventIndex = getModule().eventIndex();
        try (final BinaryWriter.SectionWriter exportWriter = binaryWriter.exportsSection()) {
            exportWriter.writeUnsignedLeb128(exports.size());
            for (final Map.Entry<String, Exportable> entry : exports.entrySet()) {
                exportWriter.writeUTF8(entry.getKey());
                final Exportable value = entry.getValue();
                if (value instanceof ExportableFunction) {
                    exportWriter.writeByte(ExternalKind.EXTERNAL_KIND_FUNCTION);
                    exportWriter.writeUnsignedLeb128(functionIndex.indexOf((Function) value));
                } else if (value instanceof Memory) {
                    exportWriter.writeByte(ExternalKind.EXTERNAL_KIND_MEMORY);
                    exportWriter.writeUnsignedLeb128(memoryIndex.indexOf(value));
                } else if (value instanceof WASMEvent) {
                    exportWriter.writeByte(ExternalKind.EXTERNAL_KIND_EXCEPTION);
                    exportWriter.writeUnsignedLeb128(eventIndex.indexOf((WASMEvent) value));
                } else {
                    throw new IllegalStateException("Not Implemented yet for " + value);
                }
            }
        }
    }
}