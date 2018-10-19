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

public class Branch extends Expression {

    private final Block outerBlock;

    Branch(final Block surroundingBlock) {
        super("br");
        this.outerBlock = surroundingBlock;
    }

    @Override
    public void writeTo(final TextWriter textWriter) {
        textWriter.opening();
        textWriter.write("br");
        textWriter.space();
        textWriter.writeLabel(outerBlock.getLabel());
        textWriter.closing();
    }
}
