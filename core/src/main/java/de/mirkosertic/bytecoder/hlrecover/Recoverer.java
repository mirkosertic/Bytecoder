/*
 * Copyright 2019 Mirko Sertic
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
package de.mirkosertic.bytecoder.hlrecover;

import de.mirkosertic.bytecoder.core.BytecodeBasicBlock;
import de.mirkosertic.bytecoder.core.BytecodeClass;
import de.mirkosertic.bytecoder.core.BytecodeClassinfoConstant;
import de.mirkosertic.bytecoder.core.BytecodeCodeAttributeInfo;
import de.mirkosertic.bytecoder.core.BytecodeExceptionTableEntry;
import de.mirkosertic.bytecoder.core.BytecodeInstruction;
import de.mirkosertic.bytecoder.core.BytecodeInstructionATHROW;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericRETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionInvoke;
import de.mirkosertic.bytecoder.core.BytecodeInstructionObjectRETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionRET;
import de.mirkosertic.bytecoder.core.BytecodeInstructionRETURN;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.core.BytecodeProgram;
import de.mirkosertic.bytecoder.ssa.RegionNode;

import javax.swing.plaf.basic.BasicCheckBoxMenuItemUI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Recoverer {

    public Element generateFrom(BytecodeClass aOwningClass, BytecodeMethod aMethod) {
        final BytecodeCodeAttributeInfo theCode = aMethod.getCode(aOwningClass);
        final BytecodeProgram theProgram = theCode.getProgram();

        // First, we create a list of basic blocks
        final Map<BytecodeOpcodeAddress, BytecodeBasicBlock> theKnownBlocks = new HashMap<>();
        final Set<BytecodeOpcodeAddress> theJumpTargets = theProgram.getJumpTargets();
        BytecodeBasicBlock theCurrentBlock = null;
        for (BytecodeInstruction theInstruction : theProgram.getInstructions()) {
            if (theJumpTargets.contains(theInstruction.getOpcodeAddress())) {
                // Jump target, start a new basic block
                theCurrentBlock = null;
            }
            if (theProgram.isStartOfTryBlock(theInstruction.getOpcodeAddress())) {
                // start of try block, hence new basic block
                theCurrentBlock = null;
            }

            if (theCurrentBlock == null) {
                theCurrentBlock = new BytecodeBasicBlock(BytecodeBasicBlock.Type.NORMAL);
                theKnownBlocks.put(theInstruction.getOpcodeAddress(), theCurrentBlock);
            }
            theCurrentBlock.addInstruction(theInstruction);

            if (theInstruction.isJumpSource()) {
                // conditional or unconditional jump, start new basic block
                theCurrentBlock = null;
            } else if (theInstruction instanceof BytecodeInstructionRET) {
                // returning, start new basic block
                theCurrentBlock = null;
            } else if (theInstruction instanceof BytecodeInstructionRETURN) {
                // returning, start new basic block
                theCurrentBlock = null;
            } else if (theInstruction instanceof BytecodeInstructionObjectRETURN) {
                // returning, start new basic block
                theCurrentBlock = null;
            } else if (theInstruction instanceof BytecodeInstructionGenericRETURN) {
                // returning, start new basic block
                theCurrentBlock = null;
            } else if (theInstruction instanceof BytecodeInstructionATHROW) {
                // thowing an exception, start new basic block
                theCurrentBlock = null;
            }
        }

        // Calculate edges
        generateEdges(theProgram, theKnownBlocks.get(new BytecodeOpcodeAddress(0)), new HashSet<>(), theKnownBlocks);

        return null;
    }

    private void generateEdges(BytecodeProgram aProgram, BytecodeBasicBlock aBlock, Set<BytecodeBasicBlock> aAlreadySeen, Map<BytecodeOpcodeAddress, BytecodeBasicBlock> aBlocks) {
        aAlreadySeen.add(aBlock);
        for (BytecodeInstruction theInstruction : aBlock.getInstructions()) {
            if (theInstruction.isJumpSource()) {
                for (BytecodeOpcodeAddress theTarget : theInstruction.getPotentialJumpTargets()) {
                    BytecodeBasicBlock theTargetBlock = aBlocks.get(theTarget);
                    if (!aAlreadySeen.contains(theTargetBlock)) {
                        // Normal edge
                        aBlock.addSuccessor(theTargetBlock);
                        generateEdges(aProgram, theTargetBlock, new HashSet<>(aAlreadySeen), aBlocks);
                    } else {
                        // Back edge
                        theTargetBlock.addBackEdge(aBlock);
                        aBlock.addSuccessor(theTargetBlock);
                    }
                }
            }
        }

        if (!aBlock.endsWithReturn() && !aBlock.endsWithThrow() && !aBlock.endsWithGoto()) {
            BytecodeInstruction theLast = aBlock.lastInstruction();
            BytecodeInstruction theNext = aProgram.nextInstructionOf(theLast);
            BytecodeBasicBlock theNextBlock = aBlocks.get(theNext.getOpcodeAddress());
            aBlock.addSuccessor(theNextBlock);
            generateEdges(aProgram, theNextBlock, new HashSet<>(aAlreadySeen), aBlocks);
        }
    }
}