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

import de.mirkosertic.bytecoder.api.EmulatedByRuntime;
import de.mirkosertic.bytecoder.api.OverrideParentClass;
import de.mirkosertic.bytecoder.backend.CompileBackend;
import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.wasm.WASMWriterUtils;
import de.mirkosertic.bytecoder.classlib.ExceptionRethrower;
import de.mirkosertic.bytecoder.classlib.java.lang.TArray;
import de.mirkosertic.bytecoder.classlib.java.lang.TString;
import de.mirkosertic.bytecoder.classlib.java.lang.TThrowable;
import de.mirkosertic.bytecoder.core.BytecodeAnnotation;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeClass;
import de.mirkosertic.bytecoder.core.BytecodeClassinfoConstant;
import de.mirkosertic.bytecoder.core.BytecodeExceptionTableEntry;
import de.mirkosertic.bytecoder.core.BytecodeFieldMap;
import de.mirkosertic.bytecoder.core.BytecodeImportedLink;
import de.mirkosertic.bytecoder.core.BytecodeInstruction;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeProgram;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.relooper.Relooper;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.ssa.ProgramGeneratorFactory;
import de.mirkosertic.bytecoder.ssa.Variable;

public class JSSSACompilerBackend implements CompileBackend<JSCompileResult> {

    private final BytecodeMethodSignature registerExceptionOutcomeSignature;
    private final BytecodeMethodSignature getLastExceptionOutcomeSignature;
    private final ProgramGeneratorFactory programGeneratorFactory;

    public JSSSACompilerBackend(ProgramGeneratorFactory aProgramGeneratorFactory) {

        programGeneratorFactory = aProgramGeneratorFactory;
        registerExceptionOutcomeSignature = new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {BytecodeObjectTypeRef.fromRuntimeClass(TThrowable.class)});
        getLastExceptionOutcomeSignature = new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(TThrowable.class), new BytecodeTypeRef[0]);
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
    public JSCompileResult generateCodeFor(CompileOptions aOptions, BytecodeLinkerContext aLinkerContext, Class aEntryPointClass, String aEntryPointMethodName, BytecodeMethodSignature aEntryPointSignatue) {

        BytecodeLinkedClass theExceptionRethrower = aLinkerContext.linkClass(BytecodeObjectTypeRef.fromRuntimeClass(
                ExceptionRethrower.class));
        theExceptionRethrower.linkStaticMethod("registerExceptionOutcome", registerExceptionOutcomeSignature);
        theExceptionRethrower.linkStaticMethod("getLastOutcomeOrNullAndReset", getLastExceptionOutcomeSignature);

        StringWriter theStrWriter = new StringWriter();
        PrintWriter theWriter = new PrintWriter(theStrWriter);
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
        theWriter.println("          var theNewString = new " + JSWriterUtils.toClassName(theStringTypeRef) + ".Create();");
        theWriter.println("          var theBytes = new " + JSWriterUtils.toClassName(theArrayTypeRef) + ".Create();");
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
        theWriter.println("          var theInstance = new " + JSWriterUtils.toClassName(theArrayType)+ ".Create();");
        theWriter.println("          theInstance.data = [];");
        theWriter.println("          theInstance.data.length = aLength;");
        theWriter.println("          for (var i=0;i<aLength;i++) {");
        theWriter.println("             theInstance.data[i] = aDefault;");
        theWriter.println("          }");
        theWriter.println("          return theInstance;");
        theWriter.println("     },");

        theWriter.println();
        theWriter.println("     dynamicType : function(aFunction) { ");
        theWriter.println("         return new Proxy({}, {");
        theWriter.println("             get: function(target, name) {");
        theWriter.println("                 return function(inst, _p1, _p2, _p3, _p4, _p5, _p6, _p7, _p8, _p9) {");
        theWriter.println("                    return aFunction(_p1, _p2, _p3, _p4, _p5, _p6, _p7, _p8, _p9);");
        theWriter.println("                 }");
        theWriter.println("             }");
        theWriter.println("         });");
        theWriter.println("     }, ");
        theWriter.println();

        theWriter.println("     imports : [],");
        theWriter.println();

        theWriter.println("     bootstrap : function() {");
        aLinkerContext.linkedClasses().forEach(aEntry -> {
            if (!aEntry.targetNode().getBytecodeClass().getAccessFlags().isInterface()) {
                theWriter.print("          ");
                theWriter.print(JSWriterUtils.toClassName(aEntry.edgeType().objectTypeRef()));
                theWriter.println(".classInitCheck();");
            }
        });
        theWriter.println("     }");
        theWriter.println("};");
        theWriter.println();

        aLinkerContext.linkedClasses().forEach(theEntry -> {

            if (theEntry.targetNode().getBytecodeClass().getAccessFlags().isInterface()) {
                return;
            }

            // Fix constructor invocation delegation
            String theOverriddenParentClassName = getOverriddenParentClassFor(theEntry.targetNode().getBytecodeClass());

            String theJSClassName = JSWriterUtils.toClassName(theEntry.edgeType().objectTypeRef());
            theWriter.println("var " + theJSClassName + " = {");

            if (!theEntry.targetNode().getBytecodeClass().getAccessFlags().isInterface()) {

                // First of all, we add static fields required by the framework
                theWriter.println("    __name : '" + theEntry.edgeType().objectTypeRef().name() + "',");
                theWriter.println("    __initialized : false,");
                theWriter.println("    __staticCallSites : [],");
                theWriter.print("    __typeId : ");
                theWriter.print(theEntry.targetNode().getUniqueId());
                theWriter.println(",");
                theWriter.print("    __implementedTypes : [");
                {
                    boolean first = true;
                    for (BytecodeLinkedClass theType : theEntry.targetNode().getImplementingTypes()) {
                        if (!first) {
                            theWriter.print(",");
                        }
                        first = false;
                        theWriter.print(theType.getUniqueId());
                    }
                }
                theWriter.println("],");

                // then we add class specific static fields
                BytecodeFieldMap theStaticFields = theEntry.targetNode().staticFieldMap();
                theStaticFields.stream().forEach(
                        aFieldEntry -> theWriter.println("    " + aFieldEntry.getValue().getName().stringValue() + " : null, // declared in " + aFieldEntry.getProvidingClass().getClassName().name()));
                theWriter.println();

                theWriter.println("    resolveStaticCallSiteObject: function(aKey, aProducerFunction) {");
                theWriter.print("        var resolvedCallsiteObject = ");
                theWriter.print(theJSClassName);
                theWriter.println(".__staticCallSites[aKey];");
                theWriter.println("        if (resolvedCallsiteObject == null) {");
                theWriter.println("            resolvedCallsiteObject = aProducerFunction();");
                theWriter.print("            ");
                theWriter.print(theJSClassName);
                theWriter.println(".__staticCallSites[aKey] = resolvedCallsiteObject;");
                theWriter.println("        }");
                theWriter.println("        return resolvedCallsiteObject;");
                theWriter.println("    },");
                theWriter.println();

                // The Constructor function initializes all object members with null

                BytecodeFieldMap theInstanceFields = theEntry.targetNode().instanceFieldMap();
                theWriter.println("    Create : function() {");
                theInstanceFields.stream().forEach(
                        aFieldEntry -> theWriter.println("        this." + aFieldEntry.getValue().getName().stringValue() + " = null; // declared in " + aFieldEntry.getProvidingClass().getClassName().name()));
                theWriter.println("    },");
                theWriter.println();

                theWriter.println("    instanceOf : function(aType) {");
                theWriter.println("        return " + theJSClassName +".__implementedTypes.includes(aType.__typeId);");
                theWriter.println("    },");
                theWriter.println();

                theWriter.println("    TClassgetClass : function() {");
                theWriter.println("        return " + theJSClassName +";");
                theWriter.println("    },");
                theWriter.println();

                theWriter.println("    BOOLEANdesiredAssertionStatus : function() {");
                theWriter.println("        return false;");
                theWriter.println("    },");
                theWriter.println();

                theWriter.println("    A1TObjectgetEnumConstants : function(aClazz) {");
                theWriter.println("        return aClazz.$VALUES;");
                theWriter.println("    },");
            }

            Set<BytecodeObjectTypeRef> theStaticReferences = new HashSet<>();

            theEntry.targetNode().forEachMethod(theMethod -> {

                // Do not generate code for abstract methods
                if (theMethod.getAccessFlags().isAbstract()) {
                    return;
                }

                ProgramGenerator theGenerator = programGeneratorFactory.createFor(aLinkerContext);
                Program theSSAProgram = theGenerator.generateFrom(theEntry.targetNode().getBytecodeClass(), theMethod);

                //Run optimizer
                aOptions.getOptimizer().optimize(theSSAProgram.getControlFlowGraph(), aLinkerContext);

                BytecodeMethodSignature theCurrentMethodSignature = theMethod.getSignature();
                StringBuilder theArguments = new StringBuilder();
                for (Program.Argument theArgument : theSSAProgram.getArguments()) {
                    if (theArguments.length() > 0) {
                        theArguments.append(",");
                    }
                    theArguments.append(theArgument.getVariable().getName());
                }

                if (theMethod.getAccessFlags().isNative()) {
                    if (theEntry.targetNode().getBytecodeClass().getAttributes().getAnnotationByType(EmulatedByRuntime.class.getName()) != null) {
                        return;
                    }

                    BytecodeImportedLink theLink = theEntry.targetNode().linkfor(theMethod);

                    theWriter.println();
                    theWriter.println("    " + JSWriterUtils.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature) + " : function(" + theArguments
                            + ") {");

                    theWriter.print("         return bytecoder.imports.");
                    theWriter.print(theLink.getModuleName());
                    theWriter.print(".");
                    theWriter.print(theLink.getLinkName());
                    theWriter.print("(");
                    theWriter.print(theArguments);
                    theWriter.println(");");

                    theWriter.println("    },");
                    return;
                }

                theWriter.println();
                theWriter.println("    " + JSWriterUtils.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature) + " : function(" + theArguments
                        + ") {");

                aOptions.getLogger().info("Compiling " + theEntry.targetNode().getClassName().name() + "." + theMethod.getName().stringValue());

                theStaticReferences.addAll(theSSAProgram.getStaticReferences());

                if (aOptions.isDebugOutput()) {
                    theWriter.println("        /**");
                    theWriter.println("        " + theSSAProgram.getControlFlowGraph().toDOT());
                    theWriter.println("        */");
                }

                JSSSAWriter theVariablesWriter = new JSSSAWriter(aOptions, theSSAProgram,"        ", theWriter, aLinkerContext);
                for (Variable theVariable : theSSAProgram.globalVariables()) {
                    if (!theVariable.isSynthetic()) {
                        theVariablesWriter.print("var ");
                        theVariablesWriter.print(theVariable.getName());
                        theVariablesWriter.print(" = null;");
                        theVariablesWriter.print(" // type is ");
                        theVariablesWriter.print(theVariable.resolveType().resolve().name());
                        theVariablesWriter.print(" # of inits = " + theVariable.incomingDataFlows().size());
                        theVariablesWriter.println();
                    }
                }

                // Try to reloop it!
                try {
                    Relooper theRelooper = new Relooper();
                    Relooper.Block theReloopedBlock = theRelooper.reloop(theSSAProgram.getControlFlowGraph());

                    theVariablesWriter.printRelooped(theReloopedBlock);
                } catch (Exception e) {
                    System.out.println(theSSAProgram.getControlFlowGraph().toDOT());
                    throw new IllegalStateException("Error relooping cfg for " + theEntry.targetNode().getClassName().name() + "." + theMethod.getName().stringValue(), e);
                }

                theWriter.println("    },");
            });


            theWriter.println();
            theWriter.println("    classInitCheck : function() {");
            theWriter.println("        if (!" + theJSClassName + ".__initialized) {");
            theWriter.println("            " + theJSClassName + ".__initialized = true;");

            // Now we have to setup the prototype
            theWriter.println("            var thePrototype = " + theJSClassName + ".Create.prototype;");
            theWriter.println("            thePrototype.instanceOf = " + theJSClassName + ".instanceOf;");
            theWriter.println("            thePrototype.TClassgetClass = " + theJSClassName + ".TClassgetClass;");

            BytecodeLinkedClass theCurrentClass = theEntry.targetNode();
            Set<String> theAlreadyProcessed = new HashSet<>();
            while(theCurrentClass != null) {

                BytecodeLinkedClass theCurrentCl = theCurrentClass;
                BytecodeObjectTypeRef theCurrent = theCurrentClass.getClassName();

                theCurrentClass.forEachMethod(aEntry -> {
                    if (!aEntry.getAccessFlags().isStatic() &&
                        !aEntry.getAccessFlags().isAbstract() &&
                        !aEntry.isConstructor() &&
                        !aEntry.isClassInitializer() &&
                        theCurrentCl.hasMethod(aEntry)) {
                        String theMethodName = JSWriterUtils.toMethodName(aEntry.getName().stringValue(), aEntry.getSignature());
                        if (theAlreadyProcessed.add(theMethodName)) {
                            theWriter.print("            thePrototype.");
                            theWriter.print(theMethodName);
                            theWriter.print(" = ");
                            theWriter.print(WASMWriterUtils.toClassName(theCurrent));
                            theWriter.print(".");
                            theWriter.print(theMethodName);
                            theWriter.println(";");
                        }
                    }
                });

                theCurrentClass = theCurrentClass.getSuperClass();
            }

            for (BytecodeObjectTypeRef theRef : theStaticReferences) {
                if (!Objects.equals(theRef, theEntry.edgeType().objectTypeRef())) {
                    theWriter.print("            ");
                    theWriter.print(JSWriterUtils.toClassName(theRef));
                    theWriter.println(".classInitCheck();");
                }
            }
            if (theEntry.targetNode().hasClassInitializer()) {
                theWriter.println("            " + theJSClassName + ".VOIDclinit();");
            }
            theWriter.println("        }");

            theWriter.println("    },");
            theWriter.println();

            theWriter.println("};");
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