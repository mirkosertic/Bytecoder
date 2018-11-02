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

public class Expressions {

    public class I32 {
        public void store(final Alignment alignment, final int offset, final WASMValue ptr, final WASMValue value) {
            final I32Store store = new I32Store(alignment, offset, ptr, value);
            parent.addChild(store);
        }

        public void store(final int offset, final WASMValue ptr, final WASMValue value) {
            final I32Store store = new I32Store(offset, ptr, value);
            parent.addChild(store);
        }

        public void store8(final Alignment alignment, final int offset, final WASMValue ptr, final WASMValue value) {
            final I32Store8 store = new I32Store8(alignment, offset, ptr, value);
            parent.addChild(store);
        }

        public void store8(final int offset, final WASMValue ptr, final WASMValue value) {
            final I32Store8 store = new I32Store8(offset, ptr, value);
            parent.addChild(store);
        }

        public void store16(final Alignment alignment, final int offset, final WASMValue ptr, final WASMValue value) {
            final I32Store16 store = new I32Store16(alignment, offset, ptr, value);
            parent.addChild(store);
        }

        public void store16(final int offset, final WASMValue ptr, final WASMValue value) {
            final I32Store16 store = new I32Store16(offset, ptr, value);
            parent.addChild(store);
        }
    }

    public class F32 {
        public void store(final Alignment alignment, final int offset, final WASMValue ptr, final WASMValue value) {
            final F32Store store = new F32Store(alignment, offset, ptr, value);
            parent.addChild(store);
        }

        public void store(final int offset, final WASMValue ptr, final WASMValue value) {
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

    public void voidCall(final Function function, final List<WASMValue> arguments) {
        final Call call = new Call(function, arguments);
        parent.addChild(call);
    }

    public void voidCallIndirect(final FunctionType functionType, final List<WASMValue> arguments, final WASMValue tableIndex) {
        final CallIndirect call = new CallIndirect(functionType, arguments, tableIndex);
        parent.addChild(call);
    }

    public Iff iff(final String label, final WASMValue condition) {
        final Iff elem = new Iff(parent, label, condition);
        parent.addChild(elem);
        return elem;
    }

    public Block block(final String label) {
        final Block block = new Block(label, parent);
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

    public void branchIff(final LabeledContainer block, final WASMValue condition) {
        final BranchIff branch = new BranchIff(block, condition);
        parent.addChild(branch);
    }

    public void ret(final WASMValue value) {
        parent.addChild(new ReturnValue(value));
    }

    public void ret() {
        parent.addChild(new Return());
    }

    public void nop() {
        parent.addChild(new Nop());
    }

    public void drop(final WASMValue value) {
        parent.addChild(new Drop(value));
    }

    public void unreachable() {
        parent.addChild(new Unreachable());
    }

    public void setLocal(final Local local, final WASMValue value) {
        final SetLocal setLocal = new SetLocal(local, value);
        parent.addChild(setLocal);
    }

    public void setGlobal(final Global global, final WASMValue value) {
        final SetGlobal setGlobal = new SetGlobal(global, value);
        parent.addChild(setGlobal);
    }
}