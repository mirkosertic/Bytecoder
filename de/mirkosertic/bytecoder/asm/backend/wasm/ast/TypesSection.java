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
package de.mirkosertic.bytecoder.asm.backend.wasm.ast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TypesSection extends ModuleSection {

    private final List<WasmType> types;

    TypesSection(final Module aModule) {
        super(aModule);
        this.types = new ArrayList<>();
    }

    public FunctionType functionType(final List<WasmType> arguments, final WasmType returnType) {
        // We search for the same type
        for (final WasmType type : types) {
            if (type instanceof FunctionType) {
                final FunctionType otherType = (FunctionType) type;
                if (otherType.matches(arguments, returnType)) {
                    return otherType;
                }
            }
        }
        // Register a new one
        return register(new FunctionType(this, arguments, returnType));
    }

    public FunctionType functionType(final List<WasmType> arguments) {
        // We search for the same type
        for (final WasmType type : types) {
            if (type instanceof FunctionType) {
                final FunctionType otherType = (FunctionType) type;
                if (otherType.matches(arguments)) {
                    return otherType;
                }
            }
        }
        // Register a new one
        return register(new FunctionType(this, arguments));
    }

    public FunctionType functionType(final WasmType returnType) {
        // We search for the same type
        for (final WasmType type : types) {
            if (type instanceof FunctionType) {
                final FunctionType otherType = (FunctionType) type;
                if (otherType.matches(returnType)) {
                    return otherType;
                }
            }
        }
        // Register a new one
        return register(new FunctionType(this, returnType));
    }

    public StructType structType(final String name, final List<StructType.Field> fields) {
        return register(new StructType(this, name, fields));
    }

    public StructSubtype structSubtype(final String name, final StructType superType, final List<StructType.Field> fields) {
        return register(new StructSubtype(this, name, superType, fields));
    }

    public ArrayType arrayType(final WasmType elementType) {
        return register(new ArrayType(this, elementType));
    }

    <T extends WasmType> T register(final T wasmType) {
        if (!types.contains(wasmType)) {
            types.add(wasmType);
        }
        return wasmType;
    }

    public int indexOf(final WasmType type) {
        return types.indexOf(type);
    }

    public void writeTo(final TextWriter textWriter) {
        for (final WasmType type : types) {
            type.writeTo(textWriter);
            textWriter.newLine();
        }
    }

    public void writeTo(final BinaryWriter binaryWriter) throws IOException {
        try (final BinaryWriter.SectionWriter writer = binaryWriter.typeSection()) {
            writer.writeUnsignedLeb128(types.size());
            for (final WasmType type : types) {
                type.writeTo(writer);
            }
        }
    }

    public TypeIndex typesIndex() {
        final TypeIndex result = new TypeIndex();
        for (final WasmType t : types) {
            result.add(t);
        }
        return result;
    }
}
