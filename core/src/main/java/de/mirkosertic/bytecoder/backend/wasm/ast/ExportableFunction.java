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

    private final ExportsContent exportsContent;
    private final List<Expression> expressions;

    public ExportableFunction(final ExportsContent exportsContent,
            final FunctionType functionType, final String label,
            final List<Param> params, final PrimitiveType result) {
        super(functionType, label, params, result);
        this.exportsContent = exportsContent;
        this.expressions = new ArrayList<>();
    }

    public ExportableFunction(final ExportsContent exportsContent,
            final FunctionType functionType, final String label,
            final List<Param> params) {
        super(functionType, label, params);
        this.exportsContent = exportsContent;
        this.expressions = new ArrayList<>();
    }

    public ExportableFunction(final ExportsContent exportsContent,
            final FunctionType functionType, final String label, final PrimitiveType result) {
        super(functionType, label, result);
        this.exportsContent = exportsContent;
        this.expressions = new ArrayList<>();
    }

    public void exportAs(final String functionName) {
        exportsContent.export(this, functionName);
    }

    public void addChild(final Expression expression) {
        expressions.add(expression);
    }

    @Override
    public void writeTo(final TextWriter textWriter) throws IOException {
        textWriter.opening();
        textWriter.write("func");
        textWriter.space();
        textWriter.writeLabel(label);
        if (null != params) {
            for (final Param param : params) {
                textWriter.space();
                param.writeTo(textWriter);
            }
        }
        if (null != resultType) {
            textWriter.space();
            textWriter.opening();
            textWriter.write("result");
            textWriter.space();
            resultType.writeTo(textWriter);
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
        textWriter.writeLabel(label);
        textWriter.closing();
    }
}
