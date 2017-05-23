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

import de.mirkosertic.bytecoder.core.BytecodeBasicBlock;
import de.mirkosertic.bytecoder.core.BytecodeInstruction;
import de.mirkosertic.bytecoder.core.BytecodeInstructionACONSTNULL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionALOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionARETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionASTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionATHROW;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDUP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGETSTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericADD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericDIV;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericLDC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericLOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericMUL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericRETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSUB;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESPECIAL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKEVIRTUAL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNEW;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPUTSTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionRETURN;
import de.mirkosertic.bytecoder.core.BytecodeMethodRefConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeNameAndTypeConstant;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;

public class ASTGenerator {

    public ASTBlock generateFrom(BytecodeBasicBlock aBasicBlock) {
        ASTBlock theResult = new ASTBlock();
        Stack<ASTValue> theCurrentValueStack = new Stack<>();
        theCurrentValueStack.push(new ASTInputOfBlock());
        for (BytecodeInstruction theInstruction : aBasicBlock.getInstructions()) {
            if (theInstruction instanceof BytecodeInstructionRETURN) {
                theResult.add(new ASTReturn());
            } else if (theInstruction instanceof BytecodeInstructionGenericLDC) {
                BytecodeInstructionGenericLDC theLoad = (BytecodeInstructionGenericLDC) theInstruction;
                theCurrentValueStack.push(new ASTConstantValue(theLoad.constant()));
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
                theCurrentValueStack.push(new ASTGetStatic(theGet.getConstant()));
            } else if (theInstruction instanceof BytecodeInstructionATHROW) {
                BytecodeInstructionATHROW theThrow = (BytecodeInstructionATHROW) theInstruction;
                theResult.add(new ASTThrow(theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionGenericADD) {
                BytecodeInstructionGenericADD theAdd = (BytecodeInstructionGenericADD) theInstruction;
                theCurrentValueStack.push(new ASTComputationADD(theCurrentValueStack.pop(), theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionGenericSUB) {
                BytecodeInstructionGenericSUB theSub = (BytecodeInstructionGenericSUB) theInstruction;
                theCurrentValueStack.push(new ASTComputationSUB(theCurrentValueStack.pop(), theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionGenericMUL) {
                BytecodeInstructionGenericMUL theMul = (BytecodeInstructionGenericMUL) theInstruction;
                theCurrentValueStack.push(new ASTComputationMUL(theCurrentValueStack.pop(), theCurrentValueStack.pop()));
            } else if (theInstruction instanceof BytecodeInstructionGenericDIV) {
                BytecodeInstructionGenericDIV theDiv = (BytecodeInstructionGenericDIV) theInstruction;
                theCurrentValueStack.push(new ASTComputationDIV(theCurrentValueStack.pop(), theCurrentValueStack.pop()));
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

                if (theSig.getReturnType() == BytecodePrimitiveTypeRef.VOID) {
                    theResult.add(new ASTInvokeVirtual(theReference, theArguments, theMethodRefConstant));
                } else {
                    theCurrentValueStack.push(new ASTInvokeVirtual(theReference, theArguments, theMethodRefConstant));
                }

            } else if (theInstruction instanceof BytecodeInstructionNEW) {
                BytecodeInstructionNEW theNew = (BytecodeInstructionNEW) theInstruction;
                theCurrentValueStack.push(new ASTNewObject(theNew.getClassInfoForObjectToCreate()));
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
