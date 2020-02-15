/*
 * Copyright 2020 Mirko Sertic
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
package de.mirkosertic.bytecoder.backend.llvm;

import de.mirkosertic.bytecoder.api.EmulatedByRuntime;
import de.mirkosertic.bytecoder.api.Export;
import de.mirkosertic.bytecoder.api.Substitutes;
import de.mirkosertic.bytecoder.backend.CompileBackend;
import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.CompileResult;
import de.mirkosertic.bytecoder.backend.NativeMemoryLayouter;
import de.mirkosertic.bytecoder.classlib.Address;
import de.mirkosertic.bytecoder.classlib.Array;
import de.mirkosertic.bytecoder.classlib.MemoryManager;
import de.mirkosertic.bytecoder.core.BytecodeAnnotation;
import de.mirkosertic.bytecoder.core.BytecodeClass;
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
import de.mirkosertic.bytecoder.core.BytecodeVirtualMethodIdentifier;
import de.mirkosertic.bytecoder.graph.Edge;
import de.mirkosertic.bytecoder.optimizer.KnownOptimizer;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.ssa.ProgramGeneratorFactory;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Variable;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LLVMCompilerBackend implements CompileBackend<LLVMCompileResult> {

    private final ProgramGeneratorFactory programGeneratorFactory;

    public LLVMCompilerBackend(final ProgramGeneratorFactory aProgramGeneratorFactory) {
        this.programGeneratorFactory = aProgramGeneratorFactory;
    }

    private static class CallSite {
        private final Program program;
        private final RegionNode bootstrapMethod;

        private CallSite(final Program aProgram, final RegionNode aBootstrapMethod) {
            this.program = aProgram;
            this.bootstrapMethod = aBootstrapMethod;
        }
    }

    @Override
    public LLVMCompileResult generateCodeFor(final CompileOptions aOptions, final BytecodeLinkerContext aLinkerContext,
            final Class aEntryPointClass, final String aEntryPointMethodName, final BytecodeMethodSignature aEntryPointSignatue) {

        final BytecodeLinkedClass theArrayClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Array.class));
        theArrayClass.resolveConstructorInvocation(new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[0]));

        // We need to link the memory manager
        final BytecodeLinkedClass theMemoryManagerClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class));

        theMemoryManagerClass.resolveStaticMethod("freeMem", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.LONG, new BytecodeTypeRef[0]));
        theMemoryManagerClass.resolveStaticMethod("usedMem", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.LONG, new BytecodeTypeRef[0]));

        theMemoryManagerClass.resolveStaticMethod("free", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT}));
        theMemoryManagerClass.resolveStaticMethod("malloc", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT}));
        theMemoryManagerClass.resolveStaticMethod("newObject", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
        theMemoryManagerClass.resolveStaticMethod("newArray", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
        theMemoryManagerClass.resolveStaticMethod("newArray", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

        final LLVMCompileResult theCompileResult = new LLVMCompileResult();

        try {
            final List<String> stringPool = new ArrayList<>();
            final Map<String, CallSite> callsites = new HashMap<>();
            final LLVMWriter.SymbolResolver theSymbolResolver = new LLVMWriter.SymbolResolver() {
                @Override
                public String globalFromStringPool(final String aValue) {
                    final int i = stringPool.indexOf(aValue);
                    if (i >= 0) {
                        return "strpool_" + i;
                    }
                    stringPool.add(aValue);
                    return "strpool_" + (stringPool.size() - 1);
                }

                @Override
                public String resolveCallsiteBootstrapFor(final BytecodeClass owningClass, final String callsiteId, final Program program, final RegionNode bootstrapMethod) {
                    CallSite callSite = callsites.get(callsiteId);
                    if (callSite == null) {
                        callSite = new CallSite(program, bootstrapMethod);
                        callsites.put(callsiteId, callSite);
                    }
                    return "resolvecallsite" + System.identityHashCode(callSite);
                }
            };

            final NativeMemoryLayouter memoryLayouter = new NativeMemoryLayouter(aLinkerContext);

            final File theLLFile = File.createTempFile("llvm", ".ll");
            try (final PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(theLLFile), StandardCharsets.UTF_8))) {
                // We write the header first
                pw.println("target triple = \"wasm32-unknown-unknown\"");
                pw.println();

                pw.println("@__heap_base = external global i32");
                pw.println("@__data_end = external global i32");
                pw.println("@__memory_base = external global i32");

                pw.println("@stacktop = global i32 0");
                pw.println();

                pw.println("declare i32 @llvm.wasm.memory.size.i32(i32) nounwind readonly");
                pw.println("declare void @llvm.trap() cold noreturn nounwind");
                pw.println("declare float @llvm.minimum.f32(float %Val0, float %Val1)");
                pw.println("declare float @llvm.maximum.f32(float %Val0, float %Val1)");
                pw.println("declare float @llvm.floor.f32(float  %Val)");
                pw.println("declare float @llvm.ceil.f32(float  %Val)");
                pw.println();

                final AtomicInteger attributeCounter = new AtomicInteger();

                // We write the imported functions first
                aLinkerContext.linkedClasses().forEach(aEntry -> {

                    if (aEntry.targetNode().getBytecodeClass().getAccessFlags().isInterface() && !aEntry.targetNode().isOpaqueType()) {
                        return;
                    }
                    if (Objects.equals(aEntry.targetNode().getClassName(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                        return;
                    }
                    if (Objects.equals(aEntry.targetNode().getClassName(), BytecodeObjectTypeRef.fromRuntimeClass(java.lang.reflect.Array.class))) {
                        return;
                    }

                    final BytecodeResolvedMethods theMethodMap = aEntry.targetNode().resolvedMethods();
                    theMethodMap.stream().forEach(aMethodMapEntry -> {

                        final BytecodeLinkedClass theProvidingClass = aMethodMapEntry.getProvidingClass();

                        // Only add implementation methods
                        if (!(theProvidingClass == aEntry.targetNode())) {
                            return;
                        }

                        final BytecodeMethod t = aMethodMapEntry.getValue();
                        final BytecodeMethodSignature theSignature = t.getSignature();

                        if (t.getAccessFlags().isNative() || (t.getAccessFlags().isAbstract() && theProvidingClass.isOpaqueType())) {
                            if (theProvidingClass.emulatedByRuntime()) {
                                return;
                            }
                            if (t.emulatedByRuntime()) {
                                return;
                            }

                            // Native methods are imported via annotation
                            if (!t.getAccessFlags().isNative() && theProvidingClass.isOpaqueType()) {
                                // For all the other methods we programatically generate
                                // the JS wrapper implementation later by this compiler
                                //opaqueReferenceMethods.add(new WASMSSAASTCompilerBackend.OpaqueReferenceMethod(theProvidingClass, t));
                            }

                            final BytecodeImportedLink theLink = theProvidingClass.linkfor(t);

                            final String methodName = LLVMWriterUtils
                                    .toMethodName(theProvidingClass.getClassName(), t.getName(), theSignature);

                            pw.print("attributes #");
                            pw.print(attributeCounter.get());
                            pw.print(" = {");
                            pw.print("\"wasm-import-module\"");
                            pw.print("=");
                            pw.print("\"");
                            pw.print(theLink.getModuleName());
                            pw.print("\" ");
                            pw.print("\"wasm-import-name\"");
                            pw.print("=");
                            pw.print("\"");
                            pw.print(theLink.getLinkName());
                            pw.println("\"}");

                            pw.print("declare ");
                            pw.print(LLVMWriterUtils.toType(TypeRef.toType(t.getSignature().getReturnType())));
                            pw.print(" @");
                            pw.print(methodName);
                            pw.print("(");

                            pw.print(LLVMWriterUtils.toType(TypeRef.Native.REFERENCE));
                            pw.print(" ");
                            pw.print("%thisRef");
                            for (int i = 0; i < theSignature.getArguments().length; i++) {
                                final BytecodeTypeRef theParamType = theSignature.getArguments()[i];
                                pw.print(",");
                                pw.print(LLVMWriterUtils.toType(TypeRef.toType(theParamType)));
                                pw.print(" ");
                                pw.print("%p");
                                pw.print(i + 1);
                            }

                            pw.print(")");
                            pw.print(" #");
                            pw.print(attributeCounter);
                            pw.println();
                            pw.println();

                            attributeCounter.incrementAndGet();
                        }
                    });
                });

                // Some utility function
                pw.println("define internal i32 @instanceof(i32 %object, i32 %typeid) alwaysinline  {");
                pw.println("entry:");
                pw.println("    %nulltest = icmp eq i32 %object, 0");
                pw.println("    br i1 %nulltest, label %isnull, label %notnull");
                pw.println("notnull:");

                pw.println("    %ptr = add i32 %object, 4");
                pw.println("    %vtable = inttoptr i32 %ptr to i32(i32,i32)*");
                pw.print("    %resolved = call i32(i32,i32) %vtable(i32 %object, i32 ");
                pw.print(LLVMWriter.GENERATED_INSTANCEOF_METHOD_ID);
                pw.println(")");
                pw.println("    %resolved_ptr = inttoptr i32 %resolved to i32(i32,i32)*");
                pw.println("    %result = call i32 %resolved_ptr(i32 %object, i32 %typeid)");
                pw.println("    ret i32 %result");
                pw.println("isnull:");
                pw.println("    ret i32 0");
                pw.println("}");
                pw.println();

                // NaN values are not equals to themself
                pw.println("define internal i32 @isnan(float %value) alwaysinline  {");
                pw.println("entry:");
                pw.println("    %test = fcmp oeq float %value, %value");
                pw.println("    br i1 %test, label %iseq, label %isnoteq");
                pw.println("iseq:");
                pw.println("    ret i32 0");
                pw.println("isnoteq:");
                pw.println("    ret i32 1");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @toi32(float %value) alwaysinline {");
                pw.println("entry:");
                pw.println("    %test = fcmp oeq float %value, %value");
                pw.println("    br i1 %test, label %iseq, label %isnoteq");
                pw.println("iseq:");
                pw.println("    %converted = fptosi float %value to i32");
                pw.println("    ret i32 %converted");
                pw.println("isnoteq:");
                pw.println("    ret i32 0");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @compare_i32(i32 %v1, i32 %v2) alwaysinline  {");
                pw.println("entry:");
                pw.println("    %test = icmp eq i32 %v1,%v2");
                pw.println("    br i1 %test, label %iseq, label %isnoteq");
                pw.println("iseq:");
                pw.println("    ret i32 0");
                pw.println("isnoteq:");
                pw.println("    %test2 = icmp sgt i32 %v1,%v2");
                pw.println("    br i1 %test, label %isgreater, label %issmaller");
                pw.println("isgreater:");
                pw.println("    ret i32 1");
                pw.println("issmaller:");
                pw.println("    ret i32 -1");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @compare_f32(float %v1, float %v2) alwaysinline  {");
                pw.println("entry:");
                pw.println("    %test = fcmp oeq float %v1,%v2");
                pw.println("    br i1 %test, label %iseq, label %isnoteq");
                pw.println("iseq:");
                pw.println("    ret i32 0");
                pw.println("isnoteq:");
                pw.println("    %test2 = fcmp ogt float %v1,%v2");
                pw.println("    br i1 %test, label %isgreater, label %issmaller");
                pw.println("isgreater:");
                pw.println("    ret i32 1");
                pw.println("issmaller:");
                pw.println("    ret i32 -1");
                pw.println("}");
                pw.println();

                pw.println("define internal i1 @exceedsrange(i32 %v, i32 %min, i32 %max) alwaysinline  {");
                pw.println("entry:");
                pw.println("    %test = icmp slt i32 %v, %min");
                pw.println("    br i1 %test, label %exceed, label %continue");
                pw.println("continue:");
                pw.println("    %test2 = icmp sgt i32 %v, %max");
                pw.println("    br i1 %test2, label %exceed, label %ok");
                pw.println("ok:");
                pw.println("    ret i1 false");
                pw.println("exceed:");
                pw.println("    ret i1 true");
                pw.println("}");
                pw.println();

                pw.println("define internal float @div_floatfloat(float %a, float %b) alwaysinline  {");
                pw.println("entry:");
                pw.println("    %result = fdiv float %a, %b");
                pw.println("    ret float %result");
                pw.println("}");
                pw.println();

                pw.println("define internal float @div_i32i32(i32 %a, i32 %b) alwaysinline  {");
                pw.println("entry:");
                pw.println("    %temp1 = sitofp i32 %a to float");
                pw.println("    %temp2 = sitofp i32 %b to float");
                pw.println("    %result = fdiv float %temp1, %temp2");
                pw.println("    ret float %result");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @maximum(i32 %a, i32 %b) alwaysinline  {");
                pw.println("entry:");
                pw.println("   %test = icmp sgt i32 %a, %b");
                pw.println("   br i1 %test, label %aisgreater, label %notgreater");
                pw.println("aisgreater:");
                pw.println("   ret i32 %a");
                pw.println("notgreater:");
                pw.println("   ret i32 %b");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @minimum(i32 %a, i32 %b) alwaysinline  {");
                pw.println("entry:");
                pw.println("   %test = icmp slt i32 %a, %b");
                pw.println("   br i1 %test, label %aissmaller, label %notsmaller");
                pw.println("aissmaller:");
                pw.println("   ret i32 %a");
                pw.println("notsmaller:");
                pw.println("   ret i32 %b");
                pw.println("}");
                pw.println();

                // Method type
                pw.println("define internal i32 @checkmethodtype(i32 %methodType, i32 %index, i32 %expectedType) alwaysinline {");
                pw.println("entry:");
                pw.println("    %offset = mul i32 %index, 4");
                pw.println("    %ptr1 = add i32 %index, 20");
                pw.println("    %ptr2 = add i32 %index, %methodType");
                pw.println("    %ptr = inttoptr i32 %ptr2 to i32*");
                pw.println("    %type = load i32, i32* %ptr");
                pw.println("    %cmp = icmp eq i32 %type, %expectedType");
                pw.println("    br i1 %cmp, label %equals, label %notequals");
                pw.println("equals:");
                pw.println("    ret i32 1");
                pw.println("notequals:");
                pw.println("    ret i32 0");
                pw.println("}");

                // Lambda
                pw.println("define internal i32 @newlambda(i32 %a, i32 %b, i32 %c) {");
                pw.println("entry:");
                // TODO: implement this
                pw.println("    unreachable");
                pw.println("}");

                // Some utility functions for runtime class management
                pw.println("define internal i32 @runtimeClass__resolvevtableindex(i32 %thisRef,i32 %methodId) {");
                pw.println("entry:");
                // TODO: implement this
                pw.println("    unreachable");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @jlClass_jlStringgetName(i32 %thisRef) {");
                pw.println("entry:");
                // TODO: implement this
                pw.println("    ret i32 0");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @jlClass_BOOLEANdesiredAssertionStatus(i32 %thisRef) {");
                pw.println("entry:");
                // TODO: implement this
                pw.println("    ret i32 0");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @jlClass_A1jlObjectgetEnumConstants(i32 %thisRef) {");
                pw.println("entry:");
                // TODO: implement this
                pw.println("    ret i32 0");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @jlClass_jlClassLoadergetClassLoader(i32 %thisRef) {");
                pw.println("entry:");
                // TODO: implement this
                pw.println("    ret i32 0");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @jlClass_jlClassforNamejlStringBOOLEANjlClassLoader(i32 %thisRef, i32 %name, i32 %initialize, i32 %classloader) {");
                pw.println("entry:");
                // TODO: implement this
                pw.println("    ret i32 0");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @newRuntimeClass(i32 %type, i32 %staticSize, i32 %enumValuesOffset, i32 %nameStringPoolIndex) {");
                pw.println("entry:");
                pw.println("    %vtableptr = ptrtoint i32(i32,i32)* @runtimeClass__resolvevtableindex to i32");
                pw.println("    %allocated = call i32 @dmbcMemoryManager_INTnewObjectINTINTINT(i32 0, i32 %staticSize, i32 -1, i32 %vtableptr)");
                pw.println("    %enumpos = add i32 %allocated, 12");
                pw.println("    %enumpos_ptr = inttoptr i32 %enumpos to i32*");
                pw.println("    store i32 %enumValuesOffset, i32* %enumpos_ptr");
                pw.println("    %namepos = add i32 %allocated, 16");
                pw.println("    %namepos_ptr = inttoptr i32 %namepos to i32*");
                pw.println("    store i32 %nameStringPoolIndex, i32* %namepos_ptr");
                pw.println("    %typepos = add i32 %allocated, 20");
                pw.println("    %typepos_ptr = inttoptr i32 %typepos to i32*");
                pw.println("    store i32 %type, i32* %typepos_ptr");
                pw.println("    ret i32 %allocated");
                pw.println("}");
                pw.println();

                // Now, we can continue to write implementation code
                aLinkerContext.linkedClasses().forEach(aEntry -> {

                    final BytecodeLinkedClass theLinkedClass = aEntry.targetNode();

                    if (!LLVMWriterUtils.filteredForTest(theLinkedClass)) {
                        return;
                    }

                    if (Objects.equals(aEntry.targetNode().getClassName(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                        return;
                    }

                    if (theLinkedClass.emulatedByRuntime()) {
                        return;
                    }

                    final BytecodeResolvedMethods theMethodMap = theLinkedClass.resolvedMethods();
                    final String theClassName = LLVMWriterUtils.toClassName(aEntry.targetNode().getClassName());

                    pw.print("@");
                    pw.print(theClassName);
                    pw.print(LLVMWriter.RUNTIMECLASSSUFFIX);
                    pw.println(" = private global i32 0");
                    pw.println();

                    if (!Objects.equals(theLinkedClass.getClassName(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                        pw.print("define i32 @");
                        pw.print(theClassName);
                        pw.print(LLVMWriter.CLASSINITSUFFIX);
                        pw.println("() {");
                        pw.println("entry:");
                        pw.print("    %ptr = ptrtoint i32* @");
                        pw.print(theClassName);
                        pw.print(LLVMWriter.RUNTIMECLASSSUFFIX);
                        pw.println(" to i32");
                        pw.println("    %status = add i32 %ptr, 8");
                        pw.println("    %statusptr = inttoptr i32 %status to i32*");
                        pw.println("    %value = load i32, i32* %statusptr");
                        pw.println("    %initialized_compare = icmp eq i32 %value, 1");
                        pw.println("    br i1 %initialized_compare, label %done, label %unitialized");
                        pw.println("unitialized:");
                        pw.println("    store i32 1,i32* %statusptr");

                        // Call superclass init
                        if (!theLinkedClass.getClassName().name().equals(Object.class.getName())) {
                            final BytecodeLinkedClass theSuper = theLinkedClass.getSuperClass();
                            if (LLVMWriterUtils.filteredForTest(theSuper)) {
                                final String theSuperWASMName = LLVMWriterUtils.toClassName(theSuper.getClassName());
                                pw.print("    call i32() @");
                                pw.print(theSuperWASMName);
                                pw.print(LLVMWriter.CLASSINITSUFFIX);
                                pw.println("()");
                            }
                        }

                        // Call class initializer
                        if (theLinkedClass.hasClassInitializer()) {
                            pw.print("    call void(i32) @");
                            pw.print(theClassName);
                            pw.println("_VOID$clinit$(i32 %ptr)");
                        }

                        pw.println("    br label %done");
                        pw.println("done:");
                        pw.println("    ret i32 %ptr");

                        pw.println("}");
                        pw.println();
                    }

                    if (!theLinkedClass.getBytecodeClass().getAccessFlags().isInterface() && !theLinkedClass.getBytecodeClass().getAccessFlags().isAbstract()) {
                        pw.print("define internal i1 @");
                        pw.print(theClassName);
                        pw.print(LLVMWriter.INSTANCEOFSUFFIX);
                        pw.println("(i32 %thisRef,i32 %typeId) {");
                        pw.println("entry:");
                        pw.println("    switch i32 %typeId, label %default [");
                        for (final BytecodeLinkedClass theType : theLinkedClass.getImplementingTypes()) {
                            pw.print("        i32 ");
                            pw.print(theType.getUniqueId());
                            pw.println(",label %true");
                        }
                        pw.println("    ]");
                        pw.println("default:");
                        pw.println("    ret i1 0");
                        pw.println("true:");
                        pw.println("    ret i1 1");
                        pw.println("}");
                        pw.println();

                        final List<BytecodeResolvedMethods.MethodEntry> thevTable = new ArrayList<>();
                        final List<BytecodeResolvedMethods.MethodEntry> theEntries = theMethodMap.stream().collect(
                                Collectors.toList());
                        final Set<BytecodeVirtualMethodIdentifier> theVisitedMethods = new HashSet<>();
                        for (int i = theEntries.size() - 1; 0 <= i; i--) {
                            final BytecodeResolvedMethods.MethodEntry aMethodMapEntry = theEntries.get(i);
                            final BytecodeMethod theMethod = aMethodMapEntry.getValue();

                            if (!theMethod.getAccessFlags().isStatic() &&
                                    !theMethod.isConstructor() &&
                                    !theMethod.getAccessFlags().isAbstract() &&
                                    !"desiredAssertionStatus".equals(theMethod.getName().stringValue()) &&
                                    !"getEnumConstants".equals(theMethod.getName().stringValue()) &&
                                    theMethod.getAttributes().getAnnotationByType(Substitutes.class.getName()) == null) {

                                final BytecodeVirtualMethodIdentifier theMethodIdentifier = aLinkerContext.getMethodCollection()
                                        .identifierFor(theMethod);

                                if (theVisitedMethods.add(theMethodIdentifier)) {
                                    thevTable.add(aMethodMapEntry);
                                }
                            }
                        }

                        pw.print("define internal i32 @");
                        pw.print(theClassName);
                        pw.print(LLVMWriter.VTABLEFUNCTIONSUFFIX);
                        pw.println("(i32 %thisRef,i32 %methodId) {");
                        pw.println("entry:");
                        pw.println("    switch i32 %methodId, label %default [");
                        pw.print("        i32 ");
                        pw.print(LLVMWriter.GENERATED_INSTANCEOF_METHOD_ID);
                        pw.println(",label %instanceof");

                        for (final BytecodeResolvedMethods.MethodEntry entry : thevTable) {
                            final BytecodeMethod theMethod = entry.getValue();
                            final BytecodeVirtualMethodIdentifier theMethodIdentifier = aLinkerContext.getMethodCollection()
                                    .identifierFor(theMethod);

                            if (LLVMWriterUtils.filteredForTest(entry.getProvidingClass())) {
                                pw.print("        i32 ");
                                pw.print(theMethodIdentifier.getIdentifier());
                                pw.print(",label %v_table_");
                                pw.println(theMethodIdentifier.getIdentifier());
                            }
                        }

                        pw.println("    ]");
                        pw.println("default:");
                        pw.println("    call void @llvm.trap()");
                        pw.println("    unreachable");

                        for (final BytecodeResolvedMethods.MethodEntry methodEntry : thevTable) {
                            final BytecodeMethod theMethod = methodEntry.getValue();
                            final BytecodeVirtualMethodIdentifier theMethodIdentifier = aLinkerContext.getMethodCollection()
                                    .identifierFor(theMethod);

                            if (LLVMWriterUtils.filteredForTest(methodEntry.getProvidingClass())) {
                                pw.print("v_table_");
                                pw.print(theMethodIdentifier.getIdentifier());
                                pw.println(":");
                                pw.print("    %ptr_");
                                pw.print(theMethodIdentifier.getIdentifier());
                                pw.print(" = ptrtoint ");
                                pw.print(LLVMWriterUtils.toSignature(theMethod.getSignature()));
                                pw.print("* @");
                                pw.print(LLVMWriterUtils.toMethodName(methodEntry.getProvidingClass().getClassName(), theMethod.getName(), theMethod.getSignature()));
                                pw.println(" to i32");
                                pw.print("    ret i32 %ptr_");
                                pw.print(theMethodIdentifier.getIdentifier());
                                pw.println();
                            }
                        }

                        pw.println("instanceof:");
                        pw.print("    %iofptr = ptrtoint i1(i32,i32)* @");
                        pw.print(theClassName);
                        pw.print(LLVMWriter.INSTANCEOFSUFFIX);
                        pw.println(" to i32");
                        pw.println("    ret i32 %iofptr");
                        pw.println("}");
                        pw.println();
                    }

                    theMethodMap.stream().forEach(aMethodMapEntry -> {

                        final BytecodeMethod theMethod = aMethodMapEntry.getValue();
                        final BytecodeMethodSignature theSignature = theMethod.getSignature();

                        // If the method is provided by the runtime, we do not need to generate the implementation
                        if (null != theMethod.getAttributes().getAnnotationByType(EmulatedByRuntime.class.getName())) {
                            return;
                        }

                        // Do not generate code for abstract methods
                        if (theMethod.getAccessFlags().isAbstract()) {
                            return;
                        }

                        if (theMethod.getAccessFlags().isNative()) {
                            // Already written
                            return;
                        }

                        if (!(aMethodMapEntry.getProvidingClass() == theLinkedClass)) {
                            // Skip methods not implemented here
                            // Skip methods not implemented in this class
                            // But include static methods, as they are inherited from the base classes
                            if (theMethod.getAccessFlags().isStatic() && !theMethod.isClassInitializer()) {
                                // We need to create a delegate function here
                                if (!theMethodMap.isImplementedBy(theMethod, theLinkedClass)) {
                                    final String theMethodName = LLVMWriterUtils.toMethodName(aEntry.targetNode().getClassName(), theMethod.getName().stringValue(), theMethod.getSignature());
                                    pw.print("define internal ");
                                    pw.print(LLVMWriterUtils.toType(TypeRef.toType(theMethod.getSignature().getReturnType())));
                                    pw.print(" @");
                                    pw.print(theMethodName);
                                    pw.print("(i32 %runtimeClass");
                                    for (int i=0;i<theMethod.getSignature().getArguments().length;i++) {
                                        pw.print(",");
                                        pw.print(LLVMWriterUtils.toType(TypeRef.toType(theMethod.getSignature().getArguments()[i])));
                                        pw.print(" %p");
                                        pw.print(i);
                                    }
                                    pw.println(") {");
                                    pw.println("entry:");

                                    if (theMethod.getSignature().getReturnType().isVoid()) {
                                        pw.print("    call void @");
                                        pw.print(LLVMWriterUtils.toMethodName(aMethodMapEntry.getProvidingClass().getClassName(), theMethod.getName().stringValue(), theMethod.getSignature()));
                                        pw.print("(i32 %runtimeClass");
                                        for (int i=0;i<theMethod.getSignature().getArguments().length;i++) {
                                            pw.print(",");
                                            pw.print(LLVMWriterUtils.toType(TypeRef.toType(theMethod.getSignature().getArguments()[i])));
                                            pw.print(" %p");
                                            pw.print(i);
                                        }
                                        pw.println(")");
                                        pw.println("    ret void");
                                    } else {
                                        pw.print("    %temp = call ");
                                        pw.print(LLVMWriterUtils.toType(TypeRef.toType(theMethod.getSignature().getReturnType())));
                                        pw.print(" @");
                                        pw.print(LLVMWriterUtils.toMethodName(aMethodMapEntry.getProvidingClass().getClassName(), theMethod.getName().stringValue(), theMethod.getSignature()));
                                        pw.print("(i32 %runtimeClass");
                                        for (int i=0;i<theMethod.getSignature().getArguments().length;i++) {
                                            pw.print(",");
                                            pw.print(LLVMWriterUtils.toType(TypeRef.toType(theMethod.getSignature().getArguments()[i])));
                                            pw.print(" %p");
                                            pw.print(i);
                                        }
                                        pw.println(")");
                                        pw.print("    ret ");
                                        pw.print(LLVMWriterUtils.toType(TypeRef.toType(theMethod.getSignature().getReturnType())));
                                        pw.println(" %temp");
                                    }

                                    pw.println("}");
                                    pw.println();
                                }
                            }
                            return;
                        }

                        // We need to create a newInstance function in case this is a constructor
                        if (theMethod.isConstructor() && !theLinkedClass.getBytecodeClass().getAccessFlags().isAbstract() && !theLinkedClass.getBytecodeClass().getAccessFlags().isInterface()) {

                            pw.print("attributes #");
                            pw.print(attributeCounter.get());
                            pw.print(" = {");
                            pw.print("\"wasm-export-name\"");
                            pw.print("=");
                            pw.print("\"");
                            pw.print(LLVMWriterUtils.toMethodName(theLinkedClass.getClassName(), LLVMWriter.NEWINSTANCE_METHOD_NAME, theMethod.getSignature()));
                            pw.println("\"}");
                            attributeCounter.incrementAndGet();

                            pw.print("define i32 @");
                            pw.print(LLVMWriterUtils.toMethodName(theLinkedClass.getClassName(), LLVMWriter.NEWINSTANCE_METHOD_NAME, theMethod.getSignature()));
                            pw.print("(");
                            for (int i=0;i<theMethod.getSignature().getArguments().length;i++) {
                                if (i>0) {
                                    pw.print(",");
                                }
                                pw.print(LLVMWriterUtils.toType(TypeRef.toType(theMethod.getSignature().getArguments()[i])));
                                pw.print(" %p");
                                pw.print(i);
                            }
                            pw.print(") #");
                            pw.print(attributeCounter.get());
                            attributeCounter.incrementAndGet();
                            pw.println(" {");
                            pw.println("entry:");
                            pw.print("    call i32 @");
                            pw.print(theClassName);
                            pw.print(LLVMWriter.CLASSINITSUFFIX);
                            pw.println("()");

                            pw.print("    %vtableptr = ptrtoint i32(i32,i32)* @");
                            pw.print(theClassName);
                            pw.print(LLVMWriter.VTABLEFUNCTIONSUFFIX);
                            pw.println(" to i32");
                            pw.print("    %allocated = call i32(i32,i32,i32,i32) @");
                            pw.print(LLVMWriterUtils.toMethodName(theMemoryManagerClass.getClassName(), "newObject",
                                    new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT})));
                            pw.print("(");
                            pw.print("i32 0,");

                            final NativeMemoryLayouter.MemoryLayout theLayout = memoryLayouter.layoutFor(theLinkedClass.getClassName());
                            pw.print("i32 ");
                            pw.print(theLayout.instanceSize());
                            pw.print(",");
                            pw.print("i32 ");
                            pw.print(theLinkedClass.getUniqueId());
                            pw.print(",i32 %vtableptr");
                            pw.println(")");

                            pw.print("    call ");
                            pw.print(LLVMWriterUtils.toSignature(theMethod.getSignature()));
                            pw.print(" @");
                            pw.print(LLVMWriterUtils.toMethodName(theLinkedClass.getClassName(), theMethod.getName(), theMethod.getSignature()));
                            pw.print("(i32 %allocated");
                            for (int i=0;i<theMethod.getSignature().getArguments().length;i++) {
                                pw.print(",");
                                pw.print(LLVMWriterUtils.toType(TypeRef.toType(theMethod.getSignature().getArguments()[i])));
                                pw.print(" %p");
                                pw.print(i);
                            }
                            pw.println(")");

                            pw.println("    ret i32 %allocated");
                            pw.println("}");
                            pw.println();
                        }

                        final ProgramGenerator theGenerator = programGeneratorFactory.createFor(aLinkerContext, new LLVMIntrinsics());
                        final Program theSSAProgram = theGenerator.generateFrom(aMethodMapEntry.getProvidingClass().getBytecodeClass(), theMethod);

                        // Run optimizer
                        // We use a special LLVM optimizer, which does only stuff LLVM CANNOT do, such
                        // as virtual method invocation optimization. All other optimization work
                        // is done by LLVM!
                        KnownOptimizer.LLVM.optimize(theSSAProgram.getControlFlowGraph(), aLinkerContext);

                        // Now, we can generate the instance method here
                        final String methodName = LLVMWriterUtils
                                .toMethodName(theLinkedClass.getClassName(), theMethod.getName(), theSignature);

                        final List<String> attributes = new ArrayList<>();
                        final BytecodeAnnotation theExport = theMethod.getAttributes().getAnnotationByType(Export.class.getName());
                        if (theExport != null) {
                            pw.print("attributes #");
                            pw.print(attributeCounter.get());
                            pw.print(" = {");
                            pw.print("\"wasm-export-name\"");
                            pw.print("=");
                            pw.print("\"");
                            pw.print(theExport.getElementValueByName("value").stringValue());
                            pw.println("\"}");

                            final int theAttribute = attributeCounter.getAndIncrement();
                            attributes.add("#" + theAttribute);
                        }

                        pw.print("define ");
                        if (attributes.isEmpty()) {
                            pw.print("internal ");
                        }
                        pw.print(LLVMWriterUtils.toType(TypeRef.toType(theSignature.getReturnType())));
                        pw.print(" @");
                        pw.print(methodName);
                        pw.print("(");
                        if (theMethod.getAccessFlags().isStatic()) {
                            pw.print(LLVMWriterUtils.toType(TypeRef.Native.REFERENCE));
                            pw.print(" ");
                            pw.print("%runtimeClass");
                        }
                        final List<Variable> theArguments = theSSAProgram.getArguments();
                        for (int i=0;i<theArguments.size();i++) {
                            final Variable theArgument = theArguments.get(i);
                            final TypeRef theParamType = theArgument.resolveType();
                            if (i == 0 && theMethod.getAccessFlags().isStatic()) {
                                pw.print(",");
                            }
                            if (i > 0) {
                                pw.print(",");
                            }
                            pw.print(LLVMWriterUtils.toType(theParamType));
                            pw.print(" ");
                            pw.print("%");
                            pw.print(theArgument.getName());
                            pw.print("_");
                        }

                        if (!attributes.isEmpty()) {
                            pw.print(")");
                            for (final String attr : attributes) {
                                pw.print(" ");
                                pw.print(attr);
                            }
                            pw.print(" {");
                        } else {
                            pw.println(") {");
                        }

                        try (final LLVMWriter theWriter = new LLVMWriter(pw, memoryLayouter, aLinkerContext, theSymbolResolver)) {
                            theWriter.write(theSSAProgram);
                        }

                        pw.println("}");
                        pw.println();

                        // Export main entry point
                        if (theLinkedClass.getClassName().name().equals(aEntryPointClass.getName()) && theMethod.getName().stringValue().equals(aEntryPointMethodName) && theMethod.getSignature().matchesExactlyTo(aEntryPointSignatue)) {

                            pw.print("attributes #");
                            pw.print(attributeCounter.get());
                            pw.print(" = {");
                            pw.print("\"wasm-export-name\"");
                            pw.print("=");
                            pw.println("\"main\"}");

                            final int theAttribute = attributeCounter.getAndIncrement();

                            pw.print("define ");
                            pw.print(LLVMWriterUtils.toType(TypeRef.toType(theSignature.getReturnType())));
                            pw.print(" @");
                            pw.print(methodName);
                            pw.print("_export_delegate (");
                            if (theMethod.getAccessFlags().isStatic()) {
                                pw.print(LLVMWriterUtils.toType(TypeRef.Native.REFERENCE));
                                pw.print(" ");
                                pw.print("%runtimeClass");
                            }
                            for (int i=0;i<theArguments.size();i++) {
                                final Variable theArgument = theArguments.get(i);
                                final TypeRef theParamType = theArgument.resolveType();
                                if (i == 0 && theMethod.getAccessFlags().isStatic()) {
                                    pw.print(",");
                                }
                                if (i > 0) {
                                    pw.print(",");
                                }
                                pw.print(LLVMWriterUtils.toType(theParamType));
                                pw.print(" ");
                                pw.print("%");
                                pw.print(theArgument.getName());
                                pw.print("_");
                            }

                            pw.print(") #");
                            pw.print(theAttribute);
                            pw.println(" {");
                            pw.println("entry:");
                            if (theMethod.getSignature().getReturnType().isVoid()) {
                                pw.print("    call ");
                            } else {
                                pw.print("    %value = call ");
                            }

                            pw.print(LLVMWriterUtils.toSignature(theSignature));
                            pw.print(" @");
                            pw.print(methodName);
                            pw.print("(");
                            if (theMethod.getAccessFlags().isStatic()) {
                                pw.print(LLVMWriterUtils.toType(TypeRef.Native.REFERENCE));
                                pw.print(" ");
                                pw.print("%runtimeClass");
                            }
                            for (int i=0;i<theArguments.size();i++) {
                                final Variable theArgument = theArguments.get(i);
                                final TypeRef theParamType = theArgument.resolveType();
                                if (i == 0 && theMethod.getAccessFlags().isStatic()) {
                                    pw.print(",");
                                }
                                if (i > 0) {
                                    pw.print(",");
                                }
                                pw.print(LLVMWriterUtils.toType(theParamType));
                                pw.print(" ");
                                pw.print("%");
                                pw.print(theArgument.getName());
                                pw.print("_");
                            }
                            pw.println(")");

                            if (theMethod.getSignature().getReturnType().isVoid()) {
                                pw.println("    ret void");
                            } else {
                                pw.print("    ret ");
                                pw.print(LLVMWriterUtils.toType(TypeRef.toType(theSignature.getReturnType())));
                                pw.println(" %value");
                            }

                            pw.println("}");
                            pw.println();
                        }
                    });
                });

                // Generate callsite resolver code
                for (final Map.Entry<String, CallSite> theEntry : callsites.entrySet()) {
                    final CallSite callsite = theEntry.getValue();

                    pw.print("@");
                    pw.print("callsite");
                    pw.print(System.identityHashCode(callsite));
                    pw.println(" = private global i32 0");

                    pw.print(";; ");
                    pw.println(theEntry.getKey());

                    pw.print("define internal i32 @resolvecallsite");
                    pw.print(System.identityHashCode(callsite));
                    pw.println("() {");

                    // TODO: implement logic here
                    pw.println("entry:");
                    pw.println("    call void @llvm.trap()");
                    pw.println("    unreachable");

                    pw.println("}");
                    pw.println();
                }

                // New Instance helper for reflection stuff
                pw.print("define internal i32 @");
                pw.print(LLVMWriter.NEWINSTANCEHELPER);
                pw.println("(i32 %runtimeclass) {");
                pw.println("entry:");

                aLinkerContext.linkedClasses().map(Edge::targetNode).forEach(search -> {

                    // Hack for unit-testing
                    if (!LLVMWriterUtils.filteredForTest(search)) {
                        return;
                    }

                    if (!search.getBytecodeClass().getAccessFlags().isAbstract() && !search.getBytecodeClass().getAccessFlags()
                            .isInterface() && !search.emulatedByRuntime()) {

                        final String theClassName = LLVMWriterUtils.toClassName(search.getClassName());

                        // Only if the class has a zero arg constructor
                        final BytecodeResolvedMethods theResolved = search.resolvedMethods();
                        theResolved.stream().filter(j -> j.getProvidingClass() == search).map(BytecodeResolvedMethods.MethodEntry::getValue)
                                .filter(j -> j.isConstructor() && j.getSignature().getArguments().length == 0).forEach(m -> {

                            // We found a zero arg constructor
                            pw.print("    %runtimeclass_");
                            pw.print(search.getUniqueId());
                            pw.print(" = load i32, i32* @");
                            pw.print(theClassName);
                            pw.println(LLVMWriter.RUNTIMECLASSSUFFIX);
                            pw.print("    %runtimeclass_");
                            pw.print(search.getUniqueId());
                            pw.print("_check = icmp eq i32 %runtimeclass, %runtimeclass_");
                            pw.println(search.getUniqueId());
                            pw.print("    br i1 %runtimeclass_");
                            pw.print(search.getUniqueId());
                            pw.print("_check, label %checktrue_");
                            pw.print(search.getUniqueId());
                            pw.print(", label %checkfalse_");
                            pw.println(search.getUniqueId());
                            pw.print("checktrue_");
                            pw.print(search.getUniqueId());
                            pw.println(":");

                            pw.print("    %newinstance_");
                            pw.print(search.getUniqueId());
                            pw.print(" = call i32 @");
                            pw.print(LLVMWriterUtils.toMethodName(search.getClassName(), "$newInstance", m.getSignature()));
                            pw.println("()");

                            pw.print("    ret i32 %newinstance_");
                            pw.println(search.getUniqueId());

                            pw.print("checkfalse_");
                            pw.print(search.getUniqueId());
                            pw.println(":");
                        });
                    }
                });

                pw.println("    call void @llvm.trap()");
                pw.println("    unreachable");
                pw.println("}");
                pw.println();

                // We create the string pool now
                for (int i=0;i<stringPool.size();i++) {
                    pw.print("@");
                    pw.print("strpool_");
                    pw.print(i);
                    pw.println(" = private global i32 0");
                }
                pw.println();

                // Generate bootstrap code
                attributeCounter.incrementAndGet();
                pw.print("attributes #");
                pw.print(attributeCounter.get());
                pw.println(" = { \"wasm-export-name\"=\"bootstrap\" }");
                pw.print("define void @bootstrap() #");
                pw.print(attributeCounter.get());
                pw.println(" {");
                pw.println("entry:");

                aLinkerContext.linkedClasses().forEach(aEntry -> {
                    final BytecodeLinkedClass theLinkedClass = aEntry.targetNode();

                    if (!LLVMWriterUtils.filteredForTest(theLinkedClass)) {
                        return;
                    }

                    if (Objects.equals(aEntry.targetNode().getClassName(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                        return;
                    }
                    if (theLinkedClass.emulatedByRuntime()) {
                        return;
                    }

                    final String theClassName = LLVMWriterUtils.toClassName(aEntry.targetNode().getClassName());

                    final NativeMemoryLayouter.MemoryLayout theMemoryLayout = memoryLayouter.layoutFor(theLinkedClass.getClassName());

                    pw.print("    %");
                    pw.print(theClassName);
                    pw.print(LLVMWriter.RUNTIMECLASSSUFFIX);
                    pw.print("_allocated = call i32(i32,i32,i32,i32) @newRuntimeClass(i32 ");
                    pw.print(theLinkedClass.getUniqueId());
                    pw.print(",i32 ");
                    pw.print(theMemoryLayout.classSize());
                    pw.print(",i32 ");
                    final BytecodeResolvedFields theStaticFields = theLinkedClass.resolvedFields();
                    if (null != theStaticFields.fieldByName("$VALUES")) {
                        pw.print(theMemoryLayout.offsetForClassMember("$VALUES"));
                    } else {
                        pw.print("-1");
                    }
                    // TODO: reference to class name as constant pool here
                    pw.print(",i32 0");
                    pw.println(")");

                    pw.print("    store i32 %");
                    pw.print(theClassName);
                    pw.print(LLVMWriter.RUNTIMECLASSSUFFIX);
                    pw.print("_allocated, i32* @");
                    pw.print(theClassName);
                    pw.println(LLVMWriter.RUNTIMECLASSSUFFIX);
                });

                    pw.println("    ret void");
                pw.println("}");
                pw.println();
            }

            try (final Reader reader = new InputStreamReader(new FileInputStream(theLLFile))) {
                final String theLLContent = IOUtils.toString(reader);
                theCompileResult.add(new CompileResult.StringContent(aOptions.getFilenamePrefix() + ".ll", theLLContent));
            }

            // Compile LLVM Assembly File to object file
            final List<String> theLLCommand = new ArrayList<>();
            if ("\\".equals(File.separator)) {
                // We are running on windows
                // llvm needs to be installed in the Windows Subsystem for Linux
                theLLCommand.add("wsl");
            }
            final String theObjectFileName = theLLFile.getName() + ".o";
            theLLCommand.add("llc-10");
            theLLCommand.add("-O3");
            theLLCommand.add("--stats");
            theLLCommand.add("--time-passes");
            theLLCommand.add("-filetype=obj");
            theLLCommand.add(theLLFile.getName());
            theLLCommand.add("-o");
            theLLCommand.add(theObjectFileName);
            final ProcessBuilder theLLCProcessBuilder = new ProcessBuilder(theLLCommand).directory(theLLFile.getParentFile()).inheritIO();
            aOptions.getLogger().info("LLVM compiler command is {}", theLLCProcessBuilder.command());
            final Process theLLCProcess = theLLCProcessBuilder.start();
            if (theLLCProcess.waitFor() != 0) {
                aOptions.getLogger().warn("llc returned with exit code {}", theLLCProcess.exitValue());

                try (final BufferedReader processOutput = new BufferedReader(new InputStreamReader(theLLCProcess.getErrorStream()))) {
                    String line;
                    while ((line = processOutput.readLine()) != null) {
                        System.out.println(line);
                    }
                }
            } else {

                try (final FileInputStream inputStream = new FileInputStream(new File(theLLFile.getParent(), theObjectFileName))) {
                    theCompileResult.add(new CompileResult.BinaryContent(aOptions.getFilenamePrefix() + ".o",
                            IOUtils.toByteArray(inputStream)));
                }
            }

            // Link object file to wasm binary
            final List<String> theLinkerCommand = new ArrayList<>();
            if ("\\".equals(File.separator)) {
                // We are running on windows
                // llvm needs to be installed in the Windows Subsystem for Linux
                theLinkerCommand.add("wsl");
            }
            final String theWASMFileName = theLLFile.getName() + ".wasm";
            theLinkerCommand.add("wasm-ld-10");
            theLinkerCommand.add(theObjectFileName);
            theLinkerCommand.add("-o");
            theLinkerCommand.add(theWASMFileName);
            theLinkerCommand.add("-export-dynamic");
            theLinkerCommand.add("-allow-undefined");
            theLinkerCommand.add("--lto-O3");
            theLinkerCommand.add("--no-entry");
            theLinkerCommand.add("--initial-memory=" + aOptions.getWasmMinimumPageSize() * 65536);
            theLinkerCommand.add("--max-memory=" + aOptions.getWasmMaximumPageSize() * 65536);
            if (!aOptions.isDebugOutput()) {
                theLinkerCommand.add("-s");
            }
            final ProcessBuilder theLinkerProcessBuilder = new ProcessBuilder(theLinkerCommand).directory(theLLFile.getParentFile());
            aOptions.getLogger().info("LLVM linker command is {}", theLinkerProcessBuilder.command());

            final Process theLinkerProcess = theLinkerProcessBuilder.start();
            if (theLinkerProcess.waitFor() != 0) {
                aOptions.getLogger().warn("wasm-ld returned with exit code {} ", theLinkerProcess.exitValue());
                try (final BufferedReader processOutput = new BufferedReader(
                        new InputStreamReader(theLinkerProcess.getErrorStream()))) {
                    String line;
                    while ((line = processOutput.readLine()) != null) {
                        System.out.println(line);
                    }
                }
            } else {
                try (final FileInputStream inputStream = new FileInputStream(
                        new File(theLLFile.getParent(), theWASMFileName))) {
                    theCompileResult.add(new CompileResult.BinaryContent(aOptions.getFilenamePrefix() + ".wasm",
                            IOUtils.toByteArray(inputStream)));
                }
            }

            return theCompileResult;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
