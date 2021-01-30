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
import java.util.ArrayList;
import java.util.List;

public class MemorySection extends ModuleSection {

    private final List<Memory> memories;

    MemorySection(final Module aModule) {
        super(aModule);
        this.memories = new ArrayList<>();
    }

    public Memory newMemory(final int initialPages, final int maximumPages) {
        final Memory memory = new Memory(this, initialPages, maximumPages);
        memories.add(memory);
        return memory;
    }

    int indexOf(final Memory memory) {
        return memories.indexOf(memory);
    }

    void export(final Memory memory, final String objectName) {
        getModule().getExports().export(memory, objectName);
    }

    public void writeTo(final TextWriter textWriter) {
        for (final Memory memory : memories) {
            memory.writeTo(textWriter);
            textWriter.newLine();
        }
    }

    public void writeTo(final BinaryWriter binaryWriter) throws IOException {
        try (final BinaryWriter.SectionWriter writer = binaryWriter.memorySection()) {
            writer.writeUnsignedLeb128(memories.size());
            for (final Memory memory : memories) {
                memory.writeTo(writer);
            }
        }
    }

    public void addMemoriesToIndex(final List<Memory> memoryIndex) {
        memoryIndex.addAll(memories);
    }
}