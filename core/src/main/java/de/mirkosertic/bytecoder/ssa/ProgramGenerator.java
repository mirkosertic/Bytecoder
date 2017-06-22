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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import de.mirkosertic.bytecoder.classlib.Address;
import de.mirkosertic.bytecoder.classlib.GC;
import de.mirkosertic.bytecoder.core.BytecodeAccessFlags;
import de.mirkosertic.bytecoder.core.BytecodeBasicBlock;
import de.mirkosertic.bytecoder.core.BytecodeBootstrapMethod;
import de.mirkosertic.bytecoder.core.BytecodeBootstrapMethodsAttributeInfo;
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
import de.mirkosertic.bytecoder.core.BytecodeInstructionDUP2X1;
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
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKEDYNAMIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKEINTERFACE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESPECIAL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKEVIRTUAL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionL2Generic;
import de.mirkosertic.bytecoder.core.BytecodeInstructionLCMP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionLCONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionLOOKUPSWITCH;
import de.mirkosertic.bytecoder.core.BytecodeInstructionMONITORENTER;
import de.mirkosertic.bytecoder.core.BytecodeInstructionMONITOREXIT;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNEW;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNEWARRAY;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNEWMULTIARRAY;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNOP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionObjectArrayLOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionObjectArraySTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionObjectRETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPOP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPOP2;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPUTFIELD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPUTSTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionRETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionSIPUSH;
import de.mirkosertic.bytecoder.core.BytecodeInstructionTABLESWITCH;
import de.mirkosertic.bytecoder.core.BytecodeIntegerConstant;
import de.mirkosertic.bytecoder.core.BytecodeInvokeDynamicConstant;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeLongConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethodHandleConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethodRefConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeStringConstant;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeUtf8Constant;
import de.mirkosertic.bytecoder.ssa.optimizer.AllOptimizer;

public class ProgramGenerator {

    private static class ParsingHelper {

        private final Block block;
        private final Stack<Variable> stack;
        private final Map<Integer, Variable> localVariables;

        public ParsingHelper(Block aBlock) {
            stack = new Stack();
            block = aBlock;
            localVariables = new HashMap<>();
        }

        public ParsingHelper cloneToNewFor(Block aBlock) {
            ParsingHelper theNew = new ParsingHelper(aBlock);
            for (Map.Entry<Integer, Variable> theEntry : localVariables.entrySet()) {
                theNew.localVariables.put(theEntry.getKey(), theEntry.getValue());
                aBlock.addToImportedList(theEntry.getValue(), new LocalVariableDescription(theEntry.getKey()));
                aBlock.addToExportedList(theEntry.getValue(), new LocalVariableDescription(theEntry.getKey()));
            }
            for (int i=0;i<stack.size();i++) {
                theNew.stack.push(stack.get(i));
            }
            return theNew;
        }

        public ParsingHelper cloneToNewWithPHIFunctions(Block aBlock, Program aProgram) {
            ParsingHelper theNew = new ParsingHelper(aBlock);
            for (Map.Entry<Integer, Variable> theEntry : localVariables.entrySet()) {
                Variable theVar = aProgram.createVariable(theEntry.getValue().getType(), new PHIFunction());
                aBlock.addToImportedList(theVar, new LocalVariableDescription(theEntry.getKey()));
                aBlock.addToExportedList(theVar, new LocalVariableDescription(theEntry.getKey()));
                theNew.localVariables.put(theEntry.getKey(), theVar);
            }
            for (int i=stack.size() - 1 ; i>= 0; i--) {
                Variable theVar = aProgram.createVariable(stack.get(stack.size() - 1 - i).getType(), new PHIFunction());
                aBlock.addToImportedList(theVar, new StackVariableDescription(i));
                theNew.stack.push(theVar);
            }
            return theNew;
        }

        public Variable pop() {
            return stack.pop();
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
                theVariable = block.newImportedVariable(Type.UNKNOWN, new UnknownValue(),
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
            localVariables.put(aIndex, aValue);
            block.addToExportedList(aValue, new LocalVariableDescription(aIndex));
        }

        public void finalizeExportState() {
            for (Map.Entry<Integer, Variable> theEntry : localVariables.entrySet()) {
                block.addToExportedList(theEntry.getValue(), new LocalVariableDescription(theEntry.getKey()));
            }
            for (int i=stack.size() - 1 ; i>= 0; i--) {
                // Numbering must be consistent here!!
                block.addToExportedList(stack.get(i), new StackVariableDescription(stack.size() - 1 - i));
            }
        }

        public void removeVariable(Variable aVariable) {
            block.removeVariable(aVariable);
        }
    }

    private final BytecodeLinkerContext linkerContext;

    public ProgramGenerator(BytecodeLinkerContext aLinkerContext) {
        linkerContext = aLinkerContext;
    }

    public Type toType(BytecodeTypeRef aRef) {
        if (aRef.isPrimitive()) {
            BytecodePrimitiveTypeRef theP = (BytecodePrimitiveTypeRef) aRef;
            switch (theP) {
                case FLOAT:
                    return Type.FLOAT;
                case SHORT:
                    return Type.SHORT;
                case LONG:
                    return Type.LONG;
                case CHAR:
                    return Type.CHAR;
                case BOOLEAN:
                    return Type.BOOLEAN;
                case BYTE:
                    return Type.BYTE;
                case INT:
                    return Type.INT;
                case DOUBLE:
                    return Type.DOUBLE;
                case VOID:
                    return Type.VOID;
                default:
                    throw new IllegalArgumentException("Not supported : " + aRef);
            }
        }
        return Type.REFERENCE;
    }

    public Program generateFrom(BytecodeControlFlowGraph aFlowGraph, BytecodeMethodSignature aSignature, BytecodeAccessFlags aAccessFlags) {
        Program theProgram = new Program();

        // Initialize SSA Block structure
        ControlFlowRecreator theControlFlowRecreator = new ControlFlowRecreator();
        Map<BytecodeBasicBlock, Block> theCreatedBlocks = theControlFlowRecreator.initializeBlocksFor(theProgram, aFlowGraph);

        Set<Block> theVisited = new HashSet<>();
        Block theStart = theProgram.blockStartingAt(new BytecodeOpcodeAddress(0));

        ParsingHelper theHelper = new ParsingHelper(theStart);

        // At this point, local variables are initialized based on the method signature
        // The stack is empty
        int theCurrentIndex = 0;
        if (!aAccessFlags.isStatic()) {
            theHelper.setLocalVariable(theCurrentIndex++, new Variable(Type.REFERENCE, "thisRef", new SelfReferenceParameterValue()));
        }
        BytecodeTypeRef[] theTypes = aSignature.getArguments();
        for (int i=0;i<theTypes.length;i++) {
            BytecodeTypeRef theRef = theTypes[i];
            theHelper.setLocalVariable(theCurrentIndex++, new Variable(toType(theTypes[i]), "p" + (i + 1), new MethodParameterValue(i, theTypes[i])));
            if (theRef == BytecodePrimitiveTypeRef.LONG || theRef == BytecodePrimitiveTypeRef.DOUBLE) {
                theCurrentIndex++;
            }
        }

        // This will traverse the CFG from top to bottom
        initializeBlock(theProgram, theStart, theVisited, theHelper, aFlowGraph);

        // Finally, we have to check for blocks what were not directly accessible, for instance exception handlers or
        // finally blocks
        for (Map.Entry<BytecodeBasicBlock, Block> theEntry : theCreatedBlocks.entrySet()) {
            Block theNewBlock = theEntry.getValue();
            if (theVisited.add(theNewBlock)) {
                ParsingHelper theNewHelper = new ParsingHelper(theNewBlock);
                if (theNewBlock.getType() != Block.BlockType.NORMAL) {
                    theHelper.push(theNewBlock.newVariable(Type.REFERENCE, new CurrentExceptionValue()));
                }
                // TODO: Check what exception handler might reference from here!!!
                initializeBlock(theProgram, theNewBlock, theVisited, theNewHelper, aFlowGraph);
            }
        }

        // Check for blocks with an conditional jump at the end and the successor has PHI functions
        // For these cases we have resolve the PHIs
        for (Map.Entry<BytecodeBasicBlock, Block> theEntry : theCreatedBlocks.entrySet()) {
            Block theBlock = theEntry.getValue();
            if (!theBlock.endWithNeverReturningExpression() && !theBlock.getSuccessors().isEmpty()) {
                // This is a block
                BlockState theFinalState = theBlock.toFinalState();

                BytecodeBasicBlock theNext = aFlowGraph.getBlocks().get(aFlowGraph.getBlocks().indexOf(theEntry.getKey()) + 1);
                Block theDirectSuccessor = theCreatedBlocks.get(theNext);

                BlockState theImportingState = theDirectSuccessor.toStartState();

                theBlock.getExpressions().add(new CommentExpression("Code to jump to " + theDirectSuccessor.getStartAddress().getAddress()));

                for (Map.Entry<VariableDescription, Variable> theImport : theImportingState.getPorts().entrySet()) {
                    Variable theVariable = theImport.getValue();
                    if (theVariable.getValue() instanceof PHIFunction) {
                        // We found one, we need to resolve a variable from the final state that satisfied the required constraints
                        Variable theFinalVariable = theFinalState.findBySlot(theImport.getKey());
                        if (theFinalVariable == null) {
                            // Variable is not present, hence we assume it is null
                            InitVariableExpression theExpression = new InitVariableExpression(theVariable.withNewValue(new NullValue()));
                            theBlock.getExpressions().add(theExpression);
                        } else {
                            if (!theVariable.getName().equals(theFinalVariable.getName())) {

                                InitVariableExpression theExpression = new InitVariableExpression(
                                        theVariable.withNewValue(new VariableReferenceValue(theFinalVariable)));
                                theBlock.getExpressions().add(new CommentExpression("Resolving " + theImport.getKey()));
                                theBlock.getExpressions().add(theExpression);
                            }

                            // Propagate the type
                            theVariable.setType(theFinalVariable.getType());
                        }
                    }
                }
            }
        }

        // Try to recreate higher level control flow constructs
        theControlFlowRecreator.tryToRecreateControlFlowsIn(theCreatedBlocks);

        for (Block theBlock : theVisited) {
            processGotosIn(theBlock, theBlock.getExpressions());
        }

        AllOptimizer theOptimizer = new AllOptimizer();
        theOptimizer.optimize(theProgram, linkerContext);

        return theProgram;
    }

    private void insertCodeToJumpFrom(Block aSource, Block aTarget, Expression aExpressionToInsertBefore, ExpressionList aExpressionList) {
        BlockState theImportingState = aTarget.toStartState();

        Expression theComment = new CommentExpression("Code to jump to " + aTarget.getStartAddress().getAddress());
        aExpressionList.addBefore(theComment, aExpressionToInsertBefore);

        BlockState theFinalState = aSource.toFinalState();

        for (Map.Entry<VariableDescription, Variable> theImport : theImportingState.getPorts().entrySet()) {
            Variable theVariable = theImport.getValue();
            if (theVariable.getValue() instanceof PHIFunction) {
                // We found one, we need to resolve a variable from the final state that satisfied the required constraints
                Variable theFinalVariable = theFinalState.findBySlot(theImport.getKey());
                if (theFinalVariable == null) {
                    // Variable is not present, hence we assume it is null
                    InitVariableExpression theInitExpression = new InitVariableExpression(theVariable.withNewValue(new NullValue()));
                    aExpressionList.addBefore(theInitExpression, aExpressionToInsertBefore);
                } else {
                    if (!theVariable.getName().equals(theFinalVariable.getName())) {
                        InitVariableExpression theInitExpression = new InitVariableExpression(
                                theVariable.withNewValue(new VariableReferenceValue(theFinalVariable)));
                        aExpressionList.addBefore(theInitExpression, aExpressionToInsertBefore);
                    }
                }
            }
        }

    }

    private void processGotosIn(Block aBlock, ExpressionList aList) {
        for (Expression theExpression : aList.toList()) {
            if (theExpression instanceof HighLevelIFExpression) {
                // Do nothing here yet!
                HighLevelIFExpression theIf = (HighLevelIFExpression) theExpression;
                insertCodeToJumpFrom(aBlock, theIf.getThenBlock(), theIf.getThenBlock().getExpressions().firstExpression(), theIf.getThenBlock().getExpressions());
                insertCodeToJumpFrom(aBlock, theIf.getElseBlock(), theIf.getElseBlock().getExpressions().firstExpression(), theIf.getElseBlock().getExpressions());
                continue;
            }
            if (theExpression instanceof ExpressionListContainer) {
                ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                for (ExpressionList theList : theContainer.getExpressionLists()) {
                    processGotosIn(aBlock, theList);
                }
            }
            if (theExpression instanceof GotoExpression) {
                GotoExpression theGOTO = (GotoExpression) theExpression;
                Block theTargetBlock = aBlock.successorByJumpTarget(theGOTO.getJumpTarget());

                insertCodeToJumpFrom(aBlock, theTargetBlock, theGOTO, aList);
            }
        }
    }

    private void initializeBlock(Program aProgram, Block aCurrentBlock, Set<Block> aAlreadyVisited, ParsingHelper aHelper, BytecodeControlFlowGraph aGraph) {
        if (aAlreadyVisited.add(aCurrentBlock)) {

            initializeBlockWith(aCurrentBlock, aGraph, aHelper);

            for (Block theSuccessor : aCurrentBlock.getSuccessors()) {
                if (!aAlreadyVisited.contains(theSuccessor)) {
                    ParsingHelper theNewHelper;
                    if (theSuccessor.getPredecessors().size() == 1) {
                        // Everything is ok
                        theNewHelper = aHelper.cloneToNewFor(theSuccessor);
                    } else {
                        // We have a join point, so we have to introduce PHI function for everything that was imported
                        // This is the naive implementation that can be vastly improved
                        theNewHelper = aHelper.cloneToNewWithPHIFunctions(theSuccessor, aProgram);
                    }

                    initializeBlock(aProgram, theSuccessor, aAlreadyVisited, theNewHelper, aGraph);
                }
            }
        }
    }

    protected Variable rootFor(Variable aVariable) {
        Value theValue = aVariable.getValue();
        if (theValue instanceof VariableReferenceValue) {
            VariableReferenceValue theRef = (VariableReferenceValue) theValue;
            return rootFor(theRef.getVariable());
        }
        return aVariable;
    }

    private void initializeBlockWith(Block aTargetBlock, BytecodeControlFlowGraph aControlFlowGraph,  ParsingHelper theHelper) {

        // Finally we can start to parse the program
        BytecodeBasicBlock theBytecodeBlock = aControlFlowGraph.blockByStartAddress(aTargetBlock.getStartAddress());

        for (BytecodeInstruction theInstruction : theBytecodeBlock.getInstructions()) {

            if (theInstruction instanceof BytecodeInstructionNOP) {
                BytecodeInstructionNOP theINS = (BytecodeInstructionNOP) theInstruction;
                // Completely ignored
            } else if (theInstruction instanceof BytecodeInstructionMONITORENTER) {
                BytecodeInstructionMONITORENTER theINS = (BytecodeInstructionMONITORENTER) theInstruction;
                // Completely ignored
            } else if (theInstruction instanceof BytecodeInstructionMONITOREXIT) {
                BytecodeInstructionMONITOREXIT theINS = (BytecodeInstructionMONITOREXIT) theInstruction;
                // Completely ignored
            } else if (theInstruction instanceof BytecodeInstructionCHECKCAST) {
                BytecodeInstructionCHECKCAST theINS = (BytecodeInstructionCHECKCAST) theInstruction;
                Variable theVariable = theHelper.peek();
                aTargetBlock.addExpression(new CheckCastExpression(theVariable, theINS.getTypeCheck()));
            } else if (theInstruction instanceof BytecodeInstructionPOP) {
                BytecodeInstructionPOP theINS = (BytecodeInstructionPOP) theInstruction;
                theHelper.pop();
            } else if (theInstruction instanceof BytecodeInstructionPOP2) {
                BytecodeInstructionPOP2 theINS = (BytecodeInstructionPOP2) theInstruction;
                Variable theVariable = theHelper.pop();
                switch (theVariable.getType()) {
                    case LONG:
                        break;
                    case DOUBLE:
                        break;
                    default:
                        theHelper.pop();
                }
            } else if (theInstruction instanceof BytecodeInstructionDUP) {
                BytecodeInstructionDUP theINS = (BytecodeInstructionDUP) theInstruction;
                Variable theVariable = theHelper.peek();
                Variable theClone = aTargetBlock.newVariable(theVariable.getType(), new VariableReferenceValue(theVariable));
                theHelper.push(theClone);
            } else if (theInstruction instanceof BytecodeInstructionDUP2X1) {
                BytecodeInstructionDUP2X1 theINS = (BytecodeInstructionDUP2X1) theInstruction;
                Variable theValue1 = theHelper.pop();
                if (theValue1.getType() == Type.LONG || theValue1.getType() == Type.DOUBLE) {
                    Variable theValue2 = theHelper.pop();
                    Variable theClone1 = aTargetBlock.newVariable(theValue1.getType(), new VariableReferenceValue(theValue2));

                    theHelper.push(theValue1);
                    theHelper.push(theValue2);
                    theHelper.push(theClone1);
                } else {
                    Variable theValue2 = theHelper.pop();
                    Variable theValue3 = theHelper.pop();
                    Variable theClone1 = aTargetBlock.newVariable(theValue1.getType(), new VariableReferenceValue(theValue2));
                    Variable theClone2 = aTargetBlock.newVariable(theValue1.getType(), new VariableReferenceValue(theValue2));

                    theHelper.push(theValue2);
                    theHelper.push(theValue1);
                    theHelper.push(theValue3);
                    theHelper.push(theClone2);
                    theHelper.push(theClone1);
                }
            } else if (theInstruction instanceof BytecodeInstructionDUPX1) {
                BytecodeInstructionDUPX1 theINS = (BytecodeInstructionDUPX1) theInstruction;
                Variable theValue1 = theHelper.pop();
                Variable theValue2 = theHelper.pop();

                theHelper.push(theValue1);
                theHelper.push(theValue2);
                Variable theClone = aTargetBlock.newVariable(theValue1.getType(), new VariableReferenceValue(theValue1));
                theHelper.push(theClone);
            } else if (theInstruction instanceof BytecodeInstructionGETSTATIC) {
                BytecodeInstructionGETSTATIC theINS = (BytecodeInstructionGETSTATIC) theInstruction;
                GetStaticValue theValue = new GetStaticValue(theINS.getConstant());
                Variable theVariable = aTargetBlock.newVariable(toType(theINS.getConstant().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().fieldType()), theValue);
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
                Variable theVariable = aTargetBlock.newVariable(Type.REFERENCE, new ArrayEntryValue(theTarget, theIndex));
                theHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericArrayLOAD) {
                BytecodeInstructionGenericArrayLOAD theINS = (BytecodeInstructionGenericArrayLOAD) theInstruction;
                Variable theIndex = theHelper.pop();
                Variable theTarget = theHelper.pop();
                Variable theVariable = aTargetBlock.newVariable(toType(theINS.getType()), new ArrayEntryValue(theTarget, theIndex));
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
                Variable theVariable = aTargetBlock.newVariable(Type.REFERENCE, new NullValue());
                theHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionPUTFIELD) {
                BytecodeInstructionPUTFIELD theINS = (BytecodeInstructionPUTFIELD) theInstruction;
                Variable theValue = theHelper.pop();
                Variable theTarget = theHelper.pop();
                aTargetBlock.addExpression(new PutFieldExpression(theINS.getFieldRefConstant(), theTarget, theValue));
            } else if (theInstruction instanceof BytecodeInstructionGETFIELD) {
                BytecodeInstructionGETFIELD theINS = (BytecodeInstructionGETFIELD) theInstruction;
                Variable theTarget = theHelper.pop();
                Variable theVariable = aTargetBlock.newVariable(toType(theINS.getFieldRefConstant().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().fieldType()), new GetFieldValue(theINS.getFieldRefConstant(), theTarget));
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
                    Variable theVariable = aTargetBlock.newVariable(Type.DOUBLE, new DoubleValue(theC.getDoubleValue()));
                    theHelper.push(theVariable);
                } else if (theConstant instanceof BytecodeLongConstant) {
                    BytecodeLongConstant theC = (BytecodeLongConstant) theConstant;
                    Variable theVariable = aTargetBlock.newVariable(Type.LONG, new LongValue(theC.getLongValue()));
                    theHelper.push(theVariable);
                } else if (theConstant instanceof BytecodeFloatConstant) {
                    BytecodeFloatConstant theC = (BytecodeFloatConstant) theConstant;
                    Variable theVariable = aTargetBlock.newVariable(Type.FLOAT, new FloatValue(theC.getFloatValue()));
                    theHelper.push(theVariable);
                } else if (theConstant instanceof BytecodeIntegerConstant) {
                    BytecodeIntegerConstant theC = (BytecodeIntegerConstant) theConstant;
                    Variable theVariable = aTargetBlock.newVariable(Type.INT, new IntegerValue(theC.getIntegerValue()));
                    theHelper.push(theVariable);
                } else if (theConstant instanceof BytecodeStringConstant) {
                    BytecodeStringConstant theC = (BytecodeStringConstant) theConstant;
                    Variable theVariable = aTargetBlock.newVariable(Type.REFERENCE, new StringValue(theC.getValue().stringValue()));
                    theHelper.push(theVariable);
                } else if (theConstant instanceof BytecodeClassinfoConstant) {
                    BytecodeClassinfoConstant theC = (BytecodeClassinfoConstant) theConstant;
                    Variable theVariable = aTargetBlock.newVariable(Type.REFERENCE, new ClassReferenceValue(BytecodeObjectTypeRef.fromUtf8Constant(theC.getConstant())));
                    theHelper.push(theVariable);
                } else {
                    throw new IllegalArgumentException("Unsupported constant type : " + theConstant);
                }
            } else if (theInstruction instanceof BytecodeInstructionBIPUSH) {
                BytecodeInstructionBIPUSH theINS = (BytecodeInstructionBIPUSH) theInstruction;
                Variable theVariable = aTargetBlock.newVariable(Type.BYTE, new ByteValue(theINS.getByteValue()));
                theHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionSIPUSH) {
                BytecodeInstructionSIPUSH theINS = (BytecodeInstructionSIPUSH) theInstruction;
                Variable theVariable = aTargetBlock.newVariable(Type.SHORT, new ShortValue(theINS.getShortValue()));
                theHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionICONST) {
                BytecodeInstructionICONST theINS = (BytecodeInstructionICONST) theInstruction;
                Variable theVariable = aTargetBlock.newVariable(Type.INT, new IntegerValue(theINS.getIntConst()));
                theHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionFCONST) {
                BytecodeInstructionFCONST theINS = (BytecodeInstructionFCONST) theInstruction;
                Variable theVariable = aTargetBlock.newVariable(Type.FLOAT, new FloatValue(theINS.getFloatValue()));
                theHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionDCONST) {
                BytecodeInstructionDCONST theINS = (BytecodeInstructionDCONST) theInstruction;
                Variable theVariable = aTargetBlock.newVariable(Type.DOUBLE, new DoubleValue(theINS.getDoubleConst()));
                theHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionLCONST) {
                BytecodeInstructionLCONST theINS = (BytecodeInstructionLCONST) theInstruction;
                Variable theVariable = aTargetBlock.newVariable(Type.LONG, new LongValue(theINS.getLongConst()));
                theHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericNEG) {
                BytecodeInstructionGenericNEG theINS = (BytecodeInstructionGenericNEG) theInstruction;
                Variable theVariable = theHelper.pop();
                Variable theNegatedValue = aTargetBlock.newVariable(theVariable.getType(), new NegatedValue(theVariable));
                theHelper.push(theNegatedValue);
            } else if (theInstruction instanceof BytecodeInstructionARRAYLENGTH) {
                BytecodeInstructionARRAYLENGTH theINS = (BytecodeInstructionARRAYLENGTH) theInstruction;
                Variable theVariable = theHelper.pop();
                Variable theNegatedValue = aTargetBlock.newVariable(Type.INT, new ArrayLengthValue(theVariable));
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
                Variable theNewVariable = aTargetBlock.newVariable(Type.INT, new CompareValue(theValue1, theValue2));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionLCMP) {
                BytecodeInstructionLCMP theINS = (BytecodeInstructionLCMP) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(Type.INT, new CompareValue(theValue1, theValue2));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionIINC) {
                BytecodeInstructionIINC theINS = (BytecodeInstructionIINC) theInstruction;
                Variable theVariable = theHelper.getLocalVariable(theINS.getIndex());
                Variable theAmount = aTargetBlock.newVariable(Type.INT, new IntegerValue(theINS.getConstant()));
                Variable theNewVariable = aTargetBlock.newVariable(Type.INT, new BinaryValue(theVariable, BinaryValue.Operator.ADD, theAmount));
                theHelper.setLocalVariable(theINS.getIndex(), theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericREM) {
                BytecodeInstructionGenericREM theINS = (BytecodeInstructionGenericREM) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(toType(theINS.getType()), new BinaryValue(theValue1, BinaryValue.Operator.REMAINDER, theValue2));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericADD) {
                BytecodeInstructionGenericADD theINS = (BytecodeInstructionGenericADD) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(toType(theINS.getType()), new BinaryValue(theValue1, BinaryValue.Operator.ADD, theValue2));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericDIV) {
                BytecodeInstructionGenericDIV theINS = (BytecodeInstructionGenericDIV) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();

                Variable theNewVariable;
                Value theDivValue = new BinaryValue(theValue1, BinaryValue.Operator.DIV, theValue2);
                switch (theINS.getType()) {
                    case FLOAT:
                        theNewVariable = aTargetBlock.newVariable(toType(theINS.getType()), theDivValue);
                        break;
                    case DOUBLE:
                        theNewVariable = aTargetBlock.newVariable(toType(theINS.getType()), theDivValue);
                        break;
                    default:
                        theNewVariable = aTargetBlock.newVariable(toType(theINS.getType()), new FloorValue(theDivValue));
                        break;
                }

                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericMUL) {
                BytecodeInstructionGenericMUL theINS = (BytecodeInstructionGenericMUL) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(toType(theINS.getType()), new BinaryValue(theValue1, BinaryValue.Operator.MUL, theValue2));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericSUB) {
                BytecodeInstructionGenericSUB theINS = (BytecodeInstructionGenericSUB) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(toType(theINS.getType()), new BinaryValue(theValue1, BinaryValue.Operator.SUB, theValue2));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericXOR) {
                BytecodeInstructionGenericXOR theINS = (BytecodeInstructionGenericXOR) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(toType(theINS.getType()), new BinaryValue(theValue1, BinaryValue.Operator.BINARYXOR, theValue2));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericOR) {
                BytecodeInstructionGenericOR theINS = (BytecodeInstructionGenericOR) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(toType(theINS.getType()), new BinaryValue(theValue1, BinaryValue.Operator.BINARYOR, theValue2));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericAND) {
                BytecodeInstructionGenericAND theINS = (BytecodeInstructionGenericAND) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(toType(theINS.getType()), new BinaryValue(theValue1, BinaryValue.Operator.BINARYAND, theValue2));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericSHL) {
                BytecodeInstructionGenericSHL theINS = (BytecodeInstructionGenericSHL) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(toType(theINS.getType()), new BinaryValue(theValue1, BinaryValue.Operator.BINARYSHIFTLEFT, theValue2));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericSHR) {
                BytecodeInstructionGenericSHR theINS = (BytecodeInstructionGenericSHR) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(toType(theINS.getType()), new BinaryValue(theValue1, BinaryValue.Operator.BINARYSHIFTRIGHT, theValue2));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericUSHR) {
                BytecodeInstructionGenericUSHR theINS = (BytecodeInstructionGenericUSHR) theInstruction;
                Variable theValue2 = theHelper.pop();
                Variable theValue1 = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(toType(theINS.getType()), new BinaryValue(theValue1, BinaryValue.Operator.BINARYUNSIGNEDSHIFTRIGHT, theValue2));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionIFNULL) {
                BytecodeInstructionIFNULL theINS = (BytecodeInstructionIFNULL) theInstruction;
                Variable theValue = theHelper.pop();
                FixedBinaryValue theBinaryValue = new FixedBinaryValue(theValue, FixedBinaryValue.Operator.ISNULL);
                Variable theResult = aTargetBlock.newVariable(Type.BOOLEAN, theBinaryValue);

                ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(theINS.getJumpTarget(), aTargetBlock));

                aTargetBlock.addExpression(new IFExpression(theResult, theINS.getJumpTarget(), theExpressions));
            } else if (theInstruction instanceof BytecodeInstructionIFNONNULL) {
                BytecodeInstructionIFNONNULL theINS = (BytecodeInstructionIFNONNULL) theInstruction;
                Variable theValue = theHelper.pop();
                FixedBinaryValue theBinaryValue = new FixedBinaryValue(theValue, FixedBinaryValue.Operator.ISNONNULL);
                Variable theResult = aTargetBlock.newVariable(Type.BOOLEAN, theBinaryValue);

                ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(theINS.getJumpTarget(), aTargetBlock));

                aTargetBlock.addExpression(new IFExpression(theResult, theINS.getJumpTarget(), theExpressions));
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
                Variable theNewVariable = aTargetBlock.newVariable(Type.BOOLEAN, theBinaryValue);

                ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(theINS.getJumpTarget(), aTargetBlock));

                aTargetBlock.addExpression(new IFExpression(theNewVariable, theINS.getJumpTarget(), theExpressions));

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
                Variable theNewVariable = aTargetBlock.newVariable(Type.BOOLEAN, theBinaryValue);

                ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(theINS.getJumpTarget(), aTargetBlock));

                aTargetBlock.addExpression(new IFExpression(theNewVariable, theINS.getJumpTarget(), theExpressions));

            } else if (theInstruction instanceof BytecodeInstructionIFCOND) {
                BytecodeInstructionIFCOND theINS = (BytecodeInstructionIFCOND) theInstruction;
                Variable theValue = theHelper.pop();
                Variable theZero = aTargetBlock.newVariable(Type.INT, new IntegerValue(0));
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
                Variable theNewVariable = aTargetBlock.newVariable(Type.BOOLEAN, theBinaryValue);

                ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(theINS.getJumpTarget(), aTargetBlock));

                aTargetBlock.addExpression(new IFExpression(theNewVariable, theINS.getJumpTarget(), theExpressions));
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

                BytecodeClassinfoConstant theClassInfo = theINS.getClassInfoForObjectToCreate();
                BytecodeObjectTypeRef theObjectType = BytecodeObjectTypeRef.fromUtf8Constant(theClassInfo.getConstant());
                if (theObjectType.name().equals(Address.class.getName())) {
                    // At this time the exact location is unknown, the value
                    // will be set at constructor invocation time
                    Variable theNewVariable = aTargetBlock.newVariable(Type.MEMORYLOCATION, new UnknownValue());
                    theHelper.push(theNewVariable);
                } else {
                    Variable theNewVariable = aTargetBlock.newVariable(Type.REFERENCE, new NewObjectValue(theClassInfo));
                    theHelper.push(theNewVariable);
                }
            } else if (theInstruction instanceof BytecodeInstructionNEWARRAY) {
                BytecodeInstructionNEWARRAY theINS = (BytecodeInstructionNEWARRAY) theInstruction;
                Variable theLength = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(Type.REFERENCE, new NewArrayValue(theINS.getPrimitiveType(), theLength));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionNEWMULTIARRAY) {
                BytecodeInstructionNEWMULTIARRAY theINS = (BytecodeInstructionNEWMULTIARRAY) theInstruction;
                List<Variable> theDimensions = new ArrayList<>();
                for (int i=0;i<theINS.getDimensions();i++) {
                    theDimensions.add(theHelper.pop());
                }
                Collections.reverse(theDimensions);
                BytecodeTypeRef theTypeRef = linkerContext.getSignatureParser().toFieldType(new BytecodeUtf8Constant(theINS.getTypeConstant().getConstant().stringValue()));
                Variable theNewVariable = aTargetBlock.newVariable(Type.REFERENCE, new NewMultiArrayValue(theTypeRef, theDimensions));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionANEWARRAY) {
                BytecodeInstructionANEWARRAY theINS = (BytecodeInstructionANEWARRAY) theInstruction;
                Variable theLength = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(Type.REFERENCE, new NewArrayValue(BytecodeObjectTypeRef.fromUtf8Constant(theINS.getTypeConstant().getConstant()), theLength));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGOTO) {
                BytecodeInstructionGOTO theINS = (BytecodeInstructionGOTO) theInstruction;
                aTargetBlock.addExpression(new GotoExpression(theINS.getJumpAddress(), aTargetBlock));
            } else if (theInstruction instanceof BytecodeInstructionL2Generic) {
                BytecodeInstructionL2Generic theINS = (BytecodeInstructionL2Generic) theInstruction;
                Variable theValue = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(toType(theINS.getTargetType()), new TypeConversionValue(theValue, theINS.getTargetType()));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionI2Generic) {
                BytecodeInstructionI2Generic theINS = (BytecodeInstructionI2Generic) theInstruction;
                Variable theValue = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(toType(theINS.getTargetType()), new TypeConversionValue(theValue, theINS.getTargetType()));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionF2Generic) {
                BytecodeInstructionF2Generic theINS = (BytecodeInstructionF2Generic) theInstruction;
                Variable theValue = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(toType(theINS.getTargetType()), new TypeConversionValue(theValue, theINS.getTargetType()));
                theHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionD2Generic) {
                BytecodeInstructionD2Generic theINS = (BytecodeInstructionD2Generic) theInstruction;
                Variable theValue = theHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(toType(theINS.getTargetType()), new TypeConversionValue(theValue, theINS.getTargetType()));
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

                BytecodeObjectTypeRef theType = BytecodeObjectTypeRef.fromUtf8Constant(theINS.getMethodReference().getClassIndex().getClassConstant().getConstant());
                if (theType.name().equals(Address.class.getName())) {
                    Variable theRoot = rootFor(theTarget);
                    theRoot.setValue(theArguments.get(0).getValue());

                } else {
                    String theMethodName = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue();

                    DirectInvokeMethodValue theValue = new DirectInvokeMethodValue(theType, theMethodName, theSignature, theTarget, theArguments);
                    if (theSignature.getReturnType().isVoid()) {
                        aTargetBlock.addExpression(new DirectInvokeMethodExpression(theValue));
                    } else {
                        Variable theNewVariable = aTargetBlock.newVariable(toType(theSignature.getReturnType()), theValue);
                        theHelper.push(theNewVariable);
                    }
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
                    Variable theNewVariable = aTargetBlock.newVariable(toType(theSignature.getReturnType()), theValue);
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
                    Variable theNewVariable = aTargetBlock.newVariable(toType(theSignature.getReturnType()), theValue);
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

                BytecodeClassinfoConstant theTargetClass = theINS.getMethodReference().getClassIndex().getClassConstant();
                BytecodeObjectTypeRef theObjectType = BytecodeObjectTypeRef.fromUtf8Constant(theTargetClass.getConstant());
                if (theObjectType.name().equals(GC.class.getName()) && "initTestMemory".equals(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue())) {
                    // This invocation can be skipped!!!
                } else if (theObjectType.name().equals(Address.class.getName())) {
                    String theMethodName = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex()
                            .getName().stringValue();
                    switch (theMethodName) {
                    case "setIntValue": {

                        Variable theTarget = theArguments.get(0);
                        Variable theOffset = theArguments.get(1);
                        Variable theNewValue = theArguments.get(2);

                        ComputedMemoryLocationWriteValue theLocation = new ComputedMemoryLocationWriteValue(theTarget, theOffset);
                        Variable theNewVariable = aTargetBlock.newVariable(Type.INT, theLocation);
                        aTargetBlock.addExpression(new SetMemoryLocationExpression(theNewVariable, theNewValue));
                        break;
                    }
                    case "setObjectValue": {

                        Variable theTarget = theArguments.get(0);
                        Variable theOffset = theArguments.get(1);
                        Variable theNewValue = theArguments.get(2);

                        ComputedMemoryLocationWriteValue theLocation = new ComputedMemoryLocationWriteValue(theTarget, theOffset);
                        Variable theNewVariable = aTargetBlock.newVariable(Type.REFERENCE, theLocation);
                        aTargetBlock.addExpression(new SetMemoryLocationExpression(theNewVariable, theNewValue));
                        break;
                    }
                    case "getStart": {

                        Variable theTarget = theArguments.get(0);
                        Variable theNewVariable = aTargetBlock.newVariable(Type.REFERENCE, new VariableReferenceValue(theTarget));

                        theHelper.push(theNewVariable);
                        break;
                    }
                    case "getIntValue": {

                        Variable theTarget = theArguments.get(0);
                        Variable theOffset = theArguments.get(1);

                        ComputedMemoryLocationReadValue theLocation = new ComputedMemoryLocationReadValue(theTarget, theOffset);
                        Variable theNewVariable = aTargetBlock.newVariable(Type.INT, theLocation);
                        theHelper.push(theNewVariable);

                        break;
                    }
                    case "getObjectValue": {
                        Variable theTarget = theArguments.get(0);
                        Variable theOffset = theArguments.get(1);

                        ComputedMemoryLocationReadValue theLocation = new ComputedMemoryLocationReadValue(theTarget, theOffset);
                        Variable theNewVariable = aTargetBlock.newVariable(Type.REFERENCE, theLocation);
                        theHelper.push(theNewVariable);

                        break;
                    }
                    default:
                        throw new IllegalStateException("Not implemented : " + theMethodName);
                    }
                } else {
                    if (theArguments.size() > 0 && theArguments.get(0).getType() == Type.METHODTYPE) {
                        // We are invoking a method pointer here,
                        Variable theTarget = theArguments.get(0);
                        List<Variable> theRemainingArguments = theArguments.subList(1, theArguments.size());
                        InvokeMethodTypeValue theValue = new InvokeMethodTypeValue(theTarget, theRemainingArguments);
                        if (theSignature.getReturnType().isVoid()) {
                            aTargetBlock.addExpression(new InvokeMethodTypeExpression(theValue));
                        } else {
                            Variable theNewVariable = aTargetBlock.newVariable(toType(theSignature.getReturnType()), theValue);
                            theHelper.push(theNewVariable);
                        }
                    } else {
                        InvokeStaticMethodValue theValue = new InvokeStaticMethodValue(theINS.getMethodReference(), theArguments);
                        if (theSignature.getReturnType().isVoid()) {
                            aTargetBlock.addExpression(new InvokeStaticMethodExpression(theValue));
                        } else {
                            Variable theNewVariable = aTargetBlock.newVariable(toType(theSignature.getReturnType()), theValue);
                            theHelper.push(theNewVariable);
                        }
                    }
                }
            } else if (theInstruction instanceof BytecodeInstructionINSTANCEOF) {
                BytecodeInstructionINSTANCEOF theINS = (BytecodeInstructionINSTANCEOF) theInstruction;

                Variable theVariable = theHelper.pop();
                InstanceOfValue theValue = new InstanceOfValue(theVariable, theINS.getTypeRef());

                Variable theCheckResult = aTargetBlock.newVariable(Type.BOOLEAN, theValue);
                theHelper.push(theCheckResult);
            } else if (theInstruction instanceof BytecodeInstructionTABLESWITCH) {
                BytecodeInstructionTABLESWITCH theINS = (BytecodeInstructionTABLESWITCH) theInstruction;
                Variable theVariable = theHelper.pop();

                ExpressionList theDefault = new ExpressionList();
                theDefault.add(new GotoExpression(theINS.getDefaultJumpTarget(), aTargetBlock));

                Map<Long, ExpressionList> theOffsets = new HashMap<>();
                long[] theJumpTargets = theINS.getOffsets();
                for (int i=0;i<theJumpTargets.length;i++) {
                    ExpressionList theJump = new ExpressionList();
                    theJump.add(new GotoExpression(theINS.getOpcodeAddress().add((int) theJumpTargets[i]), aTargetBlock));
                    theOffsets.put((long) i, theJump);
                }

                aTargetBlock.addExpression(new TableSwitchExpression(theVariable, theINS.getLowValue(), theINS.getHighValue(),
                        theDefault, theOffsets));
            } else if (theInstruction instanceof BytecodeInstructionLOOKUPSWITCH) {
                BytecodeInstructionLOOKUPSWITCH theINS = (BytecodeInstructionLOOKUPSWITCH) theInstruction;
                Variable theVariable = theHelper.pop();

                ExpressionList theDefault = new ExpressionList();
                theDefault.add(new GotoExpression(theINS.getDefaultJumpTarget(), aTargetBlock));

                Map<Long, ExpressionList> thePairs = new HashMap<>();
                for (BytecodeInstructionLOOKUPSWITCH.Pair thePair : theINS.getPairs()) {
                    ExpressionList thePairExpressions = new ExpressionList();
                    thePairExpressions.add(new GotoExpression(theINS.getOpcodeAddress().add((int) thePair.getOffset()), aTargetBlock));
                    thePairs.put((long) thePair.getMatch(), thePairExpressions);
                }

                aTargetBlock.addExpression(new LookupSwitchExpression(theVariable, theDefault, thePairs));
            } else if (theInstruction instanceof BytecodeInstructionINVOKEDYNAMIC) {
                BytecodeInstructionINVOKEDYNAMIC theINS = (BytecodeInstructionINVOKEDYNAMIC) theInstruction;

                BytecodeInvokeDynamicConstant theConstant = theINS.getCallSite();

                BytecodeMethodSignature theSignature = theConstant.getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                BytecodeBootstrapMethodsAttributeInfo theBootStrapMethods = aControlFlowGraph.getOwningClass().getAttributes().getByType(BytecodeBootstrapMethodsAttributeInfo.class);
                BytecodeBootstrapMethod theBootstrapMethod = theBootStrapMethods.methodByIndex(theConstant.getBootstrapMethodAttributeIndex().getIndex());
                BytecodeMethodHandleConstant theMethodRef = theBootstrapMethod.getMethodRef();

                switch (theMethodRef.getReferenceKind()) {
                    case REF_invokeStatic: {
                        // in this case we assume that the invoke dynamic can be replaced by an invokestatic
                        // to the implementing method
                        BytecodeMethodHandleConstant theHandle = (BytecodeMethodHandleConstant) theBootstrapMethod.getArguments()[0];
                        BytecodeMethodRefConstant theImplementingMethodRef = (BytecodeMethodRefConstant) theHandle.getReferenceIndex().getConstant();

                        List<Variable> theArguments = new ArrayList<>();
                        BytecodeTypeRef[] theArgumentTypes = theSignature.getArguments();
                        for (int i=0;i<theArgumentTypes.length;i++) {
                            theArguments.add(theHelper.pop());
                        }
                        Collections.reverse(theArguments);

                        MethodTypeValue theValue = new MethodTypeValue(theImplementingMethodRef);
                        Variable theNewVariable = aTargetBlock.newVariable(Type.METHODTYPE, theValue);
                        theHelper.push(theNewVariable);

                        break;
                    }
                    default:
                        throw new IllegalStateException("Nut supported reference kind for invoke dynamic : " + theMethodRef.getReferenceKind());
                }
            } else {
                throw new IllegalArgumentException("Not implemented : " + theInstruction);
            }
        }

        aTargetBlock.addExpression(new CommentExpression("Final stack size is " + theHelper.stack.size()));

        theHelper.finalizeExportState();
    }
}