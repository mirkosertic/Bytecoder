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

public enum Expressions {
    ;

    public enum i32 {
        ;

        public static I32Const c(final int aValue) {
            return new I32Const(aValue);
        }

        public static I32If eq(final I32 leftValue, final I32 rightValue) {
            return I32If.eq(leftValue, rightValue);
        }
    }

    public enum control {
        ;

        public static Block block(final String label) {
            return new Block(label);
        }

        public static Branch branchOutOf(final Block block) {
            return new Branch(block);
        }

        public static BranchIf branchOutIf(final Block block, final I32 condition) {
            return new BranchIf(block, condition);
        }

        public static Return ret(final Value value) {
            return new Return(value);
        }

        public static Return ret() {
            return new Return();
        }
    }
}
