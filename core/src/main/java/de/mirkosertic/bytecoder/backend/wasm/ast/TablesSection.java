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

public class TablesSection extends ModuleSection {

    public static class AnyFuncTable {

        private final List<Function> functions;

        AnyFuncTable() {
            this.functions = new ArrayList<>();
        }

        public int index() {
            return 0;
        }

        List<Function> functions() {
            return functions;
        }

        void addToTable(final Function function) {
            functions.add(function);
        }

        public void writeTo(final BinaryWriter.SectionWriter writer) throws IOException {
            PrimitiveType.anyfunc.writeTo(writer);
            writer.writeByte((byte) 0);
            writer.writeUnsignedLeb128(functions.size());
        }

        public void writeTo(final TextWriter textWriter) {
            textWriter.opening();
            textWriter.write("table");
            textWriter.space();
            textWriter.writeInteger(functions.size());
            textWriter.space();
            textWriter.write("anyfunc");
            textWriter.closing();
            textWriter.newLine();
        }

        public int indexOf(final Function function) {
            return functions.indexOf(function);
        }
    }

    private AnyFuncTable funcTable;

    TablesSection(final Module aModule) {
        super(aModule);
    }

    public void writeTo(final TextWriter textWriter) {
        if (funcTable != null) {
            funcTable.writeTo(textWriter);
        }
    }

    public void writeTo(final BinaryWriter binaryWriter) throws IOException {
        try (final BinaryWriter.SectionWriter writer = binaryWriter.tablesSection()) {
            if (funcTable == null) {
                writer.writeUnsignedLeb128(0);
            } else {
                writer.writeUnsignedLeb128(1);
                funcTable.writeTo(writer);
            }
        }
    }

    public boolean hasFuncTable() {
        return funcTable != null;
    }

    public AnyFuncTable funcTable() {
        if (funcTable == null) {
            funcTable = new AnyFuncTable();
        }
        return funcTable;
    }
}
