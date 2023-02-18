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
package de.mirkosertic.bytecoder.asm.backend.wasm.ast;

import java.io.IOException;

public class BranchOnException implements WasmExpression {

    private final LabeledContainer targetContainer;
    private final Tag exceptionType;

    BranchOnException(final LabeledContainer targetContainer, final Tag exceptionType) {
        this.targetContainer = targetContainer;
        this.exceptionType = exceptionType;
    }

    @Override
    public void writeTo(final TextWriter textWriter, final ExportContext context) {
        textWriter.opening();
        textWriter.write("br_on_exn");
        textWriter.space();
        textWriter.writeLabel(targetContainer.getLabel());
        textWriter.space();
        textWriter.writeLabel(exceptionType.getLabel());
        textWriter.closing();
        textWriter.newLine();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer codeWriter, final ExportContext context) throws IOException {
        final int relativeDepth = context.owningContainer().relativeDepthTo(targetContainer);
        codeWriter.writeByte((byte) 0x0a);
        codeWriter.writeUnsignedLeb128(relativeDepth);
        codeWriter.writeUnsignedLeb128(context.tagIndex().indexOf(exceptionType));
    }
}
