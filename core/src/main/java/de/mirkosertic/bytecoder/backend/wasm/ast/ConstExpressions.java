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

    public static Call call(final Function function, final List<WASMValue> arguments) {
        return new Call(function, arguments);
    }

    public static CallIndirect call(final FunctionType type, final List<WASMValue> arguments, final WASMValue functionIndex) {
        return new CallIndirect(type, arguments, functionIndex);
    }

    public static class i32 {
        public static I32Const c(final int aValue) {
            return new I32Const(aValue);
        }

        public static I32Eq eq(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32Eq(leftValue, rightValue);
        }

        public static I32Ne ne(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32Ne(leftValue, rightValue);
        }

        public static I32GeS ge_s(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32GeS(leftValue, rightValue);
        }

        public static I32GeU ge_u(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32GeU(leftValue, rightValue);
        }

        public static I32GtS gt_s(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32GtS(leftValue, rightValue);
        }

        public static I32GtU gt_u(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32GtU(leftValue, rightValue);
        }

        public static I32LeS le_s(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32LeS(leftValue, rightValue);
        }

        public static I32LeU le_u(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32LeU(leftValue, rightValue);
        }

        public static I32LtS lt_s(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32LtS(leftValue, rightValue);
        }

        public static I32LtU lt_u(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32LtU(leftValue, rightValue);
        }

        public static I32Eqz eqz(final WASMValue value) {
            return new I32Eqz(value);
        }

        public static I32Popcount popcount(final WASMValue value) {
            return new I32Popcount(value);
        }

        public static I32ReinterpretF32 reinterpretF32(final WASMValue value) {
            return new I32ReinterpretF32(value);
        }

        public static I32Add add(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32Add(leftValue, rightValue);
        }

        public static I32And and(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32And(leftValue, rightValue);
        }

        public static I32Or or(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32Or(leftValue, rightValue);
        }

        public static I32Sub sub(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32Sub(leftValue, rightValue);
        }

        public static I32Mul mul(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32Mul(leftValue, rightValue);
        }

        public static I32DivS div_s(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32DivS(leftValue, rightValue);
        }

        public static I32DivU div_u(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32DivU(leftValue, rightValue);
        }

        public static I32RemS rem_s(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32RemS(leftValue, rightValue);
        }

        public static I32RemU rem_u(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32RemU(leftValue, rightValue);
        }

        public static I32Rotl rotl(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32Rotl(leftValue, rightValue);
        }

        public static I32Rotr rotr(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32Rotr(leftValue, rightValue);
        }

        public static I32Shl shl(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32Shl(leftValue, rightValue);
        }

        public static I32ShrS shr_s(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32ShrS(leftValue, rightValue);
        }

        public static I32ShrU shr_u(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32ShrU(leftValue, rightValue);
        }

        public static I32Xor xor(final WASMValue leftValue, final WASMValue rightValue) {
            return new I32Xor(leftValue, rightValue);
        }

        public static I32Clz clz(final WASMValue value) {
            return new I32Clz(value);
        }

        public static I32Ctz ctz(final WASMValue value) {
            return new I32Ctz(value);
        }

        public static I32TruncSF32 trunc_sF32(final WASMValue value) {
            return new I32TruncSF32(value);
        }

        public static I32TruncUF32 trunc_uF32(final WASMValue value) {
            return new I32TruncUF32(value);
        }

        public static I32Load load(final Alignment alignment, final int offset, final WASMValue ptr) {
            return new I32Load(alignment, offset, ptr);
        }

        public static I32Load load(final int offset, final WASMValue ptr) {
            return new I32Load(offset, ptr);
        }

        public static I32Load8S load8_s(final Alignment alignment, final int offset, final WASMValue ptr) {
            return new I32Load8S(alignment, offset, ptr);
        }

        public static I32Load8S load8_s(final int offset, final WASMValue ptr) {
            return new I32Load8S(offset, ptr);
        }

        public static I32Load16S load16_s(final Alignment alignment, final int offset, final WASMValue ptr) {
            return new I32Load16S(alignment, offset, ptr);
        }

        public static I32Load16S load16_s(final int offset, final WASMValue ptr) {
            return new I32Load16S(offset, ptr);
        }

        public static I32Load8U load8_u(final Alignment alignment, final int offset, final WASMValue ptr) {
            return new I32Load8U(alignment, offset, ptr);
        }

        public static I32Load8U load8_u(final int offset, final WASMValue ptr) {
            return new I32Load8U(offset, ptr);
        }

        public static I32Load16U load16_u(final Alignment alignment, final int offset, final WASMValue ptr) {
            return new I32Load16U(alignment, offset, ptr);
        }

        public static I32Load16U load16_u(final int offset, final WASMValue ptr) {
            return new I32Load16U(offset, ptr);
        }

    }

    public static class f32 {

        public static F32Const c(final float aValue) {
            return new F32Const(aValue);
        }

        public static F32Eq eq(final WASMValue leftValue, final WASMValue rightValue) {
            return new F32Eq(leftValue, rightValue);
        }

        public static F32Ne ne(final WASMValue leftValue, final WASMValue rightValue) {
            return new F32Ne(leftValue, rightValue);
        }

        public static F32Ge ge(final WASMValue leftValue, final WASMValue rightValue) {
            return new F32Ge(leftValue, rightValue);
        }

        public static F32Gt gt(final WASMValue leftValue, final WASMValue rightValue) {
            return new F32Gt(leftValue, rightValue);
        }

        public static F32Le le(final WASMValue leftValue, final WASMValue rightValue) {
            return new F32Le(leftValue, rightValue);
        }

        public static F32Lt lt(final WASMValue leftValue, final WASMValue rightValue) {
            return new F32Lt(leftValue, rightValue);
        }

        public static F32Abs abs(final WASMValue value) {
            return new F32Abs(value);
        }

        public static F32Add add(final WASMValue leftValue, final WASMValue rightValue) {
            return new F32Add(leftValue, rightValue);
        }

        public static F32Sub sub(final WASMValue leftValue, final WASMValue rightValue) {
            return new F32Sub(leftValue, rightValue);
        }

        public static F32Max max(final WASMValue leftValue, final WASMValue rightValue) {
            return new F32Max(leftValue, rightValue);
        }

        public static F32Min min(final WASMValue leftValue, final WASMValue rightValue) {
            return new F32Min(leftValue, rightValue);
        }

        public static F32Mul mul(final WASMValue leftValue, final WASMValue rightValue) {
            return new F32Mul(leftValue, rightValue);
        }

        public static F32Div div(final WASMValue leftValue, final WASMValue rightValue) {
            return new F32Div(leftValue, rightValue);
        }

        public static F32CopySign copysign(final WASMValue leftValue, final WASMValue rightValue) {
            return new F32CopySign(leftValue, rightValue);
        }

        public static F32Ceil ceil(final WASMValue value) {
            return new F32Ceil(value);
        }

        public static F32Nearest nearest(final WASMValue value) {
            return new F32Nearest(value);
        }

        public static F32Neg neg(final WASMValue value) {
            return new F32Neg(value);
        }

        public static F32Sqrt sqrt(final WASMValue value) {
            return new F32Sqrt(value);
        }

        public static F32Trunc trunc(final WASMValue value) {
            return new F32Trunc(value);
        }

        public static F32Floor floor(final WASMValue value) {
            return new F32Floor(value);
        }

        public static F32ConvertSI32 convert_sI32(final WASMValue value) {
            return new F32ConvertSI32(value);
        }

        public static F32ConvertUI32 convert_uI32(final WASMValue value) {
            return new F32ConvertUI32(value);
        }

        public static F32Load load(final Alignment alignment, final int offset, final WASMValue ptr) {
            return new F32Load(alignment, offset, ptr);
        }

        public static F32Load load(final int offset, final WASMValue ptr) {
            return new F32Load(offset, ptr);
        }
    }

    public static GetLocal getLocal(final Local local) {
        return new GetLocal(local);
    }

    public static TeeLocal teeLocal(final Local local, final WASMValue value) {
        return new TeeLocal(local, value);
    }

    public static GetGlobal getGlobal(final Global global) {
        return new GetGlobal(global);
    }

    public static CurrentMemory currentMemory() {
        return new CurrentMemory();
    }

    public static Select select(final WASMValue leftValue, final WASMValue rightValue, final WASMValue condition) {
        return new Select(leftValue, rightValue, condition);
    }

    public static WeakFunctionTableReference weakFunctionTableReference(final String aFunctionName) {
        return new WeakFunctionTableReference(aFunctionName);
    }
}
