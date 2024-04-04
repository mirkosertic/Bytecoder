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

import org.objectweb.asm.tree.MethodInsnNode;

public class MethodInvocationExpression extends Value implements AbstractInvocation {

    public final MethodInsnNode insnNode;

    public final ResolvedMethod method;

    public InvocationType invocationType;

    MethodInvocationExpression(final Graph owner, final MethodInsnNode insnNode, final ResolvedMethod method, final InvocationType invocationType) {
        super(owner, method.methodType.getReturnType(), NodeType.MethodInvocationExpression);
        this.insnNode = insnNode;
        this.method = method;
        this.invocationType = invocationType;
    }

    @Override
    public String additionalDebugInfo() {
        if (insnNode == null) {
            return invocationType + " " + method.owner + "." + method.methodNode.name;
        }
        return invocationType + " " + method.owner + "." + method.methodNode.name + insnNode.desc;
    }

    @Override
    public ResolvedMethod method() {
        return method;
    }

    @Override
    public InvocationType invocationType() {
        return invocationType;
    }

    @Override
    public void changeInvocationTypeTo(final InvocationType newInvocationType) {
        invocationType = newInvocationType;
    }

    @Override
    public boolean hasSideSideEffect() {
        return true;
    }
}
