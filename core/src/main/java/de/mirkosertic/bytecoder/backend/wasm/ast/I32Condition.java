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

public class I32Condition implements Expression {

    public enum Condition {
        eq
    }

    static I32Condition eq(final Value leftValue, final Value rightValue) {
        return new I32Condition(Condition.eq, leftValue, rightValue);
    }

    private final Condition condition;
    private final Value leftValue;
    private final Value rightValue;

    private I32Condition(final Condition condition, final Value singleValue) {
        this.condition = condition;
        this.leftValue = singleValue;
        this.rightValue = null;
    }

    private I32Condition(final Condition condition, final Value leftValue, final Value rightValue) {
        this.condition = condition;
        this.leftValue = leftValue;
        this.rightValue = rightValue;
    }

    @Override
    public void writeTo(final TextWriter textWriter, final ExportableFunction exportableFunction) throws IOException {
        textWriter.opening();
        textWriter.write("i32." + condition);
        if (leftValue != null) {
            textWriter.space();
            leftValue.writeTo(textWriter, exportableFunction);
        }
        if (rightValue != null) {
            textWriter.space();
            rightValue.writeTo(textWriter, exportableFunction);
        }
        textWriter.closing();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer codeWriter, final Container owningContainer, final ExportableFunction exportableFunction) throws IOException {
        if (leftValue != null) {
            leftValue.writeTo(codeWriter, owningContainer, exportableFunction);
        }
        if (rightValue != null) {
            rightValue.writeTo(codeWriter, owningContainer, exportableFunction);
        }
        switch (condition) {
            case eq: {
                codeWriter.writeByte((byte) 0x46);
                break;
            }
            default: {
                throw new RuntimeException("Not implemented : " + condition);
            }
        }
    }
}