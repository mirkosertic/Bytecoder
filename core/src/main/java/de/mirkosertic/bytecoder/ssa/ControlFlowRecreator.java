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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import de.mirkosertic.bytecoder.core.BytecodeBasicBlock;
import de.mirkosertic.bytecoder.core.BytecodeControlFlowGraph;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;

public class ControlFlowRecreator {

    public ControlFlowRecreator() {
    }

    public Map<BytecodeBasicBlock, Block> initializeBlocksFor(Program aProgram, BytecodeControlFlowGraph aFlowGraph) {
        // Initialize SSA Block structure
        Map<BytecodeBasicBlock, Block> theCreatedBlocks = new HashMap<>();
        for (BytecodeBasicBlock theBlock : aFlowGraph.getBlocks()) {
            Block theSingleAssignmentBlock;
            switch (theBlock.getType()) {
                case NORMAL:
                    theSingleAssignmentBlock = aProgram.createAt(theBlock.getStartAddress(), Block.BlockType.NORMAL);
                    break;
                case EXCEPTION_HANDLER:
                    theSingleAssignmentBlock = aProgram.createAt(theBlock.getStartAddress(), Block.BlockType.EXCEPTION_HANDLER);
                    break;
                case FINALLY:
                    theSingleAssignmentBlock = aProgram.createAt(theBlock.getStartAddress(), Block.BlockType.FINALLY);
                    break;
                default:
                    throw new IllegalStateException("Unsupported block type : " + theBlock.getType());
            }
            theCreatedBlocks.put(theBlock, theSingleAssignmentBlock);
        }

        // Initialize Block dependency graph
        for (Map.Entry<BytecodeBasicBlock, Block> theEntry : theCreatedBlocks.entrySet()) {
            for (BytecodeBasicBlock theSuccessor : theEntry.getKey().getSuccessors()) {
                Block theSuccessorBlock = theCreatedBlocks.get(theSuccessor);
                if (theSuccessorBlock == null) {
                    throw new IllegalStateException("Cannot find successor block");
                }
                theEntry.getValue().addSuccessor(theSuccessorBlock);
            }
        }

        return theCreatedBlocks;
    }

    public void tryToRecreateControlFlowsIn(Map<BytecodeBasicBlock, Block> aCreatedBlocks) {
        // We try to find blocks what may be an if statement
        for (Map.Entry<BytecodeBasicBlock, Block> theEntry : aCreatedBlocks.entrySet()) {
            Block theBlock = theEntry.getValue();
            if (!theBlock.endWithNeverReturningExpression()) {
                Set<Block> theSuccessors = theBlock.getSuccessors();
                if (theSuccessors.size() == 2) {
                    Iterator<Block> theIt = theSuccessors.iterator();
                    Block theBlock1 = theIt.next();
                    Block theBlock2 = theIt.next();

                    Set<Block> theBlock1Predecessors = theBlock1.getPredecessors();
                    Set<Block> theBlock2Predecessors = theBlock2.getPredecessors();
                    Expression theLastExpression = theBlock.getExpressions().lastExpression();
                    if (theBlock1Predecessors.size() == 1 && theBlock2Predecessors.size() == 1 && theLastExpression instanceof IFExpression) {
                        // This might be one
                        IFExpression theIF = (IFExpression) theLastExpression;
                        BytecodeOpcodeAddress theJumpTarget = theIF.getJumpTarget();
                        Block theThenBlock;
                        Block theElseBlock;
                        if (theBlock1.getStartAddress().equals(theJumpTarget)) {
                            theThenBlock = theBlock1;
                            theElseBlock = theBlock2;
                        } else {
                            theThenBlock = theBlock2;
                            theElseBlock = theBlock1;
                        }

                        HighLevelIFExpression theHLIF = new HighLevelIFExpression(theIF.getBooleanExpression(), theThenBlock, theElseBlock);
                        theBlock.getExpressions().replace(theIF, theHLIF);
                    }
                }
            }
        }
    }
}
