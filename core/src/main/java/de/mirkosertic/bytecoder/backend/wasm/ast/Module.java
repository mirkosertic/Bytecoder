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

    private final TypesContent types;
    private final FunctionsContent functions;
    private final TablesContent tables;
    private final MemoryContent mems;
    private final GlobalsContent globals;
    private final ElementContent elements;
    private final DataContent data;
    private final StartContent start;
    private final ImportsContent imports;
    private final ExportsContent exports;

    public Module() {
        types = new TypesContent();
        exports = new ExportsContent();
        functions = new FunctionsContent(types, exports);
        tables = new TablesContent();
        mems = new MemoryContent(exports);
        globals = new GlobalsContent();
        elements = new ElementContent();
        data = new DataContent();
        start = new StartContent();
        imports = new ImportsContent(types);
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
        writer.header();
        types.writeTo(writer);
        functions.writeTo(writer);
        mems.writeTo(writer);
        exports.writeTo(writer);
        functions.writeCodeTo(writer);
    }

    public MemoryContent getMems() {
        return mems;
    }

    public FunctionsContent getFunctions() {
        return functions;
    }

    public ImportsContent getImports() {
        return imports;
    }
}