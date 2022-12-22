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

public class GraphParserState {

    final Node[] locals;
    final Node[] stack;

    final ControlTokenConsumerNode lastControlTokenConsumer;

    final int lineNumber;

    public GraphParserState(final Node[] locals, final Node[] stack, final ControlTokenConsumerNode lastControlTokenConsumer, final int lineNumber) {
        this.locals = locals;
        this.stack = stack;
        this.lastControlTokenConsumer = lastControlTokenConsumer;
        this.lineNumber = lineNumber;
    }

    public GraphParserState controlFlowsTo(final ControlTokenConsumerNode node) {
        if (lastControlTokenConsumer != null) {
            lastControlTokenConsumer.addControlFlowTo(node);
        }
        return new GraphParserState(locals, stack, node, lineNumber);
    }

    public GraphParserState withLineNumber(final int line) {
        return new GraphParserState(locals, stack, lastControlTokenConsumer, lineNumber);
    }

    public GraphParserState withNewStack(final Node[] newStack) {
        return new GraphParserState(locals, newStack, lastControlTokenConsumer, lineNumber);
    }
    public GraphParserState withStackAndLocals(final Node[] newStack, final Node[] newLocals) {
        return new GraphParserState(newLocals, newStack, lastControlTokenConsumer, lineNumber);
    }

    public GraphParserState setLocalWithStack(final int local, final Node value, final Node[] newStack) {
        final Node[] newLocals = new Node[locals.length];
        System.arraycopy(locals, 0, newLocals, 0, locals.length);
        newLocals[local] = value;
        return new GraphParserState(newLocals, newStack, lastControlTokenConsumer, lineNumber);
    }
}
