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

public class Memory implements Exportable {

    private final MemorySection memory;
    private final int initialPages;
    private final int maximumPages;

    Memory(final MemorySection content, final int initialPages, final int maximumPages) {
        this.memory = content;
        this.initialPages = initialPages;
        this.maximumPages = maximumPages;
    }

    public void exportAs(final String objectName) {
        memory.export(this, objectName);
    }

    public void writeTo(final TextWriter textWriter) {
        textWriter.opening();
        textWriter.write("memory");
        textWriter.space();
        textWriter.writeLabel("mem" + memory.indexOf(this));
        textWriter.space();
        textWriter.writeInteger(initialPages);
        textWriter.space();
        textWriter.writeInteger(maximumPages);
        textWriter.closing();
    }

    @Override
    public void writeRefTo(final TextWriter textWriter) {
        textWriter.opening();
        textWriter.write("memory");
        textWriter.space();
        textWriter.writeLabel("mem" + memory.indexOf(this));
        textWriter.closing();
    }

    public void writeTo(final BinaryWriter.Writer writer) throws IOException {
        writer.writeByte((byte) 1);
        writer.writeUnsignedLeb128(initialPages);
        writer.writeUnsignedLeb128(maximumPages);
    }
}