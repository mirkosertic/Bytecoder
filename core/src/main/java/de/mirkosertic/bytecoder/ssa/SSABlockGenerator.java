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
import java.util.List;
import java.util.Map;
import java.util.Stack;

import de.mirkosertic.bytecoder.core.BytecodeBasicBlock;
import de.mirkosertic.bytecoder.core.BytecodeClassinfoConstant;
import de.mirkosertic.bytecoder.core.BytecodeConstant;
import de.mirkosertic.bytecoder.core.BytecodeDoubleConstant;
import de.mirkosertic.bytecoder.core.BytecodeFloatConstant;
import de.mirkosertic.bytecoder.core.BytecodeInstruction;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDUPX1;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSHL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSHR;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericUSHR;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFNONNULL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKEINTERFACE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionLOOKUPSWITCH;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNEWMULTIARRAY;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNOP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionObjectArraySTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionObjectArrayLOAD;
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
import de.mirkosertic.bytecoder.core.BytecodeInstructionDCONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDUP;
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
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSUB;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericXOR;
import de.mirkosertic.bytecoder.core.BytecodeInstructionI2Generic;
import de.mirkosertic.bytecoder.core.BytecodeInstructionICONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFACMP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFCOND;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFICMP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFNULL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIINC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINSTANCEOF;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESPECIAL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKEVIRTUAL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionL2Generic;
import de.mirkosertic.bytecoder.core.BytecodeInstructionLCONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNEW;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNEWARRAY;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPOP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPUTFIELD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPUTSTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionRETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionSIPUSH;
import de.mirkosertic.bytecoder.core.BytecodeInstructionTABLESWITCH;
import de.mirkosertic.bytecoder.core.BytecodeIntegerConstant;
import de.mirkosertic.bytecoder.core.BytecodeLongConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeStringConstant;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;

public class SSABlockGenerator {

    private static class StackHelper {

        private final Stack<Variable> stack;
        private final Block ast;

        public StackHelper(Block aAST) {
            stack = new Stack();
            ast = aAST;
        }

        public Variable pop() {
            if (stack.isEmpty()) {
                stack.push(ast.newImportedStackVariable());
            }
            return stack.pop();
        }

        public Variable peek() {
            return stack.peek();
        }

        public void push(Variable aValue) {
            stack.push(aValue);
        }
    }

    public Block generateFrom(BytecodeBasicBlock aBasicBlock) {

        Block theAST = new Block();

        Map<Integer, Variable> theLocalVariables = new HashMap<>();
        StackHelper theVariablesStack = new StackHelper(theAST);

        for (BytecodeInstruction theInstruction : aBasicBlock.getInstructions()) {
            if (theInstruction instanceof BytecodeInstructionNOP) {
                BytecodeInstructionNOP theINS = (BytecodeInstructionNOP) theInstruction;
                // Completely ignored
            } else if (theInstruction instanceof BytecodeInstructionCHECKCAST) {
                BytecodeInstructionCHECKCAST theINS = (BytecodeInstructionCHECKCAST) theInstruction;
                Variable theVariable = theVariablesStack.peek();
                theAST.addExpression(new CheckCastExpression(theVariable, theINS.getTypeCheck()));
            } else if (theInstruction instanceof BytecodeInstructionPOP) {
                BytecodeInstructionPOP theINS = (BytecodeInstructionPOP) theInstruction;
                theVariablesStack.pop();
            } else if (theInstruction instanceof BytecodeInstructionDUP) {
                BytecodeInstructionDUP theINS = (BytecodeInstructionDUP) theInstruction;
                Variable theVariable = theVariablesStack.peek();
                Variable theClone = theAST.newVariable(new VariableReferenceValue(theVariable.getIndex()));
                theVariablesStack.push(theClone);
            } else if (theInstruction instanceof BytecodeInstructionDUPX1) {
                BytecodeInstructionDUPX1 theINS = (BytecodeInstructionDUPX1) theInstruction;
                Variable theValue1 = theVariablesStack.peek();
                Variable theValue2 = theVariablesStack.peek();

                theVariablesStack.push(theValue1);
                theVariablesStack.push(theValue2);
                Variable theClone = theAST.newVariable(new VariableReferenceValue(theValue1.getIndex()));
                theVariablesStack.push(theClone);
            } else if (theInstruction instanceof BytecodeInstructionGETSTATIC) {
                BytecodeInstructionGETSTATIC theINS = (BytecodeInstructionGETSTATIC) theInstruction;
                GetStaticValue theValue = new GetStaticValue(theINS.getConstant());
                Variable theVariable = theAST.newVariable(theValue);
                theVariablesStack.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionASTORE) {
                BytecodeInstructionASTORE theINS = (BytecodeInstructionASTORE) theInstruction;
                Variable theValue = theVariablesStack.pop();
                theLocalVariables.put(theINS.getVariableIndex(), theValue);
            } else if (theInstruction instanceof BytecodeInstructionGenericSTORE) {
                BytecodeInstructionGenericSTORE theINS = (BytecodeInstructionGenericSTORE) theInstruction;
                Variable theVariable = theVariablesStack.pop();
                theLocalVariables.put(theINS.getVariableIndex(), theVariable);
            } else if (theInstruction instanceof BytecodeInstructionObjectArrayLOAD) {
                BytecodeInstructionObjectArrayLOAD theINS = (BytecodeInstructionObjectArrayLOAD) theInstruction;
                Variable theIndex = theVariablesStack.pop();
                Variable theTarget = theVariablesStack.pop();
                Variable theVariable = theAST.newVariable(new ArrayEntryValue(theTarget, theIndex));
                theVariablesStack.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericArrayLOAD) {
                BytecodeInstructionGenericArrayLOAD theINS = (BytecodeInstructionGenericArrayLOAD) theInstruction;
                Variable theIndex = theVariablesStack.pop();
                Variable theTarget = theVariablesStack.pop();
                Variable theVariable = theAST.newVariable(new ArrayEntryValue(theTarget, theIndex));
                theVariablesStack.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericArraySTORE) {
                BytecodeInstructionGenericArraySTORE theINS = (BytecodeInstructionGenericArraySTORE) theInstruction;
                Variable theValue = theVariablesStack.pop();
                Variable theIndex = theVariablesStack.pop();
                Variable theTarget = theVariablesStack.pop();
                theAST.addExpression(new ArrayStoreExpression(theTarget, theIndex, theValue));
            } else if (theInstruction instanceof BytecodeInstructionObjectArraySTORE) {
                BytecodeInstructionObjectArraySTORE theINS = (BytecodeInstructionObjectArraySTORE) theInstruction;
                Variable theValue = theVariablesStack.pop();
                Variable theIndex = theVariablesStack.pop();
                Variable theTarget = theVariablesStack.pop();
                theAST.addExpression(new ArrayStoreExpression(theTarget, theIndex, theValue));
            } else if (theInstruction instanceof BytecodeInstructionACONSTNULL) {
                BytecodeInstructionACONSTNULL theINS = (BytecodeInstructionACONSTNULL) theInstruction;
                Variable theVariable = theAST.newVariable(new NullValue());
                theVariablesStack.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionPUTFIELD) {
                BytecodeInstructionPUTFIELD theINS = (BytecodeInstructionPUTFIELD) theInstruction;
                Variable theValue = theVariablesStack.pop();
                Variable theTarget = theVariablesStack.pop();
                theAST.addExpression(new PutFieldExpression(theINS.getFieldRefConstant(), theTarget, theValue));
            } else if (theInstruction instanceof BytecodeInstructionGETFIELD) {
                BytecodeInstructionGETFIELD theINS = (BytecodeInstructionGETFIELD) theInstruction;
                Variable theTarget = theVariablesStack.pop();
                Variable theVariable = theAST.newVariable(new GetFieldValue(theINS.getFieldRefConstant(), theTarget));
                theVariablesStack.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionPUTSTATIC) {
                BytecodeInstructionPUTSTATIC theINS = (BytecodeInstructionPUTSTATIC) theInstruction;
                Variable theValue = theVariablesStack.pop();
                theAST.addExpression(new PutStaticExpression(theINS.getConstant(), theValue));
            } else if (theInstruction instanceof BytecodeInstructionGenericLDC) {
                BytecodeInstructionGenericLDC theINS = (BytecodeInstructionGenericLDC) theInstruction;
                BytecodeConstant theConstant = theINS.constant();
                if (theConstant instanceof BytecodeDoubleConstant) {
                    BytecodeDoubleConstant theC = (BytecodeDoubleConstant) theConstant;
                    Variable theVariable = theAST.newVariable(new DoubleValue(theC.getDoubleValue()));
                    theVariablesStack.push(theVariable);
                } else if (theConstant instanceof BytecodeLongConstant) {
                    BytecodeLongConstant theC = (BytecodeLongConstant) theConstant;
                    Variable theVariable = theAST.newVariable(new LongValue(theC.getLongValue()));
                    theVariablesStack.push(theVariable);
                } else if (theConstant instanceof BytecodeFloatConstant) {
                    BytecodeFloatConstant theC = (BytecodeFloatConstant) theConstant;
                    Variable theVariable = theAST.newVariable(new FloatValue(theC.getFloatValue()));
                    theVariablesStack.push(theVariable);
                } else if (theConstant instanceof BytecodeIntegerConstant) {
                    BytecodeIntegerConstant theC = (BytecodeIntegerConstant) theConstant;
                    Variable theVariable = theAST.newVariable(new IntegerValue(theC.getIntegerValue()));
                    theVariablesStack.push(theVariable);
                } else if (theConstant instanceof BytecodeStringConstant) {
                    BytecodeStringConstant theC = (BytecodeStringConstant) theConstant;
                    Variable theVariable = theAST.newVariable(new StringValue(theC.getValue().stringValue()));
                    theVariablesStack.push(theVariable);
                } else if (theConstant instanceof BytecodeClassinfoConstant) {
                    BytecodeClassinfoConstant theC = (BytecodeClassinfoConstant) theConstant;
                    Variable theVariable = theAST.newVariable(new ClassReferenceValue(BytecodeObjectTypeRef.fromUtf8Constant(theC.getConstant())));
                    theVariablesStack.push(theVariable);
                } else {
                    throw new IllegalArgumentException("Unsupported constant type : " + theConstant);
                }
            } else if (theInstruction instanceof BytecodeInstructionBIPUSH) {
                BytecodeInstructionBIPUSH theINS = (BytecodeInstructionBIPUSH) theInstruction;
                Variable theVariable = theAST.newVariable(new ByteValue(theINS.getByteValue()));
                theVariablesStack.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionSIPUSH) {
                BytecodeInstructionSIPUSH theINS = (BytecodeInstructionSIPUSH) theInstruction;
                Variable theVariable = theAST.newVariable(new ShortValue(theINS.getShortValue()));
                theVariablesStack.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionICONST) {
                BytecodeInstructionICONST theINS = (BytecodeInstructionICONST) theInstruction;
                Variable theVariable = theAST.newVariable(new IntegerValue(theINS.getIntConst()));
                theVariablesStack.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionFCONST) {
                BytecodeInstructionFCONST theINS = (BytecodeInstructionFCONST) theInstruction;
                Variable theVariable = theAST.newVariable(new FloatValue(theINS.getFloatValue()));
                theVariablesStack.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionDCONST) {
                BytecodeInstructionDCONST theINS = (BytecodeInstructionDCONST) theInstruction;
                Variable theVariable = theAST.newVariable(new DoubleValue(theINS.getDoubleConst()));
                theVariablesStack.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionLCONST) {
                BytecodeInstructionLCONST theINS = (BytecodeInstructionLCONST) theInstruction;
                Variable theVariable = theAST.newVariable(new LongValue(theINS.getLongConst()));
                theVariablesStack.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericNEG) {
                BytecodeInstructionGenericNEG theINS = (BytecodeInstructionGenericNEG) theInstruction;
                Variable theVariable = theVariablesStack.pop();
                Variable theNegatedValue = theAST.newVariable(new NegatedValue(theVariable));
                theVariablesStack.push(theNegatedValue);
            } else if (theInstruction instanceof BytecodeInstructionARRAYLENGTH) {
                BytecodeInstructionARRAYLENGTH theINS = (BytecodeInstructionARRAYLENGTH) theInstruction;
                Variable theVariable = theVariablesStack.pop();
                Variable theNegatedValue = theAST.newVariable(new ArrayLengthValue(theVariable));
                theVariablesStack.push(theNegatedValue);
            } else if (theInstruction instanceof BytecodeInstructionGenericLOAD) {
                BytecodeInstructionGenericLOAD theINS = (BytecodeInstructionGenericLOAD) theInstruction;
                Variable theVariable = theLocalVariables.get(theINS.getVariableIndex());
                if (theVariable == null) {
                    theVariable = theAST.newImportedLocalVariable(theINS.getVariableIndex());
                    theLocalVariables.put(theINS.getVariableIndex(), theVariable);
                }
                theVariablesStack.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionALOAD) {
                BytecodeInstructionALOAD theINS = (BytecodeInstructionALOAD) theInstruction;
                Variable theVariable = theLocalVariables.get(theINS.getVariableIndex());
                if (theVariable == null) {
                    theVariable = theAST.newImportedLocalVariable(theINS.getVariableIndex());
                    theLocalVariables.put(theINS.getVariableIndex(), theVariable);
                }
                theVariablesStack.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericCMP) {
                BytecodeInstructionGenericCMP theINS = (BytecodeInstructionGenericCMP) theInstruction;
                Variable theValue2 = theVariablesStack.pop();
                Variable theValue1 = theVariablesStack.pop();
                Variable theNewVariable = theAST.newVariable(new CompareValue(theValue1, theValue2));
                theVariablesStack.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionIINC) {
                BytecodeInstructionIINC theINS = (BytecodeInstructionIINC) theInstruction;
                Variable theVariable = theLocalVariables.get(theINS.getIndex());
                Variable theAmount = theAST.newVariable(new IntegerValue(theINS.getConstant()));
                Variable theNewVariable = theAST.newVariable(new BinaryValue(theVariable, BinaryValue.Operator.ADD, theAmount));
                theVariablesStack.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericREM) {
                BytecodeInstructionGenericREM theINS = (BytecodeInstructionGenericREM) theInstruction;
                Variable theValue2 = theVariablesStack.pop();
                Variable theValue1 = theVariablesStack.pop();
                Variable theNewVariable = theAST.newVariable(new BinaryValue(theValue1, BinaryValue.Operator.REMAINDER, theValue2));
                theVariablesStack.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericADD) {
                BytecodeInstructionGenericADD theINS = (BytecodeInstructionGenericADD) theInstruction;
                Variable theValue2 = theVariablesStack.pop();
                Variable theValue1 = theVariablesStack.pop();
                Variable theNewVariable = theAST.newVariable(new BinaryValue(theValue1, BinaryValue.Operator.ADD, theValue2));
                theVariablesStack.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericDIV) {
                BytecodeInstructionGenericDIV theINS = (BytecodeInstructionGenericDIV) theInstruction;
                Variable theValue2 = theVariablesStack.pop();
                Variable theValue1 = theVariablesStack.pop();
                Variable theNewVariable = theAST.newVariable(new BinaryValue(theValue1, BinaryValue.Operator.DIV, theValue2));
                theVariablesStack.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericMUL) {
                BytecodeInstructionGenericMUL theINS = (BytecodeInstructionGenericMUL) theInstruction;
                Variable theValue2 = theVariablesStack.pop();
                Variable theValue1 = theVariablesStack.pop();
                Variable theNewVariable = theAST.newVariable(new BinaryValue(theValue1, BinaryValue.Operator.MUL, theValue2));
                theVariablesStack.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericSUB) {
                BytecodeInstructionGenericSUB theINS = (BytecodeInstructionGenericSUB) theInstruction;
                Variable theValue2 = theVariablesStack.pop();
                Variable theValue1 = theVariablesStack.pop();
                Variable theNewVariable = theAST.newVariable(new BinaryValue(theValue1, BinaryValue.Operator.SUB, theValue2));
                theVariablesStack.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericXOR) {
                BytecodeInstructionGenericXOR theINS = (BytecodeInstructionGenericXOR) theInstruction;
                Variable theValue2 = theVariablesStack.pop();
                Variable theValue1 = theVariablesStack.pop();
                Variable theNewVariable = theAST.newVariable(new BinaryValue(theValue1, BinaryValue.Operator.BINARYXOR, theValue2));
                theVariablesStack.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericOR) {
                BytecodeInstructionGenericOR theINS = (BytecodeInstructionGenericOR) theInstruction;
                Variable theValue2 = theVariablesStack.pop();
                Variable theValue1 = theVariablesStack.pop();
                Variable theNewVariable = theAST.newVariable(new BinaryValue(theValue1, BinaryValue.Operator.BINARYOR, theValue2));
                theVariablesStack.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericAND) {
                BytecodeInstructionGenericAND theINS = (BytecodeInstructionGenericAND) theInstruction;
                Variable theValue2 = theVariablesStack.pop();
                Variable theValue1 = theVariablesStack.pop();
                Variable theNewVariable = theAST.newVariable(new BinaryValue(theValue1, BinaryValue.Operator.BINARYAND, theValue2));
                theVariablesStack.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericSHL) {
                BytecodeInstructionGenericSHL theINS = (BytecodeInstructionGenericSHL) theInstruction;
                Variable theValue2 = theVariablesStack.pop();
                Variable theValue1 = theVariablesStack.pop();
                Variable theNewVariable = theAST.newVariable(new BinaryValue(theValue1, BinaryValue.Operator.BINARYSHIFTLEFT, theValue2));
                theVariablesStack.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericSHR) {
                BytecodeInstructionGenericSHR theINS = (BytecodeInstructionGenericSHR) theInstruction;
                Variable theValue2 = theVariablesStack.pop();
                Variable theValue1 = theVariablesStack.pop();
                Variable theNewVariable = theAST.newVariable(new BinaryValue(theValue1, BinaryValue.Operator.BINARYSHIFTRIGHT, theValue2));
                theVariablesStack.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericUSHR) {
                BytecodeInstructionGenericUSHR theINS = (BytecodeInstructionGenericUSHR) theInstruction;
                Variable theValue2 = theVariablesStack.pop();
                Variable theValue1 = theVariablesStack.pop();
                Variable theNewVariable = theAST.newVariable(new BinaryValue(theValue1, BinaryValue.Operator.BINARYUNSIGNEDSHIFTRIGHT, theValue2));
                theVariablesStack.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionIFNULL) {
                BytecodeInstructionIFNULL theINS = (BytecodeInstructionIFNULL) theInstruction;
                Variable theValue = theVariablesStack.pop();
                FixedBinaryValue theBinaryValue = new FixedBinaryValue(theValue, FixedBinaryValue.Operator.ISNULL);
                Variable theResult = theAST.newVariable(theBinaryValue);
                theAST.addExpression(new IFExpression(theResult, theINS.getJumpTarget()));
            } else if (theInstruction instanceof BytecodeInstructionIFNONNULL) {
                BytecodeInstructionIFNONNULL theINS = (BytecodeInstructionIFNONNULL) theInstruction;
                Variable theValue = theVariablesStack.pop();
                FixedBinaryValue theBinaryValue = new FixedBinaryValue(theValue, FixedBinaryValue.Operator.ISNONNULL);
                Variable theResult = theAST.newVariable(theBinaryValue);
                theAST.addExpression(new IFExpression(theResult, theINS.getJumpTarget()));
            } else if (theInstruction instanceof BytecodeInstructionIFICMP) {
                BytecodeInstructionIFICMP theINS = (BytecodeInstructionIFICMP) theInstruction;
                Variable theValue2 = theVariablesStack.pop();
                Variable theValue1 = theVariablesStack.pop();
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
                Variable theNewVariable = theAST.newVariable(theBinaryValue);
                theAST.addExpression(new IFExpression(theNewVariable, theINS.getJumpTarget()));

            } else if (theInstruction instanceof BytecodeInstructionIFACMP) {
                BytecodeInstructionIFACMP theINS = (BytecodeInstructionIFACMP) theInstruction;
                Variable theValue2 = theVariablesStack.pop();
                Variable theValue1 = theVariablesStack.pop();
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
                Variable theNewVariable = theAST.newVariable(theBinaryValue);
                theAST.addExpression(new IFExpression(theNewVariable, theINS.getJumpTarget()));

            } else if (theInstruction instanceof BytecodeInstructionIFCOND) {
                BytecodeInstructionIFCOND theINS = (BytecodeInstructionIFCOND) theInstruction;
                Variable theValue = theVariablesStack.pop();
                Variable theZero = theAST.newVariable(new IntegerValue(0));
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
                Variable theNewVariable = theAST.newVariable(theBinaryValue);
                theAST.addExpression(new IFExpression(theNewVariable, theINS.getJumpTarget()));
            } else if (theInstruction instanceof BytecodeInstructionARETURN) {
                BytecodeInstructionARETURN theINS = (BytecodeInstructionARETURN) theInstruction;
                Variable theVariable = theVariablesStack.pop();
                theAST.addExpression(new ReturnVariableExpression(theVariable));
            } else if (theInstruction instanceof BytecodeInstructionGenericRETURN) {
                BytecodeInstructionGenericRETURN theINS = (BytecodeInstructionGenericRETURN) theInstruction;
                Variable theVariable = theVariablesStack.pop();
                theAST.addExpression(new ReturnVariableExpression(theVariable));
            } else if (theInstruction instanceof BytecodeInstructionATHROW) {
                BytecodeInstructionATHROW theINS = (BytecodeInstructionATHROW) theInstruction;
                Variable theVariable = theVariablesStack.pop();
                theAST.addExpression(new ThrowExpression(theVariable));
            } else if (theInstruction instanceof BytecodeInstructionRETURN) {
                BytecodeInstructionRETURN theINS = (BytecodeInstructionRETURN) theInstruction;
                theAST.addExpression(new ReturnExpression());
            } else if (theInstruction instanceof BytecodeInstructionNEW) {
                BytecodeInstructionNEW theINS = (BytecodeInstructionNEW) theInstruction;
                Variable theNewVariable = theAST.newVariable(new NewObjectValue(theINS.getClassInfoForObjectToCreate()));
                theVariablesStack.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionNEWARRAY) {
                BytecodeInstructionNEWARRAY theINS = (BytecodeInstructionNEWARRAY) theInstruction;
                Variable theLength = theVariablesStack.pop();
                Variable theNewVariable = theAST.newVariable(new NewArrayValue(theINS.getPrimitiveType(), theLength));
                theVariablesStack.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionNEWMULTIARRAY) {
                BytecodeInstructionNEWMULTIARRAY theINS = (BytecodeInstructionNEWMULTIARRAY) theInstruction;
                List<Variable> theDimensions = new ArrayList<>();
                for (int i=0;i<theINS.getDimensions();i++) {
                    theDimensions.add(theVariablesStack.pop());
                }
                Collections.reverse(theDimensions);
                Variable theNewVariable = theAST.newVariable(new NewMultiArrayValue(BytecodeObjectTypeRef.fromUtf8Constant(theINS.getTypeConstant().getConstant()), theDimensions));
                theVariablesStack.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionANEWARRAY) {
                BytecodeInstructionANEWARRAY theINS = (BytecodeInstructionANEWARRAY) theInstruction;
                Variable theLength = theVariablesStack.pop();
                Variable theNewVariable = theAST.newVariable(new NewArrayValue(BytecodeObjectTypeRef.fromUtf8Constant(theINS.getTypeConstant().getConstant()), theLength));
                theVariablesStack.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGOTO) {
                BytecodeInstructionGOTO theINS = (BytecodeInstructionGOTO) theInstruction;
                theAST.addExpression(new GotoExpression(theINS.getJumpAddress()));
            } else if (theInstruction instanceof BytecodeInstructionL2Generic) {
                BytecodeInstructionL2Generic theINS = (BytecodeInstructionL2Generic) theInstruction;
                Variable theValue = theVariablesStack.pop();
                Variable theNewVariable = theAST.newVariable(new TypeConvertionValue(theValue, theINS.getTargetType()));
                theVariablesStack.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionI2Generic) {
                BytecodeInstructionI2Generic theINS = (BytecodeInstructionI2Generic) theInstruction;
                Variable theValue = theVariablesStack.pop();
                Variable theNewVariable = theAST.newVariable(new TypeConvertionValue(theValue, theINS.getTargetType()));
                theVariablesStack.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionF2Generic) {
                BytecodeInstructionF2Generic theINS = (BytecodeInstructionF2Generic) theInstruction;
                Variable theValue = theVariablesStack.pop();
                Variable theNewVariable = theAST.newVariable(new TypeConvertionValue(theValue, theINS.getTargetType()));
                theVariablesStack.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionD2Generic) {
                BytecodeInstructionD2Generic theINS = (BytecodeInstructionD2Generic) theInstruction;
                Variable theValue = theVariablesStack.pop();
                Variable theNewVariable = theAST.newVariable(new TypeConvertionValue(theValue, theINS.getTargetType()));
                theVariablesStack.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionINVOKESPECIAL) {
                BytecodeInstructionINVOKESPECIAL theINS = (BytecodeInstructionINVOKESPECIAL) theInstruction;
                BytecodeMethodSignature theSignature = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                List<Variable> theArguments = new ArrayList<>();
                BytecodeTypeRef[] theArgumentTypes = theSignature.getArguments();
                for (int i=theArgumentTypes.length - 1; i>=0;i--) {
                    theArguments.add(theVariablesStack.pop());
                }

                Variable theTarget = theVariablesStack.pop();

                InvokeMethodValue theValue = new InvokeMethodValue(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType(), theTarget, theArguments);
                if (theSignature.getReturnType().isVoid()) {
                    theAST.addExpression(new InvokeMethodExpression(theValue));
                } else {
                    Variable theNewVariable = theAST.newVariable(theValue);
                    theVariablesStack.push(theNewVariable);
                }
            } else if (theInstruction instanceof BytecodeInstructionINVOKEVIRTUAL) {
                BytecodeInstructionINVOKEVIRTUAL theINS = (BytecodeInstructionINVOKEVIRTUAL) theInstruction;
                BytecodeMethodSignature theSignature = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                List<Variable> theArguments = new ArrayList<>();
                BytecodeTypeRef[] theArgumentTypes = theSignature.getArguments();
                for (int i=theArgumentTypes.length - 1; i>=0;i--) {
                    theArguments.add(theVariablesStack.pop());
                }

                Variable theTarget = theVariablesStack.pop();

                InvokeMethodValue theValue = new InvokeMethodValue(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType(), theTarget, theArguments);
                if (theSignature.getReturnType().isVoid()) {
                    theAST.addExpression(new InvokeMethodExpression(theValue));
                } else {
                    Variable theNewVariable = theAST.newVariable(theValue);
                    theVariablesStack.push(theNewVariable);
                }

            } else if (theInstruction instanceof BytecodeInstructionINVOKEINTERFACE) {
                BytecodeInstructionINVOKEINTERFACE theINS = (BytecodeInstructionINVOKEINTERFACE) theInstruction;
                BytecodeMethodSignature theSignature = theINS.getMethodDescriptor().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                List<Variable> theArguments = new ArrayList<>();
                BytecodeTypeRef[] theArgumentTypes = theSignature.getArguments();
                for (int i=theArgumentTypes.length - 1; i>=0;i--) {
                    theArguments.add(theVariablesStack.pop());
                }

                Variable theTarget = theVariablesStack.pop();

                InvokeMethodValue theValue = new InvokeMethodValue(theINS.getMethodDescriptor().getNameAndTypeIndex().getNameAndType(), theTarget, theArguments);
                if (theSignature.getReturnType().isVoid()) {
                    theAST.addExpression(new InvokeMethodExpression(theValue));
                } else {
                    Variable theNewVariable = theAST.newVariable(theValue);
                    theVariablesStack.push(theNewVariable);
                }

            } else if (theInstruction instanceof BytecodeInstructionINVOKESTATIC) {
                BytecodeInstructionINVOKESTATIC theINS = (BytecodeInstructionINVOKESTATIC) theInstruction;
                BytecodeMethodSignature theSignature = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                List<Variable> theArguments = new ArrayList<>();
                BytecodeTypeRef[] theArgumentTypes = theSignature.getArguments();
                for (int i=theArgumentTypes.length - 1; i>=0;i--) {
                    theArguments.add(theVariablesStack.pop());
                }

                InvokeStaticMethodValue theValue = new InvokeStaticMethodValue(theINS.getMethodReference(), theArguments);
                if (theSignature.getReturnType().isVoid()) {
                    theAST.addExpression(new InvokeStaticMethodExpression(theValue));
                } else {
                    Variable theNewVariable = theAST.newVariable(theValue);
                    theVariablesStack.push(theNewVariable);
                }
            } else if (theInstruction instanceof BytecodeInstructionINSTANCEOF) {
                BytecodeInstructionINSTANCEOF theINS = (BytecodeInstructionINSTANCEOF) theInstruction;

                Variable theVariable = theVariablesStack.pop();
                InstanceOfValue theValue = new InstanceOfValue(theVariable, theINS.getTypeRef());

                Variable theCheckResult = theAST.newVariable(theValue);
                theVariablesStack.push(theCheckResult);
            } else if (theInstruction instanceof BytecodeInstructionTABLESWITCH) {
                BytecodeInstructionTABLESWITCH theINS = (BytecodeInstructionTABLESWITCH) theInstruction;
                Variable theVariable = theVariablesStack.pop();
                theAST.addExpression(new TableSwitchExpression(theVariable, theINS));
            } else if (theInstruction instanceof BytecodeInstructionLOOKUPSWITCH) {
                BytecodeInstructionLOOKUPSWITCH theINS = (BytecodeInstructionLOOKUPSWITCH) theInstruction;
                Variable theVariable = theVariablesStack.pop();
                theAST.addExpression(new LookupSwitchExpression(theVariable, theINS));
            } else {
                throw new IllegalArgumentException("Not implemented : " + theInstruction);
            }
        }

        return theAST;
    }
}