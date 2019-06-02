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
import de.mirkosertic.bytecoder.backend.SourceMapWriter;
import de.mirkosertic.bytecoder.classlib.ExceptionManager;
import de.mirkosertic.bytecoder.core.BytecodeAnnotation;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeImportedLink;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
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

        final SourceMapWriter theSourceMapWriter = new SourceMapWriter();
        final JSMinifier theMinifier = new JSMinifier(aOptions);

        final BytecodeLinkedClass theExceptionManager = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(
                ExceptionManager.class));
        theExceptionManager.resolveStaticMethod("push", pushExceptionSignature);
        theExceptionManager.resolveStaticMethod("pop", popExceptionSignature);
        theExceptionManager.resolveStaticMethod("lastExceptionOrNull", popExceptionSignature);

        final StringWriter theStrWriter = new StringWriter();
        final JSPrintWriter theWriter = new JSPrintWriter(theStrWriter, theMinifier, theSourceMapWriter);
        theWriter.print("'use strict';").newLine();

        theWriter.text("var bytecoder").assign().text("{").newLine();

        theWriter.tab().text("logDebug").colon().text("function(aValue)").space().text("{").newLine();
        theWriter.tab(2).text("console.log(aValue);").newLine();
        theWriter.tab().text("},").newLine();
        theWriter.tab().text("logByteArrayAsString").colon().text("function(aArray)").space().text("{").newLine();
        theWriter.tab(2).text("var theResult").assign().text("'';").newLine();
        theWriter.tab(2).text("for").space().text("(var i=0;i<aArray.data.length;i++)").space().text("{").newLine();
        theWriter.tab(2).tab().text("theResult").space().text("+=").space().text("String.fromCharCode(aArray.data[i]);").newLine();
        theWriter.tab(2).text("}").newLine();
        theWriter.tab(2).text("console.log(theResult);").newLine();
        theWriter.tab().text("},").newLine();

        final BytecodeObjectTypeRef theStringTypeRef = BytecodeObjectTypeRef.fromRuntimeClass(String.class);
        final BytecodeObjectTypeRef theArrayTypeRef = BytecodeObjectTypeRef.fromRuntimeClass(Array.class);

        final BytecodeMethodSignature theStringConstructorSignature = new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID,
                new BytecodeTypeRef[]{new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.BYTE, 1)});

        // Construct a String
        theWriter.tab().text("newString").colon().text("function(aByteArray)").space().text("{").newLine();
        theWriter.tab(2).text("var theNewString").assign().text("new ").text(theMinifier.toClassName(theStringTypeRef)).text(".C();").newLine();
        theWriter.tab(2).text("var theBytes").assign().text("new ").text(theMinifier.toClassName(theArrayTypeRef)).text(".C();").newLine();
        theWriter.tab(2).text("theBytes.data").assign().text("aByteArray;").newLine();
        theWriter.tab(2).text(theMinifier.toClassName(theStringTypeRef)).text(".").text(theMinifier.toMethodName("init", theStringConstructorSignature)).text("(theNewString,theBytes);").newLine();
        theWriter.tab(2).text("return theNewString;").newLine();
        theWriter.tab().text("},").newLine();

        theWriter.tab().text("newMultiArray").colon().text("function(aDimensions,aDefault)").space().text("{").newLine();
        theWriter.tab(2).text("var theLength").assign().text("aDimensions[0];").newLine();
        theWriter.tab(2).text("var theArray").assign().text("bytecoder.newArray(theLength,aDefault);").newLine();
        theWriter.tab(2).text("if").space().text("(aDimensions.length").space().text(">").space().text("1)").space().text("{").newLine();
        theWriter.tab(3).text("var theNewDimensions").assign().text("aDimensions.slice(0);").newLine();
        theWriter.tab(3).text("theNewDimensions.shift();").newLine();
        theWriter.tab(3).text("for").space().text("(var i=0;i<theLength;i++)").space().text("{").newLine();
        theWriter.tab(4).text("theArray.data[i]").assign().text("bytecoder.newMultiArray(theNewDimensions,aDefault);").newLine();
        theWriter.tab(3).text("}").newLine();
        theWriter.tab(2).text("}").newLine();
        theWriter.tab(2).text("return theArray;").newLine();
        theWriter.tab().text("},").newLine();

        final BytecodeObjectTypeRef theArrayType = BytecodeObjectTypeRef.fromRuntimeClass(Array.class);
        theWriter.tab().text("newArray").colon().text("function(aLength,aDefault)").space().text("{").newLine();
        theWriter.tab(2).text("var theInstance").assign().text("new ").text(theMinifier.toClassName(theArrayType)).text(".C();").newLine();
        theWriter.tab(2).text("theInstance.data").assign().text("[];").newLine();
        theWriter.tab(2).text("theInstance.data.length").assign().text("aLength;").newLine();
        theWriter.tab(2).text("for").space().text("(var i=0;i<aLength;i++)").space().text("{").newLine();
        theWriter.tab(3).text("theInstance.data[i]").assign().text("aDefault;").newLine();
        theWriter.tab(2).text("}").newLine();
        theWriter.tab(2).text("return theInstance;").newLine();
        theWriter.tab().text("},").newLine();

        theWriter.tab().text("toBytecoderString").colon().text("function(aJSString)").space().text("{").newLine();
        theWriter.tab(2).text("var theLength").assign().text("aJSString.length;").newLine();
        theWriter.tab(2).text("var theArray").assign().text("[];").newLine();
        theWriter.tab(2).text("for").space ().text("(var i=0;i<theLength;i++)").space().text("{").newLine();
        theWriter.tab(3).text("theArray.push(aJSString.charCodeAt(i));").newLine();
        theWriter.tab(2).text("}").newLine();
        theWriter.tab(2).text("return bytecoder.newString(theArray);").newLine();
        theWriter.tab().text("},").newLine();

        theWriter.tab().text("toJSString").colon().text("function(aBytecoderString)").space().text("{").newLine();
        theWriter.tab(2).text("if").space().text("(aBytecoderString").space().text("==").space().text("null)").space().text("{").newLine();
        theWriter.tab(3).text("return 'NULL';").newLine();
        theWriter.tab(2).text("}").newLine();
        theWriter.tab(2).text("if").space().text("(typeof(aBytecoderString)").space().text("===").space().text("'string')").space().text("{").newLine();
        theWriter.tab(3).text("return aBytecoderString;").newLine();
        theWriter.tab(2).text("}").newLine();
        theWriter.tab(2).text("var theArray").assign().text("aBytecoderString.").text(theMinifier.toSymbol("data")).text(".data").text(";").newLine();
        theWriter.tab(2).text("var theResult = '';").newLine();
        theWriter.tab(2).text("for").space().text("(var i=0;i<theArray.length;i++)").space().text("{").newLine();
        theWriter.tab(3).text("theResult+=String.fromCharCode(theArray[i]);").newLine();
        theWriter.tab(2).text("}").newLine();
        theWriter.tab(2).text("return theResult;").newLine();
        theWriter.tab().text("},").newLine();

        theWriter.tab().text("dynamicType").colon().text("function(aFunction,staticArguments)").space().text("{").newLine();
        theWriter.tab(2).text("return new Proxy({},{").newLine();
        theWriter.tab(3).text("get").colon().text("function(target,name)").space().text("{").newLine();
        theWriter.tab(4).text("return function()").space().text("{").newLine();
        theWriter.tab(5).text("var args").assign().text("Array.prototype.slice.call(arguments);").newLine();
        theWriter.tab(5).text("return aFunction.apply(target,staticArguments.data.concat(args.splice(1)));").newLine();
        theWriter.tab(4).text("}").newLine();
        theWriter.tab(3).text("}").newLine();
        theWriter.tab(2).text("});").newLine();
        theWriter.tab().text("},").newLine();

        theWriter.tab().text("resolveStaticCallSiteObject").colon().text("function(aWhere,aKey,aProducerFunction)").space().text("{").newLine();
        theWriter.tab(2).text("var resolvedCallsiteObject").assign().text("aWhere.__staticCallSites[aKey];").newLine();
        theWriter.tab(2).text("if").space().text("(resolvedCallsiteObject").space().text("==").space().text("null)").space().text("{").newLine();
        theWriter.tab(3).text("resolvedCallsiteObject").assign().text("aProducerFunction();").newLine();
        theWriter.tab(3).text("aWhere.__staticCallSites[aKey]").assign().text("resolvedCallsiteObject;").newLine();
        theWriter.tab(2).text("}").newLine();
        theWriter.tab(2).text("return resolvedCallsiteObject;").newLine();
        theWriter.tab().text("},").newLine();

        theWriter.tab().text("imports").colon().text("{").newLine();
        theWriter.tab(2).text("system").colon().text("{").newLine();
        theWriter.tab(3).text("currentTimeMillis").colon().text("function()").space().text("{").newLine();
        theWriter.tab(4).text("return Date.now();").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("nanoTime").colon().text("function()").space().text("{").newLine();
        theWriter.tab(4).text("return Date.now() * 1000000;").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("writeByteArrayToConsole").colon().text("function(").text(Variable.THISREF_NAME).text(",p1)").space().text("{").newLine();
        theWriter.tab(4).text("bytecoder.logByteArrayAsString(p1);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("logDebugObject").colon().text("function(").text(Variable.THISREF_NAME).text(",p1)").space().text("{").newLine();
        theWriter.tab(4).text("bytecoder.logDebug(p1);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("printstream").colon().text("{").newLine();
        theWriter.tab(3).text("logDebug").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("bytecoder.logDebug(p1);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("opaquearrays").colon().text("{").newLine();
        theWriter.tab(3).text("createIntArrayINT").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("return new Int32Array(p1);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("createFloatArrayINT").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("return new Float32Array(p1);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("createObjectArray").colon().text("function()").space().text("{").newLine();
        theWriter.tab(4).text("return [];").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("createInt8ArrayINT").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("return new Int8Array(p1);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("math").colon().text("{").newLine();
        theWriter.tab(3).text("ceilDOUBLE").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.ceil(p1);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("floorDOUBLE").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.floor(p1);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("sinDOUBLE").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.sin(p1);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("cosDOUBLE").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.cos(p1);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("sqrtDOUBLE").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.sqrt(p1);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("roundDOUBLE").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.round(p1);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("NaN").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("return NaN;").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("atan2DOUBLEDOUBLE").colon().text("function(p1,p2)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.atan2(p1,p2);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("maxLONGLONG").colon().text("function(p1,p2)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.max(p1, p2);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("maxINTINT").colon().text("function(p1,p2)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.max(p1,p2);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("maxDOUBLEDOUBLE").colon().text("function(p1,p2)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.max(p1,p2);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("random").colon().text("function()").space().text("{").newLine();
        theWriter.tab(4).text("return Math.random();").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("tanDOUBLE").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.tan(p1);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("toRadiansDOUBLE").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("return p1*(Math.PI/180);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("toDegreesDOUBLE").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("return p1*(180/Math.PI);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("minINTINT").colon().text("function(p1,p2)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.min(p1,p2);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("minFLOATFLOAT").colon().text("function(p1,p2)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.min(p1,p2);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("minLONGLONG").colon().text("function(p1,p2)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.min(p1,p2);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("minDOUBLEDOUBLE").colon().text("function(p1,p2)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.min(p1,p2);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("add").colon().text("function(p1,p2)").space().text("{").newLine();
        theWriter.tab(4).text("return p1+p2;").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("random").colon().text("function()").space().text("{").newLine();
        theWriter.tab(4).text("return Math.random();").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("strictmath").colon().text("{").newLine();
        theWriter.tab(3).text("sinDOUBLE").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.sin(p1);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("cosDOUBLE").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.cos(p1);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("ceilDOUBLE").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.ceil(p1);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("floorDOUBLE").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.floor(p1);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("sqrtDOUBLE").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.sqrt(p1);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("roundDOUBLE").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.round(p1);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("atan2DOUBLEDOUBLE").colon().text("function(p1,p2)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.atan2(p1,p2);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("runtime").space().text(":").text("{").newLine();
        theWriter.tab(3).text("nativewindow").colon().text("function()").space().text("{").newLine();
        theWriter.tab(4).text("return window;").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("nativeconsole").colon().text("function()").space().text("{").newLine();
        theWriter.tab(4).text("return console;").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();
        theWriter.tab(1).text("},").newLine();

        theWriter.tab().text("exports").colon().text("{},").newLine();

        theWriter.tab().text("stringpool").colon().text("[],").newLine();

        theWriter.text("};").newLine();

        final String theGetClassMethodName = theMinifier.toMethodName("getClass", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Class.class), new BytecodeTypeRef[0]));
        final String theGetNameMethodName = theMinifier.toMethodName("getName", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(String.class), new BytecodeTypeRef[0]));
        final String theDesiredAssertionStatusMethodName = theMinifier.toMethodName("desiredAssertionStatus", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.BOOLEAN, new BytecodeTypeRef[0]));
        final String theGetEnumConstantsMethodName = theMinifier.toMethodName("getEnumConstants", new BytecodeMethodSignature(new BytecodeArrayTypeRef(BytecodeObjectTypeRef.fromRuntimeClass(Object.class),1), new BytecodeTypeRef[0]));

        final ConstantPool thePool = new ConstantPool();

        aLinkerContext.linkedClasses().forEach(theEntry -> {

            final BytecodeLinkedClass theLinkedClass = theEntry.targetNode();
            final BytecodeResolvedMethods theMethods = theLinkedClass.resolvedMethods();

            final String theJSClassName = theMinifier.toClassName(theEntry.edgeType().objectTypeRef());
            theWriter.text("var ").text(theJSClassName).assign().text("{").newLine();

            // First of all, we add static fields required by the framework
            theWriter.tab().text("__").symbol("initialized", null).colon().text("false,").newLine();
            theWriter.tab().text("__staticCallSites").colon().text("[],").newLine();
            theWriter.tab().text("__typeId").colon().text("" + theLinkedClass.getUniqueId()).text(",").newLine();
            theWriter.tab().text("__implementedTypes").colon().text("[");
            {
                boolean first = true;
                for (final BytecodeLinkedClass theType : theLinkedClass.getImplementingTypes()) {
                    if (!first) {
                        theWriter.print(",");
                    }
                    first = false;
                    theWriter.print("" + theType.getUniqueId());
                }
            }
            theWriter.text("],").newLine();

            // then we add class specific static fields
            final BytecodeResolvedFields theStaticFields = theLinkedClass.resolvedFields();
            theStaticFields.streamForStaticFields().forEach(
                    aFieldEntry -> {
                        final BytecodeTypeRef theFieldType = aFieldEntry.getValue().getTypeRef();
                        if (theFieldType.isPrimitive()) {
                            final BytecodePrimitiveTypeRef thePrimitive = (BytecodePrimitiveTypeRef) theFieldType;
                            if (thePrimitive == BytecodePrimitiveTypeRef.BOOLEAN) {
                                theWriter.tab().symbol(aFieldEntry.getValue().getName().stringValue(), null).colon().text("false,").newLine();
                            } else {
                                theWriter.tab().symbol(aFieldEntry.getValue().getName().stringValue(), null).colon().text("0,").newLine();
                            }
                        } else {
                            theWriter.tab().symbol(aFieldEntry.getValue().getName().stringValue(), null).colon().text("null,").newLine();
                        }
                    });

            if (!theLinkedClass.getBytecodeClass().getAccessFlags().isAbstract()) {
                // The Constructor function initializes all object members with null
                // Only non abstract classes can be instantiated
                final BytecodeResolvedFields theInstanceFields = theLinkedClass.resolvedFields();
                theWriter.tab().text("C").colon().text("function()").space().text("{").newLine();
                theWriter.tab(2).text(theJSClassName).text(".i();").newLine();
                theInstanceFields.streamForInstanceFields().forEach(
                        aFieldEntry -> {
                            final BytecodeTypeRef theFieldType = aFieldEntry.getValue().getTypeRef();
                            if (theFieldType.isPrimitive()) {
                                final BytecodePrimitiveTypeRef thePrimitive = (BytecodePrimitiveTypeRef) theFieldType;
                                if (thePrimitive == BytecodePrimitiveTypeRef.BOOLEAN) {
                                    theWriter.tab(2).text("this.").text(theMinifier.toSymbol(aFieldEntry.getValue().getName().stringValue())).assign().text("false;").newLine();
                                } else {
                                    theWriter.tab(2).text("this.").text(theMinifier.toSymbol(aFieldEntry.getValue().getName().stringValue())).assign().text("0;").newLine();
                                }
                            } else {
                                theWriter.tab(2).text("this.").text(theMinifier.toSymbol(aFieldEntry.getValue().getName().stringValue())).assign().text("null;").newLine();
                            }
                        });
                theWriter.tab().text("},").newLine();
            }

            if (!theLinkedClass.getBytecodeClass().getAccessFlags().isInterface()) {
                theWriter.tab().text("iof").colon().text("function(aType)").space().text("{").newLine();
                theWriter.tab(2).text("return ").text(theJSClassName).text(".__implementedTypes.includes(aType.__typeId);").newLine();
                theWriter.tab().text("},").newLine();

                theWriter.tab().text(theGetClassMethodName).colon().text("function()").space().text("{").newLine();
                theWriter.tab(2).text("return ").text(theJSClassName).text(";").newLine();
                theWriter.tab().text("},").newLine();

                theWriter.tab().text(theGetNameMethodName).colon().text("function()").space().text("{").newLine();
                theWriter.tab(2).text("return bytecoder.stringpool[").text("" + thePool.register(new StringValue(ConstantPool.simpleClassName(theLinkedClass.getClassName().name())))).text("];").newLine();
                theWriter.tab().text("},").newLine();

                theWriter.tab().text(theDesiredAssertionStatusMethodName).colon().text("function()").space().text("{").newLine();
                theWriter.tab(2).text("return false;").newLine();
                theWriter.tab().text("},").newLine();

                theWriter.tab().text(theGetEnumConstantsMethodName).colon().text("function(aClazz)").space().text("{").newLine();
                theWriter.tab(2).text("return aClazz.").symbol("$VALUES", null).text(";").newLine();
                theWriter.tab().text("},").newLine();
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
                        theWriter.tab().text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature)).colon().text("function(").text(theArguments.toString()).text(")").space().text("{").newLine();
                        if (!theCurrentMethodSignature.getReturnType().isVoid()) {
                            theWriter.tab(2).text("return ");
                        } else {
                            theWriter.tab(2);
                        }
                        theWriter.text(theMinifier.toClassName(aEntry.getProvidingClass().getClassName())).text(".").text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature)).text("(").text(theArguments.toString()).text(");").newLine();
                        theWriter.tab().text("},").newLine();
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
                    theArguments.append(theMinifier.toVariableName(theArgument.getVariable().getName()));
                }

                if (theMethod.getAccessFlags().isNative()) {
                    if (null != theLinkedClass.getBytecodeClass().getAttributes()
                            .getAnnotationByType(EmulatedByRuntime.class.getName())) {
                        return;
                    }

                    final BytecodeImportedLink theLink = theLinkedClass.linkfor(theMethod);

                    theWriter.tab().text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature)).colon().text("function(").text(theArguments.toString()).text(")").space().text("{").newLine();
                    theWriter.tab(2).text("return bytecoder.imports.").text(theLink.getModuleName()).text(".").text(theLink.getLinkName()).text("(").text(theArguments.toString()).text(");").newLine();
                    theWriter.tab().text("},").newLine();
                    return;
                }

                theWriter.assignSymbolToSourceFile(theLinkedClass.getClassName().name() + "." + theMethod.getName().stringValue(), theSSAProgram.getDebugInformation().debugPositionFor(
                        BytecodeOpcodeAddress.START_AT_ZERO));

                theWriter.tab().text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature)).colon().text("function(").text(theArguments.toString()).text(")").space().text("{").newLine();

                if (aOptions.isDebugOutput()) {
                    theWriter.tab(2).text("/**").newLine();
                    theWriter.tab(2).text(theSSAProgram.getControlFlowGraph().toDOT()).newLine();
                    theWriter.tab(2).text("**/").newLine();
                }

                theWriter.flush();

                final JSSSAWriter theVariablesWriter = new JSSSAWriter(aOptions, theSSAProgram, 2, theWriter, aLinkerContext, thePool, false, theMinifier);
                for (final Variable theVariable : theSSAProgram.globalVariables()) {
                    if (!theVariable.isSynthetic()) {
                        final JSPrintWriter thePW = theVariablesWriter.startLine().text("var ").text(theMinifier.toVariableName(theVariable.getName())).assign().text("null;");
                        if (aOptions.isDebugOutput()) {
                            thePW.text(" // type is ").text(theVariable.resolveType().resolve().name()).text(" # of inits = " + theVariable.incomingDataFlows().size());
                        }
                        thePW.newLine();
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

                theWriter.tab().text("},").newLine();
            });


            theWriter.tab().text("i").colon().text("function()").space().text("{").newLine();
            theWriter.tab(2).text("if").space ().text("(!").text(theJSClassName).text(".__").symbol("initialized", null).text(")").space().text("{").newLine();

            theWriter.tab(3).text(theJSClassName).text(".__").symbol("initialized", null).assign().text("true;").newLine();

            if (!theLinkedClass.getClassName().name().equals(Object.class.getName())) {
                final BytecodeLinkedClass theSuper = theLinkedClass.getSuperClass();
                final String theSuperJSName = theMinifier.toClassName(theSuper.getClassName());

                theWriter.tab(3).text(theSuperJSName).text(".i();").newLine();
            }

            if (theLinkedClass.hasClassInitializer()) {
                theWriter.tab(3).text(theJSClassName).text(".").text(theMinifier.toMethodName("clinit", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[0]))).text("();").newLine();
            }

            theWriter.tab(2).text("}").newLine();
            theWriter.tab(2).text("return ").text(theJSClassName).text(";").newLine();
            theWriter.tab(1).text("},").newLine();

            theWriter.tab(1).text("link").colon().text("function()").space().text("{").newLine();

            if (!theLinkedClass.getBytecodeClass().getAccessFlags().isAbstract()) {
                // Now we have to setup the prototype
                // Only in case this class can be instantiated of course
                theWriter.tab(2).text("var p").assign().text(theJSClassName).text(".C.prototype;").newLine();
                theWriter.tab(2).text("p.iof").assign().text(theJSClassName).text(".iof;").newLine();
                theWriter.tab(2).text("p.").text(theGetClassMethodName).assign().text(theJSClassName).text(".").text(theGetClassMethodName).text(";").newLine();

                final List<BytecodeResolvedMethods.MethodEntry> theEntries = theMethods.stream().collect(Collectors.toList());
                final Set<String> theVisitedMethods = new HashSet<>();

                for (int i = theEntries.size()-1; 0 <= i; i--) {
                    final BytecodeResolvedMethods.MethodEntry aEntry = theEntries.get(i);
                    final BytecodeMethod theMethod = aEntry.getValue();

                    final String theMethodName = theMinifier.toMethodName(theMethod.getName().stringValue(), theMethod.getSignature());
                    if (!theMethod.getAccessFlags().isStatic() &&
                            !theMethod.getAccessFlags().isAbstract() &&
                            !theMethod.isConstructor() &&
                            !theMethod.isClassInitializer()) {

                        if (theVisitedMethods.add(theMethodName)) {
                            theWriter.tab(2).text("p.").text(theMethodName).assign().text(theMinifier.toClassName(aEntry.getProvidingClass().getClassName())).text(".").text(theMethodName).text(";").newLine();
                        }
                    }
                }
            }

            theWriter.tab(1).text("},").newLine();

            theWriter.text("};").newLine();
        });

        theWriter.text("bytecoder.bootstrap").assign().text("function()").space().text("{").newLine();
        final List<StringValue> theValues = thePool.stringValues();
        for (int i=0; i<theValues.size(); i++) {
            final StringValue theValue = theValues.get(i);
            theWriter.tab().text("bytecoder.stringpool[").text("" + i).text("]").assign().text("bytecoder.newString(").text(toArray(theValue.getStringValue().getBytes())).text(");").newLine();
        }

        aLinkerContext.linkedClasses().forEach(aEntry -> {
            if (!aEntry.targetNode().getBytecodeClass().getAccessFlags().isInterface()) {
                theWriter.tab().text(theMinifier.toClassName(aEntry.edgeType().objectTypeRef())).text(".link();").newLine();
            }
            final BytecodeResolvedMethods theMethods = aEntry.targetNode().resolvedMethods();
            theMethods.stream().forEach(eMethod -> {
                final BytecodeMethod theMethod = eMethod.getValue();
                final BytecodeMethodSignature theCurrentMethodSignature = theMethod.getSignature();

                // If the method is provided by the runtime, we do not need to generate the implementation
                final BytecodeAnnotation theAnnotation = theMethod.getAttributes().getAnnotationByType(Export.class.getName());
                if (theAnnotation != null) {
                    theWriter.tab().text("bytecoder.exports.").text(theAnnotation.getElementValueByName("value").stringValue()).assign().text(theMinifier.toClassName(aEntry.targetNode().getClassName())).text(".").text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature)).text(";").newLine();
                } else {
                    // If the method is the main method and not exported by annotation, it is
                    // exported by default name "main".
                    if (aEntry.targetNode().getClassName().name().equals(aEntryPointClass.getName()) && theMethod.getName().stringValue().equals(aEntryPointMethodName)) {
                        theWriter.tab().text("bytecoder.exports.main").assign().text(theMinifier.toClassName(aEntry.targetNode().getClassName())).text(".").text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature)).text(";").newLine();
                    }
                }
            });
        });

        theWriter.text("}").newLine();

        theWriter.flush();

        theStrWriter.append(System.lineSeparator()).append("//# sourceMappingURL=").append(aOptions.getFilenamePrefix()).append(".js.map");

        return new JSCompileResult(theMinifier,
                new JSCompileResult.JSContent(aOptions.getFilenamePrefix() + ".js", theStrWriter.toString()),
                new JSCompileResult.JSContent(aOptions.getFilenamePrefix() + ".js.map", theSourceMapWriter.toSourceMap(aOptions.getFilenamePrefix() + ".js")));
    }

    public String toArray(final byte[] aData) {
        final StringBuilder theResult = new StringBuilder("[");
        for (int i=0;i<aData.length;i++) {
            if (i>0) {
                theResult.append(",");
            }
            theResult.append(aData[i]);
        }
        theResult.append("]");
        return theResult.toString();
    }
}