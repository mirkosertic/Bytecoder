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

import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESTATIC;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.ssa.*;

import java.lang.reflect.Array;
import java.util.List;

public class JavaUtilArrayIntrinsic extends Intrinsic {

    @Override
    public boolean intrinsify(final Program aProgram, final BytecodeInstructionINVOKESTATIC aInstruction, final String aMethodName, final List<Value> aArguments, final BytecodeObjectTypeRef aTargetClass, final RegionNode aTargetBlock, final ParsingHelper aHelper) {
        if (Array.class.getName().equals(aTargetClass.name())) {
            if ("newInstance".equals(aMethodName)) {

                final Value theArrayType = aArguments.get(0);
                final Value theArraySize = aArguments.get(1);

                final Value theValue = new NewArrayExpression(aProgram, aInstruction.getOpcodeAddress(), BytecodeObjectTypeRef.fromRuntimeClass(Object.class), theArraySize);
                final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.toType(aInstruction.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature().getReturnType()), theValue);
                aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);

                return true;
            }
            if ("getLength".equals(aMethodName)) {

                final Value theArray = aArguments.get(0);

                final Value theValue = new ArrayLengthExpression(aProgram, aInstruction.getOpcodeAddress(), theArray);
                final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.Native.INT, theValue);
                aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);

                return true;
            }
            if ("setBoolean".equals(aMethodName)) {

                final Value theArray = aArguments.get(0);
                final Value theIndex = aArguments.get(1);
                final Value theValue = aArguments.get(2);

                aTargetBlock.getExpressions().add(new ArrayStoreExpression(aProgram, aInstruction.getOpcodeAddress(),
                        TypeRef.Native.BOOLEAN, theArray, theIndex, theValue));

                return true;
            }
            if ("getBoolean".equals(aMethodName)) {

                final Value theArray = aArguments.get(0);
                final Value theIndex = aArguments.get(1);

                final Value theValue = new ArrayEntryExpression(aProgram, aInstruction.getOpcodeAddress(), TypeRef.Native.BOOLEAN, theArray, theIndex);
                final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.Native.BOOLEAN, theValue);
                aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);

                return true;
            }
            if ("setByte".equals(aMethodName)) {

                final Value theArray = aArguments.get(0);
                final Value theIndex = aArguments.get(1);
                final Value theValue = aArguments.get(2);

                aTargetBlock.getExpressions().add(new ArrayStoreExpression(aProgram, aInstruction.getOpcodeAddress(),
                        TypeRef.Native.BYTE, theArray, theIndex, theValue));

                return true;
            }
            if ("getByte".equals(aMethodName)) {

                final Value theArray = aArguments.get(0);
                final Value theIndex = aArguments.get(1);

                final Value theValue = new ArrayEntryExpression(aProgram, aInstruction.getOpcodeAddress(), TypeRef.Native.BYTE, theArray, theIndex);
                final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.Native.BYTE, theValue);
                aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);

                return true;
            }
            if ("setChar".equals(aMethodName)) {

                final Value theArray = aArguments.get(0);
                final Value theIndex = aArguments.get(1);
                final Value theValue = aArguments.get(2);

                aTargetBlock.getExpressions().add(new ArrayStoreExpression(aProgram, aInstruction.getOpcodeAddress(),
                        TypeRef.Native.CHAR, theArray, theIndex, theValue));

                return true;
            }
            if ("getChar".equals(aMethodName)) {

                final Value theArray = aArguments.get(0);
                final Value theIndex = aArguments.get(1);

                final Value theValue = new ArrayEntryExpression(aProgram, aInstruction.getOpcodeAddress(), TypeRef.Native.CHAR, theArray, theIndex);
                final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.Native.CHAR, theValue);
                aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);

                return true;
            }
            if ("setDouble".equals(aMethodName)) {

                final Value theArray = aArguments.get(0);
                final Value theIndex = aArguments.get(1);
                final Value theValue = aArguments.get(2);

                aTargetBlock.getExpressions().add(new ArrayStoreExpression(aProgram, aInstruction.getOpcodeAddress(),
                        TypeRef.Native.DOUBLE, theArray, theIndex, theValue));

                return true;
            }
            if ("getDouble".equals(aMethodName)) {

                final Value theArray = aArguments.get(0);
                final Value theIndex = aArguments.get(1);

                final Value theValue = new ArrayEntryExpression(aProgram, aInstruction.getOpcodeAddress(), TypeRef.Native.DOUBLE, theArray, theIndex);
                final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.Native.DOUBLE, theValue);
                aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);

                return true;
            }
            if ("setFloat".equals(aMethodName)) {

                final Value theArray = aArguments.get(0);
                final Value theIndex = aArguments.get(1);
                final Value theValue = aArguments.get(2);

                aTargetBlock.getExpressions().add(new ArrayStoreExpression(aProgram, aInstruction.getOpcodeAddress(),
                        TypeRef.Native.FLOAT, theArray, theIndex, theValue));

                return true;
            }
            if ("getFloat".equals(aMethodName)) {

                final Value theArray = aArguments.get(0);
                final Value theIndex = aArguments.get(1);

                final Value theValue = new ArrayEntryExpression(aProgram, aInstruction.getOpcodeAddress(), TypeRef.Native.FLOAT, theArray, theIndex);
                final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.Native.FLOAT, theValue);
                aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);

                return true;
            }
            if ("setInt".equals(aMethodName)) {

                final Value theArray = aArguments.get(0);
                final Value theIndex = aArguments.get(1);
                final Value theValue = aArguments.get(2);

                aTargetBlock.getExpressions().add(new ArrayStoreExpression(aProgram, aInstruction.getOpcodeAddress(),
                        TypeRef.Native.INT, theArray, theIndex, theValue));

                return true;
            }
            if ("getInt".equals(aMethodName)) {

                final Value theArray = aArguments.get(0);
                final Value theIndex = aArguments.get(1);

                final Value theValue = new ArrayEntryExpression(aProgram, aInstruction.getOpcodeAddress(), TypeRef.Native.INT, theArray, theIndex);
                final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.Native.INT, theValue);
                aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);

                return true;
            }
            if ("setLong".equals(aMethodName)) {

                final Value theArray = aArguments.get(0);
                final Value theIndex = aArguments.get(1);
                final Value theValue = aArguments.get(2);

                aTargetBlock.getExpressions().add(new ArrayStoreExpression(aProgram, aInstruction.getOpcodeAddress(),
                        TypeRef.Native.LONG, theArray, theIndex, theValue));

                return true;
            }
            if ("getLong".equals(aMethodName)) {

                final Value theArray = aArguments.get(0);
                final Value theIndex = aArguments.get(1);

                final Value theValue = new ArrayEntryExpression(aProgram, aInstruction.getOpcodeAddress(), TypeRef.Native.LONG, theArray, theIndex);
                final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.Native.LONG, theValue);
                aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);

                return true;
            }
            if ("setShort".equals(aMethodName)) {

                final Value theArray = aArguments.get(0);
                final Value theIndex = aArguments.get(1);
                final Value theValue = aArguments.get(2);

                aTargetBlock.getExpressions().add(new ArrayStoreExpression(aProgram, aInstruction.getOpcodeAddress(),
                        TypeRef.Native.SHORT, theArray, theIndex, theValue));

                return true;
            }
            if ("getShort".equals(aMethodName)) {

                final Value theArray = aArguments.get(0);
                final Value theIndex = aArguments.get(1);

                final Value theValue = new ArrayEntryExpression(aProgram, aInstruction.getOpcodeAddress(), TypeRef.Native.SHORT, theArray, theIndex);
                final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.Native.SHORT, theValue);
                aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);

                return true;
            }
            if ("set".equals(aMethodName)) {

                final Value theArray = aArguments.get(0);
                final Value theIndex = aArguments.get(1);
                final Value theValue = aArguments.get(2);

                aTargetBlock.getExpressions().add(new ArrayStoreExpression(aProgram, aInstruction.getOpcodeAddress(),
                        TypeRef.toType(BytecodeObjectTypeRef.fromRuntimeClass(Object.class)), theArray, theIndex, theValue));

                return true;
            }
            if ("get".equals(aMethodName)) {

                final Value theArray = aArguments.get(0);
                final Value theIndex = aArguments.get(1);

                final Value theValue = new ArrayEntryExpression(aProgram, aInstruction.getOpcodeAddress(), TypeRef.toType(BytecodeObjectTypeRef.fromRuntimeClass(Object.class)), theArray, theIndex);
                final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.toType(BytecodeObjectTypeRef.fromRuntimeClass(Object.class)), theValue);
                aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);

                return true;

            }
            if ("newArray".equals(aMethodName)) {

                final Value theType = aArguments.get(0);
                final Value theLength = aArguments.get(1);

                final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(),
                        TypeRef.Native.REFERENCE, new NewArrayExpression(aProgram, aInstruction.getOpcodeAddress(), BytecodeObjectTypeRef.fromRuntimeClass(Object.class), theLength));
                aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);

                return true;
            }
        }

        return false;
    }
}
