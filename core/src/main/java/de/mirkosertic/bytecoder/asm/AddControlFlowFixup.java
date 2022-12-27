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

public class AddControlFlowFixup implements Fixup {

    private final ControlTokenConsumer sourceNode;
    private final Projection projection;
    private final AbstractInsnNode targetInstruction;

    public AddControlFlowFixup(ControlTokenConsumer sourceNode, Projection projection, AbstractInsnNode targetInstruction) {
        this.sourceNode = sourceNode;
        this.projection = projection;
        this.targetInstruction = targetInstruction;
    }

    @Override
    public void applyTo(Graph g) {
        AbstractInsnNode target = this.targetInstruction;
        while ((target instanceof LineNumberNode) || (target instanceof FrameNode)) {
            target = target.getNext();
        }
        final InstructionTranslation translation = g.translationFor(target);
        if (translation != null) {
            if (projection.edgeType() == EdgeType.FORWARD) {
                ControlTokenConsumer t = translation.preludeStart;
                if (t == null) {
                    t = translation.main;
                }
                sourceNode.addControlFlowTo(projection, t);
            } else {
                ControlTokenConsumer t = translation.main;
                if (t == null) {
                    t = translation.preludeStart;
                }
                sourceNode.addControlFlowTo(projection, t);
            }
        } else {
            System.out.println("No translation found for " + target + " opcode " + target.getOpcode());
        }
    }
}
