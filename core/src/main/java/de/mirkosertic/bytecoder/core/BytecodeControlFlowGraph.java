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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BytecodeControlFlowGraph {

    private final BytecodeClass owningClass;
    private final List<BytecodeBasicBlock> blocks;

    public BytecodeControlFlowGraph(BytecodeClass aOwningClass, BytecodeProgram aProgramm) {
        owningClass = aOwningClass;
        blocks = new ArrayList<>();
        Set<BytecodeOpcodeAddress> theJumpTarget = aProgramm.getJumpTargets();
        BytecodeBasicBlock currentBlock = null;
        for (BytecodeInstruction theInstruction : aProgramm.getInstructions()) {
            if (theJumpTarget.contains(theInstruction.getOpcodeAddress())) {
                // Jump target, start a new basic block
                currentBlock = null;
            }
            if (aProgramm.isStartOfTryBlock(theInstruction.getOpcodeAddress())) {
                // start of try block, hence new basic block
                currentBlock = null;
            }
            if (currentBlock == null) {
                BytecodeBasicBlock.Type theType = BytecodeBasicBlock.Type.NORMAL;
                for (BytecodeExceptionTableEntry theHandler : aProgramm.getExceptionHandlers()) {
                    if (theHandler.getHandlerPc().equals(theInstruction.getOpcodeAddress())) {
                        if (theHandler.isFinally()) {
                            theType = BytecodeBasicBlock.Type.FINALLY;
                        } else {
                            theType = BytecodeBasicBlock.Type.EXCEPTION_HANDLER;
                        }
                    }
                }
                currentBlock = new BytecodeBasicBlock(theType);
                blocks.add(currentBlock);
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
        for (int i=0;i<blocks.size();i++) {
            BytecodeBasicBlock theBlock = blocks.get(i);
            if (!theBlock.endsWithReturn() && !theBlock.endsWithThrow()) {
                if (theBlock.endsWithJump()) {
                    for (BytecodeInstruction theInstruction : theBlock.getInstructions()) {
                        if (theInstruction.isJumpSource()) {
                            for (BytecodeOpcodeAddress theBlockJumpTarget : theInstruction.getPotentialJumpTargets()) {
                                theBlock.addSuccessor(blockByStartAddress(theBlockJumpTarget));
                            }
                        }
                    }
                    if (theBlock.endsWithConditionalJump()) {
                        if (i<blocks.size()-1) {
                            theBlock.addSuccessor(blocks.get(i + 1));
                        } else {
                            throw new IllegalStateException("Block at end with no jump target!");
                        }
                    }
                } else {
                    if (i<blocks.size()-1) {
                        theBlock.addSuccessor(blocks.get(i + 1));
                    } else {
                        throw new IllegalStateException("Block at end with no jump target!");
                    }
                }
            }
        }
    }

    public BytecodeBasicBlock blockByStartAddress(BytecodeOpcodeAddress aAddress) {
        for (BytecodeBasicBlock theBlock : blocks) {
            if (aAddress.equals(theBlock.getStartAddress())) {
                return theBlock;
            }
        }
        return null;
    }

    public List<BytecodeBasicBlock> getBlocks() {
        return blocks;
    }

    public Set<BytecodeObjectTypeRef> getStaticReferences() {
        Set<BytecodeObjectTypeRef> theResult = new HashSet<>();
        for (BytecodeBasicBlock theBlock : blocks) {

            for (BytecodeInstruction theInstruction : theBlock.getInstructions()) {
                if (theInstruction instanceof BytecodeInstructionNEW) {
                    BytecodeInstructionNEW theNew = (BytecodeInstructionNEW) theInstruction;

                    BytecodeClassinfoConstant theConstant = theNew.getClassInfoForObjectToCreate();
                    theResult.add(BytecodeObjectTypeRef.fromUtf8Constant(theConstant.getConstant()));

                } else if (theInstruction instanceof BytecodeInstructionANEWARRAY) {
                    BytecodeInstructionANEWARRAY theNew = (BytecodeInstructionANEWARRAY) theInstruction;

                    theResult.add(BytecodeObjectTypeRef.fromUtf8Constant(theNew.getTypeConstant().getConstant()));

                }else if (theInstruction instanceof BytecodeInstructionINVOKESTATIC) {
                    BytecodeInstructionINVOKESTATIC theStaticInvoke = (BytecodeInstructionINVOKESTATIC) theInstruction;

                    BytecodeMethodRefConstant theMethodRefConstant = theStaticInvoke.getMethodReference();

                    theResult.add(BytecodeObjectTypeRef.fromUtf8Constant(theMethodRefConstant.getClassIndex().getClassConstant().getConstant()));

                } else if (theInstruction instanceof BytecodeInstructionINSTANCEOF) {
                    BytecodeInstructionINSTANCEOF theInstanceOf = (BytecodeInstructionINSTANCEOF) theInstruction;

                    theResult.add(BytecodeObjectTypeRef.fromUtf8Constant(theInstanceOf.getTypeRef().getConstant()));

                } else if (theInstruction instanceof BytecodeInstructionPUTSTATIC) {
                    BytecodeInstructionPUTSTATIC thePut = (BytecodeInstructionPUTSTATIC) theInstruction;
                    BytecodeFieldRefConstant theConstant = thePut.getConstant();

                    theResult.add(BytecodeObjectTypeRef.fromUtf8Constant(theConstant.getClassIndex().getClassConstant().getConstant()));

                } else if (theInstruction instanceof BytecodeInstructionGETSTATIC) {
                    BytecodeInstructionGETSTATIC theGet = (BytecodeInstructionGETSTATIC) theInstruction;
                    BytecodeFieldRefConstant theConstant = theGet.getConstant();

                    theResult.add(BytecodeObjectTypeRef.fromUtf8Constant(theConstant.getClassIndex().getClassConstant().getConstant()));

                } else if (theInstruction instanceof BytecodeInstructionGenericLDC) {
                    BytecodeInstructionGenericLDC theLoad = (BytecodeInstructionGenericLDC) theInstruction;
                    BytecodeConstant theConstant = theLoad.constant();
                    if (theConstant instanceof BytecodeClassinfoConstant) {
                        BytecodeClassinfoConstant theClassInfo = (BytecodeClassinfoConstant) theConstant;
                        theResult.add(BytecodeObjectTypeRef.fromUtf8Constant(theClassInfo.getConstant()));
                    }
                }
            }
        }

        return theResult;
    }

    public BytecodeClass getOwningClass() {
        return owningClass;
    }
}
