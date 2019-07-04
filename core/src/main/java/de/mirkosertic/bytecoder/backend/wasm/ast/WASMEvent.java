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

public class WASMEvent implements Exportable {

    private final WASMType type;
    private final String label;
    private final TypesSection typesSection;

    protected WASMEvent(final TypesSection typesSection, final String label, final WASMType type) {
        this.label = label;
        this.type = type;
        this.typesSection = typesSection;
    }

    public String getLabel() {
        return label;
    }

    public void writeTo(final BinaryWriter.SectionWriter sectionWriter) throws IOException {
        sectionWriter.writeUnsignedLeb128(0);
        sectionWriter.writeUnsignedLeb128(typesSection.indexOf(type));
    }

    public void writeTo(final TextWriter textWriter) throws IOException {
        textWriter.opening();
        textWriter.write("event");
        textWriter.space();
        textWriter.writeLabel(label);
        textWriter.space();
        type.writeRefTo(textWriter);
        textWriter.closing();
        textWriter.newLine();
    }

    @Override
    public void writeRefTo(final TextWriter textWriter) throws IOException {
        textWriter.opening();
        textWriter.write("event");
        textWriter.space();
        textWriter.writeLabel(label);
        textWriter.closing();
    }
}