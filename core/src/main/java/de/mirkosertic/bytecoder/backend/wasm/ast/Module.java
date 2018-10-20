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

public class Module {

    private final TypesSection types;
    private final FunctionsSection functions;
    private final MemorySection mems;
    private final ImportsSection imports;
    private final ExportsSection exports;

    public Module() {
        types = new TypesSection();
        exports = new ExportsSection();
        functions = new FunctionsSection(types, exports);
        TablesSection tables = new TablesSection();
        mems = new MemorySection(exports);
        GlobalsSection globals = new GlobalsSection();
        ElementSection elements = new ElementSection();
        DataSection data = new DataSection();
        StartSection start = new StartSection();
        imports = new ImportsSection(types);
    }

    public void writeTo(final TextWriter writer) throws IOException {
        writer.opening();
        writer.write("module");
        writer.space();
        writer.newLine();

        mems.writeTo(writer);
        functions.writeTo(writer);
        types.writeTo(writer);
        exports.writeTo(writer);
        imports.writeTo(writer);
        writer.closing();
    }

    public void writeTo(final BinaryWriter writer) throws IOException {

        FunctionIndex functionIndex = new FunctionIndex();
        imports.addFunctionsToIndex(functionIndex);
        functions.addFunctionsToIndex(functionIndex);

        writer.header();
        types.writeTo(writer);
        functions.writeTo(writer, functionIndex);
        mems.writeTo(writer);
        exports.writeTo(writer, functionIndex);
        functions.writeCodeTo(writer, functionIndex);
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
}