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
package de.mirkosertic.bytecoder.ssa;

import de.mirkosertic.bytecoder.core.BytecodeBasicBlock;
import de.mirkosertic.bytecoder.core.BytecodeClass;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public interface ProgramGenerator {

    Program generateFrom(BytecodeClass aBytecodeClass, BytecodeMethod aMethod);

    default Map<BytecodeBasicBlock, GraphNode> toControlFlowGraph(Program aSSAProgram, BytecodeProgram aProgram) {
        List<BytecodeBasicBlock> theBlocks = new ArrayList<>();

        Function<BytecodeOpcodeAddress, BytecodeBasicBlock> theBasicBlockByAddress = aValue -> {
            for (BytecodeBasicBlock theBlock : theBlocks) {
                if (aValue.equals(theBlock.getStartAddress())) {
                    return theBlock;
                }
            }
            throw new IllegalStateException("No Block for " + aValue);
        };

        Set<BytecodeOpcodeAddress> theJumpTarget = aProgram.getJumpTargets();
        BytecodeBasicBlock currentBlock = null;
        for (BytecodeInstruction theInstruction : aProgram.getInstructions()) {
            if (theJumpTarget.contains(theInstruction.getOpcodeAddress())) {
                // Jump target, start a new basic block
                currentBlock = null;
            }
            if (aProgram.isStartOfTryBlock(theInstruction.getOpcodeAddress())) {
                // start of try block, hence new basic block
                currentBlock = null;
            }
            if (currentBlock == null) {
                BytecodeBasicBlock.Type theType = BytecodeBasicBlock.Type.NORMAL;
                for (BytecodeExceptionTableEntry theHandler : aProgram.getExceptionHandlers()) {
                    if (theHandler.getHandlerPc().equals(theInstruction.getOpcodeAddress())) {
                        if (theHandler.isFinally()) {
                            theType = BytecodeBasicBlock.Type.FINALLY;
                        } else {
                            theType = BytecodeBasicBlock.Type.EXCEPTION_HANDLER;
                        }
                    }
                }
                BytecodeBasicBlock theCurrentTemp = currentBlock;
                currentBlock = new BytecodeBasicBlock(theType);
                if (theCurrentTemp != null && !theCurrentTemp.endsWithReturn() && !theCurrentTemp.endsWithThrow() && theCurrentTemp.endsWithGoto() && !theCurrentTemp.endsWithConditionalJump()) {
                    theCurrentTemp.addSuccessor(currentBlock);
                }
                theBlocks.add(currentBlock);
            }
            currentBlock.addInstruction(theInstruction);
            if (theInstruction.isJumpSource()) {
                // conditional or unconditional jump, start new basic block
                currentBlock = null;
            } else if (theInstruction instanceof BytecodeInstructionRET) {
                // returning, start new basic block
                currentBlock = null;
            } else if (theInstruction instanceof BytecodeInstructionRETURN) {
                // returning, start new basic block
                currentBlock = null;
            } else if (theInstruction instanceof BytecodeInstructionObjectRETURN) {
                // returning, start new basic block
                currentBlock = null;
            } else if (theInstruction instanceof BytecodeInstructionGenericRETURN) {
                // returning, start new basic block
                currentBlock = null;
            } else if (theInstruction instanceof BytecodeInstructionATHROW) {
                // thowing an exception, start new basic block
                currentBlock = null;
            } else if (theInstruction instanceof BytecodeInstructionInvoke) {
                // invocation, start new basic block
                // currentBlock = null;
            }
        }

        // Now, we have to build the successors of each block
        for (int i=0;i<theBlocks.size();i++) {
            BytecodeBasicBlock theBlock = theBlocks.get(i);
            if (!theBlock.endsWithReturn() && !theBlock.endsWithThrow()) {
                if (theBlock.endsWithJump()) {
                    for (BytecodeInstruction theInstruction : theBlock.getInstructions()) {
                        if (theInstruction.isJumpSource()) {
                            for (BytecodeOpcodeAddress theBlockJumpTarget : theInstruction.getPotentialJumpTargets()) {
                                theBlock.addSuccessor(theBasicBlockByAddress.apply(theBlockJumpTarget));
                            }
                        }
                    }
                    if (theBlock.endsWithConditionalJump()) {
                        if (i<theBlocks.size()-1) {
                            theBlock.addSuccessor(theBlocks.get(i + 1));
                        } else {
                            throw new IllegalStateException("Block at end with no jump target!");
                        }
                    }
                } else {
                    if (i<theBlocks.size()-1) {
                        theBlock.addSuccessor(theBlocks.get(i + 1));
                    } else {
                        throw new IllegalStateException("Block at end with no jump target!");
                    }
                }
            }
        }

        // Ok, now we transform it to GraphNodes with yet empty content
        Map<BytecodeBasicBlock, GraphNode> theCreatedBlocks = new HashMap<>();

        ControlFlowGraph theGraph = aSSAProgram.getControlFlowGraph();
        for (BytecodeBasicBlock theBlock : theBlocks) {
            GraphNode theSingleAssignmentBlock;
            switch (theBlock.getType()) {
            case NORMAL:
                theSingleAssignmentBlock = theGraph.createAt(theBlock.getStartAddress(), GraphNode.BlockType.NORMAL);
                break;
            case EXCEPTION_HANDLER:
                theSingleAssignmentBlock = theGraph.createAt(theBlock.getStartAddress(), GraphNode.BlockType.EXCEPTION_HANDLER);
                break;
            case FINALLY:
                theSingleAssignmentBlock = theGraph.createAt(theBlock.getStartAddress(), GraphNode.BlockType.FINALLY);
                break;
            default:
                throw new IllegalStateException("Unsupported block type : " + theBlock.getType());
            }
            theCreatedBlocks.put(theBlock, theSingleAssignmentBlock);
        }

        // Initialize Block dependency graph
        for (Map.Entry<BytecodeBasicBlock, GraphNode> theEntry : theCreatedBlocks.entrySet()) {
            for (BytecodeBasicBlock theSuccessor : theEntry.getKey().getSuccessors()) {
                GraphNode theSuccessorBlock = theCreatedBlocks.get(theSuccessor);
                if (theSuccessorBlock == null) {
                    throw new IllegalStateException("Cannot find successor block");
                }
                theEntry.getValue().addSuccessor(GraphNode.EdgeType.NORMAL, theSuccessorBlock);
            }
        }
        return theCreatedBlocks;
    }
}
