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

public class TagSection extends ModuleSection {

    private final TagIndex tagIndex;

    public TagSection(final Module module) {
        super(module);
        tagIndex = new TagIndex();
    }

    public TagIndex tagIndex() {
        return tagIndex;
    }

    public void writeCodeTo(final BinaryWriter writer) throws IOException {
        if (!tagIndex.isEmpty()) {
            try (final BinaryWriter.SectionWriter sectionWriter = writer.tagSection()) {
                sectionWriter.writeUnsignedLeb128(tagIndex.size());
                for (int i = 0; i< tagIndex.size(); i++) {
                    final Tag event = tagIndex.get(i);
                    event.writeTo(sectionWriter);
                }
            }
        }
    }

    public void writeCodeTo(final TextWriter writer) {
        for (int i = 0; i< tagIndex.size(); i++) {
            final Tag event = tagIndex.get(i);
            event.writeTo(writer);
        }
    }
}
