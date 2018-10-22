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

public class I32Condition extends Expression implements Value {

    public enum Condition {
        eq
    }

    static I32Condition eq(final Value leftValue, final Value rightValue) {
        return new I32Condition(Condition.eq, leftValue, rightValue);
    }

    private I32Condition(final Condition condition, final Value singleValue) {
        super("i32." + condition);
        addChildInternal(singleValue);
    }

    private I32Condition(final Condition condition, final Value leftValue, final Value rightValue) {
        super("i32." + condition);
        addChildInternal(leftValue);
        addChildInternal(rightValue);
    }

    public void addChild(final Expression expression) {
        addChildInternal(expression);
    }

    @Override
    public void writeTo(final BinaryWriter.Writer codeWriter) throws IOException {
        throw new RuntimeException("Not implemented!");
    }
}