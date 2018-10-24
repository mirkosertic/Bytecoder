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

    public static Param param(final String label, final PrimitiveType type) {
        return new Param(label, type);
    }

    public static I32Const c(final int aValue) {
        return new I32Const(aValue);
    }

    public static I32Condition i32eq(final Value leftValue, final Value rightValue) {
        return I32Condition.eq(leftValue, rightValue);
    }

    public static Call call(final Function function, final Value... arguments) {
        return new Call(function, arguments);
    }

    public static I32Add i32Add(final Value leftValue, final Value rightValue) {
        return new I32Add(leftValue, rightValue);
    }

    public static GetLocal getLocal(final Local local) {
        return new GetLocal(local);
    }

    public static GetGlobal getGlobal(final Global global) {
        return new GetGlobal(global);
    }

    private final Container parent;

    Expressions(final Container parent) {
        this.parent = parent;
    }

    public void voidCall(final Function function, final Value... arguments) {
        final Call call = new Call(function, arguments);
        parent.addChild(call);
    }

    public Block block(final String label) {
        final Block block = new Block(label, parent);
        parent.addChild(block);
        return block;
    }

    public void branchOutOf(final Block surroundingBlock) {
        final Branch branch = new Branch(surroundingBlock);
        parent.addChild(branch);
    }

    public void branchOutIf(final Block block, final Value condition) {
        final BranchIf branch = new BranchIf(block, condition);
        parent.addChild(branch);
    }

    public void ret(final Value value) {
        parent.addChild(new Return(value));
    }

    public void ret() {
        parent.addChild(new Return());
    }

    public I32IF i32if(final I32Condition condition) {
        final I32IF elem = new I32IF(parent, condition);
        parent.addChild(elem);
        return elem;
    }

    public I32IF i32ifeq(final Value leftValue, final Value rightValue) {
        final I32Condition condition = i32eq(leftValue, rightValue);
        return i32if(condition);
    }

    public void unreachable() {
        parent.addChild(new Unreachable());
    }

    public void setLocal(final Local local, final Value value) {
        final SetLocal setLocal = new SetLocal(local, value);
        parent.addChild(setLocal);
    }
}