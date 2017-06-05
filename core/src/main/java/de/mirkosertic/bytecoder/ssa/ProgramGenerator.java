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

import de.mirkosertic.bytecoder.core.BytecodeAccessFlags;
import de.mirkosertic.bytecoder.core.BytecodeBasicBlock;
import de.mirkosertic.bytecoder.core.BytecodeClassinfoConstant;
import de.mirkosertic.bytecoder.core.BytecodeConstant;
import de.mirkosertic.bytecoder.core.BytecodeControlFlowGraph;
import de.mirkosertic.bytecoder.core.BytecodeDoubleConstant;
import de.mirkosertic.bytecoder.core.BytecodeFloatConstant;
import de.mirkosertic.bytecoder.core.BytecodeInstruction;
import de.mirkosertic.bytecoder.core.BytecodeInstructionACONSTNULL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionALOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionANEWARRAY;
import de.mirkosertic.bytecoder.core.BytecodeInstructionARRAYLENGTH;
import de.mirkosertic.bytecoder.core.BytecodeInstructionASTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionATHROW;
import de.mirkosertic.bytecoder.core.BytecodeInstructionBIPUSH;
import de.mirkosertic.bytecoder.core.BytecodeInstructionCHECKCAST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionD2Generic;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDCONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDUP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDUPX1;
import de.mirkosertic.bytecoder.core.BytecodeInstructionF2Generic;
import de.mirkosertic.bytecoder.core.BytecodeInstructionFCONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGETFIELD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGETSTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGOTO;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericADD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericAND;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericArrayLOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericArraySTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericCMP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericDIV;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericLDC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericLOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericMUL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericNEG;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericOR;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericREM;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericRETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSHL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSHR;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSUB;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericUSHR;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericXOR;
import de.mirkosertic.bytecoder.core.BytecodeInstructionI2Generic;
import de.mirkosertic.bytecoder.core.BytecodeInstructionICONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFACMP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFCOND;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFICMP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFNONNULL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFNULL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIINC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINSTANCEOF;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKEINTERFACE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESPECIAL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKEVIRTUAL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionL2Generic;
import de.mirkosertic.bytecoder.core.BytecodeInstructionLCONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionLOOKUPSWITCH;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNEW;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNEWARRAY;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNEWMULTIARRAY;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNOP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionObjectArrayLOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionObjectArraySTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionObjectRETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPOP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPUTFIELD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPUTSTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionRETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionSIPUSH;
import de.mirkosertic.bytecoder.core.BytecodeInstructionTABLESWITCH;
import de.mirkosertic.bytecoder.core.BytecodeIntegerConstant;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeLongConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeStringConstant;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeUtf8Constant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class ProgramGenerator {

    private static class ParsingHelper {

        private final Block block;
        private final Stack<Variable> stack;
        private final Map<Integer, Variable> localVariables;
        private int alienStackCounter;

        public ParsingHelper(Block aBlock) {
            stack = new Stack();
            block = aBlock;
            alienStackCounter = 0;
            localVariables = new HashMap<>();
        }

        public Variable pop() {
            if (stack.isEmpty()) {
                Variable theImported = block.newImportedVariable(new UnknownValue(),
                        new StackVariableDescription(alienStackCounter++));
                return theImported;
            }
            return stack.pop();
        }

        public boolean isStackEmpty() {
            return stack.isEmpty();
        }

        public Variable peek() {
            return stack.peek();
        }

        public void push(Variable aValue) {
            if (aValue == null) {
                throw new IllegalStateException("Trying to push null in " + this);
            }
            stack.push(aValue);
        }

        public Variable getLocalVariable(int aIndex) {
            Variable theVariable = localVariables.get(aIndex);
            if (theVariable == null) {
                VariableDescription theDesc = new LocalVariableDescription(aIndex);
                theVariable = block.newImportedVariable(new UnknownValue(),
                        theDesc);
                localVariables.put(aIndex, theVariable);
                block.addToExportedList(theVariable, theDesc);
                return theVariable;
            }
            if (theVariable == null) {
                throw new IllegalStateException("local variable " + aIndex + " is null in " + this);
            }
            return theVariable;
        }

        public void setLocalVariable(int aIndex, Variable aValue) {
            if (aValue == null) {
                throw new IllegalStateException("local variable " + aIndex + " must not be null in " + this);
            }
            Variable theOld = localVariables.get(aIndex);
            if (theOld != null) {
                block.removeFromExportedList(theOld);
            }
            localVariables.put(aIndex, aValue);
            block.addToExportedList(aValue, new LocalVariableDescription(aIndex));
        }

        public Variable popOnly() {
            return stack.pop();
        }
    }

    private final BytecodeLinkerContext linkerContext;

    public ProgramGenerator(BytecodeLinkerContext aLinkerContext) {
        linkerContext = aLinkerContext;
    }

    public Program generateFrom(BytecodeControlFlowGraph aFlowGraph, BytecodeMethodSignature aSignature, BytecodeAccessFlags aAccessFlags) {
        Program theProgram = new Program(linkerContext);

        // Initialize SSA Block structure
        Map<BytecodeBasicBlock, Block> theCreatedBlocks = new HashMap<>();
        for (BytecodeBasicBlock theBlock : aFlowGraph.getBlocks()) {
            Block theSingleAssignmentBlock;
            switch (theBlock.getType()) {
                case NORMAL:
                    theSingleAssignmentBlock = theProgram.createAt(theBlock.getStartAddress(), Block.Type.NORMAL);
                    break;
                case EXCEPTION_HANDLER:
                    theSingleAssignmentBlock = theProgram.createAt(theBlock.getStartAddress(), Block.Type.EXCEPTION_HANDLER);
                    break;
                case FINALLY:
                    theSingleAssignmentBlock = theProgram.createAt(theBlock.getStartAddress(), Block.Type.FINALLY);
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
                    throw new IllegalStateException("Cannot find successor block!");
                }
                theEntry.getValue().addSuccessor(theSuccessorBlock);
            }
        }

        for (Map.Entry<BytecodeBasicBlock, Block> theEntry : theCreatedBlocks.entrySet()) {
            initializeBlockWith(theEntry.getValue(), aFlowGraph, aSignature, aAccessFlags);
        }

        return theProgram;
    }

    private void initializeBlockWith(Block aTargetBlock, BytecodeControlFlowGraph aControlFlowGraph, BytecodeMethodSignature aSignature, BytecodeAccessFlags aAccessFlags) {

        if (aTargetBlock.getExpressions().size() > 0) {
            return;
        }

        ParsingHelper theHelper = new ParsingHelper(aTargetBlock);


        if (aTargetBlock.isStartBlock()) {

            // At this point, local variables are initialized based on the method signature
            // The stack is empty
            int theCurrentIndex = 0;
            if (!aAccessFlags.isStatic()) {
                theHelper.setLocalVariable(theCurrentIndex++, aTargetBlock.newVariable(new SelfReferenceParameterValue()));
            }
            BytecodeTypeRef[] theTypes = aSignature.getArguments();
            for (int i=0;i<theTypes.length;i++) {
                theHelper.setLocalVariable(theCurrentIndex++, aTargetBlock.newVariable(new MethodParameterValue(i, theTypes[i])));
            }
        } else {

            Set<Block> thePredecessors = aTargetBlock.getPredecessors();

            if (thePredecessors.size() == 1) {
                Block theOnlyPredecessor = thePredecessors.iterator().next();
                // There is only one predescessor
            } else if (thePredecessors.size() > 1) {
                // More than one predescessor, we need to generate PHI functions

                //TODO: be implemented

            } else {
                // TODO: Further analyze what this is, maybe exception handler or something else

                if (aTargetBlock.getType() != Block.Type.NORMAL) {
                    theHelper.push(aTargetBlock.newVariable(new CurrentExceptionValue()));
                }
            }
        }

        System.out.println("Parsing block at " + aTargetBlock.getStartAddress().getAddress() + " with pred ");
        for (Block thePred : aTargetBlock.getPredecessors()) {
            System.out.println("   " + thePred.getStartAddress().getAddress());
        }

        // Finally we can start to parse the program
        BytecodeBasicBlock theBytecodeBlock = aControlFlowGraph.blockByStartAddress(aTargetBlock.getStartAddress());

        for (BytecodeInstruction theInstruction : theBytecodeBlock.getInstructions()) {

            System.out.println("Parsing instruction at " + theInstruction.getOpcodeAddress().getAddress());

            if (theInstruction instanceof BytecodeInstructionNOP) {
                BytecodeInstructionNOP theINS = (BytecodeInstructionNOP) theInstruction;
                // Completely ignored
            } else if (theInstruction instanceof BytecodeInstructionCHECKCAST) {
                BytecodeInstructionCHECKCAST theINS = (BytecodeInstructionCHECKCAST) theInstruction;
                Variable theVariable = theHelper.peek();
                aTargetBlock.addExpression(new CheckCastExpression(theVariable, theINS.getTypeCheck()));
            } else if (theInstruction instanceof BytecodeInstructionPOP) {
                BytecodeInstructionPOP theINS = (BytecodeInstructionPOP) theInstruction;
                theHelper.pop();
            } else if (theInstruction instanceof BytecodeInstructionDUP) {
                BytecodeInstructionDUP theINS = (BytecodeInstructionDUP) theInstruction;
                Variable theVariable = theHelper.peek();
                Variable theClone = aTargetBlock.newVariable(new VariableReferenceValue(theVariable));
                theHelper.push(theClone);
            } else if (theInstruction instanceof BytecodeInstructionDUPX1) {
                BytecodeInstructionDUPX1 theINS = (BytecodeInstructionDUPX1) theInstruction;
                Variable theValue1 = theHelper.pop();
                Variable theValue2 = theHelper.pop();

                theHelper.push(theValue1);
                theHelper.push(theValue2);
                Variable theClone = aTargetBlock.newVariable(new VariableReferenceValue(theValue1));
                theHelper.push(theClone);
            } else if (theInstruction instanceof BytecodeInstructionGETSTATIC) {
                BytecodeInstructionGETSTATIC theINS = (BytecodeInstructionGETSTATIC) theInstruction;
                GetStaticValue theValue = new GetStaticValue(theINS.getConstant());
                Variable theVariable = aTargetBlock.newVariable(theValue);
                theHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionASTORE) {
                BytecodeInstructionASTORE theINS = (BytecodeInstructionASTORE) theInstruction;
                Variable theVariable = theHelper.pop();
                theHelper.setLocalVariable(theINS.getVariableIndex(), theVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericSTORE) {
                BytecodeInstructionGenericSTORE theINS = (BytecodeInstructionGenericSTORE) theInstruction;
                Variable theVariable = theHelper.pop();
                theHelper.setLocalVariable(theINS.getVariableIndex(), theVariable);
            } else if (theInstruction instanceof BytecodeInstructionObjectArrayLOAD) {
                BytecodeInstructionObjectArrayLOAD theINS = (BytecodeInstructionObjectArrayLOAD) theInstruction;
                Variable theIndex = theHelper.pop();
                Variable theTarget = theHelper.pop();
                Variable theVariable = aTargetBlock.newVariable(new ArrayEntryValue(theTarget, theIndex));
                theHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericArrayLOAD) {
                BytecodeInstructionGenericArrayLOAD theINS = (BytecodeInstructionGenericArrayLOAD) theInstruction;
                Variable theIndex = theHelper.pop();
                Variable theTarget = theHelper.pop();
                Variable theVariable = aTargetBlock.newVariable(new ArrayEntryValue(theTarget, theIndex));
                theHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericArraySTORE) {
                BytecodeInstructionGenericArraySTORE theINS = (BytecodeInstructionGenericArraySTORE) theInstruction;
                Variable theValue = theHelper.pop();
                Variable theIndex = theHelper.pop();
                Variable theTarget = theHelper.pop();
                aTargetBlock.addExpression(new ArrayStoreExpression(theTarget, theIndex, theValue));
            } else if (theInstruction instanceof BytecodeInstructionObjectArraySTORE) {
                BytecodeInstructionObjectArraySTORE theINS = (BytecodeInstructionObjectArraySTORE) theInstruction;
                Variable theValue = theHelper.pop();
                Variable theIndex = theHelper.pop();
                Variable theTarget = theHelper.pop();
                aTargetBlock.addExpression(new ArrayStoreExpression(theTarget, theIndex, theValue));
            } else if (theInstruction instanceof BytecodeInstructionACONSTNULL) {
                BytecodeInstructionACONSTNULL theINS = (BytecodeInstructionACONSTNULL) theInstruction;
                Variable theVariable = aTargetBlock.newVariable(new NullValue());
                theHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionPUTFIELD) {
                BytecodeInstructionPUTFIELD theINS = (BytecodeInstructionPUTFIELD) theInstruction;
                Variable theValue = theHelper.pop();
                Variable theTarget = theHelper.pop();
                aTargetBlock.addExpression(new PutFieldExpression(theINS.getFieldRefConstant(), theTarget, theValue));
            } else if (theInstruction instanceof BytecodeInstructionGETFIELD) {
                BytecodeInstructionGETFIELD theINS = (BytecodeInstructionGETFIELD) theInstruction;
                Variable theTarget = theHelper.pop();
                Variable theVariable = aTargetBlock.newVariable(new GetFieldValue(theINS.getFieldRefConstant(), theTarget));
                theHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionPUTSTATIC) {
                BytecodeInstructionPUTSTATIC theINS = (BytecodeInstructionPUTSTATIC) theInstruction;
                Variable theValue = theHelper.pop();
                aTargetBlock.addExpression(new PutStaticExpression(theINS.getConstant(), theValue));
            } else if (theInstruction instanceof BytecodeInstructionGenericLDC) {
                BytecodeInstructionGenericLDC theINS = (BytecodeInstructionGenericLDC) theInstruction;
                BytecodeConstant theConstant = theINS.constant();
                if (theConstant instanceof BytecodeDoubleConstant) {
                    BytecodeDoubleConstant theC = (BytecodeDoubleConstant) theConstant;
                    Variable theVariable = aTargetBlock.newVariable(new DoubleValue(theC.getDoubleValue()));
                    theHelper.push(theVariable);
                } else if (theConstant instanceof BytecodeLongConstant) {
                    BytecodeLongConstant theC = (BytecodeLongConstant) theConstant;
                    Variable theVariable = aTargetBlock.newVariable(new LongValue(theC.getLongValue()));
                    theHelper.push(theVariable);
                } else if (theConstant instanceof BytecodeFloatConstant) {
                    BytecodeFloatConstant theC = (BytecodeFloatConstant) theConstant;
                    Variable theVariable = aTargetBlock.newVariable(new FloatValue(theC.getFloatValue()));
                    theHelper.push(theVariable);
                } else if (theConstant instanceof BytecodeIntegerConstant) {
                    BytecodeIntegerConstant theC = (BytecodeIntegerConstant) theConstant;
                    Variable theVariable = aTargetBlock.newVariable(new IntegerValue(theC.getIntegerValue()));
                    theHelper.push(theVariable);
                } else if (theConstant instanceof BytecodeStringConstant) {
                    BytecodeStringConstant theC = (BytecodeStringConstant) theConstant;
                    Variable theVariable = aTargetBlock.newVariable(new StringValue(theC.getValue().stringValue()));
                    theHelper.push(theVariable);
                } else if (theConstant instanceof BytecodeClassinfoConstant) {
                    BytecodeClassinfoConstant theC = (BytecodeClassinfoConstant) theConstant;
                    Variable theVariable = aTargetBlock.newVariable(new ClassReferenceValue(BytecodeObjectTypeRef.fromUtf8Constant(theC.getConstant())));
                    theHelper.push(theVariable);
                } else {
                    throw new IllegalArgumentException("Unsupported constant type : " + theConstant);
                }
            } else if (theInstruction instanceof BytecodeInstructionBIPUSH) {
                BytecodeInstructionBIPUSH theINS = (BytecodeInstructionBIPUSH) theInstruction;
                Variable theVariable = aTargetBlock.newVariable(new ByteValue(theINS.getByteValue()));
                theHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionSIPUSH) {
                BytecodeInstructionSIPUSH theINS = (BytecodeInstructionSIPUSH) theInstruction;
                Variable theVariable = aTargetBlock.newVariable(new ShortValue(theINS.getShortValue()));
                theHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionICONST) {
                BytecodeInstructionICONST theINS = (BytecodeInstructionICONST) theInstruction;
                Variable theVariable = aTargetBlock.newVariable(new IntegerValue(theINS.getIntConst()));
                theHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionFCONST) {
                BytecodeInstructionFCONST theINS = (BytecodeInstructionFCONST) theInstruction;
                Variable theVariable = aTargetBlock.newVariable(new FloatValue(theINS.getFloatValue()));
                theHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionDCONST) {
                BytecodeInstructionDCONST theINS = (BytecodeInstructionDCONST) theInstruction;
                Variable theVariable = aTargetBlock.newVariable(new DoubleValue(theINS.getDoubleConst()));
                theHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionLCONST) {
                BytecodeInstructionLCONST theINS = (BytecodeInstructionLCONST) theInstruction;
                Variable theVariable = aTargetBlock.newVariable(new LongValue(theINS.getLongConst()));
                theHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericNEG) {
                BytecodeInstructionGenericNEG theINS = (BytecodeInstructionGenericNEG) theInstruction;
                Variable theVariable = theHelper.pop();
                Variable theNegatedValue = aTargetBlock.newVariable(new NegatedValue(theVariable));
                theHelper.push(theNegatedValue);
            } else if (theInstruction instanceof BytecodeInstructionARRAYLENGTH) {
                BytecodeInstructionARRAYLENGTH theINS = (BytecodeInstructionARRAYLENGTH) theInstruction;
                Variable theVariable = theHelper.pop();
                Variable theNegatedValue = aTargetBlock.newVariable(new ArrayLengthValue(theVariable));
                theHelper.push(theNegatedValue);
            } else if (theInstruction instanceof BytecodeInstructionGenericLOAD) {
                BytecodeInstructionGenericLOAD theINS = (BytecodeInstructionGenericLOAD) theInstruction;
                Variable theVariable = theHelper.getLocalVariable(theINS.getVariableIndex());
                theHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionALOAD) {
                BytecodeInstructionALOAD theINS = (BytecodeInstructionALOAD) theInstruction;
                Variable theVariable = theHelper.getLocalVariable(theINS.getVariableIndex());
                theHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericCMP) {
                BytecodeInstructionGenericCMP theINS = (BytecodeInstructionGenericCMP) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(new CompareValue(theValue1, theValue2));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionIINC) {
                BytecodeInstructionIINC theINS = (BytecodeInstructionIINC) theInstruction;
                Variable theVariable = theHelper.getLocalVariable(theINS.getIndex());
                Variable theAmount = aTargetBlock.newVariable(new IntegerValue(theINS.getConstant()));
                Variable theNewVariable = aTargetBlock.newVariable(new BinaryValue(theVariable, BinaryValue.Operator.ADD, theAmount));
                theHelper.setLocalVariable(theINS.getIndex(), theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericREM) {
                BytecodeInstructionGenericREM theINS = (BytecodeInstructionGenericREM) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(new BinaryValue(theValue1, BinaryValue.Operator.REMAINDER, theValue2));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericADD) {
                BytecodeInstructionGenericADD theINS = (BytecodeInstructionGenericADD) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(new BinaryValue(theValue1, BinaryValue.Operator.ADD, theValue2));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericDIV) {
                BytecodeInstructionGenericDIV theINS = (BytecodeInstructionGenericDIV) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(new BinaryValue(theValue1, BinaryValue.Operator.DIV, theValue2));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericMUL) {
                BytecodeInstructionGenericMUL theINS = (BytecodeInstructionGenericMUL) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(new BinaryValue(theValue1, BinaryValue.Operator.MUL, theValue2));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericSUB) {
                BytecodeInstructionGenericSUB theINS = (BytecodeInstructionGenericSUB) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(new BinaryValue(theValue1, BinaryValue.Operator.SUB, theValue2));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericXOR) {
                BytecodeInstructionGenericXOR theINS = (BytecodeInstructionGenericXOR) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(new BinaryValue(theValue1, BinaryValue.Operator.BINARYXOR, theValue2));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericOR) {
                BytecodeInstructionGenericOR theINS = (BytecodeInstructionGenericOR) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(new BinaryValue(theValue1, BinaryValue.Operator.BINARYOR, theValue2));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericAND) {
                BytecodeInstructionGenericAND theINS = (BytecodeInstructionGenericAND) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(new BinaryValue(theValue1, BinaryValue.Operator.BINARYAND, theValue2));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericSHL) {
                BytecodeInstructionGenericSHL theINS = (BytecodeInstructionGenericSHL) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(new BinaryValue(theValue1, BinaryValue.Operator.BINARYSHIFTLEFT, theValue2));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericSHR) {
                BytecodeInstructionGenericSHR theINS = (BytecodeInstructionGenericSHR) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(new BinaryValue(theValue1, BinaryValue.Operator.BINARYSHIFTRIGHT, theValue2));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericUSHR) {
                BytecodeInstructionGenericUSHR theINS = (BytecodeInstructionGenericUSHR) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(new BinaryValue(theValue1, BinaryValue.Operator.BINARYUNSIGNEDSHIFTRIGHT, theValue2));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionIFNULL) {
                BytecodeInstructionIFNULL theINS = (BytecodeInstructionIFNULL) theInstruction;
                Variable theValue = theHelper.pop();
                FixedBinaryValue theBinaryValue = new FixedBinaryValue(theValue, FixedBinaryValue.Operator.ISNULL);
                Variable theResult = aTargetBlock.newVariable(theBinaryValue);

                ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(theINS.getJumpTarget(), aTargetBlock.toFinalState()));

                aTargetBlock.addExpression(new IFExpression(theResult, theExpressions));
            } else if (theInstruction instanceof BytecodeInstructionIFNONNULL) {
                BytecodeInstructionIFNONNULL theINS = (BytecodeInstructionIFNONNULL) theInstruction;
                Variable theValue = theHelper.pop();
                FixedBinaryValue theBinaryValue = new FixedBinaryValue(theValue, FixedBinaryValue.Operator.ISNONNULL);
                Variable theResult = aTargetBlock.newVariable(theBinaryValue);

                ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(theINS.getJumpTarget(), aTargetBlock.toFinalState()));

                aTargetBlock.addExpression(new IFExpression(theResult, theExpressions));
            } else if (theInstruction instanceof BytecodeInstructionIFICMP) {
                BytecodeInstructionIFICMP theINS = (BytecodeInstructionIFICMP) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                BinaryValue theBinaryValue;
                switch (theINS.getType()) {
                    case lt:
                        theBinaryValue = new BinaryValue(theValue1, BinaryValue.Operator.LESSTHAN, theValue2);
                        break;
                    case eq:
                        theBinaryValue = new BinaryValue(theValue1, BinaryValue.Operator.EQUALS, theValue2);
                        break;
                    case gt:
                        theBinaryValue = new BinaryValue(theValue1, BinaryValue.Operator.GREATERTHAN, theValue2);
                        break;
                    case ge:
                        theBinaryValue = new BinaryValue(theValue1, BinaryValue.Operator.GREATEROREQUALS, theValue2);
                        break;
                    case le:
                        theBinaryValue = new BinaryValue(theValue1, BinaryValue.Operator.LESSTHANOREQUALS, theValue2);
                        break;
                    case ne:
                        theBinaryValue = new BinaryValue(theValue1, BinaryValue.Operator.NOTEQUALS, theValue2);
                        break;
                    default:
                        throw new IllegalStateException("Not supported operation : " + theINS.getType());
                }
                Variable theNewVariable = aTargetBlock.newVariable(theBinaryValue);

                ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(theINS.getJumpTarget(), aTargetBlock.toFinalState()));

                aTargetBlock.addExpression(new IFExpression(theNewVariable, theExpressions));

            } else if (theInstruction instanceof BytecodeInstructionIFACMP) {
                BytecodeInstructionIFACMP theINS = (BytecodeInstructionIFACMP) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                BinaryValue theBinaryValue;
                switch (theINS.getType()) {
                    case eq:
                        theBinaryValue = new BinaryValue(theValue1, BinaryValue.Operator.EQUALS, theValue2);
                        break;
                    case ne:
                        theBinaryValue = new BinaryValue(theValue1, BinaryValue.Operator.NOTEQUALS, theValue2);
                        break;
                    default:
                        throw new IllegalStateException("Not supported operation : " + theINS.getType());
                }
                Variable theNewVariable = aTargetBlock.newVariable(theBinaryValue);

                ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(theINS.getJumpTarget(), aTargetBlock.toFinalState()));

                aTargetBlock.addExpression(new IFExpression(theNewVariable, theExpressions));

            } else if (theInstruction instanceof BytecodeInstructionIFCOND) {
                BytecodeInstructionIFCOND theINS = (BytecodeInstructionIFCOND) theInstruction;
                Variable theValue = theHelper.pop();
                Variable theZero = aTargetBlock.newVariable(new IntegerValue(0));
                BinaryValue theBinaryValue;
                switch (theINS.getType()) {
                    case lt:
                        theBinaryValue = new BinaryValue(theValue, BinaryValue.Operator.LESSTHAN, theZero);
                        break;
                    case eq:
                        theBinaryValue = new BinaryValue(theValue, BinaryValue.Operator.EQUALS, theZero);
                        break;
                    case gt:
                        theBinaryValue = new BinaryValue(theValue, BinaryValue.Operator.GREATERTHAN, theZero);
                        break;
                    case ge:
                        theBinaryValue = new BinaryValue(theValue, BinaryValue.Operator.GREATEROREQUALS, theZero);
                        break;
                    case le:
                        theBinaryValue = new BinaryValue(theValue, BinaryValue.Operator.LESSTHANOREQUALS, theZero);
                        break;
                    case ne:
                        theBinaryValue = new BinaryValue(theValue, BinaryValue.Operator.NOTEQUALS, theZero);
                        break;
                    default:
                        throw new IllegalStateException("Not supported operation : " + theINS.getType());
                }
                Variable theNewVariable = aTargetBlock.newVariable(theBinaryValue);

                ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(theINS.getJumpTarget(), aTargetBlock.toFinalState()));

                aTargetBlock.addExpression(new IFExpression(theNewVariable, theExpressions));
            } else if (theInstruction instanceof BytecodeInstructionObjectRETURN) {
                BytecodeInstructionObjectRETURN theINS = (BytecodeInstructionObjectRETURN) theInstruction;
                Variable theVariable = theHelper.pop();
                aTargetBlock.addExpression(new ReturnVariableExpression(theVariable));
            } else if (theInstruction instanceof BytecodeInstructionGenericRETURN) {
                BytecodeInstructionGenericRETURN theINS = (BytecodeInstructionGenericRETURN) theInstruction;
                Variable theVariable = theHelper.pop();
                aTargetBlock.addExpression(new ReturnVariableExpression(theVariable));
            } else if (theInstruction instanceof BytecodeInstructionATHROW) {
                BytecodeInstructionATHROW theINS = (BytecodeInstructionATHROW) theInstruction;
                Variable theVariable = theHelper.pop();
                aTargetBlock.addExpression(new ThrowExpression(theVariable));
            } else if (theInstruction instanceof BytecodeInstructionRETURN) {
                BytecodeInstructionRETURN theINS = (BytecodeInstructionRETURN) theInstruction;
                aTargetBlock.addExpression(new ReturnExpression());
            } else if (theInstruction instanceof BytecodeInstructionNEW) {
                BytecodeInstructionNEW theINS = (BytecodeInstructionNEW) theInstruction;
                Variable theNewVariable = aTargetBlock.newVariable(new NewObjectValue(theINS.getClassInfoForObjectToCreate()));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionNEWARRAY) {
                BytecodeInstructionNEWARRAY theINS = (BytecodeInstructionNEWARRAY) theInstruction;
                Variable theLength = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(new NewArrayValue(theINS.getPrimitiveType(), theLength));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionNEWMULTIARRAY) {
                BytecodeInstructionNEWMULTIARRAY theINS = (BytecodeInstructionNEWMULTIARRAY) theInstruction;
                List<Variable> theDimensions = new ArrayList<>();
                for (int i=0;i<theINS.getDimensions();i++) {
                    theDimensions.add(theHelper.pop());
                }
                Collections.reverse(theDimensions);
                BytecodeTypeRef theTypeRef = linkerContext.getSignatureParser().toFieldType(new BytecodeUtf8Constant(theINS.getTypeConstant().getConstant().stringValue()));
                Variable theNewVariable = aTargetBlock.newVariable(new NewMultiArrayValue(theTypeRef, theDimensions));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionANEWARRAY) {
                BytecodeInstructionANEWARRAY theINS = (BytecodeInstructionANEWARRAY) theInstruction;
                Variable theLength = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(new NewArrayValue(BytecodeObjectTypeRef.fromUtf8Constant(theINS.getTypeConstant().getConstant()), theLength));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGOTO) {
                BytecodeInstructionGOTO theINS = (BytecodeInstructionGOTO) theInstruction;
                aTargetBlock.addExpression(new GotoExpression(theINS.getJumpAddress(), aTargetBlock.toFinalState()));
            } else if (theInstruction instanceof BytecodeInstructionL2Generic) {
                BytecodeInstructionL2Generic theINS = (BytecodeInstructionL2Generic) theInstruction;
                Variable theValue = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(new TypeConversionValue(theValue, theINS.getTargetType()));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionI2Generic) {
                BytecodeInstructionI2Generic theINS = (BytecodeInstructionI2Generic) theInstruction;
                Variable theValue = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(new TypeConversionValue(theValue, theINS.getTargetType()));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionF2Generic) {
                BytecodeInstructionF2Generic theINS = (BytecodeInstructionF2Generic) theInstruction;
                Variable theValue = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(new TypeConversionValue(theValue, theINS.getTargetType()));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionD2Generic) {
                BytecodeInstructionD2Generic theINS = (BytecodeInstructionD2Generic) theInstruction;
                Variable theValue = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(new TypeConversionValue(theValue, theINS.getTargetType()));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionINVOKESPECIAL) {
                BytecodeInstructionINVOKESPECIAL theINS = (BytecodeInstructionINVOKESPECIAL) theInstruction;
                BytecodeMethodSignature theSignature = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                List<Variable> theArguments = new ArrayList<>();
                BytecodeTypeRef[] theArgumentTypes = theSignature.getArguments();
                for (int i=0;i<theArgumentTypes.length;i++) {
                    theArguments.add(theHelper.pop());
                }
                Collections.reverse(theArguments);

                Variable theTarget = theHelper.pop();

                DirectInvokeMethodValue theValue = new DirectInvokeMethodValue(theINS.getMethodReference().getClassIndex().getClassConstant(), theINS.getMethodReference().getNameAndTypeIndex().getNameAndType(), theTarget, theArguments);
                if (theSignature.getReturnType().isVoid()) {
                    aTargetBlock.addExpression(new DirectInvokeMethodExpression(theValue));
                } else {
                    Variable theNewVariable = aTargetBlock.newVariable(theValue);
                    theHelper.push(theNewVariable);
                }
            } else if (theInstruction instanceof BytecodeInstructionINVOKEVIRTUAL) {
                BytecodeInstructionINVOKEVIRTUAL theINS = (BytecodeInstructionINVOKEVIRTUAL) theInstruction;
                BytecodeMethodSignature theSignature = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                List<Variable> theArguments = new ArrayList<>();
                BytecodeTypeRef[] theArgumentTypes = theSignature.getArguments();
                for (int i=0;i<theArgumentTypes.length;i++) {
                    theArguments.add(theHelper.pop());
                }
                Collections.reverse(theArguments);

                Variable theTarget = theHelper.pop();

                InvokeVirtualMethodValue theValue = new InvokeVirtualMethodValue(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType(), theTarget, theArguments);
                if (theSignature.getReturnType().isVoid()) {
                    aTargetBlock.addExpression(new InvokeVirtualMethodExpression(theValue));
                } else {
                    Variable theNewVariable = aTargetBlock.newVariable(theValue);
                    theHelper.push(theNewVariable);
                }

            } else if (theInstruction instanceof BytecodeInstructionINVOKEINTERFACE) {
                BytecodeInstructionINVOKEINTERFACE theINS = (BytecodeInstructionINVOKEINTERFACE) theInstruction;
                BytecodeMethodSignature theSignature = theINS.getMethodDescriptor().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                List<Variable> theArguments = new ArrayList<>();
                BytecodeTypeRef[] theArgumentTypes = theSignature.getArguments();
                for (int i=0;i<theArgumentTypes.length;i++) {
                    theArguments.add(theHelper.pop());
                }
                Collections.reverse(theArguments);

                Variable theTarget = theHelper.pop();

                InvokeVirtualMethodValue theValue = new InvokeVirtualMethodValue(theINS.getMethodDescriptor().getNameAndTypeIndex().getNameAndType(), theTarget, theArguments);
                if (theSignature.getReturnType().isVoid()) {
                    aTargetBlock.addExpression(new InvokeVirtualMethodExpression(theValue));
                } else {
                    Variable theNewVariable = aTargetBlock.newVariable(theValue);
                    theHelper.push(theNewVariable);
                }

            } else if (theInstruction instanceof BytecodeInstructionINVOKESTATIC) {
                BytecodeInstructionINVOKESTATIC theINS = (BytecodeInstructionINVOKESTATIC) theInstruction;
                BytecodeMethodSignature theSignature = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                List<Variable> theArguments = new ArrayList<>();
                BytecodeTypeRef[] theArgumentTypes = theSignature.getArguments();
                for (int i=0;i<theArgumentTypes.length;i++) {
                    theArguments.add(theHelper.pop());
                }
                Collections.reverse(theArguments);

                InvokeStaticMethodValue theValue = new InvokeStaticMethodValue(theINS.getMethodReference(), theArguments);
                if (theSignature.getReturnType().isVoid()) {
                    aTargetBlock.addExpression(new InvokeStaticMethodExpression(theValue));
                } else {
                    Variable theNewVariable = aTargetBlock.newVariable(theValue);
                    theHelper.push(theNewVariable);
                }
            } else if (theInstruction instanceof BytecodeInstructionINSTANCEOF) {
                BytecodeInstructionINSTANCEOF theINS = (BytecodeInstructionINSTANCEOF) theInstruction;

                Variable theVariable = theHelper.pop();
                InstanceOfValue theValue = new InstanceOfValue(theVariable, theINS.getTypeRef());

                Variable theCheckResult = aTargetBlock.newVariable(theValue);
                theHelper.push(theCheckResult);
            } else if (theInstruction instanceof BytecodeInstructionTABLESWITCH) {
                BytecodeInstructionTABLESWITCH theINS = (BytecodeInstructionTABLESWITCH) theInstruction;
                Variable theVariable = theHelper.pop();
                aTargetBlock.addExpression(new TableSwitchExpression(theVariable, theINS));
            } else if (theInstruction instanceof BytecodeInstructionLOOKUPSWITCH) {
                BytecodeInstructionLOOKUPSWITCH theINS = (BytecodeInstructionLOOKUPSWITCH) theInstruction;
                Variable theVariable = theHelper.pop();
                aTargetBlock.addExpression(new LookupSwitchExpression(theVariable, theINS));
            } else {
                throw new IllegalArgumentException("Not implemented : " + theInstruction);
            }
        }

        int theExportedStackPos = 0;
        while(!theHelper.isStackEmpty()) {
            Variable theVar = theHelper.popOnly();
            aTargetBlock.addToExportedList(theVar, new StackVariableDescription(theExportedStackPos++));
        }
    }
}