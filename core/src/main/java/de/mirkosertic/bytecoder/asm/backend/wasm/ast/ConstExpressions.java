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

    public static Tag tag(final String label, final WasmType... params) {
        return new Tag(label, params);
    }

    public static Param param(final String label, final WasmType type) {
        return new Param(label, type);
    }

    public static Call call(final Callable function, final List<WasmValue> arguments) {
        return new Call(function, arguments);
    }

    public static CallIndirect call(final FunctionType type, final List<WasmValue> arguments, final WasmValue functionIndex) {
        return new CallIndirect(type, arguments, functionIndex);
    }

    public static Pop pop(final WasmType type) {
        return new Pop(type);
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

        public static I32GtS gt_s(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32GtS(leftValue, rightValue);
        }

        public static I32LeS le_s(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32LeS(leftValue, rightValue);
        }

        public static I32LtS lt_s(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32LtS(leftValue, rightValue);
        }

        public static I32Eqz eqz(final WasmValue value) {
            return new I32Eqz(value);
        }

        public static I32Popcount popcount(final WasmValue value) {
            return new I32Popcount(value);
        }

        public static I32ReinterpretF32 reinterpretf32(final WasmValue value) {
            return new I32ReinterpretF32(value);
        }

        public static I32WrapI64 wrap_i64(final WasmValue value) {
            return new I32WrapI64(value);
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

        public static I32RemS rem_s(final WasmValue leftValue, final WasmValue rightValue) {
            return new I32RemS(leftValue, rightValue);
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

        public static I32TruncSF32 trunc_sf32(final WasmValue value) {
            return new I32TruncSF32(value);
        }

        public static I32TruncSF64 trunc_f64s(final WasmValue value) {
            return new I32TruncSF64(value);
        }
    }

    public static class i64 {

        public static I64Const c(final Long aValue) {
            return new I64Const(aValue);
        }

        public static I64Mul mul(final WasmValue leftValue, final WasmValue rightValue) {
            return new I64Mul(leftValue, rightValue);
        }

        public static I64RemS rem_s(final WasmValue leftValue, final WasmValue rightValue) {
            return new I64RemS(leftValue, rightValue);
        }

        public static I64And and(final WasmValue leftValue, final WasmValue rightValue) {
            return new I64And(leftValue, rightValue);
        }
        public static I64ShrS shr_s(final WasmValue leftValue, final WasmValue rightValue) {
            return new I64ShrS(leftValue, rightValue);
        }

        public static I64ShrU shr_u(final WasmValue leftValue, final WasmValue rightValue) {
            return new I64ShrU(leftValue, rightValue);
        }

        public static I64LeS le_s(final WasmValue leftValue, final WasmValue rightValue) {
            return new I64LeS(leftValue, rightValue);
        }

        public static I64GeS ge_s(final WasmValue leftValue, final WasmValue rightValue) {
            return new I64GeS(leftValue, rightValue);
        }

        public static I64GtS gt_s(final WasmValue leftValue, final WasmValue rightValue) {
            return new I64GtS(leftValue, rightValue);
        }

        public static I64LtS lt_s(final WasmValue leftValue, final WasmValue rightValue) {
            return new I64LtS(leftValue, rightValue);
        }

        public static I64Ne ne(final WasmValue leftValue, final WasmValue rightValue) {
            return new I64Ne(leftValue, rightValue);
        }

        public static I64ExtendI32S extend_i32s(final WasmValue value) {
            return new I64ExtendI32S(value);
        }

        public static I64TruncSF32 trunc_sf32(final WasmValue value) {
            return new I64TruncSF32(value);
        }

        public static I64TruncSF64 trunc_sf64(final WasmValue value) {
            return new I64TruncSF64(value);
        }

        public static I64Sub sub(final WasmValue leftValue, final WasmValue rightValue) {
            return new I64Sub(leftValue, rightValue);
        }

        public static I64DivS div_s(final WasmValue leftValue, final WasmValue rightValue) {
            return new I64DivS(leftValue, rightValue);
        }

        public static I64Shl shl(final WasmValue leftValue, final WasmValue rightValue) {
            return new I64Shl(leftValue, rightValue);
        }

        public static I64Xor xor(final WasmValue leftValue, final WasmValue rightValue) {
            return new I64Xor(leftValue, rightValue);
        }

        public static I64Add add(final WasmValue leftValue, final WasmValue rightValue) {
            return new I64Add(leftValue, rightValue);
        }

        public static I64Or or(final WasmValue leftValue, final WasmValue rightValue) {
            return new I64Or(leftValue, rightValue);
        }

        public static I64Eq eq(final WasmValue leftValue, final WasmValue rightValue) {
            return new I64Eq(leftValue, rightValue);
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

        public static F32ConvertSI32 convert_si32(final WasmValue value) {
            return new F32ConvertSI32(value);
        }

        public static F32ConvertSI64 convert_si64(final WasmValue value) {
            return new F32ConvertSI64(value);
        }

        public static F32TruncSF64 trunc_f64s(final WasmValue value) {
            return new F32TruncSF64(value);
        }
    }

    public static class f64 {

        public static F64Const c(final double aValue) {
            return new F64Const(aValue);
        }

        public static F64Mul mul(final WasmValue leftValue, final WasmValue rightValue) {
            return new F64Mul(leftValue, rightValue);
        }

        public static F64Trunc trunc(final WasmValue value) {
            return new F64Trunc(value);
        }

        public static F64Gt gt(final WasmValue leftValue, final WasmValue rightValue) {
            return new F64Gt(leftValue, rightValue);
        }

        public static F64Le le(final WasmValue leftValue, final WasmValue rightValue) {
            return new F64Le(leftValue, rightValue);
        }

        public static F64Ge ge(final WasmValue leftValue, final WasmValue rightValue) {
            return new F64Ge(leftValue, rightValue);
        }

        public static F64Ne ne(final WasmValue leftValue, final WasmValue rightValue) {
            return new F64Ne(leftValue, rightValue);
        }

        public static F64Eq eq(final WasmValue leftValue, final WasmValue rightValue) {
            return new F64Eq(leftValue, rightValue);
        }

        public static F64Lt lt(final WasmValue leftValue, final WasmValue rightValue) {
            return new F64Lt(leftValue, rightValue);
        }

        public static F64ConvertSI32 convert_si32(final WasmValue value) {
            return new F64ConvertSI32(value);
        }

        public static F64ConvertSI64 convert_si64(final WasmValue value) {
            return new F64ConvertSI64(value);
        }

        public static F64PromoteF32 promote_f32(final WasmValue value) {
            return new F64PromoteF32(value);
        }

        public static F64Sub sub(final WasmValue leftValue, final WasmValue rightValue) {
            return new F64Sub(leftValue, rightValue);
        }

        public static F64Div div(final WasmValue leftValue, final WasmValue rightValue) {
            return new F64Div(leftValue, rightValue);
        }

        public static F64Add add(final WasmValue leftValue, final WasmValue rightValue) {
            return new F64Add(leftValue, rightValue);
        }

        public static F64Neg neg(final WasmValue value) {
            return new F64Neg(value);
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

    public static Select select(final WasmValue leftValue, final WasmValue rightValue, final WasmValue condition) {
        return new Select(leftValue, rightValue, condition);
    }

    public static WeakFunctionReferenceCallable weakFunctionReference(final String aFunctionName) {
        return new WeakFunctionReferenceCallable(aFunctionName);
    }

    public static WeakFunctionTableReference weakFunctionTableReference(final String aFunctionName) {
        return new WeakFunctionTableReference(aFunctionName);
    }

    public static class ref {

        public static RefType type(final ReferencableType type, final boolean nullable) {
            return new RefType(type, nullable);
        }

        public static WasmFuncRef ref(final Function function) {
            return new WasmFuncRef(function);
        }

        public static WasmNullRef nullRef() {
            return new WasmNullRef();
        }

        public static WasmExternNullRef externNullRef() {
            return new WasmExternNullRef();
        }

        public static HostType host() {
            return new HostType();
        }

        public static WasmValue callRef(final FunctionType type, final List<WasmValue> arguments) {
            return new CallRef(type, arguments);
        }

        public static Cast cast(final StructType type, final WasmValue value) {
            return new Cast(type, value);
        }

        public static WasmValue eq(final WasmValue left, final WasmValue right) {
            return new RefEq(left, right);
        }

        public static WasmValue isnull(final WasmValue left) {
            return new RefIsNull(left);
        }

    }

    public static class struct {

        public static WasmValue newInstance(final ReferencableType type, final List<WasmValue> arguments) {
            return new NewStruct(type, arguments);
        }

        public static WasmValue get(final StructType structType, final WasmValue source, final String fieldName) {
            return new GetStruct(structType, source, fieldName);
        }
    }

    public static class array {

        public static WasmValue newInstance(final WasmType type, final WasmValue length, final List<WasmValue> arguments) {
            return new NewWasmArray(type, length, arguments);
        }

        public static WasmValue newInstanceDefault(final WasmType type, final WasmValue length) {
            return new NewWasmArrayDefault(type, length);
        }

        public static WasmValue get(final WasmType type, final WasmValue array, final WasmValue index) {
            return new GetWasmArray(type, array, index);
        }

        public static WasmValue len(final WasmType type, final WasmValue array) {
            return new GetWasmArrayLength(type, array);
        }

    }

}
