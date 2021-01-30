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

import de.mirkosertic.bytecoder.ssa.Expression;

import java.util.List;

public class Expressions {

    public class I32 {
        public void store(final Alignment alignment, final int offset, final WASMValue ptr, final WASMValue value, final Expression expression) {
            final I32Store store = new I32Store(alignment, offset, ptr, value, expression);
            parent.addChild(store);
        }

        public void store(final int offset, final WASMValue ptr, final WASMValue value, final Expression expression) {
            final I32Store store = new I32Store(offset, ptr, value, expression);
            parent.addChild(store);
        }

        public void store8(final Alignment alignment, final int offset, final WASMValue ptr, final WASMValue value, final Expression expression) {
            final I32Store8 store = new I32Store8(alignment, offset, ptr, value, expression);
            parent.addChild(store);
        }

        public void store8(final int offset, final WASMValue ptr, final WASMValue value, final Expression expression) {
            final I32Store8 store = new I32Store8(offset, ptr, value, expression);
            parent.addChild(store);
        }

        public void store16(final Alignment alignment, final int offset, final WASMValue ptr, final WASMValue value, final Expression expression) {
            final I32Store16 store = new I32Store16(alignment, offset, ptr, value, expression);
            parent.addChild(store);
        }

        public void store16(final int offset, final WASMValue ptr, final WASMValue value, final Expression expression) {
            final I32Store16 store = new I32Store16(offset, ptr, value, expression);
            parent.addChild(store);
        }
    }

    public class F32 {
        public void store(final Alignment alignment, final int offset, final WASMValue ptr, final WASMValue value, final Expression expression) {
            final F32Store store = new F32Store(alignment, offset, ptr, value, expression);
            parent.addChild(store);
        }

        public void store(final int offset, final WASMValue ptr, final WASMValue value, final Expression expression) {
            final F32Store store = new F32Store(offset, ptr, value, expression);
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

    public void voidCall(final Callable function, final List<WASMValue> arguments, final Expression expression) {
        final Call call = new Call(function, arguments, expression);
        parent.addChild(call);
    }

    public void voidCallIndirect(final WASMType functionType, final List<WASMValue> arguments, final WASMValue tableIndex, final Expression expression) {
        final CallIndirect call = new CallIndirect(functionType, arguments, tableIndex, expression);
        parent.addChild(call);
    }

    public Iff iff(final String label, final WASMValue condition, final Expression expression) {
        final Iff elem = new Iff(parent, label, condition, expression);
        parent.addChild(elem);
        return elem;
    }

    public Block block(final String label, final Expression expression) {
        final Block block = new Block(label, parent, expression, PrimitiveType.empty_pseudo_block);
        parent.addChild(block);
        return block;
    }

    public Block block(final String label, final PrimitiveType blockType, final Expression expression) {
        final Block block = new Block(label, parent, expression, blockType);
        parent.addChild(block);
        return block;
    }

    public Loop loop(final String label, final Expression expression) {
        final Loop loop = new Loop(label, parent, expression);
        parent.addChild(loop);
        return loop;
    }

    public void branch(final LabeledContainer surroundingBlock, final Expression expression) {
        final Branch branch = new Branch(surroundingBlock, expression);
        parent.addChild(branch);
    }

    public void branchIff(final LabeledContainer block, final WASMValue condition, final Expression expression) {
        final BranchIff branch = new BranchIff(block, condition, expression);
        parent.addChild(branch);
    }

    public void ret(final WASMValue value, final Expression expression) {
        parent.addChild(new ReturnValue(value, expression));
    }

    public void ret(final Expression expression) {
        parent.addChild(new Return(expression));
    }

    public void nop(final Expression expression) {
        parent.addChild(new Nop(expression));
    }

    public void drop(final WASMValue value, final Expression expression) {
        parent.addChild(new Drop(value,expression));
    }

    public void unreachable(final Expression expression) {
        parent.addChild(new Unreachable(expression));
    }

    public void setLocal(final Local local, final Expression expression) {
        final SetLocal setLocal = new SetLocal(local, null, expression);
        parent.addChild(setLocal);
    }

    public void setLocal(final Local local, final WASMValue value, final Expression expression) {
        final SetLocal setLocal = new SetLocal(local, value, expression);
        parent.addChild(setLocal);
    }

    public void setGlobal(final Global global, final WASMValue value, final Expression expression) {
        final SetGlobal setGlobal = new SetGlobal(global, value, expression);
        parent.addChild(setGlobal);
    }

    public Try Try(final String label, final Expression expression) {
        final Try t = new Try(parent, null, label, expression);
        parent.addChild(t);
        return t;
    }

    public Try Try(final String label, final PrimitiveType blockType, final Expression expression) {
        final Try t = new Try(parent, blockType, label, expression);
        parent.addChild(t);
        return t;
    }

    public void throwException(final WASMEvent exception, final List<WASMValue> arguments, final Expression expression) {
        final ThrowException t = new ThrowException(exception, arguments, expression);
        parent.addChild(t);
    }

    public void rethrowException(final Expression expression) {
        final RethrowException r  = new RethrowException(null, expression);
        parent.addChild(r);
    }

    public void rethrowException(final WASMValue value, final Expression expression) {
        final RethrowException r  = new RethrowException(value, expression);
        parent.addChild(r);
    }

    public void branchOnException(final LabeledContainer branchContainer, final WASMEvent exceptionType, final Expression expression) {
        final BranchOnException b = new BranchOnException(branchContainer, exceptionType, expression);
        parent.addChild(b);
    }
}