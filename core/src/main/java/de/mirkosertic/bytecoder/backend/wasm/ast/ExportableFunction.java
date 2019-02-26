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

public class ExportableFunction extends Function implements Exportable {

    private class DefaultExportContext implements WASMValue.ExportContext {

        private final Container owningContainer;
        private final FunctionIndex functionIndex;

        DefaultExportContext(final Container owningContainer, final FunctionIndex functionIndex) {
            this.owningContainer = owningContainer;
            this.functionIndex = functionIndex;
        }

        @Override
        public Container owningContainer() {
            return owningContainer;
        }

        @Override
        public FunctionIndex functionIndex() {
            return functionIndex;
        }

        @Override
        public GlobalsIndex globalsIndex() {
            return getModule().getGlobals().globalsIndex();
        }

        @Override
        public LocalIndex localIndex() {
            return localIndex;
        }

        @Override
        public WASMValue.ExportContext subWith(final Container container) {
            return new DefaultExportContext(container, functionIndex);
        }

        @Override
        public TypeIndex typeIndex() {
            return getModule().getTypes().typesIndex();
        }

        @Override
        public TablesSection.AnyFuncTable anyFuncTable() {
            return getModule().getTables().funcTable();
        }

        @Override
        public ExceptionIndex eventIndex() {
            return getModule().exceptionIndex();
        }
    }

    private final LocalIndex localIndex;

    ExportableFunction(final Module aModule, final WASMType functionType, final String label, final List<Param> params, final PrimitiveType result) {
        super(aModule, functionType, label, params, result);
        this.localIndex = new LocalIndex(params);
    }

    ExportableFunction(final Module aModule, final WASMType functionType, final String label, final List<Param> params) {
        super(aModule, functionType, label, params);
        this.localIndex = new LocalIndex(params);
    }

    ExportableFunction(final Module aModule, final WASMType functionType, final String label, final PrimitiveType result) {
        super(aModule, functionType, label, result);
        this.localIndex = new LocalIndex();
    }

    public void exportAs(final String functionName) {
        getModule().getExports().export(this, functionName);
    }

    public Local localByLabel(final String label) {
        final Local local = localIndex.localByLabel(label);
        if (local != null) {
            return local;
        }
        throw new IllegalArgumentException("No such local : " + label);
    }

    public LocalIndex localIndex() {
        return localIndex;
    }

    public Local newLocal(final String label, final PrimitiveType type) {
        if (localIndex.localByLabel(label) != null) {
            throw new IllegalStateException("Local " + label + " already defined!");
        }
        final Local local = new Local(label, type);
        localIndex.add(local);
        return local;
    }

    @Override
    public void writeTo(final TextWriter textWriter, final Module aModule) throws IOException {
        textWriter.opening();
        textWriter.write("func");
        textWriter.space();
        textWriter.writeLabel(getLabel());
        textWriter.space();
        getFunctionType().writeRefTo(textWriter);
        if (getParams() != null) {
            for (final Param param : getParams()) {
                textWriter.space();
                param.writeTo(textWriter);
            }
        }
        if (getResultType() != null) {
            textWriter.space();
            textWriter.opening();
            textWriter.write("result");
            textWriter.space();
            getResultType().writeTo(textWriter);
            textWriter.closing();
        }
        textWriter.newLine();
        for (final Local local : localIndex.localsExcludingParams()) {
            local.writeTo(textWriter);
            textWriter.newLine();
        }
        final DefaultExportContext context = new DefaultExportContext(this, getModule().functionIndex());
        for (final WASMExpression expression : getChildren()) {
            expression.writeTo(textWriter, context);
        }
        textWriter.closing();
    }

    @Override
    public void writeRefTo(final TextWriter textWriter) {
        textWriter.opening();
        textWriter.write("func");
        textWriter.space();
        textWriter.writeLabel(getLabel());
        textWriter.closing();
    }

    public void writeCodeTo(final BinaryWriter.Writer sectionWriter, final FunctionIndex functionIndex) throws IOException {
        try (final BinaryWriter.BlockWriter codeWriter = sectionWriter.blockWriter()) {

            // Local data
            final List<Local> locals = localIndex.localsExcludingParams();
            codeWriter.writeUnsignedLeb128(locals.size());
            for (final Local local : locals) {
                codeWriter.writeUnsignedLeb128(1);
                local.getType().writeTo(codeWriter);
            }

            final DefaultExportContext context = new DefaultExportContext(this, functionIndex);

            for (final WASMExpression expression : getChildren()) {
                expression.writeTo(codeWriter, context);
            }

            // Finish with an end Instruction
            codeWriter.writeByte((byte) 0x0b);
        }
    }

    @Override
    public ExportableFunction toTable() {
        return (ExportableFunction) super.toTable();
    }
}