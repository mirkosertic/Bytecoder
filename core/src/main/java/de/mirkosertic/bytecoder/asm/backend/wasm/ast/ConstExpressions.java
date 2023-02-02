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

public class ConstExpressions {

    public static Param param(final String label, final PrimitiveType type) {
        return new Param(label, type);
    }

    public static Call call(final Callable function, final List<WasmValue> arguments) {
        return new Call(function, arguments);
    }

    public static CallIndirect call(final FunctionType type, final List<WasmValue> arguments, final WasmValue functionIndex) {
        return new CallIndirect(type, arguments, functionIndex);
    }

    public static class i32 {
        public static I32Const c(final int aValue) {
            return new I32Const(aValue);
        }

        public static I32Eq eq(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32Eq(leftValue, rightValue);
        }

        public static I32Ne ne(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32Ne(leftValue, rightValue);
        }

        public static I32GeS ge_s(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32GeS(leftValue, rightValue);
        }

        public static I32GeU ge_u(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32GeU(leftValue, rightValue);
        }

        public static I32GtS gt_s(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32GtS(leftValue, rightValue);
        }

        public static I32GtU gt_u(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32GtU(leftValue, rightValue);
        }

        public static I32LeS le_s(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32LeS(leftValue, rightValue);
        }

        public static I32LeU le_u(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32LeU(leftValue, rightValue);
        }

        public static I32LtS lt_s(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32LtS(leftValue, rightValue);
        }

        public static I32LtU lt_u(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32LtU(leftValue, rightValue);
        }

        public static I32Eqz eqz(final WasmValue value) {
            return new I32Eqz(value);
        }

        public static I32Popcount popcount(final WasmValue value) {
            return new I32Popcount(value);
        }

        public static I32ReinterpretF32 reinterpretF32(final WasmValue value) {
            return new I32ReinterpretF32(value);
        }

        public static I32Add add(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32Add(leftValue, rightValue);
        }

        public static I32And and(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32And(leftValue, rightValue);
        }

        public static I32Or or(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32Or(leftValue, rightValue);
        }

        public static I32Sub sub(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32Sub(leftValue, rightValue);
        }

        public static I32Mul mul(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32Mul(leftValue, rightValue);
        }

        public static I32DivS div_s(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32DivS(leftValue, rightValue);
        }

        public static I32DivU div_u(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32DivU(leftValue, rightValue);
        }

        public static I32RemS rem_s(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32RemS(leftValue, rightValue);
        }

        public static I32RemU rem_u(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32RemU(leftValue, rightValue);
        }

        public static I32Rotl rotl(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32Rotl(leftValue, rightValue);
        }

        public static I32Rotr rotr(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32Rotr(leftValue, rightValue);
        }

        public static I32Shl shl(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32Shl(leftValue, rightValue);
        }

        public static I32ShrS shr_s(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32ShrS(leftValue, rightValue);
        }

        public static I32ShrU shr_u(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32ShrU(leftValue, rightValue);
        }

        public static I32Xor xor(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32Xor(leftValue, rightValue);
        }

        public static I32Clz clz(final WasmValue value) {
            return new I32Clz(value);
        }

        public static I32Ctz ctz(final WasmValue value) {
            return new I32Ctz(value);
        }

        public static I32TruncSF32 trunc_sF32(final WasmValue value) {
            return new I32TruncSF32(value);
        }

        public static I32TruncUF32 trunc_uF32(final WasmValue value) {
            return new I32TruncUF32(value);
        }

        public static I32Load load(final Alignment alignment, final int offset, final WasmValue ptr) {
            return new I32Load(alignment, offset, ptr);
        }

        public static I32Load load(final int offset, final WasmValue ptr) {
            return new I32Load(offset, ptr);
        }

        public static I32Load8S load8_s(final Alignment alignment, final int offset, final WasmValue ptr) {
            return new I32Load8S(alignment, offset, ptr);
        }

        public static I32Load8S load8_s(final int offset, final WasmValue ptr) {
            return new I32Load8S(offset, ptr);
        }

        public static I32Load16S load16_s(final Alignment alignment, final int offset, final WasmValue ptr) {
            return new I32Load16S(alignment, offset, ptr);
        }

        public static I32Load16S load16_s(final int offset, final WasmValue ptr) {
            return new I32Load16S(offset, ptr);
        }

        public static I32Load8U load8_u(final Alignment alignment, final int offset, final WasmValue ptr) {
            return new I32Load8U(alignment, offset, ptr);
        }

        public static I32Load8U load8_u(final int offset, final WasmValue ptr) {
            return new I32Load8U(offset, ptr);
        }

        public static I32Load16U load16_u(final Alignment alignment, final int offset, final WasmValue ptr) {
            return new I32Load16U(alignment, offset, ptr);
        }

        public static I32Load16U load16_u(final int offset, final WasmValue ptr) {
            return new I32Load16U(offset, ptr);
        }

    }

    public static class f32 {

        public static F32Const c(final float aValue) {
            return new F32Const(aValue);
        }

        public static F32Eq eq(final WasmValue leftValue, final WasmValue rightValue) {
            return new F32Eq(leftValue, rightValue);
        }

        public static F32Ne ne(final WasmValue leftValue, final WasmValue rightValue) {
            return new F32Ne(leftValue, rightValue);
        }

        public static F32Ge ge(final WasmValue leftValue, final WasmValue rightValue) {
            return new F32Ge(leftValue, rightValue);
        }

        public static F32Gt gt(final WasmValue leftValue, final WasmValue rightValue) {
            return new F32Gt(leftValue, rightValue);
        }

        public static F32Le le(final WasmValue leftValue, final WasmValue rightValue) {
            return new F32Le(leftValue, rightValue);
        }

        public static F32Lt lt(final WasmValue leftValue, final WasmValue rightValue) {
            return new F32Lt(leftValue, rightValue);
        }

        public static F32Abs abs(final WasmValue value) {
            return new F32Abs(value);
        }

        public static F32Add add(final WasmValue leftValue, final WasmValue rightValue) {
            return new F32Add(leftValue, rightValue);
        }

        public static F32Sub sub(final WasmValue leftValue, final WasmValue rightValue) {
            return new F32Sub(leftValue, rightValue);
        }

        public static F32Max max(final WasmValue leftValue, final WasmValue rightValue) {
            return new F32Max(leftValue, rightValue);
        }

        public static F32Min min(final WasmValue leftValue, final WasmValue rightValue) {
            return new F32Min(leftValue, rightValue);
        }

        public static F32Mul mul(final WasmValue leftValue, final WasmValue rightValue) {
            return new F32Mul(leftValue, rightValue);
        }

        public static F32Div div(final WasmValue leftValue, final WasmValue rightValue) {
            return new F32Div(leftValue, rightValue);
        }

        public static F32CopySign copysign(final WasmValue leftValue, final WasmValue rightValue) {
            return new F32CopySign(leftValue, rightValue);
        }

        public static F32Ceil ceil(final WasmValue value) {
            return new F32Ceil(value);
        }

        public static F32Nearest nearest(final WasmValue value) {
            return new F32Nearest(value);
        }

        public static F32Neg neg(final WasmValue value) {
            return new F32Neg(value);
        }

        public static F32Sqrt sqrt(final WasmValue value) {
            return new F32Sqrt(value);
        }

        public static F32Trunc trunc(final WasmValue value) {
            return new F32Trunc(value);
        }

        public static F32Floor floor(final WasmValue value) {
            return new F32Floor(value);
        }

        public static F32ConvertSI32 convert_sI32(final WasmValue value) {
            return new F32ConvertSI32(value);
        }

        public static F32ConvertUI32 convert_uI32(final WasmValue value) {
            return new F32ConvertUI32(value);
        }

        public static F32Load load(final Alignment alignment, final int offset, final WasmValue ptr) {
            return new F32Load(alignment, offset, ptr);
        }

        public static F32Load load(final int offset, final WasmValue ptr) {
            return new F32Load(offset, ptr);
        }
    }

    public static GetLocal getLocal(final Local local) {
        return new GetLocal(local);
    }

    public static TeeLocal teeLocal(final Local local, final WasmValue value) {
        return new TeeLocal(local, value);
    }

    public static TeeLocal teeLocal(final Local local) {
        return new TeeLocal(local, null);
    }

    public static GetGlobal getGlobal(final Global global) {
        return new GetGlobal(global);
    }

    public static CurrentMemory currentMemory() {
        return new CurrentMemory();
    }

    public static Select select(final WasmValue leftValue, final WasmValue rightValue, final WasmValue condition) {
        return new Select(leftValue, rightValue, condition);
    }

    public static WeakFunctionTableReference weakFunctionTableReference(final String aFunctionName) {
        return new WeakFunctionTableReference(aFunctionName);
    }

    public static WeakFunctionReferenceCallable weakFunctionReference(final String aFunctionName) {
        return new WeakFunctionReferenceCallable(aFunctionName);
    }

    public static RefType ref(final ReferencableType type) {
        return new RefType(type);
    }
}