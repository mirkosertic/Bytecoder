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

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;

import java.util.ArrayList;
import java.util.List;

public class ControlFlow {
    final AbstractInsnNode currentNode;
    private final List<LabelNode> visitedLabels;

    final GraphParserState graphParserState;

    public ControlFlow(final AbstractInsnNode currentNode, final GraphParserState graphParserState) {
        this(currentNode, new ArrayList<>(), graphParserState);
    }
    ControlFlow(final AbstractInsnNode currentNode, final List<LabelNode> visitedLabels, final GraphParserState graphParserState) {
        this.currentNode = currentNode;
        this.visitedLabels = visitedLabels;
        this.graphParserState = graphParserState;
    }
    public boolean visited(final LabelNode node) {
        return visitedLabels.contains(node);
    }
    public ControlFlow addLabelAndContinueWith(final LabelNode labelNode, final AbstractInsnNode next) {
        final List<LabelNode> newVisited = new ArrayList<>(visitedLabels);
        newVisited.add(labelNode);
        return new ControlFlow(next, newVisited, graphParserState);
    }
    public ControlFlow continueWith(final AbstractInsnNode next) {
        return new ControlFlow(next, visitedLabels, graphParserState);
    }
    public ControlFlow continueWith(final AbstractInsnNode next, final GraphParserState newParserState) {
        return new ControlFlow(next, visitedLabels, newParserState);
    }

    public ControlFlow continueWith(final GraphParserState newParserState) { {
        return new ControlFlow(currentNode, visitedLabels, newParserState);
    }
    }
}
