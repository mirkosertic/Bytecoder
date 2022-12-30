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

import org.objectweb.asm.tree.LabelNode;

public class GraphParserState {

    final Frame frame;

    final ControlTokenConsumer lastControlTokenConsumer;

    final int lineNumber;

    final TryCatchGuardStackEntry[] tryCatchGuardStack;

    public GraphParserState(final Frame frame, final ControlTokenConsumer lastControlTokenConsumer, final int lineNumber, final TryCatchGuardStackEntry[] tryCatchGuardStack) {
        this.frame = frame;
        this.lastControlTokenConsumer = lastControlTokenConsumer;
        this.lineNumber = lineNumber;
        this.tryCatchGuardStack = tryCatchGuardStack;
    }

    public GraphParserState controlFlowsTo(final ControlTokenConsumer node) {
        return new GraphParserState(frame, node, lineNumber, tryCatchGuardStack);
    }

    public GraphParserState withLineNumber(final int line) {
        return new GraphParserState(frame, lastControlTokenConsumer, line, tryCatchGuardStack);
    }

    public GraphParserState withFrame(final Frame frame) {
        return new GraphParserState(frame, lastControlTokenConsumer, lineNumber, tryCatchGuardStack);
    }

    public GraphParserState withNewTryCatchOnStack(final TryCatchGuardStackEntry newTryCatchGuardStackEntry) {
        final TryCatchGuardStackEntry[] newTryCatchStackEntries = new TryCatchGuardStackEntry[tryCatchGuardStack.length + 1];
        System.arraycopy(tryCatchGuardStack, 0, newTryCatchStackEntries, 0, tryCatchGuardStack.length);
        newTryCatchStackEntries[tryCatchGuardStack.length] = newTryCatchGuardStackEntry;
        return new GraphParserState(frame, lastControlTokenConsumer, lineNumber, newTryCatchStackEntries);
    }

    public boolean isEndOfTryCatchGuardBlock(final LabelNode node) {
        if (tryCatchGuardStack.length > 0) {
            return tryCatchGuardStack[tryCatchGuardStack.length - 1].endLabel == node;
        }
        return false;
    }

    public GraphParserState popLatestTryBatchGuardBlock() {
        final TryCatchGuardStackEntry[] newTryCatchStackEntries = new TryCatchGuardStackEntry[tryCatchGuardStack.length -1];
        System.arraycopy(tryCatchGuardStack, 0, newTryCatchStackEntries, 0, tryCatchGuardStack.length - 1);
        return new GraphParserState(frame, lastControlTokenConsumer, lineNumber, newTryCatchStackEntries);
    }
}
