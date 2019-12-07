/*
 * Copyright 2019 Mirko Sertic
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
package de.mirkosertic.bytecoder.intrinsics;

import de.mirkosertic.bytecoder.classlib.VM;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESTATIC;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.ssa.ByteValue;
import de.mirkosertic.bytecoder.ssa.MethodTypeArgumentCheckExpression;
import de.mirkosertic.bytecoder.ssa.NewInstanceFromDefaultConstructorExpression;
import de.mirkosertic.bytecoder.ssa.NewObjectAndConstructExpression;
import de.mirkosertic.bytecoder.ssa.ParsingHelper;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.ReinterpretAsNativeExpression;
import de.mirkosertic.bytecoder.ssa.RuntimeGeneratedTypeExpression;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VMIntrinsic extends Intrinsic {

    @Override
    public boolean intrinsify(final Program aProgram, final BytecodeInstructionINVOKESTATIC aInstruction, final String aMethodName, final List<Value> aArguments, final BytecodeObjectTypeRef aTargetClass, final RegionNode aTargetBlock, final ParsingHelper aHelper) {
        if (Objects.equals(aTargetClass.name(), VM.class.getName())) {
            if ("newRuntimeGeneratedType".equals(aMethodName)) {
                final RuntimeGeneratedTypeExpression theValue = new RuntimeGeneratedTypeExpression(aProgram, aInstruction.getOpcodeAddress(), aArguments.get(1), aArguments.get(2), aArguments.get(3), aArguments.get(0));
                final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, theValue);
                aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);

                return true;
            }
            if ("newInstanceWithDefaultConstructor".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new NewInstanceFromDefaultConstructorExpression(aProgram, aInstruction.getOpcodeAddress(), aArguments.get(0)));
                return true;
            }
            if ("newStringInternal".equals(aMethodName)) {
                // We call the package-private string(byte[],coder) constructor here
                final BytecodeObjectTypeRef theStringRef = BytecodeObjectTypeRef.fromRuntimeClass(String.class);
                final BytecodeMethodSignature theConstructorSignature = new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID,
                        new BytecodeTypeRef[]{new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.BYTE, 1), BytecodePrimitiveTypeRef.BYTE});
                final List<Value> theArguments = new ArrayList<>();
                theArguments.add(aArguments.get(0));
                theArguments.add(new ByteValue((byte) 0));

                final TypeRef theStringType = TypeRef.toType(BytecodeObjectTypeRef.fromRuntimeClass(String.class));

                final Variable theNewVariable = aTargetBlock
                        .newVariable(aInstruction.getOpcodeAddress(), theStringType, new NewObjectAndConstructExpression(aProgram, aInstruction.getOpcodeAddress(),
                                theStringRef, theConstructorSignature, theArguments));
                aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);
                return true;
            }
            if ("isChar".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new MethodTypeArgumentCheckExpression(aProgram, aInstruction.getOpcodeAddress(), aArguments.get(0), aArguments.get(1), TypeRef.Native.CHAR));
                return true;
            }
            if ("isFloat".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new MethodTypeArgumentCheckExpression(aProgram, aInstruction.getOpcodeAddress(), aArguments.get(0), aArguments.get(1), TypeRef.Native.FLOAT));
                return true;
            }
            if ("isDouble".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new MethodTypeArgumentCheckExpression(aProgram, aInstruction.getOpcodeAddress(), aArguments.get(0), aArguments.get(1), TypeRef.Native.DOUBLE));
                return true;
            }
            if ("isBoolean".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new MethodTypeArgumentCheckExpression(aProgram, aInstruction.getOpcodeAddress(), aArguments.get(0), aArguments.get(1), TypeRef.Native.BOOLEAN));
                return true;
            }
            if ("isInteger".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new MethodTypeArgumentCheckExpression(aProgram, aInstruction.getOpcodeAddress(), aArguments.get(0), aArguments.get(1), TypeRef.Native.INT));
                return true;
            }
            if ("isLong".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new MethodTypeArgumentCheckExpression(aProgram, aInstruction.getOpcodeAddress(), aArguments.get(0), aArguments.get(1), TypeRef.Native.LONG));
                return true;
            }
            if ("reinterpretAsInt".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new ReinterpretAsNativeExpression(aProgram, aInstruction.getOpcodeAddress(), aArguments.get(0), TypeRef.Native.INT));
                return true;
            }
            if ("reinterpretAsLong".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new ReinterpretAsNativeExpression(aProgram, aInstruction.getOpcodeAddress(), aArguments.get(0), TypeRef.Native.LONG));
                return true;
            }
            if ("reinterpretAsFloat".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new ReinterpretAsNativeExpression(aProgram, aInstruction.getOpcodeAddress(), aArguments.get(0), TypeRef.Native.FLOAT));
                return true;
            }
            if ("reinterpretAsDouble".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new ReinterpretAsNativeExpression(aProgram, aInstruction.getOpcodeAddress(), aArguments.get(0), TypeRef.Native.DOUBLE));
                return true;
            }
            if ("reinterpretAsChar".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new ReinterpretAsNativeExpression(aProgram, aInstruction.getOpcodeAddress(), aArguments.get(0), TypeRef.Native.CHAR));
                return true;
            }
            if ("reinterpretAsBoolean".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new ReinterpretAsNativeExpression(aProgram, aInstruction.getOpcodeAddress(), aArguments.get(0), TypeRef.Native.BOOLEAN));
                return true;
            }
        }
        return false;
    }
}
