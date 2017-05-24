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
import java.util.List;
import java.util.Stack;

import de.mirkosertic.bytecoder.classlib.ExceptionRethrower;
import de.mirkosertic.bytecoder.core.*;

public class ASTGenerator {

    public ASTBlock generateFrom(BytecodeBasicBlock aBasicBlock) {

        int localVariableCounter = 100;

        ASTBlock theResult = new ASTBlock();
        Stack<ASTValue> theCurrentValueStack = new Stack<>();

        theCurrentValueStack.push(new ASTGetStatic(BytecodeObjectTypeRef.fromRuntimeClass(ExceptionRethrower.class), "lastMethodOutcome"));

        for (BytecodeInstruction theInstruction : aBasicBlock.getInstructions()) {
            if (theInstruction instanceof BytecodeInstructionBIPUSH) {
                BytecodeInstructionBIPUSH thePush = (BytecodeInstructionBIPUSH) theInstruction;
                theCurrentValueStack.push(new ASTByteValue(thePush.getByteValue()));
            } else if (theInstruction instanceof BytecodeInstructionINSTANCEOF) {
                BytecodeInstructionINSTANCEOF theOf = (BytecodeInstructionINSTANCEOF) theInstruction;

                ASTValue theReference = theCurrentValueStack.pop();
                if (!(theReference instanceof ASTLocalVariable)) {
                    int theCallSite = localVariableCounter++;
                    theResult.add(new ASTSetLocalVariable(theCallSite, theReference));
                    theReference = new ASTLocalVariable(theCallSite);
                }

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
                theCurrentValueStack.push(new ASTLocalVariable(theLoad.getLocalVariableIndex()));
            } else if (theInstruction instanceof BytecodeInstructionACONSTNULL) {
                theCurrentValueStack.push(new ASTNull());
            } else if (theInstruction instanceof BytecodeInstructionARETURN) {
                theResult.add(new ASTObjectReturn(theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionGenericRETURN) {
                theResult.add(new ASTObjectReturn(theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionALOAD) {
                BytecodeInstructionALOAD theLoad = (BytecodeInstructionALOAD) theInstruction;
                theCurrentValueStack.push(new ASTLocalVariable(theLoad.getVariableIndex()));
            } else if (theInstruction instanceof BytecodeInstructionASTORE) {
                BytecodeInstructionASTORE theStore = (BytecodeInstructionASTORE) theInstruction;
                theResult.add(new ASTSetLocalVariable(theStore.getVariableIndex(), theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionPUTSTATIC) {
                BytecodeInstructionPUTSTATIC thePut = (BytecodeInstructionPUTSTATIC) theInstruction;
                theResult.add(new ASTPutStatic(theCurrentValueStack.pop(), thePut.getConstant()));
            } else if (theInstruction instanceof BytecodeInstructionGETSTATIC) {
                BytecodeInstructionGETSTATIC theGet = (BytecodeInstructionGETSTATIC) theInstruction;
                BytecodeObjectTypeRef theObjectType = BytecodeObjectTypeRef.fromUtf8Constant(theGet.getConstant().getClassIndex().getClassConstant().getConstant());
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
            } else if (theInstruction instanceof BytecodeInstructionDUP) {
                BytecodeInstructionDUP theDup = (BytecodeInstructionDUP) theInstruction;
                theCurrentValueStack.push(new ASTValueReference(theCurrentValueStack.peek()));
            } else if (theInstruction instanceof BytecodeInstructionINVOKESTATIC) {

                BytecodeInstructionINVOKESTATIC theInvoke = (BytecodeInstructionINVOKESTATIC) theInstruction;

                BytecodeMethodRefConstant theMethodRefConstant = theInvoke.getMethodReference();
                BytecodeNameAndTypeConstant theMethodRef = theMethodRefConstant.getNameAndTypeIndex().getNameAndType();
                BytecodeMethodSignature theSig = theMethodRef.getDescriptorIndex().methodSignature();

                List<ASTValue> theArguments = new ArrayList<>();
                for (int i = 0; i < theSig.getArguments().length; i++) {
                    theArguments.add(theCurrentValueStack.pop());
                }

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
                ASTValue theReference = theCurrentValueStack.pop();
                if (!(theReference instanceof ASTLocalVariable)) {
                    int theCallSite = localVariableCounter++;
                    theResult.add(new ASTSetLocalVariable(theCallSite, theReference));
                    theReference = new ASTLocalVariable(theCallSite);
                }

                if (theSig.getReturnType() == BytecodePrimitiveTypeRef.VOID) {
                    theResult.add(new ASTInvokeVirtual(theReference, theArguments, theMethodRefConstant));
                } else {
                    theCurrentValueStack.push(new ASTInvokeVirtual(theReference, theArguments, theMethodRefConstant));
                }

            } else if (theInstruction instanceof BytecodeInstructionNEW) {
                int theNewVariable = localVariableCounter++;
                BytecodeInstructionNEW theNew = (BytecodeInstructionNEW) theInstruction;
                theResult.add(new ASTSetLocalVariable(theNewVariable, new ASTNewObject(theNew.getClassInfoForObjectToCreate())));
                theCurrentValueStack.push(new ASTLocalVariable(theNewVariable));
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
                theResult.add(new ASTIF(new ASTValuesWithOperator(theCurrentValueStack.pop(), ASTValuesWithOperator.Operator.EQUALS, new ASTIntegerValue(0)), theIf.getJumpOffset()));
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
                int theNewVariable = localVariableCounter++;
                theResult.add(new ASTSetLocalVariable(theNewVariable, new ASTNewArray(theNew.getPrimitiveType(), theCurrentValueStack.pop())));
                theCurrentValueStack.push(new ASTLocalVariable(theNewVariable));
            } else if (theInstruction instanceof BytecodeInstructionIFNULL) {
                BytecodeInstructionIFNULL theIf = (BytecodeInstructionIFNULL) theInstruction;
                theResult.add(new ASTIF(new ASTValuesWithOperator(theCurrentValueStack.pop(), ASTValuesWithOperator.Operator.EQUALS, new ASTNull()), theIf.getJumpTarget()));
            } else if (theInstruction instanceof BytecodeInstructionGOTO) {
                BytecodeInstructionGOTO theGoto = (BytecodeInstructionGOTO) theInstruction;
                theResult.add(new ASTGoto(theGoto.getJumpAddress()));
            } else if (theInstruction instanceof BytecodeInstructionGenericLOAD) {
                BytecodeInstructionGenericLOAD theLoad = (BytecodeInstructionGenericLOAD) theInstruction;
                theCurrentValueStack.push(new ASTLocalVariable(theLoad.getLocalVariableIndex()));
            } else if (theInstruction instanceof BytecodeInstructionGenericSTORE) {
                BytecodeInstructionGenericSTORE theStore = (BytecodeInstructionGenericSTORE) theInstruction;
                theResult.add(new ASTSetLocalVariable(theStore.getVariableIndex(), theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionIFICMP) {
                BytecodeInstructionIFICMP theIf = (BytecodeInstructionIFICMP) theInstruction;

                int theNewVariable1 = localVariableCounter++;
                theResult.add(new ASTSetLocalVariable(theNewVariable1, theCurrentValueStack.pop()));

                int theNewVariable2 = localVariableCounter++;
                theResult.add(new ASTSetLocalVariable(theNewVariable2, theCurrentValueStack.pop()));

                switch (theIf.getType()) {
                    case lt:
                        theResult.add(new ASTIF(new ASTValuesWithOperator(new ASTLocalVariable(theNewVariable2), ASTValuesWithOperator.Operator.LESSTHAN, new ASTLocalVariable(theNewVariable1)), theIf.getJumpAddress()));
                        break;
                    case eq:
                        theResult.add(new ASTIF(new ASTValuesWithOperator(new ASTLocalVariable(theNewVariable2), ASTValuesWithOperator.Operator.EQUALS, new ASTLocalVariable(theNewVariable1)), theIf.getJumpAddress()));
                        break;
                    case ge:
                        theResult.add(new ASTIF(new ASTValuesWithOperator(new ASTLocalVariable(theNewVariable2), ASTValuesWithOperator.Operator.GREATEREQUALS, new ASTLocalVariable(theNewVariable1)), theIf.getJumpAddress()));
                        break;
                    case gt:
                        theResult.add(new ASTIF(new ASTValuesWithOperator(new ASTLocalVariable(theNewVariable2), ASTValuesWithOperator.Operator.GREATERTHAN, new ASTLocalVariable(theNewVariable1)), theIf.getJumpAddress()));
                        break;
                    case le:
                        theResult.add(new ASTIF(new ASTValuesWithOperator(new ASTLocalVariable(theNewVariable2), ASTValuesWithOperator.Operator.LESSOREQUALS, new ASTLocalVariable(theNewVariable1)), theIf.getJumpAddress()));
                        break;
                    case ne:
                        theResult.add(new ASTIF(new ASTValuesWithOperator(new ASTLocalVariable(theNewVariable2), ASTValuesWithOperator.Operator.NOTEQUALS, new ASTLocalVariable(theNewVariable1)), theIf.getJumpAddress()));
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
            } else if (theInstruction instanceof BytecodeInstructionGenericASTORE) {
                BytecodeInstructionGenericASTORE theStore = (BytecodeInstructionGenericASTORE) theInstruction;
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

                int theNewVariable2 = localVariableCounter++;
                theResult.add(new ASTSetLocalVariable(theNewVariable2, theCurrentValueStack.pop()));

                int theNewVariable1 = localVariableCounter++;
                theResult.add(new ASTSetLocalVariable(theNewVariable1, theCurrentValueStack.pop()));

                switch (theIf.getType()) {
                    case eq:
                        theResult.add(new ASTIF(new ASTValuesWithOperator(new ASTLocalVariable(theNewVariable2), ASTValuesWithOperator.Operator.EQUALS, new ASTLocalVariable(theNewVariable1)), theIf.getJumpAddress()));
                        break;
                    case ne:
                        theResult.add(new ASTIF(new ASTValuesWithOperator(new ASTLocalVariable(theNewVariable2), ASTValuesWithOperator.Operator.NOTEQUALS, new ASTLocalVariable(theNewVariable1)), theIf.getJumpAddress()));
                        break;
                }
            } else {
                throw new IllegalStateException("Not implemented : " + theInstruction);
            }
        }
        for (int i=1;i<theCurrentValueStack.size();i++) {
            theResult.add(theCurrentValueStack.get(i));
        }
        return theResult;
    }
}