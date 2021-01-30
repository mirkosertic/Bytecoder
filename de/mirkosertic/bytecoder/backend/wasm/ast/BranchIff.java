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

public class BranchIff implements WASMExpression {

    private final LabeledContainer outerBlock;
    private final WASMValue condition;
    private final Expression expression;

    BranchIff(final LabeledContainer surroundingBlock, final WASMValue condition, final Expression expression) {
        this.outerBlock = surroundingBlock;
        this.condition = condition;
        this.expression = expression;
    }

    @Override
    public void writeTo(final TextWriter textWriter, final ExportContext context) throws IOException {
        textWriter.opening();
        textWriter.write("br_if");
        textWriter.space();
        textWriter.writeLabel(outerBlock.getLabel());

        textWriter.newLine();
        condition.writeTo(textWriter, context);

        textWriter.closing();
        textWriter.newLine();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer codeWriter, final ExportContext context) throws IOException {
        condition.writeTo(codeWriter, context);
        final int relativeDepth = context.owningContainer().relativeDepthTo(outerBlock);

        codeWriter.registerDebugInformationFor(expression);
        codeWriter.writeByte((byte) 0x0d);
        codeWriter.writeUnsignedLeb128(relativeDepth);
    }
}
