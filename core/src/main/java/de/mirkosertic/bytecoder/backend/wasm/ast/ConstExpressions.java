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

public class ConstExpressions {

    public static Param param(final String label, final PrimitiveType type) {
        return new Param(label, type);
    }

    public static Call call(final Function function, final List<Value> arguments) {
        return new Call(function, arguments);
    }

    public static class i32 {
        public static I32Const c(final int aValue) {
            return new I32Const(aValue);
        }

        public static I32Condition eq(final Value leftValue, final Value rightValue) {
            return I32Condition.eq(leftValue, rightValue);
        }

        public static I32Add add(final Value leftValue, final Value rightValue) {
            return new I32Add(leftValue, rightValue);
        }

        public static I32Sub sub(final Value leftValue, final Value rightValue) {
            return new I32Sub(leftValue, rightValue);
        }

        public static I32Mul mul(final Value leftValue, final Value rightValue) {
            return new I32Mul(leftValue, rightValue);
        }
    }

    public static class f32 {

        public static F32Condition ne(final Value leftValue, final Value rightValue) {
            return F32Condition.ne(leftValue, rightValue);
        }

        public static F32Condition ge(final Value leftValue, final Value rightValue) {
            return F32Condition.ge(leftValue, rightValue);
        }
    }

    public static GetLocal getLocal(final Local local) {
        return new GetLocal(local);
    }

    public static GetGlobal getGlobal(final Global global) {
        return new GetGlobal(global);
    }

    public static CurrentMemory currentMemory() {
        return new CurrentMemory();
    }
}
