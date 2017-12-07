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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import de.mirkosertic.bytecoder.annotations.EmulatedByRuntime;
import de.mirkosertic.bytecoder.annotations.Import;
import de.mirkosertic.bytecoder.annotations.OverrideParentClass;
import de.mirkosertic.bytecoder.backend.CompileBackend;
import de.mirkosertic.bytecoder.classlib.ExceptionRethrower;
import de.mirkosertic.bytecoder.classlib.java.lang.TArray;
import de.mirkosertic.bytecoder.classlib.java.lang.TClass;
import de.mirkosertic.bytecoder.classlib.java.lang.TString;
import de.mirkosertic.bytecoder.classlib.java.lang.TThrowable;
import de.mirkosertic.bytecoder.core.BytecodeAnnotation;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeClass;
import de.mirkosertic.bytecoder.core.BytecodeClassinfoConstant;
import de.mirkosertic.bytecoder.core.BytecodeExceptionTableEntry;
import de.mirkosertic.bytecoder.core.BytecodeInstruction;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeProgram;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.core.Logger;
import de.mirkosertic.bytecoder.ssa.MethodParameterValue;
import de.mirkosertic.bytecoder.ssa.PrimitiveValue;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.ssa.ProgramGeneratorFactory;
import de.mirkosertic.bytecoder.ssa.SelfReferenceParameterValue;
import de.mirkosertic.bytecoder.ssa.Variable;

public class JSSSACompilerBackend implements CompileBackend<JSCompileResult> {

    private final BytecodeMethodSignature registerExceptionOutcomeSignature;
    private final BytecodeMethodSignature getLastExceptionOutcomeSignature;
    private final JSModules modules;
    private final ProgramGeneratorFactory programGeneratorFactory;

    public JSSSACompilerBackend(ProgramGeneratorFactory aProgramGeneratorFactory) {

        programGeneratorFactory = aProgramGeneratorFactory;
        registerExceptionOutcomeSignature = new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {BytecodeObjectTypeRef.fromRuntimeClass(TThrowable.class)});
        getLastExceptionOutcomeSignature = new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(TThrowable.class), new BytecodeTypeRef[0]);
        modules = new JSModules();

        JSModule theMathModule = new JSModule();
        theMathModule.registerFunction("ceil", new JSFunction("return Math.ceil(p1);"));
        theMathModule.registerFunction("floor", new JSFunction("return Math.floor(p1);"));
        theMathModule.registerFunction("sin", new JSFunction("return Math.sin(p1);"));
        theMathModule.registerFunction("cos", new JSFunction("return Math.cos(p1);"));
        theMathModule.registerFunction("sqrt", new JSFunction("return Math.sqrt(p1);"));
        theMathModule.registerFunction("round", new JSFunction("return Math.round(p1);"));
        theMathModule.registerFunction("NaN", new JSFunction("return NaN;"));
        theMathModule.registerFunction("atan2", new JSFunction("return Math.atan2(p1, p2);"));
        theMathModule.registerFunction("max", new JSFunction("return Math.max(p1, p2);"));
        theMathModule.registerFunction("random", new JSFunction("return Math.random();"));
        theMathModule.registerFunction("tan", new JSFunction("return Math.tan(p1);"));
        theMathModule.registerFunction("toRadians", new JSFunction("return Math.toRadians(p1);"));
        theMathModule.registerFunction("toDegrees", new JSFunction("return Math.toDegrees(p1);"));
        theMathModule.registerFunction("min", new JSFunction("return Math.min(p1, p2);"));

        JSModule theSystemModule = new JSModule();
        theSystemModule.registerFunction("currentTimeMillis", new JSFunction("return Date.now();"));
        theSystemModule.registerFunction("nanoTime", new JSFunction("return Date.now() * 1000000;"));
        theSystemModule.registerFunction("logByteArrayAsString", new JSFunction("bytecoder.logByteArrayAsString(p1);"));
        theSystemModule.registerFunction("logDebug", new JSFunction("bytecoder.logDebug(p1);"));

        modules.register("math", theMathModule);
        modules.register("system", theSystemModule);
    }

    private String getOverriddenParentClassFor(BytecodeClass aBytecodeClass) {
        BytecodeAnnotation theDelegatesTo = aBytecodeClass.getAttributes().getAnnotationByType(OverrideParentClass.class.getName());
        if (theDelegatesTo != null) {
            BytecodeAnnotation.ElementValue theParentOverride = theDelegatesTo.getElementValueByName("parentClass");
            return theParentOverride.stringValue().replace("/",".");
        }
        return null;
    }

    @Override
    public JSCompileResult generateCodeFor(Logger aLogger, BytecodeLinkerContext aLinkerContext, Class aEntryPointClass, String aEntryPointMethodName, BytecodeMethodSignature aEntryPointSignatue) {

        BytecodeLinkedClass theClassLinkedCass = aLinkerContext.linkClass(BytecodeObjectTypeRef.fromRuntimeClass(TClass.class));

        BytecodeLinkedClass theExceptionRethrower = aLinkerContext.linkClass(BytecodeObjectTypeRef.fromRuntimeClass(
                ExceptionRethrower.class));
        theExceptionRethrower.linkStaticMethod("registerExceptionOutcome", registerExceptionOutcomeSignature);
        theExceptionRethrower.linkStaticMethod("getLastOutcomeOrNullAndReset", getLastExceptionOutcomeSignature);

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
        theWriter.println("     },");
        theWriter.println();
        theWriter.println("     bootstrap : function() {");
        aLinkerContext.forEachClass(theEntry -> {
            if (!theEntry.getValue().getAccessFlags().isInterface()) {
                theWriter.print("          ");
                theWriter.print(JSWriterUtils.toClassName(theEntry.getKey()));
                theWriter.println(".classInitCheck();");
            }
        });
        theWriter.println("     }");
        theWriter.println("};");
        theWriter.println();
        theWriter.println("var RUNTIME_CLASS = {");
        theWriter.println("     resolveVirtualMethod: function(aIdentifier) {");
        theWriter.println("         switch(aIdentifier) {");

        theClassLinkedCass.forEachVirtualMethod(
                aClassMethod -> {
                    theWriter.println("             case " + aClassMethod.getKey().getIdentifier() + ": // " + aClassMethod.getValue().getTargetMethod().getName().stringValue());
                    if (Objects.equals("getClass", aClassMethod.getValue().getTargetMethod().getName().stringValue())) {
                        theWriter.println(
                                "                   return " + JSWriterUtils.toClassName(theClassLinkedCass.getClassName()));
                    } else if (Objects
                            .equals("toString", aClassMethod.getValue().getTargetMethod().getName().stringValue())) {
                        theWriter.println("                   throw 'Not implemented';");
                    } else if (Objects
                            .equals("equals", aClassMethod.getValue().getTargetMethod().getName().stringValue())) {
                        theWriter.println("                   throw 'Not implemented';");
                    } else if (Objects
                            .equals("hashCode", aClassMethod.getValue().getTargetMethod().getName().stringValue())) {
                        theWriter.println("                   throw 'Not implemented';");
                    } else if (Objects.equals("desiredAssertionStatus",
                            aClassMethod.getValue().getTargetMethod().getName().stringValue())) {
                        theWriter.println("                   return function(callsite) {return false};");
                    } else if (Objects
                            .equals("getEnumConstants",
                                    aClassMethod.getValue().getTargetMethod().getName().stringValue())) {
                        theWriter.println("                   return function(callsite) {return callsite.jsType().staticFields.$VALUES;};");
                    } else {
                        theWriter.println("                   throw {type: 'not implemented virtual name'} // " + aClassMethod.getValue().getTargetMethod().getName().stringValue());
                    }
                });

        theWriter.println("             default:");
        theWriter.println("                 throw {type: 'unknown virtual name'}");
        theWriter.println("         }");
        theWriter.println("     }");
        theWriter.println("};");
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
                theWriter.println("        staticCallSites : [],");
                theWriter.println("        classInitialized : false,");
                theEntry.getValue().forEachStaticField(
                        aFieldEntry -> theWriter.println("        " + aFieldEntry.getKey() + " : null, // declared in " + aFieldEntry.getValue().getDeclaringType().name() ));
                theWriter.println("    },");
                theWriter.println();

                theWriter.println("    resolveStaticCallSiteObject: function(aKey, aProducerFunction) {");
                theWriter.print("        var resolvedCallsiteObject = ");
                theWriter.print(theJSClassName);
                theWriter.println(".staticFields.staticCallSites[aKey];");
                theWriter.println("        if (resolvedCallsiteObject == null) {");
                theWriter.println("            resolvedCallsiteObject = aProducerFunction();");
                theWriter.print("            ");
                theWriter.print(theJSClassName);
                theWriter.println(".staticFields.staticCallSites[aKey] = resolvedCallsiteObject;");
                theWriter.println("        }");
                theWriter.println("        return resolvedCallsiteObject;");
                theWriter.println("    },");
                theWriter.println();

                theWriter.println("    runtimeClass : {");
                theWriter.println("        jsType: function() {return " + theJSClassName + ";},");
                theWriter.println("        clazz: RUNTIME_CLASS");
                theWriter.println("    },");
                theWriter.println();

                theWriter.println("    resolveVirtualMethod : function(aIdentifier) {");
                theWriter.println("        switch(aIdentifier) {");
                theEntry.getValue().forEachVirtualMethod(aVirtualMethod -> {
                    BytecodeLinkedClass.LinkedMethod theLinkTarget = aVirtualMethod.getValue();
                    if (!aVirtualMethod.getValue().getTargetMethod().getAccessFlags().isAbstract()) {

                        theWriter.println("            case " + aVirtualMethod.getKey().getIdentifier() + ":");
                        if (theLinkTarget.getTargetMethod() != BytecodeLinkedClass.GET_CLASS_PLACEHOLDER) {
                            theWriter.println(
                                    "                return " + JSWriterUtils.toClassName(theLinkTarget.getDeclaringType()) + "."
                                            + JSWriterUtils.toMethodName(
                                            theLinkTarget.getTargetMethod().getName().stringValue(),
                                            theLinkTarget.getTargetMethod().getSignature()) + ";");
                        } else {
                            theWriter.println(
                                    "                return function(callsite) {return " + theJSClassName + ".runtimeClass;};");
                        }
                    }
                });
                theWriter.println("            default:");
                theWriter.println("                throw {type: 'unknown virtual name'}");
                theWriter.println("        }");
                theWriter.println("    },");
                theWriter.println();

                theWriter.println("    emptyInstance : function() {");
                theWriter.println("        return {");
                theEntry.getValue().forEachMemberField(aField -> theWriter.println("            " + aField.getKey() + " : null, // declared in " + aField.getValue().getDeclaringType().name()));
                theWriter.println("            clazz: " + JSWriterUtils.toClassName(theEntry.getKey())+ "};");
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
            }

            Set<BytecodeObjectTypeRef> theStaticReferences = new HashSet<>();

            theEntry.getValue().forEachMethod(theMethod -> {

                // Do not generate code for abstract methods
                if (theMethod.getAccessFlags().isAbstract()) {
                    return;
                }

                BytecodeMethodSignature theCurrentMethodSignature = theMethod.getSignature();
                BytecodeTypeRef[] theMethodArguments = theCurrentMethodSignature.getArguments();
                StringBuilder theArguments = new StringBuilder();
                if (!theMethod.getAccessFlags().isStatic()) {
                    theArguments.append("thisRef");
                }
                for (int i=1;i<=theMethodArguments.length;i++) {
                    if (theArguments.length() > 0) {
                        theArguments.append(",");
                    }
                    theArguments.append("p").append(i);
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

                theWriter.println();
                theWriter.println("    " + JSWriterUtils.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature) + " : function(" + theArguments.toString() + ") {");
                // theWriter.println("console.log('" + JSWriterUtils.toClassName(theEntry.getValue().getClassName()) + "." + JSWriterUtils.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature) + "');");

                aLogger.info("Compiling " + theEntry.getValue().getClassName().name() + "." + theMethod.getName().stringValue());

                ProgramGenerator theGenerator = programGeneratorFactory.createFor(aLinkerContext);
                Program theSSAProgram = theGenerator.generateFrom(theEntry.getValue().getBytecodeClass(), theMethod);

                theStaticReferences.addAll(theSSAProgram.getStaticReferences());

                theWriter.println("        // # basic blocks in flow graph : " + theSSAProgram.getControlFlowGraph().getDominatedNodes().size());

                JSSSAWriter theVariablesWriter = new JSSSAWriter(theSSAProgram,"        ", theWriter, aLinkerContext);
                for (Variable theVariable : theSSAProgram.globalVariables()) {
                    if (!(theVariable.getValue() instanceof PrimitiveValue) &&
                        !(theVariable.getValue() instanceof MethodParameterValue) &&
                        !(theVariable.getValue() instanceof SelfReferenceParameterValue)) {
                        theVariablesWriter.print("var ");
                        theVariablesWriter.print(theVariable.getName());
                        theVariablesWriter.print(" = null;");
                        theVariablesWriter.print(" // type is ");
                        theVariablesWriter.println(theVariable.resolveType().resolve().name());
                    }
                }

                theVariablesWriter.print(theSSAProgram.getControlFlowGraph().toRootNode());

                theWriter.println("    },");
            });


            theWriter.println();
            theWriter.println("    classInitCheck : function() {");
            theWriter.println("        if (" + theJSClassName + ".staticFields.classInitialized == false) {");
            theWriter.println("            " + theJSClassName + ".staticFields.classInitialized = true;");
            for (BytecodeObjectTypeRef theRef : theStaticReferences) {
                if (!theRef.equals(theEntry.getKey())) {
                    theWriter.print("            ");
                    theWriter.print(JSWriterUtils.toClassName(theRef));
                    theWriter.println(".classInitCheck();");
                }
            }
            if (theEntry.getValue().hasClassInitializer()) {
                theWriter.println("            " + theJSClassName + ".VOIDclinit();");
            }
            theWriter.println("        }");

            theWriter.println("    },");
            theWriter.println();

            theWriter.println("}");
            theWriter.println();
        });

        theWriter.flush();

        return new JSCompileResult(theStrWriter.toString());
    }

    private void writeExceptionHandlerCode(BytecodeLinkerContext aLinkerContext, BytecodeLinkedClass aExceptionRethrower,
            PrintWriter aWriter, BytecodeProgram aProgram,
            String aInset, BytecodeInstruction aInstruction, String aExceptionVariableName) {
        BytecodeExceptionTableEntry[] theActiveHandlers = aProgram.getActiveExceptionHandlers(aInstruction.getOpcodeAddress(), aProgram.getExceptionHandlers());
        if (theActiveHandlers.length == 0) {
            // Missing catch block
            aWriter.println(aInset + JSWriterUtils.toClassName(aExceptionRethrower.getClassName()) + "." + JSWriterUtils.toMethodName("registerExceptionOutcome",
                    registerExceptionOutcomeSignature) + "(" + aExceptionVariableName + ");");
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
            aWriter.println(aInset + JSWriterUtils.toClassName(aExceptionRethrower.getClassName()) + "." + JSWriterUtils.toMethodName("registerExceptionOutcome",
                    registerExceptionOutcomeSignature) + "(" + aExceptionVariableName + ");");
            aWriter.println(aInset + "return;");
        }
    }

    @Override
    public String generatedFileName() {
        return "bytecoder.js";
    }
}