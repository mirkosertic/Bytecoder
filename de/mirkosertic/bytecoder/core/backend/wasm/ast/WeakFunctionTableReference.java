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

public class WeakFunctionTableReference implements WasmValue {

    private final String functionName;

    WeakFunctionTableReference(final String functionName) {
        this.functionName = functionName;
    }

    @Override
    public void writeTo(final TextWriter textWriter, final ExportContext context) {
        final Function f = context.functionIndex().firstByLabel(functionName);
        final int theIndex = context.anyFuncTable().indexOf(f);
        if (theIndex < 0) {
            throw new IllegalStateException("Cannot call function that is not part of the table : " + functionName + " : " + f);
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
        final I32Const c = new I32Const(theIndex);
        c.writeTo(codeWriter, context);
    }
}
