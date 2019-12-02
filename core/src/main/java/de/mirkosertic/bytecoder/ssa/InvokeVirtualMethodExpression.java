/*
 * Copyright 2017 Mirko Sertic
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
package de.mirkosertic.bytecoder.ssa;

import java.util.List;

import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeNameAndTypeConstant;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;

public class InvokeVirtualMethodExpression extends InvocationExpression {

    private final String methodName;
    private final boolean interfaceInvocation;
    private final BytecodeTypeRef invokedClass;

    public InvokeVirtualMethodExpression(final Program aProgram, final BytecodeOpcodeAddress aAddress, final BytecodeNameAndTypeConstant aMethod, final Value aTarget,
                                         final List<Value> aArguments, final boolean aInterfaceInvocation, final BytecodeTypeRef aInvokedClass) {
        this(aProgram, aAddress, aMethod.getNameIndex().getName().stringValue(), aMethod.getDescriptorIndex().methodSignature(),
                aTarget, aArguments, aInterfaceInvocation, aInvokedClass);
    }

    public InvokeVirtualMethodExpression(final Program aProgram, final BytecodeOpcodeAddress aAddress, final String aMethodName, final BytecodeMethodSignature aSignature, final Value aTarget,
                                         final List<Value> aArguments, final boolean aInterfaceInvocation, final BytecodeTypeRef aInvokedClass) {
        super(aProgram, aAddress, aSignature);
        methodName = aMethodName;
        interfaceInvocation = aInterfaceInvocation;
        invokedClass = aInvokedClass;

        receivesDataFrom(aTarget);
        receivesDataFrom(aArguments);
    }

    public String getMethodName() {
        return methodName;
    }

    public boolean isInterfaceInvocation() {
        return interfaceInvocation;
    }

    public BytecodeTypeRef getInvokedClass() {
        return invokedClass;
    }
}