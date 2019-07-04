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
import java.util.List;
import java.util.Objects;

public class WASMType {

    private final TypesSection typesSection;
    private final List<PrimitiveType> parameter;
    private final PrimitiveType resultType;

    WASMType(final TypesSection section, final List<PrimitiveType> parameter, final PrimitiveType resultType) {
        this.typesSection = section;
        this.parameter = parameter;
        this.resultType = resultType;
    }

    WASMType(final TypesSection section, final List<PrimitiveType> parameter) {
        this.typesSection = section;
        this.parameter = parameter;
        this.resultType = null;
    }

    WASMType(final TypesSection section, final PrimitiveType resultType) {
        this.typesSection = section;
        this.parameter = null;
        this.resultType = resultType;
    }

    public boolean matches(final List<PrimitiveType> otherParameter, final PrimitiveType otherResultType) {
        return Objects.equals(parameter, otherParameter)
                && Objects.equals(resultType, otherResultType);
    }

    public boolean isVoid() {
        return resultType == null;
    }

    public PrimitiveType getResultType() {
        return resultType;
    }

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
            for (final PrimitiveType param : parameter) {
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

    public void writeRefTo(final TextWriter writer) {
        writer.opening();
        writer.write("type");
        writer.space();
        writer.write("$t");
        writer.write(Integer.toString(typesSection.indexOf(this)));
        writer.closing();
    }

    public void writeTo(final BinaryWriter.SectionWriter sectionWriter) throws IOException {
        sectionWriter.writeByte(PrimitiveType.func.getBinaryType());
        if (null != parameter) {
            sectionWriter.writeUnsignedLeb128(parameter.size());
            for (final PrimitiveType type : parameter) {
                type.writeTo(sectionWriter);
            }
        } else {
            sectionWriter.writeUnsignedLeb128(0);
        }
        if (null != resultType) {
            sectionWriter.writeUnsignedLeb128(1);
            resultType.writeTo(sectionWriter);
        } else {
            sectionWriter.writeUnsignedLeb128(0);
        }
    }

    public int index() {
        return typesSection.indexOf(this);
    }
}