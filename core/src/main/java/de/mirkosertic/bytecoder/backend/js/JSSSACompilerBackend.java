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
import de.mirkosertic.bytecoder.backend.CompileResult;
import de.mirkosertic.bytecoder.backend.ConstantPool;
import de.mirkosertic.bytecoder.backend.SourceMapWriter;
import de.mirkosertic.bytecoder.classlib.Array;
import de.mirkosertic.bytecoder.classlib.ExceptionManager;
import de.mirkosertic.bytecoder.classlib.VM;
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
import de.mirkosertic.bytecoder.ssa.MethodHandleExpression;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.ssa.ProgramGeneratorFactory;
import de.mirkosertic.bytecoder.ssa.StringValue;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.stackifier.HeadToHeadControlFlowException;
import de.mirkosertic.bytecoder.stackifier.Stackifier;

import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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

        final BytecodeLinkedClass theVMClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(VM.class));
        theVMClass.resolveStaticMethod("newStringUTF8", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                String.class), new BytecodeTypeRef[] {new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.BYTE, 1)}));
        theVMClass.resolveStaticMethod("newByteArray", new BytecodeMethodSignature(new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.BYTE, 1), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT}));
        theVMClass.resolveStaticMethod("setByteArrayEntry", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.BYTE, 1), BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.BYTE}));

        // We need this class and constructor to build reflective metadata
        final BytecodeMethodSignature theFieldClassConstructorSignature = new BytecodeMethodSignature(
                BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {
                BytecodeObjectTypeRef.fromRuntimeClass(Class.class),
                BytecodeObjectTypeRef.fromRuntimeClass(String.class),
                BytecodePrimitiveTypeRef.INT,
                BytecodeObjectTypeRef.fromRuntimeClass(Class.class),
                BytecodeObjectTypeRef.fromRuntimeClass(Object.class),
                BytecodeObjectTypeRef.fromRuntimeClass(Object.class),
                BytecodePrimitiveTypeRef.INT
        });
        final BytecodeLinkedClass theFieldClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Field.class));
        theFieldClass.tagWith(BytecodeLinkedClass.Tag.INSTANTIATED);
        theFieldClass.resolveConstructorInvocation(theFieldClassConstructorSignature);

        // We need this package-private constructor in String.class for bootstrap
        aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(String.class))
                .resolveConstructorInvocation(new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID,
                        new BytecodeTypeRef[] {new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.BYTE, 1),BytecodePrimitiveTypeRef.BYTE}));

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

        theWriter.tab().text("newString").colon().text("function(aByteArray)").space().text("{").newLine();
        theWriter.tab(2).text("var theBytes").assign().text("bytecoder.exports.newByteArray(aByteArray.length);").newLine();
        theWriter.tab(2).text("for").space().text("(var i=0;i<aByteArray.length;i++)").space().text("{").newLine();
        theWriter.tab(3).text("bytecoder.exports.setByteArrayEntry(theBytes,i,aByteArray[i]);").newLine();
        theWriter.tab(2).text("}").newLine();
        theWriter.tab(2).text("return bytecoder.exports.newStringUTF8(theBytes);").newLine();
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
        theWriter.tab(2).text("var theArray").assign().text("aBytecoderString.").text(theMinifier.toSymbol("value")).text(".data").text(";").newLine();
        theWriter.tab(2).text("var theResult = '';").newLine();
        theWriter.tab(2).text("for").space().text("(var i=0;i<theArray.length;i++)").space().text("{").newLine();
        theWriter.tab(3).text("theResult+=String.fromCharCode(theArray[i]);").newLine();
        theWriter.tab(2).text("}").newLine();
        theWriter.tab(2).text("return theResult;").newLine();
        theWriter.tab().text("},").newLine();

        theWriter.tab().text("methodType").colon().text("function(ret,args)").space().text("{").newLine();
        theWriter.tab(2).text("return {returntype: ret, arguments:args};").newLine();
        theWriter.tab().text("},").newLine();

        theWriter.tab().text("lambdaStaticRef").colon().text("function(aFunction,staticArguments,name,typeToConstruct)").space().text("{").newLine();
        theWriter.tab(2).text("var handler").assign().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("var args").assign().text("Array.prototype.slice.call(arguments);").newLine();
        theWriter.tab(3).text("var concated").assign().text("staticArguments.data.concat(args);").newLine();
        theWriter.tab(3).text("return aFunction.apply(this,concated);").newLine();
        theWriter.tab(2).text("};").newLine();
        theWriter.tab(2).text("return typeToConstruct.returntype.").text(theMinifier.toSymbol("newLambdaInstance")).text("(handler);").newLine();
       theWriter.tab().text("},").newLine();

        theWriter.tab().text("lambdaConstructorRef").colon().text("function(typeToConstruct,constructorRef,staticArguments)").space().text("{").newLine();
        theWriter.tab(2).text("var selfRef").assign().text("staticArguments.data[0];").newLine();
        theWriter.tab(2).text("var staticArgs").assign().text("staticArguments.data.splice(1);").newLine();
        theWriter.tab(2).text("var handler").assign().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("var args").assign().text("Array.prototype.slice.call(arguments);").newLine();
        theWriter.tab(3).text("var concated").assign().text("staticArgs.concat(args);").newLine();
        theWriter.tab(3).text("return constructorRef.apply(selfRef,concated);").newLine();
        theWriter.tab(2).text("};").newLine();
        theWriter.tab(2).text("return typeToConstruct.returntype.").text(theMinifier.toSymbol("newLambdaInstance")).text("(handler);").newLine();
        theWriter.tab().text("},").newLine();

        theWriter.tab().text("lambdaInterfaceRef").colon().text("function(typeToConstruct,methodRef,staticArguments)").space().text("{").newLine();
        theWriter.tab(2).text("var handler").assign().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("var args").assign().text("Array.prototype.slice.call(arguments);").newLine();
        theWriter.tab(3).text("var concated").assign().text("staticArguments.data.concat(args);").newLine();
        theWriter.tab(3).text("return methodRef.apply(concated[0],concated.splice(1));").newLine();
        theWriter.tab(2).text("};").newLine();
        theWriter.tab(2).text("return typeToConstruct.returntype.").text(theMinifier.toSymbol("newLambdaInstance")).text("(handler);").newLine();
        theWriter.tab().text("},").newLine();

        theWriter.tab().text("lambdaVirtualRef").colon().text("function(typeToConstruct,methodRef,staticArguments)").space().text("{").newLine();
        theWriter.tab(2).text("var selfRef").assign().text("staticArguments.data[0];").newLine();
        theWriter.tab(2).text("var staticArgs").assign().text("staticArguments.data.splice(1);").newLine();
        theWriter.tab(2).text("var handler").assign().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("var args").assign().text("Array.prototype.slice.call(arguments);").newLine();
        theWriter.tab(3).text("var concated").assign().text("staticArgs.concat(args);").newLine();
        theWriter.tab(3).text("return methodRef.apply(selfRef,concated);").newLine();
        theWriter.tab(2).text("};").newLine();
        theWriter.tab(2).text("return typeToConstruct.returntype.").text(theMinifier.toSymbol("newLambdaInstance")).text("(handler);").newLine();
        theWriter.tab().text("},").newLine();

        theWriter.tab().text("lambdaSpecialRef").colon().text("function(typeToConstruct,methodRef,staticArguments)").space().text("{").newLine();
        theWriter.tab(2).text("var selfRef").assign().text("staticArguments.data[0];").newLine();
        theWriter.tab(2).text("var staticArgs").assign().text("staticArguments.data.splice(1);").newLine();
        theWriter.tab(2).text("var handler").assign().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("var args").assign().text("Array.prototype.slice.call(arguments);").newLine();
        theWriter.tab(3).text("var concated").assign().text("staticArgs.concat(args);").newLine();
        theWriter.tab(3).text("return methodRef.apply(selfRef,concated);").newLine();
        theWriter.tab(2).text("};").newLine();
        theWriter.tab(2).text("return typeToConstruct.returntype.").text(theMinifier.toSymbol("newLambdaInstance")).text("(handler);").newLine();
        theWriter.tab().text("},").newLine();

        theWriter.tab().text("resolveStaticCallSiteObject").colon().text("function(aWhere,aKey,aProducerFunction)").space().text("{").newLine();
        theWriter.tab(2).text("var resolvedCallsiteObject").assign().text("aWhere.").text(theMinifier.toSymbol("__runtimeclass")).text(".").text("staticCallSites").text("[aKey];").newLine();
        theWriter.tab(2).text("if").space().text("(resolvedCallsiteObject").space().text("==").space().text("null)").space().text("{").newLine();
        theWriter.tab(3).text("resolvedCallsiteObject").assign().text("aProducerFunction();").newLine();
        theWriter.tab(3).text("aWhere.").text(theMinifier.toSymbol("__runtimeclass")).text(".").text("staticCallSites").text("[aKey]").assign().text("resolvedCallsiteObject;").newLine();
        theWriter.tab(2).text("}").newLine();
        theWriter.tab(2).text("return resolvedCallsiteObject;").newLine();
        theWriter.tab().text("},").newLine();

        theWriter.tab().text("imports").colon().text("{").newLine();

        theWriter.tab(2).text("stringutf16").colon().text("{").newLine();
        theWriter.tab(3).text("isBigEndian").colon().text("function()").space().text("{").newLine();
        theWriter.tab(4).text("return 1;").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("toolkit").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("component").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("awtevent").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("inputevent").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("dimension").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("trayicon").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("raster").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("region").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("samplemodel").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("inflater").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("graphicsprimitivemgr").colon().text("{").newLine();
        theWriter.tab(3).text("initIDsClassClassClassClassClassClassClassClassClassClassClass").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("registerNativeLoops").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("filekey").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("ioutil").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("cursor").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("event").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("font").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("fontmetrics").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("insets").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("keyboardfocusmanager").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("menucomponent").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("colormodel").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("singlepixelpackedsamplemodel").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("gifimagedecoder").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("imagerepresentation").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("jpegimagedecoder").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("disposer").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("surfacedata").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("shapespaniterator").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("spancliprenderer").colon().text("{").newLine();
        theWriter.tab(3).text("initIDsClassClass").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("button").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("choice").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("color").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("container").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("menubar").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("menuitem").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("bufferedimage").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("indexcolormodel").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("randomaccessfile").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("bufimgsurfacedata").colon().text("{").newLine();
        theWriter.tab(3).text("initIDsClassClass").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("initRasterObjectINTINTINTINTINTINTIndexColorModel").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("freetypefontscaler").colon().text("{").newLine();
        theWriter.tab(3).text("initIDsClass").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("sunfontmanager").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("checkboxmenuitem").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("menu").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("rectangle").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("window").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("bytecomponentraster").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("bytepackedraster").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("integercomponentraster").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("shortcomponentraster").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("dialog").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("frame").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("keyevent").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("mouseevent").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("filechannelimpl").colon().text("{").newLine();
        theWriter.tab(3).text("initIDs").colon().text("function()").space().text("{").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("system").colon().text("{").newLine();
        theWriter.tab(3).text("currentTimeMillis").colon().text("function()").space().text("{").newLine();
        theWriter.tab(4).text("return Date.now();").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("nanoTime").colon().text("function()").space().text("{").newLine();
        theWriter.tab(4).text("return Date.now() * 1000000;").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("cds").colon().text("{").newLine();
        theWriter.tab(3).text("isDumpingClassList0").colon().text("function()").space().text("{").newLine();
        theWriter.tab(4).text("return false;").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("isDumpingArchive0").colon().text("function()").space().text("{").newLine();
        theWriter.tab(4).text("return false;").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("isSharingEnabled0").colon().text("function()").space().text("{").newLine();
        theWriter.tab(4).text("return false;").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("initializeFromArchiveClass").colon().text("function()").space().text("{").newLine();
        theWriter.tab(4).text("").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("getRandomSeedForDumping").colon().text("function()").space().text("{").newLine();
        theWriter.tab(4).text("return 0;").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("scopedmemoryaccess").colon().text("{").newLine();
        theWriter.tab(3).text("registerNatives").colon().text("function()").space().text("{").newLine();
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
        theWriter.tab(3).text("createInt16ArrayINT").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("return new Int16Array(p1);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("float").colon().text("{").newLine();
        theWriter.tab(3).text("floatToRawIntBitsFLOAT").colon().text("function(value)").space().text("{").newLine();
        theWriter.tab(4).text("var fl = new Float32Array(1);").newLine();
        theWriter.tab(4).text("fl[0] = value;").newLine();
        theWriter.tab(4).text("var br = new Int32Array(fl.buffer);").newLine();
        theWriter.tab(4).text("return br[0];").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("intBitsToFloatINT").colon().text("function(value)").space().text("{").newLine();
        theWriter.tab(4).text("var fl = new Int32Array(1);").newLine();
        theWriter.tab(4).text("fl[0] = value;").newLine();
        theWriter.tab(4).text("var br = new Float32Array(fl.buffer);").newLine();
        theWriter.tab(4).text("return br[0];").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("double").colon().text("{").newLine();
        theWriter.tab(3).text("doubleToRawLongBitsDOUBLE").colon().text("function(value)").space().text("{").newLine();
        theWriter.tab(4).text("var fl = new Float64Array(1);").newLine();
        theWriter.tab(4).text("fl[0] = value;").newLine();
        theWriter.tab(4).text("var br = new BigInt64Array(fl.buffer);").newLine();
        theWriter.tab(4).text("return br[0];").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("longBitsToDoubleINT").colon().text("function(value)").space().text("{").newLine();
        theWriter.tab(4).text("var fl = new BigInt64Array(1);").newLine();
        theWriter.tab(4).text("fl[0] = value;").newLine();
        theWriter.tab(4).text("var br = new Float64Array(fl.buffer);").newLine();
        theWriter.tab(4).text("return br[0];").newLine();
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
        theWriter.tab(3).text("minLONGLONG").colon().text("function(p1,p2)").space().text("{").newLine();
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
        theWriter.tab(3).text("powDOUBLEDOUBLE").colon().text("function(p1,p2)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.pow(p1,p2);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("acosDOUBLE").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.acos(p1);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("atan2DOUBLE").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.atan2(p1);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("cbrtDOUBLE").colon().text("function(p1)").space().text("{").newLine();
        theWriter.tab(4).text("return Math.cbrt(p1);").newLine();
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

        theWriter.tab(2).text("unixfilesystem").space().text(":").space().text("{").newLine();
        theWriter.tab(3).text("getBooleanAttributes0String").colon().text("function(path)").space().text("{").newLine();

        theWriter.tab(4).text("var jsPath").assign().text("bytecoder.toJSString(path);").newLine();
        theWriter.tab(4).text("try").space().text("{").newLine();
        theWriter.tab(5).text("var request").assign().text("new XMLHttpRequest();").newLine();
        theWriter.tab(5).text("request.open('HEAD',jsPath,false);").newLine();
        theWriter.tab(5).text("request.send(null);").newLine();
        theWriter.tab(5).text("if").space().text("(request.status").space().text("==").space().text("200)").space().text("{").newLine();
        theWriter.tab(6).text("var length").assign().text("request.getResponseHeader('content-length');").newLine();
        theWriter.tab(6).text("return 0x01;").newLine();
        theWriter.tab(5).text("}").newLine();
        theWriter.tab(5).text("return 0;").newLine();
        theWriter.tab(4).text("}").space().text("catch(e)").space().text("{").newLine();
        theWriter.tab(5).text("return 0;").newLine();
        theWriter.tab(4).text("}").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("nullpointerexception").space().text(":").space().text("{").newLine();
        theWriter.tab(3).text("getExtendedNPEMessage").colon().text("function(handle, data, offset, length)").space().text("{").newLine();
        theWriter.tab(4).text("return null;").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("fileoutputstream").space().text(":").space().text("{").newLine();
        theWriter.tab(3).text("writeBytesINTL1BYTEINTINT").colon().text("function(handle, data, offset, length)").space().text("{").newLine();
        theWriter.tab(4).text("bytecoder.filehandles[handle].writeBytesINTL1BYTEINTINT(handle,data,offset,length);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("writeIntINTINT").colon().text("function(handle, value)").space().text("{").newLine();
        theWriter.tab(4).text("bytecoder.filehandles[handle].writeIntINTINT(handle,value);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("close0INT").colon().text("function(handle)").space().text("{").newLine();
        theWriter.tab(4).text("bytecoder.filehandles[handle].close0INT(handle);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(2).text("fileinputstream").space().text(":").space().text("{").newLine();
        theWriter.tab(3).text("open0String").colon().text("function(name)").space().text("{").newLine();
        theWriter.tab(4).text("return bytecoder.openForRead(bytecoder.toJSString(name));").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("read0INT").colon().text("function(handle)").space().text("{").newLine();
        theWriter.tab(4).text("return bytecoder.filehandles[handle].read0INT(handle);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("readBytesINTL1BYTEINTINT").colon().text("function(handle,data,offset,length)").space().text("{").newLine();
        theWriter.tab(4).text("return bytecoder.filehandles[handle].readBytesINTL1BYTEINTINT(handle,data,offset,length);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("close0INT").colon().text("function(handle)").space().text("{").newLine();
        theWriter.tab(4).text("bytecoder.filehandles[handle].close0INT(handle);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("skip0INTINT").colon().text("function(handle,amount)").space().text("{").newLine();
        theWriter.tab(4).text("return bytecoder.filehandles[handle].skip0INTINT(handle,amount);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("available0INT").colon().text("function(handle)").space().text("{").newLine();
        theWriter.tab(4).text("return bytecoder.filehandles[handle].available0INT(handle);").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(2).text("},").newLine();

        theWriter.tab(1).text("},").newLine();

        theWriter.tab().text("filehandles").colon().text("[],").newLine();

        theWriter.tab().text("exports").colon().text("{},").newLine();

        theWriter.tab().text("stringpool").colon().text("[],").newLine();

        theWriter.tab().text("methodhandles").colon().text("[],").newLine();

        theWriter.tab().text("memory").colon().text("[],").newLine();
        theWriter.tab().text("openForRead").colon().space().text("function(path)").space().text("{").newLine();
        theWriter.tab(2).text("try").space().text("{").newLine();
        theWriter.tab(3).text("var request").assign().text("new XMLHttpRequest();").newLine();
        theWriter.tab(3).text("request.open('GET',path,false);").newLine();
        theWriter.tab(3).text("request.overrideMimeType('text\\/plain; charset=x-user-defined');").newLine();
        theWriter.tab(3).text("request.send(null);").newLine();
        theWriter.tab(3).text("if").space().text("(request.status==200)").space().text("{").newLine();
        theWriter.tab(4).text("var length").assign().text("request.getResponseHeader('content-length');").newLine();
        theWriter.tab(4).text("var responsetext").assign().text("request.response;").newLine();
        theWriter.tab(4).text("var buf = new ArrayBuffer(responsetext.length);").newLine();
        theWriter.tab(4).text("var bufView = new Uint8Array(buf);").newLine();
        theWriter.tab(4).text("for (var i=0, strLen=responsetext.length; i<strLen; i++) {").newLine();
        theWriter.tab(5).text("bufView[i] = responsetext.charCodeAt(i) & 0xff;").newLine();
        theWriter.tab(4).text("}").newLine();
        theWriter.tab(4).text("var handle = bytecoder.filehandles.length;").newLine();
        theWriter.tab(4).text("bytecoder.filehandles[handle] = {").newLine();
        theWriter.tab(5).text("currentpos: 0,").newLine();
        theWriter.tab(5).text("data: bufView,").newLine();
        theWriter.tab(5).text("size: length,").newLine();
        theWriter.tab(5).text("skip0INTINT: function(handle,amount) {").newLine();
        theWriter.tab(6).text("var remaining = this.size - this.currentpos;").newLine();
        theWriter.tab(6).text("var possible = Math.min(remaining, amount);").newLine();
        theWriter.tab(6).text("this.currentpos+=possible;").newLine();
        theWriter.tab(6).text("return possible;").newLine();
        theWriter.tab(5).text("},").newLine();
        theWriter.tab(5).text("available0INT: function(handle) {").newLine();
        theWriter.tab(6).text("return this.size - this.currentpos;").newLine();
        theWriter.tab(5).text("},").newLine();
        theWriter.tab(5).text("read0INT: function(handle) {").newLine();
        theWriter.tab(6).text("return this.data[this.currentpos++];").newLine();
        theWriter.tab(5).text("},").newLine();
        theWriter.tab(5).text("readBytesINTL1BYTEINTINT: function(handle,target,offset,length) {").newLine();
        theWriter.tab(6).text("if (length === 0) {return 0;}").newLine();
        theWriter.tab(6).text("var remaining = this.size - this.currentpos;").newLine();
        theWriter.tab(6).text("var possible = Math.min(remaining, length);").newLine();
        theWriter.tab(6).text("if (possible === 0) {return -1;}").newLine();
        theWriter.tab(6).text("for (var j=0;j<possible;j++) {").newLine();
        theWriter.tab(7).text("target.data[offset++]=this.data[this.currentpos++];").newLine();
        theWriter.tab(6).text("}").newLine();
        theWriter.tab(6).text("return possible;").newLine();
        theWriter.tab(5).text("}").newLine();
        theWriter.tab(4).text("};").newLine();
        theWriter.tab(4).text("return handle;").newLine();
        theWriter.tab(3).text("}").newLine();
        theWriter.tab(3).text("return -1;").newLine();
        theWriter.tab(2).text("} catch(e) {").newLine();
        theWriter.tab(3).text("return -1;").newLine();
        theWriter.tab(2).text("}").newLine();
        theWriter.tab().text("},").newLine();

        theWriter.tab().text("initializeFileIO: function() {").newLine();
        theWriter.tab(2).text("var stddin = {").newLine();
        theWriter.tab(2).text("};").newLine();
        theWriter.tab(2).text("var stdout = {").newLine();
        theWriter.tab(3).text("buffer: \"\",").newLine();
        theWriter.tab(3).text("writeBytesINTL1BYTEINTINT: function(handle, data, offset, length) {").newLine();
        theWriter.tab(4).text("if (length > 0) {").newLine();
        theWriter.tab(5).text("var array = new Uint8Array(length);").newLine();
        theWriter.tab(5).text("for (var i = 0; i < length; i++) {").newLine();
        theWriter.tab(6).text("array[i] = data.data[i];").newLine();
        theWriter.tab(5).text("}").newLine();
        theWriter.tab(5).text("var asstring = String.fromCharCode.apply(null, array);").newLine();
        theWriter.tab(5).text("for (var i=0;i<asstring.length;i++) {").newLine();
        theWriter.tab(6).text("var c = asstring.charAt(i);").newLine();
        theWriter.tab(6).text("if (c == '\\n') {").newLine();
        theWriter.tab(7).text("console.log(stdout.buffer);").newLine();
        theWriter.tab(7).text("stdout.buffer=\"\";").newLine();
        theWriter.tab(6).text("} else {").newLine();
        theWriter.tab(7).text("stdout.buffer = stdout.buffer.concat(c);").newLine();
        theWriter.tab(6).text("}").newLine();
        theWriter.tab(5).text("}").newLine();
        theWriter.tab(4).text("}").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("close0INT: function(handle) {").newLine();
        theWriter.tab(3).text("},").newLine();
        theWriter.tab(3).text("writeIntINTINT: function(handle,value) {").newLine();
        theWriter.tab(4).text("var c = String.fromCharCode(value);").newLine();
        theWriter.tab(4).text("if (c == '\\n') {").newLine();
        theWriter.tab(5).text("console.log(stdout.buffer);").newLine();
        theWriter.tab(5).text("stdout.buffer=\"\";").newLine();
        theWriter.tab(4).text("} else {").newLine();
        theWriter.tab(5).text("stdout.buffer = stdout.buffer.concat(c);").newLine();
        theWriter.tab(4).text("}").newLine();
        theWriter.tab(3).text("}").newLine();
        theWriter.tab(2).text("};").newLine();
        theWriter.tab(2).text("bytecoder.filehandles[0] = stddin;").newLine();
        theWriter.tab(2).text("bytecoder.filehandles[1] = stdout;").newLine();
        theWriter.tab(2).text("bytecoder.filehandles[2] = stdout;").newLine();
        theWriter.tab(2).text("bytecoder.exports.initDefaultFileHandles(0,1,2);").newLine();
        theWriter.tab().text("},").newLine();

        theWriter.text("};").newLine();

        final String theGetNameMethodName = theMinifier.toMethodName("getName", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(String.class), new BytecodeTypeRef[0]));
        final String theGetClassLoaderMethodName = theMinifier.toMethodName("getClassLoader", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(ClassLoader.class), new BytecodeTypeRef[0]));
        final String theGetClassLoaderMethodName0 = theMinifier.toMethodName("getClassLoader0", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(ClassLoader.class), new BytecodeTypeRef[0]));
        final String theHashCodeMethodName = theMinifier.toMethodName("hashCode", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[0]));
        final String theEqualsMethodName = theMinifier.toMethodName("equals", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.BOOLEAN, new BytecodeTypeRef[] {BytecodeObjectTypeRef.fromRuntimeClass(Object.class)}));

        final String theIsAssignableFromMethodName = theMinifier.toMethodName("isAssignableFrom", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.BOOLEAN, new BytecodeTypeRef[] {BytecodeObjectTypeRef.fromRuntimeClass(Class.class)}));
        final String theGetConstructorMethodName = theMinifier.toMethodName("getConstructor", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                Constructor.class), new BytecodeTypeRef[] {new BytecodeArrayTypeRef(BytecodeObjectTypeRef.fromRuntimeClass(Class.class), 1)}));
        final String theGetSimpleNameMethodName = theMinifier.toMethodName("getSimpleName", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(String.class), new BytecodeTypeRef[0]));
        final String theGetFieldNameMethodName = theMinifier.toMethodName("getField", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Field.class), new BytecodeTypeRef[] {BytecodeObjectTypeRef.fromRuntimeClass(String.class)}));
        final String theGetDeclaredFieldsNameMethodName = theMinifier.toMethodName("getDeclaredFields", new BytecodeMethodSignature(new BytecodeArrayTypeRef(BytecodeObjectTypeRef.fromRuntimeClass(Field.class), 1), new BytecodeTypeRef[] {}));
        final String theGetDeclaredFieldNameMethodName = theMinifier.toMethodName("getDeclaredField", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Field.class), new BytecodeTypeRef[] {BytecodeObjectTypeRef.fromRuntimeClass(String.class)}));
        final String theIsPrimitiveMethodName = theMinifier.toMethodName("isPrimitive", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.BOOLEAN, new BytecodeTypeRef[] {}));
        final String theIsInstanceMethodName = theMinifier.toMethodName("isInstance", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.BOOLEAN, new BytecodeTypeRef[] {BytecodeObjectTypeRef.fromRuntimeClass(Object.class)}));

        // We need the runtimeclass logic
        theWriter.text("var ").text(theMinifier.toSymbol("RuntimeClass")).assign().text("function()").space().text("{").newLine();
        theWriter.tab(1).text("var C").assign().text("function(classNameIndex,superClass,implementedTypes,primitive)").space().text("{").newLine();
        theWriter.tab(2).text("this.classNameIndex").assign().text("classNameIndex;").newLine();
        theWriter.tab(2).text("this.superClass").assign().text("superClass;").newLine();
        theWriter.tab(2).text("this.implementedTypes").assign().text("implementedTypes;").newLine();
        theWriter.tab(2).text("this.staticCallSites").assign().text("[];").newLine();
        theWriter.tab(2).text("this.declaredFields").assign().text("undefined;").newLine();
        theWriter.tab(2).text("this.declaredFields").assign().text("undefined;").newLine();
        theWriter.tab(2).text("this.primitive").assign().text("primitive;").newLine();
        theWriter.tab(1).text("};").newLine();
        theWriter.tab(1).text("C.").text(theMinifier.toSymbol("__runtimeclass")).assign().text("new C(-1,null,[],false);").newLine();
        theWriter.tab(1).text("C.prototype.").text(theGetNameMethodName).assign().text("function()").space().text("{").newLine();
        theWriter.tab(2).text("return bytecoder.stringpool[this.classNameIndex];").newLine();
        theWriter.tab(1).text("};").newLine();

        theWriter.tab(1).text("C.prototype.").text(theHashCodeMethodName).assign().text("function()").space().text("{").newLine();
        theWriter.tab(2).text("return 42;").newLine();
        theWriter.tab(1).text("};").newLine();

        theWriter.tab().text("C.prototype.").text(theEqualsMethodName).assign().text("function(args)").space().text("{").newLine();
        theWriter.tab(2).text("return ").text(theMinifier.toClassName(BytecodeObjectTypeRef.fromRuntimeClass(Class.class))).text(".prototype.").text(theEqualsMethodName).text(".call(this, args);").newLine();
        theWriter.tab().text("};").newLine();

        theWriter.tab(1).text("C.prototype.").text(theIsAssignableFromMethodName).assign().text("function(aOtherType)").space().text("{").newLine();
        theWriter.tab(2).text("for (var i=0;i<aOtherType.implementedTypes.length;i++) {").newLine();
        theWriter.tab(3).text("if (aOtherType.implementedTypes[i].").text(theMinifier.toSymbol("__runtimeclass")).text(" === this) {").newLine();
        theWriter.tab(4).text("return true;").newLine();
        theWriter.tab(3).text("}").newLine();
        theWriter.tab(2).text("}").newLine();
        theWriter.tab(2).text("return false;").newLine();
        theWriter.tab(1).text("};").newLine();

        theWriter.tab(1).text("C.prototype.").text(theIsInstanceMethodName).assign().text("function(aInstance)").space().text("{").newLine();
        theWriter.tab(2).text("if (aInstance) {").newLine();
        theWriter.tab(3).text("return aInstance.constructor.").text(theMinifier.toSymbol("__runtimeclass")).text(".isInstance(this);").newLine();
        theWriter.tab(2).text("}").newLine();
        theWriter.tab(2).text("return false;").newLine();
        theWriter.tab(1).text("};").newLine();

        theWriter.tab().text("C.prototype.").text("iof").assign().text("function(aType)").space().text("{").newLine();
        theWriter.tab(2).text("if (aType === ").text(theMinifier.toSymbol("jlClass")).text(") return true;").newLine();
        theWriter.tab(2).text("for (var i=0;i<this.implementedTypes.length;i++) {").newLine();
        theWriter.tab(3).text("if (this.implementedTypes[i].").text(theMinifier.toSymbol("__runtimeclass")).text("===aType.").text(theMinifier.toSymbol("__runtimeclass")).text(") {").newLine();
        theWriter.tab(4).text("return true;").newLine();
        theWriter.tab(3).text("}").newLine();
        theWriter.tab(2).text("}").newLine();
        theWriter.tab(2).text("return false;").newLine();
        theWriter.tab().text("};").newLine();

        theWriter.tab().text("C.prototype.").text("isInstance").assign().text("function(aType)").space().text("{").newLine();
        theWriter.tab(2).text("for (var i=0;i<this.implementedTypes.length;i++) {").newLine();
        theWriter.tab(3).text("if (this.implementedTypes[i].").text(theMinifier.toSymbol("__runtimeclass")).text("===aType").text(") {").newLine();
        theWriter.tab(4).text("return true;").newLine();
        theWriter.tab(3).text("}").newLine();
        theWriter.tab(2).text("}").newLine();
        theWriter.tab(2).text("return false;").newLine();
        theWriter.tab().text("};").newLine();

        theWriter.tab().text("C.prototype.").text(theGetClassLoaderMethodName).assign().text("function()").space().text("{").newLine();
        theWriter.tab(2).text("return null;").newLine();
        theWriter.tab().text("};").newLine();

        theWriter.tab().text("C.prototype.").text(theGetClassLoaderMethodName0).assign().text("function()").space().text("{").newLine();
        theWriter.tab(2).text("return null;").newLine();
        theWriter.tab().text("};").newLine();

        theWriter.tab().text("C.prototype.").text(theGetConstructorMethodName).assign().text("function(args)").space().text("{").newLine();
        theWriter.tab(2).text("return ").text(theMinifier.toClassName(BytecodeObjectTypeRef.fromRuntimeClass(Class.class))).text(".prototype.").text(theGetConstructorMethodName).text(".call(this, args);").newLine();
        theWriter.tab().text("};").newLine();

        theWriter.tab().text("C.prototype.").text(theGetSimpleNameMethodName).assign().text("function(args)").space().text("{").newLine();
        theWriter.tab(2).text("return ").text(theMinifier.toClassName(BytecodeObjectTypeRef.fromRuntimeClass(Class.class))).text(".prototype.").text(theGetSimpleNameMethodName).text(".call(this, args);").newLine();
        theWriter.tab().text("};").newLine();

        theWriter.tab().text("C.prototype.").text(theGetFieldNameMethodName).assign().text("function(args)").space().text("{").newLine();
        theWriter.tab(2).text("return ").text(theMinifier.toClassName(BytecodeObjectTypeRef.fromRuntimeClass(Class.class))).text(".prototype.").text(theGetFieldNameMethodName).text(".call(this, args);").newLine();
        theWriter.tab().text("};").newLine();

        theWriter.tab().text("C.prototype.").text(theGetDeclaredFieldsNameMethodName).assign().text("function(args)").space().text("{").newLine();
        theWriter.tab(2).text("return this.declaredFields;").newLine();
        theWriter.tab().text("};").newLine();

        theWriter.tab().text("C.prototype.").text(theGetDeclaredFieldNameMethodName).assign().text("function(args)").space().text("{").newLine();
        theWriter.tab(2).text("return ").text(theMinifier.toClassName(BytecodeObjectTypeRef.fromRuntimeClass(Class.class))).text(".prototype.").text(theGetDeclaredFieldNameMethodName).text(".call(this, args);").newLine();
        theWriter.tab().text("};").newLine();

        theWriter.tab().text("C.prototype.").text(theIsPrimitiveMethodName).assign().text("function(args)").space().text("{").newLine();
        theWriter.tab(2).text("return this.primitive;").newLine();
        theWriter.tab().text("};").newLine();

        theWriter.tab(1).text("return C;").newLine();
        theWriter.text("}();").newLine();

        // We need runtime classes for Primitives
        theWriter.text("var ").text(theMinifier.toSymbol("CharPrimitiveRuntimeClass")).assign().text("function()").space().text("{").newLine();
        theWriter.tab(1).text("return new ").text(theMinifier.toSymbol("RuntimeClass"));
        theWriter.text("(-3,undefined,[],true);").newLine();
        theWriter.text("}();").newLine();
        theWriter.text("var ").text(theMinifier.toSymbol("IntPrimitiveRuntimeClass")).assign().text("function()").space().text("{").newLine();
        theWriter.tab(1).text("return new ").text(theMinifier.toSymbol("RuntimeClass"));
        theWriter.text("(-4,undefined,[],true);").newLine();
        theWriter.text("}();").newLine();
        theWriter.text("var ").text(theMinifier.toSymbol("LongPrimitiveRuntimeClass")).assign().text("function()").space().text("{").newLine();
        theWriter.tab(1).text("return new ").text(theMinifier.toSymbol("RuntimeClass"));
        theWriter.text("(-5,undefined,[],true);").newLine();
        theWriter.text("}();").newLine();
        theWriter.text("var ").text(theMinifier.toSymbol("BytePrimitiveRuntimeClass")).assign().text("function()").space().text("{").newLine();
        theWriter.tab(1).text("return new ").text(theMinifier.toSymbol("RuntimeClass"));
        theWriter.text("(-6,undefined,[],true);").newLine();
        theWriter.text("}();").newLine();
        theWriter.text("var ").text(theMinifier.toSymbol("FloatPrimitiveRuntimeClass")).assign().text("function()").space().text("{").newLine();
        theWriter.tab(1).text("return new ").text(theMinifier.toSymbol("RuntimeClass"));
        theWriter.text("(-7,undefined,[],true);").newLine();
        theWriter.text("}();").newLine();
        theWriter.text("var ").text(theMinifier.toSymbol("BooleanPrimitiveRuntimeClass")).assign().text("function()").space().text("{").newLine();
        theWriter.tab(1).text("return new ").text(theMinifier.toSymbol("RuntimeClass"));
        theWriter.text("(-8,undefined,[],true);").newLine();
        theWriter.text("}();").newLine();
        theWriter.text("var ").text(theMinifier.toSymbol("ShortPrimitiveRuntimeClass")).assign().text("function()").space().text("{").newLine();
        theWriter.tab(1).text("return new ").text(theMinifier.toSymbol("RuntimeClass"));
        theWriter.text("(-9,undefined,[],true);").newLine();
        theWriter.text("}();").newLine();
        theWriter.text("var ").text(theMinifier.toSymbol("DoublePrimitiveRuntimeClass")).assign().text("function()").space().text("{").newLine();
        theWriter.tab(1).text("return new ").text(theMinifier.toSymbol("RuntimeClass"));
        theWriter.text("(-10,undefined,[],true);").newLine();
        theWriter.text("}();").newLine();

        final BytecodeLinkedClass theByteClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Byte.class));
        final BytecodeLinkedClass theIntegerClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Integer.class));
        final BytecodeLinkedClass theCharacterClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Character.class));
        final BytecodeLinkedClass theBooleanClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Boolean.class));
        final BytecodeLinkedClass theFloatClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Float.class));
        final BytecodeLinkedClass theDoubleClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Double.class));
        final BytecodeLinkedClass theLongClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Long.class));
        final BytecodeLinkedClass theShortClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Short.class));

        aLinkerContext.finalizeLinkage();

        final ConstantPool thePool = new ConstantPool();

        final BytecodeClassTopologicOrder theOrderedClasses = new BytecodeClassTopologicOrder(aLinkerContext);
        final List<MethodHandleExpression> methodHandles = new ArrayList<>();
        final JSSSAWriter.IDResolver theResolver = e -> {
            final int pos = methodHandles.size();
            methodHandles.add(e);
            return "handle" + pos;
        };

        theOrderedClasses.getClassesInOrder().stream().forEach(theEntry -> {

            final BytecodeLinkedClass theLinkedClass = theEntry;
            final BytecodeResolvedMethods theMethods = aLinkerContext.resolveMethods(theLinkedClass);

            final String theJSClassName = theMinifier.toClassName(theEntry.getClassName());
            theWriter.text("var ").text(theJSClassName).assign().text("function()").space().text("{").newLine();

            // Constructor function
            theWriter.tab().text("var C").assign().text("function()").space().text("{").newLine();
            theWriter.tab().text("};").newLine();

            if (!theLinkedClass.getClassName().name().equals(Object.class.getName())) {
                theWriter.tab().text("var p").assign().text("Object.create(").text(theMinifier.toClassName(theLinkedClass.getSuperClass().getClassName())).text(".prototype);").newLine();
            } else {
                theWriter.tab().text("var p").assign().text("Object.create(null);").newLine();
            }
            theWriter.tab().text("C.prototype").assign().text("p;").newLine();

            // Framework-Specific methods
            theWriter.tab().text("C.").text(theMinifier.toSymbol("__runtimeclass")).assign().text("new ").symbol("RuntimeClass", null).text("(");
            // Classname index
            if (aLinkerContext.reflectionConfiguration().resolve(theLinkedClass.getClassName().name()).supportsClassForName()) {
                theWriter.text("" + thePool.register(new StringValue(theLinkedClass.getClassName().name())));
            } else {
                theWriter.text("-1");
            }
            theWriter.text(",");
            // Superclass reference
            if (!theLinkedClass.getClassName().name().equals(Object.class.getName())) {
                final BytecodeLinkedClass theSuperClass = theLinkedClass.getSuperClass();
                theWriter.text(theMinifier.toClassName(theSuperClass.getClassName())).text(".").text(theMinifier.toSymbol("__runtimeclass"));
            } else {
                theWriter.text("null");
            }
            // Implemented types
            theWriter.text(",[");
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

            theWriter.text("],false);").newLine();

            theWriter.tab().text("var ").text(theMinifier.toSymbol("$INITIALIZED")).assign().text("false;").newLine();

            // Init function
            theWriter.tab().text("C.").text(theMinifier.toSymbol("init")).assign().text("function()").space().text("{").newLine();
            theWriter.tab(2).text("if").space().text("(!").text(theMinifier.toSymbol("$INITIALIZED")).text(")").space().text("{").newLine();
            theWriter.tab(3).text(theMinifier.toSymbol("$INITIALIZED")).assign().text("true;").newLine();

            if (aLinkerContext.reflectionConfiguration().resolve(theLinkedClass.getClassName().name()).supportsClassForName()) {
                final List<BytecodeResolvedFields.FieldEntry> declaredFields = theLinkedClass.resolvedFields().stream().filter(
                        t -> t.getProvidingClass() == theLinkedClass
                ).collect(Collectors.toList());

                theWriter.tab(3).text("var ").text("fields").assign().text("bytecoder.newArray(").text(Integer.toString(declaredFields.size())).text(",null);").newLine();
                for (int i=0;i<declaredFields.size();i++) {
                    final BytecodeResolvedFields.FieldEntry theFieldEntry = declaredFields.get(i);
                    theWriter.tab(3).text("fields.data[").text(Integer.toString(i)).text("]").assign();

                    theWriter.text(theMinifier.toClassName(theFieldClass.getClassName())).text(".").text(theMinifier.toSymbol("__runtimeclass")).text(".").text(theMinifier.toMethodName("$newInstance", theFieldClassConstructorSignature)).text("(");
                    // Declared class
                    theWriter.text(theMinifier.toClassName(theFieldEntry.getProvidingClass().getClassName())).text(".").text(theMinifier.toSymbol("init")).text("()")
                            .text(".").text(theMinifier.toSymbol("__runtimeclass"));
                    theWriter.text(",");

                    // Name
                    final int theIndex = thePool.register(new StringValue(theFieldEntry.getValue().getName().stringValue()));
                    theWriter.text("bytecoder.stringpool[").text("" + theIndex).text("]");
                    theWriter.text(",");

                    // Modifier
                    theWriter.text(Integer.toString(theFieldEntry.getValue().getAccessFlags().getModifiers()));
                    theWriter.text(",");

                    // Type
                    if (theFieldEntry.getValue().getTypeRef().isPrimitive()) {
                        theWriter.text("'");
                        theWriter.text(theFieldEntry.getValue().getTypeRef().name());
                        theWriter.text("'");
                    } else if (theFieldEntry.getValue().getTypeRef().isArray()) {
                        theWriter.text(theMinifier.toClassName(theArrayType)).text(".").text(theMinifier.toSymbol("init")).text("()")
                                .text(".").text(theMinifier.toSymbol("__runtimeclass"));
                    } else {
                        final BytecodeObjectTypeRef typeRef = (BytecodeObjectTypeRef) theFieldEntry.getValue().getTypeRef();
                        theWriter.text(theMinifier.toClassName(typeRef)).text(".").text(theMinifier.toSymbol("init")).text("()")
                                .text(".").text(theMinifier.toSymbol("__runtimeclass"));
                    }
                    theWriter.text(",");

                    // Access method
                    theWriter.newLine();
                    theWriter.text("function(target) {");
                    String readBoxingFunction = null;
                    String writeUnboxingFunction = null;
                    if (theFieldEntry.getValue().getTypeRef().isPrimitive()) {
                        // We need some conversion logic here
                        final BytecodePrimitiveTypeRef primitiveTypeRef = (BytecodePrimitiveTypeRef) theFieldEntry.getValue().getTypeRef();
                        switch (primitiveTypeRef) {
                            case BYTE: {
                                final BytecodeMethodSignature theByteClassValueOfSignature = new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Byte.class),
                                        new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.BYTE});
                                theByteClass.resolveStaticMethod("valueOf", theByteClassValueOfSignature);

                                readBoxingFunction = theMinifier.toClassName(theByteClass.getClassName()) + "." + theMinifier.toMethodName("valueOf", theByteClassValueOfSignature);
                                writeUnboxingFunction = theMinifier.toMethodName("byteValue", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.BYTE, new BytecodeTypeRef[0]));
                                break;
                            }
                            case INT: {
                                final BytecodeMethodSignature theIntegerClassValueOfSignature = new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Integer.class),
                                        new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT});
                                theIntegerClass.resolveStaticMethod("valueOf", theIntegerClassValueOfSignature);

                                readBoxingFunction = theMinifier.toClassName(theIntegerClass.getClassName()) + "." + theMinifier.toMethodName("valueOf", theIntegerClassValueOfSignature);
                                writeUnboxingFunction = theMinifier.toMethodName("intValue", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[0]));
                                break;
                            }
                            case CHAR: {
                                final BytecodeMethodSignature theCharacterClassValueOfSignature = new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Character.class),
                                        new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.CHAR});
                                theCharacterClass.resolveStaticMethod("valueOf", theCharacterClassValueOfSignature);

                                readBoxingFunction = theMinifier.toClassName(theCharacterClass.getClassName()) + "." + theMinifier.toMethodName("valueOf", theCharacterClassValueOfSignature);
                                writeUnboxingFunction = theMinifier.toMethodName("charValue", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.CHAR, new BytecodeTypeRef[0]));
                                break;
                            }
                            case BOOLEAN: {
                                final BytecodeMethodSignature theBooleanClassValueOfSignature = new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Boolean.class),
                                        new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.BOOLEAN});
                                theBooleanClass.resolveStaticMethod("valueOf", theBooleanClassValueOfSignature);

                                readBoxingFunction = theMinifier.toClassName(theBooleanClass.getClassName()) + "." + theMinifier.toMethodName("valueOf", theBooleanClassValueOfSignature);
                                writeUnboxingFunction = theMinifier.toMethodName("booleanValue", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.BOOLEAN, new BytecodeTypeRef[0]));
                                break;
                            }
                            case FLOAT: {
                                final BytecodeMethodSignature theFloatClassValueOfSignature = new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Float.class),
                                        new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.FLOAT});
                                theFloatClass.resolveStaticMethod("valueOf", theFloatClassValueOfSignature);

                                readBoxingFunction = theMinifier.toClassName(theFloatClass.getClassName()) + "." + theMinifier.toMethodName("valueOf", theFloatClassValueOfSignature);
                                writeUnboxingFunction = theMinifier.toMethodName("floatValue", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.FLOAT, new BytecodeTypeRef[0]));
                                break;
                            }
                            case DOUBLE: {
                                final BytecodeMethodSignature theDoubleClassValueOfSignature = new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Double.class),
                                        new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.DOUBLE});
                                theDoubleClass.resolveStaticMethod("valueOf", theDoubleClassValueOfSignature);

                                readBoxingFunction = theMinifier.toClassName(theDoubleClass.getClassName()) + "." + theMinifier.toMethodName("valueOf", theDoubleClassValueOfSignature);
                                writeUnboxingFunction = theMinifier.toMethodName("doubleValue", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.DOUBLE, new BytecodeTypeRef[0]));
                                break;
                            }
                            case LONG: {
                                final BytecodeMethodSignature theLongClassValueOfSignature = new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Long.class),
                                        new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.LONG});
                                theLongClass.resolveStaticMethod("valueOf", theLongClassValueOfSignature);

                                readBoxingFunction = theMinifier.toClassName(theLongClass.getClassName()) + "." + theMinifier.toMethodName("valueOf", theLongClassValueOfSignature);
                                writeUnboxingFunction = theMinifier.toMethodName("longValue", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.LONG, new BytecodeTypeRef[0]));
                                break;
                            }
                            case SHORT: {
                                final BytecodeMethodSignature theShortClassValueOfSignature = new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Short.class),
                                        new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.SHORT});
                                theShortClass.resolveStaticMethod("valueOf", theShortClassValueOfSignature);

                                readBoxingFunction = theMinifier.toClassName(theShortClass.getClassName()) + "." + theMinifier.toMethodName("valueOf", theShortClassValueOfSignature);
                                writeUnboxingFunction = theMinifier.toMethodName("shortValue", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.SHORT, new BytecodeTypeRef[0]));
                                break;
                            }
                            default:
                                throw new IllegalStateException("Type conversion to " + primitiveTypeRef + " for reflective accessor not supported!");
                        }
                    }
                    if (theFieldEntry.getValue().getAccessFlags().isStatic()) {
                        // Static access
                        theWriter.text("return ");
                        if (readBoxingFunction != null) {
                            theWriter.text(readBoxingFunction).text("(");
                        }
                        theWriter.text("C.").text(theMinifier.toSymbol("init")).text("()")
                                .text(".").text(theMinifier.toSymbol("__runtimeclass"))
                                .text(".").text(theMinifier.toSymbol(theFieldEntry.getValue().getName().stringValue()));

                        if (readBoxingFunction != null) {
                            theWriter.text(")");
                        }
                        theWriter.text(";");
                    } else {
                        // Instance access
                        theWriter.text("return ");
                        if (readBoxingFunction != null) {
                            theWriter.text(readBoxingFunction).text("(");
                        }
                        theWriter.text("target.").text(theMinifier.toSymbol(theFieldEntry.getValue().getName().stringValue()));
                        if (readBoxingFunction != null) {
                            theWriter.text(")");
                        }
                        theWriter.text(";");
                    }
                    theWriter.text("},");

                    // Mutation method
                    theWriter.newLine();
                    theWriter.text("function(target,newValue) {");

                    if (theFieldEntry.getValue().getAccessFlags().isStatic()) {
                        theWriter.text("C.").text(theMinifier.toSymbol("init")).text("()")
                                .text(".").text(theMinifier.toSymbol("__runtimeclass"))
                                .text(".").text(theMinifier.toSymbol(theFieldEntry.getValue().getName().stringValue()));

                        theWriter.assign().text("newValue");

                        if (writeUnboxingFunction != null) {
                            theWriter.text(".").text(writeUnboxingFunction).text("()");
                        }
                        theWriter.text(";");
                    } else {
                        theWriter.text("target.").text(theMinifier.toSymbol(theFieldEntry.getValue().getName().stringValue()));
                        theWriter.assign().text("newValue");

                        if (writeUnboxingFunction != null) {
                            theWriter.text(".").text(writeUnboxingFunction).text("()");
                        }


                        theWriter.text(";");
                    }
                    theWriter.text("}").newLine();

                    theWriter.text(",-1);");
                }
            } else {
                // Empty field list
                theWriter.tab(3).text("var ").text("fields").assign().text("bytecoder.newArray(0,null);").newLine();
            }

            theWriter.tab(3).text("C.").text(theMinifier.toSymbol("__runtimeclass")).text(".declaredFields").assign().text("fields;").newLine();

            // Constructors
            if (theLinkedClass.hasTag(BytecodeLinkedClass.Tag.INSTANTIATED)) {
                theMethods.stream().forEach(aEntry -> {
                    final BytecodeMethod theMethod = aEntry.getValue();
                    if (theMethod.isConstructor() && aEntry.getProvidingClass() == theLinkedClass) {

                        final StringBuilder theSignature = new StringBuilder();
                        for (int i = 0; i < theMethod.getSignature().getArguments().length; i++) {
                            if (i > 0) {
                                theSignature.append(",");
                            }
                            theSignature.append("p");
                            theSignature.append(i);
                        }

                        theWriter.tab(3).text("C.").text(theMinifier.toSymbol("__runtimeclass")).text(".").text(theMinifier.toMethodName("$newInstance", theMethod.getSignature())).assign().text("function(").text(theSignature.toString()).text(")").space().text("{").newLine();
                        theWriter.tab(4).text("var ").text(theMinifier.toSymbol("instance")).assign().text("new C();").newLine();

                        theWriter.tab(4).text(theMinifier.toSymbol("instance")).text(".").text("$").text(Integer.toString(theLinkedClass.getUniqueId())).text(theMinifier.toMethodName(theMethod.getName().stringValue(), theMethod.getSignature()))
                                .text("(").text(theSignature.toString()).text(");").newLine();


                        theWriter.tab(4).text("return ").text(theMinifier.toSymbol("instance")).text(";").newLine();
                        theWriter.tab(3).text("};").newLine();
                    }
                });
            }

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
            theWriter.tab().text("p.constructor").assign().text("C").space().text(";").newLine();

            // Constructors
            if (theLinkedClass.hasTag(BytecodeLinkedClass.Tag.INSTANTIATED)) {
                theMethods.stream().forEach(aEntry -> {

                    final BytecodeMethod theMethod = aEntry.getValue();

                    if (theMethod.isConstructor() && aEntry.getProvidingClass() == theLinkedClass) {

                        final StringBuilder theSignature = new StringBuilder();
                        for (int i = 0; i < theMethod.getSignature().getArguments().length; i++) {
                            if (i > 0) {
                                theSignature.append(",");
                            }
                            theSignature.append("p");
                            theSignature.append(i);
                        }
                        theWriter.tab().text("C.").text(theMinifier.toSymbol("__runtimeclass")).text(".").text(theMinifier.toMethodName("$newInstance", theMethod.getSignature())).assign().text("function(").text(theSignature.toString()).text(")").space().text("{").newLine();
                        theWriter.tab(2).text("C.").text(theMinifier.toSymbol("init")).text("();").newLine();
                        theWriter.tab(2).text("var ").text(theMinifier.toSymbol("instance")).assign().text("new C();").newLine();

                        theWriter.tab(2).text(theMinifier.toSymbol("instance")).text(".").text("$").text(Integer.toString(theLinkedClass.getUniqueId())).text(theMinifier.toMethodName(theMethod.getName().stringValue(), theMethod.getSignature()))
                                .text("(").text(theSignature.toString()).text(");").newLine();


                        theWriter.tab(2).text("return ").text(theMinifier.toSymbol("instance")).text(";").newLine();
                        theWriter.tab().text("};").newLine();
                    }

                });
            }

            // NewLambdaInstance function
            if (theLinkedClass.getBytecodeClass().getAccessFlags().isInterface()) {
                theWriter.tab().text("C.").text(theMinifier.toSymbol("newLambdaInstance")).assign().text("function(impl)").space().text("{").newLine();
                theWriter.tab(2).text("var l").assign().text("C.").text(theMinifier.toSymbol("newInstance")).text("();").newLine();
                for (final BytecodeMethod theMethod : theLinkedClass.getBytecodeClass().getMethods()) {
                    if (!theMethod.isConstructor() && !theMethod.isClassInitializer() && theMethod.getAccessFlags().isAbstract()) {
                        theWriter.tab(2).text("l.").text(theMinifier.toMethodName(theMethod.getName().stringValue(), theMethod.getSignature())).assign().text("impl.bind(l);").newLine();
                    }
                }
                theWriter.tab(2).text("return l;").newLine();

                theWriter.tab().text("};").newLine();
            }

            // then we add class specific static fields
            final BytecodeResolvedFields theStaticFields = theLinkedClass.resolvedFields();
            theStaticFields.streamForStaticFields().filter(t -> t.getProvidingClass() == theLinkedClass).forEach(
                    aFieldEntry -> {
                        if (!"$assertionsDisabled".equals(aFieldEntry.getValue().getName().stringValue())) {
                            final BytecodeTypeRef theFieldType = aFieldEntry.getValue().getTypeRef();
                            if (theFieldType.isPrimitive()) {
                                final BytecodePrimitiveTypeRef thePrimitive = (BytecodePrimitiveTypeRef) theFieldType;
                                if (thePrimitive == BytecodePrimitiveTypeRef.BOOLEAN) {
                                    theWriter.tab().text("C.").text(theMinifier.toSymbol("__runtimeclass")).text(".").symbol(aFieldEntry.getValue().getName().stringValue(), null).assign().text("false;").newLine();
                                } else {
                                    theWriter.tab().text("C.").text(theMinifier.toSymbol("__runtimeclass")).text(".").symbol(aFieldEntry.getValue().getName().stringValue(), null).assign().text("0;").newLine();
                                }
                            } else {
                                theWriter.tab().text("C.").text(theMinifier.toSymbol("__runtimeclass")).text(".").symbol(aFieldEntry.getValue().getName().stringValue(), null).assign().text("null;").newLine();
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
                                    theWriter.tab().text("p.").text(theMinifier.toSymbol(aFieldEntry.getValue().getName().stringValue())).assign().text("false;").newLine();
                                } else {
                                    theWriter.tab().text("p.").text(theMinifier.toSymbol(aFieldEntry.getValue().getName().stringValue())).assign().text("0;").newLine();
                                }
                            } else {
                                theWriter.tab().text("p.").text(theMinifier.toSymbol(aFieldEntry.getValue().getName().stringValue())).assign().text("null;").newLine();
                            }
                        });
            }

            theMethods.stream().forEach(aEntry -> {
                final BytecodeMethod theMethod = aEntry.getValue();
                final BytecodeMethodSignature theCurrentMethodSignature = theMethod.getSignature();

                // If the method is provided by the runtime, we do not need to generate the implementation
                if (null != theMethod.getAttributes().getAnnotationByType(EmulatedByRuntime.class.getName())) {

                    if (aEntry.getProvidingClass().getClassName().equals(BytecodeObjectTypeRef.fromRuntimeClass(Class.class))
                        && theMethod.getName().stringValue().equals("forName")
                        && theMethod.getSignature().matchesExactlyTo(BytecodeLinkedClass.CLASS_FOR_NAME_SIGNATURE)) {

                        // Special method: we resolve a runtime class by name here
                        theWriter.tab().text("C.");

                        final String theJSMethodName = theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature);

                        theWriter.text(theJSMethodName).assign().text("function(className,resolve,classloader)").space().text("{").newLine();
                        theWriter.tab(2).text("var jsClassName").assign().text("bytecoder.toJSString(className);").newLine();

                        // We search for all non abstract non interface classes
                        theOrderedClasses.getClassesInOrder().stream().filter(t -> aLinkerContext.reflectionConfiguration().resolve(t.getClassName().name()).supportsClassForName()).forEach(search -> {
                            if (!search.getBytecodeClass().getAccessFlags().isAbstract() && !search.getBytecodeClass().getAccessFlags().isInterface()) {
                                final String theSearchClassName = theMinifier.toClassName(search.getClassName());
                                theWriter.tab(2).text("if").space().text("(jsClassName").space().text("===").space().text("'").text(search.getClassName().name()).text("') return ").text(theSearchClassName).text(".").text(theMinifier.toSymbol("__runtimeclass")).text(";").newLine();
                            }
                        });

                        theWriter.tab(2).text("throw new Error();").newLine();
                        theWriter.tab().text("};").newLine();
                    }

                    if (aEntry.getProvidingClass().getClassName().equals(BytecodeObjectTypeRef.fromRuntimeClass(Class.class))
                            && theMethod.getName().stringValue().equals("getClassLoader")
                            && theMethod.getSignature().matchesExactlyTo(BytecodeLinkedClass.GET_CLASSLOADER_SIGNATURE)) {

                        // Special method: we resolve a runtime class by name here
                        theWriter.tab().text("C.");

                        final String theJSMethodName = theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature);

                        theWriter.text(theJSMethodName).assign().text("function()").space().text("{").newLine();
                        theWriter.tab(2).text("return null;").newLine();
                        theWriter.tab().text("};").newLine();
                    }

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
                        theWriter.tab().text("p.").text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature)).assign()
                                .text(theMinifier.toClassName(aEntry.getProvidingClass().getClassName())).text(".prototype.").text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature)).text(";").newLine();
                    }
                    return;
                }

                aLinkerContext.getLogger().info("Compiling {}.{}", theLinkedClass.getClassName().name(), theMethod.getName().stringValue());

                final ProgramGenerator theGenerator = programGeneratorFactory.createFor(aLinkerContext, new JSIntrinsics());
                final Program theSSAProgram = theGenerator.generateFrom(aEntry.getProvidingClass().getBytecodeClass(), theMethod);

                //Run optimizer
                if (!theMethod.getAccessFlags().isAbstract() && !theMethod.getAccessFlags().isNative()) {
                    aOptions.getOptimizer().optimize(this, theSSAProgram.getControlFlowGraph(), aLinkerContext);
                }

                final StringBuilder theArguments = new StringBuilder();
                boolean myfirst = true;
                for (final Variable theVariable : theSSAProgram.getArguments()) {
                    if (!Variable.THISREF_NAME.equals(theVariable.getName())) {
                        if (!myfirst) {
                            theArguments.append(',');
                        }
                        theArguments.append(theMinifier.toVariableName(theVariable.getName()));
                        myfirst = false;
                    }
                }

                if (theMethod.getAccessFlags().isNative()) {
                    if (theLinkedClass.emulatedByRuntime()) {
                        return;
                    }

                    final BytecodeImportedLink theLink = theLinkedClass.linkFor(theMethod);

                    theWriter.tab().text("C.");

                    final String theJSMethodName = theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature);

                    theWriter.text(theJSMethodName)
                            .assign().text("function(").text(theArguments.toString()).text(")").space().text("{").newLine();

                    theWriter.tab(2).text("return bytecoder.imports.").text(theLink.getModuleName()).text(".").text(theLink.getLinkName()).text("(").text(theArguments.toString()).text(");").newLine();
                    theWriter.tab().text("};").newLine();

                    if (!theMethod.getAccessFlags().isStatic()) {
                        theWriter.tab().text("p.").text(theJSMethodName).assign().text("C.").text(theJSMethodName).text(";").newLine();
                    }

                    return;
                }

                theWriter.assignSymbolToSourceFile(theLinkedClass.getClassName().name() + "." + theMethod.getName().stringValue(), theSSAProgram.getDebugInformation().debugPositionFor(
                        BytecodeOpcodeAddress.START_AT_ZERO));
                if (theMethod.getAccessFlags().isStatic()) {
                    theWriter.tab().text("C.").text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature))
                            .assign().text("function(").text(theArguments.toString()).text(")").space().text("{").newLine();
                } else if (theMethod.isConstructor()) {
                    theWriter.tab().text("p.").text("$").text(Integer.toString(theLinkedClass.getUniqueId())).text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature))
                            .assign().text("function(").text(theArguments.toString()).text(")").space().text("{").newLine();
                } else {
                    theWriter.tab().text("p.").text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature))
                            .assign().text("function(").text(theArguments.toString()).text(")").space().text("{").newLine();
                }

                theWriter.flush();

                // Perform register allocation
                final AbstractAllocator theAllocator = aOptions.getAllocator().allocate(theSSAProgram, Variable::resolveType, aLinkerContext);

                final JSSSAWriter theVariablesWriter = new JSSSAWriter(aOptions, theSSAProgram, 2, theWriter, aLinkerContext, thePool, false, theMinifier, theAllocator, theResolver);
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
                    // Do nothing here
                } else {
                    theWriter.tab().text("C.").text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature))
                        .assign().text("p.").text(theMinifier.toMethodName(theMethod.getName().stringValue(), theCurrentMethodSignature)).text(";").newLine();
                }

            });

            theWriter.tab().text("return C;").newLine();

            theWriter.text("}();").newLine();
        });

        // Generate method handle delegate functions here
        for (int i=0;i<methodHandles.size();i++) {
            final MethodHandleExpression theMethodHandle = methodHandles.get(i);

            final String theDelegateMethodName = theMinifier.toSymbol("handle" + i);

            switch (theMethodHandle.getReferenceKind()) {
                case REF_invokeStatic:
                    writeMethodHandleDelegateInvokeStatic(theMethodHandle, theDelegateMethodName, theWriter, theMinifier);
                    break;
                case REF_newInvokeSpecial:
                    writeMethodHandleDelegateNewInvokeSpecial(theMethodHandle, theDelegateMethodName, theWriter, theMinifier);
                    break;
                case REF_invokeInterface:
                    writeMethodHandleDelegateInvokeInterface(theMethodHandle, theDelegateMethodName, theWriter, theMinifier);
                    break;
                case REF_invokeVirtual:
                    writeMethodHandleDelegateInvokeVirtual(theMethodHandle, theDelegateMethodName, theWriter, theMinifier);
                    break;
                case REF_invokeSpecial:
                    writeMethodHandleDelegateInvokeSpecial(theMethodHandle, theDelegateMethodName, theWriter, theMinifier);
                    break;
                default:
                    throw new IllegalArgumentException("Not supported refkind for method handle " + theMethodHandle.getReferenceKind());
            }
        }
        theWriter.text("bytecoder.bootstrap").assign().text("{").newLine();
        theWriter.text("};").newLine();

        theWriter.text("bytecoder.bootstrap").assign().text("function()").space().text("{").newLine();

        // String initialization depends on exports, so we have to declare them first
        theOrderedClasses.getClassesInOrder().forEach(aEntry -> {
            final BytecodeResolvedMethods theMethods = aLinkerContext.resolveMethods(aEntry);
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

        final List<StringValue> theValues = thePool.stringValues();
        for (int i=0; i<theValues.size(); i++) {
            final StringValue theValue = theValues.get(i);
            theWriter.tab().text("bytecoder.stringpool[").text("" + i).text("]").assign().text("bytecoder.newString(").text(toArray(theValue.getStringValue())).text(");").newLine();
        }

        theWriter.tab().text("bytecoder.initializeFileIO();").newLine();

        theWriter.text("}").newLine();

        theWriter.flush();

        theStrWriter.append(System.lineSeparator()).append("//# sourceMappingURL=").append(aOptions.getFilenamePrefix()).append(".js.map");

        return new JSCompileResult(theMinifier,
                new CompileResult.StringContent(aOptions.getFilenamePrefix() + ".js", theStrWriter.toString()),
                new CompileResult.StringContent(aOptions.getFilenamePrefix() + ".js.map", theSourceMapWriter.toSourceMap(aOptions.getFilenamePrefix() + ".js")));
    }

    public String toArray(final String aData) {
        final byte[] theUTF8Data = aData.getBytes(StandardCharsets.ISO_8859_1);
        final StringBuilder theResult = new StringBuilder("[");
        for (int i=0;i<theUTF8Data.length;i++) {
            if (i>0) {
                theResult.append(",");
            }
            theResult.append(theUTF8Data[i]);
        }
        theResult.append("]");
        return theResult.toString();
    }

    private void writeMethodHandleDelegateInvokeStatic(final MethodHandleExpression aMethodHandle, final String aDelegateMethodName, final JSPrintWriter aWriter, final JSMinifier aMinifier) {
        aWriter.text("bytecoder.methodhandles.").text(aDelegateMethodName).assign().text("function(");

        final MethodHandleExpression.AdapterAnnotation theAdapterAnnotation = aMethodHandle.getAdapterAnnotation();

        // We build the dynamic signature here
        final List<String> theDelegateArgs = new ArrayList<>();
        final List<String> theCallingArgs = new ArrayList<>();
        for (int k=0;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
            final String theArgName = "linkArg" + k;
            theDelegateArgs.add(theArgName);
            theCallingArgs.add(theArgName);
        }
        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            theDelegateArgs.add(theArgName);
            theCallingArgs.add(theArgName);
        }

        for (int j=0;j<theDelegateArgs.size();j++) {
            if (j>0) {
                aWriter.text(",");
            }
            aWriter.text(theDelegateArgs.get(j));
        }
        aWriter.text(") {").newLine();

        aWriter.tab(1).text("return ");
        aWriter.text(aMinifier.toClassName(aMethodHandle.getClassName()));
        aWriter.text(".").text(aMinifier.toSymbol("init")).text("().");
        aWriter.text(aMinifier.toMethodName(aMethodHandle.getMethodName(), aMethodHandle.getImplementationSignature()));

        aWriter.text("(");
        for (int j=0;j<theCallingArgs.size();j++) {
            if (j>0) {
                aWriter.text(",");
            }
            aWriter.text(theCallingArgs.get(j));
        }
        aWriter.text(");");
        aWriter.newLine();

        aWriter.text("};").newLine();
    }

    private void writeMethodHandleDelegateInvokeInterface(final MethodHandleExpression aMethodHandle, final String aDelegateMethodName, final JSPrintWriter aWriter, final JSMinifier aMinifier) {
        aWriter.text("bytecoder.methodhandles.").text(aDelegateMethodName).assign().text("function(");

        final MethodHandleExpression.AdapterAnnotation theAdapterAnnotation = aMethodHandle.getAdapterAnnotation();

        // We build the dynamic signature here
        final List<String> theDelegateArgs = new ArrayList<>();
        final List<String> theCallingArgs = new ArrayList<>();
        for (int k=0;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
            // We ignore the first static arg, as this is passed as "this" to the function
            final String theArgName = "linkArg" + k;
            theDelegateArgs.add(theArgName);
            theCallingArgs.add(theArgName);
        }
        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            if (k>0) {
                theDelegateArgs.add(theArgName);
                theCallingArgs.add(theArgName);
            } else {
                theCallingArgs.add("this");
            }
        }

        for (int j=0;j<theDelegateArgs.size();j++) {
            if (j>0) {
                aWriter.text(",");
            }
            aWriter.text(theDelegateArgs.get(j));
        }
        aWriter.text(") {").newLine();

        aWriter.tab(1).text("return this.");
        aWriter.text(aMinifier.toMethodName(aMethodHandle.getMethodName(), aMethodHandle.getImplementationSignature()));

        aWriter.text("(");
        for (int j=0;j<theCallingArgs.size();j++) {
            if (j>0) {
                aWriter.text(",");
            }
            aWriter.text(theCallingArgs.get(j));
        }
        aWriter.text(");");
        aWriter.newLine();

        aWriter.text("};").newLine();
    }

    private void writeMethodHandleDelegateNewInvokeSpecial(final MethodHandleExpression aMethodHandle, final String aDelegateMethodName, final JSPrintWriter aWriter, final JSMinifier aMinifier) {
        aWriter.text("bytecoder.methodhandles.").text(aDelegateMethodName).assign().text("function(");

        final MethodHandleExpression.AdapterAnnotation theAdapterAnnotation = aMethodHandle.getAdapterAnnotation();

        // We build the dynamic signature here
        final List<String> theDelegateArgs = new ArrayList<>();
        final List<String> theCallingArgs = new ArrayList<>();
        for (int k=0;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
            // We ignore the first static arg, as this is passed as "this" to the function
            final String theArgName = "linkArg" + k;
            if (k>0) {
                theDelegateArgs.add(theArgName);
                theCallingArgs.add(theArgName);
            } else {
                theCallingArgs.add("this");
            }
        }
        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            theDelegateArgs.add(theArgName);
            theCallingArgs.add(theArgName);
        }

        for (int j=0;j<theDelegateArgs.size();j++) {
            if (j>0) {
                aWriter.text(",");
            }
            aWriter.text(theDelegateArgs.get(j));
        }
        aWriter.text(") {").newLine();

        aWriter.tab(1).text("return ");
        aWriter.text(aMinifier.toClassName(aMethodHandle.getClassName()));
        aWriter.text(".").text(aMinifier.toSymbol("init")).text("()");
        aWriter.text(".").text(aMinifier.toSymbol("__runtimeclass"));
        aWriter.text(".").text(aMinifier.toMethodName("$newInstance", aMethodHandle.getImplementationSignature()));

        aWriter.text("(");
        for (int j=0;j<theCallingArgs.size();j++) {
            if (j>0) {
                aWriter.text(",");
            }
            aWriter.text(theCallingArgs.get(j));
        }
        aWriter.text(");");
        aWriter.newLine();

        aWriter.text("};").newLine();
    }

    private void writeMethodHandleDelegateInvokeVirtual(final MethodHandleExpression aMethodHandle, final String aDelegateMethodName, final JSPrintWriter aWriter, final JSMinifier aMinifier) {
        aWriter.text("bytecoder.methodhandles.").text(aDelegateMethodName).assign().text("function(");

        final MethodHandleExpression.AdapterAnnotation theAdapterAnnotation = aMethodHandle.getAdapterAnnotation();

        // We build the dynamic signature here
        final List<String> theDelegateArgs = new ArrayList<>();
        final List<String> theCallingArgs = new ArrayList<>();
        for (int k=0;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
            // We ignore the first static arg, as this is passed as "this" to the function
            final String theArgName = "linkArg" + k;
            if (k>0) {
                theDelegateArgs.add(theArgName);
                theCallingArgs.add(theArgName);
            }
        }
        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            theDelegateArgs.add(theArgName);
            theCallingArgs.add(theArgName);
        }

        for (int j=0;j<theDelegateArgs.size();j++) {
            if (j>0) {
                aWriter.text(",");
            }
            aWriter.text(theDelegateArgs.get(j));
        }
        aWriter.text(") {").newLine();

        aWriter.tab(1).text("return this.");
        aWriter.text(aMinifier.toMethodName(aMethodHandle.getMethodName(), aMethodHandle.getImplementationSignature()));

        aWriter.text("(");
        for (int j=0;j<theCallingArgs.size();j++) {
            if (j>0) {
                aWriter.text(",");
            }
            aWriter.text(theCallingArgs.get(j));
        }
        aWriter.text(");");
        aWriter.newLine();

        aWriter.text("};").newLine();
    }

    private void writeMethodHandleDelegateInvokeSpecial(final MethodHandleExpression aMethodHandle, final String aDelegateMethodName, final JSPrintWriter aWriter, final JSMinifier aMinifier) {
        aWriter.text("bytecoder.methodhandles.").text(aDelegateMethodName).assign().text("function(");

        final MethodHandleExpression.AdapterAnnotation theAdapterAnnotation = aMethodHandle.getAdapterAnnotation();

        // We build the dynamic signature here
        final List<String> theDelegateArgs = new ArrayList<>();
        final List<String> theCallingArgs = new ArrayList<>();
        for (int k=0;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
            // We ignore the first static arg, as this is passed as "this" to the function
            final String theArgName = "linkArg" + k;
            if (k>0) {
                theDelegateArgs.add(theArgName);
                theCallingArgs.add(theArgName);
            }
        }
        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            theDelegateArgs.add(theArgName);
            theCallingArgs.add(theArgName);
        }

        for (int j=0;j<theDelegateArgs.size();j++) {
            if (j>0) {
                aWriter.text(",");
            }
            aWriter.text(theDelegateArgs.get(j));
        }
        aWriter.text(") {").newLine();

        aWriter.tab(1).text("return ");
        aWriter.text(aMinifier.toClassName(aMethodHandle.getClassName()));
        aWriter.text(".");
        aWriter.text(aMinifier.toMethodName(aMethodHandle.getMethodName(), aMethodHandle.getImplementationSignature()));
        aWriter.text(".call(this");
        for (final String theCallingArg : theCallingArgs) {
            aWriter.text(",");
            aWriter.text(theCallingArg);
        }
        aWriter.text(");");
        aWriter.newLine();

        aWriter.text("};").newLine();
    }
}