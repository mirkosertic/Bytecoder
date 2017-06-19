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
import java.util.Map;
import java.util.Set;

import de.mirkosertic.bytecoder.annotations.EmulatedByRuntime;
import de.mirkosertic.bytecoder.annotations.Import;
import de.mirkosertic.bytecoder.classlib.ExceptionRethrower;
import de.mirkosertic.bytecoder.classlib.java.lang.TArray;
import de.mirkosertic.bytecoder.classlib.java.lang.TClass;
import de.mirkosertic.bytecoder.classlib.java.lang.TString;
import de.mirkosertic.bytecoder.core.BytecodeAnnotation;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeClassinfoConstant;
import de.mirkosertic.bytecoder.core.BytecodeCodeAttributeInfo;
import de.mirkosertic.bytecoder.core.BytecodeControlFlowGraph;
import de.mirkosertic.bytecoder.core.BytecodeExceptionTableEntry;
import de.mirkosertic.bytecoder.core.BytecodeInstruction;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeProgram;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.ssa.Block;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableDescription;

public class JSSSACompilerBackend extends AbstractJSBackend {

    public JSSSACompilerBackend() {
    }

    @Override
    public String generateCodeFor(BytecodeLinkerContext aLinkerContext) {

        BytecodeLinkedClass theClassLinkedCass = aLinkerContext.linkClass(BytecodeObjectTypeRef.fromRuntimeClass(TClass.class));

        BytecodeLinkedClass theExceptionRethrower = aLinkerContext.linkClass(BytecodeObjectTypeRef.fromRuntimeClass(
                ExceptionRethrower.class));
        theExceptionRethrower.linkStaticMethod("registerExceptionOutcome", theRegisterExceptionOutcomeSignature);
        theExceptionRethrower.linkStaticMethod("getLastOutcomeOrNullAndReset", theGetLastExceptionOutcomeSignature);

        StringWriter theStrWriter = new StringWriter();
        final PrintWriter theWriter = new PrintWriter(theStrWriter);
        theWriter.println("'use strict';");

        theWriter.println();
        theWriter.println("var bytecoderGlobalMemory = [];");
        theWriter.println();

        theWriter.println("var bytecoder = {");

        theWriter.println();
        theWriter.println("     logDebug : function(aValue) { ");
        theWriter.println("         console.log(aValue);");
        theWriter.println("     }, ");

        theWriter.println();
        theWriter.println("     logByteArrayAsString : function(aArray) { ");
        theWriter.println("         var theResult = '';");
        theWriter.println("         for (var i=0;i<aArray.data.length;i++) {");
        theWriter.println("             theResult += String.fromCharCode(aArray.data[i]);");
        theWriter.println("         }");
        theWriter.println("         console.log(theResult);");
        theWriter.println("     }, ");
        theWriter.println();

        theWriter.println("     newString : function(aByteArray) { ");

        BytecodeObjectTypeRef theStringTypeRef = BytecodeObjectTypeRef.fromRuntimeClass(TString.class);
        BytecodeObjectTypeRef theArrayTypeRef = BytecodeObjectTypeRef.fromRuntimeClass(TArray.class);

        BytecodeMethodSignature theStringConstructorSignature = new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID,
                new BytecodeTypeRef[]{new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.BYTE, 1)});

        // Construct a String
        theWriter.println("          var theNewString = " + JSWriterUtils.toClassName(theStringTypeRef) + ".emptyInstance();");
        theWriter.println("          var theBytes = " + JSWriterUtils.toClassName(theArrayTypeRef) + ".emptyInstance();");
        theWriter.println("          theBytes.data = aByteArray;");
        theWriter.println("          " + JSWriterUtils.toClassName(theStringTypeRef) + "." + JSWriterUtils.toMethodName("init", theStringConstructorSignature) + "(theNewString, theBytes);");
        theWriter.println("          return theNewString;");
        theWriter.println("     },");
        theWriter.println();
        theWriter.println("     newMultiArray : function(aDimensions, aDefault) {");
        theWriter.println("         var theLength = aDimensions[0];");
        theWriter.println("         var theArray = bytecoder.newArray(theLength, aDefault);");
        theWriter.println("         if (aDimensions.length > 1) {");
        theWriter.println("             var theNewDimensions = aDimensions.slice(0);");
        theWriter.println("             theNewDimensions.shift();");
        theWriter.println("             for (var i=0;i<theLength;i++) {");
        theWriter.println("                 theArray.data[i] = bytecoder.newMultiArray(theNewDimensions, aDefault);");
        theWriter.println("             }");
        theWriter.println("         }");
        theWriter.println("         return theArray;");
        theWriter.println("     },");
        theWriter.println();
        theWriter.println("     newArray : function(aLength, aDefault) {");

        BytecodeObjectTypeRef theArrayType = BytecodeObjectTypeRef.fromRuntimeClass(TArray.class);
        theWriter.println("          var theInstance = " + JSWriterUtils.toClassName(theArrayType)+ ".emptyInstance();");
        theWriter.println("          theInstance.data = [];");
        theWriter.println("          theInstance.data.length = aLength;");
        theWriter.println("          for (var i=0;i<aLength;i++) {");
        theWriter.println("             theInstance.data[i] = aDefault;");
        theWriter.println("          }");
        theWriter.println("          return theInstance;");
        theWriter.println("     }");
        theWriter.println("}");
        theWriter.println();
        aLinkerContext.forEachClass(theEntry -> {

            if (theEntry.getValue().getBytecodeClass().getAccessFlags().isInterface()) {
                return;
            }

            // Fix constructor invocation delegation
            final String theOverriddenParentClassName = getOverriddenParentClassFor(theEntry.getValue().getBytecodeClass());

            String theJSClassName = JSWriterUtils.toClassName(theEntry.getKey());
            theWriter.println("var " + theJSClassName + " = {");

            if (!theEntry.getValue().getBytecodeClass().getAccessFlags().isInterface()) {

                theWriter.println("    staticFields : {");

                theWriter.println("        name : '" + theEntry.getValue().getClassName().name() + "',");
                if (theEntry.getValue().hasClassInitializer()) {
                    theWriter.println("        classInitialized : false,");
                }
                theEntry.getValue().forEachStaticField(
                        aFieldEntry -> theWriter.println("        " + aFieldEntry.getKey() + " : null, // declared in " + aFieldEntry.getValue().getDeclaringType().name() ));
                theWriter.println("    },");
                theWriter.println();

                theWriter.println("    runtimeClass : {");
                theWriter.println("        jsType: function() {return " + theJSClassName + ";},");
                theWriter.println("        clazz: {");
                theWriter.println("            resolveVirtualMethod: function(aIdentifier) {");
                theWriter.println("                switch(aIdentifier) {");

                theClassLinkedCass.forEachVirtualMethod(
                        aClassMethod -> {
                            theWriter.println("                    case " + aClassMethod.getKey().getIdentifier() + ": // " + aClassMethod.getValue().getTargetMethod().getName().stringValue());
                            if ("getClass".equals(aClassMethod.getValue().getTargetMethod().getName().stringValue())) {
                                theWriter.println(
                                        "                        return " + JSWriterUtils.toClassName(theClassLinkedCass.getClassName()));
                            } else if ("toString".equals(aClassMethod.getValue().getTargetMethod().getName().stringValue())) {
                                    theWriter.println("                        throw 'Not implemented';");
                            } else if ("equals".equals(aClassMethod.getValue().getTargetMethod().getName().stringValue())) {
                                theWriter.println("                        throw 'Not implemented';");
                            } else if ("hashCode".equals(aClassMethod.getValue().getTargetMethod().getName().stringValue())) {
                                theWriter.println("                        throw 'Not implemented';");
                            } else if ("desiredAssertionStatus".equals(aClassMethod.getValue().getTargetMethod().getName().stringValue())) {
                                theWriter.println("                        return function(callsite) {return false};");
                            } else if ("getEnumConstants".equals(aClassMethod.getValue().getTargetMethod().getName().stringValue())) {
                                theWriter.println("                        return function(callsite) {return callsite.jsType().staticFields.$VALUES;};");
                            } else {
                                theWriter.println("                        throw {type: 'not implemented virtual name'} // " + aClassMethod.getValue().getTargetMethod().getName().stringValue());
                            }
                        });

                theWriter.println("                    default:");
                theWriter.println("                        throw {type: 'unknown virtual name'}");
                theWriter.println("                }");
                theWriter.println("            }");
                theWriter.println("        }");
                theWriter.println("    },");
                theWriter.println();

                theWriter.println("    resolveVirtualMethod : function(aIdentifier) {");
                theWriter.println("        switch(aIdentifier) {");
                theEntry.getValue().forEachVirtualMethod(aVirtualMethod -> {
                    BytecodeLinkedClass.LinkedMethod theLinkTarget = aVirtualMethod.getValue();
                    theWriter.println("            case " + aVirtualMethod.getKey().getIdentifier() + ":");
                    if (theLinkTarget.getTargetMethod() != BytecodeLinkedClass.GET_CLASS_PLACEHOLDER) {
                        theWriter.println(
                                "                return " + JSWriterUtils.toClassName(theLinkTarget.getDeclaringType()) + "." + JSWriterUtils.toMethodName(
                                        theLinkTarget.getTargetMethod().getName().stringValue(),
                                        theLinkTarget.getTargetMethod().getSignature()) + ";");
                    } else {
                        theWriter.println(
                                "                return function(callsite) {return " + theJSClassName + ".runtimeClass;};");
                    }
                });
                theWriter.println("            default:");
                theWriter.println("                throw {type: 'unknown virtual name'}");
                theWriter.println("        }");
                theWriter.println("    },");
                theWriter.println();

                theWriter.println("    classInitCheck : function() {");
                if (theEntry.getValue().hasClassInitializer()) {
                    theWriter.println("        if (" + theJSClassName + ".staticFields.classInitialized == false) {");
                    theWriter.println("            " + theJSClassName + ".staticFields.classInitialized = true;");
                    theWriter.println("            " + theJSClassName + ".VOIDclinit();");
                    theWriter.println("        }");
                }
                theWriter.println("    },");
                theWriter.println();


                theWriter.println("    emptyInstance : function() {");
                if (theEntry.getValue().hasClassInitializer()) {
                    theWriter.println("        " + theJSClassName + ".classInitCheck();");
                }
                theWriter.println("        return {data: {");
                theEntry.getValue().forEachMemberField(aField -> theWriter.println("            " + aField.getKey() + " : null, // declared in " + aField.getValue().getDeclaringType().name()));
                theWriter.println("        }, clazz: " + JSWriterUtils.toClassName(theEntry.getKey())+ "};");
                theWriter.println("    },");
                theWriter.println();

                theWriter.println("    thisIdentifier : function() {");
                theWriter.println("        return " + theEntry.getValue().getUniqueId());
                theWriter.println("    },");
                theWriter.println();

                theWriter.println("    instanceOfType : function(aType) {");
                theWriter.println("        switch(aType) {");

                for (BytecodeLinkedClass theType : theEntry.getValue().getImplementingTypes()) {
                    theWriter.println("            case " + theType.getUniqueId() +":");
                    theWriter.println("                return 1;");
                }

                theWriter.println("            default:");
                theWriter.println("                return 0;");
                theWriter.println("        }");
                theWriter.println("    },");
                theWriter.println();
            }

            theEntry.getValue().forEachMethod(theMethod -> {

                // Do not generate code for abstract methods
                if (theMethod.getAccessFlags().isAbstract()) {
                    return;
                }

                BytecodeMethodSignature theCurrentMethodSignature = theMethod.getSignature();
                BytecodeTypeRef[] theMethodArguments = theCurrentMethodSignature.getArguments();
                StringBuffer theArguments = new StringBuffer();
                if (!theMethod.getAccessFlags().isStatic()) {
                    theArguments.append("thisRef");
                }
                for (int i=1;i<=theMethodArguments.length;i++) {
                    if (theArguments.length() > 0) {
                        theArguments.append(",");
                    }
                    theArguments.append("p" + i);
                }

                if (theMethod.getAccessFlags().isNative()) {
                    if (theEntry.getValue().getBytecodeClass().getAttributes().getAnnotationByType(EmulatedByRuntime.class.getName()) != null) {
                        return;
                    }
                    BytecodeAnnotation theImportAnnotation = theMethod.getAttributes().getAnnotationByType(Import.class.getName());
                    if (theImportAnnotation == null) {
                        throw new IllegalStateException("No @Import annotation found. Potential linker error!");
                    }

                    JSModule theModule = modules.resolveModule(theImportAnnotation.getElementValueByName("module").stringValue());
                    JSFunction theFunction = theModule.resolveFunction(theImportAnnotation.getElementValueByName("name").stringValue());
                    theWriter.println();
                    theWriter.println("    " + JSWriterUtils.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature) + " : function(" + theArguments.toString() + ") {");
                    theWriter.println("         " + theFunction.generateCode(theCurrentMethodSignature));
                    theWriter.println("    },");
                    return;
                }

                BytecodeCodeAttributeInfo theCode = theMethod.getCode(theEntry.getValue().getBytecodeClass());

                theWriter.println();
                theWriter.println("    " + JSWriterUtils.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature) + " : function(" + theArguments.toString() + ") {");
                // theWriter.println("console.log('" + JSWriterUtils.toClassName(theEntry.getValue().getClassName()) + "." + JSWriterUtils.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature) + "');");

                System.out.println("Compiling " + theEntry.getValue().getClassName().name() + "." + theMethod.getName().stringValue());

                BytecodeProgram theProgram = theCode.getProgramm();
                BytecodeControlFlowGraph theFlowGraph = new BytecodeControlFlowGraph(theEntry.getValue().getBytecodeClass(), theProgram);
                ProgramGenerator theGenerator = new ProgramGenerator(aLinkerContext);
                Program theSSAProgram = theGenerator.generateFrom(theFlowGraph, theMethod.getSignature(), theMethod.getAccessFlags());

                theWriter.println("        // Brute force static references init");
                Set<BytecodeObjectTypeRef> theStaticReferences = theSSAProgram.getStaticReferences();
                for (BytecodeObjectTypeRef theRef : theStaticReferences) {
                    theWriter.print("        ");
                    theWriter.print(JSWriterUtils.toClassName(theRef));
                    theWriter.println(".classInitCheck();");
                }

                theWriter.println("        // # basic blocks in flow graph : " + theFlowGraph.getBlocks().size());

                JSSSAWriter theVariablesWriter = new JSSSAWriter("        ", theWriter, aLinkerContext);
                for (Variable theVariable : theSSAProgram.getVariables()) {
                    theVariablesWriter.print("var ");
                    theVariablesWriter.printVariableName(theVariable);
                    theVariablesWriter.print(" = null;");
                    theVariablesWriter.print(" // type is ");
                    theVariablesWriter.println(theVariable.getType().name());
                }

                theWriter.println();
                theWriter.println("        var currentLabel = " + theSSAProgram.getBlocks().get(0).getStartAddress().getAddress() + ";");
                theWriter.println("        controlflowloop: while(true) {switch(currentLabel) {");

                for (Block theBlock : theSSAProgram.getBlocks()) {

                    if (!theBlock.isConsumedByHighLevelControlFlowExpression()) {
                        theWriter.println("         case " + theBlock.getStartAddress().getAddress() + ": {");

                        JSSSAWriter theJSWriter = new JSSSAWriter("             ", theWriter, aLinkerContext);

                        for (Map.Entry<VariableDescription, Variable> theImported : theBlock.toStartState().getPorts()
                                .entrySet()) {
                            theJSWriter.print("// ");
                            theJSWriter.printVariableName(theImported.getValue());
                            theJSWriter.print(" is imported as ");
                            theJSWriter
                                    .println(theImported.getKey().toString() + " and type " + theImported.getValue().getValue());
                        }

                        for (Block thePrececessor : theBlock.getPredecessors()) {
                            theJSWriter.printlnComment(
                                    "Predecessor of this block is " + thePrececessor.getStartAddress().getAddress());
                        }
                        for (Block theSuccessor : theBlock.getSuccessors()) {
                            theJSWriter
                                    .printlnComment("Successor of this block is " + theSuccessor.getStartAddress().getAddress());
                        }

                        theJSWriter.writeExpressions(theBlock.getExpressions());

    /*                    for (Map.Entry<VariableDescription, Variable> theExported : theBlock.toFinalState().getPorts().entrySet()) {
                            theJSWriter.print("// ");
                            theJSWriter.printVariableName(theExported.getAddress());
                            theJSWriter.print(" is exported as ");
                            theJSWriter.println(theExported.getKey().toString());
                        }*/

                        theWriter.println("         }");
                    }
                }
                theWriter.println("                 default: throw 'Illegal state exception ' + currentLabel;");
                theWriter.println("        }}");
                theWriter.println("    },");
            });

            theWriter.println("}");
            theWriter.println();
        });

        theWriter.flush();

        return theStrWriter.toString();
    }

    private void writeExceptionHandlerCode(BytecodeLinkerContext aLinkerContext, BytecodeLinkedClass aExceptionRethrower,
            PrintWriter aWriter, BytecodeProgram aProgram,
            String aInset, BytecodeInstruction aInstruction, String aExceptionVariableName) {
        BytecodeExceptionTableEntry[] theActiveHandlers = aProgram.getActiveExceptionHandlers(aInstruction.getOpcodeAddress(), aProgram.getExceptionHandlers());
        if (theActiveHandlers.length == 0) {
            // Missing catch block
            aWriter.println(aInset + JSWriterUtils.toClassName(aExceptionRethrower.getClassName()) + "." + JSWriterUtils.toMethodName("registerExceptionOutcome", theRegisterExceptionOutcomeSignature) + "(" + aExceptionVariableName + ");");
            aWriter.println(aInset + "return;");
        } else {
            for (BytecodeExceptionTableEntry theEntry : theActiveHandlers) {
                if (!theEntry.isFinally()) {
                    BytecodeClassinfoConstant theConstant = theEntry.getCatchType();
                    BytecodeLinkedClass theLinkedClass = aLinkerContext.isLinkedOrNull(theConstant.getConstant());
                    if (theLinkedClass != null) {
                        aWriter.println(
                                aInset + "if (" + aExceptionVariableName + ".clazz.instanceOfType(" + theLinkedClass.getUniqueId()
                                        + ")) {");
                        aWriter.println(aInset + "    currentLabel = " + theEntry.getHandlerPc().getAddress() + ";");
                        aWriter.println(aInset + "    continue controlflowloop;");
                        aWriter.println(aInset + "}");
                    }
                }
            }
            aWriter.println(aInset + JSWriterUtils.toClassName(aExceptionRethrower.getClassName()) + "." + JSWriterUtils.toMethodName("registerExceptionOutcome", theRegisterExceptionOutcomeSignature) + "(" + aExceptionVariableName + ");");
            aWriter.println(aInset + "return;");
        }
    }
}