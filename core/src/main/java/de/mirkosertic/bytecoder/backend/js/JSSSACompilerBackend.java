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

import de.mirkosertic.bytecoder.allocator.AbstractAllocator;
import de.mirkosertic.bytecoder.api.EmulatedByRuntime;
import de.mirkosertic.bytecoder.api.Export;
import de.mirkosertic.bytecoder.backend.CompileBackend;
import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.ConstantPool;
import de.mirkosertic.bytecoder.backend.SourceMapWriter;
import de.mirkosertic.bytecoder.classlib.Array;
import de.mirkosertic.bytecoder.classlib.ExceptionManager;
import de.mirkosertic.bytecoder.core.BytecodeAnnotation;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeClassTopologicOrder;
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
import de.mirkosertic.bytecoder.stackifier.HeadToHeadControlFlowException;
import de.mirkosertic.bytecoder.stackifier.Stackifier;

import java.io.StringWriter;
import java.util.List;

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

        theWriter.tab().text("logCharArrayAsString").colon().text("function(aArray)").space().text("{").newLine();
        theWriter.tab(2).text("var theResult").assign().text("'';").newLine();
        theWriter.tab(2).text("for").space().text("(var i=0;i<aArray.data.length;i++)").space().text("{").newLine();
        theWriter.tab(2).tab().text("theResult").space().text("+=").space().text("String.fromCharCode(aArray.data[i]);").newLine();
        theWriter.tab(2).text("}").newLine();
        theWriter.tab(2).text("console.log(theResult);").newLine();
        theWriter.tab().text("},").newLine();

        final BytecodeObjectTypeRef theStringTypeRef = BytecodeObjectTypeRef.fromRuntimeClass(String.class);
        final BytecodeObjectTypeRef theArrayTypeRef = BytecodeObjectTypeRef.fromRuntimeClass(Array.class);

        final BytecodeMethodSignature theStringConstructorSignature = new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID,
                new BytecodeTypeRef[]{new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.CHAR, 1)});

        final BytecodeLinkedClass theStringClass = aLinkerContext.resolveClass(theStringTypeRef);

        // Construct a String
        theWriter.tab().text("newString").colon().text("function(aCharArray)").space().text("{").newLine();
        theWriter.tab(2).text("var theBytes").assign().text(theMinifier.toClassName(theArrayTypeRef)).text(".").text(theMinifier.toSymbol("newInstance")).text("();").newLine();
        theWriter.tab(2).text("theBytes.data").assign().text("aCharArray;").newLine();
        theWriter.tab(2).text("return ").text(theMinifier.toClassName(theStringTypeRef)).text(".").text(theMinifier.toMethodName("$newInstance", theStringConstructorSignature)).text("(theBytes);").newLine();
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
        theWriter.tab(2).text("var theInstance").assign().text(theMinifier.toClassName(theArrayType)).text(".").text(theMinifier.toSymbol("newInstance")).text("();").newLine();
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

        theWriter.tab().text("methodType").colon().text("function(ret,args)").space().text("{").newLine();
        theWriter.tab(2).text("return {returntype: ret, arguments:args};").newLine();
        theWriter.tab().text("},").newLine();

        theWriter.tab().text("dynamicType").colon().text("function(aFunction,staticArguments,name,typeToConstruct)").space().text("{").newLine();

        theWriter.tab(2).text("if").space().text("(aFunction.static)").space().text("{").newLine();

        theWriter.tab(3).text("var handler").assign().text("function()").space().text("{").newLine();
        theWriter.tab(4).text("var args").assign().text("Array.prototype.slice.call(arguments);").newLine();
        theWriter.tab(5).text("var concated").assign().text("staticArguments.data.concat(args);").newLine();
        theWriter.tab(5).text("return aFunction.apply(this,concated);").newLine();
        theWriter.tab(3).text("};").newLine();
        theWriter.tab(3).text("return typeToConstruct.returntype.").text(theMinifier.toSymbol("newLambdaInstance")).text("(handler);").newLine();

        theWriter.tab(2).text("}").newLine();

        theWriter.tab(2).text("var handler").assign().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("var args").assign().text("Array.prototype.slice.call(arguments);").newLine();
        theWriter.tab(3).text("var concated").assign().text("staticArguments.data.splice(1).concat(args);").newLine();
        theWriter.tab(3).text("return aFunction.apply(staticArguments.data[0],concated);").newLine();
        theWriter.tab(2).text("};").newLine();
        theWriter.tab(2).text("return typeToConstruct.returntype.").text(theMinifier.toSymbol("newLambdaInstance")).text("(handler);").newLine();
        theWriter.tab().text("},").newLine();

        theWriter.tab().text("resolveStaticCallSiteObject").colon().text("function(aWhere,aKey,aProducerFunction)").space().text("{").newLine();
        theWriter.tab(2).text("var resolvedCallsiteObject").assign().text("aWhere.").text(theMinifier.toSymbol("__staticCallSites")).text("[aKey];").newLine();
        theWriter.tab(2).text("if").space().text("(resolvedCallsiteObject").space().text("==").space().text("null)").space().text("{").newLine();
        theWriter.tab(3).text("resolvedCallsiteObject").assign().text("aProducerFunction();").newLine();
        theWriter.tab(3).text("aWhere.").text(theMinifier.toSymbol("__staticCallSites")).text("[aKey]").assign().text("resolvedCallsiteObject;").newLine();
        theWriter.tab(2).text("}").newLine();
        theWriter.tab(2).text("return resolvedCallsiteObject;").newLine();
        theWriter.tab().text("},").newLine();

        theWriter.tab().text("imports").colon().text("{").newLine();
        theWriter.tab(2).text("stringutf16").colon().text("{").newLine();
        theWriter.tab(3).text("isBigEndian").colon().text("function()").space().text("{").newLine();
        theWriter.tab(4).text("return 1;").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("},").newLine();

        theWriter.tab(2).text("system").colon().text("{").newLine();
        theWriter.tab(3).text("currentTimeMillis").colon().text("function()").space().text("{").newLine();
        theWriter.tab(4).text("return Date.now();").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("nanoTime").colon().text("function()").space().text("{").newLine();
        theWriter.tab(4).text("return Date.now() * 1000000;").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("writeCharArrayToConsole").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("bytecoder.logCharArrayAsString(p1);").newLine();
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
        theWriter.tab(3).text("logDOUBLE").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.log(p1);").newLine();
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

        theWriter.tab().text("memory").colon().text("[],").newLine();

        theWriter.text("};").newLine();

        final String theGetNameMethodName = theMinifier.toMethodName("getName", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(String.class), new BytecodeTypeRef[0]));

        final ConstantPool thePool = new ConstantPool();

        final BytecodeClassTopologicOrder theOrderedClasses = new BytecodeClassTopologicOrder(aLinkerContext);

        theOrderedClasses.getClassesInOrder().stream().forEach(theEntry -> {

            final BytecodeLinkedClass theLinkedClass = theEntry;
            final BytecodeResolvedMethods theMethods = theLinkedClass.resolvedMethods();

            final String theJSClassName = theMinifier.toClassName(theEntry.getClassName());
            theWriter.text("var ").text(theJSClassName).assign().text("function()").space().text("{").newLine();

            // Constructor function
            theWriter.tab().text("var C").assign().text("function()").space().text("{").newLine();
            theWriter.tab().text("};").newLine();

            if (!theLinkedClass.getClassName().name().equals(Object.class.getName())) {
                theWriter.tab().text("C.prototype").assign().text("Object.create(").text(theMinifier.toClassName(theLinkedClass.getSuperClass().getClassName())).text(".prototype);").newLine();
            } else {
                theWriter.tab().text("C.prototype").assign().text("Object.create(null);").newLine();
            }

            // Framework-Specific methods
            theWriter.tab().text("var ").text(theMinifier.toSymbol("$INITIALIZED")).assign().text("false;").newLine();

            if (!theLinkedClass.getBytecodeClass().getAccessFlags().isInterface()) {
                theWriter.tab().text("var ").text(theMinifier.toSymbol("__implementedTypes")).assign().text("[");
                boolean first = true;
                for (final BytecodeLinkedClass theType : theLinkedClass.getImplementingTypes()) {
                    if (!first) {
                        theWriter.print(",");
                    }
                    first = false;
                    if (theType.getUniqueId() == theLinkedClass.getUniqueId()) {
                        theWriter.print("C");
                    } else {
                        theWriter.print(theMinifier.toClassName(theType.getClassName()));
                    }
                }
                theWriter.text("];").newLine();
            }
            theWriter.tab().text("C.").text(theMinifier.toSymbol("__staticCallSites")).assign().text("[];").newLine();

            // Init function
            theWriter.tab().text("C.").text(theMinifier.toSymbol("init")).assign().text("function()").space().text("{").newLine();
            theWriter.tab(2).text("if").space().text("(!").text(theMinifier.toSymbol("$INITIALIZED")).text(")").space().text("{").newLine();
            theWriter.tab(3).text(theMinifier.toSymbol("$INITIALIZED")).assign().text("true;").newLine();

            // Constructors
            theMethods.stream().forEach(aEntry -> {
                final BytecodeMethod theMethod = aEntry.getValue();
                if (theMethod.isConstructor() && aEntry.getProvidingClass() == theLinkedClass) {

                    final StringBuilder theSignature = new StringBuilder();
                    for (int i=0;i<theMethod.getSignature().getArguments().length;i++) {
                        if (i>0) {
                            theSignature.append(",");
                        }
                        theSignature.append("p");
                        theSignature.append(i);
                    }

                    theWriter.tab(3).text("C.").text(theMinifier.toMethodName("$newInstance", theMethod.getSignature())).assign().text("function(").text(theSignature.toString()).text(")").space().text("{").newLine();
                    theWriter.tab(4).text("var ").text(theMinifier.toSymbol("instance")).assign().text("new C();").newLine();

                    theWriter.tab(4).text(theMinifier.toSymbol("instance")).text(".").text("$").text(Integer.toString(theLinkedClass.getUniqueId())).text(theMinifier.toMethodName(theMethod.getName().stringValue(), theMethod.getSignature()))
                            .text("(").text(theSignature.toString()).text(");").newLine();


                    theWriter.tab(4).text("return ").text(theMinifier.toSymbol("instance")).text(";").newLine();
                    theWriter.tab(3).text("};").newLine();
                }
            });

            // NewInstance function
            theWriter.tab(3).text("C.").text(theMinifier.toSymbol("newInstance")).assign().text("function()").space().text("{").newLine();
            theWriter.tab(4).text("return new C();").newLine();
            theWriter.tab(3).text("};").newLine();

            theWriter.tab(3).text("C.").text(theMinifier.toSymbol("init")).assign().text("function()").space().text("{").newLine();
            theWriter.tab(4).text("return C;").newLine();
            theWriter.tab(3).text("};").newLine();

            if (!theLinkedClass.getClassName().name().equals(Object.class.getName())) {
                final BytecodeLinkedClass theSuper = theLinkedClass.getSuperClass();
                final String theSuperJSName = theMinifier.toClassName(theSuper.getClassName());

                theWriter.tab(3).text(theSuperJSName).text(".").text(theMinifier.toSymbol("init")).text("();").newLine();
            }

            if (theLinkedClass.hasClassInitializer()) {
                theWriter.tab(3).text(theJSClassName).text(".").text(theMinifier.toMethodName("clinit", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[0]))).text("();").newLine();
            }

            theWriter.tab().tab().text("}").newLine();
            theWriter.tab().tab().text("return C;").newLine();
            theWriter.tab().text("};").newLine();

            // NewInstance function
            theWriter.tab().text("C.").text(theMinifier.toSymbol("newInstance")).assign().text("function()").space().text("{").newLine();
            theWriter.tab(2).text("C.").text(theMinifier.toSymbol("init")).text("();").newLine();
            theWriter.tab(2).text("return new C();").newLine();
            theWriter.tab().text("};").newLine();
            theWriter.tab().text("C.prototype.constructor").assign().text("C").space().text(";").newLine();

            // Constructors
            theMethods.stream().forEach(aEntry -> {

                final BytecodeMethod theMethod = aEntry.getValue();

                if (theMethod.isConstructor() && aEntry.getProvidingClass() == theLinkedClass) {

                    final StringBuilder theSignature = new StringBuilder();
                    for (int i=0;i<theMethod.getSignature().getArguments().length;i++) {
                        if (i>0) {
                            theSignature.append(",");
                        }
                        theSignature.append("p");
                        theSignature.append(i);
                    }
                    theWriter.tab().text("C.").text(theMinifier.toMethodName("$newInstance", theMethod.getSignature())).assign().text("function(").text(theSignature.toString()).text(")").space().text("{").newLine();
                    theWriter.tab(2).text("C.").text(theMinifier.toSymbol("init")).text("();").newLine();
                    theWriter.tab(2).text("var ").text(theMinifier.toSymbol("instance")).assign().text("new C();").newLine();

                    theWriter.tab(2).text(theMinifier.toSymbol("instance")).text(".").text("$").text(Integer.toString(theLinkedClass.getUniqueId())).text(theMinifier.toMethodName(theMethod.getName().stringValue(), theMethod.getSignature()))
                            .text("(").text(theSignature.toString()).text(");").newLine();


                    theWriter.tab(2).text("return ").text(theMinifier.toSymbol("instance")).text(";").newLine();
                    theWriter.tab().text("};").newLine();
                }

            });

            // NewLambdaInstance function
            if (theLinkedClass.getBytecodeClass().getAccessFlags().isInterface()) {
                theWriter.tab().text("C.").text(theMinifier.toSymbol("newLambdaInstance")).assign().text("function(impl)").space().text("{").newLine();
                theWriter.tab(2).text("var l").assign().text("C.").text(theMinifier.toSymbol("newInstance")).text("();").newLine();
                for (final BytecodeMethod theMethod : theLinkedClass.getBytecodeClass().getMethods()) {
                    if (!theMethod.isConstructor() && !theMethod.isClassInitializer()) {
                        theWriter.tab(2).text("l.").text(theMinifier.toMethodName(theMethod.getName().stringValue(), theMethod.getSignature())).assign().text("impl.bind(l);").newLine();
                    }
                }
                theWriter.tab(2).text("return l;").newLine();

                theWriter.tab().text("};").newLine();
            }

            // then we add class specific static fields
            final BytecodeResolvedFields theStaticFields = theLinkedClass.resolvedFields();
            theStaticFields.streamForStaticFields().forEach(
                    aFieldEntry -> {
                        if (!"$assertionsDisabled".equals(aFieldEntry.getValue().getName().stringValue())) {
                            final BytecodeTypeRef theFieldType = aFieldEntry.getValue().getTypeRef();
                            if (theFieldType.isPrimitive()) {
                                final BytecodePrimitiveTypeRef thePrimitive = (BytecodePrimitiveTypeRef) theFieldType;
                                if (thePrimitive == BytecodePrimitiveTypeRef.BOOLEAN) {
                                    theWriter.tab().text("C.").symbol(aFieldEntry.getValue().getName().stringValue(), null).assign().text("false;").newLine();
                                } else {
                                    theWriter.tab().text("C.").symbol(aFieldEntry.getValue().getName().stringValue(), null).assign().text("0;").newLine();
                                }
                            } else {
                                theWriter.tab().text("C.").symbol(aFieldEntry.getValue().getName().stringValue(), null).assign().text("null;").newLine();
                            }
                        }
                    });

            if (!theLinkedClass.getBytecodeClass().getAccessFlags().isAbstract()) {
                // The Constructor function initializes all object members with null
                // Only non abstract classes can be instantiated
                final BytecodeResolvedFields theInstanceFields = theLinkedClass.resolvedFields();
                theInstanceFields.streamForInstanceFields().forEach(
                        aFieldEntry -> {
                            final BytecodeTypeRef theFieldType = aFieldEntry.getValue().getTypeRef();
                            if (theFieldType.isPrimitive()) {
                                final BytecodePrimitiveTypeRef thePrimitive = (BytecodePrimitiveTypeRef) theFieldType;
                                if (thePrimitive == BytecodePrimitiveTypeRef.BOOLEAN) {
                                    theWriter.tab().text("C.prototype.").text(theMinifier.toSymbol(aFieldEntry.getValue().getName().stringValue())).assign().text("false;").newLine();
                                } else {
                                    theWriter.tab().text("C.prototype.").text(theMinifier.toSymbol(aFieldEntry.getValue().getName().stringValue())).assign().text("0;").newLine();
                                }
                            } else {
                                theWriter.tab().text("C.prototype.").text(theMinifier.toSymbol(aFieldEntry.getValue().getName().stringValue())).assign().text("null;").newLine();
                            }
                        });
            }

            if (!theLinkedClass.getBytecodeClass().getAccessFlags().isInterface()) {
                theWriter.tab().text("C.prototype.").text("iof").assign().text("function(aType)").space().text("{").newLine();
                theWriter.tab(2).text("return ").text(theMinifier.toSymbol("__implementedTypes")).text(".includes(aType);").newLine();
                theWriter.tab().text("};").newLine();

                theWriter.tab().text("C.").text(theGetNameMethodName).assign().text("function()").space().text("{").newLine();
                if (!theLinkedClass.getClassName().name().equals("java.lang.Class")) {
                    theWriter.tab(2).text("return bytecoder.stringpool[").text("" + thePool.register(new StringValue(ConstantPool.simpleClassName(theLinkedClass.getClassName().name())))).text("];").newLine();
                } else {
                    theWriter.tab(2).text("return this.").text(theGetNameMethodName).text("();").newLine();
                }
                theWriter.tab().text("};").newLine();
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

                        // Static methods will just delegate to the implementation in the class
                        theWriter.tab().text("C.").text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature)).assign()
                            .text(theMinifier.toClassName(aEntry.getProvidingClass().getClassName())).text(".").text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature)).text(";").newLine();
                    }
                    if (aEntry.getProvidingClass().getBytecodeClass().getAccessFlags().isInterface()) {
                        // Default method inherited from interface
                        // Static methods will just delegate to the implementation in the class
                        theWriter.tab().text("C.prototype.").text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature)).assign()
                                .text(theMinifier.toClassName(aEntry.getProvidingClass().getClassName())).text(".prototype.").text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature)).text(";").newLine();
                    }
                    return;
                }

                aLinkerContext.getLogger().info("Compiling {}.{}", theLinkedClass.getClassName().name(), theMethod.getName().stringValue());

                final ProgramGenerator theGenerator = programGeneratorFactory.createFor(aLinkerContext, new JSIntrinsics());
                final Program theSSAProgram = theGenerator.generateFrom(aEntry.getProvidingClass().getBytecodeClass(), theMethod);

                //Run optimizer
                if (!theMethod.getAccessFlags().isAbstract() && !theMethod.getAccessFlags().isNative()) {
                    aOptions.getOptimizer().optimize(theSSAProgram.getControlFlowGraph(), aLinkerContext);
                }

                final StringBuilder theArguments = new StringBuilder();
                boolean first = true;
                for (final Variable theVariable : theSSAProgram.getArguments()) {
                    if (!Variable.THISREF_NAME.equals(theVariable.getName())) {
                        if (!first) {
                            theArguments.append(',');
                        }
                        theArguments.append(theMinifier.toVariableName(theVariable.getName()));
                        first = false;
                    }
                }

                if (theMethod.getAccessFlags().isNative()) {
                    if (null != theLinkedClass.getBytecodeClass().getAttributes()
                            .getAnnotationByType(EmulatedByRuntime.class.getName())) {
                        return;
                    }

                    final BytecodeImportedLink theLink = theLinkedClass.linkfor(theMethod);

                    if (theMethod.getAccessFlags().isStatic()) {
                        theWriter.tab().text("C.");
                    } else {
                        theWriter.tab().text("C.prototype.");
                    }
                    theWriter.text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature))
                            .assign().text("function(").text(theArguments.toString()).text(")").space().text("{").newLine();

                    theWriter.tab(2).text("return bytecoder.imports.").text(theLink.getModuleName()).text(".").text(theLink.getLinkName()).text("(").text(theArguments.toString()).text(");").newLine();
                    theWriter.tab().text("};").newLine();
                    return;
                }

                theWriter.assignSymbolToSourceFile(theLinkedClass.getClassName().name() + "." + theMethod.getName().stringValue(), theSSAProgram.getDebugInformation().debugPositionFor(
                        BytecodeOpcodeAddress.START_AT_ZERO));
                if (theMethod.getAccessFlags().isStatic()) {
                    theWriter.tab().text("C.").text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature))
                            .assign().text("function(").text(theArguments.toString()).text(")").space().text("{").newLine();
                } else if (theMethod.isConstructor()) {
                    theWriter.tab().text("C.prototype.").text("$").text(Integer.toString(theLinkedClass.getUniqueId())).text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature))
                            .assign().text("function(").text(theArguments.toString()).text(")").space().text("{").newLine();
                } else {
                    theWriter.tab().text("C.prototype.").text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature))
                            .assign().text("function(").text(theArguments.toString()).text(")").space().text("{").newLine();
                }

                if (aOptions.isDebugOutput()) {
                    theWriter.tab(2).text("/**").newLine();
                    //theWriter.tab(2).text(theSSAProgram.getControlFlowGraph().toDOT()).newLine();
                    theWriter.tab(2).text("**/").newLine();
                }

                theWriter.flush();

                // Perform register allocation
                final AbstractAllocator theAllocator = aOptions.getAllocator().allocate(theSSAProgram, t -> t.resolveType(), aLinkerContext);

                final JSSSAWriter theVariablesWriter = new JSSSAWriter(aOptions, theSSAProgram, 2, theWriter, aLinkerContext, thePool, false, theMinifier, theAllocator);
                theVariablesWriter.printRegisterDeclarations();

                // Try to reloop it or stackify it!
                try {
                    if (aOptions.isPreferStackifier()) {
                        try {
                            final Stackifier stackifier = new Stackifier(theSSAProgram.getControlFlowGraph());
                            theVariablesWriter.printStackified(stackifier);

                            aOptions.getLogger().debug("Method {}.{} successfully stackified ", theLinkedClass.getClassName().name(), theMethod.getName().stringValue());

                        } catch (final HeadToHeadControlFlowException e) {

                            // Stackifier has problems, we fallback to relooper instead
                            aOptions.getLogger().warn("Method {}.{} could not be stackified, using Relooper instead", theLinkedClass.getClassName().name(), theMethod.getName().stringValue());

                            final Relooper theRelooper = new Relooper(aOptions);
                            final Relooper.Block theReloopedBlock = theRelooper.reloop(theSSAProgram.getControlFlowGraph());

                            theVariablesWriter.printRelooped(theReloopedBlock);
                        }
                    } else {

                        final Relooper theRelooper = new Relooper(aOptions);
                        final Relooper.Block theReloopedBlock = theRelooper.reloop(theSSAProgram.getControlFlowGraph());

                        theVariablesWriter.printRelooped(theReloopedBlock);
                    }
                } catch (final Exception e) {
                    throw new IllegalStateException("General error while processing " + theLinkedClass.getClassName().name() + '.'
                            + theMethod.getName().stringValue(), e);
                }

                theWriter.tab().text("};").newLine();

                if (theMethod.isConstructor() || theMethod.getAccessFlags().isStatic()) {
                    if (theMethod.getAccessFlags().isStatic()) {
                        theWriter.tab().text("C.").text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature)).text(".static").assign().text("true;").newLine();
                    }
                } else {
                    theWriter.tab().text("C.").text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature))
                        .assign().text("C.prototype.").text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature)).text(";").newLine();
                }

            });

            theWriter.tab().text("return C;").newLine();

            theWriter.text("}();").newLine();
        });

        theWriter.text("bytecoder.bootstrap").assign().text("function()").space().text("{").newLine();
        final List<StringValue> theValues = thePool.stringValues();
        for (int i=0; i<theValues.size(); i++) {
            final StringValue theValue = theValues.get(i);
            theWriter.tab().text("bytecoder.stringpool[").text("" + i).text("]").assign().text("bytecoder.newString(").text(toArray(theValue.getStringValue())).text(");").newLine();
        }

        theOrderedClasses.getClassesInOrder().forEach(aEntry -> {
            final BytecodeResolvedMethods theMethods = aEntry.resolvedMethods();
            theMethods.stream().forEach(eMethod -> {
                final BytecodeMethod theMethod = eMethod.getValue();
                final BytecodeMethodSignature theCurrentMethodSignature = theMethod.getSignature();

                // If the method is provided by the runtime, we do not need to generate the implementation
                final BytecodeAnnotation theAnnotation = theMethod.getAttributes().getAnnotationByType(Export.class.getName());
                if (theAnnotation != null) {
                    theWriter.tab().text("bytecoder.exports.").text(theAnnotation.getElementValueByName("value").stringValue()).assign().text(theMinifier.toClassName(aEntry.getClassName())).text(".").text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature)).text(";").newLine();
                } else {
                    // If the method is the main method and not exported by annotation, it is
                    // exported by default name "main".
                    if (aEntry.getClassName().name().equals(aEntryPointClass.getName()) && theMethod.getName().stringValue().equals(aEntryPointMethodName)) {
                        theWriter.tab().text("bytecoder.exports.main").assign().text(theMinifier.toClassName(aEntry.getClassName())).text(".").text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature)).text(";").newLine();
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

    public String toArray(final String aData) {
        final StringBuilder theResult = new StringBuilder("[");
        for (int i=0;i<aData.length();i++) {
            if (i>0) {
                theResult.append(",");
            }
            theResult.append((int) aData.charAt(i));
        }
        theResult.append("]");
        return theResult.toString();
    }
}