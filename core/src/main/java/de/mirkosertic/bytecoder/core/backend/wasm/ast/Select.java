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

public class Select implements WasmExpression {

    private final WasmValue leftValue;
    private final WasmValue rightValue;
    private final WasmValue condition;

    Select(final WasmValue leftValue, final WasmValue rightValue, final WasmValue condition) {
        this.leftValue = leftValue;
        this.rightValue = rightValue;
        this.condition = condition;
    }

    @Override
    public void writeTo(final TextWriter textWriter, final ExportContext context) throws IOException {
        textWriter.opening();
        textWriter.write("select");
        textWriter.space();
        leftValue.writeTo(textWriter, context);
        textWriter.space();
        rightValue.writeTo(textWriter, context);
        textWriter.space();
        condition.writeTo(textWriter, context);
        textWriter.closing();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer codeWriter, final ExportContext context) throws IOException {
        leftValue.writeTo(codeWriter, context);
        rightValue.writeTo(codeWriter, context);
        condition.writeTo(codeWriter, context);
        codeWriter.writeByte((byte) 0x1b);
    }
}
