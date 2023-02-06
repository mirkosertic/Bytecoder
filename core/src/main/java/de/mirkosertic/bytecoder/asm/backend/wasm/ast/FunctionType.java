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

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class FunctionType implements ReferencableType {

    private final TypesSection typesSection;
    private final List<WasmType> parameter;
    private final WasmType resultType;

    FunctionType(final TypesSection section, final List<WasmType> parameter, final WasmType resultType) {
        this.typesSection = section;
        this.parameter = parameter;
        this.resultType = resultType;
    }

    FunctionType(final TypesSection section, final List<WasmType> parameter) {
        this.typesSection = section;
        this.parameter = parameter;
        this.resultType = null;
    }

    FunctionType(final TypesSection section, final WasmType resultType) {
        this.typesSection = section;
        this.parameter = null;
        this.resultType = resultType;
    }

    public boolean matches(final List<WasmType> otherParameter, final WasmType otherResultType) {
        return Objects.equals(parameter, otherParameter)
                && Objects.equals(resultType, otherResultType);
    }

    public boolean isVoid() {
        return resultType == null;
    }

    public WasmType getResultType() {
        return resultType;
    }

    @Override
    public void writeTo(final TextWriter writer) {
        writer.opening();
        writer.write("type");
        writer.space();
        writer.write("$t");
        writer.write(Integer.toString(typesSection.indexOf(this)));
        writer.space();
        writer.opening();
        writer.write("func");
        if (null != parameter) {
            for (final WasmType param : parameter) {
                writer.space();
                writer.opening();
                writer.write("param");
                writer.space();
                param.writeTo(writer);
                writer.closing();
            }
        }
        if (null != resultType) {
            writer.space();
            writer.opening();
            writer.write("result");
            writer.space();
            resultType.writeTo(writer);
            writer.closing();
        }
        writer.closing();
        writer.closing();
    }

    @Override
    public void writeRefTo(final TextWriter writer) {
        writer.write("$t");
        writer.write(Integer.toString(index()));
    }

    @Override
    public void writeTo(final BinaryWriter.Writer writer) throws IOException {
        writer.writeByte(PrimitiveType.func.getBinaryType());
        if (null != parameter) {
            writer.writeUnsignedLeb128(parameter.size());
            for (final WasmType type : parameter) {
                type.writeTo(writer);
            }
        } else {
            writer.writeUnsignedLeb128(0);
        }
        if (null != resultType) {
            writer.writeUnsignedLeb128(1);
            resultType.writeTo(writer);
        } else {
            writer.writeUnsignedLeb128(0);
        }
    }

    @Override
    public int index() {
        return typesSection.indexOf(this);
    }
}
