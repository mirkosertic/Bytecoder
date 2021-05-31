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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.util.HashMap;
import java.util.Map;

public class MethodAnalyzer {

    private final Map<LabelNode, BasicBlock> blocks;
    private BasicBlock startBlock;

    public MethodAnalyzer(final CompilationUnit compilationUnit, final MethodNode methodNode) {

        blocks = new HashMap<>();

        constructBasicBlocksFromLabels(methodNode);
        tagExceptionHandlersAndFinallyBlocks(methodNode);
        processJumpsInBlocks();
    }

    private void constructBasicBlocksFromLabels(final MethodNode methodNode) {
        final InsnList insnList = methodNode.instructions;
        AbstractInsnNode currentInstruction = insnList.getFirst();
        BasicBlock currentBasicBlock = null;
        while (currentInstruction != null) {
            switch (currentInstruction.getType()) {
                case AbstractInsnNode.LABEL: {
                    final LabelNode labelNode = (LabelNode) currentInstruction;
                    final BasicBlock basicBlock = new BasicBlock(labelNode.getNext());
                    if (startBlock == null) {
                        startBlock = basicBlock;
                    } else {
                        final AbstractInsnNode prevInstruction = currentInstruction.getPrevious();
                        switch (prevInstruction.getOpcode()) {
                            case Opcodes.JSR:
                            case Opcodes.GOTO:
                            case Opcodes.RET:
                            case Opcodes.RETURN:
                            case Opcodes.IRETURN:
                            case Opcodes.LRETURN:
                            case Opcodes.FRETURN:
                            case Opcodes.DRETURN:
                            case Opcodes.ARETURN:
                            case Opcodes.ATHROW:
                            case Opcodes.LOOKUPSWITCH: {
                                break;
                            }
                            default: {
                                currentBasicBlock.addSuccessor(basicBlock);
                            }
                        }
                    }

                    blocks.put(labelNode, basicBlock);
                    currentBasicBlock = basicBlock;
                    break;
                }
            }
            currentInstruction = currentInstruction.getNext();
        }
    }

    private void tagExceptionHandlersAndFinallyBlocks(final MethodNode node) {
        for (final TryCatchBlockNode tryCatchBlockNode : node.tryCatchBlocks) {
            final BasicBlock targetBlock = blocks.get(tryCatchBlockNode.handler);
            if (targetBlock == null) {
                throw new IllegalArgumentException("Jump target basic block is not defined");
            }
            if (tryCatchBlockNode.type == null) {
                targetBlock.tag(BasicBlock.Tag.FINALLY_BLOCK);
            } else {
                targetBlock.tag(BasicBlock.Tag.EXCEPTION_HANDLER);
            }
        }
    }

    private void processJumpsInBlocks() {
        for (final BasicBlock block : blocks.values()) {
            AbstractInsnNode currentInstruction = block.start;
            while (currentInstruction != null && !(currentInstruction.getType() == AbstractInsnNode.LABEL)) {
                switch (currentInstruction.getType()) {
                    case AbstractInsnNode.JUMP_INSN: {
                        final JumpInsnNode jumpInsnNode = (JumpInsnNode) currentInstruction;
                        final BasicBlock targetBlock = blocks.get(jumpInsnNode.label);
                        if (targetBlock == null) {
                            throw new IllegalArgumentException("Jump target basic block is not defined");
                        }

                        block.addSuccessor(targetBlock);
                        break;
                    }
                    case AbstractInsnNode.LOOKUPSWITCH_INSN:
                        // TODO: Implement stuff
                        break;
                    case AbstractInsnNode.TABLESWITCH_INSN:
                        // TODO: Implement stuff
                        break;
                }
                currentInstruction = currentInstruction.getNext();
            }
        }
    }
}
