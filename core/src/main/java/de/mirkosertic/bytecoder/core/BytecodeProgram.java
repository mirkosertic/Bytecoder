/*
 * Copyright 2017 Mirko Sertic
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
package de.mirkosertic.bytecoder.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class BytecodeProgram {

    public class FlowInformation {

        private final BytecodeOpcodeAddress regularStartNode;
        private final Map<BytecodeOpcodeAddress, Set<BytecodeBasicBlock>> roots;
        private final Map<BytecodeOpcodeAddress, BytecodeBasicBlock> knownBlocks;

        public FlowInformation(final BytecodeOpcodeAddress regularStartNode, final Map<BytecodeOpcodeAddress, Set<BytecodeBasicBlock>> roots, final Map<BytecodeOpcodeAddress, BytecodeBasicBlock> knownBlocks) {
            this.regularStartNode = regularStartNode;
            this.roots = roots;
            this.knownBlocks = knownBlocks;
        }

        public BytecodeProgram getProgram() {
            return BytecodeProgram.this;
        }
    }

    private final List<BytecodeInstruction> instructions;
    private final List<BytecodeExceptionTableEntry> exceptionHandlers;

    public BytecodeProgram() {
        instructions = new ArrayList<>();
        exceptionHandlers = new ArrayList<>();
    }

    public void addInstruction(BytecodeInstruction aInstruction) {
        instructions.add(aInstruction);
    }

    public void addExceptionHandler(BytecodeExceptionTableEntry aHandler) {
        exceptionHandlers.add(aHandler);
    }

    public List<BytecodeInstruction> getInstructions() {
        return instructions;
    }

    public BytecodeExceptionTableEntry[] getActiveExceptionHandlers(BytecodeOpcodeAddress aAddress, List<BytecodeExceptionTableEntry> aExceptionHandlerEntries) {
        List<BytecodeExceptionTableEntry> theResult = new ArrayList<>();
        for (BytecodeExceptionTableEntry aEntry : aExceptionHandlerEntries) {
            if (!aEntry.isFinally()) {
                if (aEntry.getStartPC().isBefore(aAddress) && aEntry.getEndPc().isAfter(aAddress)) {
                    theResult.add(aEntry);
                }
            }
        }
        return theResult.toArray(new BytecodeExceptionTableEntry[theResult.size()]);
    }

    public boolean isStartOfTryBlock(BytecodeOpcodeAddress aAddress) {
        for (BytecodeExceptionTableEntry aEntry : exceptionHandlers) {
            if (aAddress.equals(aEntry.getStartPC())) {
                return true;
            }
        }
        return false;
    }

    public Set<BytecodeOpcodeAddress> getJumpTargets() {
        Set<BytecodeOpcodeAddress> theJumpTarget = new HashSet();
        for (BytecodeInstruction theInstruction : instructions) {
            if (theInstruction.isJumpSource()) {
                theJumpTarget.addAll(Arrays.asList(theInstruction.getPotentialJumpTargets()));
            }
        }
        for (BytecodeExceptionTableEntry aEntry: exceptionHandlers) {
            theJumpTarget.add(aEntry.getHandlerPc());
        }
        return theJumpTarget;
    }

    public List<BytecodeExceptionTableEntry> getExceptionHandlers() {
        return exceptionHandlers;
    }

    public BytecodeInstruction nextInstructionOf(BytecodeInstruction aInstruction) {
        int i = instructions.indexOf(aInstruction);
        return instructions.get(i + 1);
    }

    public FlowInformation toFlow() {

        // First, we create a list of basic blocks
        final Map<BytecodeOpcodeAddress, BytecodeBasicBlock> theKnownBlocks = new HashMap<>();
        final Set<BytecodeOpcodeAddress> theJumpTargets = getJumpTargets();
        BytecodeBasicBlock theCurrentBlock = null;
        for (BytecodeInstruction theInstruction : instructions) {
            if (theJumpTargets.contains(theInstruction.getOpcodeAddress())) {
                // Jump target, start a new basic block
                theCurrentBlock = null;
            }
            if (isStartOfTryBlock(theInstruction.getOpcodeAddress())) {
                // start of try block, hence new basic block
                theCurrentBlock = null;
            }

            if (theCurrentBlock == null) {
                BytecodeClassinfoConstant theCatchType = null;
                BytecodeBasicBlock.Type theType = BytecodeBasicBlock.Type.NORMAL;
                for (final BytecodeExceptionTableEntry theHandler : getExceptionHandlers()) {
                    if (Objects.equals(theHandler.getHandlerPc(), theInstruction.getOpcodeAddress())) {
                        if (theHandler.isFinally()) {
                            theType = BytecodeBasicBlock.Type.FINALLY;
                        } else {
                            theType = BytecodeBasicBlock.Type.EXCEPTION_HANDLER;
                            theCatchType = theHandler.getCatchType();
                        }
                    }
                }

                if (theCatchType != null) {
                    theCurrentBlock = new BytecodeBasicBlock(theCatchType);
                } else {
                    theCurrentBlock = new BytecodeBasicBlock(theType);
                }

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

        BytecodeOpcodeAddress theRegularStart = new BytecodeOpcodeAddress(0);
        // Calculage regular control flow
        Map<BytecodeOpcodeAddress, Set<BytecodeBasicBlock>> theRoots = new HashMap<>();
        Set<BytecodeBasicBlock> theRegularBlocks = new HashSet<>();
        theRoots.put(theRegularStart, generateEdges(theKnownBlocks.get(theRegularStart), theRegularBlocks, theKnownBlocks));

        // Calculate the program flow for finally blocks and exception handlers
        // till their merging with the regular control flow
        for (BytecodeBasicBlock theBlock : theKnownBlocks.values()) {
            if (theBlock.getType() != BytecodeBasicBlock.Type.NORMAL) {
                theRoots.put(theBlock.getStartAddress(), generateEdges(theBlock, new HashSet<>(theRegularBlocks), theKnownBlocks));
            }
        }

        // We are done here
        return new FlowInformation(theRegularStart, theRoots, theKnownBlocks);
    }

    private Set<BytecodeBasicBlock> generateEdges(BytecodeBasicBlock aBlock, Set<BytecodeBasicBlock> aAlreadySeen, Map<BytecodeOpcodeAddress, BytecodeBasicBlock> aBlocks) {
        aAlreadySeen.add(aBlock);
        for (BytecodeInstruction theInstruction : aBlock.getInstructions()) {
            if (theInstruction.isJumpSource()) {
                for (BytecodeOpcodeAddress theTarget : theInstruction.getPotentialJumpTargets()) {
                    BytecodeBasicBlock theTargetBlock = aBlocks.get(theTarget);
                    if (!aAlreadySeen.contains(theTargetBlock)) {
                        // Normal edge
                        aBlock.addSuccessor(theTargetBlock);
                        generateEdges(theTargetBlock, new HashSet<>(aAlreadySeen), aBlocks);
                    } else {
                        // Back edge
                        theTargetBlock.addBackEdge(aBlock);
                        aBlock.addSuccessor(theTargetBlock);
                    }
                }
            }
        }

        // Properly handle the fall thru case
        if (!aBlock.endsWithReturn() && !aBlock.endsWithThrow() && !aBlock.endsWithGoto()) {
            BytecodeInstruction theLast = aBlock.lastInstruction();
            BytecodeInstruction theNext = nextInstructionOf(theLast);
            BytecodeBasicBlock theNextBlock = aBlocks.get(theNext.getOpcodeAddress());
            aBlock.addSuccessor(theNextBlock);
            generateEdges(theNextBlock, new HashSet<>(aAlreadySeen), aBlocks);
        }

        return aAlreadySeen;
    }
}