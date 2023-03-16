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

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class StructType implements ReferencableType {

    public static class Field {
        protected final String name;
        protected final WasmType type;

        protected final boolean mutable;

        public Field(final String name, final WasmType type) {
            this(name, type, true);
        }

        public Field(final String name, final WasmType type, final boolean mutable) {
            this.name = name;
            this.type = type;
            this.mutable = mutable;
        }

        public String getName() {
            return name;
        }

        public WasmType getType() {
            return type;
        }

        public void writeTo(final TextWriter writer) {
            writer.opening();
            writer.write("field");
            writer.space();
            writer.write("$");
            writer.write(name);
            writer.space();
            if (mutable) {
                writer.opening();
                writer.write("mut ");
                type.writeRefTo(writer);
                writer.closing();
            } else {
                type.writeRefTo(writer);
            }
            writer.closing();
        }
    }

    protected final TypesSection typesSection;
    protected final String name;
    protected final List<Field> fields;

    StructType(final TypesSection section, final String name, final List<Field> fields) {
        this.typesSection = section;
        this.name = name;
        this.fields = fields;
    }

    public String getName() {
        return name;
    }

    @Override
    public void writeTo(final TextWriter writer) {
        writer.opening();
        writer.write("type");
        writer.space();
        writer.write("$");
        writer.write(name);
        writer.space();
        writer.opening();
        writer.write("struct");
        for (final Field field : fields) {
            writer.space();
            field.writeTo(writer);
        }
        writer.closing();
        writer.closing();
    }

    @Override
    public void writeRefTo(final TextWriter writer) {
        writer.write("$");
        writer.write(name);
    }

    @Override
    public void writeTo(final BinaryWriter.Writer writer) throws IOException {
        writer.writeByte(PrimitiveType.struct.getBinaryType());
        writer.writeByte((byte) fields.size());
        for (final Field f : fields) {
            f.type.writeTo(writer);
            if (f.mutable) {
                writer.writeByte((byte) 0x01);
            } else {
                writer.writeByte((byte) 0x00);
            }
        }
    }

    @Override
    public int index() {
        return typesSection.indexOf(this);
    }

    public List<Field> getFields() {
        return fields;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final StructType that = (StructType) o;
        return Objects.equals(typesSection, that.typesSection) && Objects.equals(name, that.name) && Objects.equals(fields, that.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typesSection, name, fields);
    }
}
