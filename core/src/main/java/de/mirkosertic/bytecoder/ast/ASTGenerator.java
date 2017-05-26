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
package de.mirkosertic.bytecoder.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import de.mirkosertic.bytecoder.classlib.ExceptionRethrower;
import de.mirkosertic.bytecoder.core.BytecodeBasicBlock;
import de.mirkosertic.bytecoder.core.BytecodeInstruction;
import de.mirkosertic.bytecoder.core.BytecodeInstructionAALOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionAASTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionACONSTNULL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionALOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionANEWARRAY;
import de.mirkosertic.bytecoder.core.BytecodeInstructionARETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionARRAYLENGTH;
import de.mirkosertic.bytecoder.core.BytecodeInstructionASTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionATHROW;
import de.mirkosertic.bytecoder.core.BytecodeInstructionBIPUSH;
import de.mirkosertic.bytecoder.core.BytecodeInstructionCHECKCAST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionD2Generic;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDUP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionF2Generic;
import de.mirkosertic.bytecoder.core.BytecodeInstructionFCMP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionFCONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGETFIELD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGETSTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGOTO;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericADD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericALOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericAND;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericASTORE;
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
import de.mirkosertic.bytecoder.core.BytecodeInstructionNEW;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNEWARRAY;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPOP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPUTFIELD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPUTSTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionRETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionSIPUSH;
import de.mirkosertic.bytecoder.core.BytecodeInterfaceRefConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethodRefConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeNameAndTypeConstant;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeUtf8Constant;

public class ASTGenerator {

    public static class GenerationResult {

        private final ASTGenerator generator;
        private final ASTBlock block;
        private final Stack<ASTValue> currentStack;
        private int additionalVariablesCounter;

        public GenerationResult(ASTGenerator aGenerator, ASTBlock aBlock, Stack<ASTValue> aStackRemainder) {
            generator = aGenerator;
            block = aBlock;
            currentStack = aStackRemainder;
            additionalVariablesCounter = 100;
        }

        public ASTBlock getBlock() {
            return block;
        }

        public GenerationResult continueWith(BytecodeBasicBlock aBasicBlock) {
            return generator.generateFrom(aBasicBlock, new GenerationResult(generator, new ASTBlock(), currentStack));
        }

        public void push(ASTValue aValue) {
            currentStack.push(aValue);
        }

        public ASTValue pop() {
            return currentStack.pop();
        }

        public ASTValue popAndStoreToLocal() {
            ASTValue theValue = currentStack.pop();
            return toNewLocalVariable(theValue);
        }

        public ASTValue toNewLocalVariable(ASTValue aValue) {
            int theNewVariable = additionalVariablesCounter++;
            block.add(new ASTSetLocalVariable(theNewVariable, aValue));
            return new ASTLocalVariable(theNewVariable);
        }

        public ASTValue peek() {
            return currentStack.peek();
        }
    }

    public GenerationResult generateFrom(BytecodeBasicBlock aBasicBlock) {
        Stack<ASTValue> theCurrentValueStack = new Stack<>();
        theCurrentValueStack.push(new ASTGetStatic(BytecodeObjectTypeRef.fromRuntimeClass(ExceptionRethrower.class), "lastMethodOutcome"));

        return generateFrom(aBasicBlock, new GenerationResult(this, new ASTBlock(), theCurrentValueStack));
    }

    private GenerationResult generateFrom(BytecodeBasicBlock aBasicBlock, GenerationResult aGenerationResult) {

        for (BytecodeInstruction theInstruction : aBasicBlock.getInstructions()) {
            if (theInstruction instanceof BytecodeInstructionBIPUSH) {
                BytecodeInstructionBIPUSH thePush = (BytecodeInstructionBIPUSH) theInstruction;
                aGenerationResult.push(new ASTByteValue(thePush.getByteValue()));
            } else if (theInstruction instanceof BytecodeInstructionINSTANCEOF) {
                BytecodeInstructionINSTANCEOF theOf = (BytecodeInstructionINSTANCEOF) theInstruction;
                ASTValue theReference = aGenerationResult.popAndStoreToLocal();
                aGenerationResult.push(new ASTInstanceOf(theReference, theOf.getTypeRef()));
            } else if (theInstruction instanceof BytecodeInstructionPOP) {
                aGenerationResult.pop();
            } else if (theInstruction instanceof BytecodeInstructionRETURN) {
                aGenerationResult.getBlock().add(new ASTReturn());
            } else if (theInstruction instanceof BytecodeInstructionGenericLDC) {
                BytecodeInstructionGenericLDC theLoad = (BytecodeInstructionGenericLDC) theInstruction;
                aGenerationResult.push(new ASTConstant(theLoad.constant()));
            } else if (theInstruction instanceof BytecodeInstructionGenericLOAD) {
                BytecodeInstructionGenericLOAD theLoad = (BytecodeInstructionGenericLOAD) theInstruction;
                aGenerationResult.push(aGenerationResult.toNewLocalVariable(new ASTLocalVariable(theLoad.getLocalVariableIndex())));
            } else if (theInstruction instanceof BytecodeInstructionACONSTNULL) {
                aGenerationResult.push(new ASTNull());
            } else if (theInstruction instanceof BytecodeInstructionARETURN) {
                aGenerationResult.getBlock().add(new ASTObjectReturn(aGenerationResult.pop()));
            } else if (theInstruction instanceof BytecodeInstructionGenericRETURN) {
               aGenerationResult.getBlock().add(new ASTObjectReturn(aGenerationResult.pop()));
            } else if (theInstruction instanceof BytecodeInstructionALOAD) {
                BytecodeInstructionALOAD theLoad = (BytecodeInstructionALOAD) theInstruction;
                aGenerationResult.push(aGenerationResult.toNewLocalVariable(new ASTLocalVariable(theLoad.getVariableIndex())));
            } else if (theInstruction instanceof BytecodeInstructionASTORE) {
                BytecodeInstructionASTORE theStore = (BytecodeInstructionASTORE) theInstruction;
                aGenerationResult.getBlock().add(new ASTSetLocalVariable(theStore.getVariableIndex(), aGenerationResult.popAndStoreToLocal()));
            } else if (theInstruction instanceof BytecodeInstructionPUTSTATIC) {
                BytecodeInstructionPUTSTATIC thePut = (BytecodeInstructionPUTSTATIC) theInstruction;
                aGenerationResult.getBlock().add(new ASTClassInitCheck(BytecodeObjectTypeRef.fromUtf8Constant(thePut.getConstant().getClassIndex().getClassConstant().getConstant())));
                aGenerationResult.getBlock().add(new ASTPutStatic(aGenerationResult.popAndStoreToLocal(), thePut.getConstant()));
            } else if (theInstruction instanceof BytecodeInstructionGETSTATIC) {
                BytecodeInstructionGETSTATIC theGet = (BytecodeInstructionGETSTATIC) theInstruction;
                BytecodeObjectTypeRef theObjectType = BytecodeObjectTypeRef.fromUtf8Constant(theGet.getConstant().getClassIndex().getClassConstant().getConstant());
                aGenerationResult.getBlock().add(new ASTClassInitCheck(theObjectType));
                String theFieldName = theGet.getConstant().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue();
                aGenerationResult.push(new ASTGetStatic(theObjectType, theFieldName));
            } else if (theInstruction instanceof BytecodeInstructionATHROW) {
                BytecodeInstructionATHROW theThrow = (BytecodeInstructionATHROW) theInstruction;
                aGenerationResult.getBlock().add(new ASTThrow(aGenerationResult.popAndStoreToLocal()));
            } else if (theInstruction instanceof BytecodeInstructionGenericADD) {
                BytecodeInstructionGenericADD theAdd = (BytecodeInstructionGenericADD) theInstruction;
                aGenerationResult.push(new ASTValuesWithOperator(aGenerationResult.popAndStoreToLocal(), ASTValuesWithOperator.Operator.ADD, aGenerationResult.popAndStoreToLocal()));
            } else if (theInstruction instanceof BytecodeInstructionGenericSUB) {
                BytecodeInstructionGenericSUB theSub = (BytecodeInstructionGenericSUB) theInstruction;
                aGenerationResult.push(new ASTValuesWithOperator(aGenerationResult.popAndStoreToLocal(), ASTValuesWithOperator.Operator.SUBSTRACT, aGenerationResult.popAndStoreToLocal()));
            } else if (theInstruction instanceof BytecodeInstructionGenericMUL) {
                BytecodeInstructionGenericMUL theMul = (BytecodeInstructionGenericMUL) theInstruction;
                aGenerationResult.push(new ASTValuesWithOperator(aGenerationResult.popAndStoreToLocal(), ASTValuesWithOperator.Operator.MULTIPLY, aGenerationResult.popAndStoreToLocal()));
            } else if (theInstruction instanceof BytecodeInstructionGenericDIV) {
                BytecodeInstructionGenericDIV theDiv = (BytecodeInstructionGenericDIV) theInstruction;
                aGenerationResult.push(new ASTValuesWithOperator(aGenerationResult.popAndStoreToLocal(), ASTValuesWithOperator.Operator.DIVIDE, aGenerationResult.popAndStoreToLocal()));
            } else if (theInstruction instanceof BytecodeInstructionGenericOR) {
                BytecodeInstructionGenericOR theOr = (BytecodeInstructionGenericOR) theInstruction;
                aGenerationResult.push(new ASTValuesWithOperator(aGenerationResult.popAndStoreToLocal(), ASTValuesWithOperator.Operator.BINARYOR, aGenerationResult.popAndStoreToLocal()));
            } else if (theInstruction instanceof BytecodeInstructionGenericAND) {
                BytecodeInstructionGenericAND theAnd = (BytecodeInstructionGenericAND) theInstruction;
                aGenerationResult.push(new ASTValuesWithOperator(aGenerationResult.popAndStoreToLocal(), ASTValuesWithOperator.Operator.BINARYAND, aGenerationResult.popAndStoreToLocal()));
            } else if (theInstruction instanceof BytecodeInstructionGenericXOR) {
                BytecodeInstructionGenericXOR theXor = (BytecodeInstructionGenericXOR) theInstruction;
                aGenerationResult.push(new ASTValuesWithOperator(aGenerationResult.popAndStoreToLocal(), ASTValuesWithOperator.Operator.BINARYXOR, aGenerationResult.popAndStoreToLocal()));
            } else if (theInstruction instanceof BytecodeInstructionDUP) {
                BytecodeInstructionDUP theDup = (BytecodeInstructionDUP) theInstruction;
                aGenerationResult.push(aGenerationResult.toNewLocalVariable(aGenerationResult.peek()));
            } else if (theInstruction instanceof BytecodeInstructionINVOKESTATIC) {

                BytecodeInstructionINVOKESTATIC theInvoke = (BytecodeInstructionINVOKESTATIC) theInstruction;

                BytecodeMethodRefConstant theMethodRefConstant = theInvoke.getMethodReference();
                BytecodeNameAndTypeConstant theMethodRef = theMethodRefConstant.getNameAndTypeIndex().getNameAndType();
                BytecodeMethodSignature theSig = theMethodRef.getDescriptorIndex().methodSignature();

                List<ASTValue> theArguments = new ArrayList<>();
                for (int i = 0; i < theSig.getArguments().length; i++) {
                    theArguments.add(aGenerationResult.popAndStoreToLocal());
                }

                Collections.reverse(theArguments);

                if (theSig.getReturnType() == BytecodePrimitiveTypeRef.VOID) {
                    aGenerationResult.getBlock().add(new ASTInvokeStatic(theArguments, theMethodRefConstant));
                } else {
                    aGenerationResult.push(new ASTInvokeStatic(theArguments, theMethodRefConstant));
                }
            } else if (theInstruction instanceof BytecodeInstructionINVOKESPECIAL) {

                BytecodeInstructionINVOKESPECIAL theInvoke = (BytecodeInstructionINVOKESPECIAL) theInstruction;

                BytecodeMethodRefConstant theMethodRefConstant = theInvoke.getMethodReference();
                BytecodeNameAndTypeConstant theMethodRef = theMethodRefConstant.getNameAndTypeIndex().getNameAndType();
                BytecodeMethodSignature theSig = theMethodRef.getDescriptorIndex().methodSignature();

                List<ASTValue> theArguments = new ArrayList<>();
                for (int i = 0; i < theSig.getArguments().length; i++) {
                    theArguments.add(aGenerationResult.popAndStoreToLocal());
                }

                Collections.reverse(theArguments);

                ASTValue theReference = aGenerationResult.popAndStoreToLocal();

                if (theSig.getReturnType() == BytecodePrimitiveTypeRef.VOID) {
                    aGenerationResult.getBlock().add(new ASTInvokeSpecial(theReference, theArguments, theMethodRefConstant));
                } else {
                    aGenerationResult.push(new ASTInvokeSpecial(theReference, theArguments, theMethodRefConstant));
                }

            } else if (theInstruction instanceof BytecodeInstructionINVOKEVIRTUAL) {

                BytecodeInstructionINVOKEVIRTUAL theInvoke = (BytecodeInstructionINVOKEVIRTUAL) theInstruction;

                BytecodeMethodRefConstant theMethodRefConstant = theInvoke.getMethodReference();
                BytecodeNameAndTypeConstant theMethodRef = theMethodRefConstant.getNameAndTypeIndex().getNameAndType();
                BytecodeMethodSignature theSig = theMethodRef.getDescriptorIndex().methodSignature();

                List<ASTValue> theArguments = new ArrayList<>();
                for (int i = 0; i < theSig.getArguments().length; i++) {
                    theArguments.add(aGenerationResult.popAndStoreToLocal());
                }

                Collections.reverse(theArguments);

                ASTValue theReference = aGenerationResult.popAndStoreToLocal();

                if (theSig.getReturnType() == BytecodePrimitiveTypeRef.VOID) {
                    aGenerationResult.getBlock().add(new ASTInvokeVirtual(theReference, theArguments, theMethodRefConstant));
                } else {
                    aGenerationResult.push(new ASTInvokeVirtual(theReference, theArguments, theMethodRefConstant));
                }

            } else if (theInstruction instanceof BytecodeInstructionINVOKEINTERFACE) {

                BytecodeInstructionINVOKEINTERFACE theInvoke = (BytecodeInstructionINVOKEINTERFACE) theInstruction;

                BytecodeInterfaceRefConstant theMethodRefConstant = theInvoke.getMethodDescriptor();

                BytecodeNameAndTypeConstant theMethodRef = theMethodRefConstant.getNameAndTypeIndex().getNameAndType();
                BytecodeMethodSignature theSig = theMethodRef.getDescriptorIndex().methodSignature();
                BytecodeUtf8Constant theName = theMethodRef.getNameIndex().getName();

                List<ASTValue> theArguments = new ArrayList<>();
                for (int i = 0; i < theSig.getArguments().length; i++) {
                    theArguments.add(aGenerationResult.popAndStoreToLocal());
                }

                Collections.reverse(theArguments);

                ASTValue theReference = aGenerationResult.popAndStoreToLocal();

                if (theSig.getReturnType() == BytecodePrimitiveTypeRef.VOID) {
                    aGenerationResult.getBlock().add(new ASTInvokeInterface(theReference, theArguments, theName.stringValue(), theSig));
                } else {
                    aGenerationResult.push(new ASTInvokeInterface(theReference, theArguments, theName.stringValue(), theSig));
                }

            } else if (theInstruction instanceof BytecodeInstructionNEW) {
                BytecodeInstructionNEW theNew = (BytecodeInstructionNEW) theInstruction;
                aGenerationResult.push(aGenerationResult.toNewLocalVariable(new ASTNewObject(theNew.getClassInfoForObjectToCreate())));
            } else if (theInstruction instanceof BytecodeInstructionPUTFIELD) {
                BytecodeInstructionPUTFIELD thePut = (BytecodeInstructionPUTFIELD) theInstruction;
                ASTValue theValue = aGenerationResult.popAndStoreToLocal();
                ASTValue theReference = aGenerationResult.popAndStoreToLocal();
                aGenerationResult.getBlock().add(new ASTPutField(theReference, thePut.getFieldRefConstant().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(), theValue));
            } else if (theInstruction instanceof BytecodeInstructionGETFIELD) {
                BytecodeInstructionGETFIELD thePut = (BytecodeInstructionGETFIELD) theInstruction;
                ASTValue theReference = aGenerationResult.popAndStoreToLocal();
                aGenerationResult.push(new ASTGetField(theReference, thePut.getFieldRefConstant().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue()));
            } else if (theInstruction instanceof BytecodeInstructionFCMP) {
                BytecodeInstructionFCMP theCMP = (BytecodeInstructionFCMP) theInstruction;
                ASTValue theValue2 = aGenerationResult.popAndStoreToLocal();
                ASTValue theValue1 = aGenerationResult.popAndStoreToLocal();
                aGenerationResult.push(new ASTFloatCompare(theValue1, theValue2, theCMP.getType()));
            } else if (theInstruction instanceof BytecodeInstructionIFCOND) {
                BytecodeInstructionIFCOND theIf = (BytecodeInstructionIFCOND) theInstruction;
                switch (theIf.getType()) {
                    case eq:
                        aGenerationResult.getBlock().add(new ASTIF(new ASTValuesWithOperator(aGenerationResult.popAndStoreToLocal(), ASTValuesWithOperator.Operator.EQUALS, new ASTIntegerValue(0)), theIf.getJumpOffset()));
                        break;
                    case ne:
                        aGenerationResult.getBlock().add(new ASTIF(new ASTValuesWithOperator(aGenerationResult.popAndStoreToLocal(), ASTValuesWithOperator.Operator.NOTEQUALS, new ASTIntegerValue(0)), theIf.getJumpOffset()));
                        break;
                    case lt:
                        aGenerationResult.getBlock().add(new ASTIF(new ASTValuesWithOperator(aGenerationResult.popAndStoreToLocal(), ASTValuesWithOperator.Operator.LESSTHAN, new ASTIntegerValue(0)), theIf.getJumpOffset()));
                        break;
                    case ge:
                        aGenerationResult.getBlock().add(new ASTIF(new ASTValuesWithOperator(aGenerationResult.popAndStoreToLocal(), ASTValuesWithOperator.Operator.GREATEREQUALS, new ASTIntegerValue(0)), theIf.getJumpOffset()));
                        break;
                    case gt:
                        aGenerationResult.getBlock().add(new ASTIF(new ASTValuesWithOperator(aGenerationResult.popAndStoreToLocal(), ASTValuesWithOperator.Operator.GREATERTHAN, new ASTIntegerValue(0)), theIf.getJumpOffset()));
                        break;
                    case le:
                        aGenerationResult.getBlock().add(new ASTIF(new ASTValuesWithOperator(aGenerationResult.popAndStoreToLocal(), ASTValuesWithOperator.Operator.LESSOREQUALS, new ASTIntegerValue(0)), theIf.getJumpOffset()));
                        break;
                }
            } else if (theInstruction instanceof BytecodeInstructionICONST) {
                BytecodeInstructionICONST theConst = (BytecodeInstructionICONST) theInstruction;
                aGenerationResult.push(new ASTIntegerValue(theConst.getIntConst()));
            } else if (theInstruction instanceof BytecodeInstructionFCONST) {
                BytecodeInstructionFCONST theConst = (BytecodeInstructionFCONST) theInstruction;
                aGenerationResult.push(new ASTFloatValue(theConst.getFloatValue()));
            } else if (theInstruction instanceof BytecodeInstructionGenericNEG) {
                BytecodeInstructionGenericNEG theNeg = (BytecodeInstructionGenericNEG) theInstruction;
                aGenerationResult.push(new ASTNeg(aGenerationResult.pop()));
            } else if (theInstruction instanceof BytecodeInstructionARRAYLENGTH) {
                BytecodeInstructionARRAYLENGTH theLength = (BytecodeInstructionARRAYLENGTH) theInstruction;
                aGenerationResult.push(new ASTArrayLength(aGenerationResult.pop()));
            } else if (theInstruction instanceof BytecodeInstructionI2Generic) {
                BytecodeInstructionI2Generic theConv = (BytecodeInstructionI2Generic) theInstruction;
                aGenerationResult.push(new ASTInteger2Generic(aGenerationResult.pop(), theConv.getTargetType()));
            } else if (theInstruction instanceof BytecodeInstructionNEWARRAY) {
                BytecodeInstructionNEWARRAY theNew = (BytecodeInstructionNEWARRAY) theInstruction;
                aGenerationResult.push(aGenerationResult.toNewLocalVariable(new ASTNewArray(theNew.getPrimitiveType(), aGenerationResult.popAndStoreToLocal())));
            } else if (theInstruction instanceof BytecodeInstructionANEWARRAY) {
                BytecodeInstructionANEWARRAY theNew = (BytecodeInstructionANEWARRAY) theInstruction;
                aGenerationResult.push(aGenerationResult.toNewLocalVariable(new ASTNewArray(BytecodeObjectTypeRef.fromUtf8Constant(theNew.getTypeConstant().getConstant()), aGenerationResult.popAndStoreToLocal())));
            } else if (theInstruction instanceof BytecodeInstructionIFNULL) {
                BytecodeInstructionIFNULL theIf = (BytecodeInstructionIFNULL) theInstruction;
                aGenerationResult.getBlock().add(new ASTIF(new ASTValuesWithOperator(aGenerationResult.popAndStoreToLocal(), ASTValuesWithOperator.Operator.EQUALS, new ASTNull()), theIf.getJumpTarget()));
            } else if (theInstruction instanceof BytecodeInstructionIFNONNULL) {
                BytecodeInstructionIFNONNULL theIf = (BytecodeInstructionIFNONNULL) theInstruction;
                aGenerationResult.getBlock().add(new ASTIF(new ASTValuesWithOperator(aGenerationResult.popAndStoreToLocal(), ASTValuesWithOperator.Operator.NOTEQUALS, new ASTNull()), theIf.getJumpTarget()));
            } else if (theInstruction instanceof BytecodeInstructionGOTO) {
                BytecodeInstructionGOTO theGoto = (BytecodeInstructionGOTO) theInstruction;
                aGenerationResult.getBlock().add(new ASTGoto(theGoto.getJumpAddress()));
            } else if (theInstruction instanceof BytecodeInstructionGenericLOAD) {
                BytecodeInstructionGenericLOAD theLoad = (BytecodeInstructionGenericLOAD) theInstruction;
                aGenerationResult.push(aGenerationResult.toNewLocalVariable(new ASTLocalVariable(theLoad.getLocalVariableIndex())));
            } else if (theInstruction instanceof BytecodeInstructionGenericSTORE) {
                BytecodeInstructionGenericSTORE theStore = (BytecodeInstructionGenericSTORE) theInstruction;
                aGenerationResult.getBlock().add(new ASTSetLocalVariable(theStore.getVariableIndex(), aGenerationResult.popAndStoreToLocal()));
            } else if (theInstruction instanceof BytecodeInstructionIFICMP) {
                BytecodeInstructionIFICMP theIf = (BytecodeInstructionIFICMP) theInstruction;

                ASTValue theValue2 = aGenerationResult.popAndStoreToLocal();
                ASTValue theValue1 = aGenerationResult.popAndStoreToLocal();

                switch (theIf.getType()) {
                    case lt:
                        aGenerationResult.getBlock().add(new ASTIF(new ASTValuesWithOperator(theValue2, ASTValuesWithOperator.Operator.LESSTHAN, theValue1), theIf.getJumpAddress()));
                        break;
                    case eq:
                        aGenerationResult.getBlock().add(new ASTIF(new ASTValuesWithOperator(theValue2, ASTValuesWithOperator.Operator.EQUALS, theValue1), theIf.getJumpAddress()));
                        break;
                    case ge:
                        aGenerationResult.getBlock().add(new ASTIF(new ASTValuesWithOperator(theValue2, ASTValuesWithOperator.Operator.GREATEREQUALS, theValue1), theIf.getJumpAddress()));
                        break;
                    case gt:
                        aGenerationResult.getBlock().add(new ASTIF(new ASTValuesWithOperator(theValue2, ASTValuesWithOperator.Operator.GREATERTHAN, theValue1), theIf.getJumpAddress()));
                        break;
                    case le:
                        aGenerationResult.getBlock().add(new ASTIF(new ASTValuesWithOperator(theValue2, ASTValuesWithOperator.Operator.LESSOREQUALS, theValue1), theIf.getJumpAddress()));
                        break;
                    case ne:
                        aGenerationResult.getBlock().add(new ASTIF(new ASTValuesWithOperator(theValue2, ASTValuesWithOperator.Operator.NOTEQUALS, theValue1), theIf.getJumpAddress()));
                        break;
                }
            } else if (theInstruction instanceof BytecodeInstructionIINC) {
                BytecodeInstructionIINC theInc = (BytecodeInstructionIINC) theInstruction;
                aGenerationResult.getBlock().add(new ASTSetLocalVariable(theInc.getIndex(),
                        new ASTValuesWithOperator(new ASTLocalVariable(theInc.getIndex()), ASTValuesWithOperator.Operator.ADD, new ASTIntegerValue(theInc.getConstant()))));
            } else if (theInstruction instanceof BytecodeInstructionALOAD) {
                BytecodeInstructionALOAD theLoad = (BytecodeInstructionALOAD) theInstruction;
                aGenerationResult.push(aGenerationResult.toNewLocalVariable(new ASTLocalVariable(theLoad.getVariableIndex())));
            } else if (theInstruction instanceof BytecodeInstructionGenericALOAD) {
                BytecodeInstructionGenericALOAD theLoad = (BytecodeInstructionGenericALOAD) theInstruction;
                aGenerationResult.push(new ASTArrayValue(aGenerationResult.popAndStoreToLocal(), aGenerationResult.popAndStoreToLocal()));
            } else if (theInstruction instanceof BytecodeInstructionAALOAD) {
                BytecodeInstructionAALOAD theLoad = (BytecodeInstructionAALOAD) theInstruction;
                aGenerationResult.push(new ASTArrayValue(aGenerationResult.popAndStoreToLocal(), aGenerationResult.popAndStoreToLocal()));
            } else if (theInstruction instanceof BytecodeInstructionGenericASTORE) {
                BytecodeInstructionGenericASTORE theStore = (BytecodeInstructionGenericASTORE) theInstruction;
                aGenerationResult.getBlock().add(new ASTSetArrayValue(aGenerationResult.popAndStoreToLocal(), aGenerationResult.popAndStoreToLocal(), aGenerationResult.popAndStoreToLocal()));
            } else if (theInstruction instanceof BytecodeInstructionAASTORE) {
                BytecodeInstructionAASTORE theStore = (BytecodeInstructionAASTORE) theInstruction;
                aGenerationResult.getBlock().add(new ASTSetArrayValue(aGenerationResult.popAndStoreToLocal(), aGenerationResult.popAndStoreToLocal(), aGenerationResult.popAndStoreToLocal()));
            } else if (theInstruction instanceof BytecodeInstructionCHECKCAST) {
                BytecodeInstructionCHECKCAST theCast = (BytecodeInstructionCHECKCAST) theInstruction;
                aGenerationResult.push(new ASTCheckCast(theCast.getTypeCheck(), aGenerationResult.pop()));
            } else if (theInstruction instanceof BytecodeInstructionF2Generic) {
                BytecodeInstructionF2Generic theConv = (BytecodeInstructionF2Generic) theInstruction;
                aGenerationResult.push(new ASTFloat2Generic(aGenerationResult.popAndStoreToLocal(), theConv.getTargetType()));
            } else if (theInstruction instanceof BytecodeInstructionL2Generic) {
                BytecodeInstructionL2Generic theConv = (BytecodeInstructionL2Generic) theInstruction;
                aGenerationResult.push(new ASTLong2Generic(aGenerationResult.popAndStoreToLocal(), theConv.getTargetType()));
            } else if (theInstruction instanceof BytecodeInstructionGenericREM) {
                BytecodeInstructionGenericREM theRem = (BytecodeInstructionGenericREM) theInstruction;
                aGenerationResult.push(new ASTValuesWithOperator(aGenerationResult.popAndStoreToLocal(), ASTValuesWithOperator.Operator.REMAINDER, aGenerationResult.popAndStoreToLocal()));
            } else if (theInstruction instanceof BytecodeInstructionD2Generic) {
                BytecodeInstructionD2Generic theConv = (BytecodeInstructionD2Generic) theInstruction;
                aGenerationResult.push(new ASTDouble2Generic(aGenerationResult.popAndStoreToLocal(), theConv.getTargetType()));
            } else if (theInstruction instanceof BytecodeInstructionSIPUSH) {
                BytecodeInstructionSIPUSH thePush = (BytecodeInstructionSIPUSH) theInstruction;
                aGenerationResult.push(new ASTShortValue(thePush.getShortValue()));
            } else if (theInstruction instanceof BytecodeInstructionIFACMP) {
                BytecodeInstructionIFACMP theIf = (BytecodeInstructionIFACMP) theInstruction;
                ASTValue theValue2 = aGenerationResult.popAndStoreToLocal();
                ASTValue theValue1 = aGenerationResult.popAndStoreToLocal();
                switch (theIf.getType()) {
                    case eq:
                        aGenerationResult.getBlock().add(new ASTIF(new ASTValuesWithOperator(theValue2, ASTValuesWithOperator.Operator.EQUALS, theValue1), theIf.getJumpAddress()));
                        break;
                    case ne:
                        aGenerationResult.getBlock().add(new ASTIF(new ASTValuesWithOperator(theValue2, ASTValuesWithOperator.Operator.NOTEQUALS, theValue1), theIf.getJumpAddress()));
                        break;
                }
            } else if (theInstruction instanceof BytecodeInstructionGenericSHL) {
                ASTValue theValue2 = aGenerationResult.popAndStoreToLocal();
                ASTValue theValue1 = aGenerationResult.popAndStoreToLocal();
                aGenerationResult.push(new ASTShift(theValue1, ASTShift.Direction.left, theValue2));
            } else if (theInstruction instanceof BytecodeInstructionGenericSHR) {
                ASTValue theValue2 = aGenerationResult.popAndStoreToLocal();
                ASTValue theValue1 = aGenerationResult.popAndStoreToLocal();
                aGenerationResult.push(new ASTShift(theValue1, ASTShift.Direction.right, theValue2));
            } else if (theInstruction instanceof BytecodeInstructionSIPUSH) {
                BytecodeInstructionSIPUSH thePush = (BytecodeInstructionSIPUSH) theInstruction;
                aGenerationResult.push(new ASTShortValue(thePush.getShortValue()));
            } else {
                throw new IllegalStateException("Not implemented : " + theInstruction);
            }
        }

        return aGenerationResult;
    }
}