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

public class WASMException implements Exportable {

    private final ExceptionsSection exceptionSection;
    private final List<PrimitiveType> arguments;
    private final String label;

    protected WASMException(final ExceptionsSection exceptionsSection, final String label, final List<PrimitiveType> argument) {
        this.exceptionSection = exceptionsSection;
        this.label = label;
        this.arguments = argument;
    }

    public String getLabel() {
        return label;
    }

    public void writeTo(final BinaryWriter.SectionWriter sectionWriter) throws IOException {
        sectionWriter.writeUnsignedLeb128(arguments.size());
        for (final PrimitiveType type : arguments) {
            type.writeTo(sectionWriter);
        }
    }

    public void writeTo(final TextWriter textWriter) throws IOException {
        textWriter.opening();
        textWriter.write("except");
        textWriter.space();
        textWriter.writeLabel(label);
        for (final PrimitiveType type : arguments) {
            textWriter.space();
            type.writeTo(textWriter);
        }
        textWriter.closing();
        textWriter.newLine();
    }

    @Override
    public void writeRefTo(final TextWriter textWriter) throws IOException {
        textWriter.opening();
        textWriter.write("except");
        textWriter.space();
        textWriter.writeLabel(label);
        textWriter.closing();
    }
}