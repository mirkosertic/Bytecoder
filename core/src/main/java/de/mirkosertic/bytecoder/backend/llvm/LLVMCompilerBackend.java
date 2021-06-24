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
import de.mirkosertic.bytecoder.api.OpaqueIndexed;
import de.mirkosertic.bytecoder.api.OpaqueMethod;
import de.mirkosertic.bytecoder.api.OpaqueProperty;
import de.mirkosertic.bytecoder.api.Substitutes;
import de.mirkosertic.bytecoder.backend.CompileBackend;
import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.CompileResult;
import de.mirkosertic.bytecoder.backend.NativeMemoryLayouter;
import de.mirkosertic.bytecoder.classlib.Address;
import de.mirkosertic.bytecoder.classlib.Array;
import de.mirkosertic.bytecoder.classlib.MemoryManager;
import de.mirkosertic.bytecoder.classlib.VM;
import de.mirkosertic.bytecoder.classlib.java.util.Quicksort;
import de.mirkosertic.bytecoder.core.BytecodeAccessFlags;
import de.mirkosertic.bytecoder.core.BytecodeAnnotation;
import de.mirkosertic.bytecoder.core.BytecodeAttributeInfo;
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
import de.mirkosertic.bytecoder.core.BytecodeUtf8Constant;
import de.mirkosertic.bytecoder.core.BytecodeVTable;
import de.mirkosertic.bytecoder.core.BytecodeVirtualMethodIdentifier;
import de.mirkosertic.bytecoder.escapeanalysis.EscapeAnalysis;
import de.mirkosertic.bytecoder.optimizer.KnownOptimizer;
import de.mirkosertic.bytecoder.pointsto.PointsToAnalysis;
import de.mirkosertic.bytecoder.pointsto.PointsToAnalysisResult;
import de.mirkosertic.bytecoder.ssa.MethodHandleExpression;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramDescriptor;
import de.mirkosertic.bytecoder.ssa.ProgramDescriptorProvider;
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
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LLVMCompilerBackend implements CompileBackend<LLVMCompileResult> {

    private static class CallSite {
        private final BytecodeLinkedClass owningClass;
        private final Program program;
        private final RegionNode bootstrapMethod;

        private CallSite(final BytecodeLinkedClass aOwningClass, final Program aProgram, final RegionNode aBootstrapMethod) {
            this.owningClass = aOwningClass;
            this.program = aProgram;
            this.bootstrapMethod = aBootstrapMethod;
        }
    }

    private static class OpaqueReferenceMethod {

        private final BytecodeLinkedClass linkedClass;
        private final BytecodeMethod method;

        public OpaqueReferenceMethod(final BytecodeLinkedClass linkedClass, final BytecodeMethod method) {
            this.linkedClass = linkedClass;
            this.method = method;
        }

        public BytecodeLinkedClass getLinkedClass() {
            return linkedClass;
        }

        public BytecodeMethod getMethod() {
            return method;
        }
    }

    private static class CompiledMethod {

        private final BytecodeLinkedClass linkedClass;
        private final BytecodeMethod method;
        private final Program program;
        private final LLVMDebugInformation.SubProgram debugInformationSubProgram;

        public CompiledMethod(final BytecodeLinkedClass linkedClass, final BytecodeMethod method, final Program program, final LLVMDebugInformation.SubProgram debugInformationSubProgram) {
            this.linkedClass = linkedClass;
            this.method = method;
            this.program = program;
            this.debugInformationSubProgram = debugInformationSubProgram;
        }
    }

    private final ProgramGeneratorFactory programGeneratorFactory;

    public LLVMCompilerBackend(final ProgramGeneratorFactory aProgramGeneratorFactory) {
        this.programGeneratorFactory = aProgramGeneratorFactory;
    }

    @Override
    public LLVMCompileResult generateCodeFor(final CompileOptions aOptions, final BytecodeLinkerContext aLinkerContext,
            final Class aEntryPointClass, final String aEntryPointMethodName, final BytecodeMethodSignature aEntryPointSignature) {

        final BytecodeLinkedClass theArrayClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Array.class));
        theArrayClass.tagWith(BytecodeLinkedClass.Tag.INSTANTIATED);
        theArrayClass.resolveConstructorInvocation(new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[0]));

        // We need to link the memory manager
        final BytecodeLinkedClass theMemoryManagerClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class));

        theMemoryManagerClass.resolveStaticMethod("logAllocations", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[0]));

        theMemoryManagerClass.resolveStaticMethod("freeMem", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[0]));
        theMemoryManagerClass.resolveStaticMethod("usedMem", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[0]));

        theMemoryManagerClass.resolveStaticMethod("free", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT}));
        theMemoryManagerClass.resolveStaticMethod("malloc", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT}));
        theMemoryManagerClass.resolveStaticMethod("newObject", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
        theMemoryManagerClass.resolveStaticMethod("newArray", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
        theMemoryManagerClass.resolveStaticMethod("newArray", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

        theMemoryManagerClass.resolveStaticMethod("initStackObject", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
        theMemoryManagerClass.resolveStaticMethod("initStackArray", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

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

        final LLVMCompileResult theCompileResult = new LLVMCompileResult();
        final List<OpaqueReferenceMethod> opaqueReferenceMethods = new ArrayList<>();
        final List<CompiledMethod> compiledMethods = new ArrayList<>();

        aLinkerContext.finalizeLinkage();

        try {
            final List<String> stringPool = new ArrayList<>();
            final Map<String, CallSite> callsites = new HashMap<>();
            final List<BytecodeMethodSignature> theMethodTypes = new ArrayList<>();
            final List<MethodHandleExpression> methodHandles = new ArrayList<>();
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
                public String resolveCallsiteBootstrapFor(final BytecodeLinkedClass owningClass, final String callsiteId, final Program program, final RegionNode bootstrapMethod) {
                    CallSite callSite = callsites.get(callsiteId);
                    if (callSite == null) {
                        callSite = new CallSite(owningClass, program, bootstrapMethod);
                        callsites.put(callsiteId, callSite);
                    }
                    return "resolvecallsite" + System.identityHashCode(callSite);
                }

                @Override
                public String methodTypeFactoryNameFor(final BytecodeMethodSignature aSignature) {
                    for (int i=0;i<theMethodTypes.size();i++) {
                        if (aSignature.matchesExactlyTo(theMethodTypes.get(0))) {
                            return "methodtypefactory" + i;
                        }
                    }
                    theMethodTypes.add(aSignature);
                    return "methodtypefactory" + (theMethodTypes.size() - 1);
                }

                private final Map<BytecodeLinkedClass, BytecodeVTable> tablesCache = new HashMap<>();

                @Override
                public BytecodeVTable vtableFor(final BytecodeLinkedClass aClass) {
                    BytecodeVTable theTable = tablesCache.get(aClass);
                    if (theTable == null) {
                        theTable = aClass.resolveVTable();
                        tablesCache.put(aClass, theTable);
                    }
                    return theTable;
                }

                @Override
                public String methodHandleDelegateFor(final MethodHandleExpression e) {
                    final int pos = methodHandles.size();
                    methodHandles.add(e);
                    return "handle" + pos;
                }
            };
            final LLVMDebugInformation debugInformation = new LLVMDebugInformation();
            final NativeMemoryLayouter memoryLayouter = new NativeMemoryLayouter(aLinkerContext, 8);

            final File theTempDirectory = new File(new File("."), ".llvm");
            aOptions.getLogger().info("Using directory {} for temporary LLVM files", theTempDirectory);
            if (!theTempDirectory.exists()) {
                theTempDirectory.mkdirs();
            }
            final File theLLFile = new File(theTempDirectory, aEntryPointClass.getName() + aEntryPointMethodName + ".ll");

            try (final PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(theLLFile), StandardCharsets.UTF_8))) {
                // We write the header first
                pw.println("target triple = \"wasm32-unknown-unknown\"");
                pw.println("target datalayout = \"e-m:e-p:32:32-i64:64-n32:64-S128\"");
                pw.println();

                pw.println("@__heap_base = external global i32");
                pw.println("@__data_end = external global i32");
                pw.println("@__memory_base = external global i32");

                pw.println("@stacktop = global i32 0");
                pw.println();

                pw.println("declare i32 @llvm.wasm.memory.size.i32(i32) nounwind readonly");
                pw.println("declare void @llvm.trap() cold noreturn nounwind");
                pw.println("declare float @llvm.minimum.f32(float %Val0, float %Val1)");
                pw.println("declare double @llvm.minimum.f64(double %Val0, double %Val1)");
                pw.println("declare float @llvm.maximum.f32(float %Val0, float %Val1)");
                pw.println("declare double @llvm.maximum.f64(double %Val0, double %Val1)");
                pw.println("declare float @llvm.floor.f32(float %Val)");
                pw.println("declare double @llvm.floor.f64(double %Val)");
                pw.println("declare float @llvm.ceil.f32(float %Val)");
                pw.println("declare double @llvm.ceil.f64(double %Val)");
                pw.println("declare float @llvm.sqrt.f32(float %Val)");
                pw.println("declare double @llvm.sqrt.f64(double %Val)");
                pw.println();

                final AtomicInteger attributeCounter = new AtomicInteger();

                // We write the imported functions first
                aLinkerContext.linkedClasses().forEach(aEntry -> {

                    if (aEntry.getBytecodeClass().getAccessFlags().isInterface() && !aEntry.isOpaqueType()) {
                        return;
                    }
                    if (Objects.equals(aEntry.getClassName(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                        return;
                    }
                    if (Objects.equals(aEntry.getClassName(), BytecodeObjectTypeRef.fromRuntimeClass(java.lang.reflect.Array.class))) {
                        return;
                    }

                    final BytecodeResolvedMethods theMethodMap = aLinkerContext.resolveMethods(aEntry);
                    theMethodMap.stream().forEach(aMethodMapEntry -> {

                        final BytecodeLinkedClass theProvidingClass = aMethodMapEntry.getProvidingClass();

                        // Only add implementation methods
                        if (!(theProvidingClass == aEntry)) {
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
                                // For all the other methods we generate
                                // the JS wrapper implementation later by this compiler
                                opaqueReferenceMethods.add(new OpaqueReferenceMethod(theProvidingClass, t));
                            }

                            boolean needsAdapterMethod = false;
                            if (theSignature.getReturnType() == BytecodePrimitiveTypeRef.LONG) {
                                needsAdapterMethod = true;
                            }
                            for (final BytecodeTypeRef theTypeRef : t.getSignature().getArguments()) {
                                if (theTypeRef == BytecodePrimitiveTypeRef.LONG) {
                                    needsAdapterMethod = true;
                                    aLinkerContext.getLogger().warn("Possible pecision impact on Long datatype when calling imported method {}.{} with signature {}", theProvidingClass.getClassName().name(), t.getName().stringValue(), t.getSignature());
                                }
                            }

                            final BytecodeImportedLink theLink = theProvidingClass.linkFor(t);

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

                            final String returnType = LLVMWriterUtils.toType(TypeRef.toType(t.getSignature().getReturnType()));

                            if (needsAdapterMethod) {
                                // In this case, we expect a double as a return from JS
                                // side and convert it into i64
                                pw.print("declare ");
                                if (t.getSignature().getReturnType() == BytecodePrimitiveTypeRef.LONG) {
                                    pw.print("double");
                                } else {
                                    pw.print(returnType);
                                }
                                pw.print(" @");
                                pw.print(methodName);
                                pw.print("_adapter(");

                                pw.print(LLVMWriterUtils.toType(TypeRef.Native.REFERENCE));
                                pw.print(" ");
                                pw.print("%thisRef");

                                for (int i = 0; i < theSignature.getArguments().length; i++) {
                                    final BytecodeTypeRef theParamType = theSignature.getArguments()[i];
                                    if (theParamType == BytecodePrimitiveTypeRef.LONG) {
                                        pw.print(",double %p");
                                        pw.print(i + 1);
                                    } else {
                                        final String theTypeAsString = LLVMWriterUtils.toType(TypeRef.toType(theParamType));
                                        pw.print(",");
                                        pw.print(theTypeAsString);
                                        pw.print(" %p");
                                        pw.print(i + 1);
                                    }
                                }

                                pw.print(")");
                                pw.print(" #");
                                pw.print(attributeCounter);
                                pw.println();
                                pw.println();

                                pw.print("define internal ");
                                pw.print(returnType);
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

                                pw.println(") inlinehint {");

                                final StringBuilder theArguments = new StringBuilder("i32 %thisRef");
                                for (int i = 0; i < theSignature.getArguments().length; i++) {
                                    if (theSignature.getArguments()[i] == BytecodePrimitiveTypeRef.LONG) {
                                        pw.print("    %p");
                                        pw.print(i + 1);
                                        pw.print("_converted = sitofp i64 %p");
                                        pw.print(i + 1);
                                        pw.println(" to double");

                                        theArguments.append(",double %p");
                                        theArguments.append(i + 1);
                                        theArguments.append("_converted");
                                    } else {
                                        theArguments.append(",");
                                        theArguments.append(LLVMWriterUtils.toType(TypeRef.toType(theSignature.getArguments()[i])));
                                        theArguments.append(" %p");
                                        theArguments.append(i + 1);
                                    }
                                }

                                if (theSignature.getReturnType() != BytecodePrimitiveTypeRef.VOID) {
                                    pw.print("    %temp = call ");
                                    if (theSignature.getReturnType() == BytecodePrimitiveTypeRef.LONG) {
                                        pw.print("double");
                                    } else {
                                        pw.print(returnType);
                                    }
                                    pw.print(" @");
                                    pw.print(methodName);
                                } else {
                                    pw.print("    call void @");
                                    pw.print(methodName);
                                }
                                pw.print("_adapter(");
                                pw.print(theArguments);
                                pw.println(")");

                                if (theSignature.getReturnType() != BytecodePrimitiveTypeRef.VOID) {
                                    if (theSignature.getReturnType() == BytecodePrimitiveTypeRef.LONG) {
                                        pw.print("    %conv = fptosi double %temp to ");
                                        pw.println(returnType);

                                        pw.print("    ret ");
                                        pw.print(returnType);
                                        pw.println(" %conv");

                                    } else {
                                        pw.print("    ret ");
                                        pw.print(returnType);
                                        pw.println(" %temp");
                                    }
                                } else {
                                    pw.println("    ret void");
                                }

                                pw.println("}");
                                pw.println();
                            } else {
                                // No adapter code
                                pw.print("declare ");
                                pw.print(returnType);
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
                            }

                            attributeCounter.incrementAndGet();
                        }
                    });
                });

                // Some utility function
                final NativeMemoryLayouter.MemoryLayout theFieldMemoryLayout = memoryLayouter.layoutFor(BytecodeObjectTypeRef.fromRuntimeClass(Field.class));
                final int theOffsetOffset = theFieldMemoryLayout.offsetForInstanceMember("offset");
                pw.println("define internal i32 @bytecoder.getObjectFieldValueAsObject(i32 %object, i32 %field) {");
                pw.println("entry:");
                pw.print("    %offset = add i32 %field, ");
                pw.println(theOffsetOffset);
                pw.println("  %ptr = inttoptr i32 %offset to i32*");
                pw.println("  %offsetvalue = load i32, i32* %ptr");

                pw.println("  %data = add i32 %object, %offsetvalue");
                pw.println("  %dataptr = inttoptr i32 %data to i32*");
                pw.println("  %result = load i32, i32* %dataptr");
                pw.println("  ret i32 %result");
                pw.println("}");
                pw.println();

                pw.println("define internal void @bytecoder.putObjectFieldValueFromObject(i32 %object, i32 %field, i32 %value) {");
                pw.println("entry:");
                pw.print("    %offset = add i32 %field, ");
                pw.println(theOffsetOffset);
                pw.println("  %ptr = inttoptr i32 %offset to i32*");
                pw.println("  %offsetvalue = load i32, i32* %ptr");

                pw.println("  %data = add i32 %object, %offsetvalue");
                pw.println("  %dataptr = inttoptr i32 %data to i32*");
                pw.println("  store i32 %value, i32* %dataptr");
                pw.println("  ret void");
                pw.println("}");
                pw.println();

                // boolean
                pw.println("define internal i32 @bytecoder.getBooleanFieldValueAsObject(i32 %object, i32 %field) {");
                pw.println("entry:");
                pw.print("    %offset = add i32 %field, ");
                pw.println(theOffsetOffset);
                pw.println("  %ptr = inttoptr i32 %offset to i32*");
                pw.println("  %offsetvalue = load i32, i32* %ptr");

                pw.println("  %data = add i32 %object, %offsetvalue");
                pw.println("  %dataptr = inttoptr i32 %data to i32*");
                pw.println("  %result = load i32, i32* %dataptr");
                pw.println("  %class = call i32 @jlBoolean__init()");
                pw.println("  %converted = call i32 @jlBoolean_jlBooleanvalueOfBOOLEAN(i32 %class, i32 %result);");
                pw.println("  ret i32 %converted");
                pw.println("}");
                pw.println();

                pw.println("define internal void @bytecoder.putBooleanFieldValueFromObject(i32 %object, i32 %field, i32 %value) {");
                pw.println("entry:");
                pw.print("    %offset = add i32 %field, ");
                pw.println(theOffsetOffset);
                pw.println("  %ptr = inttoptr i32 %offset to i32*");
                pw.println("  %offsetvalue = load i32, i32* %ptr");

                pw.println("  %data = add i32 %object, %offsetvalue");
                pw.println("  %dataptr = inttoptr i32 %data to i32*");

                pw.println("  %class = call i32 @jlBoolean__init()");
                pw.println("  %converted = call i32 @jlBoolean_BOOLEANbooleanValue(i32 %value)");
                pw.println("  store i32 %converted, i32* %dataptr");
                pw.println("  ret void");
                pw.println("}");
                pw.println();

                // integer
                pw.println("define internal i32 @bytecoder.getIntegerFieldValueAsObject(i32 %object, i32 %field) {");
                pw.println("entry:");
                pw.print("    %offset = add i32 %field, ");
                pw.println(theOffsetOffset);
                pw.println("  %ptr = inttoptr i32 %offset to i32*");
                pw.println("  %offsetvalue = load i32, i32* %ptr");

                pw.println("  %data = add i32 %object, %offsetvalue");
                pw.println("  %dataptr = inttoptr i32 %data to i32*");
                pw.println("  %result = load i32, i32* %dataptr");
                pw.println("  %class = call i32 @jlInteger__init()");
                pw.println("  %converted = call i32 @jlInteger_jlIntegervalueOfINT(i32 %class, i32 %result);");
                pw.println("  ret i32 %converted");
                pw.println("}");
                pw.println();

                pw.println("define internal void @bytecoder.putIntegerFieldValueFromObject(i32 %object, i32 %field, i32 %value) {");
                pw.println("entry:");
                pw.print("    %offset = add i32 %field, ");
                pw.println(theOffsetOffset);
                pw.println("  %ptr = inttoptr i32 %offset to i32*");
                pw.println("  %offsetvalue = load i32, i32* %ptr");

                pw.println("  %data = add i32 %object, %offsetvalue");
                pw.println("  %dataptr = inttoptr i32 %data to i32*");

                pw.println("  %class = call i32 @jlInteger__init()");
                pw.println("  %converted = call i32 @jlInteger_INTintValue(i32 %value)");
                pw.println("  store i32 %converted, i32* %dataptr");
                pw.println("  ret void");
                pw.println("}");
                pw.println();

                // byte
                pw.println("define internal i32 @bytecoder.getByteFieldValueAsObject(i32 %object, i32 %field) {");
                pw.println("entry:");
                pw.print("    %offset = add i32 %field, ");
                pw.println(theOffsetOffset);
                pw.println("  %ptr = inttoptr i32 %offset to i32*");
                pw.println("  %offsetvalue = load i32, i32* %ptr");

                pw.println("  %data = add i32 %object, %offsetvalue");
                pw.println("  %dataptr = inttoptr i32 %data to i32*");
                pw.println("  %result = load i32, i32* %dataptr");
                pw.println("  %class = call i32 @jlByte__init()");
                pw.println("  %converted = call i32 @jlByte_jlBytevalueOfBYTE(i32 %class, i32 %result);");
                pw.println("  ret i32 %converted");
                pw.println("}");
                pw.println();

                pw.println("define internal void @bytecoder.putByteFieldValueFromObject(i32 %object, i32 %field, i32 %value) {");
                pw.println("entry:");
                pw.print("    %offset = add i32 %field, ");
                pw.println(theOffsetOffset);
                pw.println("  %ptr = inttoptr i32 %offset to i32*");
                pw.println("  %offsetvalue = load i32, i32* %ptr");

                pw.println("  %data = add i32 %object, %offsetvalue");
                pw.println("  %dataptr = inttoptr i32 %data to i32*");

                pw.println("  %class = call i32 @jlByte__init()");
                pw.println("  %converted = call i32 @jlByte_BYTEbyteValue(i32 %value)");
                pw.println("  store i32 %converted, i32* %dataptr");
                pw.println("  ret void");
                pw.println("}");
                pw.println();

                // char
                pw.println("define internal i32 @bytecoder.getCharFieldValueAsObject(i32 %object, i32 %field) {");
                pw.println("entry:");
                pw.print("    %offset = add i32 %field, ");
                pw.println(theOffsetOffset);
                pw.println("  %ptr = inttoptr i32 %offset to i32*");
                pw.println("  %offsetvalue = load i32, i32* %ptr");

                pw.println("  %data = add i32 %object, %offsetvalue");
                pw.println("  %dataptr = inttoptr i32 %data to i32*");
                pw.println("  %result = load i32, i32* %dataptr");
                pw.println("  %class = call i32 @jlCharacter__init()");
                pw.println("  %converted = call i32 @jlCharacter_jlCharactervalueOfCHAR(i32 %class, i32 %result);");
                pw.println("  ret i32 %converted");
                pw.println("}");
                pw.println();

                pw.println("define internal void @bytecoder.putCharFieldValueFromObject(i32 %object, i32 %field, i32 %value) {");
                pw.println("entry:");
                pw.print("    %offset = add i32 %field, ");
                pw.println(theOffsetOffset);
                pw.println("  %ptr = inttoptr i32 %offset to i32*");
                pw.println("  %offsetvalue = load i32, i32* %ptr");

                pw.println("  %data = add i32 %object, %offsetvalue");
                pw.println("  %dataptr = inttoptr i32 %data to i32*");

                pw.println("  %class = call i32 @jlCharacter__init()");
                pw.println("  %converted = call i32 @jlCharacter_CHARcharValue(i32 %value)");
                pw.println("  store i32 %converted, i32* %dataptr");
                pw.println("  ret void");
                pw.println("}");
                pw.println();

                // short
                pw.println("define internal i32 @bytecoder.getShortFieldValueAsObject(i32 %object, i32 %field) {");
                pw.println("entry:");
                pw.print("    %offset = add i32 %field, ");
                pw.println(theOffsetOffset);
                pw.println("  %ptr = inttoptr i32 %offset to i32*");
                pw.println("  %offsetvalue = load i32, i32* %ptr");

                pw.println("  %data = add i32 %object, %offsetvalue");
                pw.println("  %dataptr = inttoptr i32 %data to i32*");
                pw.println("  %result = load i32, i32* %dataptr");
                pw.println("  %class = call i32 @jlShort__init()");
                pw.println("  %converted = call i32 @jlShort_jlShortvalueOfSHORT(i32 %class, i32 %result);");
                pw.println("  ret i32 %converted");
                pw.println("}");
                pw.println();

                pw.println("define internal void @bytecoder.putShortFieldValueFromObject(i32 %object, i32 %field, i32 %value) {");
                pw.println("entry:");
                pw.print("    %offset = add i32 %field, ");
                pw.println(theOffsetOffset);
                pw.println("  %ptr = inttoptr i32 %offset to i32*");
                pw.println("  %offsetvalue = load i32, i32* %ptr");

                pw.println("  %data = add i32 %object, %offsetvalue");
                pw.println("  %dataptr = inttoptr i32 %data to i32*");

                pw.println("  %class = call i32 @jlShort__init()");
                pw.println("  %converted = call i32 @jlShort_SHORTshortValue(i32 %value)");
                pw.println("  store i32 %converted, i32* %dataptr");
                pw.println("  ret void");
                pw.println("}");
                pw.println();

                // float
                pw.println("define internal i32 @bytecoder.getFloatFieldValueAsObject(i32 %object, i32 %field) {");
                pw.println("entry:");
                pw.print("    %offset = add i32 %field, ");
                pw.println(theOffsetOffset);
                pw.println("  %ptr = inttoptr i32 %offset to i32*");
                pw.println("  %offsetvalue = load i32, i32* %ptr");

                pw.println("  %data = add i32 %object, %offsetvalue");
                pw.println("  %dataptr = inttoptr i32 %data to float*");
                pw.println("  %result = load float, float* %dataptr");
                pw.println("  %class = call i32 @jlFloat__init()");
                pw.println("  %converted = call i32 @jlFloat_jlFloatvalueOfFLOAT(i32 %class, float %result);");
                pw.println("  ret i32 %converted");
                pw.println("}");
                pw.println();

                pw.println("define internal void @bytecoder.putFloatFieldValueFromObject(i32 %object, i32 %field, i32 %value) {");
                pw.println("entry:");
                pw.print("    %offset = add i32 %field, ");
                pw.println(theOffsetOffset);
                pw.println("  %ptr = inttoptr i32 %offset to i32*");
                pw.println("  %offsetvalue = load i32, i32* %ptr");

                pw.println("  %data = add i32 %object, %offsetvalue");
                pw.println("  %dataptr = inttoptr i32 %data to float*");

                pw.println("  %class = call i32 @jlFloat__init()");
                pw.println("  %converted = call float @jlFloat_FLOATfloatValue(i32 %value)");
                pw.println("  store float %converted, float* %dataptr");
                pw.println("  ret void");
                pw.println("}");
                pw.println();

                // double
                pw.println("define internal i32 @bytecoder.getDoubleFieldValueAsObject(i32 %object, i32 %field) {");
                pw.println("entry:");
                pw.print("    %offset = add i32 %field, ");
                pw.println(theOffsetOffset);
                pw.println("  %ptr = inttoptr i32 %offset to i32*");
                pw.println("  %offsetvalue = load i32, i32* %ptr");

                pw.println("  %data = add i32 %object, %offsetvalue");
                pw.println("  %dataptr = inttoptr i32 %data to double*");
                pw.println("  %result = load double, double* %dataptr");
                pw.println("  %class = call i32 @jlDouble__init()");
                pw.println("  %converted = call i32 @jlDouble_jlDoublevalueOfDOUBLE(i32 %class, double %result);");
                pw.println("  ret i32 %converted");
                pw.println("}");
                pw.println();

                pw.println("define internal void @bytecoder.putDoubleFieldValueFromObject(i32 %object, i32 %field, i32 %value) {");
                pw.println("entry:");
                pw.print("    %offset = add i32 %field, ");
                pw.println(theOffsetOffset);
                pw.println("  %ptr = inttoptr i32 %offset to i32*");
                pw.println("  %offsetvalue = load i32, i32* %ptr");

                pw.println("  %data = add i32 %object, %offsetvalue");
                pw.println("  %dataptr = inttoptr i32 %data to double*");

                pw.println("  %class = call i32 @jlDouble__init()");
                pw.println("  %converted = call double @jlDouble_DOUBLEdoubleValue(i32 %value)");
                pw.println("  store double %converted, double* %dataptr");
                pw.println("  ret void");
                pw.println("}");
                pw.println();

                // long
                pw.println("define internal i32 @bytecoder.getLongFieldValueAsObject(i32 %object, i32 %field) {");
                pw.println("entry:");
                pw.print("    %offset = add i32 %field, ");
                pw.println(theOffsetOffset);
                pw.println("  %ptr = inttoptr i32 %offset to i32*");
                pw.println("  %offsetvalue = load i32, i32* %ptr");

                pw.println("  %data = add i32 %object, %offsetvalue");
                pw.println("  %dataptr = inttoptr i32 %data to i64*");
                pw.println("  %result = load i64, i64* %dataptr");
                pw.println("  %class = call i32 @jlLong__init()");
                pw.println("  %converted = call i32 @jlLong_jlLongvalueOfLONG(i32 %class, i64 %result);");
                pw.println("  ret i32 %converted");
                pw.println("}");
                pw.println();

                pw.println("define internal void @bytecoder.putLongFieldValueFromObject(i32 %object, i32 %field, i32 %value) {");
                pw.println("entry:");
                pw.print("    %offset = add i32 %field, ");
                pw.println(theOffsetOffset);
                pw.println("  %ptr = inttoptr i32 %offset to i32*");
                pw.println("  %offsetvalue = load i32, i32* %ptr");

                pw.println("  %data = add i32 %object, %offsetvalue");
                pw.println("  %dataptr = inttoptr i32 %data to i64*");

                pw.println("  %class = call i32 @jlLong__init()");
                pw.println("  %converted = call i64 @jlLong_LONGlongValue(i32 %value)");
                pw.println("  store i64 %converted, i64* %dataptr");
                pw.println("  ret void");
                pw.println("}");
                pw.println();

                final int theAccessorMethod = theFieldMemoryLayout.offsetForInstanceMember("accessorMethod");
                pw.println("define internal i32 @bytecoder.getfieldvalue(i32 %target,i32 %field) inlinehint {");
                pw.println("entry:");
                pw.print("    %offset = add i32 %field,");
                pw.println(theAccessorMethod);
                pw.println("  %offsetptr = inttoptr i32 %offset to i32*");
                pw.println("  %method = load i32, i32* %offsetptr");
                pw.println("  %methodptr = inttoptr i32 %method to i32(i32,i32)*");
                pw.println("  %result = call i32(i32,i32) %methodptr(i32 %target, i32 %field)");
                pw.println("  ret i32 %result");
                pw.println("}");
                pw.println();

                final int theMutatorMethod = theFieldMemoryLayout.offsetForInstanceMember("mutationMethod");
                pw.println("define internal i32 @bytecoder.putfieldvalue(i32 %target,i32 %field,i32 %value) inlinehint {");
                pw.println("entry:");
                pw.print("    %offset = add i32 %field,");
                pw.println(theMutatorMethod);
                pw.println("  %offsetptr = inttoptr i32 %offset to i32*");
                pw.println("  %method = load i32, i32* %offsetptr");
                pw.println("  %methodptr = inttoptr i32 %method to void(i32,i32,i32)*");
                pw.println("  call void(i32,i32,i32) %methodptr(i32 %target, i32 %field, i32 %value)");
                pw.println("  ret i32 0");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @bytecoder.instanceof(i32 %object, i32 %typeid) inlinehint {");
                pw.println("entry:");
                pw.println("    %nulltest = icmp eq i32 %object, 0");
                pw.println("    br i1 %nulltest, label %isnull, label %notnull");
                pw.println("notnull:");

                pw.println("    %ptr = add i32 %object, 4");
                pw.println("    %ptr_ptr = inttoptr i32 %ptr to i32*");
                pw.println("    %ptr_loaded = load i32, i32* %ptr_ptr");
                pw.println("    %vtable = inttoptr i32 %ptr_loaded to i32*");
                pw.println("    %resolved_ptr = load i32, i32* %vtable");
                pw.println("    %resolved_ptr_ptr = inttoptr i32 %resolved_ptr to i32(i32,i32)*");
                pw.println("    %result = call i32 %resolved_ptr_ptr(i32 %object, i32 %typeid)");
                pw.println("    ret i32 %result");
                pw.println("isnull:");
                pw.println("    ret i32 0");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @jlClass_BOOLEANisInstancejlObject(i32 %thisRef, i32 %instance) {");
                pw.println("entry:");
                pw.println("    %nulltest = icmp eq i32 %instance, 0");
                pw.println("    br i1 %nulltest, label %isnull, label %notnull");
                pw.println("notnull:");
                pw.println("    %ptr = inttoptr i32 %instance to i32*");
                pw.println("    %type = load i32, i32* %ptr");
                pw.println("    %assignable = call i32 @jlClass_BOOLEANisAssignableFromjlClass(i32 %thisRef, i32 %type)");
                pw.println("    ret i32 %assignable");
                pw.println("isnull:");
                pw.println("    ret i32 0");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @jlClass_BOOLEANisAssignableFromjlClass(i32 %thisRef, i32 %otherType) {");
                pw.println("entry:");

                aLinkerContext.linkedClasses().forEach(aEntry -> {

                    if (aEntry.emulatedByRuntime()) {
                        return;
                    }

                    if (aEntry.getClassName().equals(BytecodeObjectTypeRef.fromRuntimeClass(Class.class))) {
                        return;
                    }

                    if (aEntry.getClassName().equals(BytecodeObjectTypeRef.fromRuntimeClass(Void.class))) {
                        return;
                    }

                    pw.print("    %runtimeclass_");
                    pw.print(aEntry.getUniqueId());
                    pw.print(" = call i32 @");
                    pw.print(LLVMWriterUtils.toClassName(aEntry.getClassName()));
                    pw.print(LLVMWriter.CLASSINITSUFFIX);
                    pw.println("()");

                    pw.print("    %test");
                    pw.print(aEntry.getUniqueId());
                    pw.print(" = icmp eq i32 %otherType, %runtimeclass_");
                    pw.println(aEntry.getUniqueId());

                    pw.print("    br i1 %test");
                    pw.print(aEntry.getUniqueId());
                    pw.print(", label %class");
                    pw.print(aEntry.getUniqueId());
                    pw.print(", label %notclass");
                    pw.println(aEntry.getUniqueId());

                    pw.print("class");
                    pw.print(aEntry.getUniqueId());
                    pw.println(":");

                    for (final BytecodeLinkedClass theImplType : aEntry.getImplementingTypes()) {
                        pw.print("    %runtimeclass_");
                        pw.print(aEntry.getUniqueId());
                        pw.print("_");
                        pw.print(theImplType.getUniqueId());
                        pw.print(" = call i32 @");
                        pw.print(LLVMWriterUtils.toClassName(theImplType.getClassName()));
                        pw.print(LLVMWriter.CLASSINITSUFFIX);
                        pw.println("()");

                        pw.print("    %test_");
                        pw.print(aEntry.getUniqueId());
                        pw.print("_");
                        pw.print(theImplType.getUniqueId());
                        pw.print(" = icmp eq i32 %thisRef, %");
                        pw.print("runtimeclass_");
                        pw.print(aEntry.getUniqueId());
                        pw.print("_");
                        pw.println(theImplType.getUniqueId());

                        pw.print("    br i1 %test_");
                        pw.print(aEntry.getUniqueId());
                        pw.print("_");
                        pw.print(theImplType.getUniqueId());
                        pw.print(", label %assignable, label %test");
                        pw.print(aEntry.getUniqueId());
                        pw.print("_");
                        pw.print(theImplType.getUniqueId());
                        pw.println("_notok");

                        pw.print("test");
                        pw.print(aEntry.getUniqueId());
                        pw.print("_");
                        pw.print(theImplType.getUniqueId());
                        pw.println("_notok:");
                    }
                    pw.println("    ret i32 0");

                    pw.print("notclass");
                    pw.print(aEntry.getUniqueId());
                    pw.println(":");
                });

                pw.println("    ret i32 0");
                pw.println("assignable:");
                pw.println("    ret i32 1");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @jlClass_jlClassgetSuperclass(i32 %type) inlinehint {");
                pw.println("entry:");
                aLinkerContext.linkedClasses().forEach(aEntry -> {
                    if (aEntry.emulatedByRuntime()) {
                        return;
                    }

                    final String prefix = "pre" + aEntry.getUniqueId();
                    pw.print("    %");
                    pw.print(prefix);
                    pw.print("_runtimeclass = load i32, i32* @");
                    pw.print(LLVMWriterUtils.toClassName(aEntry.getClassName()));
                    pw.println(LLVMWriter.RUNTIMECLASSSUFFIX);

                    pw.print("    %");
                    pw.print(prefix);
                    pw.print("_check = icmp eq i32 %type, %");
                    pw.print(prefix);
                    pw.println("_runtimeclass");

                    pw.print("    br i1 %");
                    pw.print(prefix);
                    pw.print("_check");
                    pw.print(", label %");
                    pw.print(prefix);
                    pw.print("_ok");
                    pw.print(", label %");
                    pw.print(prefix);
                    pw.println("_notok");

                    pw.print(prefix);
                    pw.print("_ok");
                    pw.println(":");

                    if (!aEntry.getClassName().name().equals(Object.class.getName())) {
                        pw.print("    %");
                        pw.print(prefix);
                        pw.print("_superclass = load i32, i32* @");
                        pw.print(LLVMWriterUtils.toClassName(aEntry.getSuperClass().getClassName()));
                        pw.println(LLVMWriter.RUNTIMECLASSSUFFIX);
                        pw.print("    ret i32 ");
                        pw.print("%");
                        pw.print(prefix);
                        pw.println("_superclass");
                    } else {
                        pw.println("    ret i32 0");
                    }

                    pw.print(prefix);
                    pw.print("_notok");
                    pw.println(":");
                });

                pw.println("    ret i32 0");
                pw.println("}");
                pw.println();

                // NaN values are not equals to themself
                pw.println("define internal i32 @bytecoder.isnan.float(float %value) inlinehint {");
                pw.println("entry:");
                pw.println("    %test = fcmp oeq float %value, %value");
                pw.println("    br i1 %test, label %iseq, label %isnoteq");
                pw.println("iseq:");
                pw.println("    ret i32 0");
                pw.println("isnoteq:");
                pw.println("    ret i32 1");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @bytecoder.isnan.double(double %value) inlinehint {");
                pw.println("entry:");
                pw.println("    %test = fcmp oeq double %value, %value");
                pw.println("    br i1 %test, label %iseq, label %isnoteq");
                pw.println("iseq:");
                pw.println("    ret i32 0");
                pw.println("isnoteq:");
                pw.println("    ret i32 1");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @bytecoder.float.to.i32(float %value) inlinehint {");
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

                pw.println("define internal i64 @bytecoder.float.to.i64(float %value) inlinehint {");
                pw.println("entry:");
                pw.println("    %test = fcmp oeq float %value, %value");
                pw.println("    br i1 %test, label %iseq, label %isnoteq");
                pw.println("iseq:");
                pw.println("    %converted = fptosi float %value to i64");
                pw.println("    ret i64 %converted");
                pw.println("isnoteq:");
                pw.println("    ret i64 0");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @bytecoder.double.to.i32(double %value) inlinehint {");
                pw.println("entry:");
                pw.println("    %test = fcmp oeq double %value, %value");
                pw.println("    br i1 %test, label %iseq, label %isnoteq");
                pw.println("iseq:");
                pw.println("    %converted = fptosi double %value to i32");
                pw.println("    ret i32 %converted");
                pw.println("isnoteq:");
                pw.println("    ret i32 0");
                pw.println("}");
                pw.println();

                pw.println("define internal i64 @bytecoder.double.to.i64(double %value) inlinehint {");
                pw.println("entry:");
                pw.println("    %test = fcmp oeq double %value, %value");
                pw.println("    br i1 %test, label %iseq, label %isnoteq");
                pw.println("iseq:");
                pw.println("    %converted = fptosi double %value to i64");
                pw.println("    ret i64 %converted");
                pw.println("isnoteq:");
                pw.println("    ret i64 0");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @bytecoder.compare.i32(i32 %v1, i32 %v2) inlinehint {");
                pw.println("entry:");
                pw.println("    %test = icmp eq i32 %v1,%v2");
                pw.println("    br i1 %test, label %iseq, label %isnoteq");
                pw.println("iseq:");
                pw.println("    ret i32 0");
                pw.println("isnoteq:");
                pw.println("    %test2 = icmp sgt i32 %v1,%v2");
                pw.println("    br i1 %test2, label %isgreater, label %issmaller");
                pw.println("isgreater:");
                pw.println("    ret i32 1");
                pw.println("issmaller:");
                pw.println("    ret i32 -1");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @bytecoder.compare.float(float %v1, float %v2) inlinehint {");
                pw.println("entry:");
                pw.println("    %test = fcmp oeq float %v1,%v2");
                pw.println("    br i1 %test, label %iseq, label %isnoteq");
                pw.println("iseq:");
                pw.println("    ret i32 0");
                pw.println("isnoteq:");
                pw.println("    %test2 = fcmp ogt float %v1,%v2");
                pw.println("    br i1 %test2, label %isgreater, label %issmaller");
                pw.println("isgreater:");
                pw.println("    ret i32 1");
                pw.println("issmaller:");
                pw.println("    ret i32 -1");
                pw.println("}");
                pw.println();

                pw.println("define internal i1 @bytecoder.exceedsrange(i32 %v, i32 %min, i32 %max) inlinehint {");
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

                pw.println("define internal i32 @bytecoder.maximum.i32(i32 %a, i32 %b) inlinehint {");
                pw.println("entry:");
                pw.println("   %test = icmp sgt i32 %a, %b");
                pw.println("   br i1 %test, label %aisgreater, label %notgreater");
                pw.println("aisgreater:");
                pw.println("   ret i32 %a");
                pw.println("notgreater:");
                pw.println("   ret i32 %b");
                pw.println("}");
                pw.println();

                pw.println("define internal i64 @bytecoder.maximum.i64(i64 %a, i64 %b) inlinehint {");
                pw.println("entry:");
                pw.println("   %test = icmp sgt i64 %a, %b");
                pw.println("   br i1 %test, label %aisgreater, label %notgreater");
                pw.println("aisgreater:");
                pw.println("   ret i64 %a");
                pw.println("notgreater:");
                pw.println("   ret i64 %b");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @bytecoder.minimum.i32(i32 %a, i32 %b) inlinehint {");
                pw.println("entry:");
                pw.println("   %test = icmp slt i32 %a, %b");
                pw.println("   br i1 %test, label %aissmaller, label %notsmaller");
                pw.println("aissmaller:");
                pw.println("   ret i32 %a");
                pw.println("notsmaller:");
                pw.println("   ret i32 %b");
                pw.println("}");
                pw.println();

                pw.println("define internal i64 @bytecoder.minimum.i64(i64 %a, i64 %b) inlinehint {");
                pw.println("entry:");
                pw.println("   %test = icmp slt i64 %a, %b");
                pw.println("   br i1 %test, label %aissmaller, label %notsmaller");
                pw.println("aissmaller:");
                pw.println("   ret i64 %a");
                pw.println("notsmaller:");
                pw.println("   ret i64 %b");
                pw.println("}");
                pw.println();

                // Method type
                pw.println("define internal i32 @bytecoder.checkmethodtype(i32 %methodType, i32 %index, i32 %expectedType) inlinehint {");
                pw.println("entry:");
                pw.println("    %offset = mul i32 %index, 4");
                pw.println("    %ptr1 = add i32 %offset, 24");
                pw.println("    %ptr2 = add i32 %ptr1, %methodType");
                pw.println("    %ptr = inttoptr i32 %ptr2 to i32*");
                pw.println("    %type = load i32, i32* %ptr");
                pw.println("    %cmp = icmp eq i32 %type, %expectedType");
                pw.println("    br i1 %cmp, label %equals, label %notequals");
                pw.println("equals:");
                pw.println("    ret i32 1");
                pw.println("notequals:");
                pw.println("    ret i32 0");
                pw.println("}");
                pw.println();

                // Lambda
                pw.println("define internal i32 @bytecoder.lambda__interfacedispatch(i32 %thisRef,i32 %methodId) {");
                pw.println("entry:");
                pw.println("    %default_idispatch_offset = add i32 %thisRef, 16");
                pw.println("    %default_idispatch_ptr = inttoptr i32 %default_idispatch_offset to i32*");
                pw.println("    %default_idispatch = load i32, i32* %default_idispatch_ptr");
                pw.println("    %default_idispatch1 = inttoptr i32 %default_idispatch to i32(i32,i32)*");
                pw.println("    %default_resolved = call i32(i32,i32) %default_idispatch1(i32 %thisRef, i32 %methodId)");
                pw.println("    %unresolved = icmp eq i32 %default_resolved, -1");
                pw.println("    br i1 %unresolved, label %overridden, label %bydefault");
                pw.println("bydefault:");
                pw.println("    ret i32 %default_resolved");
                pw.println("overridden:");
                pw.println("    %offset = add i32 %thisRef, 8");
                pw.println("    %offsetptr = inttoptr i32 %offset to i32*");
                pw.println("    %ptr = load i32, i32* %offsetptr");
                pw.println("    ret i32 %ptr");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @bytecoder.newLambda(i32 %methodType, i32 %implementationMethod, i32 %staticArguments) {");
                pw.println("entry:");
                pw.println("    %lambda_interface_dispatch = ptrtoint i32(i32,i32)* @bytecoder.lambda__interfacedispatch to i32");
                // Methodtype is internally an array
                pw.println("    %type_offset = add i32 20, %methodType");
                pw.println("    %type_offset_ptr = inttoptr i32 %type_offset to i32*");
                pw.println("    %runtimeClass = load i32, i32* %type_offset_ptr");

                pw.println("    %interfacedispatchoffset = add i32 24, %runtimeClass");
                pw.println("    %interfacedispatchptr = inttoptr i32 %interfacedispatchoffset to i32*");
                pw.println("    %defaultInterfaceDispatch = load i32, i32* %interfacedispatchptr");

                pw.println("    %vtable = call i32 @dynamicvtable(i32 %runtimeClass, i32 %implementationMethod,i32 %lambda_interface_dispatch)");

                pw.print("    %allocated = call i32 @");
                pw.print(LLVMWriterUtils.toClassName(theMemoryManagerClass.getClassName()));
                pw.println("_INTnewObjectINTINTINT(i32 0,i32 20,i32 %runtimeClass,i32 %vtable)");

                pw.println("    %offset1 = add i32 %allocated, 8");
                pw.println("    %offset1ptr = inttoptr i32 %offset1 to i32*");
                pw.println("    store i32 %implementationMethod, i32* %offset1ptr");

                pw.println("    %offset2 = add i32 %allocated, 12");
                pw.println("    %offset2ptr = inttoptr i32 %offset2 to i32*");
                pw.println("    store i32 %staticArguments, i32* %offset2ptr");

                pw.println("    %offset3 = add i32 %allocated, 16");
                pw.println("    %offset3ptr = inttoptr i32 %offset3 to i32*");
                pw.println("    store i32 %defaultInterfaceDispatch, i32* %offset3ptr");

                pw.println("    ret i32 %allocated");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @jlObject_jlClassgetClassInternal(i32 %thisRef) {");
                pw.println("entry:");
                pw.println("    ret i32 0");
                pw.println("}");
                pw.println();

                final BytecodeLinkedClass theClassLinkedCass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Class.class));
                final BytecodeVTable theClassVTable = theSymbolResolver.vtableFor(theClassLinkedCass);
                final List<BytecodeVTable.Slot> theClassSlots = theClassVTable.sortedSlots();
                pw.print("%");
                pw.print(LLVMWriterUtils.toClassName(theClassLinkedCass.getClassName()));
                pw.print(LLVMWriter.VTABLETYPESUFFIX);
                pw.print(" = type {i1(i32,i32)*,i32(i32,i32)*");
                for (final BytecodeVTable.Slot slot : theClassSlots) {
                    final BytecodeVTable.VPtr ptr = theClassVTable.slot(slot);
                    pw.print(",");
                    pw.print(LLVMWriterUtils.toSignature(ptr.getSignature()));
                    pw.print("*");
                }
                pw.println("}");
                pw.println();

                pw.print("@");
                pw.print(LLVMWriterUtils.toClassName(theClassLinkedCass.getClassName()));
                pw.print(LLVMWriter.VTABLESUFFIX);
                pw.print(" = private global %");
                pw.print(LLVMWriterUtils.toClassName(theClassLinkedCass.getClassName()));
                pw.print(LLVMWriter.VTABLETYPESUFFIX);
                pw.println(" {");
                pw.println("    i1(i32,i32)* @jlClass__instanceof,");
                pw.println("    i32(i32,i32)* @jlClass__interfacedispatch");
                for (final BytecodeVTable.Slot slot : theClassSlots) {
                    final BytecodeVTable.VPtr ptr = theClassVTable.slot(slot);
                    if (ptr.getImplementingClass() != null) {
                        pw.println(",");
                        pw.print("    ");
                        pw.print(LLVMWriterUtils.toSignature(ptr.getSignature()));
                        pw.print("* @");
                        pw.print(LLVMWriterUtils.toMethodName(ptr.getImplementingClass(), ptr.getMethodName(), ptr.getSignature()));
                    } else {
                        pw.println(",");
                        pw.print("    ");
                        pw.print(LLVMWriterUtils.toSignature(ptr.getSignature()));
                        pw.print("* undef");
                    }
                }
                pw.println();
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @jlClass_jlStringgetName(i32 %thisRef) {");
                pw.println("entry:");
                pw.println("    %offset = add i32 %thisRef, 16");
                pw.println("    %ptr = inttoptr i32 %offset to i32*");
                pw.println("    %classname_ptr = load i32, i32* %ptr");
                pw.println("    %classname_ptrptr = inttoptr i32 %classname_ptr to i32*");
                pw.println("    %classname = load i32, i32* %classname_ptrptr");
                pw.println("    ret i32 %classname");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @jlClass_BOOLEANisPrimitive(i32 %thisRef) {");
                pw.println("entry:");
                pw.println("    %offset = add i32 %thisRef, 28");
                pw.println("    %ptr = inttoptr i32 %offset to i32*");
                pw.println("    %status = load i32, i32* %ptr");
                pw.println("    ret i32 %status");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @jlClass_A1jlrFieldgetDeclaredFields(i32 %thisRef) {");
                pw.println("entry:");
                pw.println("    %offset = add i32 %thisRef, 32");
                pw.println("    %ptr = inttoptr i32 %offset to i32*");
                pw.println("    %status = load i32, i32* %ptr");
                pw.println("    ret i32 %status");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @jlClass_BOOLEANdesiredAssertionStatus(i32 %thisRef) {");
                pw.println("entry:");
                pw.println("    ret i32 0");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @jlClass_A1jlObjectgetEnumConstants(i32 %thisRef) {");
                pw.println("entry:");
                pw.println("    ret i32 0");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @jlClass_jlClassLoadergetClassLoader(i32 %thisRef) {");
                pw.println("entry:");
                pw.println("    ret i32 0");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @jlClass_jlClassforNamejlStringBOOLEANjlClassLoader(i32 %thisRef, i32 %name, i32 %initialize, i32 %classloader) {");
                pw.println("entry:");
                aLinkerContext.linkedClasses().filter(t -> aLinkerContext.reflectionConfiguration().resolve(t.getClassName().name()).supportsClassForName()).forEach(aEntry -> {

                    if (aEntry.getBytecodeClass().getAccessFlags().isInterface() || aEntry.isOpaqueType()) {
                        return;
                    }

                    if (Objects.equals(aEntry.getClassName(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                        return;
                    }

                    if (aEntry.emulatedByRuntime()) {
                        return;
                    }

                    pw.print("    %class_");
                    pw.print(aEntry.getUniqueId());
                    pw.print(" = load i32, i32* @");
                    pw.println(theSymbolResolver.globalFromStringPool(aEntry.getClassName().name()));

                    pw.print("    %test_");
                    pw.print(aEntry.getUniqueId());
                    pw.print(" = call i32 @jlString_BOOLEANequalsjlObject(i32 %class_");
                    pw.print(aEntry.getUniqueId());
                    pw.println(",i32 %name)");

                    pw.print("    %check_");
                    pw.print(aEntry.getUniqueId());
                    pw.print(" = icmp eq i32 1, %test_");
                    pw.println(aEntry.getUniqueId());

                    pw.print("    br i1 %check_");
                    pw.print(aEntry.getUniqueId());
                    pw.print(", label %check_ok_");
                    pw.print(aEntry.getUniqueId());
                    pw.print(", label %check_notok_");
                    pw.println(aEntry.getUniqueId());

                    pw.print("check_ok_");
                    pw.print(aEntry.getUniqueId());
                    pw.println(":");

                    pw.print("    %init_");
                    pw.println(aEntry.getUniqueId());
                    pw.print(" = call i32 @");
                    pw.print(LLVMWriterUtils.toClassName(aEntry.getClassName()));
                    pw.print(LLVMWriter.CLASSINITSUFFIX);
                    pw.println("()");
                    pw.print("    ret i32 %init_");
                    pw.println(aEntry.getUniqueId());

                    pw.print("check_notok_");
                    pw.print(aEntry.getUniqueId());
                    pw.println(":");
                });
                pw.println("    call void @llvm.trap()");
                pw.println("    unreachable");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @bytecoder.newRuntimeClass(i32 %type, i32 %staticSize, i32 %enumValuesOffset, i32 %nameStringPoolIndex, i32 %interfaceDispatchPtr, i32 %primitiveFlag, i32 %declaredFieldList) {");
                pw.println("entry:");
                pw.println("    %vtableptr = ptrtoint %jlClass__vtable__type* @jlClass__vtable to i32");
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
                pw.println("    %interfaceDispatchPtrpos = add i32 %allocated, 24");
                pw.println("    %interfaceDispatchPtrpos_ptr = inttoptr i32 %interfaceDispatchPtrpos to i32*");
                pw.println("    store i32 %interfaceDispatchPtr, i32* %interfaceDispatchPtrpos_ptr");

                pw.println("    %primitiveFlagPos = add i32 %allocated, 28");
                pw.println("    %primitiveFlagPos_ptr = inttoptr i32 %primitiveFlagPos to i32*");
                pw.println("    store i32 %primitiveFlag, i32* %primitiveFlagPos_ptr");

                pw.println("    %declaredFieldListPos = add i32 %allocated, 32");
                pw.println("    %declaredFieldListPos_ptr = inttoptr i32 %declaredFieldListPos to i32*");
                pw.println("    store i32 %declaredFieldList, i32* %declaredFieldListPos_ptr");

                pw.println("    ret i32 %allocated");
                pw.println("}");
                pw.println();

                // Runtime classes for primitives
                pw.print("@CharPrimitive");
                pw.print(LLVMWriter.RUNTIMECLASSSUFFIX);
                pw.println(" = private global i32 0");
                pw.println();

                pw.print("@IntPrimitive");
                pw.print(LLVMWriter.RUNTIMECLASSSUFFIX);
                pw.println(" = private global i32 0");
                pw.println();

                pw.print("@LongPrimitive");
                pw.print(LLVMWriter.RUNTIMECLASSSUFFIX);
                pw.println(" = private global i32 0");
                pw.println();

                pw.print("@BytePrimitive");
                pw.print(LLVMWriter.RUNTIMECLASSSUFFIX);
                pw.println(" = private global i32 0");
                pw.println();

                pw.print("@FloatPrimitive");
                pw.print(LLVMWriter.RUNTIMECLASSSUFFIX);
                pw.println(" = private global i32 0");
                pw.println();

                pw.print("@BooleanPrimitive");
                pw.print(LLVMWriter.RUNTIMECLASSSUFFIX);
                pw.println(" = private global i32 0");
                pw.println();

                pw.print("@ShortPrimitive");
                pw.print(LLVMWriter.RUNTIMECLASSSUFFIX);
                pw.println(" = private global i32 0");
                pw.println();

                pw.print("@DoublePrimitive");
                pw.print(LLVMWriter.RUNTIMECLASSSUFFIX);
                pw.println(" = private global i32 0");
                pw.println();

                pw.println();

                // Now, we can continue to write implementation code
                aLinkerContext.linkedClasses().forEach(aEntry -> {

                    final BytecodeLinkedClass theLinkedClass = aEntry;

                    if (Objects.equals(theLinkedClass.getClassName(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                        return;
                    }

                    if (theLinkedClass.emulatedByRuntime()) {
                        return;
                    }

                    final BytecodeResolvedMethods theMethodMap = aLinkerContext.resolveMethods(theLinkedClass);
                    final String theClassName = LLVMWriterUtils.toClassName(theLinkedClass.getClassName());
                    pw.print("@");
                    pw.print(theClassName);
                    pw.print(LLVMWriter.RUNTIMECLASSSUFFIX);
                    pw.println(" = private global i32 0");
                    pw.println();

                    pw.print("@");
                    pw.print(theClassName);
                    pw.print(LLVMWriter.RUNTIMECLASSINITSTATUSSUFFIX);
                    pw.println(" = private global i32 0");
                    pw.println();

                    if (theLinkedClass != theClassLinkedCass) {
                        final BytecodeVTable theVtable = theSymbolResolver.vtableFor(theLinkedClass);
                        final List<BytecodeVTable.Slot> theSlots = theVtable.sortedSlots();

                        if (theLinkedClass.hasTag(BytecodeLinkedClass.Tag.INSTANTIATED)) {
                            pw.print("%");
                            pw.print(LLVMWriterUtils.toClassName(theLinkedClass.getClassName()));
                            pw.print(LLVMWriter.VTABLETYPESUFFIX);
                            pw.print(" = type {i1(i32,i32)*,i32(i32,i32)*");
                            for (final BytecodeVTable.Slot slot : theSlots) {
                                final BytecodeVTable.VPtr ptr = theVtable.slot(slot);
                                pw.print(",");
                                pw.print(LLVMWriterUtils.toSignature(ptr.getSignature()));
                                pw.print("*");
                            }
                            pw.println("}");
                            pw.println();

                            pw.print("@");
                            pw.print(theClassName);
                            pw.print(LLVMWriter.VTABLESUFFIX);
                            pw.print(" = private global %");
                            pw.print(LLVMWriterUtils.toClassName(theLinkedClass.getClassName()));
                            pw.print(LLVMWriter.VTABLETYPESUFFIX);
                            pw.println(" {");

                            pw.print("    i1(i32,i32)* @");
                            pw.print(theClassName);
                            pw.print(LLVMWriter.INSTANCEOFSUFFIX);
                            pw.println(",");

                            if (!theLinkedClass.getBytecodeClass().getAccessFlags().isInterface() && !theLinkedClass.getBytecodeClass().getAccessFlags().isAbstract()) {
                                pw.print("    i32(i32,i32)* @");
                                pw.print(theClassName);
                                pw.print(LLVMWriter.INTERFACEDISPATCHSUFFIX);
                            } else {
                                pw.print("    i32(i32,i32)* undef");
                            }

                            for (final BytecodeVTable.Slot slot : theSlots) {
                                final BytecodeVTable.VPtr ptr = theVtable.slot(slot);

                                pw.println(",");
                                if (ptr.getImplementingClass() != null) {
                                    pw.println("    ;;" + slot.getPos() + ", " + ptr.getMethodName() + "," + ptr.getSignature() + "," + ptr.getImplementingClass().name());
                                } else {
                                    pw.println("    ;;" + slot.getPos() + ", " + ptr.getMethodName() + "," + ptr.getSignature() + ", abstract");
                                }
                                pw.print("    ");
                                pw.print(LLVMWriterUtils.toSignature(ptr.getSignature()));
                                pw.print("* ");
                                if (ptr.getImplementingClass() != null) {
                                    final BytecodeLinkedClass theSlotClass = aLinkerContext.resolveClass(ptr.getImplementingClass());
                                    final BytecodeMethod theMethod = theSlotClass.getBytecodeClass().methodByNameAndSignatureOrNull(ptr.getMethodName(), ptr.getSignature());
                                    if (theMethod != null && !theMethod.getAccessFlags().isAbstract()) {
                                        pw.print("@");
                                        pw.print(LLVMWriterUtils.toMethodName(ptr.getImplementingClass(), ptr.getMethodName(), ptr.getSignature()));
                                    } else {
                                        pw.print("undef");
                                    }
                                } else {
                                    pw.print("undef");
                                }
                            }
                            pw.println();
                            pw.println("}");
                            pw.println();
                        }
                    }

                    if (!Objects.equals(theLinkedClass.getClassName(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {

                        if (theLinkedClass.getClassName().name().equals(aEntryPointClass.getName())) {
                            pw.print("attributes #");
                            pw.print(attributeCounter.get());
                            pw.print(" = {");
                            pw.print("\"wasm-export-name\"");
                            pw.print("=");
                            pw.print("\"");
                            pw.print(theClassName);
                            pw.print(LLVMWriter.CLASSINITSUFFIX);
                            pw.println("\"}");
                            attributeCounter.incrementAndGet();

                            pw.print("define i32 @");
                        } else {
                            pw.print("define internal i32 @");
                        }

                        pw.print(theClassName);
                        pw.print(LLVMWriter.CLASSINITSUFFIX);
                        pw.println("() {");
                        pw.println("entry:");
                        pw.print("    %class = load i32, i32* @");
                        pw.print(theClassName);
                        pw.println(LLVMWriter.RUNTIMECLASSSUFFIX);
                        pw.print("    %value = load i32, i32* @");
                        pw.print(theClassName);
                        pw.println(LLVMWriter.RUNTIMECLASSINITSTATUSSUFFIX);
                        pw.println("    %initialized_compare = icmp eq i32 %value, 1");
                        pw.println("    br i1 %initialized_compare, label %done, label %unitialized");
                        pw.println("unitialized:");
                        pw.print("    store i32 1,i32* @");
                        pw.print(theClassName);
                        pw.println(LLVMWriter.RUNTIMECLASSINITSTATUSSUFFIX);

                        // We create the array for the declared fields
                        pw.println("    %arrayvtableptr = ptrtoint %dmbcArray__vtable__type* @dmbcArray__vtable to i32");
                        pw.println("    %arrayclassinit = call i32 @dmbcArray__init()");

                        if (aLinkerContext.reflectionConfiguration().resolve(theLinkedClass.getClassName().name()).supportsClassForName()) {

                            // Collect all declared fields
                            final List<BytecodeResolvedFields.FieldEntry> declaredFields = theLinkedClass.resolvedFields().stream().filter(
                                    t -> t.getProvidingClass() == theLinkedClass
                            ).collect(Collectors.toList());

                            final NativeMemoryLayouter.MemoryLayout theMemoryLayout = memoryLayouter.layoutFor(theLinkedClass.getClassName());

                            pw.println("   %bytecoder_getObjectFieldValueAsObject_ptr = ptrtoint i32(i32,i32)* @bytecoder.getObjectFieldValueAsObject to i32");
                            pw.println("   %bytecoder_putObjectFieldValueFromObject_ptr = ptrtoint void(i32,i32,i32)* @bytecoder.putObjectFieldValueFromObject to i32");

                            pw.println("   %bytecoder_getBooleanFieldValueAsObject_ptr = ptrtoint i32(i32,i32)* @bytecoder.getBooleanFieldValueAsObject to i32");
                            pw.println("   %bytecoder_putBooleanFieldValueFromObject_ptr = ptrtoint void(i32,i32,i32)* @bytecoder.putBooleanFieldValueFromObject to i32");

                            pw.println("   %bytecoder_getIntegerFieldValueAsObject_ptr = ptrtoint i32(i32,i32)* @bytecoder.getIntegerFieldValueAsObject to i32");
                            pw.println("   %bytecoder_putIntegerFieldValueFromObject_ptr = ptrtoint void(i32,i32,i32)* @bytecoder.putIntegerFieldValueFromObject to i32");

                            pw.println("   %bytecoder_getByteFieldValueAsObject_ptr = ptrtoint i32(i32,i32)* @bytecoder.getByteFieldValueAsObject to i32");
                            pw.println("   %bytecoder_putByteFieldValueFromObject_ptr = ptrtoint void(i32,i32,i32)* @bytecoder.putByteFieldValueFromObject to i32");

                            pw.println("   %bytecoder_getCharFieldValueAsObject_ptr = ptrtoint i32(i32,i32)* @bytecoder.getCharFieldValueAsObject to i32");
                            pw.println("   %bytecoder_putCharFieldValueFromObject_ptr = ptrtoint void(i32,i32,i32)* @bytecoder.putCharFieldValueFromObject to i32");

                            pw.println("   %bytecoder_getShortFieldValueAsObject_ptr = ptrtoint i32(i32,i32)* @bytecoder.getShortFieldValueAsObject to i32");
                            pw.println("   %bytecoder_putShortFieldValueFromObject_ptr = ptrtoint void(i32,i32,i32)* @bytecoder.putShortFieldValueFromObject to i32");

                            pw.println("   %bytecoder_getFloatFieldValueAsObject_ptr = ptrtoint i32(i32,i32)* @bytecoder.getFloatFieldValueAsObject to i32");
                            pw.println("   %bytecoder_putFloatFieldValueFromObject_ptr = ptrtoint void(i32,i32,i32)* @bytecoder.putFloatFieldValueFromObject to i32");

                            pw.println("   %bytecoder_getDoubleFieldValueAsObject_ptr = ptrtoint i32(i32,i32)* @bytecoder.getDoubleFieldValueAsObject to i32");
                            pw.println("   %bytecoder_putDoubleFieldValueFromObject_ptr = ptrtoint void(i32,i32,i32)* @bytecoder.putDoubleFieldValueFromObject to i32");

                            pw.println("   %bytecoder_getLongFieldValueAsObject_ptr = ptrtoint i32(i32,i32)* @bytecoder.getLongFieldValueAsObject to i32");
                            pw.println("   %bytecoder_putLongFieldValueFromObject_ptr = ptrtoint void(i32,i32,i32)* @bytecoder.putLongFieldValueFromObject to i32");

                            pw.print("    %declaredfields = call i32 @dmbcMemoryManager_INTnewArrayINTINTINT(i32 0,i32 ");
                            pw.print(declaredFields.size());
                            pw.println(",i32 %arrayclassinit,i32 %arrayvtableptr)");

                            for (int i=0;i < declaredFields.size();i++) {
                                final BytecodeResolvedFields.FieldEntry field = declaredFields.get(i);

                                String accessorMethod = "i32 undef";
                                String mutatorMethod = "i32 undef";

                                final String fieldName = theSymbolResolver.globalFromStringPool(field.getValue().getName().stringValue());
                                pw.print("    %field");
                                pw.print(i);
                                pw.print("_name = load i32,i32* @");
                                pw.println(fieldName);

                                if (field.getValue().getTypeRef().isPrimitive()) {
                                    final BytecodePrimitiveTypeRef primitiveTypeRef = (BytecodePrimitiveTypeRef) field.getValue().getTypeRef();

                                    pw.print("    %field");
                                    pw.print(i);
                                    pw.print("_type = load i32, i32* @");

                                    switch (primitiveTypeRef) {
                                        case SHORT:
                                            pw.print("ShortPrimitive");
                                            pw.println(LLVMWriter.RUNTIMECLASSSUFFIX);

                                            accessorMethod = "i32 %bytecoder_getShortFieldValueAsObject_ptr";
                                            mutatorMethod = "i32 %bytecoder_putShortFieldValueFromObject_ptr";

                                            break;
                                        case CHAR:
                                            pw.print("CharPrimitive");
                                            pw.println(LLVMWriter.RUNTIMECLASSSUFFIX);

                                            accessorMethod = "i32 %bytecoder_getCharFieldValueAsObject_ptr";
                                            mutatorMethod = "i32 %bytecoder_putCharFieldValueFromObject_ptr";

                                            break;
                                        case BOOLEAN:
                                            pw.print("BooleanPrimitive");
                                            pw.println(LLVMWriter.RUNTIMECLASSSUFFIX);

                                            accessorMethod = "i32 %bytecoder_getBooleanFieldValueAsObject_ptr";
                                            mutatorMethod = "i32 %bytecoder_putBooleanFieldValueFromObject_ptr";

                                            break;
                                        case BYTE:
                                            pw.print("BytePrimitive");
                                            pw.println(LLVMWriter.RUNTIMECLASSSUFFIX);

                                            accessorMethod = "i32 %bytecoder_getByteFieldValueAsObject_ptr";
                                            mutatorMethod = "i32 %bytecoder_putByteFieldValueFromObject_ptr";

                                            break;
                                        case DOUBLE:
                                            pw.print("DoublePrimitive");
                                            pw.println(LLVMWriter.RUNTIMECLASSSUFFIX);

                                            accessorMethod = "i32 %bytecoder_getDoubleFieldValueAsObject_ptr";
                                            mutatorMethod = "i32 %bytecoder_putDoubleFieldValueFromObject_ptr";

                                            break;
                                        case INT:
                                            pw.print("IntPrimitive");
                                            pw.println(LLVMWriter.RUNTIMECLASSSUFFIX);

                                            accessorMethod = "i32 %bytecoder_getIntegerFieldValueAsObject_ptr";
                                            mutatorMethod = "i32 %bytecoder_putIntegerFieldValueFromObject_ptr";

                                            break;
                                        case LONG:
                                            pw.print("LongPrimitive");
                                            pw.println(LLVMWriter.RUNTIMECLASSSUFFIX);

                                            accessorMethod = "i32 %bytecoder_getLongFieldValueAsObject_ptr";
                                            mutatorMethod = "i32 %bytecoder_putLongFieldValueFromObject_ptr";

                                            break;
                                        case FLOAT:
                                            pw.print("FloatPrimitive");
                                            pw.println(LLVMWriter.RUNTIMECLASSSUFFIX);

                                            accessorMethod = "i32 %bytecoder_getFloatFieldValueAsObject_ptr";
                                            mutatorMethod = "i32 %bytecoder_putFloatFieldValueFromObject_ptr";

                                            break;
                                        default:
                                            throw new IllegalArgumentException("Not supported primitive type" + primitiveTypeRef);
                                    }

                                } else {

                                    accessorMethod = "i32 %bytecoder_getObjectFieldValueAsObject_ptr";
                                    mutatorMethod = "i32 %bytecoder_putObjectFieldValueFromObject_ptr";

                                    pw.print("    %field");
                                    pw.print(i);
                                    pw.print("_type = call i32 @");
                                    if (field.getValue().getTypeRef().isArray()) {
                                        pw.print("dmbcArray__init()");
                                    } else {
                                        final BytecodeObjectTypeRef objectTypeRef = (BytecodeObjectTypeRef) field.getValue().getTypeRef();
                                        pw.print(LLVMWriterUtils.toClassName(objectTypeRef));
                                        pw.print(LLVMWriter.CLASSINITSUFFIX);
                                        pw.println("()");
                                    }
                                }

                                pw.print("    %field");
                                pw.print(i);
                                pw.print(" = call i32 @jlrField_VOID$newInstancejlClassjlStringINTjlClassjlObjectjlObjectINT(i32 %class,i32 %class,i32 %field");
                                pw.print(i);
                                pw.println("_name,i32 ");
                                pw.print(field.getValue().getAccessFlags().getModifiers());
                                pw.print(",i32 %field"); // Type
                                pw.print(i);
                                pw.print("_type,");
                                pw.print(accessorMethod); // Accessor
                                pw.print(",");
                                pw.print(mutatorMethod); // mutator

                                // Offset
                                pw.print(",i32 ");
                                if (field.getValue().getAccessFlags().isStatic()) {
                                    pw.print(theMemoryLayout.offsetForClassMember(field.getValue().getName().stringValue()));
                                } else {
                                    pw.print(theMemoryLayout.offsetForInstanceMember(field.getValue().getName().stringValue()));
                                }

                                pw.println(")");

                                pw.print("    %field");
                                pw.print(i);
                                pw.print("_offset = add i32 %declaredfields, ");
                                pw.println(20 + 8 * i);

                                pw.print("    %field");
                                pw.print(i);
                                pw.print("_ptr = inttoptr i32 %field");
                                pw.print(i);
                                pw.println("_offset to i32*");

                                pw.print("    store i32 %field");
                                pw.print(i);
                                pw.print(",i32* %field");
                                pw.print(i);
                                pw.println("_ptr");
                            }

                        } else {
                            pw.println("    %declaredfields = call i32 @dmbcMemoryManager_INTnewArrayINTINTINT(i32 0,i32 0,i32 %arrayclassinit,i32 %arrayvtableptr)");
                        }

                        pw.println("    %declaredfields_offset = add i32 32, %class");
                        pw.println("    %declaredfields_ptr = inttoptr i32 %declaredfields_offset to i32*");
                        pw.println("    store i32 %declaredfields, i32* %declaredfields_ptr");

                        // Call superclass init
                        if (!theLinkedClass.getClassName().name().equals(Object.class.getName())) {
                            final BytecodeLinkedClass theSuper = theLinkedClass.getSuperClass();
                            final String theSuperWASMName = LLVMWriterUtils.toClassName(theSuper.getClassName());
                            pw.print("    call i32() @");
                            pw.print(theSuperWASMName);
                            pw.print(LLVMWriter.CLASSINITSUFFIX);
                            pw.println("()");
                        }

                        // Call class initializer
                        if (theLinkedClass.hasClassInitializer()) {
                            pw.print("    call void(i32) @");
                            pw.print(theClassName);
                            pw.println("_VOID$clinit$(i32 %class)");
                        }

                        pw.println("    br label %done");
                        pw.println("done:");
                        pw.println("    ret i32 %class");
                        pw.println("}");
                        pw.println();
                    }

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
                    pw.print(LLVMWriter.INTERFACEDISPATCHSUFFIX);
                    pw.println("(i32 %thisRef,i32 %methodId) {");
                    pw.println("entry:");
                    pw.println("    switch i32 %methodId, label %default [");

                    for (final BytecodeResolvedMethods.MethodEntry entry : thevTable) {
                        final BytecodeMethod theMethod = entry.getValue();
                        final BytecodeVirtualMethodIdentifier theMethodIdentifier = aLinkerContext.getMethodCollection()
                                .identifierFor(theMethod);

                        pw.print("        i32 ");
                        pw.print(theMethodIdentifier.getIdentifier());
                        pw.print(",label %v_table_");
                        pw.println(theMethodIdentifier.getIdentifier());
                    }

                    pw.println("    ]");
                    pw.println("default:");
                    pw.println("    ret i32 -1");

                    for (final BytecodeResolvedMethods.MethodEntry methodEntry : thevTable) {
                        final BytecodeMethod theMethod = methodEntry.getValue();
                        final BytecodeVirtualMethodIdentifier theMethodIdentifier = aLinkerContext.getMethodCollection()
                                .identifierFor(theMethod);

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
                    pw.println("}");
                    pw.println();

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
                                    final String theMethodName = LLVMWriterUtils.toMethodName(aEntry.getClassName(), theMethod.getName().stringValue(), theMethod.getSignature());
                                    pw.print("define internal ");
                                    pw.print(LLVMWriterUtils.toType(TypeRef.toType(theMethod.getSignature().getReturnType())));
                                    pw.print(" @");
                                    pw.print(theMethodName);
                                    pw.print("(i32 %runtimeClass");
                                    for (int i = 0; i < theMethod.getSignature().getArguments().length; i++) {
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
                                        for (int i = 0; i < theMethod.getSignature().getArguments().length; i++) {
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
                                        for (int i = 0; i < theMethod.getSignature().getArguments().length; i++) {
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
                        if (theMethod.isConstructor() && !theLinkedClass.getBytecodeClass().getAccessFlags().isAbstract() && !theLinkedClass.getBytecodeClass().getAccessFlags().isInterface() && theLinkedClass.hasTag(BytecodeLinkedClass.Tag.INSTANTIATED)) {

                            if (theLinkedClass.getClassName().name().equals(aEntryPointClass.getName())) {
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
                            } else {
                                pw.print("define internal i32 @");
                            }

                            pw.print(LLVMWriterUtils.toMethodName(theLinkedClass.getClassName(), LLVMWriter.NEWINSTANCE_METHOD_NAME, theMethod.getSignature()));
                            pw.print("(i32 %class");
                            for (int i = 0; i < theMethod.getSignature().getArguments().length; i++) {
                                pw.print(",");
                                pw.print(LLVMWriterUtils.toType(TypeRef.toType(theMethod.getSignature().getArguments()[i])));
                                pw.print(" %p");
                                pw.print(i);
                            }
                            pw.print(") #");
                            pw.print(attributeCounter.get());
                            attributeCounter.incrementAndGet();
                            pw.println(" {");
                            pw.println("entry:");
                            pw.print("    %vtableptr = ptrtoint %");
                            pw.print(theClassName);
                            pw.print(LLVMWriter.VTABLETYPESUFFIX);
                            pw.print("* @");
                            pw.print(theClassName);
                            pw.print(LLVMWriter.VTABLESUFFIX);
                            pw.println(" to i32");
                            pw.print("    %allocated = call i32(i32,i32,i32,i32) @");
                            pw.print(LLVMWriterUtils.toMethodName(theMemoryManagerClass.getClassName(), "newObject",
                                    new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT})));
                            pw.print("(");
                            pw.print("i32 0,");

                            final NativeMemoryLayouter.MemoryLayout theLayout = memoryLayouter.layoutFor(theLinkedClass.getClassName());
                            pw.print("i32 ");
                            pw.print(theLayout.instanceSize());
                            pw.print(",");
                            pw.println("i32 %class ,i32 %vtableptr)");

                            pw.print("    call ");
                            pw.print(LLVMWriterUtils.toSignature(theMethod.getSignature()));
                            pw.print(" @");
                            pw.print(LLVMWriterUtils.toMethodName(theLinkedClass.getClassName(), theMethod.getName(), theMethod.getSignature()));
                            pw.print("(i32 %allocated");
                            for (int i = 0; i < theMethod.getSignature().getArguments().length; i++) {
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
                        final LLVMDebugInformation.CompileUnit compileUnit = debugInformation.compileUnitFor(theSSAProgram);
                        final LLVMDebugInformation.SubProgram subProgram = compileUnit.subProgram(theSSAProgram, theMethod.getName().stringValue(), theSignature);

                        // Run optimizer
                        // We use a special LLVM optimizer, which does only stuff LLVM CANNOT do, such
                        // as virtual method invocation optimization. All other optimization work
                        // is done by LLVM!
                        KnownOptimizer.LLVM.optimize(this, theSSAProgram.getControlFlowGraph(), aLinkerContext);

                        compiledMethods.add(new CompiledMethod(theLinkedClass, theMethod, theSSAProgram, subProgram));
                    });
                });

                // New Instance helper for reflection stuff
                pw.print("define internal i32 @");
                pw.print(LLVMWriter.NEWINSTANCEHELPER);
                pw.println("(i32 %runtimeclass) {");
                pw.println("entry:");

                aLinkerContext.linkedClasses().forEach(search -> {

                    if (!search.getBytecodeClass().getAccessFlags().isAbstract() && !search.getBytecodeClass().getAccessFlags()
                            .isInterface() && !search.emulatedByRuntime() && search.hasTag(BytecodeLinkedClass.Tag.INSTANTIATED)) {

                        final String theClassName = LLVMWriterUtils.toClassName(search.getClassName());

                        // Only if the class has a zero arg constructor
                        final BytecodeResolvedMethods theResolved = aLinkerContext.resolveMethods(search);
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

                            pw.print("    %");
                            pw.print(LLVMWriterUtils.runtimeClassVariableName(search.getClassName()));
                            pw.print(" = call i32 @");
                            pw.print(LLVMWriterUtils.toClassName(search.getClassName()));
                            pw.print(LLVMWriter.CLASSINITSUFFIX);
                            pw.println("()");

                            pw.print("    %newinstance_");
                            pw.print(search.getUniqueId());
                            pw.print(" = call i32 @");
                            pw.print(LLVMWriterUtils.toMethodName(search.getClassName(), "$newInstance", m.getSignature()));
                            pw.print("(i32 %");
                            pw.print(LLVMWriterUtils.runtimeClassVariableName(search.getClassName()));
                            pw.println(")");

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

                final ProgramDescriptorProvider theProvider = new ProgramDescriptorProvider() {
                    @Override
                    public ProgramDescriptor resolveStaticInvocation(final BytecodeObjectTypeRef aClass, final String aMethodName, final BytecodeMethodSignature aSignature) {
                        for (final CompiledMethod theMethod : compiledMethods) {
                            if (theMethod.linkedClass.getClassName().equals(aClass) &&
                                theMethod.method.getName().stringValue().equals(aMethodName) &&
                                theMethod.method.getSignature().matchesExactlyTo(aSignature)) {
                                return new ProgramDescriptor(theMethod.linkedClass, theMethod.method, theMethod.program);
                            }
                        }
                        // Nothing found, this might be the case for runtime emulated classes
                        return null;
                    }

                    @Override
                    public ProgramDescriptor resolveConstructorInvocation(final BytecodeObjectTypeRef aClass, final BytecodeMethodSignature aSignature) {
                        for (final CompiledMethod theMethod : compiledMethods) {
                            if (theMethod.linkedClass.getClassName().equals(aClass) &&
                                    theMethod.method.getName().stringValue().equals("<init>") &&
                                    theMethod.method.getSignature().matchesExactlyTo(aSignature)) {
                                return new ProgramDescriptor(theMethod.linkedClass, theMethod.method, theMethod.program);
                            }
                        }
                        throw new IllegalArgumentException("Cannot find " + aClass.name() + ".<init> " + aSignature);
                    }

                    @Override
                    public ProgramDescriptor resolveDirectInvocation(final BytecodeObjectTypeRef aClass, final String aMethodName, final BytecodeMethodSignature aSignature) {
                        for (final CompiledMethod theMethod : compiledMethods) {
                            if (theMethod.linkedClass.getClassName().equals(aClass) &&
                                    theMethod.method.getName().stringValue().equals(aMethodName) &&
                                    theMethod.method.getSignature().matchesExactlyTo(aSignature)) {
                                return new ProgramDescriptor(theMethod.linkedClass, theMethod.method, theMethod.program);
                            }
                        }
                        // Nothing found, this might be the case for runtime emulated classes
                        return null;
                    }
                };

                final PointsToAnalysis theAnalysis = new PointsToAnalysis(theProvider, aLinkerContext.getLogger());

                // Analyze all methods
                aOptions.getLogger().info("Starting interprocedural dataflow analysis");

                final EscapeAnalysis escapeAnalysis = new EscapeAnalysis();
                for (final CompiledMethod theCompiledMethod : compiledMethods) {

                    final ProgramDescriptor pg = new ProgramDescriptor(theCompiledMethod.linkedClass,
                            theCompiledMethod.method, theCompiledMethod.program);

                    final PointsToAnalysisResult result = theAnalysis.analyze(pg);

                    if (aOptions.isEscapeAnalysisEnabled()) {
                        // Perform some escape analysis
                        escapeAnalysis.analyze(aLinkerContext.getLogger(), pg, result);
                    }
                }

                aOptions.getLogger().info("Finished interprocedural dataflow analysis");

                // We know know the interprocedural data flow, so we can write the LLVM code
                for (final CompiledMethod theCompiledMethod : compiledMethods) {

                    final BytecodeLinkedClass theLinkedClass = theCompiledMethod.linkedClass;
                    final BytecodeMethod theMethod = theCompiledMethod.method;
                    final BytecodeMethodSignature theSignature = theMethod.getSignature();
                    final Program theSSAProgram = theCompiledMethod.program;
                    final LLVMDebugInformation.SubProgram subProgram = theCompiledMethod.debugInformationSubProgram;

                    // Now, we can generate the instance method here
                    final String methodName = LLVMWriterUtils
                            .toMethodName(theLinkedClass.getClassName(), theMethod.getName(), theSignature);

                    final List<String> attributes = new ArrayList<>();
                    final BytecodeAnnotation theExport = theMethod.getAttributes().getAnnotationByType(Export.class.getName());
                    if (theExport != null) {

                        if (theMethod.getSignature().getReturnType() == BytecodePrimitiveTypeRef.LONG) {
                            throw new IllegalArgumentException("Cannot export method " + theMethod.getName().stringValue() + " in class " + theLinkedClass.getClassName().name() + " with signature " + theMethod.getSignature() + " : return type must not be Long");
                        }
                        for (final BytecodeTypeRef theTypeRef : theMethod.getSignature().getArguments()) {
                            if (theTypeRef == BytecodePrimitiveTypeRef.LONG) {
                                throw new IllegalArgumentException("Cannot export method " + theMethod.getName().stringValue() + " in class " + theLinkedClass.getClassName().name() + " with signature " + theMethod.getSignature() + " : argument must not be Long");
                            }
                        }

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
                    for (int i = 0; i < theArguments.size(); i++) {
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
                        pw.print(" ");
                        subProgram.writeDebugSuffixTo(pw);
                        pw.println(" {");
                    } else {
                        pw.print(") ");
                        subProgram.writeDebugSuffixTo(pw);
                        pw.println(" {");
                    }

                    try (final LLVMWriter theWriter = new LLVMWriter(pw, memoryLayouter, aLinkerContext, theSymbolResolver, aOptions.isEscapeAnalysisEnabled())) {
                        theWriter.write(theLinkedClass, theSSAProgram, subProgram);
                    }

                    pw.println("}");
                    pw.println();

                    // Export main entry point
                    if (theLinkedClass.getClassName().name().equals(aEntryPointClass.getName()) && theMethod.getName().stringValue().equals(aEntryPointMethodName) && theMethod.getSignature().matchesExactlyTo(aEntryPointSignature)) {

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
                        for (int i = 0; i < theArguments.size(); i++) {
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
                        for (int i = 0; i < theArguments.size(); i++) {
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
                }

                // Now we know the exact callsites, and we can generate the code for them
                for (final Map.Entry<String, CallSite> theEntry : callsites.entrySet()) {

                    final CallSite callsite = theEntry.getValue();

                    pw.print("@");
                    pw.print("callsite");
                    pw.print(System.identityHashCode(callsite));
                    pw.println(" = private global i32 0");

                    final Program theSSAProgram = theEntry.getValue().program;
                    final LLVMDebugInformation.CompileUnit compileUnit = debugInformation.compileUnitFor("/resolvecallsite" + callsite);
                    final LLVMDebugInformation.SubProgram subProgram = compileUnit.subProgram(theSSAProgram, "/resolvecallsite" + callsite, new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[0]));

                    // Run optimizer
                    // We use a special optimizer, which does only stuff LLVM CANNOT do, such
                    // as virtual method invocation optimization. All other optimization work
                    // is done by LLVM!
                    KnownOptimizer.LLVM.optimize(this, theSSAProgram.getControlFlowGraph(), aLinkerContext);

                    // Perform escape analysis
                    if (aOptions.isEscapeAnalysisEnabled()) {
                        theAnalysis.analyze(new ProgramDescriptor(theEntry.getValue().owningClass,
                                new BytecodeMethod(new BytecodeAccessFlags(0),
                                        new BytecodeUtf8Constant("" + System.identityHashCode(callsite)),
                                        new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(java.lang.invoke.CallSite.class), new BytecodeTypeRef[0]),
                                        new BytecodeAttributeInfo[0]), theSSAProgram));
                    }

                    pw.print("define internal i32 @resolvecallsite");
                    pw.print(System.identityHashCode(callsite));
                    pw.println("() {");
                    pw.println("entry:");
                    pw.print("    %value = load i32, i32* @callsite");
                    pw.println(System.identityHashCode(callsite));
                    pw.println("    %test = icmp eq i32 %value, 0");
                    pw.println("    br i1 %test, label %notinitialized, label %initialized");
                    pw.println("notinitialized:");
                    pw.print("    %initstatus = call i32  @resolvecallsite");
                    pw.print(System.identityHashCode(callsite));
                    pw.println("_factory()");
                    pw.print("    store i32 %initstatus, i32* @callsite");
                    pw.println(System.identityHashCode(callsite));
                    pw.println("    ret i32 %initstatus");
                    pw.println("initialized:");
                    pw.println("    ret i32 %value");
                    pw.println("}");
                    pw.println();

                    pw.print("define internal i32 @resolvecallsite");
                    pw.print(System.identityHashCode(callsite));
                    pw.print("_factory() ");
                    subProgram.writeDebugSuffixTo(pw);
                    pw.println(" {");

                    try (final LLVMWriter theWriter = new LLVMWriter(pw, memoryLayouter, aLinkerContext, theSymbolResolver, aOptions.isEscapeAnalysisEnabled())) {
                        theWriter.write(theEntry.getValue().owningClass, theEntry.getValue().program, subProgram);
                    }

                    pw.println("}");
                    pw.println();
                }

                // Generate bootstrap code
                attributeCounter.incrementAndGet();
                pw.print("attributes #");
                pw.print(attributeCounter.get());
                pw.println(" = { \"wasm-export-name\"=\"bootstrap\" }");
                pw.print("define void @bytecoder.bootstrap() #");
                pw.print(attributeCounter.get());
                pw.println(" {");
                pw.println("entry:");

                final String thePrimitiveClassName = LLVMWriterUtils.toClassName(BytecodeObjectTypeRef.fromRuntimeClass(Class.class));
                final NativeMemoryLayouter.MemoryLayout thePrimitiveMemoryLayout = memoryLayouter.layoutFor(BytecodeObjectTypeRef.fromRuntimeClass(Class.class));

                pw.print("    %primitiveclass_interfacedispatchptr = ptrtoint i32(i32,i32)* @");
                pw.print(thePrimitiveClassName);
                pw.print(LLVMWriter.INTERFACEDISPATCHSUFFIX);
                pw.println(" to i32");

                pw.print("    %charprimitiveruntimeClass_allocated = call i32(i32,i32,i32,i32,i32,i32,i32) @bytecoder.newRuntimeClass(i32 -1,i32 ");
                pw.print(thePrimitiveMemoryLayout.classSize());
                pw.println(",i32 -1 ,i32 -1 ,i32 %primitiveclass_interfacedispatchptr,i32 1,i32 0)");
                pw.print("    store i32 %charprimitiveruntimeClass_allocated, i32* @CharPrimitive");
                pw.println(LLVMWriter.RUNTIMECLASSSUFFIX);

                pw.print("    %intprimitiveruntimeclass_allocated = call i32(i32,i32,i32,i32,i32,i32,i32) @bytecoder.newRuntimeClass(i32 -1,i32 ");
                pw.print(thePrimitiveMemoryLayout.classSize());
                pw.println(",i32 -1 ,i32 -1 ,i32 %primitiveclass_interfacedispatchptr,i32 1,i32 0)");
                pw.print("    store i32 %intprimitiveruntimeclass_allocated, i32* @IntPrimitive");
                pw.println(LLVMWriter.RUNTIMECLASSSUFFIX);

                pw.print("    %longprimitiveruntimeclass_allocated = call i32(i32,i32,i32,i32,i32,i32,i32) @bytecoder.newRuntimeClass(i32 -1,i32 ");
                pw.print(thePrimitiveMemoryLayout.classSize());
                pw.println(",i32 -1 ,i32 -1 ,i32 %primitiveclass_interfacedispatchptr,i32 1,i32 0)");
                pw.print("    store i32 %longprimitiveruntimeclass_allocated, i32* @LongPrimitive");
                pw.println(LLVMWriter.RUNTIMECLASSSUFFIX);

                pw.print("    %byteprimitiveruntimeclass_allocated = call i32(i32,i32,i32,i32,i32,i32,i32) @bytecoder.newRuntimeClass(i32 -1,i32 ");
                pw.print(thePrimitiveMemoryLayout.classSize());
                pw.println(",i32 -1 ,i32 -1 ,i32 %primitiveclass_interfacedispatchptr,i32 1,i32 0)");
                pw.print("    store i32 %byteprimitiveruntimeclass_allocated, i32* @BytePrimitive");
                pw.println(LLVMWriter.RUNTIMECLASSSUFFIX);

                pw.print("    %floatprimitiveruntimeclass_allocated = call i32(i32,i32,i32,i32,i32,i32,i32) @bytecoder.newRuntimeClass(i32 -1,i32 ");
                pw.print(thePrimitiveMemoryLayout.classSize());
                pw.println(",i32 -1 ,i32 -1 ,i32 %primitiveclass_interfacedispatchptr,i32 1,i32 0)");
                pw.print("    store i32 %floatprimitiveruntimeclass_allocated, i32* @FloatPrimitive");
                pw.println(LLVMWriter.RUNTIMECLASSSUFFIX);

                pw.print("    %booleanprimitiveruntimeclass_allocated = call i32(i32,i32,i32,i32,i32,i32,i32) @bytecoder.newRuntimeClass(i32 -1,i32 ");
                pw.print(thePrimitiveMemoryLayout.classSize());
                pw.println(",i32 -1 ,i32 -1 ,i32 %primitiveclass_interfacedispatchptr,i32 1,i32 0)");
                pw.print("    store i32 %booleanprimitiveruntimeclass_allocated, i32* @BooleanPrimitive");
                pw.println(LLVMWriter.RUNTIMECLASSSUFFIX);

                pw.print("    %shortprimitiveruntimeclass_allocated = call i32(i32,i32,i32,i32,i32,i32,i32) @bytecoder.newRuntimeClass(i32 -1,i32 ");
                pw.print(thePrimitiveMemoryLayout.classSize());
                pw.println(",i32 -1 ,i32 -1 ,i32 %primitiveclass_interfacedispatchptr,i32 1,i32 0)");
                pw.print("    store i32 %shortprimitiveruntimeclass_allocated, i32* @ShortPrimitive");
                pw.println(LLVMWriter.RUNTIMECLASSSUFFIX);

                pw.print("    %doubleprimitiveruntimeclass_allocated = call i32(i32,i32,i32,i32,i32,i32,i32) @bytecoder.newRuntimeClass(i32 -1,i32 ");
                pw.print(thePrimitiveMemoryLayout.classSize());
                pw.println(",i32 -1 ,i32 -1 ,i32 %primitiveclass_interfacedispatchptr,i32 1,i32 0)");
                pw.print("    store i32 %doubleprimitiveruntimeclass_allocated, i32* @DoublePrimitive");
                pw.println(LLVMWriter.RUNTIMECLASSSUFFIX);

                aLinkerContext.linkedClasses().forEach(aEntry -> {

                    if (Objects.equals(aEntry.getClassName(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                        return;
                    }

                    if (aEntry.emulatedByRuntime()) {
                        return;
                    }

                    final String theClassName = LLVMWriterUtils.toClassName(aEntry.getClassName());

                    final NativeMemoryLayouter.MemoryLayout theMemoryLayout = memoryLayouter.layoutFor(aEntry.getClassName());

                    pw.print("    %");
                    pw.print(theClassName);
                    pw.print(LLVMWriter.RUNTIMECLASSSUFFIX);
                    pw.print("_classnameptr = ptrtoint i32* @");
                    pw.print(theSymbolResolver.globalFromStringPool(aEntry.getClassName().name()));
                    pw.println(" to i32");

                    pw.print("    %");
                    pw.print(theClassName);
                    pw.print(LLVMWriter.RUNTIMECLASSSUFFIX);
                    pw.print("_interfacedispatchptr = ptrtoint i32(i32,i32)* @");
                    pw.print(theClassName);
                    pw.print(LLVMWriter.INTERFACEDISPATCHSUFFIX);
                    pw.println(" to i32");

                    pw.print("    %");
                    pw.print(theClassName);
                    pw.print(LLVMWriter.RUNTIMECLASSSUFFIX);
                    pw.print("_allocated = call i32(i32,i32,i32,i32,i32,i32,i32) @bytecoder.newRuntimeClass(i32 ");
                    pw.print(aEntry.getUniqueId());
                    pw.print(",i32 ");
                    pw.print(theMemoryLayout.classSize());
                    pw.print(",i32 ");
                    final BytecodeResolvedFields theStaticFields = aEntry.resolvedFields();
                    if (null != theStaticFields.fieldByName("$VALUES")) {
                        pw.print(theMemoryLayout.offsetForClassMember("$VALUES"));
                    } else {
                        pw.print("-1");
                    }
                    pw.print(",i32 %");
                    pw.print(theClassName);
                    pw.print(LLVMWriter.RUNTIMECLASSSUFFIX);
                    pw.print("_classnameptr");

                    pw.print(",i32 %");
                    pw.print(theClassName);
                    pw.print(LLVMWriter.RUNTIMECLASSSUFFIX);
                    pw.print("_interfacedispatchptr");

                    // Is Primitive flag
                    pw.print(",i32 0");

                    // Declared field reference array, created in init method of class
                    pw.print(",i32 0");

                    pw.println(")");

                    // Store the runtime class itself
                    pw.print("    store i32 %");
                    pw.print(theClassName);
                    pw.print(LLVMWriter.RUNTIMECLASSSUFFIX);
                    pw.print("_allocated, i32* @");
                    pw.print(theClassName);
                    pw.println(LLVMWriter.RUNTIMECLASSSUFFIX);
                });

                pw.println("    %arrayvtableptr = ptrtoint %dmbcArray__vtable__type* @dmbcArray__vtable to i32");
                pw.println("    %arrayclassinit = call i32 @dmbcArray__init()");
                pw.println("    %stringclassinit = call i32 @jlString__init()");

                // We create the string pool now
                for (int i = 0; i < stringPool.size(); i++) {

                    final String theData = stringPool.get(i);
                    final int l = theData.length();
                    final int[] theDataCharacters = new int[l];
                    for (int j = 0; j < l; j++) {
                        theDataCharacters[j] = theData.charAt(j);
                    }

                    pw.print("    %allocated_");
                    pw.print(i);
                    pw.print(" = call i32 @dmbcMemoryManager_INTnewArrayINTINTINT(i32 0,i32 ");
                    pw.print(l);
                    pw.println(",i32 %arrayclassinit,i32 %arrayvtableptr)");

                    for (int j = 0; j < l; j++) {
                        pw.print("    %offset_" + i + "_" + j);
                        pw.print(" = add i32 %allocated_");
                        pw.print(i);
                        pw.print(", ");
                        pw.println(20 + j * 8);

                        pw.print("    %offset_" + i + "_" + j + "_ptr");
                        pw.print(" = inttoptr i32 ");
                        pw.print("%offset_" + i + "_" + j);
                        pw.println(" to i32 *");

                        pw.print("    store i32 ");
                        pw.print(theDataCharacters[j]);
                        pw.print(", i32* ");
                        pw.println("%offset_" + i + "_" + j + "_ptr");
                    }

                    pw.print("    %string_");
                    pw.print(i);
                    pw.print(" = call i32 @jlString_VOID$newInstanceA1BYTEBYTE(i32 %stringclassinit,i32 ");
                    pw.print("%allocated_");
                    pw.print(i);
                    pw.println(", i32 0)");

                    pw.print("    store i32 %string_");
                    pw.print(i);
                    pw.print(", i32* @");
                    pw.print("strpool_");
                    pw.println(i);
                }

                // Some initialization logic
                aLinkerContext.linkedClasses()
                    .filter(t -> t.getClassName().name().equals(VM.class.getName()) ||
                            t.getClassName().name().equals(Quicksort.class.getName())).forEach(theClass -> {
                        pw.print("    %");
                        pw.print(LLVMWriterUtils.runtimeClassVariableName(theClass.getClassName()));
                        pw.print(" = call i32 @");
                        pw.print(LLVMWriterUtils.toClassName(theClass.getClassName()));
                        pw.print(LLVMWriter.CLASSINITSUFFIX);
                        pw.println("()");
                    });

                pw.println("    ret void");
                pw.println("}");
                pw.println();

                // We create the string pool now
                for (int i = 0; i < stringPool.size(); i++) {
                    pw.print("@");
                    pw.print("strpool_");
                    pw.print(i);
                    pw.println(" = private global i32 0");
                }
                pw.println();

                // Generate code for lambda vtables
                // Lambdas are referenced by method types, hence we use the known methodtypes to generate the code
                pw.println("define internal i32 @dynamicvtable(i32 %runtimeClass, i32 %implementationMethod, i32 %interfaceDispatch) {");
                pw.println("entry:");
                for (int j=0;j<theMethodTypes.size();j++) {
                    final BytecodeMethodSignature theSignature = theMethodTypes.get(j);
                    final BytecodeTypeRef theReturnType = theSignature.getReturnType();
                    if (!theReturnType.isArray() && !theReturnType.isVoid() && ! theReturnType.isPrimitive()) {
                        // Return type is object
                        final BytecodeObjectTypeRef theObjectTypeRef = (BytecodeObjectTypeRef) theReturnType;
                        final BytecodeLinkedClass theClass = aLinkerContext.resolveClass(theObjectTypeRef);
                        //
                        // Maybe we can go further and generate code only for classes
                        // used by lambda expressions as described by the class tags here
                        //
                        if (theClass.getBytecodeClass().getAccessFlags().isAbstract() || theClass.getBytecodeClass().getAccessFlags().isInterface()) {
                            pw.print("    %cl_");
                            pw.print(j);
                            pw.print(" = call i32 @");
                            pw.print(LLVMWriterUtils.toClassName(theObjectTypeRef));
                            pw.print(LLVMWriter.CLASSINITSUFFIX);
                            pw.println("()");

                            pw.print("    %te_");
                            pw.print(j);
                            pw.print(" = icmp eq i32 %runtimeClass, %cl_");
                            pw.print(j);
                            pw.println();

                            pw.print("    br i1 %te_");
                            pw.print(j);
                            pw.print(", label %te_");
                            pw.print(j);
                            pw.print("_ok, label %te_");
                            pw.print(j);
                            pw.println("_not_ok");

                            pw.print("te_");
                            pw.print(j);
                            pw.println("_ok:");

                            // We fill in the vtable data
                            final BytecodeVTable theTable = theSymbolResolver.vtableFor(theClass);
                            final List<BytecodeVTable.Slot> theSlots = theTable.sortedSlots();
                            final int theNumberOfSlots = theTable.numberOfSlots();

                            pw.print("    %memory_");
                            pw.print(j);
                            pw.print(" = call i32 @dmbcMemoryManager_INTmallocINT(i32 undef,i32 ");
                            pw.print(8 + theNumberOfSlots * 4);
                            pw.println(")");

                            // First position is the instanceof function
                            pw.print("    %te_");
                            pw.print(j);
                            pw.print("_0");
                            pw.print(" = inttoptr i32 %memory_");
                            pw.print(j);
                            pw.println(" to i32*");
                            pw.print("    %te_");
                            pw.print(j);
                            pw.print("_in");
                            pw.print(" = ptrtoint i1(i32,i32)* @");
                            pw.print(LLVMWriterUtils.toClassName(theObjectTypeRef));
                            pw.print(LLVMWriter.INSTANCEOFSUFFIX);
                            pw.println(" to i32");

                            pw.print("    store i32 %te_");
                            pw.print(j);
                            pw.print("_in");
                            pw.print(", i32* %te_");
                            pw.print(j);
                            pw.println("_0");

                            // Second position is interface dispatch
                            pw.print("    %te_");
                            pw.print(j);
                            pw.print("_00_offset = add i32 4, %memory_");
                            pw.println(j);
                            pw.print("    %te_");
                            pw.print(j);
                            pw.print("_00");
                            pw.print(" = inttoptr i32 %te_");
                            pw.print(j);
                            pw.println("_00_offset to i32*");
                            pw.print("    store i32 %interfaceDispatch, i32* %te_");
                            pw.print(j);
                            pw.println("_00");

                            for (final BytecodeVTable.Slot sl : theSlots) {
                                final BytecodeVTable.VPtr ptr = theTable.slot(sl);
                                pw.println("    ;; slot " + sl.getPos() + ", " + ptr.getMethodName() + ", " + ptr.getSignature());
                                pw.print("    %te_");
                                pw.print(j);
                                pw.print("_offset_");
                                pw.print(1 + sl.getPos());
                                pw.print(" = add i32 %memory_");
                                pw.print(j);
                                pw.print(", ");
                                pw.println(8 + sl.getPos() * 4);

                                pw.print("    %te_");
                                pw.print(j);
                                pw.print("_ptr_");
                                pw.print(1 + sl.getPos());
                                pw.print(" = inttoptr i32 %te_");
                                pw.print(j);
                                pw.print("_offset_");
                                pw.print(1 + sl.getPos());
                                pw.println(" to i32*");

                                if (ptr.getImplementingClass() != null) {
                                    final BytecodeLinkedClass theLinkedClass = aLinkerContext.resolveClass(ptr.getImplementingClass());
                                    final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull(ptr.getMethodName(), ptr.getSignature());
                                    if (theMethod != null && !theMethod.getAccessFlags().isAbstract()) {
                                        pw.print("    %te_");
                                        pw.print(j);
                                        pw.print("_impl_");
                                        pw.print(1 + sl.getPos());
                                        pw.print(" = ptrtoint ");
                                        pw.print(LLVMWriterUtils.toSignature(ptr.getSignature()));
                                        pw.print("* @");
                                        pw.print(LLVMWriterUtils.toMethodName(ptr.getImplementingClass(), ptr.getMethodName(), ptr.getSignature()));
                                        pw.println(" to i32");

                                        pw.print("    store i32 %te_");
                                        pw.print(j);
                                        pw.print("_impl_");
                                        pw.print(1 + sl.getPos());
                                        pw.print(", i32* %te_");
                                        pw.print(j);
                                        pw.print("_ptr_");
                                        pw.println(1 + sl.getPos());
                                    } else {
                                        pw.print("    store i32 %implementationMethod, i32* ");
                                        pw.print("%te_");
                                        pw.print(j);
                                        pw.print("_ptr_");
                                        pw.println(1 + sl.getPos());
                                    }
                                } else {
                                    pw.print("    store i32 %implementationMethod, i32* ");
                                    pw.print("%te_");
                                    pw.print(j);
                                    pw.print("_ptr_");
                                    pw.println(1 + sl.getPos());
                                }
                            }

                            pw.print("    ret i32 %memory_");
                            pw.println(j);

                            pw.print("te_");
                            pw.print(j);
                            pw.println("_not_ok:");
                        }
                    }
                }

                pw.println("    call void @llvm.trap()");
                pw.println("    unreachable");
                pw.println("}");
                pw.println();

                // Factory for method types
                for (int j=0;j<theMethodTypes.size();j++) {

                    final BytecodeMethodSignature theSignature = theMethodTypes.get(j);

                    pw.print("define internal i32 @methodtypefactory");
                    pw.print(j);
                    pw.println("() {");
                    pw.println("entry:");

                    pw.println("    %data_classinit = call i32 @dmbcArray__init()");
                    pw.println("    %data_vtable = ptrtoint %dmbcArray__vtable__type* @dmbcArray__vtable to i32");
                    pw.print("    %data = call i32 @dmbcMemoryManager_INTnewArrayINTINTINT(i32 0,i32 ");
                    pw.print(1 + theSignature.getArguments().length);
                    pw.println(",i32 %data_classinit,i32 %data_vtable)");

                    final java.util.function.BiFunction<BytecodeTypeRef, Integer, Void> theAdder = (aType, aIndex) -> {
                        final int offset = 20 + aIndex * 4;

                        pw.print("    %data_");
                        pw.print(aIndex);
                        pw.print(" = add i32 %data, ");
                        pw.println(offset);

                        pw.print("    %data_");
                        pw.print(aIndex);
                        pw.print("_ptr = inttoptr i32 %data_");
                        pw.print(aIndex);
                        pw.println(" to i32*");

                        if (aType.isPrimitive()) {
                            final TypeRef.Native theNativeType = (TypeRef.Native) TypeRef.toType(aType);
                            // Negative number to indicate it is a primitive type
                            pw.print("    store i32 ");
                            pw.print(-theNativeType.ordinal());
                            pw.print(", i32* %data_");
                            pw.print(aIndex);
                            pw.println("_ptr");
                        } else {
                            // Positive number with the id of the class
                            if (aType.isArray()) {
                                final BytecodeLinkedClass theLinkedClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Array.class));
                                pw.print("    %status");
                                pw.print(aIndex);
                                pw.print(" = call i32 @");
                                pw.print(LLVMWriterUtils.toClassName(theLinkedClass.getClassName()));
                                pw.print(LLVMWriter.CLASSINITSUFFIX);
                                pw.println("()");

                                pw.print("    store i32 %status");
                                pw.print(aIndex);
                                pw.print(", i32* %data_");
                                pw.print(aIndex);
                                pw.println("_ptr");
                            } else {
                                final BytecodeLinkedClass theLinkedClass = aLinkerContext.resolveClass((BytecodeObjectTypeRef) aType);
                                pw.print("    %status");
                                pw.print(aIndex);
                                pw.print(" = call i32 @");
                                pw.print(LLVMWriterUtils.toClassName(theLinkedClass.getClassName()));
                                pw.print(LLVMWriter.CLASSINITSUFFIX);
                                pw.println("()");

                                pw.print("    store i32 %status");
                                pw.print(aIndex);
                                pw.print(", i32* %data_");
                                pw.print(aIndex);
                                pw.println("_ptr");
                            }
                        }
                        return null;
                    };
                    // Return type and arguments
                    theAdder.apply(theSignature.getReturnType(), 0);
                    for (int i=0;i<theSignature.getArguments().length;i++) {
                        final BytecodeTypeRef theArgument = theSignature.getArguments()[i];
                        theAdder.apply(theArgument, i + 1);
                    }

                    pw.println("    ret i32 %data");
                    pw.println("}");
                    pw.println();
                }

                // Generate method handle delegate functions here
                for (int i=0;i<methodHandles.size();i++) {

                    final MethodHandleExpression theMethodHandle = methodHandles.get(i);

                    final String theDelegateMethodName = "handle" + i;

                    pw.println(";; refkind = " + theMethodHandle.getReferenceKind());
                    pw.println(";; linksignature = " + theMethodHandle.getAdapterAnnotation().getLinkageSignature());
                    pw.println(";; capturesignature = " + theMethodHandle.getAdapterAnnotation().getCaptureSignature());
                    pw.println(";; samsignature = " + theMethodHandle.getAdapterAnnotation().getSamMethodType());
                    pw.println(";; implclass = " + theMethodHandle.getClassName().name());
                    pw.println(";; implMethod = " + theMethodHandle.getMethodName());
                    pw.println(";; implSig = " + theMethodHandle.getImplementationSignature());

                    switch (theMethodHandle.getReferenceKind()) {
                        case REF_invokeStatic:
                            writeMethodHandleDelegateInvokeStatic(aLinkerContext, theMethodHandle, theDelegateMethodName, pw);
                            break;
                        case REF_invokeVirtual:
                            writeMethodHandleDelegateInvokeVirtual(aLinkerContext, theMethodHandle, theDelegateMethodName, pw);
                            break;
                        case REF_invokeInterface:
                            writeMethodHandleDelegateInvokeInterface(aLinkerContext, theMethodHandle, theDelegateMethodName, pw);
                            break;
                        case REF_invokeSpecial:
                            writeMethodHandleDelegateInvokeSpecial(aLinkerContext, theMethodHandle, theDelegateMethodName, pw);
                            break;
                        case REF_newInvokeSpecial:
                            writeMethodHandleDelegateNewInvokeSpecial(aLinkerContext, theMethodHandle, theDelegateMethodName, pw);
                            break;
                        default:
                            throw new IllegalArgumentException("Not supported refkind for method handle " + theMethodHandle.getReferenceKind());
                    }
                }

                // We need to generate the callbacks
                aLinkerContext.linkedClasses().filter(t -> t.isCallback() && t.getBytecodeClass().getAccessFlags().isInterface()).forEach(t -> {

                    final BytecodeResolvedMethods theMethods = aLinkerContext.resolveMethods(t);
                    final List<BytecodeMethod> availableCallbacks = theMethods.stream().filter(x -> !x.getValue().isConstructor() && !x.getValue().isClassInitializer()
                            && x.getProvidingClass() == t).map(BytecodeResolvedMethods.MethodEntry::getValue).collect(Collectors.toList());

                    if (availableCallbacks.size() > 0) {

                        if (availableCallbacks.size() != 1) {
                            throw new IllegalStateException(
                                    "Invalid number of callback methods available for type " + t.getClassName().name()
                                            + ", expected 1, got " + availableCallbacks.size());
                        }

                        final BytecodeMethod theDelegateMethod = availableCallbacks.get(0);

                        final BytecodeVTable theTable = theSymbolResolver.vtableFor(t);
                        final BytecodeVTable.Slot sl = theTable.slotOf(theDelegateMethod.getName().stringValue(), theDelegateMethod.getSignature());

                        final String theFunctionName = LLVMWriterUtils
                                .toMethodName(t.getClassName(), theDelegateMethod.getName(), theDelegateMethod.getSignature());

                        attributeCounter.incrementAndGet();
                        pw.print("attributes #");
                        pw.print(attributeCounter.get());
                        pw.print(" = { \"wasm-export-name\"=\"");
                        pw.print(theFunctionName);
                        pw.println("\" }");
                        pw.print("define void @");
                        pw.print(theFunctionName);
                        pw.print("(i32 %target");
                        for (int i = 0; i < theDelegateMethod.getSignature().getArguments().length; i++) {
                            pw.print(",");
                            pw.print(LLVMWriterUtils.toType(TypeRef.toType(theDelegateMethod.getSignature().getArguments()[i])));
                            pw.print(" %param");
                            pw.print(i);
                        }
                        pw.print(") #");
                        pw.print(attributeCounter.get());
                        pw.println(" {");

                        pw.println("    %ptr = add i32 %target, 4");
                        pw.println("    %ptr_ptr = inttoptr i32 %ptr to i32*");
                        pw.println("    %ptr_loaded = load i32, i32* %ptr_ptr");
                        pw.print("    %vtable = add i32 %ptr_loaded, ");
                        pw.print(8 + (sl.getPos() * 4));
                        pw.println("    %vtable_ptr = inttoptr i32 %vtable to i32*");
                        pw.println("    %resolved = load i32, i32* %vtable_ptr");
                        pw.print("    %resolved_ptr = inttoptr i32 %resolved to ");
                        pw.print(LLVMWriterUtils.toSignature(theDelegateMethod.getSignature()));
                        pw.println("*");
                        pw.print("    call ");
                        pw.print(LLVMWriterUtils.toSignature(theDelegateMethod.getSignature()));
                        pw.print(" %resolved_ptr (i32 %target");
                        for (int i = 0; i < theDelegateMethod.getSignature().getArguments().length; i++) {
                            pw.print(",");
                            pw.print(LLVMWriterUtils.toType(TypeRef.toType(theDelegateMethod.getSignature().getArguments()[i])));
                            pw.print(" %param");
                            pw.print(i);
                        }
                        pw.println(")");
                        pw.println("    ret void");
                        pw.println("}");
                    }
                });


                // finally we write the debug information
                debugInformation.writeHeaderTo(pw);
            }

            try (final Reader reader = new InputStreamReader(new FileInputStream(theLLFile))) {
                final String theLLContent = IOUtils.toString(reader);
                theCompileResult.add(new CompileResult.StringContent(aOptions.getFilenamePrefix() + ".ll", theLLContent));
            }

            final StringWriter theJSCode = new StringWriter();
            try (final PrintWriter theWriter = new PrintWriter(theJSCode)) {

                theWriter.println("var bytecoder = {");
                theWriter.println();
                theWriter.println("     runningInstance: undefined,");
                theWriter.println("     runningInstanceMemory: undefined,");
                theWriter.println("     exports: undefined,");
                theWriter.println("     referenceTable: ['EMPTY'],");
                theWriter.println("     callbacks: [],");
                theWriter.println("     filehandles: [],");
                theWriter.println();

                theWriter.println("     openForRead: function(path) {");
                theWriter.println("         try {");
                theWriter.println("             var request = new XMLHttpRequest();");
                theWriter.println("             request.open('GET',path,false);");
                theWriter.println("             request.overrideMimeType('text\\/plain; charset=x-user-defined');");
                theWriter.println("             request.send(null);");
                theWriter.println("             if (request.status == 200) {");
                theWriter.println("                var length = request.getResponseHeader('content-length');");
                theWriter.println("                var responsetext = request.response;");
                theWriter.println("                var buf = new ArrayBuffer(responsetext.length);");
                theWriter.println("                var bufView = new Uint8Array(buf);");
                theWriter.println("                for (var i=0, strLen=responsetext.length; i<strLen; i++) {");
                theWriter.println("                    bufView[i] = responsetext.charCodeAt(i) & 0xff;");
                theWriter.println("                }");
                theWriter.println("                var handle = bytecoder.filehandles.length;");
                theWriter.println("                bytecoder.filehandles[handle] = {");
                theWriter.println("                    currentpos: 0,");
                theWriter.println("                    data: bufView,");
                theWriter.println("                    size: length,");
                theWriter.println("                    skip0INTINT: function(handle,amount) {");
                theWriter.println("                        var remaining = this.size - this.currentpos;");
                theWriter.println("                        var possible = Math.min(remaining, amount);");
                theWriter.println("                        this.currentpos+=possible;");
                theWriter.println("                        return possible;");
                theWriter.println("                    },");
                theWriter.println("                    available0INT: function(handle) {");
                theWriter.println("                        return this.size - this.currentpos;");
                theWriter.println("                    },");
                theWriter.println("                    read0INT: function(handle) {");
                theWriter.println("                        return this.data[this.currentpos++];");
                theWriter.println("                    },");
                theWriter.println("                    readBytesINTL1BYTEINTINT: function(handle,target,offset,length) {");
                theWriter.println("                        if (length === 0) {");
                theWriter.println("                            return 0;");
                theWriter.println("                        }");
                theWriter.println("                        var remaining = this.size - this.currentpos;");
                theWriter.println("                        var possible = Math.min(remaining, length);");
                theWriter.println("                        if (possible === 0) {");
                theWriter.println("                            return -1;");
                theWriter.println("                        }");
                theWriter.println("                        for (var j=0;j<possible;j++) {");
                theWriter.println("                            bytecoder.runningInstanceMemory[target + 20 + offset * 8]=this.data[this.currentpos++];");
                theWriter.println("                            offset++;");
                theWriter.println("                        }");
                theWriter.println("                        return possible;");
                theWriter.println("                    }");
                theWriter.println("                };");
                theWriter.println("                return handle;");
                theWriter.println("            }");
                theWriter.println("            return -1;");
                theWriter.println("         } catch(e) {");
                theWriter.println("             return -1;");
                theWriter.println("         }");
                theWriter.println("     },");
                theWriter.println();

                theWriter.println("     init: function(instance) {");
                theWriter.println("         bytecoder.runningInstance = instance;");
                theWriter.println("         bytecoder.runningInstanceMemory = new Uint8Array(instance.exports.memory.buffer);");
                theWriter.println("         bytecoder.exports = instance.exports;");
                theWriter.println("     },");
                theWriter.println();

                theWriter.println("     initializeFileIO: function() {");
                theWriter.println("         var stddin = {");
                theWriter.println("         };");
                theWriter.println("         var stdout = {");
                theWriter.println("             buffer: \"\",");
                theWriter.println("             writeBytesINTL1BYTEINTINT: function(handle, data, offset, length) {");
                theWriter.println("                 if (length > 0) {");
                theWriter.println("                     var array = new Uint8Array(length);");
                theWriter.println("                     data+=20;");
                theWriter.println("                     for (var i = 0; i < length; i++) {");
                theWriter.println("                         array[i] = bytecoder.intInMemory(data);");
                theWriter.println("                         data+=8;");
                theWriter.println("                     }");
                theWriter.println("                     var asstring = String.fromCharCode.apply(null, array);");
                theWriter.println("                     for (var i=0;i<asstring.length;i++) {");
                theWriter.println("                         var c = asstring.charAt(i);");
                theWriter.println("                         if (c == '\\n') {");
                theWriter.println("                             console.log(stdout.buffer);");
                theWriter.println("                             stdout.buffer=\"\";");
                theWriter.println("                         } else {");
                theWriter.println("                             stdout.buffer = stdout.buffer.concat(c);");
                theWriter.println("                         }");
                theWriter.println("                     }");
                theWriter.println("                 }");
                theWriter.println("             },");
                theWriter.println("             close0INT: function(handle) {");
                theWriter.println("             },");
                theWriter.println("             writeIntINTINT: function(handle,value) {");
                theWriter.println("                 var c = String.fromCharCode(value);");
                theWriter.println("                 if (c == '\\n') {");
                theWriter.println("                     console.log(stdout.buffer);");
                theWriter.println("                     stdout.buffer=\"\";");
                theWriter.println("                 } else {");
                theWriter.println("                     stdout.buffer = stdout.buffer.concat(c);");
                theWriter.println("                 }");
                theWriter.println("             }");
                theWriter.println("         };");
                theWriter.println("         bytecoder.filehandles[0] = stddin;");
                theWriter.println("         bytecoder.filehandles[1] = stdout;");
                theWriter.println("         bytecoder.filehandles[2] = stdout;");
                theWriter.println("         bytecoder.exports.initDefaultFileHandles(-1, 0,1,2);");
                theWriter.println("     },");
                theWriter.println();

                theWriter.println("     intInMemory: function(value) {");
                theWriter.println("         return bytecoder.runningInstanceMemory[value]");
                theWriter.println("                + (bytecoder.runningInstanceMemory[value + 1] * 256)");
                theWriter.println("                + (bytecoder.runningInstanceMemory[value + 2] * 256 * 256)");
                theWriter.println("                + (bytecoder.runningInstanceMemory[value + 3] * 256 * 256 * 256);");
                theWriter.println("     },");

                final BytecodeLinkedClass theStringClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(String.class));
                final int theStringDataOffset = memoryLayouter.layoutFor(theStringClass.getClassName()).offsetForInstanceMember("value");

                theWriter.println();
                theWriter.println("     toJSString: function(value) {");
                theWriter.println("         var theByteArray = bytecoder.intInMemory(value + " + theStringDataOffset + ");");
                theWriter.println("         var theData = bytecoder.byteArraytoJSString(theByteArray);");
                theWriter.println("         return theData;");
                theWriter.println("     },");
                theWriter.println();

                theWriter.println("     byteArraytoJSString: function(value) {");
                theWriter.println("         var theLength = bytecoder.intInMemory(value + 16);");
                theWriter.println("         var theData = '';");
                theWriter.println("         value = value + 20;");
                theWriter.println("         for (var i=0;i<theLength;i++) {");
                theWriter.println("             var theCharCode = bytecoder.intInMemory(value);");
                theWriter.println("             value = value + 8;");
                theWriter.println("             theData+= String.fromCharCode(theCharCode);");
                theWriter.println("         }");
                theWriter.println("         return theData;");
                theWriter.println("     },");
                theWriter.println();

                theWriter.println("     toBytecoderReference: function(value) {");
                theWriter.println("         var index = bytecoder.referenceTable.indexOf(value);");
                theWriter.println("         if (index>=0) {");
                theWriter.println("             return index;");
                theWriter.println("         }");
                theWriter.println("         bytecoder.referenceTable.push(value);");
                theWriter.println("         return bytecoder.referenceTable.length - 1;");
                theWriter.println("     },");
                theWriter.println();

                theWriter.println("     toJSReference: function(value) {");
                theWriter.println("         return bytecoder.referenceTable[value];");
                theWriter.println("     },");
                theWriter.println();

                theWriter.println("     toBytecoderString: function(value) {");
                theWriter.println("         var newArray = bytecoder.exports.newByteArray(0, value.length);");
                theWriter.println("         for (var i=0;i<value.length;i++) {");
                theWriter.println("             bytecoder.exports.setByteArrayEntry(0,newArray,i,value.charCodeAt(i));");
                theWriter.println("         }");
                theWriter.println("         return bytecoder.exports.newStringUTF8(0, newArray);");
                theWriter.println("     },");
                theWriter.println();

                theWriter.println("     registerCallback: function(ptr,callback) {");
                theWriter.println("         bytecoder.callbacks.push(ptr);");
                theWriter.println("         return callback;");
                theWriter.println("     },");
                theWriter.println();

                theWriter.println("     imports: {");
                theWriter.println("         stringutf16: {");
                theWriter.println("             isBigEndian: function() {return 1;},");
                theWriter.println("         },");
                theWriter.println("         env: {");
                theWriter.println("             fmodf: function(f1,f2) {return f1 % f2;},");
                theWriter.println("             fmod: function(f1,f2) {return f1 % f2;},");
                theWriter.println("             debug: function(thisref, f1) {console.log(f1);}");
                theWriter.println("         },");
                theWriter.println("         system: {");
                theWriter.println("             currentTimeMillis: function() {return Date.now();},");
                theWriter.println("             nanoTime: function() {return Date.now() * 1000000;},");
                theWriter.println("         },");
                theWriter.println("         vm: {");
                theWriter.println("             newLambdaStaticInvocationStringMethodTypeMethodHandleObject: function() {},");
                theWriter.println("             newLambdaConstructorInvocationMethodTypeMethodHandleObject: function() {},");
                theWriter.println("             newLambdaInterfaceInvocationMethodTypeMethodHandleObject: function() {},");
                theWriter.println("             newLambdaVirtualInvocationMethodTypeMethodHandleObject: function() {},");
                theWriter.println("             newLambdaSpecialInvocationMethodTypeMethodHandleObject: function() {},");
                theWriter.println("         },");
                theWriter.println("         memorymanager: {");
                theWriter.println("             logINT: function(thisref, value) {");
                theWriter.println("                     console.log('Log : ' + value);");
                theWriter.println("             },");
                theWriter.println("             logAllocationDetailsINTINTINT: function(thisref, current, prev, next) {");
                theWriter.println("                     if (prev != 0) console.log('m_' + current + ' -> m_' + prev + '[label=\"Prev\"]');");
                theWriter.println("                     if (next != 0) console.log('m_' + current + ' -> m_' + next + '[label=\"Next\"]');");
                theWriter.println("             },");
                theWriter.println("             isUsedAsCallbackINT : function(thisref, ptr) {");
                theWriter.println("                 return bytecoder.callbacks.includes(ptr);");
                theWriter.println("             },");
                theWriter.println("             printObjectDebugInternalObjectINTINTBOOLEANBOOLEAN: function(thisref, ptr, indexAlloc, indexFree, usedByStack, usedByHeap) {");
                theWriter.println("                 console.log('Memory debug for ' + ptr);");
                theWriter.println("                 var theAllocatedBlock = ptr - 16;");
                theWriter.println("                 var theSize = bytecoder.intInMemory(theAllocatedBlock);");
                theWriter.println("                 var theNext = bytecoder.intInMemory(theAllocatedBlock +  4);");
                theWriter.println("                 var theSurvivorCount = bytecoder.intInMemory(theAllocatedBlock +  8);");
                theWriter.println("                 console.log(' Allocation starts at '+ theAllocatedBlock);");
                theWriter.println("                 console.log(' Size = ' + theSize + ', Next = ' + theNext);");
                theWriter.println("                 console.log(' GC survivor count        : ' + theSurvivorCount);");
                theWriter.println("                 console.log(' Index in allocation list : ' + indexAlloc);");
                theWriter.println("                 console.log(' Index in free list       : ' + indexFree);");
                theWriter.println("                 console.log(' Used by STACK            : ' + usedByStack);");
                theWriter.println("                 console.log(' Used by HEAP             : ' + usedByHeap);");
                theWriter.println("                 for (var i=0;i<theSize;i+=4) {");
                theWriter.println("                     console.log(' Memory offset +' + i + ' = ' + bytecoder.intInMemory( theAllocatedBlock + i));");
                theWriter.println("                 }");
                theWriter.println("             }");
                theWriter.println("         },");
                theWriter.println("         opaquearrays : {");
                theWriter.println("             createIntArrayINT: function(thisref, p1) {");
                theWriter.println("                 return bytecoder.toBytecoderReference(new Int32Array(p1));");
                theWriter.println("             },");
                theWriter.println("             createFloatArrayINT: function(thisref, p1) {");
                theWriter.println("                 return bytecoder.toBytecoderReference(new Float32Array(p1));");
                theWriter.println("             },");
                theWriter.println("             createObjectArray: function(thisref) {");
                theWriter.println("                 return bytecoder.toBytecoderReference([]);");
                theWriter.println("             },");
                theWriter.println("             createInt8ArrayINT: function(thisref, p1) {");
                theWriter.println("                 return bytecoder.toBytecoderReference(new Int8Array(p1));");
                theWriter.println("             },");
                theWriter.println("             createInt16ArrayINT: function(thisref, p1) {");
                theWriter.println("                 return bytecoder.toBytecoderReference(new Int16Array(p1));");
                theWriter.println("             },");
                theWriter.println("         },");

                theWriter.println("         float : {");
                theWriter.println("             floatToRawIntBitsFLOAT : function(thisref,value) {");
                theWriter.println("                 var fl = new Float32Array(1);");
                theWriter.println("                 fl[0] = value;");
                theWriter.println("                 var br = new Int32Array(fl.buffer);");
                theWriter.println("                 return br[0];");
                theWriter.println("             },");
                theWriter.println("             intBitsToFloatINT : function(thisref,value) {");
                theWriter.println("                 var fl = new Int32Array(1);");
                theWriter.println("                 fl[0] = value;");
                theWriter.println("                 var br = new Float32Array(fl.buffer);");
                theWriter.println("                 return br[0];");
                theWriter.println("             },");
                theWriter.println("         },");

                theWriter.println("         double : {");
                theWriter.println("             doubleToRawLongBitsDOUBLE : function(thisref, value) {");
                theWriter.println("                 var fl = new Float64Array(1);");
                theWriter.println("                 fl[0] = value;");
                theWriter.println("                 var br = new BigInt64Array(fl.buffer);");
                theWriter.println("                 return br[0];");
                theWriter.println("             },");
                theWriter.println("             longBitsToDoubleLONG : function(thisref, value) {");
                theWriter.println("                 var fl = new BigInt64Array(1);");
                theWriter.println("                 fl[0] = value;");
                theWriter.println("                 var br = new Float64Array(fl.buffer);");
                theWriter.println("                 return br[0];");
                theWriter.println("             },");
                theWriter.println("         },");

                theWriter.println("         math: {");
                theWriter.println("             floorDOUBLE: function (thisref, p1) {return Math.floor(p1);},");
                theWriter.println("             ceilDOUBLE: function (thisref, p1) {return Math.ceil(p1);},");
                theWriter.println("             sinDOUBLE: function (thisref, p1) {return Math.sin(p1);},");
                theWriter.println("             cosDOUBLE: function  (thisref, p1) {return Math.cos(p1);},");
                theWriter.println("             tanDOUBLE: function  (thisref, p1) {return Math.tan(p1);},");
                theWriter.println("             roundDOUBLE: function  (thisref, p1) {return Math.round(p1);},");
                theWriter.println("             sqrtDOUBLE: function(thisref, p1) {return Math.sqrt(p1);},");
                theWriter.println("             cbrtDOUBLE: function(thisref, p1) {return Math.cbrt(p1);},");
                theWriter.println("             add: function(thisref, p1, p2) {return p1 + p2;},");
                theWriter.println("             maxLONGLONG: function(thisref, p1, p2) { return Math.max(p1, p2);},");
                theWriter.println("             maxDOUBLEDOUBLE: function(thisref, p1, p2) { return Math.max(p1, p2);},");
                theWriter.println("             maxINTINT: function(thisref, p1, p2) { return Math.max(p1, p2);},");
                theWriter.println("             maxFLOATFLOAT: function(thisref, p1, p2) { return Math.max(p1, p2);},");
                theWriter.println("             minFLOATFLOAT: function(thisref, p1, p2) { return Math.min(p1, p2);},");
                theWriter.println("             minINTINT: function(thisref, p1, p2) { return Math.min(p1, p2);},");
                theWriter.println("             minLONGLONG: function(thisref, p1, p2) { return Math.min(p1, p2);},");
                theWriter.println("             minDOUBLEDOUBLE: function(thisref, p1, p2) { return Math.min(p1, p2);},");
                theWriter.println("             toRadiansDOUBLE: function(thisref, p1) {");
                theWriter.println("                 return p1 * (Math.PI / 180);");
                theWriter.println("             },");
                theWriter.println("             toDegreesDOUBLE: function(thisref, p1) {");
                theWriter.println("                 return p1 * (180 / Math.PI);");
                theWriter.println("             },");
                theWriter.println("             random: function(thisref) { return Math.random();},");
                theWriter.println("             logDOUBLE: function (thisref, p1) {return Math.log(p1);},");
                theWriter.println("             powDOUBLEDOUBLE: function (thisref, p1, p2) {return Math.pow(p1, p2);},");
                theWriter.println("             acosDOUBLE: function (thisref, p1, p2) {return Math.acos(p1);},");
                theWriter.println("             atan2DOUBLE: function (thisref, p1, p2) {return Math.atan2(p1);},");
                theWriter.println("         },");
                theWriter.println("         strictmath: {");
                theWriter.println("             floorDOUBLE: function (thisref, p1) {return Math.floor(p1);},");
                theWriter.println("             ceilDOUBLE: function (thisref, p1) {return Math.ceil(p1);},");
                theWriter.println("             sinDOUBLE: function (thisref, p1) {return Math.sin(p1);},");
                theWriter.println("             cosDOUBLE: function  (thisref, p1) {return Math.cos(p1);},");
                theWriter.println("             roundFLOAT: function  (thisref, p1) {return Math.round(p1);},");
                theWriter.println("             sqrtDOUBLE: function(thisref, p1) {return Math.sqrt(p1);},");
                theWriter.println("             atan2DOUBLEDOUBLE: function(thisref, p1) {return Math.sqrt(p1);},");
                theWriter.println("         },");
                theWriter.println("         runtime: {");
                theWriter.println("             nativewindow: function(caller) {return bytecoder.toBytecoderReference(window);},");
                theWriter.println("             nativeconsole: function(caller) {return bytecoder.toBytecoderReference(console);},");
                theWriter.println("         },");

                theWriter.println("         unixfilesystem :{");
                theWriter.println("             getBooleanAttributes0String : function(thisref,path) {");
                theWriter.println("                 var jsPath = bytecoder.toJSString(path);");
                theWriter.println("                 try {");
                theWriter.println("                     var request = new XMLHttpRequest();");
                theWriter.println("                     request.open('HEAD',jsPath,false);");
                theWriter.println("                     request.send(null);");
                theWriter.println("                     if (request.status == 200) {");
                theWriter.println("                         var length = request.getResponseHeader('content-length');");
                theWriter.println("                         return 0x01;");
                theWriter.println("                     }");
                theWriter.println("                     return 0;");
                theWriter.println("                 } catch(e) {");
                theWriter.println("                     return 0;");
                theWriter.println("                 }");
                theWriter.println("             },");
                theWriter.println("         },");

                theWriter.println("         nullpointerexception : {");
                theWriter.println("             getExtendedNPEMessage : function(thisref) {");
                theWriter.println("                 return 0;");
                theWriter.println("             },");
                theWriter.println("         },");

                theWriter.println("         fileoutputstream : {");
                theWriter.println("             writeBytesINTL1BYTEINTINT : function(thisref, handle, data, offset, length) {");
                theWriter.println("                 bytecoder.filehandles[handle].writeBytesINTL1BYTEINTINT(handle,data,offset,length);");
                theWriter.println("             },");
                theWriter.println("             writeIntINTINT : function(thisref, handle, intvalue) {");
                theWriter.println("                 bytecoder.filehandles[handle].writeIntINTINT(handle,intvalue);");
                theWriter.println("             },");
                theWriter.println("             close0INT : function(thisref,handle) {");
                theWriter.println("                 bytecoder.filehandles[handle].close0INT(handle);");
                theWriter.println("             },");
                theWriter.println("         },");

                theWriter.println("         fileinputstream : {");
                theWriter.println("             open0String : function(thisref,name) {");
                theWriter.println("                 return bytecoder.openForRead(bytecoder.toJSString(name));");
                theWriter.println("             },");
                theWriter.println("             read0INT : function(thisref,handle) {");
                theWriter.println("                 return bytecoder.filehandles[handle].read0INT(handle);");
                theWriter.println("             },");
                theWriter.println("             readBytesINTL1BYTEINTINT : function(thisref,handle,data,offset,length) {");
                theWriter.println("                 return bytecoder.filehandles[handle].readBytesINTL1BYTEINTINT(handle,data,offset,length);");
                theWriter.println("             },");
                theWriter.println("             skip0INTINT : function(thisref,handle,amount) {");
                theWriter.println("                 return bytecoder.filehandles[handle].skip0INTINT(handle,amount);");
                theWriter.println("             },");
                theWriter.println("             available0INT : function(thisref,handle) {");
                theWriter.println("                 return bytecoder.filehandles[handle].available0INT(handle);");
                theWriter.println("             },");
                theWriter.println("             close0INT : function(thisref,handle) {");
                theWriter.println("                 bytecoder.filehandles[handle].close0INT(handle);");
                theWriter.println("             },");
                theWriter.println("         },");

                theWriter.println("         inflater : {");
                theWriter.println("             initIDs : function(thisref) {");
                theWriter.println("             },");
                theWriter.println("             initBOOLEAN : function(thisref,nowrap) {");
                theWriter.println("             },");
                theWriter.println("             inflateBytesBytesLONGL1BYTEINTINTL1BYTEINTINT : function(thisref,addr,inputArray,inputOff,inputLen,outputArray,outputOff,outputLen) {");
                theWriter.println("             },");
                theWriter.println("             inflateBufferBytesLONGLONGINTL1BYTEINTINT : function(thisref,addr,inputAddress,inputLen,outputArray,outputOff,outputLen) {");
                theWriter.println("             },");
                theWriter.println("             endLONG : function(thisref,addr) {");
                theWriter.println("             },");
                theWriter.println("         },");

                final Map<String, List<OpaqueReferenceMethod>> theMethods = opaqueReferenceMethods.stream().collect(Collectors.groupingBy(opaqueReferenceMethod -> opaqueReferenceMethod.linkedClass.linkFor(opaqueReferenceMethod.getMethod()).getModuleName()));
                for (final Map.Entry<String, List<OpaqueReferenceMethod>> theEntry : theMethods.entrySet()) {

                    theWriter.print("         ");
                    theWriter.print(theEntry.getKey());
                    theWriter.println(": {");

                    for (final OpaqueReferenceMethod theMethod : theEntry.getValue()) {
                        final BytecodeMethod theBytecdeMethod = theMethod.getMethod();

                        final BytecodeImportedLink theImportedLink = theMethod.getLinkedClass().linkFor(theBytecdeMethod);
                        theWriter.print("             ");
                        theWriter.print(theImportedLink.getLinkName());
                        theWriter.print(": function(");
                        theWriter.print("target");
                        final BytecodeMethodSignature theSignature = theBytecdeMethod.getSignature();
                        for (int i = 0; i < theSignature.getArguments().length; i++) {
                            theWriter.print(",arg");
                            theWriter.print(i);
                        }
                        theWriter.println(") {");

                        String theMethodName = theBytecdeMethod.getName().stringValue();
                        final BytecodeAnnotation theOpaqueProperty = theBytecdeMethod.getAttributes().getAnnotationByType(OpaqueProperty.class.getName());
                        final BytecodeAnnotation theOpaqueIndex = theBytecdeMethod.getAttributes().getAnnotationByType(OpaqueIndexed.class.getName());
                        if (theOpaqueIndex != null) {
                            if (theSignature.getReturnType().isVoid()) {
                                theWriter.print("               ");
                                theWriter.print("bytecoder.referenceTable[target]");
                                theWriter.print("[arg0]");

                                final String theConversionFunction = conversionFunctionToJSForOpaqueType(aLinkerContext, theSignature.getArguments()[1]);
                                if (theConversionFunction != null) {
                                    theWriter.print("=");
                                    theWriter.print(theConversionFunction);
                                    theWriter.println("(arg1);");
                                } else {
                                    theWriter.println("=arg1;");
                                }
                            } else {
                                theWriter.print("               return ");

                                boolean theWriteClosingBraces = false;

                                final String theConversionFunction = conversionFunctionToWASMForOpaqueType(aLinkerContext, theSignature.getReturnType());
                                if (theConversionFunction != null) {
                                    theWriter.print(theConversionFunction);
                                    theWriter.print("(");

                                    theWriteClosingBraces = true;
                                }

                                theWriter.print("bytecoder.referenceTable[target][arg0]");

                                if (theWriteClosingBraces) {
                                    theWriter.print(")");
                                }
                                theWriter.println(";");
                            }
                        } else if (theOpaqueProperty != null) {
                            final BytecodeAnnotation.ElementValue theValue = theOpaqueProperty.getElementValueByName("value");
                            String theOpaquePropertyName;
                            if (theValue == null) {
                                if (theMethodName.startsWith("get")) {
                                    theOpaquePropertyName = theMethodName.substring(3);
                                    theOpaquePropertyName = Character.toLowerCase(theOpaquePropertyName.charAt(0)) + theOpaquePropertyName.substring(1);
                                } else if (theMethodName.startsWith("is")) {
                                    theOpaquePropertyName = theMethodName.substring(2);
                                    theOpaquePropertyName = Character.toLowerCase(theOpaquePropertyName.charAt(0)) + theOpaquePropertyName.substring(1);
                                } else if (theMethodName.startsWith("set")) {
                                    theOpaquePropertyName = theMethodName.substring(3);
                                    theOpaquePropertyName = Character.toLowerCase(theOpaquePropertyName.charAt(0)) + theOpaquePropertyName.substring(1);
                                } else {
                                    theOpaquePropertyName = theMethodName;
                                }
                            } else {
                                theOpaquePropertyName = theValue.stringValue();
                            }
                            if (theSignature.getReturnType().isVoid()) {
                                theWriter.print("               ");
                                theWriter.print("bytecoder.referenceTable[target].");
                                theWriter.print(theOpaquePropertyName);

                                final BytecodeTypeRef[] theArguments = theSignature.getArguments();
                                if (theArguments.length != 1) {
                                    throw new IllegalStateException("OpaqueProperty setter must have exactly one argument. Property " + theEntry.getKey() + "." + theOpaquePropertyName + " has " + theArguments.length + " arguments.");
                                }
                                final String theConversionFunction = conversionFunctionToJSForOpaqueType(aLinkerContext, theArguments[0]);
                                if (theConversionFunction != null) {
                                    theWriter.print("=");
                                    theWriter.print(theConversionFunction);
                                    theWriter.println("(arg0);");
                                } else {
                                    theWriter.println("=arg0;");
                                }
                            } else {
                                theWriter.print("               return ");

                                boolean theWriteClosingBraces = false;

                                final String theConversionFunction = conversionFunctionToWASMForOpaqueType(aLinkerContext, theSignature.getReturnType());
                                if (theConversionFunction != null) {
                                    theWriter.print(theConversionFunction);
                                    theWriter.print("(");

                                    theWriteClosingBraces = true;
                                }

                                theWriter.print("bytecoder.referenceTable[target].");
                                theWriter.print(theOpaquePropertyName);

                                if (theWriteClosingBraces) {
                                    theWriter.print(")");
                                }
                                theWriter.println(";");
                            }
                        } else {
                            // It is a method invocation
                            final BytecodeAnnotation theMethodAnnotation = theBytecdeMethod.getAttributes().getAnnotationByType(OpaqueMethod.class.getName());
                            if (theMethodAnnotation != null) {
                                theMethodName = theMethodAnnotation.getElementValueByName("value").stringValue();
                            }

                            boolean theWriteClosingBraces = false;

                            if (!theSignature.getReturnType().isVoid()) {
                                theWriter.print("               return ");

                                final String theConversionFunction = conversionFunctionToWASMForOpaqueType(aLinkerContext, theSignature.getReturnType());
                                if (theConversionFunction != null) {
                                    theWriter.print(theConversionFunction);
                                    theWriter.print("(");

                                    theWriteClosingBraces = true;
                                }
                            } else {
                                theWriter.print("               ");
                            }

                            theWriter.print("bytecoder.referenceTable[target].");
                            theWriter.print(theMethodName);
                            theWriter.print("(");

                            for (int i = 0; i < theSignature.getArguments().length; i++) {
                                if (i > 0) {
                                    theWriter.print(",");
                                }

                                final BytecodeTypeRef theRef = theSignature.getArguments()[i];
                                if (theRef.isPrimitive()) {
                                    theWriter.print("arg");
                                    theWriter.print(i);
                                } else if (theRef.matchesExactlyTo(BytecodeObjectTypeRef.fromRuntimeClass(String.class))) {
                                    theWriter.print("bytecoder.toJSString(");
                                    theWriter.print("arg");
                                    theWriter.print(i);
                                    theWriter.print(")");
                                } else {
                                    final BytecodeObjectTypeRef theObjectType = (BytecodeObjectTypeRef) theRef;
                                    final BytecodeLinkedClass theLinkedClass = aLinkerContext.resolveClass(theObjectType);
                                    if (theLinkedClass.isOpaqueType()) {
                                        theWriter.print("bytecoder.toJSReference(");
                                        theWriter.print("arg");
                                        theWriter.print(i);
                                        theWriter.print(")");
                                    } else if (theLinkedClass.isCallback()) {

                                        final List<BytecodeMethod> theCallbackMethods = aLinkerContext.resolveMethods(theLinkedClass).stream().filter(x -> x.getProvidingClass() == theLinkedClass).map(BytecodeResolvedMethods.MethodEntry::getValue).collect(Collectors.toList());
                                        if (theCallbackMethods.size() != 1) {
                                            throw new IllegalStateException("Wrong number of callback methods in " + theLinkedClass.getClassName().name() + ", expected 1, got " + theCallbackMethods.size());
                                        }
                                        final BytecodeMethod theImpl = theCallbackMethods.get(0);

                                        theWriter.print("bytecoder.registerCallback(arg");
                                        theWriter.print(i);
                                        theWriter.print(",function (");
                                        for (int j = 0; j < theImpl.getSignature().getArguments().length; j++) {
                                            if (j > 0) {
                                                theWriter.print(",");
                                            }
                                            theWriter.print("farg");
                                            theWriter.print(j);
                                        }
                                        theWriter.print(") {");

                                        for (int j = 0; j < theImpl.getSignature().getArguments().length; j++) {
                                            theWriter.print("var marg");
                                            theWriter.print(j);
                                            theWriter.print("=");

                                            final BytecodeTypeRef theTypeRef = theImpl.getSignature().getArguments()[j];

                                            if (theTypeRef.isPrimitive()) {
                                                theWriter.print("farg");
                                                theWriter.print(j);
                                            } else if (theTypeRef.isArray()) {
                                                throw new IllegalStateException("Type conversion to " + theTypeRef.name() + " is not supported!");
                                            } else if (theTypeRef.matchesExactlyTo(BytecodeObjectTypeRef.fromRuntimeClass(String.class))) {
                                                theWriter.print("bytecoder.toBytecoderString(");
                                                theWriter.print("farg");
                                                theWriter.print(j);
                                                theWriter.print(")");
                                            } else {
                                                final BytecodeObjectTypeRef theArgObjectType = (BytecodeObjectTypeRef) theTypeRef;
                                                final BytecodeLinkedClass theArgLinkedClass = aLinkerContext.resolveClass(theArgObjectType);
                                                if (!theArgLinkedClass.isOpaqueType()) {
                                                    throw new IllegalStateException("Type conversion from " + theTypeRef.name() + " is not supported!");

                                                }
                                                theWriter.print("bytecoder.toBytecoderReference(");
                                                theWriter.print("farg");
                                                theWriter.print(j);
                                                theWriter.print(")");
                                            }
                                            theWriter.print(";");
                                        }

                                        final String theCallbackMethod = LLVMWriterUtils.toMethodName(theLinkedClass.getClassName(), theImpl.getName(), theImpl.getSignature());
                                        theWriter.print("bytecoder.exports.");
                                        theWriter.print(theCallbackMethod);
                                        theWriter.print("(arg");
                                        theWriter.print(i);

                                        for (int j = 0; j < theImpl.getSignature().getArguments().length; j++) {
                                            theWriter.print(",");
                                            theWriter.print("marg");
                                            theWriter.print(j);
                                        }

                                        theWriter.print(");");

                                        for (int j = 0; j < theImpl.getSignature().getArguments().length; j++) {
                                            final BytecodeTypeRef theTypeRef = theImpl.getSignature().getArguments()[j];

                                            if (theTypeRef.isPrimitive()) {
                                                // Nothing to clean up
                                            } else if (theTypeRef.matchesExactlyTo(BytecodeObjectTypeRef.fromRuntimeClass(String.class))) {
                                                // Nothinng to clean up
                                            } else {
                                                final BytecodeLinkedClass theLinkedType = aLinkerContext.resolveClass((BytecodeObjectTypeRef) theTypeRef);
                                                if (theLinkedType.isEvent()) {
                                                    // Cleanup object reference
                                                    theWriter.print("delete bytecoder.referenceTable[marg");
                                                    theWriter.print(j);
                                                    theWriter.print("];");
                                                }
                                            }
                                        }

                                        theWriter.print("})");
                                    } else {
                                        throw new IllegalStateException("Type conversion from " + theRef.name() + " is not supported!");
                                    }
                                }
                            }

                            if (theWriteClosingBraces) {
                                theWriter.print(")");
                            }
                            theWriter.println(");");

                        }
                        theWriter.println("             },");
                    }

                    theWriter.println("         },");

                }

                theWriter.println("     },");
                theWriter.println("};");
            }
            theCompileResult.add(new CompileResult.StringContent(aOptions.getFilenamePrefix() + ".js", theJSCode.toString()));

            // Compile LLVM Assembly File to object file
            final List<String> theLLCommand = new ArrayList<>();
            if ("\\".equals(File.separator)) {
                // We are running on windows
                // llvm needs to be installed in the Windows Subsystem for Linux
                theLLCommand.add("wsl");
            }
            final String theObjectFileName = theLLFile.getName() + ".o";
            theLLCommand.add("llc-11");
            theLLCommand.add("-" + aOptions.getLlvmOptimizationLevel().name());
            //theLLCommand.add("--stats");
            //theLLCommand.add("--time-passes");
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
                        aOptions.getLogger().info(line);
                    }
                }

                throw new RuntimeException("llc reported an error!");
            } else {
                final File theObjectFile = new File(theLLFile.getParent(), theObjectFileName);
                theObjectFile.deleteOnExit();
                try (final FileInputStream inputStream = new FileInputStream(theObjectFile)) {
                    theCompileResult.add(new CompileResult.BinaryContent(aOptions.getFilenamePrefix() + ".o",
                            IOUtils.toByteArray(inputStream)));
                }

                // We can delete the LL file, as we have rhe object file now
                theLLFile.delete();
            }

            // Link object file to wasm binary
            final List<String> theLinkerCommand = new ArrayList<>();
            if ("\\".equals(File.separator)) {
                // We are running on windows
                // llvm needs to be installed in the Windows Subsystem for Linux
                theLinkerCommand.add("wsl");
            }
            final String theWASMFileName = theLLFile.getName() + ".wasm";
            theLinkerCommand.add("wasm-ld-11");
            theLinkerCommand.add(theObjectFileName);
            theLinkerCommand.add("-o");
            theLinkerCommand.add(theWASMFileName);
            theLinkerCommand.add("-export-dynamic");
            theLinkerCommand.add("-allow-undefined");
            theLinkerCommand.add("--lto-" + aOptions.getLlvmOptimizationLevel().name());
            theLinkerCommand.add("--no-entry");
            if (aOptions.isDebugOutput()) {
                theLinkerCommand.add("--demangle");
            } else {
                theLinkerCommand.add("-s");
            }
            theLinkerCommand.add("--initial-memory=" + aOptions.getWasmMinimumPageSize() * 65536);
            theLinkerCommand.add("--max-memory=" + aOptions.getWasmMaximumPageSize() * 65536);
            final ProcessBuilder theLinkerProcessBuilder = new ProcessBuilder(theLinkerCommand).directory(theLLFile.getParentFile());
            aOptions.getLogger().info("LLVM linker command is {}", theLinkerProcessBuilder.command());

            final Process theLinkerProcess = theLinkerProcessBuilder.start();
            if (theLinkerProcess.waitFor() != 0) {
                aOptions.getLogger().warn("wasm-ld returned with exit code {} ", theLinkerProcess.exitValue());
                try (final BufferedReader processOutput = new BufferedReader(
                        new InputStreamReader(theLinkerProcess.getErrorStream()))) {
                    String line;
                    while ((line = processOutput.readLine()) != null) {
                        aOptions.getLogger().warn(line);
                    }
                }

                throw new RuntimeException("wasm-ld reported an error!");

            } else {
                final File theWASMFile = new File(theLLFile.getParent(), theWASMFileName);
                theWASMFile.deleteOnExit();
                try (final FileInputStream inputStream = new FileInputStream(theWASMFile)) {
                    theCompileResult.add(new CompileResult.BinaryContent(aOptions.getFilenamePrefix() + ".wasm",
                            IOUtils.toByteArray(inputStream)));
                }

                // We can delete the object and the wasm file now
                new File(theObjectFileName).delete();
                theWASMFile.delete();
            }

            return theCompileResult;
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String conversionFunctionToJSForOpaqueType(final BytecodeLinkerContext alinkerContext, final BytecodeTypeRef aTypeRef) {
        if (aTypeRef.isPrimitive()) {
            return null;
        } else if (aTypeRef.isArray()) {
            throw new IllegalStateException("Type conversion to " + aTypeRef.name() + " is not supported!");
        } else if (aTypeRef.matchesExactlyTo(BytecodeObjectTypeRef.fromRuntimeClass(String.class))) {
            return "bytecoder.toJSString";
        } else {
            final BytecodeObjectTypeRef theObjectType = (BytecodeObjectTypeRef) aTypeRef;
            final BytecodeLinkedClass theLinkedClass = alinkerContext.resolveClass(theObjectType);
            if (theLinkedClass.isOpaqueType()) {
                return "bytecoder.toJSReference";
            } else {
                throw new IllegalStateException("Type conversion from " + aTypeRef.name() + " is not supported!");
            }
        }
    }

    private String conversionFunctionToWASMForOpaqueType(final BytecodeLinkerContext alinkerContext, final BytecodeTypeRef aTypeRef) {
        if (aTypeRef.isPrimitive()) {
            return null;
        } else if (aTypeRef.isArray()) {
            throw new IllegalStateException("Type conversion to " + aTypeRef.name() + " is not supported!");
        } else if (aTypeRef.matchesExactlyTo(BytecodeObjectTypeRef.fromRuntimeClass(String.class))) {
            return "bytecoder.toBytecoderString";
        } else {
            final BytecodeObjectTypeRef theObjectType = (BytecodeObjectTypeRef) aTypeRef;
            final BytecodeLinkedClass theLinkedClass = alinkerContext.resolveClass(theObjectType);
            if (theLinkedClass.isOpaqueType()) {
                return "bytecoder.toBytecoderReference";
            }
            throw new IllegalStateException("Type conversion from " + aTypeRef.name() + " is not supported!");
        }
    }

    private void writeMethodHandleDelegateInvokeStatic(final BytecodeLinkerContext aLinkerContext, final MethodHandleExpression aMethodHandle, final String aDelegateMethodName, final PrintWriter aWriter) {

        final BytecodeMethodSignature theSignature = aMethodHandle.getImplementationSignature();

        // We compile the list of arguments
        final MethodHandleExpression.AdapterAnnotation theAdapterAnnotation = aMethodHandle.getAdapterAnnotation();

        if (theSignature.getReturnType().isVoid()) {
            aWriter.print("define internal void @");
            aWriter.print(aDelegateMethodName);
            aWriter.print("(i32 %lambdaRef");
        } else {
            aWriter.print("define internal ");
            aWriter.print(LLVMWriterUtils.toType(TypeRef.toType(theSignature.getReturnType())));
            aWriter.print(" @");
            aWriter.print(aDelegateMethodName);
            aWriter.print("(i32 %lambdaRef");
        }

        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            final String theType = LLVMWriterUtils.toType(TypeRef.toType(theAdapterAnnotation.getCaptureSignature().getArguments()[k]));
            aWriter.print(",");
            aWriter.print(theType);
            aWriter.print(" %");
            aWriter.print(theArgName);
        }

        aWriter.println(") {");
        aWriter.println("entry:");

        // Now, we reed to resolve the linkargs
        // Offset 12 == list of static arguments
        if (theAdapterAnnotation.getLinkageSignature().getArguments().length > 0) {
            aWriter.println("    %staticoffset = add i32 %lambdaRef, 12");
            aWriter.println("    %staticdatalistptr = inttoptr i32 %staticoffset to i32*");
            aWriter.println("    %staticdata = load i32, i32* %staticdatalistptr");

            // For every link argument, we load it from the static data list
            for (int k=0;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
                final String theArgName = "linkArg" + k;
                final String theType = LLVMWriterUtils.toType(TypeRef.toType(theAdapterAnnotation.getLinkageSignature().getArguments()[k]));

                aWriter.print("    %");
                aWriter.print(theArgName);
                aWriter.print("_offset = add i32 %staticdata, ");
                aWriter.println(20 + k * 8);

                aWriter.print("    %");
                aWriter.print(theArgName);
                aWriter.print("_ptr = inttoptr i32 %");
                aWriter.print(theArgName);
                aWriter.print("_offset to ");

                aWriter.print(theType);
                aWriter.println("*");

                aWriter.print("    %");
                aWriter.print(theArgName);
                aWriter.print(" = load ");
                aWriter.print(theType);
                aWriter.print(",");
                aWriter.print(theType);
                aWriter.print("* %");
                aWriter.print(theArgName);
                aWriter.println("_ptr");
            }
        }

        if (!theSignature.getReturnType().isVoid()) {
            aWriter.print("    %ret = ");
        } else {
            aWriter.print("    ");
        }

        final List<BytecodeTypeRef> theEffectiveSignatureArguments = new ArrayList<>();
        theEffectiveSignatureArguments.addAll(Arrays.asList(theAdapterAnnotation.getLinkageSignature().getArguments()));
        theEffectiveSignatureArguments.addAll(Arrays.asList(theAdapterAnnotation.getCaptureSignature().getArguments()));
        final BytecodeMethodSignature theEffectiveSignature = new BytecodeMethodSignature(theSignature.getReturnType(), theEffectiveSignatureArguments.toArray(new BytecodeTypeRef[0]));

        aWriter.print("call ");
        aWriter.print(LLVMWriterUtils.toType(TypeRef.toType(theSignature.getReturnType())));
        aWriter.print(" @");
        aWriter.print(LLVMWriterUtils.toMethodName(aMethodHandle.getClassName(), aMethodHandle.getMethodName(), theEffectiveSignature));
        aWriter.print("(i32 undef");
        for (int k=0;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
            final String theArgName = "linkArg" + k;
            final String theType = LLVMWriterUtils.toType(TypeRef.toType(theAdapterAnnotation.getLinkageSignature().getArguments()[k]));
            aWriter.print(",");
            aWriter.print(theType);
            aWriter.print(" %");
            aWriter.print(theArgName);
        }
        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            final String theType = LLVMWriterUtils.toType(TypeRef.toType(theAdapterAnnotation.getCaptureSignature().getArguments()[k]));
            aWriter.print(",");
            aWriter.print(theType);
            aWriter.print(" %");
            aWriter.print(theArgName);
        }

        aWriter.println(")");
        if (!theSignature.getReturnType().isVoid()) {
            aWriter.print("    ret ");
            aWriter.print(LLVMWriterUtils.toType(TypeRef.toType(theSignature.getReturnType())));
            aWriter.println(" %ret");
        } else {
            aWriter.println("    ret void");
        }
        aWriter.println("}");
        aWriter.println();
    }

    private void writeMethodHandleDelegateInvokeVirtual(final BytecodeLinkerContext aLinkerContext, final MethodHandleExpression aMethodHandle, final String aDelegateMethodName, final PrintWriter aWriter) {

        final BytecodeMethodSignature theSignature = aMethodHandle.getImplementationSignature();

        // We compile the list of arguments
        final MethodHandleExpression.AdapterAnnotation theAdapterAnnotation = aMethodHandle.getAdapterAnnotation();

        if (theSignature.getReturnType().isVoid()) {
            aWriter.print("define internal void @");
            aWriter.print(aDelegateMethodName);
            aWriter.print("(i32 %lambdaRef");
        } else {
            aWriter.print("define internal ");
            aWriter.print(LLVMWriterUtils.toType(TypeRef.toType(theSignature.getReturnType())));
            aWriter.print(" @");
            aWriter.print(aDelegateMethodName);
            aWriter.print("(i32 %lambdaRef");
        }

        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            final String theType = LLVMWriterUtils.toType(TypeRef.toType(theAdapterAnnotation.getCaptureSignature().getArguments()[k]));
            aWriter.print(",");
            aWriter.print(theType);
            aWriter.print(" %");
            aWriter.print(theArgName);
        }

        aWriter.println(") {");
        aWriter.println("entry:");

        // Now, we reed to resolve the linkargs
        // Offset 12 == list of static arguments
        if (theAdapterAnnotation.getLinkageSignature().getArguments().length > 0) {
            aWriter.println("    %staticoffset = add i32 %lambdaRef, 12");
            aWriter.println("    %staticdatalistptr = inttoptr i32 %staticoffset to i32*");
            aWriter.println("    %staticdata = load i32, i32* %staticdatalistptr");

            // For every link argument, we load it from the static data list
            for (int k=0;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
                final String theArgName = "linkArg" + k;
                final String theType = LLVMWriterUtils.toType(TypeRef.toType(theAdapterAnnotation.getLinkageSignature().getArguments()[k]));

                aWriter.print("    %");
                aWriter.print(theArgName);
                aWriter.print("_offset = add i32 %staticdata, ");
                aWriter.println(20 + k * 8);

                aWriter.print("    %");
                aWriter.print(theArgName);
                aWriter.print("_ptr = inttoptr i32 %");
                aWriter.print(theArgName);
                aWriter.print("_offset to ");

                aWriter.print(theType);
                aWriter.println("*");

                aWriter.print("    %");
                aWriter.print(theArgName);
                aWriter.print(" = load ");
                aWriter.print(theType);
                aWriter.print(",");
                aWriter.print(theType);
                aWriter.print("* %");
                aWriter.print(theArgName);
                aWriter.println("_ptr");
            }
        }

        // linkArg0 is the invocation target
        // we perform a call over the interface resolver
        aWriter.println("    %vtable = add i32 %linkArg0, 4");
        aWriter.println("    %vtableptr = inttoptr i32 %vtable to i32*");
        aWriter.println("    %vtableref = load i32, i32* %vtableptr");
        aWriter.println("    %vtableref_offset = add i32 %vtableref, 4");
        aWriter.println("    %vtableref_offset_ptr = inttoptr i32 %vtableref_offset to i32*");
        aWriter.println("    %dispatcher = load i32, i32* %vtableref_offset_ptr");
        aWriter.println("    %dispatcher_ptr = inttoptr i32 %dispatcher to i32(i32,i32)*");

        // Resolve the index of the virtual identifier
        aWriter.print("    %resolved = call i32(i32,i32) %dispatcher_ptr(i32 %linkArg0,");

        final List<BytecodeTypeRef> theEffectiveSignatureArguments = new ArrayList<>();
        for (int k=1;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
            theEffectiveSignatureArguments.add(theAdapterAnnotation.getLinkageSignature().getArguments()[k]);
        }
        theEffectiveSignatureArguments.addAll(Arrays.asList(theAdapterAnnotation.getCaptureSignature().getArguments()));
        final BytecodeMethodSignature theEffectiveSignature = new BytecodeMethodSignature(theSignature.getReturnType(), theEffectiveSignatureArguments.toArray(new BytecodeTypeRef[0]));

        final BytecodeVirtualMethodIdentifier theMethodIdentifier = aLinkerContext.getMethodCollection().identifierFor(aMethodHandle.getMethodName(), theEffectiveSignature);

        aWriter.print("i32 ");
        aWriter.print(theMethodIdentifier.getIdentifier());
        aWriter.println(")");

        aWriter.print("    %resolved_ptr = inttoptr i32 %resolved to ");
        aWriter.print(LLVMWriterUtils.toSignature(theEffectiveSignature));
        aWriter.println("*");

        if (!theSignature.getReturnType().isVoid()) {
            aWriter.print("    %ret = ");
        } else {
            aWriter.print("    ");
        }

        aWriter.print("call ");
        aWriter.print(LLVMWriterUtils.toSignature(theEffectiveSignature));
        aWriter.print(" %resolved_ptr (i32 %linkArg0");
        for (int k=1;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
            final String theArgName = "linkArg" + k;
            final String theType = LLVMWriterUtils.toType(TypeRef.toType(theAdapterAnnotation.getLinkageSignature().getArguments()[k]));
            aWriter.print(",");
            aWriter.print(theType);
            aWriter.print(" %");
            aWriter.print(theArgName);
        }
        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            final String theType = LLVMWriterUtils.toType(TypeRef.toType(theAdapterAnnotation.getCaptureSignature().getArguments()[k]));
            aWriter.print(",");
            aWriter.print(theType);
            aWriter.print(" %");
            aWriter.print(theArgName);
        }
        aWriter.println(")");

        if (!theSignature.getReturnType().isVoid()) {
            aWriter.print("    ret ");
            aWriter.print(LLVMWriterUtils.toType(TypeRef.toType(theSignature.getReturnType())));
            aWriter.println(" %ret");
        } else {
            aWriter.println("    ret void");
        }
        aWriter.println("}");
        aWriter.println();
    }

    private void writeMethodHandleDelegateNewInvokeSpecial(final BytecodeLinkerContext aLinkerContext, final MethodHandleExpression aMethodHandle, final String aDelegateMethodName, final PrintWriter aWriter) {

        // We compile the list of arguments
        final MethodHandleExpression.AdapterAnnotation theAdapterAnnotation = aMethodHandle.getAdapterAnnotation();

        aWriter.print("define internal i32 ");
        aWriter.print(" @");
        aWriter.print(aDelegateMethodName);
        aWriter.print("(i32 %lambdaRef");

        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            final String theType = LLVMWriterUtils.toType(TypeRef.toType(theAdapterAnnotation.getCaptureSignature().getArguments()[k]));
            aWriter.print(",");
            aWriter.print(theType);
            aWriter.print(" %");
            aWriter.print(theArgName);
        }

        aWriter.println(") {");
        aWriter.println("entry:");

        // Now, we reed to resolve the linkargs
        // Offset 12 == list of static arguments
        if (theAdapterAnnotation.getLinkageSignature().getArguments().length > 0) {
            aWriter.println("    %staticoffset = add i32 %lambdaRef, 12");
            aWriter.println("    %staticdatalistptr = inttoptr i32 %staticoffset to i32*");
            aWriter.println("    %staticdata = load i32, i32* %staticdatalistptr");

            // For every link argument, we load it from the static data list
            for (int k=0;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
                final String theArgName = "linkArg" + k;
                final String theType = LLVMWriterUtils.toType(TypeRef.toType(theAdapterAnnotation.getLinkageSignature().getArguments()[k]));

                aWriter.print("    %");
                aWriter.print(theArgName);
                aWriter.print("_offset = add i32 %staticdata, ");
                aWriter.println(20 + k * 8);

                aWriter.print("    %");
                aWriter.print(theArgName);
                aWriter.print("_ptr = inttoptr i32 %");
                aWriter.print(theArgName);
                aWriter.print("_offset to ");

                aWriter.print(theType);
                aWriter.println("*");

                aWriter.print("    %");
                aWriter.print(theArgName);
                aWriter.print(" = load ");
                aWriter.print(theType);
                aWriter.print(",");
                aWriter.print(theType);
                aWriter.print("* %");
                aWriter.print(theArgName);
                aWriter.println("_ptr");
            }
        }

        final List<BytecodeTypeRef> theEffectiveSignatureArguments = new ArrayList<>();
        theEffectiveSignatureArguments.addAll(Arrays.asList(theAdapterAnnotation.getLinkageSignature().getArguments()));
        theEffectiveSignatureArguments.addAll(Arrays.asList(theAdapterAnnotation.getCaptureSignature().getArguments()));
        final BytecodeMethodSignature theEffectiveSignature = new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, theEffectiveSignatureArguments.toArray(new BytecodeTypeRef[0]));

        aWriter.print("    %");
        aWriter.print(LLVMWriterUtils.runtimeClassVariableName(aMethodHandle.getClassName()));
        aWriter.print(" = call i32 @");
        aWriter.print(LLVMWriterUtils.toClassName(aMethodHandle.getClassName()));
        aWriter.print(LLVMWriter.CLASSINITSUFFIX);
        aWriter.println("()");

        aWriter.print("    %ret = call i32 @");
        aWriter.print(LLVMWriterUtils.toMethodName(aMethodHandle.getClassName(), "$newInstance", theEffectiveSignature));
        aWriter.print("(i32 %");
        aWriter.print(LLVMWriterUtils.runtimeClassVariableName(aMethodHandle.getClassName()));
        for (int k=0;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
            final String theArgName = "linkArg" + k;
            final String theType = LLVMWriterUtils.toType(TypeRef.toType(theAdapterAnnotation.getLinkageSignature().getArguments()[k]));
            aWriter.print(",");
            aWriter.print(theType);
            aWriter.print(" %");
            aWriter.print(theArgName);
        }
        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            final String theType = LLVMWriterUtils.toType(TypeRef.toType(theAdapterAnnotation.getCaptureSignature().getArguments()[k]));
            aWriter.print(",");
            aWriter.print(theType);
            aWriter.print(" %");
            aWriter.print(theArgName);
        }
        aWriter.println(")");

        aWriter.println("    ret i32 %ret");
        aWriter.println("}");
        aWriter.println();
    }

    private void writeMethodHandleDelegateInvokeSpecial(final BytecodeLinkerContext aLinkerContext, final MethodHandleExpression aMethodHandle, final String aDelegateMethodName, final PrintWriter aWriter) {

        final BytecodeMethodSignature theSignature = aMethodHandle.getImplementationSignature();

        // We compile the list of arguments
        final MethodHandleExpression.AdapterAnnotation theAdapterAnnotation = aMethodHandle.getAdapterAnnotation();

        if (theSignature.getReturnType().isVoid()) {
            aWriter.print("define internal void @");
            aWriter.print(aDelegateMethodName);
            aWriter.print("(i32 %lambdaRef");
        } else {
            aWriter.print("define internal ");
            aWriter.print(LLVMWriterUtils.toType(TypeRef.toType(theSignature.getReturnType())));
            aWriter.print(" @");
            aWriter.print(aDelegateMethodName);
            aWriter.print("(i32 %lambdaRef");
        }

        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            final String theType = LLVMWriterUtils.toType(TypeRef.toType(theAdapterAnnotation.getCaptureSignature().getArguments()[k]));
            aWriter.print(",");
            aWriter.print(theType);
            aWriter.print(" %");
            aWriter.print(theArgName);
        }

        aWriter.println(") {");
        aWriter.println("entry:");

        // Now, we reed to resolve the linkargs
        // Offset 12 == list of static arguments
        if (theAdapterAnnotation.getLinkageSignature().getArguments().length > 0) {
            aWriter.println("    %staticoffset = add i32 %lambdaRef, 12");
            aWriter.println("    %staticdatalistptr = inttoptr i32 %staticoffset to i32*");
            aWriter.println("    %staticdata = load i32, i32* %staticdatalistptr");

            // For every link argument, we load it from the static data list
            for (int k=0;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
                final String theArgName = "linkArg" + k;
                final String theType = LLVMWriterUtils.toType(TypeRef.toType(theAdapterAnnotation.getLinkageSignature().getArguments()[k]));

                aWriter.print("    %");
                aWriter.print(theArgName);
                aWriter.print("_offset = add i32 %staticdata, ");
                aWriter.println(20 + k * 8);

                aWriter.print("    %");
                aWriter.print(theArgName);
                aWriter.print("_ptr = inttoptr i32 %");
                aWriter.print(theArgName);
                aWriter.print("_offset to ");

                aWriter.print(theType);
                aWriter.println("*");

                aWriter.print("    %");
                aWriter.print(theArgName);
                aWriter.print(" = load ");
                aWriter.print(theType);
                aWriter.print(",");
                aWriter.print(theType);
                aWriter.print("* %");
                aWriter.print(theArgName);
                aWriter.println("_ptr");
            }
        }

        final List<BytecodeTypeRef> theEffectiveSignatureArguments = new ArrayList<>();
        for (int k=1;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
            theEffectiveSignatureArguments.add(theAdapterAnnotation.getLinkageSignature().getArguments()[k]);
        }
        theEffectiveSignatureArguments.addAll(Arrays.asList(theAdapterAnnotation.getCaptureSignature().getArguments()));
        final BytecodeMethodSignature theEffectiveSignature = new BytecodeMethodSignature(theSignature.getReturnType(), theEffectiveSignatureArguments.toArray(new BytecodeTypeRef[0]));

        if (!theSignature.getReturnType().isVoid()) {
            aWriter.print("    %ret = ");
        } else {
            aWriter.print("    ");
        }

        aWriter.print("call ");
        aWriter.print(LLVMWriterUtils.toSignature(theEffectiveSignature));
        aWriter.print(" @");
        aWriter.print(LLVMWriterUtils.toMethodName(aMethodHandle.getClassName(), aMethodHandle.getMethodName(), theEffectiveSignature));
        aWriter.print("(i32 %linkArg0");
        for (int k=1;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
            final String theArgName = "linkArg" + k;
            final String theType = LLVMWriterUtils.toType(TypeRef.toType(theAdapterAnnotation.getLinkageSignature().getArguments()[k]));
            aWriter.print(",");
            aWriter.print(theType);
            aWriter.print(" %");
            aWriter.print(theArgName);
        }
        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            final String theType = LLVMWriterUtils.toType(TypeRef.toType(theAdapterAnnotation.getCaptureSignature().getArguments()[k]));
            aWriter.print(",");
            aWriter.print(theType);
            aWriter.print(" %");
            aWriter.print(theArgName);
        }
        aWriter.println(")");

        if (!theSignature.getReturnType().isVoid()) {
            aWriter.print("    ret ");
            aWriter.print(LLVMWriterUtils.toType(TypeRef.toType(theSignature.getReturnType())));
            aWriter.println(" %ret");
        } else {
            aWriter.println("    ret void");
        }
        aWriter.println("}");
        aWriter.println();
    }

    private void writeMethodHandleDelegateInvokeInterface(final BytecodeLinkerContext aLinkerContext, final MethodHandleExpression aMethodHandle, final String aDelegateMethodName, final PrintWriter aWriter) {

        final BytecodeMethodSignature theSignature = aMethodHandle.getImplementationSignature();

        // We compile the list of arguments
        final MethodHandleExpression.AdapterAnnotation theAdapterAnnotation = aMethodHandle.getAdapterAnnotation();

        if (theSignature.getReturnType().isVoid()) {
            aWriter.print("define internal void @");
            aWriter.print(aDelegateMethodName);
            aWriter.print("(i32 %lambdaRef");
        } else {
            aWriter.print("define internal ");
            aWriter.print(LLVMWriterUtils.toType(TypeRef.toType(theSignature.getReturnType())));
            aWriter.print(" @");
            aWriter.print(aDelegateMethodName);
            aWriter.print("(i32 %lambdaRef");
        }

        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            final String theType = LLVMWriterUtils.toType(TypeRef.toType(theAdapterAnnotation.getCaptureSignature().getArguments()[k]));
            aWriter.print(",");
            aWriter.print(theType);
            aWriter.print(" %");
            aWriter.print(theArgName);
        }

        aWriter.println(") {");
        aWriter.println("entry:");

        // Now, we reed to resolve the linkargs
        // Offset 12 == list of static arguments
        if (theAdapterAnnotation.getLinkageSignature().getArguments().length > 0) {
            aWriter.println("    %staticoffset = add i32 %lambdaRef, 12");
            aWriter.println("    %staticdatalistptr = inttoptr i32 %staticoffset to i32*");
            aWriter.println("    %staticdata = load i32, i32* %staticdatalistptr");

            // For every link argument, we load it from the static data list
            for (int k=0;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
                final String theArgName = "linkArg" + k;
                final String theType = LLVMWriterUtils.toType(TypeRef.toType(theAdapterAnnotation.getLinkageSignature().getArguments()[k]));

                aWriter.print("    %");
                aWriter.print(theArgName);
                aWriter.print("_offset = add i32 %staticdata, ");
                aWriter.println(20 + k * 8);

                aWriter.print("    %");
                aWriter.print(theArgName);
                aWriter.print("_ptr = inttoptr i32 %");
                aWriter.print(theArgName);
                aWriter.print("_offset to ");

                aWriter.print(theType);
                aWriter.println("*");

                aWriter.print("    %");
                aWriter.print(theArgName);
                aWriter.print(" = load ");
                aWriter.print(theType);
                aWriter.print(",");
                aWriter.print(theType);
                aWriter.print("* %");
                aWriter.print(theArgName);
                aWriter.println("_ptr");
            }
        }

        String theTarget = null;

        // The first of either the link or capture arguments is the invocation target
        final List<BytecodeTypeRef> theEffectiveSignatureArguments = new ArrayList<>();
        for (int k=0;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
            if (theTarget == null) {
                theTarget = "linkArg" + k;
            } else {
                theEffectiveSignatureArguments.add(theAdapterAnnotation.getLinkageSignature().getArguments()[k]);
            }
        }
        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            if (theTarget == null) {
                theTarget = "captureArg" + k;
            } else {
                theEffectiveSignatureArguments.add(theAdapterAnnotation.getCaptureSignature().getArguments()[k]);
            }
        }

        // we perform a call over the interface resolver
        aWriter.print("    %vtable = add i32 %");
        aWriter.print(theTarget);
        aWriter.println(", 4");
        aWriter.println("    %vtableptr = inttoptr i32 %vtable to i32*");
        aWriter.println("    %vtableref = load i32, i32* %vtableptr");
        aWriter.println("    %vtableref_offset = add i32 %vtableref, 4");
        aWriter.println("    %vtableref_offset_ptr = inttoptr i32 %vtableref_offset to i32*");
        aWriter.println("    %dispatcher = load i32, i32* %vtableref_offset_ptr");
        aWriter.println("    %dispatcher_ptr = inttoptr i32 %dispatcher to i32(i32,i32)*");

        // Resolve the index of the virtual identifier
        aWriter.print("    %resolved = call i32(i32,i32) %dispatcher_ptr(i32 %");
        aWriter.print(theTarget);
        aWriter.print(",");

        final BytecodeMethodSignature theEffectiveSignature = new BytecodeMethodSignature(theSignature.getReturnType(), theEffectiveSignatureArguments.toArray(new BytecodeTypeRef[0]));

        final BytecodeVirtualMethodIdentifier theMethodIdentifier = aLinkerContext.getMethodCollection().identifierFor(aMethodHandle.getMethodName(), theEffectiveSignature);

        aWriter.print("i32 ");
        aWriter.print(theMethodIdentifier.getIdentifier());
        aWriter.println(")");

        aWriter.print("    %resolved_ptr = inttoptr i32 %resolved to ");
        aWriter.print(LLVMWriterUtils.toSignature(theEffectiveSignature));
        aWriter.println("*");

        if (!theSignature.getReturnType().isVoid()) {
            aWriter.print("    %ret = ");
        } else {
            aWriter.print("    ");
        }

        aWriter.print("call ");
        aWriter.print(LLVMWriterUtils.toSignature(theEffectiveSignature));
        aWriter.print(" %resolved_ptr (i32 %");
        aWriter.print(theTarget);
        for (int k=0;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
            final String theArgName = "linkArg" + k;
            if (!theTarget.equals(theArgName)) {
                final String theType = LLVMWriterUtils.toType(TypeRef.toType(theAdapterAnnotation.getLinkageSignature().getArguments()[k]));
                aWriter.print(",");
                aWriter.print(theType);
                aWriter.print(" %");
                aWriter.print(theArgName);
            }
        }
        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            if (!theTarget.equals(theArgName)) {
                final String theType = LLVMWriterUtils.toType(TypeRef.toType(theAdapterAnnotation.getCaptureSignature().getArguments()[k]));
                aWriter.print(",");
                aWriter.print(theType);
                aWriter.print(" %");
                aWriter.print(theArgName);
            }
        }
        aWriter.println(")");

        if (!theSignature.getReturnType().isVoid()) {
            aWriter.print("    ret ");
            aWriter.print(LLVMWriterUtils.toType(TypeRef.toType(theSignature.getReturnType())));
            aWriter.println(" %ret");
        } else {
            aWriter.println("    ret void");
        }
        aWriter.println("}");
        aWriter.println();
    }
}