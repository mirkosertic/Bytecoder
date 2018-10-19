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

public class ExportsContent implements ModuleContent {

    private final Map<String, Exportable> exports;

    public ExportsContent() {
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

    @Override
    public void writeTo(final BinaryWriter binaryWriter) throws IOException {
        try (final BinaryWriter.SectionWriter exportWriter = binaryWriter.exportsSection()) {
            exportWriter.writeUnsignedLeb128(0);
        }
    }
}
