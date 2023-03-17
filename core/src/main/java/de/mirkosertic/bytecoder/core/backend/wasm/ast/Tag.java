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

public class Tag implements Exportable {

    private final String label;
    private final FunctionType functionType;

    protected Tag(final String label, final FunctionType functionType) {
        this.label = label;
        this.functionType = functionType;
    }

    public String getLabel() {
        return label;
    }

    public void writeTo(final BinaryWriter.SectionWriter sectionWriter) throws IOException {
        sectionWriter.writeByte((byte) 0x00);
        sectionWriter.writeUnsignedLeb128(functionType.index());
    }

    public void writeTo(final TextWriter textWriter) {
        textWriter.opening();
        textWriter.write("tag");
        textWriter.space();
        textWriter.writeLabel(label);

        for (final WasmType param : functionType.getParameter()) {
            textWriter.space();
            textWriter.opening();
            textWriter.write("param");
            textWriter.space();
            param.writeRefTo(textWriter);
            textWriter.closing();
        }
        textWriter.closing();
        textWriter.newLine();
    }

    @Override
    public void writeRefTo(final TextWriter textWriter) {
        textWriter.writeLabel(label);
    }
}
