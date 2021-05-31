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
package de.mirkosertic.bytecoder.asm;

import org.objectweb.asm.Type;

public class ResolveStaticMethodInvocationCommand implements Command {

    private final Type type;
    private final String methodName;
    private final String methodDescriptor;

    public ResolveStaticMethodInvocationCommand(final Type type, final String methodName, final String methodDescriptor) {
        this.type = type;
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
    }

    @Override
    public void execute(final CompilationUnit compilationUnit) {
        final ResolvedClass resolvedClass = compilationUnit.resolveClass(type.getClassName());
        resolvedClass.flagWith(ResolvedClass.Flag.USED_AS_STATIC_CALL_TARGET);

        final Type returnType = Type.getReturnType(methodDescriptor);
        final Type[] arguments = Type.getArgumentTypes(methodDescriptor);

        final ResolvedClass returnTypeClass = compilationUnit.resolveType(returnType);
        if (returnTypeClass != null) {
            returnTypeClass.flagWith(ResolvedClass.Flag.USED_BY_METHOD_SIGNATURE);
        }

        for (final Type argument : arguments) {
            final ResolvedClass argumentTypeClass = compilationUnit.resolveType(argument);
            if (argumentTypeClass != null) {
                argumentTypeClass.flagWith(ResolvedClass.Flag.USED_BY_METHOD_SIGNATURE);
            }
        }

        resolvedClass.resolveStaticMethod(methodName, methodDescriptor);
    }
}
