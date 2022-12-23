/*
 * Copyright 2022 Mirko Sertic
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

public class Frame {

    public static class PopResult {
        public final Frame newFrame;

        public final VarNode value;

        PopResult(final Frame newFrame, final VarNode value) {
            this.newFrame = newFrame;
            this.value = value;
        }
    }

    final VarNode[] incomingLocals;
    final VarNode[] incomingStack;

    public Frame(final VarNode[] incomingLocals, final VarNode[] incomingStack) {
        this.incomingLocals = incomingLocals;
        this.incomingStack = incomingStack;
    }

    public Frame setLocal(final int local, final VarNode value) {
        final VarNode[] newLocals = new VarNode[incomingLocals.length];
        System.arraycopy(incomingLocals, 0, newLocals, 0, incomingLocals.length);
        newLocals[local] = value;
        return new Frame(newLocals, incomingStack);
    }

    public Frame pushToStack(final VarNode variable) {
        final VarNode[] newStack = new VarNode[incomingStack.length + 1];
        System.arraycopy(incomingStack, 0, newStack, 0, incomingStack.length);
        newStack[newStack.length - 1] = variable;
        return new Frame(incomingLocals, newStack);
    }

    public PopResult popFromStack() {
        final VarNode[] newStack = new VarNode[incomingStack.length - 1];
        System.arraycopy(incomingStack, 0, newStack, 0, newStack.length);

        final VarNode value = incomingStack[incomingStack.length - 1];

        return new PopResult(new Frame(incomingLocals, newStack), value);
    }

    public Frame withLocalsAndStack(final VarNode[] locals, final VarNode[] stack) {
        return new Frame(locals, stack);
    }
}
