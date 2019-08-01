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

import java.util.Stack;

public abstract class StructuredControlFlowWriter<T> {

    protected final Stack<Block<T>> hierarchy;

    protected StructuredControlFlowWriter() {
        this.hierarchy = new Stack<>();
    }

    public void begin() {
    }

    public void beginLoopFor(final Block<T> block) {
        hierarchy.push(block);
    }

    public void beginBlockFor(final Block<T> block) {
        hierarchy.push(block);
    }

    public abstract void write(T node);

    public void closeBlock() {
        hierarchy.pop();
    }

    public void end() {
        if (!hierarchy.isEmpty()) {
            throw new IllegalStateException("Hierarchy must be empty at the end of output!");
        }
    }
}
