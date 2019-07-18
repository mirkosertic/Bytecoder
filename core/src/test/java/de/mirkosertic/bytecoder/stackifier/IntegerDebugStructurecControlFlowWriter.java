/*
 * Copyright 2019 Mirko Sertic
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
package de.mirkosertic.bytecoder.stackifier;

import java.io.PrintStream;

public class IntegerDebugStructurecControlFlowWriter extends StructuredControlFlowWriter<Integer> {

    private final PrintStream stream;

    public IntegerDebugStructurecControlFlowWriter(final PrintStream stream) {
        this.stream = stream;
    }

    private String indent(final int l) {
        final StringBuilder b = new StringBuilder();
        for (int i=0;i<l;i++) {
            b.append("    ");
        }
        return b.toString();
    }

    @Override
    public void beginLoopFor(final Block<Integer> block) {
        stream.print(indent(hierarchy.size()));
        stream.print("LOOP: {");
        stream.println();
        super.beginLoopFor(block);
    }

    @Override
    public void beginBlockFor(final Block<Integer> block) {
        stream.print(indent(hierarchy.size()));
        stream.print("BLOCK: {");
        stream.println();
        super.beginBlockFor(block);
    }

    @Override
    public void write(final Integer node) {
        stream.print(indent(hierarchy.size()));
        stream.println(String.format("%d", node));
    }

    @Override
    public void closeBlock() {
        super.closeBlock();
        stream.print(indent(hierarchy.size()));
        stream.println("}");
    }
}
