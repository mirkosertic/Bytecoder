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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.mirkosertic.bytecoder.api.EmulatedByRuntime;
import de.mirkosertic.bytecoder.backend.CompileBackend;
import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.ConstantPool;
import de.mirkosertic.bytecoder.classlib.ExceptionRethrower;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeClassinfoConstant;
import de.mirkosertic.bytecoder.core.BytecodeExceptionTableEntry;
import de.mirkosertic.bytecoder.core.BytecodeImportedLink;
import de.mirkosertic.bytecoder.core.BytecodeInstruction;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeProgram;
import de.mirkosertic.bytecoder.core.BytecodeResolvedFields;
import de.mirkosertic.bytecoder.core.BytecodeResolvedMethods;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.relooper.Relooper;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.ssa.ProgramGeneratorFactory;
import de.mirkosertic.bytecoder.ssa.StringValue;
import de.mirkosertic.bytecoder.ssa.Variable;

public class JSSSACompilerBackend implements CompileBackend<JSCompileResult> {

    private final BytecodeMethodSignature registerExceptionOutcomeSignature;
    private final BytecodeMethodSignature getLastExceptionOutcomeSignature;
    private final ProgramGeneratorFactory programGeneratorFactory;

    public JSSSACompilerBackend(ProgramGeneratorFactory aProgramGeneratorFactory) {
        programGeneratorFactory = aProgramGeneratorFactory;
        registerExceptionOutcomeSignature = new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {BytecodeObjectTypeRef.fromRuntimeClass(Throwable.class)});
        getLastExceptionOutcomeSignature = new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Throwable.class), new BytecodeTypeRef[0]);
    }

    @Override
    public JSCompileResult generateCodeFor(CompileOptions aOptions, BytecodeLinkerContext aLinkerContext, Class aEntryPointClass, String aEntryPointMethodName, BytecodeMethodSignature aEntryPointSignatue) {

        BytecodeLinkedClass theExceptionRethrower = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(
                ExceptionRethrower.class));
        theExceptionRethrower.resolveStaticMethod("registerExceptionOutcome", registerExceptionOutcomeSignature);
        theExceptionRethrower.resolveStaticMethod("getLastOutcomeOrNullAndReset", getLastExceptionOutcomeSignature);

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

        BytecodeObjectTypeRef theStringTypeRef = BytecodeObjectTypeRef.fromRuntimeClass(String.class);
        BytecodeObjectTypeRef theArrayTypeRef = BytecodeObjectTypeRef.fromRuntimeClass(Array.class);

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

        BytecodeObjectTypeRef theArrayType = BytecodeObjectTypeRef.fromRuntimeClass(Array.class);
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

        theWriter.println("     resolveStaticCallSiteObject: function(aWhere, aKey, aProducerFunction) {");
        theWriter.println("         var resolvedCallsiteObject = aWhere.__staticCallSites[aKey];");
        theWriter.println("         if (resolvedCallsiteObject == null) {");
        theWriter.println("             resolvedCallsiteObject = aProducerFunction();");
        theWriter.println("             aWhere.__staticCallSites[aKey] = resolvedCallsiteObject;");
        theWriter.println("         }");
        theWriter.println("         return resolvedCallsiteObject;");
        theWriter.println("     },");
        theWriter.println();

        theWriter.println("     imports : [],");
        theWriter.println();

        theWriter.println("     stringpool : [],");
        theWriter.println();

        theWriter.println("};");
        theWriter.println();

        ConstantPool thePool = new ConstantPool();

        aLinkerContext.linkedClasses().forEach(theEntry -> {

            // Here we collect everything that is required for class initialization
            // this includes the super class, all implementing interfaces and also static
            // dependencies of the class initialization code
            List<BytecodeObjectTypeRef> theInitDependencies = new ArrayList<>();
            BytecodeLinkedClass theSuperClass = theEntry.targetNode().getSuperClass();
            if (theSuperClass != null) {
                theInitDependencies.add(theSuperClass.getClassName());
            }
            for (BytecodeLinkedClass theInterface : theEntry.targetNode().getImplementingTypes()) {
                if (!theInitDependencies.contains(theInterface.getClassName())) {
                    theInitDependencies.add(theInterface.getClassName());
                }
            }

            String theJSClassName = JSWriterUtils.toClassName(theEntry.edgeType().objectTypeRef());
            theWriter.println("var " + theJSClassName + " = {");

            // First of all, we add static fields required by the framework
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
            BytecodeResolvedFields theStaticFields = theEntry.targetNode().resolvedFields();
            theStaticFields.streamForStaticFields().forEach(
                    aFieldEntry -> theWriter.println("    " + aFieldEntry.getValue().getName().stringValue() + " : null, // declared in " + aFieldEntry.getProvidingClass().getClassName().name()));
            theWriter.println();

            if (!theEntry.targetNode().getBytecodeClass().getAccessFlags().isAbstract()) {
                // The Constructor function initializes all object members with null
                // Only non abstract classes can be instantiated
                BytecodeResolvedFields theInstanceFields = theEntry.targetNode().resolvedFields();
                theWriter.println("    Create : function() {");
                theInstanceFields.streamForInstanceFields().forEach(
                        aFieldEntry -> {
                            BytecodeTypeRef theFieldType = aFieldEntry.getValue().getTypeRef();
                            if (theFieldType.isPrimitive()) {
                                BytecodePrimitiveTypeRef thePrimitive = (BytecodePrimitiveTypeRef) theFieldType;
                                switch (thePrimitive) {
                                    case BOOLEAN: {
                                        theWriter.println("        this." + aFieldEntry.getValue().getName().stringValue() + " = false; // declared in " + aFieldEntry.getProvidingClass().getClassName().name());
                                        break;
                                    }
                                    default: {
                                        theWriter.println("        this." + aFieldEntry.getValue().getName().stringValue() + " = 0; // declared in " + aFieldEntry.getProvidingClass().getClassName().name());
                                        break;
                                    }
                                }
                            } else {
                                theWriter.println("        this." + aFieldEntry.getValue().getName().stringValue() + " = null; // declared in " + aFieldEntry.getProvidingClass().getClassName().name());
                            }
                        });
                theWriter.println("    },");
                theWriter.println();
            }

            theWriter.println("    instanceOf : function(aType) {");
            theWriter.println("        return " + theJSClassName + ".__implementedTypes.includes(aType.__typeId);");
            theWriter.println("    },");
            theWriter.println();

            theWriter.println("    ClassgetClass : function() {");
            theWriter.println("        return " + theJSClassName + ";");
            theWriter.println("    },");
            theWriter.println();

            theWriter.println("    BOOLEANdesiredAssertionStatus : function() {");
            theWriter.println("        return false;");
            theWriter.println("    },");
            theWriter.println();

            theWriter.println("    A1javalangObjectgetEnumConstants : function(aClazz) {");
            theWriter.println("        return aClazz.$VALUES;");
            theWriter.println("    },");

            BytecodeResolvedMethods theMethods = theEntry.targetNode().resolvedMethods();

            theMethods.stream().forEach(aEntry -> {
                BytecodeMethod theMethod = aEntry.getValue();
                BytecodeMethodSignature theCurrentMethodSignature = theMethod.getSignature();

                // Do not generate code for abstract methods
                if (theMethod.getAccessFlags().isAbstract()) {
                    return;
                }

                if (!(aEntry.getProvidingClass() == theEntry.targetNode())) {
                    // Skip methods not implemented in this class
                    // But include static methods, as they are inherited from the base classes
                    if (aEntry.getValue().getAccessFlags().isStatic()) {

                        StringBuilder theArguments = new StringBuilder();;
                        for (int i=0;i<theCurrentMethodSignature.getArguments().length;i++) {
                            if (i>0) {
                                theArguments.append(",");
                            }
                            theArguments.append("p");
                            theArguments.append(i);
                        }

                        // Static methods will just delegate to the implementation in the class
                        theWriter.println();
                        theWriter.println("    " + JSWriterUtils.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature) + " : function(" + theArguments
                                + ") {");
                        if (!theCurrentMethodSignature.getReturnType().isVoid()) {
                            theWriter.print("         return ");
                        } else {
                            theWriter.print("         ");
                        }
                        theWriter.print(JSWriterUtils.toClassName(aEntry.getProvidingClass().getClassName()));
                        theWriter.print(".");
                        theWriter.print(JSWriterUtils.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature));
                        theWriter.print("(");
                        theWriter.print(theArguments);
                        theWriter.println(");");

                        theWriter.println("    },");
                    }
                    return;
                }

                aLinkerContext.getLogger().info("Compiling {}.{}", theEntry.targetNode().getClassName().name(), theMethod.getName().stringValue());

                ProgramGenerator theGenerator = programGeneratorFactory.createFor(aLinkerContext);
                Program theSSAProgram = theGenerator.generateFrom(aEntry.getProvidingClass().getBytecodeClass(), theMethod);

                //Run optimizer
                aOptions.getOptimizer().optimize(theSSAProgram.getControlFlowGraph(), aLinkerContext);

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

                if (Objects.equals(theMethod.getName().stringValue(), "<clinit>")) {
                    for (BytecodeObjectTypeRef theRef : theSSAProgram.getStaticReferences()) {
                        if (!theInitDependencies.contains(theRef)) {
                            theInitDependencies.add(theRef);
                        }
                    }
                }

                if (aOptions.isDebugOutput()) {
                    theWriter.println("        /**");
                    theWriter.println("        " + theSSAProgram.getControlFlowGraph().toDOT());
                    theWriter.println("        */");
                }

                JSSSAWriter theVariablesWriter = new JSSSAWriter(aOptions, theSSAProgram, "        ", theWriter, aLinkerContext, thePool);
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

            if (!theEntry.targetNode().getBytecodeClass().getAccessFlags().isAbstract()) {
                // Now we have to setup the prototype
                // Only in case this class can be instantiated of course
                theWriter.println("            var thePrototype = " + theJSClassName + ".Create.prototype;");
                theWriter.println("            thePrototype.instanceOf = " + theJSClassName + ".instanceOf;");
                theWriter.println("            thePrototype.ClassgetClass = " + theJSClassName + ".ClassgetClass;");

                theMethods.stream().forEach(aEntry -> {
                    BytecodeMethod theMethod = aEntry.getValue();
                    if (!theMethod.getAccessFlags().isStatic() &&
                            !theMethod.getAccessFlags().isAbstract() &&
                            !theMethod.isConstructor() &&
                            !theMethod.isClassInitializer()) {
                        String theMethodName = JSWriterUtils.toMethodName(theMethod.getName().stringValue(), theMethod.getSignature());
                        theWriter.print("            thePrototype.");
                        theWriter.print(theMethodName);
                        theWriter.print(" = ");
                        theWriter.print(JSWriterUtils.toClassName(aEntry.getProvidingClass().getClassName()));
                        theWriter.print(".");
                        theWriter.print(theMethodName);
                        theWriter.println(";");
                    }
                });
            }

            for (BytecodeObjectTypeRef theRef : theInitDependencies) {
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

        theWriter.println();
        theWriter.println("bytecoder.bootstrap = function() {");
        List<StringValue> theValues = thePool.stringValues();
        for (int i=0; i<theValues.size(); i++) {
            StringValue theValue = theValues.get(i);
            theWriter.print("    bytecoder.stringpool[");
            theWriter.print(i);
            theWriter.print("] = bytecoder.newString(");
            theWriter.print(JSWriterUtils.toArray(theValue.getStringValue().getBytes()));
            theWriter.println(");");
        }

        aLinkerContext.linkedClasses().forEach(aEntry -> {
            if (!aEntry.targetNode().getBytecodeClass().getAccessFlags().isInterface()) {
                theWriter.print("    ");
                theWriter.print(JSWriterUtils.toClassName(aEntry.edgeType().objectTypeRef()));
                theWriter.println(".classInitCheck();");
            }
        });

        theWriter.println("}");

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