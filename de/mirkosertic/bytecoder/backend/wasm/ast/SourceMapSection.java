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

public class SourceMapSection extends ModuleSection {

    private final String fileName;

    SourceMapSection(final Module module, final String fileName) {
        super(module);
        this.fileName = fileName;
    }

    public void writeTo(final TextWriter textWriter) {
    }

    public void writeTo(final BinaryWriter binaryWriter) throws IOException {
        try (final BinaryWriter.SectionWriter sectionWriter = binaryWriter.customSection()) {
            sectionWriter.writeUTF8("sourceMappingURL");
            sectionWriter.writeUTF8(fileName);
        }
    }
}
