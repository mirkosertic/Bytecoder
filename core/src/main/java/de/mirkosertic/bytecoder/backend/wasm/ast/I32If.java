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

public class I32If extends Expression implements I32 {

    public enum Condition {
        eq
    }

    static I32If eq(final I32 leftValue, final I32 rightValue) {
        return new I32If(Condition.eq, leftValue, rightValue);
    }

    private I32If(final Condition condition, final I32 singleValue) {
        super("i32." + condition);
        addChildInternal(singleValue);
    }

    private I32If(final Condition condition, final I32 leftValue, final I32 rightValue) {
        super("i32." + condition);
        addChildInternal(leftValue);
        addChildInternal(rightValue);
    }
}
