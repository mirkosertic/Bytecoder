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
import de.mirkosertic.bytecoder.core.BytecodeControlFlowGraph;

import java.util.HashMap;
import java.util.Map;

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
}
