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

import de.mirkosertic.bytecoder.ssa.Expression;

import java.io.IOException;

public class WeakFunctionTableReference implements WASMValue {

    private final String functionName;
    private final Expression expression;

    WeakFunctionTableReference(final String functionName, final Expression expression) {
        this.functionName = functionName;
        this.expression = expression;
    }

    @Override
    public void writeTo(final TextWriter textWriter, final ExportContext context) throws IOException {
        final Function f = context.functionIndex().firstByLabel(functionName);
        final int theIndex = context.anyFuncTable().indexOf(f);
        if (theIndex < 0) {
            throw new IllegalStateException("Cannot call function that is not part of the table : " + functionName);
        }
        textWriter.opening();
        textWriter.write("i32.const");
        textWriter.space();
        textWriter.writeInteger(theIndex);
        textWriter.closing();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer codeWriter, final ExportContext context) throws IOException {
        final Function f = context.functionIndex().firstByLabel(functionName);
        final int theIndex = context.anyFuncTable().indexOf(f);
        if (theIndex < 0) {
            throw new IllegalStateException("Cannot call function that is not part of the table : " + functionName);
        }
        codeWriter.registerDebugInformationFor(expression);
        final I32Const c = new I32Const(theIndex, expression);
        c.writeTo(codeWriter, context);
    }
}
