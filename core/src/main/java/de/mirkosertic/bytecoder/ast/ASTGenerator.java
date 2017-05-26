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
        private final Stack<ASTValue> stackRemainder;

        public GenerationResult(ASTGenerator aGenerator, ASTBlock aBlock, Stack<ASTValue> aStackRemainder) {
            generator = aGenerator;
            block = aBlock;
            stackRemainder = aStackRemainder;
        }

        public ASTBlock getBlock() {
            return block;
        }

        public GenerationResult continueWith(BytecodeBasicBlock aBasicBlock) {
            return generator.generateFrom(aBasicBlock, stackRemainder);
        }
    }

    public GenerationResult generateFrom(BytecodeBasicBlock aBasicBlock) {
        Stack<ASTValue> theCurrentValueStack = new Stack<>();
        theCurrentValueStack.push(new ASTGetStatic(BytecodeObjectTypeRef.fromRuntimeClass(ExceptionRethrower.class), "lastMethodOutcome"));

        return generateFrom(aBasicBlock, theCurrentValueStack);
    }

    private GenerationResult generateFrom(BytecodeBasicBlock aBasicBlock, Stack<ASTValue> aStartingStack) {

        ASTBlock theResult = new ASTBlock();
        Stack<ASTValue> theCurrentValueStack = aStartingStack;

        for (BytecodeInstruction theInstruction : aBasicBlock.getInstructions()) {
            if (theInstruction instanceof BytecodeInstructionBIPUSH) {
                BytecodeInstructionBIPUSH thePush = (BytecodeInstructionBIPUSH) theInstruction;
                theCurrentValueStack.push(new ASTByteValue(thePush.getByteValue()));
            } else if (theInstruction instanceof BytecodeInstructionINSTANCEOF) {
                BytecodeInstructionINSTANCEOF theOf = (BytecodeInstructionINSTANCEOF) theInstruction;
                ASTValue theReference = theResult.resolveToLocalVariable(theCurrentValueStack.pop());
                theCurrentValueStack.push(new ASTInstanceOf(theReference, theOf.getTypeRef()));
            } else if (theInstruction instanceof BytecodeInstructionPOP) {
                theCurrentValueStack.pop();
            } else if (theInstruction instanceof BytecodeInstructionRETURN) {
                theResult.add(new ASTReturn());
            } else if (theInstruction instanceof BytecodeInstructionGenericLDC) {
                BytecodeInstructionGenericLDC theLoad = (BytecodeInstructionGenericLDC) theInstruction;
                theCurrentValueStack.push(new ASTConstant(theLoad.constant()));
            } else if (theInstruction instanceof BytecodeInstructionGenericLOAD) {
                BytecodeInstructionGenericLOAD theLoad = (BytecodeInstructionGenericLOAD) theInstruction;
                theCurrentValueStack.push(theResult.cloneToLocalVariable(new ASTLocalVariable(theLoad.getLocalVariableIndex())));
            } else if (theInstruction instanceof BytecodeInstructionACONSTNULL) {
                theCurrentValueStack.push(new ASTNull());
            } else if (theInstruction instanceof BytecodeInstructionARETURN) {
                theResult.add(new ASTObjectReturn(theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionGenericRETURN) {
                theResult.add(new ASTObjectReturn(theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionALOAD) {
                BytecodeInstructionALOAD theLoad = (BytecodeInstructionALOAD) theInstruction;
                theCurrentValueStack.push(theResult.cloneToLocalVariable(new ASTLocalVariable(theLoad.getVariableIndex())));
            } else if (theInstruction instanceof BytecodeInstructionASTORE) {
                BytecodeInstructionASTORE theStore = (BytecodeInstructionASTORE) theInstruction;
                theResult.add(new ASTSetLocalVariable(theStore.getVariableIndex(), theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionPUTSTATIC) {
                BytecodeInstructionPUTSTATIC thePut = (BytecodeInstructionPUTSTATIC) theInstruction;
                theResult.add(new ASTClassInitCheck(BytecodeObjectTypeRef.fromUtf8Constant(thePut.getConstant().getClassIndex().getClassConstant().getConstant())));
                theResult.add(new ASTPutStatic(theCurrentValueStack.pop(), thePut.getConstant()));
            } else if (theInstruction instanceof BytecodeInstructionGETSTATIC) {
                BytecodeInstructionGETSTATIC theGet = (BytecodeInstructionGETSTATIC) theInstruction;
                BytecodeObjectTypeRef theObjectType = BytecodeObjectTypeRef.fromUtf8Constant(theGet.getConstant().getClassIndex().getClassConstant().getConstant());
                theResult.add(new ASTClassInitCheck(theObjectType));
                String theFieldName = theGet.getConstant().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue();
                theCurrentValueStack.push(new ASTGetStatic(theObjectType, theFieldName));
            } else if (theInstruction instanceof BytecodeInstructionATHROW) {
                BytecodeInstructionATHROW theThrow = (BytecodeInstructionATHROW) theInstruction;
                theResult.add(new ASTThrow(theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionGenericADD) {
                BytecodeInstructionGenericADD theAdd = (BytecodeInstructionGenericADD) theInstruction;
                theCurrentValueStack.push(new ASTValuesWithOperator(theCurrentValueStack.pop(), ASTValuesWithOperator.Operator.ADD, theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionGenericSUB) {
                BytecodeInstructionGenericSUB theSub = (BytecodeInstructionGenericSUB) theInstruction;
                theCurrentValueStack.push(new ASTValuesWithOperator(theCurrentValueStack.pop(), ASTValuesWithOperator.Operator.SUBSTRACT, theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionGenericMUL) {
                BytecodeInstructionGenericMUL theMul = (BytecodeInstructionGenericMUL) theInstruction;
                theCurrentValueStack.push(new ASTValuesWithOperator(theCurrentValueStack.pop(), ASTValuesWithOperator.Operator.MULTIPLY, theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionGenericDIV) {
                BytecodeInstructionGenericDIV theDiv = (BytecodeInstructionGenericDIV) theInstruction;
                theCurrentValueStack.push(new ASTValuesWithOperator(theCurrentValueStack.pop(), ASTValuesWithOperator.Operator.DIVIDE, theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionGenericOR) {
                BytecodeInstructionGenericOR theOr = (BytecodeInstructionGenericOR) theInstruction;
                theCurrentValueStack.push(new ASTValuesWithOperator(theCurrentValueStack.pop(), ASTValuesWithOperator.Operator.BINARYOR, theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionGenericAND) {
                BytecodeInstructionGenericAND theAnd = (BytecodeInstructionGenericAND) theInstruction;
                theCurrentValueStack.push(new ASTValuesWithOperator(theCurrentValueStack.pop(), ASTValuesWithOperator.Operator.BINARYAND, theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionGenericXOR) {
                BytecodeInstructionGenericXOR theXor = (BytecodeInstructionGenericXOR) theInstruction;
                theCurrentValueStack.push(new ASTValuesWithOperator(theCurrentValueStack.pop(), ASTValuesWithOperator.Operator.BINARYXOR, theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionDUP) {
                BytecodeInstructionDUP theDup = (BytecodeInstructionDUP) theInstruction;
                theCurrentValueStack.push(theResult.cloneToLocalVariable(theCurrentValueStack.peek()));
            } else if (theInstruction instanceof BytecodeInstructionINVOKESTATIC) {

                BytecodeInstructionINVOKESTATIC theInvoke = (BytecodeInstructionINVOKESTATIC) theInstruction;

                BytecodeMethodRefConstant theMethodRefConstant = theInvoke.getMethodReference();
                BytecodeNameAndTypeConstant theMethodRef = theMethodRefConstant.getNameAndTypeIndex().getNameAndType();
                BytecodeMethodSignature theSig = theMethodRef.getDescriptorIndex().methodSignature();

                List<ASTValue> theArguments = new ArrayList<>();
                for (int i = 0; i < theSig.getArguments().length; i++) {
                    theArguments.add(theCurrentValueStack.pop());
                }

                Collections.reverse(theArguments);

                if (theSig.getReturnType() == BytecodePrimitiveTypeRef.VOID) {
                    theResult.add(new ASTInvokeStatic(theArguments, theMethodRefConstant));
                } else {
                    theCurrentValueStack.push(new ASTInvokeStatic(theArguments, theMethodRefConstant));
                }
            } else if (theInstruction instanceof BytecodeInstructionINVOKESPECIAL) {

                BytecodeInstructionINVOKESPECIAL theInvoke = (BytecodeInstructionINVOKESPECIAL) theInstruction;

                BytecodeMethodRefConstant theMethodRefConstant = theInvoke.getMethodReference();
                BytecodeNameAndTypeConstant theMethodRef = theMethodRefConstant.getNameAndTypeIndex().getNameAndType();
                BytecodeMethodSignature theSig = theMethodRef.getDescriptorIndex().methodSignature();

                List<ASTValue> theArguments = new ArrayList<>();
                for (int i = 0; i < theSig.getArguments().length; i++) {
                    theArguments.add(theCurrentValueStack.pop());
                }

                Collections.reverse(theArguments);

                ASTValue theReference = theCurrentValueStack.pop();

                if (theSig.getReturnType() == BytecodePrimitiveTypeRef.VOID) {
                    theResult.add(new ASTInvokeSpecial(theReference, theArguments, theMethodRefConstant));
                } else {
                    theCurrentValueStack.push(new ASTInvokeSpecial(theReference, theArguments, theMethodRefConstant));
                }

            } else if (theInstruction instanceof BytecodeInstructionINVOKEVIRTUAL) {

                BytecodeInstructionINVOKEVIRTUAL theInvoke = (BytecodeInstructionINVOKEVIRTUAL) theInstruction;

                BytecodeMethodRefConstant theMethodRefConstant = theInvoke.getMethodReference();
                BytecodeNameAndTypeConstant theMethodRef = theMethodRefConstant.getNameAndTypeIndex().getNameAndType();
                BytecodeMethodSignature theSig = theMethodRef.getDescriptorIndex().methodSignature();

                List<ASTValue> theArguments = new ArrayList<>();
                for (int i = 0; i < theSig.getArguments().length; i++) {
                    theArguments.add(theCurrentValueStack.pop());
                }

                Collections.reverse(theArguments);

                ASTValue theReference = theResult.resolveToLocalVariable(theCurrentValueStack.pop());

                if (theSig.getReturnType() == BytecodePrimitiveTypeRef.VOID) {
                    theResult.add(new ASTInvokeVirtual(theReference, theArguments, theMethodRefConstant));
                } else {
                    theCurrentValueStack.push(new ASTInvokeVirtual(theReference, theArguments, theMethodRefConstant));
                }

            } else if (theInstruction instanceof BytecodeInstructionINVOKEINTERFACE) {

                BytecodeInstructionINVOKEINTERFACE theInvoke = (BytecodeInstructionINVOKEINTERFACE) theInstruction;

                BytecodeInterfaceRefConstant theMethodRefConstant = theInvoke.getMethodDescriptor();

                BytecodeNameAndTypeConstant theMethodRef = theMethodRefConstant.getNameAndTypeIndex().getNameAndType();
                BytecodeMethodSignature theSig = theMethodRef.getDescriptorIndex().methodSignature();
                BytecodeUtf8Constant theName = theMethodRef.getNameIndex().getName();

                List<ASTValue> theArguments = new ArrayList<>();
                for (int i = 0; i < theSig.getArguments().length; i++) {
                    theArguments.add(theCurrentValueStack.pop());
                }

                Collections.reverse(theArguments);

                ASTValue theReference = theResult.resolveToLocalVariable(theCurrentValueStack.pop());

                if (theSig.getReturnType() == BytecodePrimitiveTypeRef.VOID) {
                    theResult.add(new ASTInvokeInterface(theReference, theArguments, theName.stringValue(), theSig));
                } else {
                    theCurrentValueStack.push(new ASTInvokeInterface(theReference, theArguments, theName.stringValue(), theSig));
                }

            } else if (theInstruction instanceof BytecodeInstructionNEW) {
                BytecodeInstructionNEW theNew = (BytecodeInstructionNEW) theInstruction;
                theCurrentValueStack.push(theResult.resolveToLocalVariable(new ASTNewObject(theNew.getClassInfoForObjectToCreate())));
            } else if (theInstruction instanceof BytecodeInstructionPUTFIELD) {
                BytecodeInstructionPUTFIELD thePut = (BytecodeInstructionPUTFIELD) theInstruction;
                ASTValue theValue = theCurrentValueStack.pop();
                ASTValue theReference = theCurrentValueStack.pop();
                theResult.add(new ASTPutField(theReference, thePut.getFieldRefConstant().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(), theValue));
            } else if (theInstruction instanceof BytecodeInstructionGETFIELD) {
                BytecodeInstructionGETFIELD thePut = (BytecodeInstructionGETFIELD) theInstruction;
                ASTValue theReference = theCurrentValueStack.pop();
                theCurrentValueStack.push(new ASTGetField(theReference, thePut.getFieldRefConstant().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue()));
            } else if (theInstruction instanceof BytecodeInstructionFCMP) {
                BytecodeInstructionFCMP theCMP = (BytecodeInstructionFCMP) theInstruction;
                ASTValue theValue2 = theCurrentValueStack.pop();
                ASTValue theValue1 = theCurrentValueStack.pop();
                theCurrentValueStack.push(new ASTFloatCompare(theValue1, theValue2, theCMP.getType()));
            } else if (theInstruction instanceof BytecodeInstructionIFCOND) {
                BytecodeInstructionIFCOND theIf = (BytecodeInstructionIFCOND) theInstruction;
                switch (theIf.getType()) {
                    case eq:
                        theResult.add(new ASTIF(new ASTValuesWithOperator(theCurrentValueStack.pop(), ASTValuesWithOperator.Operator.EQUALS, new ASTIntegerValue(0)), theIf.getJumpOffset()));
                        break;
                    case ne:
                        theResult.add(new ASTIF(new ASTValuesWithOperator(theCurrentValueStack.pop(), ASTValuesWithOperator.Operator.NOTEQUALS, new ASTIntegerValue(0)), theIf.getJumpOffset()));
                        break;
                    case lt:
                        theResult.add(new ASTIF(new ASTValuesWithOperator(theCurrentValueStack.pop(), ASTValuesWithOperator.Operator.LESSTHAN, new ASTIntegerValue(0)), theIf.getJumpOffset()));
                        break;
                    case ge:
                        theResult.add(new ASTIF(new ASTValuesWithOperator(theCurrentValueStack.pop(), ASTValuesWithOperator.Operator.GREATEREQUALS, new ASTIntegerValue(0)), theIf.getJumpOffset()));
                        break;
                    case gt:
                        theResult.add(new ASTIF(new ASTValuesWithOperator(theCurrentValueStack.pop(), ASTValuesWithOperator.Operator.GREATERTHAN, new ASTIntegerValue(0)), theIf.getJumpOffset()));
                        break;
                    case le:
                        theResult.add(new ASTIF(new ASTValuesWithOperator(theCurrentValueStack.pop(), ASTValuesWithOperator.Operator.LESSOREQUALS, new ASTIntegerValue(0)), theIf.getJumpOffset()));
                        break;
                }
            } else if (theInstruction instanceof BytecodeInstructionICONST) {
                BytecodeInstructionICONST theConst = (BytecodeInstructionICONST) theInstruction;
                theCurrentValueStack.push(new ASTIntegerValue(theConst.getIntConst()));
            } else if (theInstruction instanceof BytecodeInstructionFCONST) {
                BytecodeInstructionFCONST theConst = (BytecodeInstructionFCONST) theInstruction;
                theCurrentValueStack.push(new ASTFloatValue(theConst.getFloatValue()));
            } else if (theInstruction instanceof BytecodeInstructionGenericNEG) {
                BytecodeInstructionGenericNEG theNeg = (BytecodeInstructionGenericNEG) theInstruction;
                theCurrentValueStack.push(new ASTNeg(theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionARRAYLENGTH) {
                BytecodeInstructionARRAYLENGTH theLength = (BytecodeInstructionARRAYLENGTH) theInstruction;
                theCurrentValueStack.push(new ASTArrayLength(theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionI2Generic) {
                BytecodeInstructionI2Generic theConv = (BytecodeInstructionI2Generic) theInstruction;
                theCurrentValueStack.push(new ASTInteger2Generic(theCurrentValueStack.pop(), theConv.getTargetType()));
            } else if (theInstruction instanceof BytecodeInstructionNEWARRAY) {
                BytecodeInstructionNEWARRAY theNew = (BytecodeInstructionNEWARRAY) theInstruction;
                theCurrentValueStack.push(theResult.resolveToLocalVariable(new ASTNewArray(theNew.getPrimitiveType(), theCurrentValueStack.pop())));
            } else if (theInstruction instanceof BytecodeInstructionANEWARRAY) {
                BytecodeInstructionANEWARRAY theNew = (BytecodeInstructionANEWARRAY) theInstruction;
                theCurrentValueStack.push(theResult.resolveToLocalVariable(new ASTNewArray(BytecodeObjectTypeRef.fromUtf8Constant(theNew.getTypeConstant().getConstant()), theCurrentValueStack.pop())));
            } else if (theInstruction instanceof BytecodeInstructionIFNULL) {
                BytecodeInstructionIFNULL theIf = (BytecodeInstructionIFNULL) theInstruction;
                theResult.add(new ASTIF(new ASTValuesWithOperator(theCurrentValueStack.pop(), ASTValuesWithOperator.Operator.EQUALS, new ASTNull()), theIf.getJumpTarget()));
            } else if (theInstruction instanceof BytecodeInstructionIFNONNULL) {
                BytecodeInstructionIFNONNULL theIf = (BytecodeInstructionIFNONNULL) theInstruction;
                theResult.add(new ASTIF(new ASTValuesWithOperator(theCurrentValueStack.pop(), ASTValuesWithOperator.Operator.NOTEQUALS, new ASTNull()), theIf.getJumpTarget()));
            } else if (theInstruction instanceof BytecodeInstructionGOTO) {
                BytecodeInstructionGOTO theGoto = (BytecodeInstructionGOTO) theInstruction;
                theResult.add(new ASTGoto(theGoto.getJumpAddress()));
            } else if (theInstruction instanceof BytecodeInstructionGenericLOAD) {
                BytecodeInstructionGenericLOAD theLoad = (BytecodeInstructionGenericLOAD) theInstruction;
                theCurrentValueStack.push(theResult.cloneToLocalVariable(new ASTLocalVariable(theLoad.getLocalVariableIndex())));
            } else if (theInstruction instanceof BytecodeInstructionGenericSTORE) {
                BytecodeInstructionGenericSTORE theStore = (BytecodeInstructionGenericSTORE) theInstruction;
                theResult.add(new ASTSetLocalVariable(theStore.getVariableIndex(), theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionIFICMP) {
                BytecodeInstructionIFICMP theIf = (BytecodeInstructionIFICMP) theInstruction;

                ASTValue theValue2 = theResult.resolveToLocalVariable(theCurrentValueStack.pop());
                ASTValue theValue1 = theResult.resolveToLocalVariable(theCurrentValueStack.pop());

                switch (theIf.getType()) {
                    case lt:
                        theResult.add(new ASTIF(new ASTValuesWithOperator(theValue2, ASTValuesWithOperator.Operator.LESSTHAN, theValue1), theIf.getJumpAddress()));
                        break;
                    case eq:
                        theResult.add(new ASTIF(new ASTValuesWithOperator(theValue2, ASTValuesWithOperator.Operator.EQUALS, theValue1), theIf.getJumpAddress()));
                        break;
                    case ge:
                        theResult.add(new ASTIF(new ASTValuesWithOperator(theValue2, ASTValuesWithOperator.Operator.GREATEREQUALS, theValue1), theIf.getJumpAddress()));
                        break;
                    case gt:
                        theResult.add(new ASTIF(new ASTValuesWithOperator(theValue2, ASTValuesWithOperator.Operator.GREATERTHAN, theValue1), theIf.getJumpAddress()));
                        break;
                    case le:
                        theResult.add(new ASTIF(new ASTValuesWithOperator(theValue2, ASTValuesWithOperator.Operator.LESSOREQUALS, theValue1), theIf.getJumpAddress()));
                        break;
                    case ne:
                        theResult.add(new ASTIF(new ASTValuesWithOperator(theValue2, ASTValuesWithOperator.Operator.NOTEQUALS, theValue1), theIf.getJumpAddress()));
                        break;
                }
            } else if (theInstruction instanceof BytecodeInstructionIINC) {
                BytecodeInstructionIINC theInc = (BytecodeInstructionIINC) theInstruction;
                theResult.add(new ASTSetLocalVariable(theInc.getIndex(),
                        new ASTValuesWithOperator(new ASTLocalVariable(theInc.getIndex()), ASTValuesWithOperator.Operator.ADD, new ASTIntegerValue(theInc.getConstant()))));
            } else if (theInstruction instanceof BytecodeInstructionALOAD) {
                BytecodeInstructionALOAD theLoad = (BytecodeInstructionALOAD) theInstruction;
                theCurrentValueStack.push(new ASTLocalVariable(theLoad.getVariableIndex()));
            } else if (theInstruction instanceof BytecodeInstructionGenericALOAD) {
                BytecodeInstructionGenericALOAD theLoad = (BytecodeInstructionGenericALOAD) theInstruction;
                theCurrentValueStack.push(new ASTArrayValue(theCurrentValueStack.pop(), theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionAALOAD) {
                BytecodeInstructionAALOAD theLoad = (BytecodeInstructionAALOAD) theInstruction;
                theCurrentValueStack.push(new ASTArrayValue(theCurrentValueStack.pop(), theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionGenericASTORE) {
                BytecodeInstructionGenericASTORE theStore = (BytecodeInstructionGenericASTORE) theInstruction;
                theResult.add(new ASTSetArrayValue(theCurrentValueStack.pop(), theCurrentValueStack.pop(), theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionAASTORE) {
                BytecodeInstructionAASTORE theStore = (BytecodeInstructionAASTORE) theInstruction;
                theResult.add(new ASTSetArrayValue(theCurrentValueStack.pop(), theCurrentValueStack.pop(), theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionCHECKCAST) {
                BytecodeInstructionCHECKCAST theCast = (BytecodeInstructionCHECKCAST) theInstruction;
                theCurrentValueStack.push(new ASTCheckCast(theCast.getTypeCheck(), theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionF2Generic) {
                BytecodeInstructionF2Generic theConv = (BytecodeInstructionF2Generic) theInstruction;
                theCurrentValueStack.push(new ASTFloat2Generic(theCurrentValueStack.pop(), theConv.getTargetType()));
            } else if (theInstruction instanceof BytecodeInstructionL2Generic) {
                BytecodeInstructionL2Generic theConv = (BytecodeInstructionL2Generic) theInstruction;
                theCurrentValueStack.push(new ASTLong2Generic(theCurrentValueStack.pop(), theConv.getTargetType()));
            } else if (theInstruction instanceof BytecodeInstructionGenericREM) {
                BytecodeInstructionGenericREM theRem = (BytecodeInstructionGenericREM) theInstruction;
                theCurrentValueStack.push(new ASTValuesWithOperator(theCurrentValueStack.pop(), ASTValuesWithOperator.Operator.REMAINDER, theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionD2Generic) {
                BytecodeInstructionD2Generic theConv = (BytecodeInstructionD2Generic) theInstruction;
                theCurrentValueStack.push(new ASTDouble2Generic(theCurrentValueStack.pop(), theConv.getTargetType()));
            } else if (theInstruction instanceof BytecodeInstructionSIPUSH) {
                BytecodeInstructionSIPUSH thePush = (BytecodeInstructionSIPUSH) theInstruction;
                theCurrentValueStack.push(new ASTShortValue(thePush.getShortValue()));
            } else if (theInstruction instanceof BytecodeInstructionIFACMP) {
                BytecodeInstructionIFACMP theIf = (BytecodeInstructionIFACMP) theInstruction;
                ASTValue theValue2 = theResult.resolveToLocalVariable(theCurrentValueStack.pop());
                ASTValue theValue1 = theResult.resolveToLocalVariable(theCurrentValueStack.pop());
                switch (theIf.getType()) {
                    case eq:
                        theResult.add(new ASTIF(new ASTValuesWithOperator(theValue2, ASTValuesWithOperator.Operator.EQUALS, theValue1), theIf.getJumpAddress()));
                        break;
                    case ne:
                        theResult.add(new ASTIF(new ASTValuesWithOperator(theValue2, ASTValuesWithOperator.Operator.NOTEQUALS, theValue1), theIf.getJumpAddress()));
                        break;
                }
            } else if (theInstruction instanceof BytecodeInstructionGenericSHL) {
                ASTValue theValue2 = theResult.resolveToLocalVariable(theCurrentValueStack.pop());
                ASTValue theValue1 = theResult.resolveToLocalVariable(theCurrentValueStack.pop());
                theCurrentValueStack.push(new ASTShift(theValue1, ASTShift.Direction.left, theValue2));
            } else if (theInstruction instanceof BytecodeInstructionGenericSHR) {
                ASTValue theValue2 = theResult.resolveToLocalVariable(theCurrentValueStack.pop());
                ASTValue theValue1 = theResult.resolveToLocalVariable(theCurrentValueStack.pop());
                theCurrentValueStack.push(new ASTShift(theValue1, ASTShift.Direction.right, theValue2));
            } else if (theInstruction instanceof BytecodeInstructionSIPUSH) {
                BytecodeInstructionSIPUSH thePush = (BytecodeInstructionSIPUSH) theInstruction;
                theCurrentValueStack.push(new ASTShortValue(thePush.getShortValue()));
            } else {
                throw new IllegalStateException("Not implemented : " + theInstruction);
            }
        }

        return new GenerationResult(this, theResult, theCurrentValueStack);
    }
}