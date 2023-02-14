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

    public class I32 {
        public void store(final Alignment alignment, final int offset, final WasmValue ptr, final WasmValue value) {
            final I32Store store = new I32Store(alignment, offset, ptr, value);
            parent.addChild(store);
        }

        public void store(final int offset, final WasmValue ptr, final WasmValue value) {
            final I32Store store = new I32Store(offset, ptr, value);
            parent.addChild(store);
        }

        public void store8(final Alignment alignment, final int offset, final WasmValue ptr, final WasmValue value) {
            final I32Store8 store = new I32Store8(alignment, offset, ptr, value);
            parent.addChild(store);
        }

        public void store8(final int offset, final WasmValue ptr, final WasmValue value) {
            final I32Store8 store = new I32Store8(offset, ptr, value);
            parent.addChild(store);
        }

        public void store16(final Alignment alignment, final int offset, final WasmValue ptr, final WasmValue value) {
            final I32Store16 store = new I32Store16(alignment, offset, ptr, value);
            parent.addChild(store);
        }

        public void store16(final int offset, final WasmValue ptr, final WasmValue value) {
            final I32Store16 store = new I32Store16(offset, ptr, value);
            parent.addChild(store);
        }
    }

    public class F32 {
        public void store(final Alignment alignment, final int offset, final WasmValue ptr, final WasmValue value) {
            final F32Store store = new F32Store(alignment, offset, ptr, value);
            parent.addChild(store);
        }

        public void store(final int offset, final WasmValue ptr, final WasmValue value) {
            final F32Store store = new F32Store(offset, ptr, value);
            parent.addChild(store);
        }
    }

    private final Container parent;
    public final I32 i32;
    public final F32 f32;

    Expressions(final Container parent) {
        this.parent = parent;
        this.i32 = new I32();
        this.f32 = new F32();
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

    public void branchIff(final LabeledContainer block, final WasmValue condition) {
        final BranchIff branch = new BranchIff(block, condition);
        parent.addChild(branch);
    }

    public void ret(final WasmValue value) {
        parent.addChild(new ReturnValue(value));
    }

    public void ret() {
        parent.addChild(new Return());
    }

    public void nop() {
        parent.addChild(new Nop());
    }

    public void drop(final WasmValue value) {
        parent.addChild(new Drop(value));
    }

    public void unreachable() {
        parent.addChild(new Unreachable());
    }

    public void setLocal(final Local local) {
        final SetLocal setLocal = new SetLocal(local, null);
        parent.addChild(setLocal);
    }

    public void setLocal(final Local local, final WasmValue value) {
        final SetLocal setLocal = new SetLocal(local, value);
        parent.addChild(setLocal);
    }

    public void setGlobal(final Global global, final WasmValue value) {
        final SetGlobal setGlobal = new SetGlobal(global, value);
        parent.addChild(setGlobal);
    }

    public void setStruct(final StructType structType, final WasmValue source, final String fieldName, final WasmValue value) {
        final SetStruct setStruct = new SetStruct(structType, source, fieldName, value);
        parent.addChild(setStruct);
    }

    public Try Try(final String label) {
        final Try t = new Try(parent, null, label);
        parent.addChild(t);
        return t;
    }

    public Try Try(final String label, final PrimitiveType blockType) {
        final Try t = new Try(parent, blockType, label);
        parent.addChild(t);
        return t;
    }

    public void throwException(final WasmEvent exception, final List<WasmValue> arguments) {
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

    public void branchOnException(final LabeledContainer branchContainer, final WasmEvent exceptionType) {
        final BranchOnException b = new BranchOnException(branchContainer, exceptionType);
        parent.addChild(b);
    }
}
