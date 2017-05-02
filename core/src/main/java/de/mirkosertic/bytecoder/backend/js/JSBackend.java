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

import de.mirkosertic.bytecoder.core.*;

import java.io.PrintWriter;
import java.io.StringWriter;

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

            theWriter.println("    // Begin method code");
            for (BytecodeInstruction theInstruction : theCode.getProgramm().getInstructions()) {
                if (theInstruction instanceof BytecodeInstructionRETURN) {
                    theWriter.println("    return;");
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
                    theWriter.print("       " + theName.stringValue()+"(");
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
                    theWriter.println("    local" + theStore.getIndex()+" = stack[stackOffset--];");
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
                } else if (theInstruction instanceof BytecodeInstructionFCONST) {
                    BytecodeInstructionFCONST theConst = (BytecodeInstructionFCONST) theInstruction;
                    theWriter.println("    stack[++stackOffset] = " + theConst.getValue() + ";");
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
                    } else {
                        throw new IllegalStateException("Unsupported constant : " + theConstant);
                    }
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