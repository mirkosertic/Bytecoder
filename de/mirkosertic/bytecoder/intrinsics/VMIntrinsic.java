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
import de.mirkosertic.bytecoder.ssa.ArrayEntryExpression;
import de.mirkosertic.bytecoder.ssa.ByteValue;
import de.mirkosertic.bytecoder.ssa.GetReflectiveFieldExpression;
import de.mirkosertic.bytecoder.ssa.GetReflectiveStaticFieldExpression;
import de.mirkosertic.bytecoder.ssa.LambdaConstructorReferenceExpression;
import de.mirkosertic.bytecoder.ssa.LambdaInterfaceReferenceExpression;
import de.mirkosertic.bytecoder.ssa.LambdaSpecialReferenceExpression;
import de.mirkosertic.bytecoder.ssa.LambdaVirtualReferenceExpression;
import de.mirkosertic.bytecoder.ssa.LambdaWithStaticImplExpression;
import de.mirkosertic.bytecoder.ssa.MethodTypeArgumentCheckExpression;
import de.mirkosertic.bytecoder.ssa.NewInstanceAndConstructExpression;
import de.mirkosertic.bytecoder.ssa.NewInstanceFromDefaultConstructorExpression;
import de.mirkosertic.bytecoder.ssa.ParsingHelper;
import de.mirkosertic.bytecoder.ssa.PrimitiveClassReferenceValue;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.PutReflectiveFieldExpression;
import de.mirkosertic.bytecoder.ssa.PutReflectiveStaticFieldExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
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
            if ("newLambdaStaticInvocation".equals(aMethodName)) {
                final LambdaWithStaticImplExpression theValue = new LambdaWithStaticImplExpression(aProgram, aInstruction.getOpcodeAddress(), aArguments.get(1), aArguments.get(2), aArguments.get(3), aArguments.get(0));
                final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, theValue);
                aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);

                return true;
            }
            if ("newLambdaInterfaceInvocation".equals(aMethodName)) {
                final LambdaInterfaceReferenceExpression theValue = new LambdaInterfaceReferenceExpression(aProgram, aInstruction.getOpcodeAddress(), aArguments.get(0), aArguments.get(1), aArguments.get(2));
                final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, theValue);
                aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);
                return true;
            }
            if ("newLambdaVirtualInvocation".equals(aMethodName)) {
                final LambdaVirtualReferenceExpression theValue = new LambdaVirtualReferenceExpression(aProgram, aInstruction.getOpcodeAddress(), aArguments.get(0), aArguments.get(1), aArguments.get(2));
                final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, theValue);
                aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);
                return true;
            }
            if ("newLambdaConstructorInvocation".equals(aMethodName)) {
                final LambdaConstructorReferenceExpression theValue = new LambdaConstructorReferenceExpression(aProgram, aInstruction.getOpcodeAddress(), aArguments.get(0), aArguments.get(1), aArguments.get(2));
                final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, theValue);
                aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);
                return true;
            }
            if ("newLambdaSpecialInvocation".equals(aMethodName)) {
                final LambdaSpecialReferenceExpression theValue = new LambdaSpecialReferenceExpression(aProgram, aInstruction.getOpcodeAddress(), aArguments.get(0), aArguments.get(1), aArguments.get(2));
                final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, theValue);
                aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);
                return true;
            }
            if ("newInstanceFromDefaultConstructor".equals(aMethodName)) {
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
                        .newVariable(aInstruction.getOpcodeAddress(), theStringType, new NewInstanceAndConstructExpression(aProgram, aInstruction.getOpcodeAddress(),
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
            if ("isShort".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new MethodTypeArgumentCheckExpression(aProgram, aInstruction.getOpcodeAddress(), aArguments.get(0), aArguments.get(1), TypeRef.Native.SHORT));
                return true;
            }
            if ("isByte".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new MethodTypeArgumentCheckExpression(aProgram, aInstruction.getOpcodeAddress(), aArguments.get(0), aArguments.get(1), TypeRef.Native.BYTE));
                return true;
            }
            if ("arrayEntryAsLong".equals(aMethodName)) {
                final ArrayEntryExpression theExpression = new ArrayEntryExpression(aProgram, aInstruction.getOpcodeAddress(),
                        TypeRef.Native.LONG, aArguments.get(0), aArguments.get(1));
                aHelper.push(aInstruction.getOpcodeAddress(), theExpression);
                return true;
            }
            if ("arrayEntryAsInt".equals(aMethodName)) {
                final ArrayEntryExpression theExpression = new ArrayEntryExpression(aProgram, aInstruction.getOpcodeAddress(),
                        TypeRef.Native.INT, aArguments.get(0), aArguments.get(1));
                aHelper.push(aInstruction.getOpcodeAddress(), theExpression);
                return true;
            }
            if ("arrayEntryAsFloat".equals(aMethodName)) {
                final ArrayEntryExpression theExpression = new ArrayEntryExpression(aProgram, aInstruction.getOpcodeAddress(),
                        TypeRef.Native.FLOAT, aArguments.get(0), aArguments.get(1));
                aHelper.push(aInstruction.getOpcodeAddress(), theExpression);
                return true;
            }
            if ("arrayEntryAsDouble".equals(aMethodName)) {
                final ArrayEntryExpression theExpression = new ArrayEntryExpression(aProgram, aInstruction.getOpcodeAddress(),
                        TypeRef.Native.DOUBLE, aArguments.get(0), aArguments.get(1));
                aHelper.push(aInstruction.getOpcodeAddress(), theExpression);
                return true;
            }
            if ("arrayEntryAsChar".equals(aMethodName)) {
                final ArrayEntryExpression theExpression = new ArrayEntryExpression(aProgram, aInstruction.getOpcodeAddress(),
                        TypeRef.Native.CHAR, aArguments.get(0), aArguments.get(1));
                aHelper.push(aInstruction.getOpcodeAddress(), theExpression);
                return true;
            }
            if ("arrayEntryAsBoolean".equals(aMethodName)) {
                final ArrayEntryExpression theExpression = new ArrayEntryExpression(aProgram, aInstruction.getOpcodeAddress(),
                        TypeRef.Native.BOOLEAN, aArguments.get(0), aArguments.get(1));
                aHelper.push(aInstruction.getOpcodeAddress(), theExpression);
                return true;
            }
            if ("arrayEntryAsShort".equals(aMethodName)) {
                final ArrayEntryExpression theExpression = new ArrayEntryExpression(aProgram, aInstruction.getOpcodeAddress(),
                        TypeRef.Native.SHORT, aArguments.get(0), aArguments.get(1));
                aHelper.push(aInstruction.getOpcodeAddress(), theExpression);
                return true;
            }
            if ("arrayEntryAsByte".equals(aMethodName)) {
                final ArrayEntryExpression theExpression = new ArrayEntryExpression(aProgram, aInstruction.getOpcodeAddress(),
                        TypeRef.Native.BYTE, aArguments.get(0), aArguments.get(1));
                aHelper.push(aInstruction.getOpcodeAddress(), theExpression);
                return true;
            }
            if ("getObjectFromStaticField".equals(aMethodName)) {
                final GetReflectiveStaticFieldExpression theExpression = new GetReflectiveStaticFieldExpression(aProgram, aInstruction.getOpcodeAddress(),
                        TypeRef.Native.REFERENCE, aArguments.get(1), aArguments.get(0));
                aHelper.push(aInstruction.getOpcodeAddress(), theExpression);
                return true;
            }
            if ("getObjectFromInstanceField".equals(aMethodName)) {
                final GetReflectiveFieldExpression theExpression = new GetReflectiveFieldExpression(aProgram, aInstruction.getOpcodeAddress(),
                        TypeRef.Native.REFERENCE, aArguments.get(1), aArguments.get(0));
                aHelper.push(aInstruction.getOpcodeAddress(), theExpression);
                return true;
            }
            if ("putObjectToStaticField".equals(aMethodName)) {
                final PutReflectiveStaticFieldExpression theExpression = new PutReflectiveStaticFieldExpression(aProgram, aInstruction.getOpcodeAddress(),
                        TypeRef.Native.REFERENCE, aArguments.get(1), aArguments.get(0), aArguments.get(2));
                aHelper.push(aInstruction.getOpcodeAddress(), theExpression);
                return true;
            }
            if ("putObjectToInstanceField".equals(aMethodName)) {
                final PutReflectiveFieldExpression theExpression = new PutReflectiveFieldExpression(aProgram, aInstruction.getOpcodeAddress(),
                        TypeRef.Native.REFERENCE, aArguments.get(1), aArguments.get(0), aArguments.get(2));
                aHelper.push(aInstruction.getOpcodeAddress(), theExpression);
                return true;
            }

            if ("bytePrimitiveClass".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new PrimitiveClassReferenceValue(TypeRef.Native.BYTE));
                return true;
            }
            if ("charPrimitiveClass".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new PrimitiveClassReferenceValue(TypeRef.Native.CHAR));
                return true;
            }
            if ("shortPrimitiveClass".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new PrimitiveClassReferenceValue(TypeRef.Native.SHORT));
                return true;
            }
            if ("intPrimitiveClass".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new PrimitiveClassReferenceValue(TypeRef.Native.INT));
                return true;
            }
            if ("floatPrimitiveClass".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new PrimitiveClassReferenceValue(TypeRef.Native.FLOAT));
                return true;
            }
            if ("doublePrimitiveClass".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new PrimitiveClassReferenceValue(TypeRef.Native.DOUBLE));
                return true;
            }
            if ("longPrimitiveClass".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new PrimitiveClassReferenceValue(TypeRef.Native.LONG));
                return true;
            }
            if ("booleanPrimitiveClass".equals(aMethodName)) {
                aHelper.push(aInstruction.getOpcodeAddress(), new PrimitiveClassReferenceValue(TypeRef.Native.BOOLEAN));
                return true;
            }
        }
        return false;
    }
}
