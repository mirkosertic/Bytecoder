/*
 * Copyright 2023 Mirko Sertic
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

public class ArrayType implements ReferencableType {

    private final TypesSection section;
    private final WasmType elementType;

    ArrayType(final TypesSection section, final WasmType elementType) {
        this.section = section;
        this.elementType = elementType;
    }

    public WasmType getElementType() {
        return elementType;
    }

    @Override
    public void writeTo(final TextWriter writer) {
        writer.opening();
        writer.write("type");
        writer.space();
        writer.write("$t");
        writer.write(Integer.toString(index()));
        writer.space();
        writer.opening();
        writer.write("array");
        writer.space();
        writer.opening();
        writer.write("mut");
        writer.space();
        this.elementType.writeRefTo(writer);
        writer.closing();
        writer.closing();
        writer.closing();
    }

    @Override
    public void writeRefTo(final TextWriter writer) {
        writer.write("$t");
        writer.write(Integer.toString(index()));
    }

    @Override
    public void writeTo(final BinaryWriter.Writer writer) {
        // TODO
    }

    @Override
    public int index() {
        return section.indexOf(this);
    }
}
