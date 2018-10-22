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
import java.util.ArrayList;
import java.util.List;

public abstract class Expression implements Value {

    private final String name;
    private final List<Value> children;

    protected Expression(final String name) {
        this.children = new ArrayList<>();
        this.name = name;
    }

    @Override
    public void writeTo(final TextWriter textWriter) throws IOException {
        textWriter.opening();
        textWriter.write(name);
        if (hasChildren()) {
            for (final Value child : children()) {
                if (child instanceof Expression) {
                    textWriter.newLine();
                } else {
                    textWriter.space();
                }
                child.writeTo(textWriter);
            }
            textWriter.closing();
        } else {
            textWriter.closing();
        }
    }

    protected final void addChildInternal(final Value child) {
        children.add(child);
    }

    protected List<Value> children() {
        return children;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }
}