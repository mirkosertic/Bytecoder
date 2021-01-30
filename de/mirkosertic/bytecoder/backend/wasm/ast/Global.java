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
import java.util.List;

public class Global {

    private final ExportsSection exportsSection;
    private final String label;
    private final PrimitiveType type;
    private final boolean mutable;
    private final WASMValue initializer;

    Global(final ExportsSection exportsSection, final String name, final PrimitiveType type, final boolean mutable, final WASMValue initializer) {
        this.exportsSection = exportsSection;
        this.label = name;
        this.type = type;
        this.mutable = mutable;
        this.initializer = initializer;
    }

    public String getLabel() {
        return label;
    }

    public PrimitiveType getType() {
        return type;
    }

    public void writeTo(final TextWriter textWriter) throws IOException {
        textWriter.opening();
        textWriter.write("global");
        textWriter.space();
        textWriter.writeLabel(label);
        textWriter.space();
        if (mutable) {
            textWriter.opening();
            textWriter.write("mut");
            textWriter.space();
            type.writeTo(textWriter);
            textWriter.closing();
        } else {
            type.writeTo(textWriter);
        }
        textWriter.space();
        initializer.writeTo(textWriter, null);
        textWriter.closing();
        textWriter.newLine();
    }

    public void writeTo(final BinaryWriter.SectionWriter writer, final List<Global> globalIndex) throws IOException {
        type.writeTo(writer);
        if (mutable) {
            writer.writeByte((byte) 1);
        } else {
            writer.writeByte((byte) 0);
        }
        initializer.writeTo(writer, null);
        writer.writeByte((byte) 0x0b);
    }
}
