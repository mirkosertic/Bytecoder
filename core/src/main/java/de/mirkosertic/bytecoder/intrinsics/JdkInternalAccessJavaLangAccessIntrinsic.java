/*
 * Copyright 2021 Mirko Sertic
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

import java.util.List;
import java.util.Objects;

import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKEINTERFACE;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeUtf8Constant;
import de.mirkosertic.bytecoder.ssa.InvokeStaticMethodExpression;
import de.mirkosertic.bytecoder.ssa.ParsingHelper;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;

public class JdkInternalAccessJavaLangAccessIntrinsic extends Intrinsic {

    @Override
    public boolean intrinsify(final Program aProgram,
                              final BytecodeInstructionINVOKEINTERFACE aInstruction,
                              final String aMethodName,
                              final Value aInvocationTarget,
                              final List<Value> aArguments,
                              final BytecodeObjectTypeRef aObjectType,
                              final RegionNode aTargetBlock,
                              final ParsingHelper aHelper) {
        final BytecodeMethodSignature theSignature = aInstruction.getMethodDescriptor().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

        if (Objects.equals(aObjectType.name(), "jdk.internal.access.JavaLangAccess")) {
            if (Objects.equals("inflateBytesToChars", aMethodName)) {
                final BytecodeObjectTypeRef theClassToInvoke = BytecodeObjectTypeRef.fromUtf8Constant(new BytecodeUtf8Constant("java/lang/StringLatin1"));
                aProgram.getLinkerContext().resolveClass(theClassToInvoke)
                        .resolveStaticMethod("inflate",
                                theSignature);

                final InvokeStaticMethodExpression theExpression = new InvokeStaticMethodExpression(aProgram, aInstruction.getOpcodeAddress(),
                        theClassToInvoke,
                        "inflate",
                        theSignature,
                        aArguments);

                aTargetBlock.getExpressions().add(theExpression);
                return true;
            }
            if (Objects.equals("decodeASCII", aMethodName)) {
                final BytecodeObjectTypeRef theClassToInvoke = BytecodeObjectTypeRef.fromRuntimeClass(String.class);
                aProgram.getLinkerContext().resolveClass(theClassToInvoke)
                        .resolveStaticMethod("decodeASCII",
                                theSignature);

                final InvokeStaticMethodExpression theExpression = new InvokeStaticMethodExpression(aProgram, aInstruction.getOpcodeAddress(),
                        theClassToInvoke,
                        "decodeASCII",
                        theSignature,
                        aArguments);

                final Variable theNewVariable = aTargetBlock.newVariable(aInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), theExpression);
                aHelper.push(aInstruction.getOpcodeAddress(), theNewVariable);

                return true;
            }
            throw new IllegalArgumentException("Not supported invocation of " + aMethodName + " on " + aObjectType.name());
        }
        return false;
    }
}
