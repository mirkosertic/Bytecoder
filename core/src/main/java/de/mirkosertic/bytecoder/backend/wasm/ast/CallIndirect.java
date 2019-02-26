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
import java.util.List;

public class CallIndirect implements WASMExpression {

    private final WASMType functionType;
    private final List<WASMValue> arguments;
    private final WASMValue functionIndex;
    private final Expression expression;

    CallIndirect(final WASMType functionType, final List<WASMValue> arguments, final WASMValue functionIndex, final Expression expression) {
        this.functionType = functionType;
        this.arguments = arguments;
        this.functionIndex = functionIndex;
        this.expression = expression;
    }

    @Override
    public void writeTo(final TextWriter textWriter, final ExportContext context) throws IOException {
        textWriter.opening();
        textWriter.write("call_indirect");
        textWriter.space();
        functionType.writeRefTo(textWriter);
        for (final WASMValue argument : arguments) {
            textWriter.space();
            argument.writeTo(textWriter, context);
        }
        textWriter.space();
        functionIndex.writeTo(textWriter, context);
        textWriter.closing();
        if (functionType.isVoid()) {
            textWriter.newLine();
        }
    }

    @Override
    public void writeTo(final BinaryWriter.Writer codeWriter, final ExportContext context) throws IOException {
        for (final WASMValue argument : arguments) {
            argument.writeTo(codeWriter, context);
        }
        functionIndex.writeTo(codeWriter, context);
        codeWriter.registerDebugInformationFor(expression);
        codeWriter.writeByte((byte) 0x11);
        codeWriter.writeUnsignedLeb128(context.typeIndex().indexOf(functionType));
        codeWriter.writeByte((byte) 0);
    }
}