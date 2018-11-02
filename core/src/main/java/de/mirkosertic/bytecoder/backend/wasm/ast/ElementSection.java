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

public class ElementSection extends ModuleSection {

    ElementSection(final Module aModule) {
        super(aModule);
    }

    public void writeTo(final TextWriter textWriter) {
        if (getModule().getTables().hasFuncTable()) {
            final TablesSection.AnyFuncTable any = getModule().getTables().funcTable();
            final List<Function> functions = any.functions();
            for (int i=0;i<functions.size();i++) {
                textWriter.opening();
                textWriter.write("elem");
                textWriter.space();

                textWriter.opening();
                textWriter.write("i32.const");
                textWriter.space();
                textWriter.writeInteger(i);
                textWriter.closing();

                textWriter.space();
                textWriter.writeLabel(functions.get(i).getLabel());
                textWriter.closing();

                textWriter.newLine();
            }
        }
    }

    public void writeTo(final BinaryWriter binaryWriter, final FunctionIndex functionIndex) throws IOException {
        try (final BinaryWriter.SectionWriter writer = binaryWriter.elementsSection()) {
            if (getModule().getTables().hasFuncTable()) {
                final TablesSection.AnyFuncTable any = getModule().getTables().funcTable();
                final List<Function> functions = any.functions();

                writer.writeUnsignedLeb128(functions.size());
                for (int i=0;i<functions.size();i++) {
                    writer.writeUnsignedLeb128(any.index());
                    writer.writeByte((byte) 0x41);
                    writer.writeSignedLeb128(i);
                    writer.writeByte((byte) 0x0b);
                    writer.writeUnsignedLeb128(1);
                    writer.writeUnsignedLeb128(functionIndex.indexOf(functions.get(i)));
                }
            } else {
                writer.writeUnsignedLeb128(0);
            }
        }
    }
}