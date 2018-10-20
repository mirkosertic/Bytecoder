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

public class FunctionsSection implements ModuleSection {

    private final ExportsSection exports;
    private final TypesSection types;
    private final List<ExportableFunction> functions;

    public FunctionsSection(final TypesSection types, final ExportsSection exports) {
        this.types = types;
        this.exports = exports;
        this.functions = new ArrayList<>();
    }

    public ExportableFunction newFunction(final String label, final List<Param> parameter, final PrimitiveType result) {
        final FunctionType type = types.typeFor(parameter.stream().map(Param::getType).collect(Collectors.toList()), result);
        final ExportableFunction function = new ExportableFunction(exports, type, label, parameter, result);
        functions.add(function);
        return function;
    }

    public ExportableFunction newFunction(final String label, final List<Param> parameter) {
        final FunctionType type = types.typeFor(parameter.stream().map(Param::getType).collect(Collectors.toList()));
        final ExportableFunction function = new ExportableFunction(exports, type, label, parameter);
        functions.add(function);
        return function;
    }

    public ExportableFunction newFunction(final String label, final PrimitiveType result) {
        final FunctionType type = types.typeFor(result);
        final ExportableFunction function = new ExportableFunction(exports, type, label, result);
        functions.add(function);
        return function;
    }

    @Override
    public void writeTo(final TextWriter textWriter) throws IOException {
        for (final Function function : functions) {
            function.writeTo(textWriter);
            textWriter.newLine();
        }
    }

    public void writeTo(final BinaryWriter binaryWriter, FunctionIndex functionIndex) throws IOException {
        try (final BinaryWriter.SectionWriter sectionWriter = binaryWriter.functionSection()) {
            List<Function> functions = functionIndex.getFunctions();
            sectionWriter.writeUnsignedLeb128(functions.size());
            for (final Function function : functions) {
                sectionWriter.writeUnsignedLeb128(function.getFunctionType().index());
            }
        }
    }

    public void writeCodeTo(final BinaryWriter binaryWriter, FunctionIndex functionIndex) throws IOException {
        try (final BinaryWriter.SectionWriter sectionWriter = binaryWriter.codeSection()) {
            List<Function> functions = functionIndex.getFunctions().stream().filter(t -> t instanceof ExportableFunction).collect(Collectors.toList());
            sectionWriter.writeUnsignedLeb128(functions.size());
            for (final Function function : functions) {
                ExportableFunction ef = (ExportableFunction) function;
                ef.writeCodeTo(sectionWriter);
            }
        }
    }

    public void addFunctionsToIndex(FunctionIndex functionIndex) {
        for (Function f : functions) {
            functionIndex.add(f);
        }
    }
}