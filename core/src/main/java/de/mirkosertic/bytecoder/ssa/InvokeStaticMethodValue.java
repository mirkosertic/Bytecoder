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

import de.mirkosertic.bytecoder.core.BytecodeMethodRefConstant;

import java.util.List;

public class InvokeStaticMethodValue extends Value {

    private final BytecodeMethodRefConstant method;
    private final List<Variable> arguments;

    public InvokeStaticMethodValue(BytecodeMethodRefConstant aMethod, List<Variable> aArguments) {
        method = aMethod;
        arguments = aArguments;
        for (Variable theVariable : aArguments) {
            theVariable.usedBy(this);
        }
    }

    public BytecodeMethodRefConstant getMethod() {
        return method;
    }

    public List<Variable> getArguments() {
        return arguments;
    }
}
