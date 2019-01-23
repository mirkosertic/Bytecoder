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

import de.mirkosertic.bytecoder.api.EmulatedByRuntime;
import de.mirkosertic.bytecoder.api.Export;
import de.mirkosertic.bytecoder.backend.CompileBackend;
import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.ConstantPool;
import de.mirkosertic.bytecoder.classlib.ExceptionManager;
import de.mirkosertic.bytecoder.core.BytecodeAnnotation;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeImportedLink;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeResolvedFields;
import de.mirkosertic.bytecoder.core.BytecodeResolvedMethods;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.relooper.Relooper;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.ssa.ProgramGeneratorFactory;
import de.mirkosertic.bytecoder.ssa.StringValue;
import de.mirkosertic.bytecoder.ssa.Variable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JSSSACompilerBackend implements CompileBackend<JSCompileResult> {

    private final BytecodeMethodSignature pushExceptionSignature;
    private final BytecodeMethodSignature popExceptionSignature;
    private final ProgramGeneratorFactory programGeneratorFactory;

    public JSSSACompilerBackend(final ProgramGeneratorFactory aProgramGeneratorFactory) {
        programGeneratorFactory = aProgramGeneratorFactory;
        pushExceptionSignature = new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {BytecodeObjectTypeRef.fromRuntimeClass(Throwable.class)});
        popExceptionSignature = new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Throwable.class), new BytecodeTypeRef[0]);
    }

    @Override
    public JSCompileResult generateCodeFor(final CompileOptions aOptions, final BytecodeLinkerContext aLinkerContext, final Class aEntryPointClass, final String aEntryPointMethodName, final BytecodeMethodSignature aEntryPointSignatue) {

        final BytecodeLinkedClass theExceptionManager = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(
                ExceptionManager.class));
        theExceptionManager.resolveStaticMethod("push", pushExceptionSignature);
        theExceptionManager.resolveStaticMethod("pop", popExceptionSignature);
        theExceptionManager.resolveStaticMethod("lastExceptionOrNull", popExceptionSignature);

        final StringWriter theStrWriter = new StringWriter();
        final PrintWriter theWriter = new PrintWriter(theStrWriter);
        theWriter.println("'use strict';");

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

        final BytecodeObjectTypeRef theStringTypeRef = BytecodeObjectTypeRef.fromRuntimeClass(String.class);
        final BytecodeObjectTypeRef theArrayTypeRef = BytecodeObjectTypeRef.fromRuntimeClass(Array.class);

        final BytecodeMethodSignature theStringConstructorSignature = new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID,
                new BytecodeTypeRef[]{new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.BYTE, 1)});

        // Construct a String
        theWriter.println("          var theNewString = new " + JSWriterUtils.toClassName(theStringTypeRef) + ".Create();");
        theWriter.println("          var theBytes = new " + JSWriterUtils.toClassName(theArrayTypeRef) + ".Create();");
        theWriter.println("          theBytes.data = aByteArray;");
        theWriter.println("          " + JSWriterUtils.toClassName(theStringTypeRef) + '.' + JSWriterUtils.toMethodName("init", theStringConstructorSignature) + "(theNewString, theBytes);");
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

        final BytecodeObjectTypeRef theArrayType = BytecodeObjectTypeRef.fromRuntimeClass(Array.class);
        theWriter.println("          var theInstance = new " + JSWriterUtils.toClassName(theArrayType)+ ".Create();");
        theWriter.println("          theInstance.data = [];");
        theWriter.println("          theInstance.data.length = aLength;");
        theWriter.println("          for (var i=0;i<aLength;i++) {");
        theWriter.println("             theInstance.data[i] = aDefault;");
        theWriter.println("          }");
        theWriter.println("          return theInstance;");
        theWriter.println("     },");

        theWriter.println();
        theWriter.println("     toBytecoderString: function(aJSString) {");
        theWriter.println("         var theLength = aJSString.length;");
        theWriter.println("         var theArray = [];");
        theWriter.println("         for (var i=0;i<theLength;i++) {");
        theWriter.println("             theArray.push(aJSString.charCodeAt(i));");
        theWriter.println("         }");
        theWriter.println("         return bytecoder.newString(theArray);");
        theWriter.println("     },");
        theWriter.println();

        theWriter.println("     toJSString: function(aBytecoderString) {");
        theWriter.println("         if (typeof(aBytecoderString) === 'string') {");
        theWriter.println("             return aBytecoderString;");
        theWriter.println("         }");
        theWriter.println("         var theArray = aBytecoderString.data.data;");
        theWriter.println("         var theResult = '';");
        theWriter.println("         for (var i=0;i<theArray.length;i++) {");
        theWriter.println("             theResult+=String.fromCharCode(theArray[i]);");
        theWriter.println("         }");
        theWriter.println("         return theResult;");
        theWriter.println("     },");

        theWriter.println();
        theWriter.println("     dynamicType : function(aFunction,staticArguments) { ");
        theWriter.println("         return new Proxy({}, {");
        theWriter.println("             get: function(target, name) {");
        theWriter.println("                 return function() {");
        theWriter.println("                    var args = Array.prototype.slice.call(arguments);");
        theWriter.println("                    return aFunction.apply(target, staticArguments.data.concat(args.splice(1)));");
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

        theWriter.println("     imports : {");
        theWriter.println("         system : {");
        theWriter.println("             currentTimeMillis: function() {");
        theWriter.println("                 return Date.now();");
        theWriter.println("             },");
        theWriter.println("             nanoTime: function() {");
        theWriter.println("                 return Date.now() * 1000000;");
        theWriter.println("             },");
        theWriter.println("             writeByteArrayToConsole: function(thisRef, p1) {");
        theWriter.println("                 bytecoder.logByteArrayAsString(p1);");
        theWriter.println("             },");
        theWriter.println("             logDebugObject: function(thisref, p1) {");
        theWriter.println("                 bytecoder.logDebug(p1);");
        theWriter.println("             },");
        theWriter.println("         },");
        theWriter.println("         printstream : {");
        theWriter.println("             logDebug: function(p1) {");
        theWriter.println("                 bytecoder.logDebug(p1);");
        theWriter.println("             },");
        theWriter.println("         },");
        theWriter.println("         opaquearrays : {");
        theWriter.println("             createIntArrayINT: function(p1) {");
        theWriter.println("                 return new Int32Array(p1);");
        theWriter.println("             },");
        theWriter.println("             createFloatArrayINT: function(p1) {");
        theWriter.println("                 return new Float32Array(p1);");
        theWriter.println("             },");
        theWriter.println("             createObjectArray: function() {");
        theWriter.println("                 return [];");
        theWriter.println("             },");
        theWriter.println("         },");
        theWriter.println("         math : {");
        theWriter.println("             ceilDOUBLE: function(p1) {");
        theWriter.println("                 return Math.ceil(p1);");
        theWriter.println("             },");
        theWriter.println("             floorDOUBLE: function(p1) {");
        theWriter.println("                 return Math.floor(p1);");
        theWriter.println("             },");
        theWriter.println("             sinDOUBLE: function(p1) {");
        theWriter.println("                 return Math.sin(p1);");
        theWriter.println("             },");
        theWriter.println("             cosDOUBLE: function(p1) {");
        theWriter.println("                 return Math.cos(p1);");
        theWriter.println("             },");
        theWriter.println("             sqrtDOUBLE: function(p1) {");
        theWriter.println("                 return Math.sqrt(p1);");
        theWriter.println("             },");
        theWriter.println("             roundDOUBLE: function(p1) {");
        theWriter.println("                 return Math.round(p1);");
        theWriter.println("             },");
        theWriter.println("             NaN: function(p1) {");
        theWriter.println("                 return NaN;");
        theWriter.println("             },");
        theWriter.println("             atan2DOUBLEDOUBLE: function(p1, p2) {");
        theWriter.println("                 return Math.atan2(p1, p2);");
        theWriter.println("             },");
        theWriter.println("             maxLONGLONG: function(p1, p2) {");
        theWriter.println("                 return Math.max(p1, p2);");
        theWriter.println("             },");
        theWriter.println("             maxINTINT: function(p1, p2) {");
        theWriter.println("                 return Math.max(p1, p2);");
        theWriter.println("             },");
        theWriter.println("             maxDOUBLEDOUBLE: function(p1, p2) {");
        theWriter.println("                 return Math.max(p1, p2);");
        theWriter.println("             },");
        theWriter.println("             random: function() {");
        theWriter.println("                 return Math.random();");
        theWriter.println("             },");
        theWriter.println("             tanDOUBLE: function(p1) {");
        theWriter.println("                 return Math.tan(p1);");
        theWriter.println("             },");
        theWriter.println("             toRadiansDOUBLE: function(p1) {");
        theWriter.println("                 return p1 * (Math.PI / 180);");
        theWriter.println("             },");
        theWriter.println("             toDegreesDOUBLE: function(p1) {");
        theWriter.println("                 return p1 * (180 / Math.PI);");
        theWriter.println("             },");
        theWriter.println("             minINTINT: function (p1, p2) {");
        theWriter.println("                 return Math.min(p1, p2);");
        theWriter.println("             },");
        theWriter.println("             minFLOATFLOAT: function (p1, p2) {");
        theWriter.println("                 return Math.min(p1, p2);");
        theWriter.println("             },");
        theWriter.println("             minLONGLONG: function (p1, p2) {");
        theWriter.println("                 return Math.min(p1, p2);");
        theWriter.println("             },");
        theWriter.println("             minDOUBLEDOUBLE: function (p1, p2) {");
        theWriter.println("                 return Math.min(p1, p2);");
        theWriter.println("             },");
        theWriter.println("             add: function(p1, p2) {");
        theWriter.println("                 return p1 + p2;");
        theWriter.println("             },");
        theWriter.println("             random: function() {");
        theWriter.println("                 return Math.random();");
        theWriter.println("             },");
        theWriter.println("         },");
        theWriter.println("         strictmath : {");
        theWriter.println("             sinDOUBLE: function(p1) {");
        theWriter.println("                 return Math.sin(p1);");
        theWriter.println("             },");
        theWriter.println("             cosDOUBLE: function(p1) {");
        theWriter.println("                 return Math.cos(p1);");
        theWriter.println("             },");
        theWriter.println("             ceilDOUBLE: function(p1) {");
        theWriter.println("                 return Math.ceil(p1);");
        theWriter.println("             },");
        theWriter.println("             floorDOUBLE: function(p1) {");
        theWriter.println("                 return Math.floor(p1);");
        theWriter.println("             },");
        theWriter.println("             sqrtDOUBLE: function(p1) {");
        theWriter.println("                 return Math.sqrt(p1);");
        theWriter.println("             },");
        theWriter.println("             roundDOUBLE: function(p1) {");
        theWriter.println("                 return Math.round(p1);");
        theWriter.println("             },");
        theWriter.println("             atan2DOUBLEDOUBLE: function(p1, p2) {");
        theWriter.println("                 return Math.atan2(p1, p2);");
        theWriter.println("             },");
        theWriter.println("         },");
        theWriter.println("         runtime : {");
        theWriter.println("             nativewindow: function() {");
        theWriter.println("                 return window;");
        theWriter.println("             },");
        theWriter.println("             nativeconsole: function() {");
        theWriter.println("                 return console;");
        theWriter.println("             },");
        theWriter.println("         },");
        theWriter.println("     },");
        theWriter.println();

        theWriter.println("     exports : {},");
        theWriter.println();

        theWriter.println("     stringpool : [],");
        theWriter.println();

        theWriter.println("};");
        theWriter.println();

        final ConstantPool thePool = new ConstantPool();

        aLinkerContext.linkedClasses().forEach(theEntry -> {

            final BytecodeLinkedClass theLinkedClass = theEntry.targetNode();
            final BytecodeResolvedMethods theMethods = theLinkedClass.resolvedMethods();

            final String theJSClassName = JSWriterUtils.toClassName(theEntry.edgeType().objectTypeRef());
            theWriter.println("var " + theJSClassName + " = {");

            // First of all, we add static fields required by the framework
            theWriter.println("    __initialized : false,");
            theWriter.println("    __staticCallSites : [],");
            theWriter.print("    __typeId : ");
            theWriter.print(theLinkedClass.getUniqueId());
            theWriter.println(",");
            theWriter.print("    __implementedTypes : [");
            {
                boolean first = true;
                for (final BytecodeLinkedClass theType : theLinkedClass.getImplementingTypes()) {
                    if (!first) {
                        theWriter.print(",");
                    }
                    first = false;
                    theWriter.print(theType.getUniqueId());
                }
            }
            theWriter.println("],");

            // then we add class specific static fields
            final BytecodeResolvedFields theStaticFields = theLinkedClass.resolvedFields();
            theStaticFields.streamForStaticFields().forEach(
                    aFieldEntry -> {
                        final BytecodeTypeRef theFieldType = aFieldEntry.getValue().getTypeRef();
                        if (theFieldType.isPrimitive()) {
                            final BytecodePrimitiveTypeRef thePrimitive = (BytecodePrimitiveTypeRef) theFieldType;
                            if (thePrimitive == BytecodePrimitiveTypeRef.BOOLEAN) {
                                theWriter.print("    ");
                                theWriter.print(aFieldEntry.getValue().getName().stringValue());
                                theWriter.print(" : false, // declared in ");
                                theWriter.println(aFieldEntry.getProvidingClass().getClassName().name());
                            } else {
                                theWriter.print("    ");
                                theWriter.print(aFieldEntry.getValue().getName().stringValue());
                                theWriter.print(" : 0, // declared in ");
                                theWriter.println(aFieldEntry.getProvidingClass().getClassName().name());
                            }
                        } else {
                            theWriter.print("    ");
                            theWriter.print(aFieldEntry.getValue().getName().stringValue());
                            theWriter.print(" : null, // declared in ");
                            theWriter.println(aFieldEntry.getProvidingClass().getClassName().name());
                        }
                    });
            theWriter.println();

            if (!theLinkedClass.getBytecodeClass().getAccessFlags().isAbstract()) {
                // The Constructor function initializes all object members with null
                // Only non abstract classes can be instantiated
                final BytecodeResolvedFields theInstanceFields = theLinkedClass.resolvedFields();
                theWriter.println("    Create : function() {");
                theWriter.println("        " + theJSClassName + ".init();");
                theInstanceFields.streamForInstanceFields().forEach(
                        aFieldEntry -> {
                            if (aFieldEntry.getProvidingClass() == theLinkedClass) {
                                final BytecodeTypeRef theFieldType = aFieldEntry.getValue().getTypeRef();
                                if (theFieldType.isPrimitive()) {
                                    final BytecodePrimitiveTypeRef thePrimitive = (BytecodePrimitiveTypeRef) theFieldType;
                                    if (thePrimitive == BytecodePrimitiveTypeRef.BOOLEAN) {
                                        theWriter.print("        this.");
                                        theWriter.print(aFieldEntry.getValue().getName().stringValue());
                                        theWriter.print(" = false; // declared in ");
                                        theWriter.println(aFieldEntry.getProvidingClass().getClassName().name());
                                    } else {
                                        theWriter.print("        this.");
                                        theWriter.print(aFieldEntry.getValue().getName().stringValue());
                                        theWriter.print(" = 0; // declared in ");
                                        theWriter.println(aFieldEntry.getProvidingClass().getClassName().name());
                                    }
                                } else {
                                    theWriter.print("        this.");
                                    theWriter.print(aFieldEntry.getValue().getName().stringValue());
                                    theWriter.print(" = null; // declared in ");
                                    theWriter.println(aFieldEntry.getProvidingClass().getClassName().name());
                                }
                            }
                        });
                theWriter.println("    },");
                theWriter.println();
            }

            if (!theLinkedClass.getBytecodeClass().getAccessFlags().isInterface()) {
                theWriter.println("    instanceOf : function(aType) {");
                theWriter.print("        return ");
                theWriter.print(theJSClassName);
                theWriter.println(".__implementedTypes.includes(aType.__typeId);");
                theWriter.println("    },");
                theWriter.println();

                theWriter.println("    ClassgetClass : function() {");
                theWriter.print("        return ");
                theWriter.print(theJSClassName);
                theWriter.println(";");
                theWriter.println("    },");
                theWriter.println();

                theWriter.println("    jlStringgetName : function() {");
                theWriter.print("        return bytecoder.stringpool[");
                theWriter.print(thePool.register(new StringValue(ConstantPool.simpleClassName(theLinkedClass.getClassName().name()))));
                theWriter.println("];");
                theWriter.println("    },");
                theWriter.println();

                theWriter.println("    BOOLEANdesiredAssertionStatus : function() {");
                theWriter.println("        return false;");
                theWriter.println("    },");
                theWriter.println();

                theWriter.println("    A1jlObjectgetEnumConstants : function(aClazz) {");
                theWriter.println("        return aClazz.$VALUES;");
                theWriter.println("    },");
            }

            theMethods.stream().forEach(aEntry -> {
                final BytecodeMethod theMethod = aEntry.getValue();
                final BytecodeMethodSignature theCurrentMethodSignature = theMethod.getSignature();

                // If the method is provided by the runtime, we do not need to generate the implementation
                if (null != theMethod.getAttributes().getAnnotationByType(EmulatedByRuntime.class.getName())) {
                    return;
                }

                // Do not generate code for abstract methods
                if (theMethod.getAccessFlags().isAbstract()) {
                    return;
                }

                if (!(aEntry.getProvidingClass() == theLinkedClass)) {
                    // Skip methods not implemented in this class
                    // But include static methods, as they are inherited from the base classes
                    if (aEntry.getValue().getAccessFlags().isStatic() && !aEntry.getValue().isClassInitializer()) {

                        final StringBuilder theArguments = new StringBuilder();
                        for (int i=0;i<theCurrentMethodSignature.getArguments().length;i++) {
                            if (0 < i) {
                                theArguments.append(',');
                            }
                            theArguments.append('p');
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

                aLinkerContext.getLogger().info("Compiling {}.{}", theLinkedClass.getClassName().name(), theMethod.getName().stringValue());

                final ProgramGenerator theGenerator = programGeneratorFactory.createFor(aLinkerContext);
                final Program theSSAProgram = theGenerator.generateFrom(aEntry.getProvidingClass().getBytecodeClass(), theMethod);

                //Run optimizer
                aOptions.getOptimizer().optimize(theSSAProgram.getControlFlowGraph(), aLinkerContext);

                final StringBuilder theArguments = new StringBuilder();
                for (final Program.Argument theArgument : theSSAProgram.getArguments()) {
                    if (0 < theArguments.length()) {
                        theArguments.append(',');
                    }
                    theArguments.append(theArgument.getVariable().getName());
                }

                if (theMethod.getAccessFlags().isNative()) {
                    if (null != theLinkedClass.getBytecodeClass().getAttributes()
                            .getAnnotationByType(EmulatedByRuntime.class.getName())) {
                        return;
                    }

                    final BytecodeImportedLink theLink = theLinkedClass.linkfor(theMethod);

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

                if (aOptions.isDebugOutput()) {
                    theWriter.println("        /**");
                    theWriter.println("        " + theSSAProgram.getControlFlowGraph().toDOT());
                    theWriter.println("        */");
                }

                final JSSSAWriter theVariablesWriter = new JSSSAWriter(aOptions, theSSAProgram, "        ", theWriter, aLinkerContext, thePool, false);
                for (final Variable theVariable : theSSAProgram.globalVariables()) {
                    if (!theVariable.isSynthetic()) {
                        theVariablesWriter.print("var ");
                        theVariablesWriter.print(theVariable.getName());
                        theVariablesWriter.print(" = null;");
                        if (aOptions.isDebugOutput()) {
                            theVariablesWriter.print(" // type is ");
                            theVariablesWriter.print(theVariable.resolveType().resolve().name());
                            theVariablesWriter.print(" # of inits = " + theVariable.incomingDataFlows().size());
                        }
                        theVariablesWriter.println();
                    }
                }

                // Try to reloop it!
                try {
                    final Relooper theRelooper = new Relooper(aOptions);
                    final Relooper.Block theReloopedBlock = theRelooper.reloop(theSSAProgram.getControlFlowGraph());

                    theVariablesWriter.printRelooped(theReloopedBlock);
                } catch (final Exception e) {
                    System.out.println(theSSAProgram.getControlFlowGraph().toDOT());
                    throw new IllegalStateException("Error relooping cfg for " + theLinkedClass.getClassName().name() + '.'
                            + theMethod.getName().stringValue(), e);
                }

                theWriter.println("    },");
            });

            theWriter.println();
            theWriter.println("    init : function() {");
            theWriter.println("        if (!" + theJSClassName + ".__initialized) {");
            theWriter.println("            " + theJSClassName + ".__initialized = true;");

            if (!theLinkedClass.getClassName().name().equals(Object.class.getName())) {
                final BytecodeLinkedClass theSuper = theLinkedClass.getSuperClass();
                final String theSuperJSName = JSWriterUtils.toClassName(theSuper.getClassName());
                theWriter.println("            " + theSuperJSName + ".init();");
            }

            if (theLinkedClass.hasClassInitializer()) {
                theWriter.println("            " + theJSClassName + ".VOIDclinit();");
            }

            theWriter.println("        }");
            theWriter.println("        return " + theJSClassName + ";");
            theWriter.println("    },");
            theWriter.println();

            theWriter.println();
            theWriter.println("    link : function() {");

            if (!theLinkedClass.getBytecodeClass().getAccessFlags().isAbstract()) {
                // Now we have to setup the prototype
                // Only in case this class can be instantiated of course
                theWriter.println("        var thePrototype = " + theJSClassName + ".Create.prototype;");
                theWriter.println("        thePrototype.instanceOf = " + theJSClassName + ".instanceOf;");
                theWriter.println("        thePrototype.ClassgetClass = " + theJSClassName + ".ClassgetClass;");

                final List<BytecodeResolvedMethods.MethodEntry> theEntries = theMethods.stream().collect(Collectors.toList());
                final Set<String> theVisitedMethods = new HashSet<>();

                for (int i = theEntries.size()-1; 0 <= i; i--) {
                    final BytecodeResolvedMethods.MethodEntry aEntry = theEntries.get(i);
                    final BytecodeMethod theMethod = aEntry.getValue();

                    final String theMethodName = JSWriterUtils.toMethodName(theMethod.getName().stringValue(), theMethod.getSignature());
                    if (!theMethod.getAccessFlags().isStatic() &&
                            !theMethod.getAccessFlags().isAbstract() &&
                            !theMethod.isConstructor() &&
                            !theMethod.isClassInitializer()) {

                        if (theVisitedMethods.add(theMethodName)) {
                            theWriter.print("        thePrototype.");
                            theWriter.print(theMethodName);
                            theWriter.print(" = ");
                            theWriter.print(JSWriterUtils.toClassName(aEntry.getProvidingClass().getClassName()));
                            theWriter.print(".");
                            theWriter.print(theMethodName);
                            theWriter.println(";");
                        }
                    }
                }
            }

            theWriter.println("    },");
            theWriter.println();

            theWriter.println("};");
            theWriter.println();
        });

        theWriter.println();
        theWriter.println("bytecoder.bootstrap = function() {");
        final List<StringValue> theValues = thePool.stringValues();
        for (int i=0; i<theValues.size(); i++) {
            final StringValue theValue = theValues.get(i);
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
                theWriter.println(".link();");
            }
            final BytecodeResolvedMethods theMethods = aEntry.targetNode().resolvedMethods();
            theMethods.stream().forEach(eMethod -> {
                final BytecodeMethod theMethod = eMethod.getValue();
                final BytecodeMethodSignature theCurrentMethodSignature = theMethod.getSignature();

                // If the method is provided by the runtime, we do not need to generate the implementation
                final BytecodeAnnotation theAnnotation = theMethod.getAttributes().getAnnotationByType(Export.class.getName());
                if (theAnnotation != null) {
                    theWriter.print("    bytecoder.exports.");
                    theWriter.print(theAnnotation.getElementValueByName("value").stringValue());
                    theWriter.print(" = ");

                    theWriter.print(JSWriterUtils.toClassName(aEntry.targetNode().getClassName()));
                    theWriter.print(".");
                    theWriter.print(JSWriterUtils.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature));
                    theWriter.println(";");

                }
            });
        });

        theWriter.println("}");

        theWriter.flush();

        return new JSCompileResult(new JSCompileResult.JSContent(aOptions.getFilenamePrefix() + ".js", theStrWriter.toString()));
    }
}