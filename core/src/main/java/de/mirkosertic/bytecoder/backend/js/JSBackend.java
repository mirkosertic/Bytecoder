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

import de.mirkosertic.bytecoder.annotations.OverrideParentClass;
import de.mirkosertic.bytecoder.core.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class JSBackend {

    private String typeRefToString(BytecodeTypeRef aTypeRef) {
        if (aTypeRef.isPrimitive()) {
            BytecodePrimitiveTypeRef thePrimitive = (BytecodePrimitiveTypeRef) aTypeRef;
            return thePrimitive.toString();
        }
        if (aTypeRef.isArray()) {
            BytecodeArrayTypeRef theRef = (BytecodeArrayTypeRef) aTypeRef;
            return "A" + theRef.getDepth() + typeRefToString(theRef.getType());
        }
        BytecodeObjectTypeRef theObjectRef = (BytecodeObjectTypeRef) aTypeRef;
        return theObjectRef.name().replace(".", "");
    }

    public String toMethodName(String aMethodName, BytecodeMethodSignature aSignature) {
        String theName = aMethodName.replace("<", "").replace(">", "");
        for (BytecodeTypeRef theTypeRef : aSignature.getArguments()) {
            theName += typeRefToString(theTypeRef);
        }
        return theName;
    }

    public String toClassName(BytecodeObjectTypeRef aTypeRef) {
        return aTypeRef.name().replace(".","_");
    }

    public String toClassName(BytecodeClassinfoConstant aTypeRef) {
        return aTypeRef.getConstant().stringValue().replace("/","_");
    }

    public String generateJumpCoodeFor(BytecodeProgramJumps aJumps, BytecodeOpcodeAddress aSource, BytecodeOpcodeAddress aTarget) {
        BytecodeProgramJumps.Range theRange = aJumps.findClosestRangeToJumpFrom(aSource, aTarget);
        if (aTarget.isAfter(aSource)) {
            return "break " + theRange.rangeName();
        }
        return "continue " + theRange.rangeName();
    }

    private String getOverriddenParentClassFor(BytecodeClass aBytecodeClass) {
        BytecodeAnnotation theDelegatesTo = aBytecodeClass.getAnnotations().getAnnotationByType(OverrideParentClass.class.getName());
        if (theDelegatesTo != null) {
            BytecodeAnnotation.ElementValue theParentOverride = (BytecodeAnnotation.ClassElementValue) theDelegatesTo.getElementValueByName("parentClass");
            return theParentOverride.stringValue().replace("/",".");
        }
        return null;
    }

    public String generateCodeFor(BytecodeLinkerContext aLinkerContext) {

        StringWriter theStrWriter = new StringWriter();
        final PrintWriter theWriter = new PrintWriter(theStrWriter);

        aLinkerContext.forEachClass(aEntry -> {

            final String theOverriddenParentClassName = getOverriddenParentClassFor(aEntry.getValue().getBytecodeClass());

            String theJSClassName = toClassName(aEntry.getKey());

            theWriter.println("var " + theJSClassName + " = {");

            theWriter.println("    resolveVirtuslMethod : function(aIdentifier) {");
            aEntry.getValue().forEachVirtualMethod(aVirtualMethod -> {
                BytecodeLinkedClass.LinkTarget theLinkTarget = aVirtualMethod.getValue();
                theWriter.println("        if (aIdentifier == " + aVirtualMethod.getKey().getIdentifier() + ") {");
                theWriter.println("            return " + toClassName(theLinkTarget.getTargetType()) + "." + toMethodName(theLinkTarget.getTargetMethod().getName().stringValue(), theLinkTarget.getTargetMethod().getSignature()));
                theWriter.println("        }");
            });
            theWriter.println("        throw {type: 'unknown virtual method'}");
            theWriter.println("    },");
            theWriter.println();

            aEntry.getValue().forEachMethod(aMethod -> {

                BytecodeCodeAttributeInfo theCode = aMethod.getCode(aEntry.getValue().getBytecodeClass());
                BytecodeMethodSignature theCurrentMethodSignature = aMethod.getSignature();
                StringBuffer theArguments = new StringBuffer();
                if (!aMethod.getAccessFlags().isStatic()) {
                    theArguments.append("thisRef");
                }
                for (int i=1;i<=theCurrentMethodSignature.getArguments().length;i++) {
                    if (theArguments.length() > 0) {
                        theArguments.append(",");
                    }
                    theArguments.append("p" + i);
                }
                theWriter.println("    " + toMethodName(aMethod.getName().stringValue(), theCurrentMethodSignature) + " : function(" + theArguments.toString() + ") {");
                for (int i=1;i<=theCode.getMaxLocals();i++) {
                    theWriter.println("        var local" + i+";");
                }
                theWriter.println("        var stack = [" + theCode.getMaxStack() + "];");
                theWriter.println("        var stackOffset = -1;");

                // Push parameters to local variables
                for (int i=1;i<=theCurrentMethodSignature.getArguments().length;i++) {
                    theWriter.println("        local" + i + " = p" + i +";");
                }

                BytecodeProgram theProgram = theCode.getProgramm();
                BytecodeProgramJumps theJumps = theProgram.buildJumps();

                int theBraceCounter = 0;
                String theInset = "        ";
                theWriter.println("        // Begin method code");
                for (BytecodeInstruction theInstruction : theCode.getProgramm().getInstructions()) {

                    List<BytecodeProgramJumps.Range> theStartRangesAt = theJumps.startRangesAt(theInstruction.getOpcodeAddress());
                    for (BytecodeProgramJumps.Range theRange : theStartRangesAt) {
                        theWriter.println(theInset + theRange.rangeName() + ": {");
                        theInset += "    ";
                        theBraceCounter++;
                    }

                    List<BytecodeProgramJumps.Range> theEndRangesAt = theJumps.endRangesAt(theInstruction.getOpcodeAddress());
                    for (BytecodeProgramJumps.Range theRange : theEndRangesAt) {
                        theBraceCounter--;
                        theInset = theInset.substring("    ".length());
                        theWriter.println(theInset+ "}");
                    }

                    theWriter.println(theInset + "// Address " + theInstruction.getOpcodeAddress().getAddress());

                    if (theInstruction instanceof BytecodeInstructionRETURN) {
                        theWriter.println(theInset + "return;");
                    } else if (theInstruction instanceof BytecodeInstructionDUP) {
                        BytecodeInstructionDUP theDup = (BytecodeInstructionDUP) theInstruction;

                        theWriter.println(theInset + "{");
                        theWriter.println(theInset + "  var top = stack[stackOffset];");
                        theWriter.println(theInset + "  stack[++stackOffset] = top;");
                        theWriter.println(theInset + "}");
                    } else if (theInstruction instanceof BytecodeInstructionNEW) {
                        BytecodeInstructionNEW theNew = (BytecodeInstructionNEW) theInstruction;

                        BytecodeClassinfoConstant theConstant = theNew.getClassInfoForObjectToCreate();
                        theWriter.println(theInset + "// Create new object " + theConstant.getConstant().stringValue());
                        theWriter.println(theInset + "stack[++stackOffset] = {data: {}, clazz: " + toClassName(theConstant)+ "};");

                    } else if (theInstruction instanceof BytecodeInstructionINVOKESPECIAL) {
                        BytecodeInstructionINVOKESPECIAL theInvokeSpecial = (BytecodeInstructionINVOKESPECIAL) theInstruction;

                        BytecodeMethodRefConstant theConstant = theInvokeSpecial.getMethodReference();

                        BytecodeNameAndTypeConstant theMethodRef = theConstant.getNameAndTypeIndex().getNameAndType();
                        BytecodeMethodSignature theSig = theMethodRef.getDescriptorIndex().methodSignature();
                        BytecodeUtf8Constant theName = theMethodRef.getNameIndex().getName();
                        theWriter.println(theInset + "{");
                        BytecodeTypeRef[] theInvokeArguments = theSig.getArguments();
                        for (int i=theInvokeArguments.length;i>0;i--) {
                            theWriter.println(theInset + "  var arg"+i+" = stack[stackOffset--];");
                        }
                        theWriter.println(theInset + "  var callsite = stack[stackOffset--];");
                        if (theSig.getReturnType().isVoid()) {
                            theWriter.print(theInset + "  ");
                        } else {
                            theWriter.print(theInset + "  stack[++stackOffset] = ");
                        }

                        if (theOverriddenParentClassName != null) {
                            theWriter.print(theOverriddenParentClassName.replace(".","_") + "." + toMethodName(theName.stringValue(), theSig) + "(callsite");
                        } else {
                            theWriter.print(toClassName(theConstant.getClassIndex().getClassConstant()) + "." + toMethodName(theName.stringValue(), theSig) + "(callsite");
                        }
                        for (int i=1;i<=theInvokeArguments.length;i++) {
                            theWriter.print(",");
                            theWriter.print("arg" + i);
                        }
                        theWriter.println(");");
                        theWriter.println(theInset + "}");

                    } else if (theInstruction instanceof BytecodeInstructionINVOKEVIRTUAL) {
                        BytecodeInstructionINVOKEVIRTUAL theVirtualInvoke = (BytecodeInstructionINVOKEVIRTUAL) theInstruction;

                        BytecodeMethodRefConstant theMethodRefConstant = theVirtualInvoke.getMethodDescriptor();

                        BytecodeClassinfoConstant theClassConstant = theMethodRefConstant.getClassIndex().getClassConstant();
                        BytecodeNameAndTypeConstant theMethodRef = theMethodRefConstant.getNameAndTypeIndex().getNameAndType();
                        BytecodeMethodSignature theSig = theMethodRef.getDescriptorIndex().methodSignature();
                        BytecodeUtf8Constant theName = theMethodRef.getNameIndex().getName();

                        BytecodeVirtualMethodIdentifier theIdentifier = aLinkerContext.getMethodCollection().toIdentifier(theName.stringValue(), theSig);

                        theWriter.println(theInset + "{");
                        BytecodeTypeRef[] theInvokeArguments = theSig.getArguments();
                        for (int i=theInvokeArguments.length;i>0;i--) {
                            theWriter.println(theInset + "  var arg"+i+" = stack[stackOffset--];");
                        }
                        theWriter.println(theInset + "  var callsite = stack[stackOffset--];");
                        if (theSig.getReturnType().isVoid()) {
                            theWriter.print(theInset + "  ");
                        } else {
                            theWriter.print(theInset + "  stack[++stackOffset] = ");
                        }

                        theWriter.println(theInset + "  callsite.clazz.resolveVirtualMethod(" + theIdentifier.getIdentifier() + ")(callsite");
                        for (int i=1;i<=theInvokeArguments.length;i++) {
                            theWriter.print(",");
                            theWriter.print("arg" + i);
                        }
                        theWriter.println(");");
                        theWriter.println(theInset + "}");
                    }else if (theInstruction instanceof BytecodeInstructionINVOKESTATIC) {
                        BytecodeInstructionINVOKESTATIC theStaticInvoke = (BytecodeInstructionINVOKESTATIC) theInstruction;

                        BytecodeMethodRefConstant theMethodRefConstant = theStaticInvoke.getMethodRefConstant();
                        BytecodeClassinfoConstant theClassConstant = theMethodRefConstant.getClassIndex().getClassConstant();
                        BytecodeNameAndTypeConstant theMethodRef = theMethodRefConstant.getNameAndTypeIndex().getNameAndType();
                        BytecodeMethodSignature theSig = theMethodRef.getDescriptorIndex().methodSignature();
                        BytecodeUtf8Constant theName = theMethodRef.getNameIndex().getName();
                        theWriter.println(theInset + "{");
                        BytecodeTypeRef[] theInvokeArguments = theSig.getArguments();
                        for (int i=theInvokeArguments.length;i>0;i--) {
                            theWriter.println(theInset + "  var arg"+i+" = stack[stackOffset--];");
                        }
                        if (theSig.getReturnType().isVoid()) {
                            theWriter.print(theInset + "  ");
                        } else {
                            theWriter.print(theInset + "  stack[++stackOffset] = ");
                        }
                        theWriter.print(toClassName((theClassConstant)) + "." + toMethodName(theName.stringValue(), theSig) +"(");
                        for (int i=1;i<=theInvokeArguments.length;i++) {
                            if (i>1) {
                                theWriter.print(",");
                            }
                            theWriter.print("arg" + i);
                        }
                        theWriter.println(");");
                        theWriter.println(theInset + "}");
                    } else if (theInstruction instanceof BytecodeInstructionBIPUSH) {
                        BytecodeInstructionBIPUSH thePush = (BytecodeInstructionBIPUSH) theInstruction;
                        theWriter.println(theInset + "stack[++stackOffset] = " + thePush.getValue() + ";");
                    } else if (theInstruction instanceof BytecodeInstructionGenericLOAD) {
                        BytecodeInstructionGenericLOAD theLoad = (BytecodeInstructionGenericLOAD) theInstruction;
                        theWriter.println(theInset + "stack[++stackOffset] = local" + (theLoad.getIndex() + 1) + ";");
                    } else if (theInstruction instanceof BytecodeInstructionGenericSTORE) {
                        BytecodeInstructionGenericSTORE theStore = (BytecodeInstructionGenericSTORE) theInstruction;
                        theWriter.println(theInset + "local" + (theStore.getVariableIndex() + 1)+" = stack[stackOffset--];");
                    } else if (theInstruction instanceof BytecodeInstructionASTORE) {
                        BytecodeInstructionASTORE theStore = (BytecodeInstructionASTORE) theInstruction;
                        theWriter.println(theInset + "local" + (theStore.getVariableIndex() + 1)+" = stack[stackOffset--];");
                    } else if (theInstruction instanceof BytecodeInstructionCHECKCAST) {
                        BytecodeInstructionCHECKCAST theCheckCast = (BytecodeInstructionCHECKCAST) theInstruction;
                        BytecodeClassinfoConstant theConstant = theCheckCast.getTypeCheck();
                        theWriter.println(theInset + "// Checkcast ignored at this place for type " + theConstant.getConstant().stringValue());
                    } else if (theInstruction instanceof BytecodeInstructionACONSTNULL) {
                        BytecodeInstructionACONSTNULL theNullConst = (BytecodeInstructionACONSTNULL) theInstruction;
                        theWriter.println(theInset + "stack[++stackOffset] = null;");
                    } else if (theInstruction instanceof BytecodeInstructionALOAD) {
                        BytecodeInstructionALOAD theStore = (BytecodeInstructionALOAD) theInstruction;
                        theWriter.println(theInset + "stack[++stackOffset] = local" + (theStore.getVariableIndex() + 1) + ";");
                    } else if (theInstruction instanceof BytecodeInstructionLCMP) {
                        BytecodeInstructionLCMP theCmp = (BytecodeInstructionLCMP) theInstruction;
                        theWriter.println(theInset + "{");
                        theWriter.println(theInset + "  var temp2 = stack[stackOffset--];");
                        theWriter.println(theInset + "  var temp1 = stack[stackOffset--];");
                        theWriter.println(theInset + "  if (temp1 > temp2) {");
                        theWriter.println(theInset + "      stack[++stackOffset] = 1;");
                        theWriter.println(theInset + "  } else if (temp1 < temp2) {");
                        theWriter.println(theInset + "      stack[++stackOffset] = -1;");
                        theWriter.println(theInset + "  } else {");
                        theWriter.println(theInset + "      stack[++stackOffset] = 0;");
                        theWriter.println(theInset + "  }");
                        theWriter.println(theInset + "}");
                    } else if (theInstruction instanceof BytecodeInstructionIFNULL) {
                        BytecodeInstructionIFNULL theIf = (BytecodeInstructionIFNULL) theInstruction;
                        theWriter.println(theInset + "{");
                        theWriter.println(theInset + "  var currentStack = stack[stackOffset--];");
                        theWriter.println(theInset + "  if (!(currentStack)) {");
                        theWriter.println(theInset + "      " + generateJumpCoodeFor(theJumps, theInstruction.getOpcodeAddress(), theIf.getJumpTarget()));
                        theWriter.println(theInset + "  }");
                        theWriter.println(theInset + "}");
                    } else if (theInstruction instanceof BytecodeInstructionIFNONNULL) {
                        BytecodeInstructionIFNONNULL theIf = (BytecodeInstructionIFNONNULL) theInstruction;
                        theWriter.println(theInset + "{");
                        theWriter.println(theInset + "  var currentStack = stack[stackOffset--];");
                        theWriter.println(theInset + "  if (currentStack) {");
                        theWriter.println(theInset + "      " + generateJumpCoodeFor(theJumps, theInstruction.getOpcodeAddress(), theIf.getJumpTarget()));
                        theWriter.println(theInset + "  }");
                        theWriter.println(theInset + "}");
                    } else if (theInstruction instanceof BytecodeInstructionFCMP) {
                        BytecodeInstructionFCMP theCmp = (BytecodeInstructionFCMP) theInstruction;
                        theWriter.println(theInset + "{");
                        theWriter.println(theInset + "  var temp2 = stack[stackOffset--];");
                        theWriter.println(theInset + "  var temp1 = stack[stackOffset--];");
                        theWriter.println(theInset + "  if (temp1 > temp2) {");
                        theWriter.println(theInset + "      stack[++stackOffset] = 1;");
                        theWriter.println(theInset + "  } else if (temp1 < temp2) {");
                        theWriter.println(theInset + "      stack[++stackOffset] = -1;");
                        theWriter.println(theInset + "  } else {");
                        theWriter.println(theInset + "      stack[++stackOffset] = 0;");
                        theWriter.println(theInset + "  }");
                        theWriter.println(theInset + "}");
                    } else if (theInstruction instanceof BytecodeInstructionGenericADD) {
                        BytecodeInstructionGenericADD theAdd = (BytecodeInstructionGenericADD) theInstruction;
                        theWriter.println(theInset + "{");
                        theWriter.println(theInset + "  var temp = stack[stackOffset--] + stack[stackOffset--];");
                        theWriter.println(theInset + "  stack[++stackOffset] = temp;");
                        theWriter.println(theInset + "}");
                    } else if (theInstruction instanceof BytecodeInstructionGenericDIV) {
                        BytecodeInstructionGenericDIV theDiv = (BytecodeInstructionGenericDIV) theInstruction;
                        theWriter.println(theInset + "{");
                        theWriter.println(theInset + "  var temp = Math.floor(stack[stackOffset-1] / stack[stackOffset]);");
                        theWriter.println(theInset + "  stackOffset -= 1;");
                        theWriter.println(theInset + "  stack[stackOffset] = temp;");
                        theWriter.println(theInset + "}");
                    } else if (theInstruction instanceof BytecodeInstructionGenericMUL) {
                        BytecodeInstructionGenericMUL theDiv = (BytecodeInstructionGenericMUL) theInstruction;
                        theWriter.println(theInset + "{");
                        theWriter.println(theInset + "  var temp = stack[stackOffset-1] * stack[stackOffset];");
                        theWriter.println(theInset + "  stackOffset -= 1;");
                        theWriter.println(theInset + "  stack[stackOffset] = temp;");
                        theWriter.println(theInset + "}");
                    } else if (theInstruction instanceof BytecodeInstructionGenericSUB) {
                        BytecodeInstructionGenericSUB theSub = (BytecodeInstructionGenericSUB) theInstruction;
                        theWriter.println(theInset + "{");
                        theWriter.println(theInset + "  var temp = stack[stackOffset-1] - stack[stackOffset];");
                        theWriter.println(theInset + "  stackOffset -= 1;");
                        theWriter.println(theInset + "  stack[stackOffset] = temp;");
                        theWriter.println(theInset + "}");
                    } else if (theInstruction instanceof BytecodeInstructionGenericNEG) {
                        BytecodeInstructionGenericNEG theNeg = (BytecodeInstructionGenericNEG) theInstruction;
                        theWriter.println(theInset + "{");
                        theWriter.println(theInset + "  var temp = -stack[stackOffset--];");
                        theWriter.println(theInset + "  stack[++stackOffset] = temp;");
                        theWriter.println(theInset + "}");
                    } else if (theInstruction instanceof BytecodeInstructionGenericRETURN) {
                        BytecodeInstructionGenericRETURN theReturn = (BytecodeInstructionGenericRETURN) theInstruction;
                        theWriter.println(theInset + "return stack[stackOffset];");
                    } else if (theInstruction instanceof BytecodeInstructionARETURN) {
                        BytecodeInstructionARETURN theReturn = (BytecodeInstructionARETURN) theInstruction;
                        theWriter.println(theInset + "return stack[stackOffset];");
                    } else if (theInstruction instanceof BytecodeInstructionATHROW) {
                        BytecodeInstructionATHROW theThrow = (BytecodeInstructionATHROW) theInstruction;
                        theWriter.println(theInset + "throw stack[stackOffset];");
                    } else if (theInstruction instanceof BytecodeInstructionFCONST) {
                        BytecodeInstructionFCONST theConst = (BytecodeInstructionFCONST) theInstruction;
                        theWriter.println(theInset + "stack[++stackOffset] = " + theConst.getFloatValue() + ";");
                    } else if (theInstruction instanceof BytecodeInstructionICONST) {
                        BytecodeInstructionICONST theConst = (BytecodeInstructionICONST) theInstruction;
                        theWriter.println(theInset + "stack[++stackOffset] = " + theConst.getIntConst() + ";");
                    } else if (theInstruction instanceof BytecodeInstructionI2F) {
                        theWriter.println(theInset + "{");
                        theWriter.println(theInset + "  var theInt = stack[stackOffset--];");
                        theWriter.println(theInset + "  stack[++stackOffset] = theInt;");
                        theWriter.println(theInset + "}");
                    } else if (theInstruction instanceof BytecodeInstructionLDC) {
                        BytecodeInstructionLDC theLoad = (BytecodeInstructionLDC) theInstruction;
                        BytecodeConstant theConstant = theLoad.constant();
                        if (theConstant instanceof BytecodeFloatConstant) {
                            BytecodeFloatConstant theFloat = (BytecodeFloatConstant) theConstant;
                            theWriter.println(theInset + "stack[++stackOffset] = " + theFloat.getFloatValue() + ";");
                        } else if (theConstant instanceof BytecodeStringConstant) {
                            BytecodeStringConstant theStr = (BytecodeStringConstant) theConstant;
                            String theValue = theStr.getValue().stringValue();
                            theWriter.println(theInset + "stack[++stackOffset] =  '" + theValue + "';");
                        } else {
                            throw new IllegalStateException("Unsupported constant : " + theConstant);
                        }
                    } else if (theInstruction instanceof BytecodeInstructionGOTO) {
                        BytecodeInstructionGOTO theGoto = (BytecodeInstructionGOTO) theInstruction;
                        theWriter.println(theInset + "      " + generateJumpCoodeFor(theJumps, theInstruction.getOpcodeAddress(), theGoto.getJumpAddress()));
                    } else if (theInstruction instanceof BytecodeInstructionICMP) {
                        BytecodeInstructionICMP theCond = (BytecodeInstructionICMP) theInstruction;

                        BytecodeOpcodeAddress theTarget = theCond.getJumpAddress();
                        theWriter.println(theInset + "{");
                        theWriter.println(theInset + "  var theValue2 = stack[stackOffset--];");
                        theWriter.println(theInset + "  var theValue1 = stack[stackOffset--];");
                        switch (theCond.getType()) {
                        case eq:
                            theWriter.println(theInset + "  if (theValue1 == theValue2) {");
                            theWriter.println(theInset + "      " + generateJumpCoodeFor(theJumps, theInstruction.getOpcodeAddress(), theTarget));
                            theWriter.println(theInset + "  }");
                            break;
                        case gt:
                            theWriter.println(theInset + "  if (theValue1 > theValue2) {");
                            theWriter.println(theInset + "      " + generateJumpCoodeFor(theJumps, theInstruction.getOpcodeAddress(), theTarget));
                            theWriter.println(theInset + "  }");
                            break;
                        case le:
                            theWriter.println(theInset + "  if (theValue1 <= theValue2) {");
                            theWriter.println(theInset + "      " + generateJumpCoodeFor(theJumps, theInstruction.getOpcodeAddress(), theTarget));
                            theWriter.println(theInset + "  }");
                            break;
                        case ge:
                            theWriter.println(theInset + "  if (theValue1 >= theValue2) {");
                            theWriter.println(theInset + "      " + generateJumpCoodeFor(theJumps, theInstruction.getOpcodeAddress(), theTarget));
                            theWriter.println(theInset + "  }");
                            break;
                        case lt:
                            theWriter.println(theInset + "  if (theValue1 < theValue2) {");
                            theWriter.println(theInset + "      " + generateJumpCoodeFor(theJumps, theInstruction.getOpcodeAddress(), theTarget));
                            theWriter.println(theInset + "  }");
                            break;
                        case ne:
                            theWriter.println(theInset + "  if (theValue1 != theValue2) {");
                            theWriter.println(theInset + "      " + generateJumpCoodeFor(theJumps, theInstruction.getOpcodeAddress(), theTarget));
                            theWriter.println(theInset + "  }");
                            break;
                        }
                        theWriter.println(theInset + "}");

                    } else if (theInstruction instanceof BytecodeInstructionIFCOND) {
                        BytecodeInstructionIFCOND theCond = (BytecodeInstructionIFCOND) theInstruction;
                        BytecodeOpcodeAddress theTarget = theCond.getJumpAddress();
                        theWriter.println(theInset + "{");
                        theWriter.println(theInset + "  var theValue = stack[stackOffset--];");
                        switch (theCond.getType()) {
                        case eq:
                            theWriter.println(theInset + "  if (theValue == 0) {");
                            theWriter.println(theInset + "      " + generateJumpCoodeFor(theJumps, theInstruction.getOpcodeAddress(), theTarget));
                            theWriter.println(theInset + "  }");
                            break;
                        case gt:
                            theWriter.println(theInset + "  if (theValue > 0) {");
                            theWriter.println(theInset + "      " + generateJumpCoodeFor(theJumps, theInstruction.getOpcodeAddress(), theTarget));
                            theWriter.println(theInset + "  }");
                            break;
                        case le:
                            theWriter.println(theInset + "  if (theValue <= 0) {");
                            theWriter.println(theInset + "      " + generateJumpCoodeFor(theJumps, theInstruction.getOpcodeAddress(), theTarget));
                            theWriter.println(theInset + "  }");
                            break;
                        case ge:
                            theWriter.println(theInset + "  if (theValue >= 0) {");
                            theWriter.println(theInset + "      " + generateJumpCoodeFor(theJumps, theInstruction.getOpcodeAddress(), theTarget));
                            theWriter.println(theInset + "  }");
                            break;
                        case lt:
                            theWriter.println(theInset + "  if (theValue < 0) {");
                            theWriter.println(theInset + "      " + generateJumpCoodeFor(theJumps, theInstruction.getOpcodeAddress(), theTarget));
                            theWriter.println(theInset + "  }");
                            break;
                        case ne:
                            theWriter.println(theInset + "  if (theValue != 0) {");
                            theWriter.println(theInset + "      " + generateJumpCoodeFor(theJumps, theInstruction.getOpcodeAddress(), theTarget));
                            theWriter.println(theInset + "  }");
                            break;
                        }
                        theWriter.println(theInset + "}");
                    } else {
                        throw new IllegalStateException("Cannot compile " + theInstruction);
                    }
                }

                theWriter.println("    },");
            });

            theWriter.println("}");
            theWriter.println();
        });

        theWriter.flush();

        return theStrWriter.toString();
    }
}