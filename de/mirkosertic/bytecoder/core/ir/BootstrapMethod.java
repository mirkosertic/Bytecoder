/*
 * Copyright 2023 Mirko Sertic
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
package de.mirkosertic.bytecoder.core.ir;

import org.objectweb.asm.Type;

import java.lang.invoke.CallSite;

public class BootstrapMethod extends Value {

    public final Type methodType;

    public final Type className;

    public final String methodName;

    public final MethodReference.Kind kind;

    public BootstrapMethod(final Type methodType, final Type className, final String methodName,
                           final MethodReference.Kind kind) {
        super(Type.getType(CallSite.class));
        this.methodType = methodType;
        this.className = className;
        this.methodName = methodName;
        this.kind = kind;
    }
}
