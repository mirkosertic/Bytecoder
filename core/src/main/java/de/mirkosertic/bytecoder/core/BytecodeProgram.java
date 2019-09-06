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
import java.util.Stack;

public class BytecodeProgram {

    public class FlowInformation {

        private final Map<BytecodeOpcodeAddress, Set<BytecodeBasicBlock>> roots;
        private final Map<BytecodeOpcodeAddress, BytecodeBasicBlock> knownBlocks;

        public FlowInformation(final Map<BytecodeOpcodeAddress, Set<BytecodeBasicBlock>> roots, final Map<BytecodeOpcodeAddress, BytecodeBasicBlock> knownBlocks) {
            this.roots = roots;
            this.knownBlocks = knownBlocks;
        }

        public BytecodeProgram getProgram() {
            return BytecodeProgram.this;
        }

        public BytecodeBasicBlock blockAt(final BytecodeOpcodeAddress aBlockAddress) {
            return knownBlocks.get(aBlockAddress);
        }

        public Set<BytecodeBasicBlock> knownBlocks() {
            return new HashSet<>(knownBlocks.values());
        }
    }

    private final List<BytecodeInstruction> instructions;
    private final List<BytecodeExceptionTableEntry> exceptionHandlers;

    public BytecodeProgram() {
        instructions = new ArrayList<>();
        exceptionHandlers = new ArrayList<>();
    }

    public void addInstruction(final BytecodeInstruction aInstruction) {
        instructions.add(aInstruction);
    }

    public void addExceptionHandler(final BytecodeExceptionTableEntry aHandler) {
        exceptionHandlers.add(aHandler);
    }

    public List<BytecodeInstruction> getInstructions() {
        return instructions;
    }

    public boolean isStartOfTryBlock(final BytecodeOpcodeAddress aAddress) {
        for (final BytecodeExceptionTableEntry aEntry : exceptionHandlers) {
            if (aAddress.equals(aEntry.getStartPC())) {
                return true;
            }
        }
        return false;
    }

    public Set<BytecodeOpcodeAddress> getJumpTargets() {
        final Set<BytecodeOpcodeAddress> theJumpTarget = new HashSet();
        for (final BytecodeInstruction theInstruction : instructions) {
            if (theInstruction.isJumpSource()) {
                theJumpTarget.addAll(Arrays.asList(theInstruction.getPotentialJumpTargets()));
            }
        }
        for (final BytecodeExceptionTableEntry aEntry: exceptionHandlers) {
            theJumpTarget.add(aEntry.getHandlerPc());
        }
        return theJumpTarget;
    }

    public List<BytecodeExceptionTableEntry> getExceptionHandlers() {
        return exceptionHandlers;
    }

    public BytecodeInstruction nextInstructionOf(final BytecodeInstruction aInstruction) {
        final int i = instructions.indexOf(aInstruction);
        return instructions.get(i + 1);
    }

    public FlowInformation toFlow() {

        // First, we create a list of basic blocks
        final Map<BytecodeOpcodeAddress, BytecodeBasicBlock> theKnownBlocks = new HashMap<>();
        final Set<BytecodeOpcodeAddress> theJumpTargets = getJumpTargets();
        BytecodeBasicBlock theCurrentBlock = null;
        for (final BytecodeInstruction theInstruction : instructions) {
            if (theJumpTargets.contains(theInstruction.getOpcodeAddress())) {
                // Jump target, start a new basic block
                theCurrentBlock = null;
            }
            if (isStartOfTryBlock(theInstruction.getOpcodeAddress())) {
                // start of try block, hence new basic block
                theCurrentBlock = null;
            }

            if (theCurrentBlock == null) {
                Set<BytecodeUtf8Constant> theCatchType = null;
                BytecodeBasicBlock.Type theType = BytecodeBasicBlock.Type.NORMAL;
                for (final BytecodeExceptionTableEntry theHandler : getExceptionHandlers()) {
                    if (Objects.equals(theHandler.getHandlerPc(), theInstruction.getOpcodeAddress())) {
                        if (theHandler.isFinally()) {
                            theType = BytecodeBasicBlock.Type.FINALLY;
                        } else {
                            theType = BytecodeBasicBlock.Type.EXCEPTION_HANDLER;
                            if (theCatchType == null) {
                                theCatchType = new HashSet<>();
                            }
                            theCatchType.add(theHandler.getCatchType().getConstant());
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
            } else if (theInstruction instanceof BytecodeInstructionInvoke) {
                // Invocations might throw an exception
                // to get data flow analysis right, we start a new block after the invocation
                theCurrentBlock = null;
            }
        }

        // Now, we add the implicit exceptional control flows here
        for (final BytecodeExceptionTableEntry theHandler : exceptionHandlers) {
            final BytecodeBasicBlock theHandlerBlock = theKnownBlocks.get(theHandler.getHandlerPc());
            if (theHandlerBlock == null) {
                throw new IllegalStateException("No exception handler at " + theHandler.getHandlerPc() + " found !");
            }
            for (final Map.Entry<BytecodeOpcodeAddress, BytecodeBasicBlock> theEntry : theKnownBlocks.entrySet()) {
                if (theHandler.coveres(theEntry.getKey())) {
                    // We add an exceptional edge here
                    theEntry.getValue().addSuccessor(theHandlerBlock);
                }
            }
        }

        final BytecodeOpcodeAddress theRegularStart = new BytecodeOpcodeAddress(0);
        // Calculage regular control flow
        final Map<BytecodeOpcodeAddress, Set<BytecodeBasicBlock>> theRoots = new HashMap<>();
        final Set<BytecodeBasicBlock> theRegularBlocks = new HashSet<>();

        theRoots.put(theRegularStart, generateEdges(theRegularBlocks, theKnownBlocks.get(theRegularStart), new Stack<>(), theKnownBlocks));

        // Calculate the program flow for exception handlers
        // till their merging with the regular control flow
        for (final BytecodeBasicBlock theBlock : theKnownBlocks.values()) {
            if (theBlock.getType() == BytecodeBasicBlock.Type.EXCEPTION_HANDLER) {
                final Set<BytecodeBasicBlock> theAlreadyVisited = new HashSet<>(theRegularBlocks);
                generateEdges(theAlreadyVisited, theBlock, new Stack<>(), theKnownBlocks);
                theAlreadyVisited.removeAll(theRegularBlocks);
                theRoots.put(theBlock.getStartAddress(), theAlreadyVisited);
            }
        }

        // We are done here
        // We have a graph with all explicit and implicit control flow edges here
        return new FlowInformation(theRoots, theKnownBlocks);
    }

    private Set<BytecodeBasicBlock> generateEdges(
            final Set<BytecodeBasicBlock> aVisited, final BytecodeBasicBlock aBlock, final Stack<BytecodeBasicBlock> aNestingStack, final Map<BytecodeOpcodeAddress, BytecodeBasicBlock> aBlocks) {
        aNestingStack.push(aBlock);

        if (aVisited.add(aBlock)) {
            for (final BytecodeInstruction theInstruction : aBlock.getInstructions()) {
                if (theInstruction.isJumpSource()) {
                    for (final BytecodeOpcodeAddress theTarget : theInstruction.getPotentialJumpTargets()) {
                        final BytecodeBasicBlock theTargetBlock = aBlocks.get(theTarget);
                        if (!aNestingStack.contains(theTargetBlock)) {
                            // Normal edge
                            aBlock.addSuccessor(theTargetBlock);
                            generateEdges(aVisited, theTargetBlock, aNestingStack, aBlocks);
                        } else {
                            // Back edge
                            aBlock.addSuccessor(theTargetBlock);
                        }
                    }
                }
            }

            // Properly handle the fall thru case
            if (!aBlock.endsWithReturn() && !aBlock.endsWithThrow() && !aBlock.endsWithGoto()) {
                final BytecodeInstruction theLast = aBlock.lastInstruction();
                final BytecodeInstruction theNext = nextInstructionOf(theLast);
                final BytecodeBasicBlock theNextBlock = aBlocks.get(theNext.getOpcodeAddress());
                aBlock.addSuccessor(theNextBlock);
                generateEdges(aVisited, theNextBlock, aNestingStack, aBlocks);
            }

            for (final BytecodeBasicBlock theBlock : aBlock.getSuccessors()) {
                if (theBlock.getType() != BytecodeBasicBlock.Type.NORMAL) {
                    generateEdges(aVisited, theBlock, aNestingStack, aBlocks);
                }
            }
        }

        aNestingStack.pop();

        return aVisited;
    }
}