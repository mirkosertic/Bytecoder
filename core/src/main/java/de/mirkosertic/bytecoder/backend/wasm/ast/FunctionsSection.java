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

public class FunctionsSection extends ModuleSection {

    private final List<ExportableFunction> functions;

    FunctionsSection(final Module aModule) {
        super(aModule);
        this.functions = new ArrayList<>();
    }

    public ExportableFunction newFunction(final String label, final List<Param> parameter, final PrimitiveType result) {
        final WASMType type = getModule().getTypes().typeFor(parameter.stream().map(Param::getType).collect(Collectors.toList()), result);
        final ExportableFunction function = new ExportableFunction(getModule(), type, label, parameter, result);
        functions.add(function);
        return function;
    }

    public ExportableFunction newFunction(final String label, final List<Param> parameter) {
        final WASMType type = getModule().getTypes().typeFor(parameter.stream().map(Param::getType).collect(Collectors.toList()));
        final ExportableFunction function = new ExportableFunction(getModule(), type, label, parameter);
        functions.add(function);
        return function;
    }

    public ExportableFunction newFunction(final String label, final PrimitiveType result) {
        final WASMType type = getModule().getTypes().typeFor(result);
        final ExportableFunction function = new ExportableFunction(getModule(), type, label, result);
        functions.add(function);
        return function;
    }

    public void writeTo(final TextWriter textWriter) throws IOException {
        for (final Function function : functions) {
            function.writeTo(textWriter, getModule());
            textWriter.newLine();
        }
    }

    public void writeTo(final BinaryWriter binaryWriter, final FunctionIndex functionIndex) throws IOException {
        try (final BinaryWriter.SectionWriter sectionWriter = binaryWriter.functionSection()) {
            final List<ExportableFunction> exportableFunction = functionIndex.exportableFunctions();
            sectionWriter.writeUnsignedLeb128(exportableFunction.size());
            for (final ExportableFunction function : exportableFunction) {
                sectionWriter.writeUnsignedLeb128(function.getFunctionType().index());
            }
        }
    }

    public void writeCodeTo(final BinaryWriter binaryWriter, final FunctionIndex functionIndex) throws IOException {
        try (final BinaryWriter.SectionWriter sectionWriter = binaryWriter.codeSection()) {
            final List<ExportableFunction> functions = functionIndex.exportableFunctions();
            sectionWriter.writeUnsignedLeb128(functions.size());
            for (final ExportableFunction function : functions) {
                function.writeCodeTo(sectionWriter, functionIndex);
            }
        }
    }

    public void addFunctionsToIndex(final FunctionIndex functionIndex) {
        for (final Function f : functions) {
            functionIndex.add(f);
        }
    }
}