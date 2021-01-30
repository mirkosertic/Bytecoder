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

public class Local {

    private String label;
    private final PrimitiveType type;

    Local(final String label, final PrimitiveType type) {
        this.label = label;
        this.type = type;
    }

    public void renameTo(final String newLabel) {
        label = newLabel;
    }

    public String getLabel() {
        return label;
    }

    public PrimitiveType getType() {
        return type;
    }

    public void writeTo(final TextWriter textWriter) {
        textWriter.opening();
        textWriter.write("local");
        textWriter.space();
        textWriter.writeLabel(getLabel());
        textWriter.space();
        getType().writeTo(textWriter);
        textWriter.closing();
    }
}
