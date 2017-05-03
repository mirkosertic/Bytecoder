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
package de.mirkosertic.bytecoder.backend.js;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import de.mirkosertic.bytecoder.core.BytecodeClassinfoConstant;
import de.mirkosertic.bytecoder.core.BytecodeCodeAttributeInfo;
import de.mirkosertic.bytecoder.core.BytecodeConstant;
import de.mirkosertic.bytecoder.core.BytecodeFloatConstant;
import de.mirkosertic.bytecoder.core.BytecodeInstruction;
import de.mirkosertic.bytecoder.core.BytecodeInstructionALOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionARETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionASTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionATHROW;
import de.mirkosertic.bytecoder.core.BytecodeInstructionBIPUSH;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDUP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionFCMP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionFCONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGOTO;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericADD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericDIV;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericLOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericMUL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericRETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSUB;
import de.mirkosertic.bytecoder.core.BytecodeInstructionI2F;
import de.mirkosertic.bytecoder.core.BytecodeInstructionICONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFCOND;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFNONNULL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFNULL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESPECIAL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKEVIRTUAL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionLCMP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionLDC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNEW;
import de.mirkosertic.bytecoder.core.BytecodeInstructionRETURN;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethodRefConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeNameAndTypeConstant;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.core.BytecodeProgramm;
import de.mirkosertic.bytecoder.core.BytecodeStringConstant;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeUtf8Constant;

public class JSBackend {

    public String generateCodeFor(BytecodeLinkerContext aLinkerContext) {

        StringWriter theStrWriter = new StringWriter();
        final PrintWriter theWriter = new PrintWriter(theStrWriter);

        aLinkerContext.forEachClass(aEntry -> aEntry.getValue().forEachMethod(aMethod -> {
            BytecodeCodeAttributeInfo theCode = aMethod.attributeByType(BytecodeCodeAttributeInfo.class);
            BytecodeMethodSignature theSignature = aMethod.getSignature();
            StringBuffer theArguments = new StringBuffer();
            for (int i=1;i<=theSignature.getArguments().length;i++) {
                if (theArguments.length() > 0) {
                    theArguments.append(",");
                }
                theArguments.append("p" + i);
            }
            theWriter.println("var " + aMethod.getName().stringValue() + " = function(" + theArguments.toString() + ") {");
            for (int i=1;i<=theCode.getMaxLocals();i++) {
                theWriter.println("    var local" + i+";");
            }
            theWriter.println("    var stack = [" + theCode.getMaxStack() + "];");
            theWriter.println("    var stackOffset = -1;");

            // Push parameters to local variables
            for (int i=1;i<=theSignature.getArguments().length;i++) {
                theWriter.println("    local" + i + " = p" + i +";");
            }

            BytecodeProgramm theProgram = theCode.getProgramm();

            String theLabelPrefix = aMethod.getName().stringValue();

            List<BytecodeOpcodeAddress> thePotentialJumpTargets =  theProgram.getPotentialJumpTargets();
            List<BytecodeOpcodeAddress> theJumpSources = theProgram.getJumpSources();

            theWriter.println("    // Begin method code");
            for (BytecodeInstruction theInstruction : theCode.getProgramm().getInstructions()) {

                if (thePotentialJumpTargets.contains(theInstruction.getOpcodeAddress())) {
                    theWriter.println("    " + theLabelPrefix + theInstruction.getOpcodeAddress().getAddress() + ":");
                }
                if (theInstruction instanceof BytecodeInstructionRETURN) {
                    theWriter.println("    return;");
                } else if (theInstruction instanceof BytecodeInstructionDUP) {
                    BytecodeInstructionDUP theDup = (BytecodeInstructionDUP) theInstruction;

                    theWriter.println("    {");
                    theWriter.println("        var top = stack[stackOffset];");
                    theWriter.println("        stack[++stackOffset] = top;");
                    theWriter.println("    }");
                } else if (theInstruction instanceof BytecodeInstructionNEW) {
                    BytecodeInstructionNEW theNew = (BytecodeInstructionNEW) theInstruction;

                    BytecodeConstant theConstant = theNew.getConstructorRef();
                    System.out.println(theConstant);

                } else if (theInstruction instanceof BytecodeInstructionINVOKESPECIAL) {
                    BytecodeInstructionINVOKESPECIAL theInvokeSpecial = (BytecodeInstructionINVOKESPECIAL) theInstruction;

                    BytecodeConstant theConstant = theInvokeSpecial.getSuperConstructorRef();
                    System.out.println(theConstant);

                } else if (theInstruction instanceof BytecodeInstructionINVOKEVIRTUAL) {
                    BytecodeInstructionINVOKEVIRTUAL theVirtualInvoke = (BytecodeInstructionINVOKEVIRTUAL) theInstruction;

                    BytecodeMethodRefConstant theMethodRefConstant = theVirtualInvoke.getMethodDescriptor();

                    BytecodeClassinfoConstant theClassConstant = theMethodRefConstant.getClassIndex().getClassConstant();
                    BytecodeNameAndTypeConstant theMethodRef = theMethodRefConstant.getNameAndTypeIndex().getNameAndType();
                    BytecodeMethodSignature theSig = theMethodRef.getDescriptorIndex().methodSignature();
                    BytecodeUtf8Constant theName = theMethodRef.getNameIndex().getName();
                    theWriter.println("    {");
                    BytecodeTypeRef[] theStaticArguments = theSig.getArguments();
                    for (int i=theStaticArguments.length;i>0;i--) {
                        theWriter.println("        var arg"+i+" = stack[stackOffset--];");
                    }
                    theWriter.println("        var callsite = stack[stackOffset--];");
                    theWriter.print("        callsite." + theName.stringValue()+"(");
                    for (int i=1;i<=theStaticArguments.length;i++) {
                        if (i>1) {
                            theWriter.print(",");
                        }
                        theWriter.print("arg" + i);
                    }
                    theWriter.println(");");
                    theWriter.println("    }");
                }else if (theInstruction instanceof BytecodeInstructionINVOKESTATIC) {
                    BytecodeInstructionINVOKESTATIC theStaticInvoke = (BytecodeInstructionINVOKESTATIC) theInstruction;

                    BytecodeMethodRefConstant theMethodRefConstant = theStaticInvoke.getMethodRefConstant();
                    BytecodeClassinfoConstant theClassConstant = theMethodRefConstant.getClassIndex().getClassConstant();
                    BytecodeNameAndTypeConstant theMethodRef = theMethodRefConstant.getNameAndTypeIndex().getNameAndType();
                    BytecodeMethodSignature theSig = theMethodRef.getDescriptorIndex().methodSignature();
                    BytecodeUtf8Constant theName = theMethodRef.getNameIndex().getName();
                    theWriter.println("    {");
                    BytecodeTypeRef[] theStaticArguments = theSig.getArguments();
                    for (int i=theStaticArguments.length;i>0;i--) {
                        theWriter.println("        var arg"+i+" = stack[stackOffset--];");
                    }
                    theWriter.print("        " + theName.stringValue()+"(");
                    for (int i=1;i<=theStaticArguments.length;i++) {
                        if (i>1) {
                            theWriter.print(",");
                        }
                        theWriter.print("arg" + i);
                    }
                    theWriter.println(");");
                    theWriter.println("    }");
                } else if (theInstruction instanceof BytecodeInstructionBIPUSH) {
                    BytecodeInstructionBIPUSH thePush = (BytecodeInstructionBIPUSH) theInstruction;
                    theWriter.println("    stack[++stackOffset] = " + thePush.getValue() + ";");
                } else if (theInstruction instanceof BytecodeInstructionGenericLOAD) {
                    BytecodeInstructionGenericLOAD theLoad = (BytecodeInstructionGenericLOAD) theInstruction;
                    theWriter.println("    stack[++stackOffset] = local" + theLoad.getIndex() + ";");
                } else if (theInstruction instanceof BytecodeInstructionGenericSTORE) {
                    BytecodeInstructionGenericSTORE theStore = (BytecodeInstructionGenericSTORE) theInstruction;
                    theWriter.println("    local" + theStore.getVariableIndex()+" = stack[stackOffset--];");
                } else if (theInstruction instanceof BytecodeInstructionASTORE) {
                    BytecodeInstructionASTORE theStore = (BytecodeInstructionASTORE) theInstruction;
                    theWriter.println("    local" + theStore.getVariableIndex()+" = stack[stackOffset--];");
                } else if (theInstruction instanceof BytecodeInstructionALOAD) {
                    BytecodeInstructionALOAD theStore = (BytecodeInstructionALOAD) theInstruction;
                    theWriter.println("    stack[++stackOffset] = local" + theStore.getVariableIndex() + ";");
                } else if (theInstruction instanceof BytecodeInstructionLCMP) {
                    BytecodeInstructionLCMP theCmp = (BytecodeInstructionLCMP) theInstruction;
                    theWriter.println("    {");
                    theWriter.println("        var temp2 = stack[stackOffset--];");
                    theWriter.println("        var temp1 = stack[stackOffset--];");
                    theWriter.println("        if (temp1 > temp2) {");
                    theWriter.println("         stack[++stackOffset] = 1;");
                    theWriter.println("        } else if (temp1 < temp2) {");
                    theWriter.println("         stack[++stackOffset] = -1;");
                    theWriter.println("        } else {");
                    theWriter.println("         stack[++stackOffset] = 0;");
                    theWriter.println("        }");
                    theWriter.println("    }");
                } else if (theInstruction instanceof BytecodeInstructionIFNULL) {
                    BytecodeInstructionIFNULL theIf = (BytecodeInstructionIFNULL) theInstruction;
                    theWriter.println("    {");
                    theWriter.println("        var currentStack = stack[stackOffset--];");
                    theWriter.println("        if (!(currentStack)) {");
                    theWriter.println("         goto label" + theIf.getOpcodeAddress() + theIf.getJumpOffset());
                    theWriter.println("        }");
                    theWriter.println("    }");
                } else if (theInstruction instanceof BytecodeInstructionIFNONNULL) {
                    BytecodeInstructionIFNONNULL theIf = (BytecodeInstructionIFNONNULL) theInstruction;
                    theWriter.println("    {");
                    theWriter.println("        var currentStack = stack[stackOffset--];");
                    theWriter.println("        if (currentStack) {");
                    theWriter.println("         goto " + theLabelPrefix + theIf.getOpcodeAddress() + theIf.getJumpOffset());
                    theWriter.println("        }");
                    theWriter.println("    }");
                } else if (theInstruction instanceof BytecodeInstructionFCMP) {
                    BytecodeInstructionFCMP theCmp = (BytecodeInstructionFCMP) theInstruction;
                    theWriter.println("    {");
                    theWriter.println("        var temp2 = stack[stackOffset--];");
                    theWriter.println("        var temp1 = stack[stackOffset--];");
                    theWriter.println("        if (temp1 > temp2) {");
                    theWriter.println("         stack[++stackOffset] = 1;");
                    theWriter.println("        } else if (temp1 < temp2) {");
                    theWriter.println("         stack[++stackOffset] = -1;");
                    theWriter.println("        } else {");
                    theWriter.println("         stack[++stackOffset] = 0;");
                    theWriter.println("        }");
                    theWriter.println("    }");
                } else if (theInstruction instanceof BytecodeInstructionGenericADD) {
                    BytecodeInstructionGenericADD theAdd = (BytecodeInstructionGenericADD) theInstruction;
                    theWriter.println("    {");
                    theWriter.println("        var temp = stack[stackOffset--] + stack[stackOffset--];");
                    theWriter.println("        stack[++stackOffset] = temp;");
                    theWriter.println("    }");
                } else if (theInstruction instanceof BytecodeInstructionGenericDIV) {
                    BytecodeInstructionGenericDIV theDiv = (BytecodeInstructionGenericDIV) theInstruction;
                    theWriter.println("    {");
                    theWriter.println("        var temp = Math.floor(stack[stackOffset-1] / stack[stackOffset]);");
                    theWriter.println("        stackOffset -= 1;");
                    theWriter.println("        stack[stackOffset] = temp;");
                    theWriter.println("    }");
                } else if (theInstruction instanceof BytecodeInstructionGenericMUL) {
                    BytecodeInstructionGenericMUL theDiv = (BytecodeInstructionGenericMUL) theInstruction;
                    theWriter.println("    {");
                    theWriter.println("        var temp = stack[stackOffset-1] * stack[stackOffset];");
                    theWriter.println("        stackOffset -= 1;");
                    theWriter.println("        stack[stackOffset] = temp;");
                    theWriter.println("    }");
                } else if (theInstruction instanceof BytecodeInstructionGenericSUB) {
                    BytecodeInstructionGenericSUB theDiv = (BytecodeInstructionGenericSUB) theInstruction;
                    theWriter.println("    {");
                    theWriter.println("        var temp = stack[stackOffset-1] - stack[stackOffset];");
                    theWriter.println("        stackOffset -= 1;");
                    theWriter.println("        stack[stackOffset] = temp;");
                    theWriter.println("    }");
                } else if (theInstruction instanceof BytecodeInstructionGenericRETURN) {
                    BytecodeInstructionGenericRETURN theReturn = (BytecodeInstructionGenericRETURN) theInstruction;
                    theWriter.println("    return stack[stackOffset];");
                } else if (theInstruction instanceof BytecodeInstructionARETURN) {
                    BytecodeInstructionARETURN theReturn = (BytecodeInstructionARETURN) theInstruction;
                    theWriter.println("    return stack[stackOffset];");
                } else if (theInstruction instanceof BytecodeInstructionATHROW) {
                    BytecodeInstructionATHROW theThrow = (BytecodeInstructionATHROW) theInstruction;
                    theWriter.println("    throw stack[stackOffset];");
                } else if (theInstruction instanceof BytecodeInstructionFCONST) {
                    BytecodeInstructionFCONST theConst = (BytecodeInstructionFCONST) theInstruction;
                    theWriter.println("    stack[++stackOffset] = " + theConst.getFloatValue() + ";");
                } else if (theInstruction instanceof BytecodeInstructionICONST) {
                    BytecodeInstructionICONST theConst = (BytecodeInstructionICONST) theInstruction;
                    theWriter.println("    stack[++stackOffset] = " + theConst.getIntConst() + ";");
                } else if (theInstruction instanceof BytecodeInstructionI2F) {
                    theWriter.println("    {");
                    theWriter.println("        var theInt = stack[stackOffset--];");
                    theWriter.println("        stack[stackOffset++] = theInt;");
                    theWriter.println("    }");
                } else if (theInstruction instanceof BytecodeInstructionLDC) {
                    BytecodeInstructionLDC theLoad = (BytecodeInstructionLDC) theInstruction;
                    BytecodeConstant theConstant = theLoad.constant();
                    if (theConstant instanceof BytecodeFloatConstant) {
                        BytecodeFloatConstant theFloat = (BytecodeFloatConstant) theConstant;
                        theWriter.println("    stack[++stackOffset] = " + theFloat.getValue() + ";");
                    } else if (theConstant instanceof BytecodeStringConstant) {
                        BytecodeStringConstant theStr = (BytecodeStringConstant) theConstant;
                        String theValue = theStr.getValue().stringValue();
                        theWriter.println("    stack[++stackOffset] =  '" + theValue + "';");
                    } else {
                        throw new IllegalStateException("Unsupported constant : " + theConstant);
                    }
                } else if (theInstruction instanceof BytecodeInstructionGOTO) {
                    BytecodeInstructionGOTO theGoto = (BytecodeInstructionGOTO) theInstruction;
                    BytecodeOpcodeAddress theTarget = theGoto.getJumpAddress();
                    theWriter.println("    goto " + theLabelPrefix + theTarget.getAddress());
                } else if (theInstruction instanceof BytecodeInstructionIFCOND) {
                    BytecodeInstructionIFCOND theCond = (BytecodeInstructionIFCOND) theInstruction;
                    BytecodeOpcodeAddress theTarget = theCond.getJumpAddress();
                    theWriter.println("    {");
                    theWriter.println("        var theValue = stack[stackOffset--];");
                    switch (theCond.getType()) {
                        case eq:
                            theWriter.println("        if (theValue == 0) {");
                            theWriter.println("         goto " + theLabelPrefix + theTarget.getAddress() + ";");
                            theWriter.println("        }");
                            break;
                        case gt:
                            theWriter.println("        if (theValue > 0) {");
                            theWriter.println("         goto " + theLabelPrefix + theTarget.getAddress() + ";");
                            theWriter.println("        }");
                            break;
                        case le:
                            theWriter.println("        if (theValue <= 0) {");
                            theWriter.println("         goto " + theLabelPrefix + theTarget.getAddress());
                            theWriter.println("        }");
                            break;
                        case ge:
                            theWriter.println("        if (theValue >= 0) {");
                            theWriter.println("         goto " + theLabelPrefix + theTarget.getAddress() + ";");
                            theWriter.println("        }");
                            break;
                        case lt:
                            theWriter.println("        if (theValue < 0) {");
                            theWriter.println("         goto " + theLabelPrefix + theTarget.getAddress() + ";");
                            theWriter.println("        }");
                            break;
                        case ne:
                            theWriter.println("        if (theValue != 0) {");
                            theWriter.println("         goto " + theLabelPrefix + theTarget.getAddress() + ";");
                            theWriter.println("        }");
                            break;
                    }
                    theWriter.println("    }");
                } else {
                    throw new IllegalStateException("Cannot compile " + theInstruction);
                }
            }

            theWriter.println("}");
        }));

        theWriter.flush();

        return theStrWriter.toString();
    }
}