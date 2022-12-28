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
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LineNumberNode;

import java.util.Map;

public class ControlFlowFixup implements Fixup {

    private final ControlTokenConsumer sourceNode;
    private final Projection projection;
    private final AbstractInsnNode targetInstruction;

    private final Frame frame;

    public ControlFlowFixup(ControlTokenConsumer sourceNode, final Frame frame, Projection projection, AbstractInsnNode targetInstruction) {
        this.sourceNode = sourceNode;
        this.projection = projection;
        this.targetInstruction = targetInstruction;
        this.frame = frame;
    }

    @Override
    public void applyTo(Graph g, Map<AbstractInsnNode, Map<AbstractInsnNode, EdgeType>> incomingEdgesPerInstruction) {
        AbstractInsnNode target = this.targetInstruction;
        while ((target instanceof LineNumberNode) || (target instanceof FrameNode)) {
            target = target.getNext();
        }
        final InstructionTranslation translation = g.translationFor(target);
        if (translation != null) {
            final Frame targetFrame = translation.frame;
            ControlTokenConsumer current = sourceNode;
            Projection p = projection;
            for (int i = 0; i < frame.incomingLocals.length; i++) {
                final Value sourceValue = frame.incomingLocals[i];
                final Value targetValue = targetFrame.incomingLocals[i];
                if (sourceValue != null && sourceValue != targetValue) {
                    final Copy c = g.newCopy(sourceValue.type);
                    c.addIncomingData(sourceValue);
                    targetValue.addIncomingData(c);
                    current.addControlFlowTo(p, c);
                    current = c;
                    p = StandardProjections.DEFAULT;
                }
            }
            for (int i = 0; i < frame.incomingStack.length; i++) {
                final Value sourceValue = frame.incomingStack[i];
                final Value targetValue = targetFrame.incomingStack[i];
                if (sourceValue != null && sourceValue != targetValue) {
                    final Copy c = g.newCopy(sourceValue.type);
                    c.addIncomingData(sourceValue);
                    targetValue.addIncomingData(c);
                    current.addControlFlowTo(p, c);
                    current = c;
                    p = StandardProjections.DEFAULT;
                }
            }

            // TODO: Check for the correct edge type here!!
            final Map<AbstractInsnNode, EdgeType> incomingEdges = incomingEdgesPerInstruction.get(target);

            if (current == sourceNode) {
                // No copy instruction generated
                current.addControlFlowTo(projection, translation.main);
            } else {
                current.addControlFlowTo(p, translation.main);
            }
        } else {
            System.out.println("No translation found for " + target + " opcode " + target.getOpcode());
        }
    }
}
