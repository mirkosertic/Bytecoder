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
package de.mirkosertic.bytecoder.asm.backend.wasm.ast;

import java.util.List;

public class Expressions {

    public class Array {

        public void set(final WasmType type, final WasmValue array, final WasmValue index, final WasmValue value) {
            final SetWasmArray set = new SetWasmArray(type, array, index, value);
            parent.addChild(set);
        }
    }

    private final Container parent;
    public final Array array;

    Expressions(final Container parent) {
        this.parent = parent;
        this.array = new Array();
    }

    public void comment(final String message) {
        parent.addChild(new Comment(message));
    }

    public void voidCall(final Callable function, final List<WasmValue> arguments) {
        final Call call = new Call(function, arguments);
        parent.addChild(call);
    }

    public void voidCallIndirect(final FunctionType functionType, final List<WasmValue> arguments, final WasmValue tableIndex) {
        final CallIndirect call = new CallIndirect(functionType, arguments, tableIndex);
        parent.addChild(call);
    }

    public Iff iff(final String label, final WasmValue condition) {
        final Iff elem = new Iff(parent, label, condition);
        parent.addChild(elem);
        return elem;
    }

    public Block block(final String label) {
        final Block block = new Block(label, parent, PrimitiveType.empty_pseudo_block);
        parent.addChild(block);
        return block;
    }

    public Block block(final String label, final PrimitiveType blockType) {
        final Block block = new Block(label, parent, blockType);
        parent.addChild(block);
        return block;
    }

    public Loop loop(final String label) {
        final Loop loop = new Loop(label, parent);
        parent.addChild(loop);
        return loop;
    }

    public void branch(final LabeledContainer surroundingBlock) {
        final Branch branch = new Branch(surroundingBlock);
        parent.addChild(branch);
    }

    public void ret(final WasmValue value) {
        parent.addChild(new ReturnValue(value));
    }

    public void ret() {
        parent.addChild(new Return());
    }

    public void drop(final WasmValue value) {
        parent.addChild(new Drop(value));
    }

    public void unreachable() {
        parent.addChild(new Unreachable());
    }

    public void setLocal(final Local local, final WasmValue value) {
        final SetLocal setLocal = new SetLocal(local, value);
        parent.addChild(setLocal);
    }

    public void setGlobal(final Global global, final WasmValue value) {
        final SetGlobal setGlobal = new SetGlobal(global, value);
        parent.addChild(setGlobal);
    }

    public void setStruct(final StructType structType, final WasmValue target, final String fieldName, final WasmValue value) {
        final SetStruct setStruct = new SetStruct(structType, target, fieldName, value);
        parent.addChild(setStruct);
    }

    public Try Try(final Tag catchTag) {
        final Try t = new Try(parent, null, catchTag);
        parent.addChild(t);
        return t;
    }

    public void throwException(final Tag exception, final List<WasmValue> arguments) {
        final ThrowException t = new ThrowException(exception, arguments);
        parent.addChild(t);
    }

    public void rethrowException() {
        final RethrowException r  = new RethrowException(null);
        parent.addChild(r);
    }

    public void rethrowException(final WasmValue value) {
        final RethrowException r  = new RethrowException(value);
        parent.addChild(r);
    }
}
