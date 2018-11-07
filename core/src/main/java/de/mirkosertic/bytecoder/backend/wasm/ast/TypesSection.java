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

public class TypesSection extends ModuleSection {

    private final List<WASMType> types;

    TypesSection(final Module aModule) {
        super(aModule);
        this.types = new ArrayList<>();
    }

    public WASMType typeFor(final List<PrimitiveType> parameter, final PrimitiveType resultType) {
        for (final WASMType known : types) {
            if (known.matches(parameter, resultType)) {
                return known;
            }
        }
        final WASMType type = new WASMType(this, parameter, resultType);
        types.add(type);
        return type;
    }

    public WASMType typeFor(final List<PrimitiveType> parameter) {
        for (final WASMType known : types) {
            if (known.matches(parameter, null)) {
                return known;
            }
        }
        final WASMType type = new WASMType(this, parameter);
        types.add(type);
        return type;
    }

    public WASMType typeFor(final PrimitiveType resultType) {
        for (final WASMType known : types) {
            if (known.matches(null, resultType)) {
                return known;
            }
        }
        final WASMType type = new WASMType(this, resultType);
        types.add(type);
        return type;
    }

    int indexOf(final WASMType type) {
        return types.indexOf(type);
    }

    public void writeTo(final TextWriter textWriter) {
        for (final WASMType type : types) {
            type.writeTo(textWriter);
            textWriter.newLine();
        }
    }

    public void writeTo(final BinaryWriter binaryWriter) throws IOException {
        try (final BinaryWriter.SectionWriter writer = binaryWriter.typeSection()) {
            writer.writeUnsignedLeb128(types.size());
            for (final WASMType type : types) {
                type.writeTo(writer);
            }
        }
    }

    public TypeIndex typesIndex() {
        final TypeIndex result = new TypeIndex();
        for (final WASMType t : types) {
            result.add(t);
        }
        return result;
    }
}