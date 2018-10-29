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

    private class DefaultExportContext implements Value.ExportContext {

        private final Container owningContainer;
        private final List<Function> functionIndex;

        public DefaultExportContext(final Container owningContainer, final List<Function> functionIndex) {
            this.owningContainer = owningContainer;
            this.functionIndex = functionIndex;
        }

        @Override
        public Container owningContainer() {
            return owningContainer;
        }

        @Override
        public List<Function> functionIndex() {
            return functionIndex;
        }

        @Override
        public List<Global> globalsIndex() {
            return globalsSection.globalsIndex();
        }

        @Override
        public LocalIndex localIndex() {
            return localIndex;
        }

        @Override
        public Value.ExportContext subWith(final Container container) {
            return new DefaultExportContext(container, functionIndex);
        }

        @Override
        public List<FunctionType> typeIndex() {
            return typesSection.typesIndex();
        }
    }

    private final TypesSection typesSection;
    private final ExportsSection exportsSection;
    private final LocalIndex localIndex;
    private final GlobalsSection globalsSection;

    ExportableFunction(final TypesSection typesSection, final GlobalsSection globalsSection, final TablesSection tablesSection, final ExportsSection exportsSection,
            final FunctionType functionType, final String label,
            final List<Param> params, final PrimitiveType result) {
        super(tablesSection, functionType, label, params, result);
        this.typesSection = typesSection;
        this.exportsSection = exportsSection;
        this.localIndex = new LocalIndex(params);
        this.globalsSection = globalsSection;
    }

    ExportableFunction(final TypesSection typesSection, final GlobalsSection globalsSection, final TablesSection tablesSection, final ExportsSection exportsSection,
            final FunctionType functionType, final String label,
            final List<Param> params) {
        super(tablesSection, functionType, label, params);
        this.typesSection = typesSection;
        this.exportsSection = exportsSection;
        this.localIndex = new LocalIndex(params);
        this.globalsSection = globalsSection;
    }

    ExportableFunction(final TypesSection typesSection, final GlobalsSection globalsSection, final TablesSection tablesSection, final ExportsSection exportsSection,
            final FunctionType functionType, final String label, final PrimitiveType result) {
        super(tablesSection, functionType, label, result);
        this.typesSection = typesSection;
        this.exportsSection = exportsSection;
        this.localIndex = new LocalIndex();
        this.globalsSection = globalsSection;
    }

    public void exportAs(final String functionName) {
        exportsSection.export(this, functionName);
    }

    public Local localByLabel(final String label, final PrimitiveType type) {
        Local local = localIndex.localByLabel(label);
        if (null == local) {
            local = new Local(label, type);
            localIndex.add(local);
        }
        return local;
    }

    @Override
    public void writeTo(final TextWriter textWriter) throws IOException {
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
        final DefaultExportContext context = new DefaultExportContext(this, null);
        for (final Expression expression : getChildren()) {
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

    public void writeCodeTo(final BinaryWriter.SectionWriter sectionWriter, final List<Function> functionIndex) throws IOException {
        try (final BinaryWriter.BlockWriter codeWriter = sectionWriter.blockWriter()) {

            // Local data
            final List<Local> locals = localIndex.localsExcludingParams();
            codeWriter.writeUnsignedLeb128(locals.size());
            for (final Local local : locals) {
                codeWriter.writeUnsignedLeb128(1);
                local.getType().writeTo(codeWriter);
            }

            final DefaultExportContext context = new DefaultExportContext(this, functionIndex);

            for (final Expression expression : getChildren()) {
                expression.writeTo(codeWriter, context);
            }

            // Finish with an end Instruction
            codeWriter.writeByte((byte) 0x0b);
        }
    }

    public List<Global> globalsIndex() {
        return globalsSection.globalsIndex();
    }
}