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
package de.mirkosertic.bytecoder.core.ir;

public class Frame {

    public static class PopResult {
        public final Frame newFrame;

        public final Value value;

        PopResult(final Frame newFrame, final Value value) {
            this.newFrame = newFrame;
            this.value = value;
        }
    }

    public final Value[] incomingLocals;
    public final Value[] incomingStack;

    public Frame(final Value[] incomingLocals, final Value[] incomingStack) {
        this.incomingLocals = incomingLocals;
        this.incomingStack = incomingStack;
    }

    public Frame setLocal(final int local, final Value value) {
        final Value[] newLocals = new Value[incomingLocals.length];
        System.arraycopy(incomingLocals, 0, newLocals, 0, incomingLocals.length);
        newLocals[local] = value;
        return new Frame(newLocals, incomingStack);
    }

    public Frame pushToStack(final Value variable) {
        final Value[] newStack = new Value[incomingStack.length + 1];
        System.arraycopy(incomingStack, 0, newStack, 0, incomingStack.length);
        newStack[newStack.length - 1] = variable;
        return new Frame(incomingLocals, newStack);
    }

    public PopResult popFromStack() {
        final Value[] newStack = new Value[incomingStack.length - 1];
        System.arraycopy(incomingStack, 0, newStack, 0, newStack.length);

        final Value value = incomingStack[incomingStack.length - 1];

        return new PopResult(new Frame(incomingLocals, newStack), value);
    }

    public Frame withLocalsAndStack(final Value[] locals, final Value[] stack) {
        return new Frame(locals, stack);
    }
}
