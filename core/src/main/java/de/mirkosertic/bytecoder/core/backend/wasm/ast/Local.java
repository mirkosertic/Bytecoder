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

public class Local {

    private final String label;
    private final WasmType type;

    Local(final String label, final WasmType type) {
        this.label = label;
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public WasmType getType() {
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
