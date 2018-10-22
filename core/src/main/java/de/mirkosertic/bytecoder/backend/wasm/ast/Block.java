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

public class Block extends Expression {

    private final Block parent;
    private final String label;

    Block(final String label, final Block parent) {
        super("block");
        this.label = label;
        this.parent = parent;
    }

    Block(final String label) {
        this(label, null);
    }

    public Block nestedBlock(final String label) {
        final Block nested = new Block(label, this);
        addChildInternal(nested);
        return nested;
    }

    public String getLabel() {
        return label;
    }

    public void addChild(final Expression child) {
        addChildInternal(child);
    }

    @Override
    public void writeTo(final TextWriter textWriter) throws IOException {
        textWriter.opening();
        textWriter.write("block");
        textWriter.space();
        textWriter.writeLabel(label);
        if (hasChildren()) {
            textWriter.newLine();
            for (final Value child : children()) {
                child.writeTo(textWriter);
                textWriter.newLine();
            }
            textWriter.closing();
        } else {
            textWriter.closing();
        }
        textWriter.newLine();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer codeWriter) throws IOException {
        codeWriter.writeByte((byte) 0x02);
        PrimitiveType.empty_pseudo_block.writeTo(codeWriter);
        for (final Value e : children()) {
            e.writeTo(codeWriter);
        }
        codeWriter.writeByte((byte) 0x0b);
    }
}