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
package de.mirkosertic.bytecoder.asm.parser;

import de.mirkosertic.bytecoder.asm.ir.ControlTokenConsumer;
import de.mirkosertic.bytecoder.asm.ir.Copy;
import de.mirkosertic.bytecoder.asm.ir.EdgeType;
import de.mirkosertic.bytecoder.asm.ir.Fixup;
import de.mirkosertic.bytecoder.asm.ir.Frame;
import de.mirkosertic.bytecoder.asm.ir.Graph;
import de.mirkosertic.bytecoder.asm.ir.InstructionTranslation;
import de.mirkosertic.bytecoder.asm.ir.Projection;
import de.mirkosertic.bytecoder.asm.ir.StandardProjections;
import de.mirkosertic.bytecoder.asm.ir.Value;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;

import java.util.Map;

public class ControlFlowFixup implements Fixup {

    private final AbstractInsnNode sourceInstruction;
    private final Projection projection;
    private final AbstractInsnNode targetInstruction;

    private final Frame frame;

    public ControlFlowFixup(final AbstractInsnNode sourceInstruction, final Frame frame, final Projection projection, final AbstractInsnNode targetInstruction) {
        this.sourceInstruction = sourceInstruction;
        this.projection = projection;
        this.targetInstruction = targetInstruction;
        this.frame = frame;
    }

    private boolean assignableTypes(final Type a, final Type b) {
        if (a.getSort() == Type.OBJECT) {
            return b.getSort() == Type.OBJECT || b.getSort() == Type.ARRAY;
        }
        if (a.getSort() == Type.ARRAY) {
            return b.getSort() == Type.ARRAY;
        }
        return true;
    }

    @Override
    public void applyTo(final Graph g, final Map<AbstractInsnNode, Map<AbstractInsnNode, EdgeType>> incomingEdgesPerInstruction) {
        final InstructionTranslation translation = g.translationFor(targetInstruction);
        if (translation != null) {
            final InstructionTranslation sourcetranslation = g.translationFor(sourceInstruction);
            if (sourcetranslation == null) {
                throw new IllegalStateException("No translation for source " + sourceInstruction + " found!");
            }
            final Frame targetFrame = translation.frame;
            ControlTokenConsumer current = sourcetranslation.main;
            Projection p = projection;
            for (int i = 0; i < frame.incomingLocals.length; i++) {
                final Value sourceValue = frame.incomingLocals[i];
                final Value targetValue = targetFrame.incomingLocals[i];
                if (sourceValue != null && sourceValue != targetValue && targetValue != null) {
                    if (assignableTypes(sourceValue.type, targetValue.type)) {
                        final Copy c = g.newCopy();
                        c.addIncomingData(sourceValue);
                        targetValue.addIncomingData(c);
                        current.addControlFlowTo(p, c);
                        current = c;
                        p = StandardProjections.DEFAULT;
                    }
                }
            }
            for (int i = 0; i < frame.incomingStack.length; i++) {
                final Value sourceValue = frame.incomingStack[i];
                final Value targetValue = targetFrame.incomingStack[i];
                if (sourceValue != null && sourceValue != targetValue) {
                    if (assignableTypes(sourceValue.type, targetValue.type)) {
                        final Copy c = g.newCopy();
                        c.addIncomingData(sourceValue);
                        targetValue.addIncomingData(c);
                        current.addControlFlowTo(p, c);
                        current = c;
                        p = StandardProjections.DEFAULT;
                    }
                }
            }

            // TODO: Check for the correct edge type here!!
            final Map<AbstractInsnNode, EdgeType> incomingEdges = incomingEdgesPerInstruction.get(targetInstruction);
            if (incomingEdges != null) {
                final EdgeType edgeType = incomingEdges.get(sourceInstruction);
                if (edgeType != null) {
                    if (current == sourcetranslation.main) {
                        // No copy instruction generated
                        current.addControlFlowTo(projection.withEdgeType(edgeType), translation.main);
                    } else {
                        current.addControlFlowTo(p.withEdgeType(edgeType), translation.main);
                    }
                } else {
                    if (sourceInstruction instanceof LabelNode) {
                        throw new IllegalStateException("No incoming edges found for " + ((LabelNode) sourceInstruction).getLabel() + " to jump to " + targetInstruction);
                    } else {
                        throw new IllegalStateException("No incoming edges found for " + sourceInstruction + " to jump to " + targetInstruction);
                    }
                }
            } else {
                throw new IllegalStateException("Confused : no incoming edges for " + targetInstruction + " from " + sourceInstruction);
            }
        } else {
            throw new IllegalStateException("No translation found for " + translation + " opcode " + targetInstruction.getOpcode());
        }
    }
}
