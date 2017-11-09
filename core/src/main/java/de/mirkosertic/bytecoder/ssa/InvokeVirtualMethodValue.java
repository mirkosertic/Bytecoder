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

import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeNameAndTypeConstant;

import java.util.List;

public class InvokeVirtualMethodValue extends Value {

    private final String methodName;
    private final BytecodeMethodSignature signature;
    private final Variable target;
    private final List<Variable> arguments;

    public InvokeVirtualMethodValue(BytecodeNameAndTypeConstant aMethod, Variable aTarget,
                                    List<Variable> aArguments) {
        this(aMethod.getNameIndex().getName().stringValue(), aMethod.getDescriptorIndex().methodSignature(),
                aTarget, aArguments);
    }

    public InvokeVirtualMethodValue(String aMethodName, BytecodeMethodSignature aSignature, Variable aTarget,
            List<Variable> aArguments) {
        methodName = aMethodName;
        signature = aSignature;
        target = aTarget;
        arguments = aArguments;
    }


    public String getMethodName() {
        return methodName;
    }

    public BytecodeMethodSignature getSignature() {
        return signature;
    }

    public Variable getTarget() {
        return target;
    }

    public List<Variable> getArguments() {
        return arguments;
    }
}
