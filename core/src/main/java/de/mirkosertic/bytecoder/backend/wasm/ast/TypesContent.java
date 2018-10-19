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

import java.util.ArrayList;
import java.util.List;

public class TypesContent implements ModuleContent {

    private final List<FunctionType> types;

    public TypesContent() {
        this.types = new ArrayList<>();
    }

    public FunctionType typeFor(final List<PrimitiveType> parameter, final PrimitiveType resultType) {
        for (final FunctionType known : types) {
            if (known.matches(parameter, resultType)) {
                return known;
            }
        }
        final FunctionType type = new FunctionType(this, parameter, resultType);
        types.add(type);
        return type;
    }

    public FunctionType typeFor(final List<PrimitiveType> parameter) {
        for (final FunctionType known : types) {
            if (known.matches(parameter, null)) {
                return known;
            }
        }
        final FunctionType type = new FunctionType(this, parameter);
        types.add(type);
        return type;
    }

    public FunctionType typeFor(final PrimitiveType resultType) {
        for (final FunctionType known : types) {
            if (known.matches(null, resultType)) {
                return known;
            }
        }
        final FunctionType type = new FunctionType(this, resultType);
        types.add(type);
        return type;
    }

    int indexOf(final FunctionType functionType) {
        return types.indexOf(functionType);
    }

    @Override
    public void writeTo(final TextWriter writer) {
        for (final FunctionType type : types) {
            type.writeTo(writer);
            writer.newLine();
        }
    }

    @Override
    public void writeTo(final BinaryWriter binaryWriter) throws Exception {
        try (final BinaryWriter.SectionWriter writer = binaryWriter.typeSection()) {
            for (final FunctionType type : types) {
                type.writeTo(writer);
            }
        }
    }
}