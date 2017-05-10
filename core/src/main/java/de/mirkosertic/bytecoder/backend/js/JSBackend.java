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

import de.mirkosertic.bytecoder.annotations.OverrideParentClass;
import de.mirkosertic.bytecoder.classlib.ExceptionRethrower;
import de.mirkosertic.bytecoder.classlib.java.lang.TThrowable;
import de.mirkosertic.bytecoder.core.BytecodeAnnotation;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeClass;
import de.mirkosertic.bytecoder.core.BytecodeClassinfoConstant;
import de.mirkosertic.bytecoder.core.BytecodeCodeAttributeInfo;
import de.mirkosertic.bytecoder.core.BytecodeConstant;
import de.mirkosertic.bytecoder.core.BytecodeExceptionTableEntry;
import de.mirkosertic.bytecoder.core.BytecodeFieldRefConstant;
import de.mirkosertic.bytecoder.core.BytecodeFloatConstant;
import de.mirkosertic.bytecoder.core.BytecodeInstruction;
import de.mirkosertic.bytecoder.core.BytecodeInstructionACONSTNULL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionALOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionARETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionASTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionATHROW;
import de.mirkosertic.bytecoder.core.BytecodeInstructionBIPUSH;
import de.mirkosertic.bytecoder.core.BytecodeInstructionCHECKCAST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDUP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionFCMP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionFCONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGETFIELD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGETSTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGOTO;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericADD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericDIV;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericLOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericMUL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericNEG;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericRETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSUB;
import de.mirkosertic.bytecoder.core.BytecodeInstructionI2F;
import de.mirkosertic.bytecoder.core.BytecodeInstructionICMP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionICONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFCOND;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFNONNULL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFNULL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINSTANCEOF;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESPECIAL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKEVIRTUAL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionLCMP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionLDC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNEW;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPUTFIELD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPUTSTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionRETURN;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethodRefConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeNameAndTypeConstant;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeProgram;
import de.mirkosertic.bytecoder.core.BytecodeProgramJumps;
import de.mirkosertic.bytecoder.core.BytecodeStringConstant;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeUtf8Constant;
import de.mirkosertic.bytecoder.core.BytecodeVirtualMethodIdentifier;

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

    public String generateJumpCodeFor(BytecodeProgramJumps aJumps, BytecodeOpcodeAddress aSource, BytecodeOpcodeAddress aTarget) {
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

        // Also link intrinsics required for code generation
        BytecodeLinkedClass theExceptionRethrower = aLinkerContext.linkClass(BytecodeObjectTypeRef.fromRuntimeClass(
                ExceptionRethrower.class));
        theExceptionRethrower.linkStaticMethod("registerExceptionOutcome", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {BytecodeObjectTypeRef.fromRuntimeClass(TThrowable.class)}));
        theExceptionRethrower.linkStaticMethod("getLastOutcomeOrNullAndReset", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(TThrowable.class), new BytecodeTypeRef[0]));

        StringWriter theStrWriter = new StringWriter();
        final PrintWriter theWriter = new PrintWriter(theStrWriter);
        theWriter.println("'use strict';");

        aLinkerContext.forEachClass(aEntry -> {

            if (aEntry.getValue().getBytecodeClass().getAccessFlags().isInterface()) {
                return;
            }

            final String theOverriddenParentClassName = getOverriddenParentClassFor(aEntry.getValue().getBytecodeClass());

            String theJSClassName = toClassName(aEntry.getKey());
            theWriter.println("var " + theJSClassName + " = {");

            if (!aEntry.getValue().getBytecodeClass().getAccessFlags().isInterface()) {

                theWriter.println("    staticFields : {");
                if (aEntry.getValue().hasClassInitializer()) {
                    theWriter.println("        classInitialized : false,");
                }
                aEntry.getValue().forEachStaticField(
                        aFieldEntry -> theWriter.println("        " + aFieldEntry.getKey() + " : null," ));
                theWriter.println("    },");
                theWriter.println();

                theWriter.println("    resolveVirtualMethod : function(aIdentifier) {");
                theWriter.println("        switch(aIdentifier) {");
                aEntry.getValue().forEachVirtualMethod(aVirtualMethod -> {
                    BytecodeLinkedClass.LinkedMethod theLinkTarget = aVirtualMethod.getValue();
                    theWriter.println("            case " + aVirtualMethod.getKey().getIdentifier() + ":");
                    theWriter.println("                return " + toClassName(theLinkTarget.getTargetType()) + "." + toMethodName(
                            theLinkTarget.getTargetMethod().getName().stringValue(),
                            theLinkTarget.getTargetMethod().getSignature()));
                });
                theWriter.println("            default:");
                theWriter.println("                throw {type: 'unknown virtual method'}");
                theWriter.println("        }");
                theWriter.println("    },");
                theWriter.println();

                theWriter.println("    emptyInstance : function() {");
                if (aEntry.getValue().hasClassInitializer()) {
                    theWriter.println("        if (" + theJSClassName + ".staticFields.classInitialized == false) {");
                    theWriter.println("            " + theJSClassName + ".clinit();");
                    theWriter.println("            " + theJSClassName + ".staticFields.classInitialized = true;");
                    theWriter.println("        }");
                }
                theWriter.println("        return {data: {");
                aEntry.getValue().forEachMemberField(aField -> theWriter.println("            " + aField.getKey() + " : null,"));
                theWriter.println("        }, clazz: " + toClassName(aEntry.getKey())+ "};");
                theWriter.println("    },");
                theWriter.println();

                theWriter.println("    thisIdentifier : function() {");
                theWriter.println("        return " + aEntry.getValue().getUniqueId());
                theWriter.println("    },");
                theWriter.println();

                theWriter.println("    instanceOfType : function(aType) {");
                theWriter.println("        switch(aType) {");

                for (BytecodeLinkedClass theType : aEntry.getValue().getImplementingTypes()) {
                    theWriter.println("            case " + theType.getUniqueId() +":");
                    theWriter.println("                return true;");
                }

                theWriter.println("            default:");
                theWriter.println("                return false;");
                theWriter.println("        }");
                theWriter.println("    },");
                theWriter.println();
            }

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
                if (aMethod.getAccessFlags().isStatic()) {
                    for (int i=1;i<=theCode.getMaxLocals();i++) {
                        if (i<=theCurrentMethodSignature.getArguments().length) {
                            theWriter.println("        var local" + i + " = p" + i + ";");
                        } else {
                            theWriter.println("        var local" + i + ";");
                        }
                    }
                } else {
                    theWriter.println("        var local1 = thisRef;");
                    for (int i=2;i<=theCode.getMaxLocals();i++) {
                        if (i-1 <=theCurrentMethodSignature.getArguments().length) {
                            theWriter.println("        var local" + i + " = p" + (i - 1) + ";");
                        } else {
                            theWriter.println("        var local" + i + ";");
                        }
                    }
                }
                theWriter.println("        var stack = [" + theCode.getMaxStack() + "];");
                theWriter.println("        var stackOffset = -1;");

                BytecodeProgram theProgram = theCode.getProgramm();
                BytecodeProgramJumps theJumps = theProgram.buildJumps(theCode.getExceptionTableEntries());

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
                        theWriter.println(theInset + "stack[++stackOffset] = " + toClassName(theConstant)+ ".emptyInstance();");

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
                        theWriter.println(theInset + "  var theLastException = de_mirkosertic_bytecoder_classlib_ExceptionRethrower.getLastOutcomeOrNullAndReset();");
                        theWriter.println(theInset + "  if (theLastException) {");

                        writeExceptionHandlerCode(aLinkerContext, theExceptionRethrower, theWriter, theCode, theProgram, theJumps,
                                theInset + "    ", theInvokeSpecial, "theLastException");

                        theWriter.println(theInset + "  }");
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

                        theWriter.print("callsite.clazz.resolveVirtualMethod(" + theIdentifier.getIdentifier() + ")(callsite");
                        for (int i=1;i<=theInvokeArguments.length;i++) {
                            theWriter.print(",");
                            theWriter.print("arg" + i);
                        }
                        theWriter.println(");");

                        theWriter.println(theInset + "  var theLastException = de_mirkosertic_bytecoder_classlib_ExceptionRethrower.getLastOutcomeOrNullAndReset();");
                        theWriter.println(theInset + "  if (theLastException) {");

                        writeExceptionHandlerCode(aLinkerContext, theExceptionRethrower, theWriter, theCode, theProgram, theJumps,
                                theInset + "    ", theVirtualInvoke, "theLastException");

                        theWriter.println(theInset + "  }");

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

                        theWriter.println(theInset + "  var theLastException = de_mirkosertic_bytecoder_classlib_ExceptionRethrower.getLastOutcomeOrNullAndReset();");
                        theWriter.println(theInset + "  if (theLastException) {");

                        writeExceptionHandlerCode(aLinkerContext, theExceptionRethrower, theWriter, theCode, theProgram, theJumps,
                                theInset + "    ", theStaticInvoke, "theLastException");

                        theWriter.println(theInset + "  }");

                        theWriter.println(theInset + "}");
                    } else if (theInstruction instanceof BytecodeInstructionINSTANCEOF) {
                        BytecodeInstructionINSTANCEOF theInstanceOf = (BytecodeInstructionINSTANCEOF) theInstruction;

                        BytecodeLinkedClass theLinkedClass = aLinkerContext.isLinkedOrNull(theInstanceOf.getTypeRef().getConstant());

                        theWriter.println(theInset + "{");
                        theWriter.println(theInset + "  var temp = stack[stackOffset--];");
                        theWriter.println(theInset + "  if (temp == null) {");
                        theWriter.println(theInset + "    stack[++stackOffset] = 0;");
                        theWriter.println(theInset + "  } else {");
                        theWriter.println(theInset + "    stack[++stackOffset] = temp.clazz.instanceOfType(" + theLinkedClass.getUniqueId() + ");");
                        theWriter.println(theInset + "  }");
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
                    } else if (theInstruction instanceof BytecodeInstructionPUTSTATIC) {
                        BytecodeInstructionPUTSTATIC thePut = (BytecodeInstructionPUTSTATIC) theInstruction;
                        BytecodeFieldRefConstant theConstant = thePut.getConstant();
                        BytecodeClassinfoConstant theClassName = theConstant.getClassIndex().getClassConstant();
                        BytecodeUtf8Constant theFieldName = theConstant.getNameAndTypeIndex().getNameAndType().getNameIndex().getName();
                        theWriter.println(theInset + toClassName(theClassName) + ".staticFields." + theFieldName.stringValue() + " = stack[stackOffset--];");
                    } else if (theInstruction instanceof BytecodeInstructionGETSTATIC) {
                        BytecodeInstructionGETSTATIC theGet = (BytecodeInstructionGETSTATIC) theInstruction;
                        BytecodeFieldRefConstant theConstant = theGet.getConstant();
                        BytecodeClassinfoConstant theClassName = theConstant.getClassIndex().getClassConstant();
                        BytecodeUtf8Constant theFieldName = theConstant.getNameAndTypeIndex().getNameAndType().getNameIndex()
                                .getName();
                        theWriter.println(
                                theInset + "stack[++stackOffset] = " + toClassName(theClassName) + ".staticFields." + theFieldName
                                        .stringValue() + ";");
                    } else if (theInstruction instanceof BytecodeInstructionPUTFIELD) {
                        BytecodeInstructionPUTFIELD theGet = (BytecodeInstructionPUTFIELD) theInstruction;
                        BytecodeFieldRefConstant theConstant = theGet.getFieldRefConstant();
                        BytecodeUtf8Constant theFieldName = theConstant.getNameAndTypeIndex().getNameAndType().getNameIndex()
                                .getName();
                        theWriter.println(theInset + "{");
                        theWriter.println(theInset + "  var theValue = stack[stackOffset--];");
                        theWriter.println(theInset + "  var theObjectRef = stack[stackOffset--];");
                        theWriter.println(
                                theInset + "  theObjectRef.data." + theFieldName.stringValue() + " = theValue;");
                        theWriter.println(theInset + "}");
                    } else if (theInstruction instanceof BytecodeInstructionGETFIELD) {
                        BytecodeInstructionGETFIELD theGet = (BytecodeInstructionGETFIELD) theInstruction;
                        BytecodeFieldRefConstant theConstant = theGet.getFieldRefConstant();
                        BytecodeUtf8Constant theFieldName = theConstant.getNameAndTypeIndex().getNameAndType().getNameIndex()
                                .getName();
                        theWriter.println(theInset + "{");
                        theWriter.println(theInset + "  var theObjectRef = stack[stackOffset--];");
                        theWriter.println(
                                theInset + "  stack[++stackOffset] = theObjectRef.data." + theFieldName.stringValue() + ";");
                        theWriter.println(theInset + "}");
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
                        theWriter.println(theInset + "      " + generateJumpCodeFor(theJumps, theInstruction.getOpcodeAddress(), theIf.getJumpTarget()));
                        theWriter.println(theInset + "  }");
                        theWriter.println(theInset + "}");
                    } else if (theInstruction instanceof BytecodeInstructionIFNONNULL) {
                        BytecodeInstructionIFNONNULL theIf = (BytecodeInstructionIFNONNULL) theInstruction;
                        theWriter.println(theInset + "{");
                        theWriter.println(theInset + "  var currentStack = stack[stackOffset--];");
                        theWriter.println(theInset + "  if (currentStack) {");
                        theWriter.println(theInset + "      " + generateJumpCodeFor(theJumps, theInstruction.getOpcodeAddress(), theIf.getJumpTarget()));
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

                        theWriter.println(theInset + "var theException = stack[stackOffset];");
                        writeExceptionHandlerCode(aLinkerContext, theExceptionRethrower, theWriter, theCode, theProgram, theJumps,
                                theInset, theThrow, "theException");

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
                        theWriter.println(theInset + generateJumpCodeFor(theJumps, theInstruction.getOpcodeAddress(), theGoto.getJumpAddress()) + ";");
                    } else if (theInstruction instanceof BytecodeInstructionICMP) {
                        BytecodeInstructionICMP theCond = (BytecodeInstructionICMP) theInstruction;

                        BytecodeOpcodeAddress theTarget = theCond.getJumpAddress();
                        theWriter.println(theInset + "{");
                        theWriter.println(theInset + "  var theValue2 = stack[stackOffset--];");
                        theWriter.println(theInset + "  var theValue1 = stack[stackOffset--];");
                        switch (theCond.getType()) {
                        case eq:
                            theWriter.println(theInset + "  if (theValue1 == theValue2) {");
                            theWriter.println(theInset + "      " + generateJumpCodeFor(theJumps, theInstruction.getOpcodeAddress(), theTarget));
                            theWriter.println(theInset + "  }");
                            break;
                        case gt:
                            theWriter.println(theInset + "  if (theValue1 > theValue2) {");
                            theWriter.println(theInset + "      " + generateJumpCodeFor(theJumps, theInstruction.getOpcodeAddress(), theTarget));
                            theWriter.println(theInset + "  }");
                            break;
                        case le:
                            theWriter.println(theInset + "  if (theValue1 <= theValue2) {");
                            theWriter.println(theInset + "      " + generateJumpCodeFor(theJumps, theInstruction.getOpcodeAddress(), theTarget));
                            theWriter.println(theInset + "  }");
                            break;
                        case ge:
                            theWriter.println(theInset + "  if (theValue1 >= theValue2) {");
                            theWriter.println(theInset + "      " + generateJumpCodeFor(theJumps, theInstruction.getOpcodeAddress(), theTarget));
                            theWriter.println(theInset + "  }");
                            break;
                        case lt:
                            theWriter.println(theInset + "  if (theValue1 < theValue2) {");
                            theWriter.println(theInset + "      " + generateJumpCodeFor(theJumps, theInstruction.getOpcodeAddress(), theTarget));
                            theWriter.println(theInset + "  }");
                            break;
                        case ne:
                            theWriter.println(theInset + "  if (theValue1 != theValue2) {");
                            theWriter.println(theInset + "      " + generateJumpCodeFor(theJumps, theInstruction.getOpcodeAddress(), theTarget));
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
                            theWriter.println(theInset + "      " + generateJumpCodeFor(theJumps, theInstruction.getOpcodeAddress(), theTarget));
                            theWriter.println(theInset + "  }");
                            break;
                        case gt:
                            theWriter.println(theInset + "  if (theValue > 0) {");
                            theWriter.println(theInset + "      " + generateJumpCodeFor(theJumps, theInstruction.getOpcodeAddress(), theTarget));
                            theWriter.println(theInset + "  }");
                            break;
                        case le:
                            theWriter.println(theInset + "  if (theValue <= 0) {");
                            theWriter.println(theInset + "      " + generateJumpCodeFor(theJumps, theInstruction.getOpcodeAddress(), theTarget));
                            theWriter.println(theInset + "  }");
                            break;
                        case ge:
                            theWriter.println(theInset + "  if (theValue >= 0) {");
                            theWriter.println(theInset + "      " + generateJumpCodeFor(theJumps, theInstruction.getOpcodeAddress(), theTarget));
                            theWriter.println(theInset + "  }");
                            break;
                        case lt:
                            theWriter.println(theInset + "  if (theValue < 0) {");
                            theWriter.println(theInset + "      " + generateJumpCodeFor(theJumps, theInstruction.getOpcodeAddress(), theTarget));
                            theWriter.println(theInset + "  }");
                            break;
                        case ne:
                            theWriter.println(theInset + "  if (theValue != 0) {");
                            theWriter.println(theInset + "      " + generateJumpCodeFor(theJumps, theInstruction.getOpcodeAddress(), theTarget));
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

    private void writeExceptionHandlerCode(BytecodeLinkerContext aLinkerContext, BytecodeLinkedClass aExceptionRethrower,
            PrintWriter aWriter, BytecodeCodeAttributeInfo aCode, BytecodeProgram aProgram, BytecodeProgramJumps aJumps,
            String aInset, BytecodeInstruction aInstruction, String aExceptionVariableName) {
        BytecodeExceptionTableEntry[] theActiveHandlers = aProgram.getActiveExceptionHandlers(aInstruction.getOpcodeAddress(), aCode.getExceptionTableEntries());
        if (theActiveHandlers.length == 0) {
            // Missing catch block
            aWriter.println(aInset + toClassName(aExceptionRethrower.getClassName()) + ".registerExceptionOutcomedemirkoserticbytecoderclasslibjavalangTThrowable(" + aExceptionVariableName + ");");
            aWriter.println(aInset + "return;");
        } else {
            for (BytecodeExceptionTableEntry theEntry : theActiveHandlers) {
                if (!theEntry.isFinally()) {
                    BytecodeClassinfoConstant theConstant = theEntry.getCatchType();
                    BytecodeLinkedClass theLinkedClass = aLinkerContext.isLinkedOrNull(theConstant.getConstant());
                    aWriter.println(aInset + "if (" + aExceptionVariableName + ".clazz.instanceOfType(" + theLinkedClass.getUniqueId() + ")) {");
                    BytecodeProgramJumps.Range theJumpRange = aJumps.findClosestRangeToJumpFrom(aInstruction.getOpcodeAddress(), theEntry.getHandlerPc());
                    aWriter.println(aInset + "    break " + theJumpRange.rangeName()+";");
                    aWriter.println(aInset + "    // should jump to " + theEntry.getHandlerPc().getAddress());
                    aWriter.println(aInset + "}");
                }
            }
            aWriter.println(aInset + toClassName(aExceptionRethrower.getClassName()) + ".registerExceptionOutcomedemirkoserticbytecoderclasslibjavalangTThrowable(" + aExceptionVariableName + ");");
            aWriter.println(aInset + "return;");
        }
    }
}