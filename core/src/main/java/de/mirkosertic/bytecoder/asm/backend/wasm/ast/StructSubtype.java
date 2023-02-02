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

import java.util.ArrayList;
import java.util.List;

public class StructSubtype extends StructType {

    private static List<Field> join(final List<Field> a, final List<Field>b) {
        final ArrayList<Field> f = new ArrayList<>();
        f.addAll(a);
        f.addAll(b);
        return f;
    }

    protected final StructType supertype;

    StructSubtype(final TypesSection section, final String name, final StructType supertype, final List<Field> fields) {
        super(section, name, join(supertype.getFields(), fields));
        this.supertype = supertype;
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
        writer.write("struct_subtype");
        for (final Field field : fields) {
            writer.space();
            field.writeTo(writer);
        }
        writer.space();
        writer.write("$");
        writer.write(supertype.name);
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
        // Do be implemented!
    }
}
