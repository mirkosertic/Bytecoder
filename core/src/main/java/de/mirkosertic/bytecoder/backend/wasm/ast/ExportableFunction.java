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

public class ExportableFunction extends Function implements Exportable {

    private final ExportsSection exportsSection;
    private final List<Expression> expressions;

    public ExportableFunction(final ExportsSection exportsSection,
            final FunctionType functionType, final String label,
            final List<Param> params, final PrimitiveType result) {
        super(functionType, label, params, result);
        this.exportsSection = exportsSection;
        this.expressions = new ArrayList<>();
    }

    public ExportableFunction(final ExportsSection exportsSection,
            final FunctionType functionType, final String label,
            final List<Param> params) {
        super(functionType, label, params);
        this.exportsSection = exportsSection;
        this.expressions = new ArrayList<>();
    }

    public ExportableFunction(final ExportsSection exportsSection,
            final FunctionType functionType, final String label, final PrimitiveType result) {
        super(functionType, label, result);
        this.exportsSection = exportsSection;
        this.expressions = new ArrayList<>();
    }

    public void exportAs(final String functionName) {
        exportsSection.export(this, functionName);
    }

    public void addChild(final Expression expression) {
        expressions.add(expression);
    }

    @Override
    public void writeTo(final TextWriter textWriter) throws IOException {
        textWriter.opening();
        textWriter.write("func");
        textWriter.space();
        textWriter.writeLabel(getLabel());
        if (null != getParams()) {
            for (final Param param : getParams()) {
                textWriter.space();
                param.writeTo(textWriter);
            }
        }
        if (null != getResultType()) {
            textWriter.space();
            textWriter.opening();
            textWriter.write("result");
            textWriter.space();
            getResultType().writeTo(textWriter);
            textWriter.closing();
        }
        textWriter.newLine();
        for (final Expression expression : expressions) {
            expression.writeTo(textWriter);
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

    public void writeCodeTo(final BinaryWriter.SectionWriter sectionWriter) throws IOException {
        try (BinaryWriter.BlockWriter codeWriter = sectionWriter.blockWriter()) {
            // We assume zero locals here
            codeWriter.writeUnsignedLeb128(0);

            // Just an unreachable
            codeWriter.writeByte((byte) 0x00);

            // Finish
            codeWriter.writeByte((byte) 0x0b);
        }
    }
}
