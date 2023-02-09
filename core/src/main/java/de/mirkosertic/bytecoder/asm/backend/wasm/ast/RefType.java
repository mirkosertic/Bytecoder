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
package de.mirkosertic.bytecoder.asm.backend.wasm.ast;

import java.util.Objects;

public class RefType implements WasmType {

    private final ReferencableType type;

    private final boolean nullable;

    RefType(final ReferencableType type, final boolean nullable) {
        this.type = type;
        this.nullable = nullable;
    }

    @Override
    public void writeTo(final TextWriter writer) {
        writer.opening();
        writer.write("ref ");
        if (nullable) {
            writer.write("null ");
        }
        type.writeRefTo(writer);
        writer.closing();
    }

    @Override
    public void writeRefTo(final TextWriter writer) {
        writer.opening();
        if (type instanceof StructType) {
            final StructType s = (StructType) type;
            if (nullable) {
                writer.write("ref null $");
            } else {
                writer.write("ref $");
            }
            writer.write(s.getName());
        } else {
            if (nullable) {
                writer.write("ref null $t");
            } else {
                writer.write("ref $t");
            }
            writer.write(Integer.toString(type.index()));
        }
        writer.closing();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer writer) {
        // Do be implemented
    }

    @Override
    public int index() {
        throw new IllegalArgumentException("Not implemented");
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final RefType refType = (RefType) o;
        return nullable == refType.nullable && type.equals(refType.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, nullable);
    }
}
