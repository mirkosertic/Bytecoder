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
import java.util.ArrayList;
import java.util.List;

public class Module {

    private final String label;
    private final TypesSection types;
    private final FunctionsSection functions;
    private final TablesSection tables;
    private final MemorySection mems;
    private final GlobalsSection globals;
    private final ElementSection elements;
    private final ImportsSection imports;
    private final ExportsSection exports;
    private final NameSection names;
    private final TagSection tags;
    private final SourceMapSection sourceMapSection;

    public Module(final String label, final String sourcemapFileName) {
        this.label = label;
        this.types = new TypesSection(this);
        this.exports = new ExportsSection(this);
        this.tables = new TablesSection(this);
        this.globals = new GlobalsSection(this);
        this.functions = new FunctionsSection(this);
        this.mems = new MemorySection(this);
        this.elements = new ElementSection(this);
        final DataSection data = new DataSection(this);
        final StartSection start = new StartSection(this);
        this.imports = new ImportsSection(this);
        this.names = new NameSection(this);
        this.tags = new TagSection(this);
        this.sourceMapSection = new SourceMapSection(this, sourcemapFileName);
    }

    public void writeTo(final TextWriter writer, final boolean enableDebug) throws IOException {
        writer.opening();
        writer.write("module");
        writer.space();
        writer.writeLabel(label);
        writer.newLine();

        types.writeTo(writer);
        imports.writeTo(writer);
        mems.writeTo(writer);
        globals.writeTo(writer);
        tags.writeCodeTo(writer);
        tables.writeTo(writer);
        elements.writeTo(writer);
        functions.writeTo(writer);
        exports.writeTo(writer);
        writer.closing();
    }

    public TypesSection getTypes() {
        return types;
    }

    public GlobalsIndex globalsIndex() {
        return globals.globalsIndex();
    }

    public TagIndex tagIndex() {
        return tags.tagIndex();
    }

    public FunctionIndex functionIndex() {
        final FunctionIndex functionIndex = new FunctionIndex();
        imports.addFunctionsToIndex(functionIndex);
        functions.addFunctionsToIndex(functionIndex);
        return functionIndex;
    }

    public void writeTo(final BinaryWriter writer, final boolean enableDebug) throws IOException {

        final FunctionIndex functionIndex = functionIndex();

        final List<Memory> memoryIndex = new ArrayList<>();
        mems.addMemoriesToIndex(memoryIndex);

        final WasmValue.ExportContext context = new WasmValue.ExportContext() {
            @Override
            public Container owningContainer() {
                return null;
            }

            @Override
            public TypeIndex typeIndex() {
                return Module.this.types.typesIndex();
            }

            @Override
            public FunctionIndex functionIndex() {
                return Module.this.functionIndex();
            }

            @Override
            public GlobalsIndex globalsIndex() {
                return Module.this.globalsIndex();
            }

            @Override
            public LocalIndex localIndex() {
                return null;
            }

            @Override
            public TagIndex tagIndex() {
                return Module.this.tagIndex();
            }

            @Override
            public WasmValue.ExportContext subWith(final Container container) {
                return null;
            }

            @Override
            public TablesSection.AnyFuncTable anyFuncTable() {
                return Module.this.getTables().funcTable();
            }
        };

        writer.header();
        types.writeTo(writer);
        imports.writeTo(writer, memoryIndex);
        functions.writeTo(writer, functionIndex);
        tables.writeTo(writer);
        mems.writeTo(writer);
        tags.writeCodeTo(writer);
        globals.writeTo(writer, context);
        exports.writeTo(writer, memoryIndex);
        elements.writeTo(writer, functionIndex);
        functions.writeCodeTo(writer, functionIndex);
        if (enableDebug) {
            names.writeCodeTo(writer);
        }
        sourceMapSection.writeTo(writer);
    }

    public String getLabel() {
        return label;
    }

    public MemorySection getMems() {
        return mems;
    }

    public FunctionsSection getFunctions() {
        return functions;
    }

    public ImportsSection getImports() {
        return imports;
    }

    public GlobalsSection getGlobals() {
        return globals;
    }

    public ExportsSection getExports() {
        return exports;
    }

    public TablesSection getTables() {
        return tables;
    }

    public TagSection getTags() {
        return tags;
    }
}
