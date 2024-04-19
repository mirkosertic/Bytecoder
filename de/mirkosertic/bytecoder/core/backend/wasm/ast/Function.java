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
import java.util.List;

public class Function extends Container implements Importable, Callable {

    private final Module module;
    private final FunctionType functionType;
    private final String label;
    private final List<Param> params;
    private final WasmType resultType;

    Function(final Module aModule, final FunctionType functionType, final String label, final List<Param> params, final WasmType result) {
        this.module = aModule;
        this.functionType = functionType;
        this.label = label;
        this.params = params;
        this.resultType = result;
    }

    Function(final Module aModule, final FunctionType functionType, final String label, final List<Param> params) {
        this.module = aModule;
        this.functionType = functionType;
        this.label = label;
        this.params = params;
        this.resultType = null;
    }

    Function(final Module aModule, final FunctionType functionType, final String label, final WasmType result) {
        this.module = aModule;
        this.functionType = functionType;
        this.label = label;
        this.params = null;
        this.resultType = result;
    }

    @Override
    public void writeTo(final TextWriter textWriter, final Module aModule, final WasmValue.ExportContext exportContext) throws IOException {
        textWriter.opening();
        textWriter.write("func");
        textWriter.space();
        textWriter.writeLabel(label);
        textWriter.space();
        textWriter.opening();
        textWriter.write("type ");
        getFunctionType().writeRefTo(textWriter);
        textWriter.closing();
        textWriter.closing();
    }

    protected Module getModule() {
        return module;
    }

    public WasmType getFunctionType() {
        return functionType;
    }

    public String getLabel() {
        return label;
    }

    public List<Param> getParams() {
        return params;
    }

    public WasmType getResultType() {
        return resultType;
    }

    public Function toTable() {
        module.getTables().funcTable().addToTable(this);
        return this;
    }

    @Override
    public WasmType resolveResultType(final WasmValue.ExportContext context) {
        return resultType;
    }

    @Override
    public int resolveIndex(final WasmValue.ExportContext context) {
        return context.functionIndex().indexOf(this);
    }
}
