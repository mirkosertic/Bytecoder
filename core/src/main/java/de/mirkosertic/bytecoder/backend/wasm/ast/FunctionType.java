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

import java.util.List;
import java.util.Objects;

public class FunctionType {

    private final TypesContent functionsContent;
    private final List<PrimitiveType> parameter;
    private final PrimitiveType resultType;

    FunctionType(final TypesContent content, final List<PrimitiveType> parameter, final PrimitiveType resultType) {
        this.functionsContent = content;
        this.parameter = parameter;
        this.resultType = resultType;
    }

    FunctionType(final TypesContent content, final List<PrimitiveType> parameter) {
        this.functionsContent = content;
        this.parameter = parameter;
        this.resultType = null;
    }

    FunctionType(final TypesContent content, final PrimitiveType resultType) {
        this.functionsContent = content;
        this.parameter = null;
        this.resultType = resultType;
    }

    public boolean matches(final List<PrimitiveType> otherParameter, final PrimitiveType otherResultType) {
        if (null != parameter && null != resultType) {
            return parameter.equals(otherParameter) == resultType.equals(otherResultType);
        }
        if (null == parameter) {
            return null == otherParameter && Objects.equals(resultType, otherResultType);
        }
        return null == resultType && Objects.equals(parameter, otherParameter);
    }

    public void writeTo(final TextWriter writer) {
        writer.opening();
        writer.write("type");
        writer.space();
        writer.write("$t");
        writer.write(Integer.toString(functionsContent.indexOf(this)));
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

    public void writeTo(final BinaryWriter.SectionWriter sectionWriter) {
    }
}