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

public class Expressions {

    public class I32Expressions {

        public I32Const c(final int aValue) {
            return new I32Const(aValue);
        }

        public I32Condition i32eq(final Value leftValue, final Value rightValue) {
            return I32Condition.eq(leftValue, rightValue);
        }
    }

    public class ControlExpressions {

        public Block block(final String label) {
            return new Block(label);
        }

        public Branch branchOutOf(final Block block) {
            return new Branch(block);
        }

        public BranchIf branchOutIf(final Block block, final Value condition) {
            return new BranchIf(block, condition);
        }

        public Return ret(final Value value) {
            return new Return(value);
        }

        public Return ret() {
            return new Return();
        }

        public I32IF i32if(final I32Condition condition) {
            return new I32IF(condition);
        }

        public I32IF i32ifeq(final Value leftValue, final Value rightValue) {
            final I32Condition condition = Expressions.this.i32.i32eq(leftValue, rightValue);
            return i32if(condition);
        }

        public Unreachable unreachable() {
            return new Unreachable();
        }
    }

    public class VariableExpressions {

        public GetLocal getLocal(final Local local) {
            return new GetLocal(local, function);
        }
    }

    private final ExportableFunction function;
    public final I32Expressions i32;
    public final ControlExpressions control;
    public final VariableExpressions var;

    Expressions(final ExportableFunction function) {
        this.function = function;
        this.i32 = new I32Expressions();
        this.control = new ControlExpressions();
        this.var = new VariableExpressions();
    }
}
