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
import java.util.stream.Collectors;

public class ImportsSection implements ModuleSection {

    private static class ImportEntry {

        private final ImportReference reference;
        private final Importable importable;

        public ImportEntry(final ImportReference reference, final Importable importable) {
            this.reference = reference;
            this.importable = importable;
        }

        public ImportReference getReference() {
            return reference;
        }

        public Importable getImportable() {
            return importable;
        }
    }

    private final TypesSection types;
    private final List<ImportEntry> imports;
    private final TablesSection tablesSection;

    ImportsSection(final TypesSection types, final TablesSection tablesSection) {
        this.types = types;
        this.imports = new ArrayList<>();
        this.tablesSection = tablesSection;
    }

    public Function importFunction(final ImportReference importReference, final String label, final List<Param> parameter, final PrimitiveType result) {
        final FunctionType type = types.typeFor(parameter.stream().map(Param::getType).collect(Collectors.toList()), result);
        final Function function = new Function(tablesSection, type, label, parameter, result);
        imports.add(new ImportEntry(importReference, function));
        return function;
    }

    public Function importFunction(final ImportReference importReference, final String label, final List<Param> parameter) {
        final FunctionType type = types.typeFor(parameter.stream().map(Param::getType).collect(Collectors.toList()));
        final Function function = new Function(tablesSection, type, label, parameter);
        imports.add(new ImportEntry(importReference, function));
        return function;
    }

    public Function importFunction(final ImportReference importReference, final String label, final PrimitiveType result) {
        final FunctionType type = types.typeFor(result);
        final Function function = new Function(tablesSection, type, label, result);
        imports.add(new ImportEntry(importReference, function));
        return function;
    }

    @Override
    public void writeTo(final TextWriter textWriter) throws IOException {
        for (final ImportEntry entry : imports) {

            final ImportReference ref = entry.getReference();

            textWriter.opening();
            textWriter.writeLabel("import");
            textWriter.space();
            textWriter.writeText(ref.getModuleName());
            textWriter.space();
            textWriter.writeText(ref.getObjectName());
            textWriter.space();
            entry.getImportable().writeTo(textWriter);
            textWriter.closing();
            textWriter.newLine();
        }
    }

    public void addFunctionsToIndex(final List<Function> functionIndex) {
        for (final ImportEntry value : imports) {
            if (value.getImportable() instanceof Function) {
                functionIndex.add((Function) value.getImportable());
            }
        }
    }

    public void writeTo(final BinaryWriter binaryWriter,
            final List<Function> functionIndex,
            final List<Memory> memoryIndex) throws IOException {
        try (final BinaryWriter.SectionWriter sectionWriter = binaryWriter.importsSection()) {
            sectionWriter.writeUnsignedLeb128(imports.size());
            for (final ImportEntry entry : imports) {
                final Importable value = entry.getImportable();
                final ImportReference ref = entry.getReference();

                sectionWriter.writeUTF8(ref.getModuleName());
                sectionWriter.writeUTF8(ref.getObjectName());

                if (value instanceof Function) {
                    sectionWriter.writeByte(ExternalKind.EXTERNAL_KIND_FUNCTION);
                    sectionWriter.writeUnsignedLeb128(functionIndex.indexOf(value));
                } else if (value instanceof Memory) {
                    sectionWriter.writeByte(ExternalKind.EXTERNAL_KIND_FUNCTION);
                    sectionWriter.writeUnsignedLeb128(memoryIndex.indexOf(value));
                } else {
                    throw new IllegalStateException("Not Implemented yet for " + value);
                }
            }
        }
    }
}