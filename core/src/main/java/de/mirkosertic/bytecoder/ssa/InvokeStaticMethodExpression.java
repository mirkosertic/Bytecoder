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
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;

public class InvokeStaticMethodExpression extends InvocationExpression {

    private final BytecodeObjectTypeRef className;
    private final String methodName;

    public InvokeStaticMethodExpression(final Program aProgram, final BytecodeOpcodeAddress aAddress, final BytecodeObjectTypeRef aClassName, final String aMethodName,
            final BytecodeMethodSignature aSignature, final List<Value> aArguments) {
        super(aProgram, aAddress, aSignature);
        className = aClassName;
        methodName = aMethodName;

        receivesDataFrom(aArguments);
    }

    public BytecodeObjectTypeRef getInvokedClass() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }
}
