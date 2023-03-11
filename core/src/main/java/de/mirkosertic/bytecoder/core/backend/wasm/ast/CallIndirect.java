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

public class CallIndirect implements WasmExpression {

    private final FunctionType functionType;
    private final List<WasmValue> arguments;
    private final WasmValue functionIndex;

    CallIndirect(final FunctionType functionType, final List<WasmValue> arguments, final WasmValue functionIndex) {
        this.functionType = functionType;
        this.arguments = arguments;
        this.functionIndex = functionIndex;
    }

    @Override
    public void writeTo(final TextWriter textWriter, final ExportContext context) throws IOException {
        textWriter.opening();
        textWriter.write("call_indirect");
        textWriter.space();
        textWriter.opening();
        textWriter.write("type");
        textWriter.space();
        functionType.writeRefTo(textWriter);
        textWriter.closing();
        for (final WasmValue argument : arguments) {
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
        for (final WasmValue argument : arguments) {
            argument.writeTo(codeWriter, context);
        }
        functionIndex.writeTo(codeWriter, context);
        codeWriter.writeByte((byte) 0x11);
        codeWriter.writeUnsignedLeb128(context.typeIndex().indexOf(functionType));
        codeWriter.writeByte((byte) 0);
    }
}
