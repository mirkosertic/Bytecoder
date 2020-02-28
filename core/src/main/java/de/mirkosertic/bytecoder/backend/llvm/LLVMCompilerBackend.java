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
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.InvokeStaticMethodExpression;
import de.mirkosertic.bytecoder.ssa.MethodRefExpression;
import de.mirkosertic.bytecoder.ssa.MethodTypeExpression;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.ssa.ProgramGeneratorFactory;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Value;
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
import java.lang.invoke.LambdaMetafactory;
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

    private static class CallSite {
        private final Program program;
        private final RegionNode bootstrapMethod;

        private CallSite(final Program aProgram, final RegionNode aBootstrapMethod) {
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

    private final ProgramGeneratorFactory programGeneratorFactory;

    public LLVMCompilerBackend(final ProgramGeneratorFactory aProgramGeneratorFactory) {
        this.programGeneratorFactory = aProgramGeneratorFactory;
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
        final List<OpaqueReferenceMethod> opaqueReferenceMethods = new ArrayList<>();

        try {
            final List<String> stringPool = new ArrayList<>();
            final Map<String, CallSite> callsites = new HashMap<>();
            final List<BytecodeMethodSignature> theMethodTypes = new ArrayList<>();
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
            };
            final LLVMDebugInformation debugInformation = new LLVMDebugInformation();
            final NativeMemoryLayouter memoryLayouter = new NativeMemoryLayouter(aLinkerContext);

            final File theLLFile = File.createTempFile("llvm", ".ll");
            theLLFile.deleteOnExit();
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
                pw.println("declare float @llvm.maximum.f32(float %Val0, float %Val1)");
                pw.println("declare float @llvm.floor.f32(float  %Val)");
                pw.println("declare float @llvm.ceil.f32(float  %Val)");
                pw.println("declare float @llvm.sqrt.f32(float %Val)");
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
                                // For all the other methods we generate
                                // the JS wrapper implementation later by this compiler
                                opaqueReferenceMethods.add(new OpaqueReferenceMethod(theProvidingClass, t));
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
                pw.println("define internal i32 @instanceof(i32 %object, i32 %typeid) inlinehint {");
                pw.println("entry:");
                pw.println("    %nulltest = icmp eq i32 %object, 0");
                pw.println("    br i1 %nulltest, label %isnull, label %notnull");
                pw.println("notnull:");

                pw.println("    %ptr = add i32 %object, 4");
                pw.println("    %ptr_ptr = inttoptr i32 %ptr to i32*");
                pw.println("    %ptr_loaded = load i32, i32* %ptr_ptr");
                pw.println("    %vtable = inttoptr i32 %ptr_loaded to i32(i32,i32)*");
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

                pw.println("define internal i32 @jlClass_BOOLEANisAssignableFromjlClass(i32 %thisRef, i32 %otherType) {");
                pw.println("entry:");

                aLinkerContext.linkedClasses().forEach(aEntry -> {

                    final BytecodeLinkedClass theLinkedClass = aEntry.targetNode();
                    if (theLinkedClass.emulatedByRuntime()) {
                        return;
                    }

                    if (theLinkedClass.getClassName().equals(BytecodeObjectTypeRef.fromRuntimeClass(Class.class))) {
                        return;
                    }

                    pw.print("    %runtimeclass_");
                    pw.print(theLinkedClass.getUniqueId());
                    pw.print(" = call i32 @");
                    pw.print(LLVMWriterUtils.toClassName(theLinkedClass.getClassName()));
                    pw.print(LLVMWriter.CLASSINITSUFFIX);
                    pw.println("()");

                    pw.print("    %test");
                    pw.print(theLinkedClass.getUniqueId());
                    pw.print(" = icmp eq i32 %otherType, %runtimeclass_");
                    pw.println(theLinkedClass.getUniqueId());

                    pw.print("    br i1 %test");
                    pw.print(theLinkedClass.getUniqueId());
                    pw.print(", label %class");
                    pw.print(theLinkedClass.getUniqueId());
                    pw.print(", label %notclass");
                    pw.println(theLinkedClass.getUniqueId());

                    pw.print("class");
                    pw.print(theLinkedClass.getUniqueId());
                    pw.println(":");

                    for (final BytecodeLinkedClass theImplType : theLinkedClass.getImplementingTypes()) {
                        pw.print("    %runtimeclass_");
                        pw.print(theLinkedClass.getUniqueId());
                        pw.print("_");
                        pw.print(theImplType.getUniqueId());
                        pw.print(" = call i32 @");
                        pw.print(LLVMWriterUtils.toClassName(theImplType.getClassName()));
                        pw.print(LLVMWriter.CLASSINITSUFFIX);
                        pw.println("()");

                        pw.print("    %test_");
                        pw.print(theLinkedClass.getUniqueId());
                        pw.print("_");
                        pw.print(theImplType.getUniqueId());
                        pw.print(" = icmp eq i32 %thisRef, %");
                        pw.print("runtimeclass_");
                        pw.print(theLinkedClass.getUniqueId());
                        pw.print("_");
                        pw.println(theImplType.getUniqueId());

                        pw.print("    br i1 %test_");
                        pw.print(theLinkedClass.getUniqueId());
                        pw.print("_");
                        pw.print(theImplType.getUniqueId());
                        pw.print(", label %assignable, label %test");
                        pw.print(theLinkedClass.getUniqueId());
                        pw.print("_");
                        pw.print(theImplType.getUniqueId());
                        pw.println("_notok");

                        pw.print("test");
                        pw.print(theLinkedClass.getUniqueId());
                        pw.print("_");
                        pw.print(theImplType.getUniqueId());
                        pw.println("_notok:");
                    }
                    pw.println("    ret i32 0");

                    pw.print("notclass");
                    pw.print(theLinkedClass.getUniqueId());
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
                    final BytecodeLinkedClass theLinkedClass = aEntry.targetNode();
                    if (theLinkedClass.emulatedByRuntime()) {
                        return;
                    }

                    final String prefix = "pre" + theLinkedClass.getUniqueId();
                    pw.print("    %");
                    pw.print(prefix);
                    pw.print("_runtimeclass = load i32, i32* @");
                    pw.print(LLVMWriterUtils.toClassName(theLinkedClass.getClassName()));
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

                    if (!theLinkedClass.getClassName().name().equals(Object.class.getName())) {
                        pw.print("    %");
                        pw.print(prefix);
                        pw.print("_superclass = load i32, i32* @");
                        pw.print(LLVMWriterUtils.toClassName(theLinkedClass.getSuperClass().getClassName()));
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
                pw.println("define internal i32 @isnan(float %value) inlinehint {");
                pw.println("entry:");
                pw.println("    %test = fcmp oeq float %value, %value");
                pw.println("    br i1 %test, label %iseq, label %isnoteq");
                pw.println("iseq:");
                pw.println("    ret i32 0");
                pw.println("isnoteq:");
                pw.println("    ret i32 1");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @toi32(float %value) inlinehint {");
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

                pw.println("define internal i32 @compare_i32(i32 %v1, i32 %v2) inlinehint {");
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

                pw.println("define internal i32 @compare_f32(float %v1, float %v2) inlinehint {");
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

                pw.println("define internal i1 @exceedsrange(i32 %v, i32 %min, i32 %max) inlinehint {");
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

                pw.println("define internal float @div_floatfloat(float %a, float %b) inlinehint {");
                pw.println("entry:");
                pw.println("    %result = fdiv float %a, %b");
                pw.println("    ret float %result");
                pw.println("}");
                pw.println();

                pw.println("define internal float @div_i32i32(i32 %a, i32 %b) inlinehint {");
                pw.println("entry:");
                pw.println("    %temp1 = sitofp i32 %a to float");
                pw.println("    %temp2 = sitofp i32 %b to float");
                pw.println("    %result = fdiv float %temp1, %temp2");
                pw.println("    ret float %result");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @maximum(i32 %a, i32 %b) inlinehint {");
                pw.println("entry:");
                pw.println("   %test = icmp sgt i32 %a, %b");
                pw.println("   br i1 %test, label %aisgreater, label %notgreater");
                pw.println("aisgreater:");
                pw.println("   ret i32 %a");
                pw.println("notgreater:");
                pw.println("   ret i32 %b");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @minimum(i32 %a, i32 %b) inlinehint {");
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
                pw.println("define internal i32 @checkmethodtype(i32 %methodType, i32 %index, i32 %expectedType) inlinehint {");
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
                pw.println();

                // Lambda
                pw.println("define internal i32 @lambda__resolvevtableindex(i32 %thisRef,i32 %methodId) {");
                pw.println("    %offset = add i32 %thisRef, 8");
                pw.println("    %offsetptr = inttoptr i32 %offset to i32*");
                pw.println("    %ptr = load i32, i32* %offsetptr");
                pw.println("    ret i32 %ptr");
                pw.println("}");
                pw.println();

                pw.println("define internal i32 @newlambda(i32 %type, i32 %implMethodNumber, i32 %staticArguments) {");
                pw.println("entry:");
                pw.println("    %vtable = ptrtoint i32(i32,i32)* @lambda__resolvevtableindex to i32");
                pw.print("    %allocated = call i32 @");
                pw.print(LLVMWriterUtils.toClassName(theMemoryManagerClass.getClassName()));
                pw.println("_INTnewObjectINTINTINT(i32 0,i32 16,i32 %type,i32 %vtable)");
                pw.println("    %offset1 = add i32 %allocated, 8");
                pw.println("    %offset1ptr = inttoptr i32 %offset1 to i32*");
                pw.println("    store i32 %implMethodNumber, i32* %offset1ptr");
                pw.println("    %offset2 = add i32 %allocated, 12");
                pw.println("    %offset2ptr = inttoptr i32 %offset2 to i32*");
                pw.println("    store i32 %staticArguments, i32* %offset2ptr");
                pw.println("    ret i32 %allocated");
                pw.println("}");
                pw.println();

                // Some utility functions for runtime class management
                pw.println("define internal i32 @runtimeClass__resolvevtableindex(i32 %thisRef,i32 %methodId) {");
                pw.println("entry:");
                final BytecodeLinkedClass theClassLinkedCass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Class.class));
                final BytecodeResolvedMethods theRuntimeMethodMap = theClassLinkedCass.resolvedMethods();
                theRuntimeMethodMap.stream().forEach(aMethodMapEntry -> {
                    final BytecodeMethod theMethod = aMethodMapEntry.getValue();
                    if (!theMethod.getAccessFlags().isStatic() && !theMethod.isConstructor() && !theMethod.isClassInitializer() &&
                            aMethodMapEntry.getProvidingClass().getClassName().equals(BytecodeObjectTypeRef.fromRuntimeClass(Class.class))) {

                        final BytecodeVirtualMethodIdentifier theMethodIdentifier = aLinkerContext.getMethodCollection().identifierFor(theMethod);
                        pw.print("    %test");
                        pw.print(theMethodIdentifier.getIdentifier());
                        pw.print(" = icmp eq i32 %methodId, ");
                        pw.println(theMethodIdentifier.getIdentifier());

                        pw.print("    br i1 %test");
                        pw.print(theMethodIdentifier.getIdentifier());
                        pw.print(", label %test");
                        pw.print(theMethodIdentifier.getIdentifier());
                        pw.print("_ok, label %test");
                        pw.print(theMethodIdentifier.getIdentifier());
                        pw.println("_notok");

                        pw.print("test");
                        pw.print(theMethodIdentifier.getIdentifier());
                        pw.println("_ok:");

                        if (Objects.equals("getClass", theMethod.getName().stringValue())) {
                            pw.println("    call void @llvm.trap()");
                            pw.println("    unreachable");
                        } else {
                            // delegate to the corresponding method of java.lang.Class
                            final String theMethodName = LLVMWriterUtils.toMethodName(aMethodMapEntry.getProvidingClass().getClassName(),
                                    theMethod.getName(), theMethod.getSignature());
                            pw.print("    %v");
                            pw.print(theMethodIdentifier.getIdentifier());
                            pw.print(" = ptrtoint ");
                            pw.print(LLVMWriterUtils.toSignature(theMethod.getSignature()));
                            pw.print("* @");
                            pw.print(theMethodName);
                            pw.println(" to i32");

                            pw.print("    ret i32 %v");
                            pw.println(theMethodIdentifier.getIdentifier());
                        }

                        pw.print("test");
                        pw.print(theMethodIdentifier.getIdentifier());
                        pw.println("_notok:");
                    }
                });
                pw.println("    call void @llvm.trap()");
                pw.println("    unreachable");
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
                aLinkerContext.linkedClasses().forEach(aEntry -> {

                    final BytecodeLinkedClass theLinkedClass = aEntry.targetNode();

                    if (theLinkedClass.getBytecodeClass().getAccessFlags().isInterface() || theLinkedClass.isOpaqueType()) {
                        return;
                    }

                    if (Objects.equals(theLinkedClass.getClassName(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                        return;
                    }

                    if (theLinkedClass.emulatedByRuntime()) {
                        return;
                    }

                    pw.print("    %class_");
                    pw.print(theLinkedClass.getUniqueId());
                    pw.print(" = load i32, i32* @");
                    pw.println(theSymbolResolver.globalFromStringPool(theLinkedClass.getClassName().name()));

                    pw.print("    %test_");
                    pw.print(theLinkedClass.getUniqueId());
                    pw.print(" = call i32 @jlString_BOOLEANequalsjlObject(i32 %class_");
                    pw.print(theLinkedClass.getUniqueId());
                    pw.println(",i32 %name)");

                    pw.print("    %check_");
                    pw.print(theLinkedClass.getUniqueId());
                    pw.print(" = icmp eq i32 1, %test_");
                    pw.println(theLinkedClass.getUniqueId());

                    pw.print("    br i1 %check_");
                    pw.print(theLinkedClass.getUniqueId());
                    pw.print(", label %check_ok_");
                    pw.print(theLinkedClass.getUniqueId());
                    pw.print(", label %check_notok_");
                    pw.println(theLinkedClass.getUniqueId());

                    pw.print("check_ok_");
                    pw.print(theLinkedClass.getUniqueId());
                    pw.println(":");

                    pw.print("    %init_");
                    pw.println(theLinkedClass.getUniqueId());
                    pw.print(" = call i32 @");
                    pw.print(LLVMWriterUtils.toClassName(theLinkedClass.getClassName()));
                    pw.print(LLVMWriter.CLASSINITSUFFIX);
                    pw.println("()");
                    pw.print("    ret i32 %init_");
                    pw.println(theLinkedClass.getUniqueId());

                    pw.print("check_notok_");
                    pw.print(theLinkedClass.getUniqueId());
                    pw.println(":");
                });
                pw.println("    call void @llvm.trap()");
                pw.println("    unreachable");
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
                        pw.print("define internal i32 @");
                        pw.print(theClassName);
                        pw.print(LLVMWriter.CLASSINITSUFFIX);
                        pw.println("() inlinehint {");
                        pw.println("entry:");
                        pw.print("    %class = load i32, i32* @");
                        pw.print(theClassName);
                        pw.println(LLVMWriter.RUNTIMECLASSSUFFIX);
                        pw.println("    %status = add i32 %class, 8");
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
                            pw.println("_VOID$clinit$(i32 %class)");
                        }

                        pw.println("    br label %done");
                        pw.println("done:");
                        pw.println("    ret i32 %class");
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
                            for (int i = 0; i < theMethod.getSignature().getArguments().length; i++) {
                                if (i > 0) {
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
                            pw.print("    %class = call i32 @");
                            pw.print(theClassName);
                            pw.print(LLVMWriter.CLASSINITSUFFIX);
                            pw.println("()");

                            pw.print("    %vtableptr = ptrtoint i32(i32,i32)* @");
                            pw.print(theClassName);
                            pw.print(LLVMWriter.VTABLEFUNCTIONSUFFIX);
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

                        try (final LLVMWriter theWriter = new LLVMWriter(pw, memoryLayouter, aLinkerContext, theSymbolResolver)) {
                            theWriter.write(theSSAProgram, subProgram);
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
                    });
                });

                // Generate callsite resolver code
                for (final Map.Entry<String, CallSite> theEntry : callsites.entrySet()) {

                    final CallSite callsite = theEntry.getValue();

                    // At this point, we need to determine if there are lambdas generated
                    // so we can generate the adapter methods at compile time. This needs
                    // to be done because WebAssembly cannot generate code at runtime

                    final RegionNode theBootStrapcode = theEntry.getValue().bootstrapMethod;
                    for (final Expression theExpression : theBootStrapcode.getExpressions().toList()) {
                        final List<Value> theIncomingDataFlows = theExpression.incomingDataFlows();
                        for (final Value theValue : theIncomingDataFlows) {
                            if (theValue instanceof InvokeStaticMethodExpression) {
                                final InvokeStaticMethodExpression theStatic = (InvokeStaticMethodExpression) theValue;
                                if (theStatic.getClassName().name().equals(LambdaMetafactory.class.getName()) && theStatic.getMethodName().equals("metafactory")) {
                                    // We have a winner
                                    final List<Value> theArguments = new ArrayList<>();
                                    for (final Value theArg : theStatic.incomingDataFlows()) {
                                        if (theArg instanceof Variable) {
                                            theArguments.add(theArg.incomingDataFlows().get(0));
                                        } else {
                                            theArguments.add(theArg);
                                        }
                                    }
                                    final MethodTypeExpression theStaticInvocationType = (MethodTypeExpression) theArguments.get(2);
                                    final MethodTypeExpression theDynamicInvocationType = (MethodTypeExpression) theArguments.get(5);
                                    final MethodRefExpression theImplementationMethod = (MethodRefExpression) theArguments.get(4);

                                    if (theStaticInvocationType.getSignature().getArguments().length == 0) {
                                        // If we have no static invocation arguments, we do not need to generate an adapter method
                                        // We can directly refer to the implementation method
                                    } else {
                                        // We need to create an adapter method to make the two signatures compatible
                                        final BytecodeMethodSignature theImplementationSignature = theImplementationMethod.getSignature();

                                        final String theAdapterFunctionName = LLVMWriterUtils.toMethodName(theImplementationMethod.getClassName(),
                                                theImplementationMethod.getMethodName() + theEntry.getKey(), theDynamicInvocationType.getSignature());

                                        final String theImplementationOriginalMethodName = theImplementationMethod.getMethodName();

                                        // This is our new implementation
                                        // We can safely rename the method here
                                        theImplementationMethod.retargetToMethodName(theImplementationMethod.getMethodName() + theEntry.getKey());
                                        theImplementationMethod.retargetToSignature(theDynamicInvocationType.getSignature());

                                        if (theImplementationSignature.getReturnType().isVoid()) {
                                            pw.print("define internal void @");
                                        } else {
                                            pw.print("define internal ");
                                            pw.print(LLVMWriterUtils.toType(TypeRef.toType(theImplementationSignature.getReturnType())));
                                            pw.print("@");
                                        }

                                        pw.print(theAdapterFunctionName);
                                        pw.print("(i32 %selfRef");
                                        for (int i=0;i<theDynamicInvocationType.getSignature().getArguments().length;i++) {
                                            pw.print(",");
                                            pw.print(LLVMWriterUtils.toType(TypeRef.toType(theDynamicInvocationType.getSignature().getArguments()[i])));
                                            pw.print(" %arg");
                                            pw.print(i);
                                        }
                                        pw.println(") {");

                                        final List<String> theDispatchArguments = new ArrayList<>();

                                        switch (theImplementationMethod.getReferenceKind()) {
                                            case REF_invokeStatic:
                                                theDispatchArguments.add("i32 -1");
                                            case REF_invokeSpecial:
                                            case REF_invokeVirtual: {
                                                pw.println("    %base_offset = add i32 %selfRef, 12");
                                                pw.println("    %base_offset_ptr = inttoptr i32 %base_offset to i32*");
                                                pw.println("    %base_ptr = load i32, i32* %base_offset_ptr");

                                                // Add static arguments
                                                for (int i=0;i<theStaticInvocationType.getSignature().getArguments().length;i++) {

                                                    pw.print("    %static");
                                                    pw.print(i);
                                                    pw.print("_offset = add i32 %base_ptr, ");
                                                    pw.println(20 + i * 4);

                                                    switch (TypeRef.toType(theStaticInvocationType.getSignature().getArguments()[i]).resolve()) {
                                                        case DOUBLE:
                                                        case FLOAT: {
                                                            pw.print("    %static");
                                                            pw.print(i);
                                                            pw.print("_ptr = inttoptr i32 %static");
                                                            pw.print(i);
                                                            pw.println("_offset to float*");

                                                            pw.print("    %static");
                                                            pw.print(i);
                                                            pw.print("_value = load f32, f32* %static");
                                                            pw.print(i);
                                                            pw.println("_ptr");

                                                            theDispatchArguments.add("f32 %static" + i + "_value");
                                                            break;
                                                        }
                                                        default: {
                                                            pw.print("    %static");
                                                            pw.print(i);
                                                            pw.print("_ptr = inttoptr i32 %static");
                                                            pw.print(i);
                                                            pw.println("_offset to i32*");

                                                            pw.print("    %static");
                                                            pw.print(i);
                                                            pw.print("_value = load i32, i32* %static");
                                                            pw.print(i);
                                                            pw.println("_ptr");

                                                            theDispatchArguments.add("i32 %static" + i + "_value");
                                                            break;
                                                        }
                                                    }
                                                }

                                                // Add dynamic arguments
                                                for (int i=0;i<theDynamicInvocationType.getSignature().getArguments().length;i++) {
                                                    theDispatchArguments.add(LLVMWriterUtils.toType(TypeRef.toType(theDynamicInvocationType.getSignature().getArguments()[i]))
                                                    + " %arg" + i);
                                                }

                                                if (theImplementationSignature.getReturnType().isVoid()) {
                                                    pw.print("    call void @");
                                                    pw.print(LLVMWriterUtils.toMethodName(
                                                            theImplementationMethod.getClassName(),
                                                            theImplementationOriginalMethodName,
                                                            theImplementationSignature
                                                    ));
                                                    pw.print("(");
                                                    for (int i = 0; i < theDispatchArguments.size(); i++) {
                                                        if (i > 0) {
                                                            pw.print(",");
                                                        }
                                                        pw.print(theDispatchArguments.get(i));
                                                    }
                                                    pw.println(")");
                                                    pw.println("    ret void");
                                                } else {
                                                    pw.print("    %result = call ");
                                                    pw.print(LLVMWriterUtils.toType(TypeRef.toType(theImplementationSignature.getReturnType())));
                                                    pw.print("  @");
                                                    pw.print(LLVMWriterUtils.toMethodName(
                                                            theImplementationMethod.getClassName(),
                                                            theImplementationOriginalMethodName,
                                                            theImplementationSignature
                                                    ));
                                                    pw.print("(");
                                                    for (int i = 0; i < theDispatchArguments.size(); i++) {
                                                        if (i > 0) {
                                                            pw.print(",");
                                                        }
                                                        pw.print(theDispatchArguments.get(i));
                                                    }
                                                    pw.println(")");

                                                    pw.print("    ret ");
                                                    pw.print(LLVMWriterUtils.toType(TypeRef.toType(theImplementationSignature.getReturnType())));
                                                    pw.println(" %result");

                                                }
                                                break;
                                            }
                                            case REF_invokeInterface: {

                                                pw.println("    %base_offset = add i32 %selfRef, 12");
                                                pw.println("    %base_offset_ptr = inttoptr i32 %base_offset to i32*");
                                                pw.println("    %base_ptr = load i32, i32* %base_offset_ptr");

                                                // Add static arguments
                                                for (int i=0;i<theStaticInvocationType.getSignature().getArguments().length;i++) {

                                                    pw.print("    %static");
                                                    pw.print(i);
                                                    pw.print("_offset = add i32 %base_ptr, ");
                                                    pw.println(20 + i * 4);

                                                    switch (TypeRef.toType(theStaticInvocationType.getSignature().getArguments()[i]).resolve()) {
                                                        case DOUBLE:
                                                        case FLOAT: {
                                                            pw.print("    %static");
                                                            pw.print(i);
                                                            pw.print("_ptr = inttoptr i32 %static");
                                                            pw.print(i);
                                                            pw.println("_offset to float*");

                                                            pw.print("    %static");
                                                            pw.print(i);
                                                            pw.print("_value = load f32, f32* %static");
                                                            pw.print(i);
                                                            pw.println("_ptr");

                                                            theDispatchArguments.add("f32 %static" + i + "_value");
                                                            break;
                                                        }
                                                        default: {
                                                            pw.print("    %static");
                                                            pw.print(i);
                                                            pw.print("_ptr = inttoptr i32 %static");
                                                            pw.print(i);
                                                            pw.println("_offset to i32*");

                                                            pw.print("    %static");
                                                            pw.print(i);
                                                            pw.print("_value = load i32, i32* %static");
                                                            pw.print(i);
                                                            pw.println("_ptr");

                                                            theDispatchArguments.add("i32 %static" + i + "_value");
                                                            break;
                                                        }
                                                    }
                                                }

                                                // Add dynamic arguments
                                                for (int i=0;i<theDynamicInvocationType.getSignature().getArguments().length;i++) {
                                                    theDispatchArguments.add(LLVMWriterUtils.toType(TypeRef.toType(theDynamicInvocationType.getSignature().getArguments()[i]))
                                                            + " %arg" + i);
                                                }

                                                final BytecodeMethodSignature theInvocationSignature = theDynamicInvocationType.getSignature();
                                                final BytecodeVirtualMethodIdentifier theMethodIdentifier = aLinkerContext.getMethodCollection().identifierFor(theImplementationOriginalMethodName, theInvocationSignature);

                                                final String theCalledFunction = LLVMWriterUtils.toSignature(theInvocationSignature);

                                                pw.print("    %ptr = add ");
                                                pw.print(theDispatchArguments.get(0));
                                                pw.println(", 4");

                                                pw.println("    %vtable = inttoptr i32 %ptr to i32(i32,i32)*");
                                                pw.print("    %resolved = call i32(i32,i32) %vtable(i32 %selfRef, i32 ");
                                                pw.print(theMethodIdentifier.getIdentifier());
                                                pw.println(")");

                                                pw.print("    %resolved_ptr = inttoptr i32 %resolved to ");
                                                pw.print(theCalledFunction);
                                                pw.println("*");

                                                if (theImplementationSignature.getReturnType().isVoid()) {
                                                    pw.print("    call ");
                                                    pw.print(theCalledFunction);
                                                    pw.print(" %resolved_ptr (");
                                                    for (int i = 0; i < theDispatchArguments.size(); i++) {
                                                        if (i > 0) {
                                                            pw.print(",");
                                                        }
                                                        pw.print(theDispatchArguments.get(i));
                                                    }
                                                    pw.println(")");
                                                    pw.println("    ret void");
                                                } else {
                                                    pw.print("    %result = call ");
                                                    pw.print(theCalledFunction);
                                                    pw.print(" %resolved_ptr (");
                                                    for (int i = 0; i < theDispatchArguments.size(); i++) {
                                                        if (i > 0) {
                                                            pw.print(",");
                                                        }
                                                        pw.print(theDispatchArguments.get(i));
                                                    }
                                                    pw.println(")");

                                                    pw.print("    ret ");
                                                    pw.print(LLVMWriterUtils.toType(TypeRef.toType(theImplementationSignature.getReturnType())));
                                                    pw.println(" %result");

                                                }
                                                break;
                                            }
                                            default:
                                                throw new IllegalStateException("Not implemented : " + theImplementationMethod.getReferenceKind() + " for LambdaMetaFactory!");
                                        }

                                        pw.println("}");
                                        pw.println();
                                    }
                                }
                            }
                        }
                    }

                    pw.print("@");
                    pw.print("callsite");
                    pw.print(System.identityHashCode(callsite));
                    pw.println(" = private global i32 0");

                    final Program theSSAProgram = theEntry.getValue().program;
                    final LLVMDebugInformation.CompileUnit compileUnit = debugInformation.compileUnitFor("/resolvecallsite" + callsite);
                    final LLVMDebugInformation.SubProgram subProgram = compileUnit.subProgram(theSSAProgram, "/resolvecallsite" + callsite, new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[0]));

                    // Run optimizer
                    // We use a special LLVM optimizer, which does only stuff LLVM CANNOT do, such
                    // as virtual method invocation optimization. All other optimization work
                    // is done by LLVM!
                    KnownOptimizer.LLVM.optimize(theSSAProgram.getControlFlowGraph(), aLinkerContext);

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

                    try (final LLVMWriter theWriter = new LLVMWriter(pw, memoryLayouter, aLinkerContext, theSymbolResolver)) {
                        theWriter.write(theEntry.getValue().program, subProgram);
                    }

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
                    pw.print("_classnameptr = ptrtoint i32* @");
                    pw.print(theSymbolResolver.globalFromStringPool(theLinkedClass.getClassName().name()));
                    pw.println(" to i32");

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
                    pw.print(",i32 %");
                    pw.print(theClassName);
                    pw.print(LLVMWriter.RUNTIMECLASSSUFFIX);
                    pw.println("_classnameptr)");

                    pw.print("    store i32 %");
                    pw.print(theClassName);
                    pw.print(LLVMWriter.RUNTIMECLASSSUFFIX);
                    pw.print("_allocated, i32* @");
                    pw.print(theClassName);
                    pw.println(LLVMWriter.RUNTIMECLASSSUFFIX);
                });

                pw.println("    %arrayvtableptr = ptrtoint i32(i32,i32)* @dmbcArray__resolvevtableindex to i32");
                pw.println("    %arrayclassinit = call i32 @dmbcArray__init()");

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
                        pw.println(20 + j * 4);

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
                    pw.print(" = call i32 @jlString_VOID$newInstanceA1BYTEBYTE(i32 ");
                    pw.print("%allocated_");
                    pw.print(i);
                    pw.println(", i32 0)");

                    pw.print("    store i32 %string_");
                    pw.print(i);
                    pw.print(", i32* @");
                    pw.print("strpool_");
                    pw.println(i);
                }

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

                // Factory for method types
                for (int j=0;j<theMethodTypes.size();j++) {

                    final BytecodeMethodSignature theSignature = theMethodTypes.get(j);

                    pw.print("define internal i32 @methodtypefactory");
                    pw.print(j);
                    pw.println("() {");
                    pw.println("entry:");

                    pw.println("    %data_classinit = call i32 @dmbcArray__init()");
                    pw.println("    %data_vtable = ptrtoint i32(i32,i32)* @dmbcArray__resolvevtableindex to i32");
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

                // We need to generate the callbacks
                aLinkerContext.linkedClasses().map(Edge::targetNode).filter(t -> t.isCallback() && t.getBytecodeClass().getAccessFlags().isInterface()).forEach(t -> {

                    final BytecodeResolvedMethods theMethods = t.resolvedMethods();
                    final List<BytecodeMethod> availableCallbacks = theMethods.stream().filter(x -> !x.getValue().isConstructor() && !x.getValue().isClassInitializer()
                            && x.getProvidingClass() == t).map(BytecodeResolvedMethods.MethodEntry::getValue).collect(Collectors.toList());

                    if (availableCallbacks.size() > 0) {

                        if (availableCallbacks.size() != 1) {
                            throw new IllegalStateException(
                                    "Invalid number of callback methods available for type " + t.getClassName().name()
                                            + ", expected 1, got " + availableCallbacks.size());
                        }

                        final BytecodeMethod theDelegateMethod = availableCallbacks.get(0);

                        final BytecodeVirtualMethodIdentifier theMethodIdentifier = aLinkerContext.getMethodCollection().identifierFor(theDelegateMethod);

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
                        pw.println("    %vtable = inttoptr i32 %ptr_loaded to i32(i32,i32)*");
                        pw.print("    %resolved = call i32(i32,i32) %vtable(i32 %target, i32 ");
                        pw.print(theMethodIdentifier.getIdentifier());
                        pw.println(")");
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
                theWriter.println("                    skip0LONGLONG: function(handle,amount) {");
                theWriter.println("                        var remaining = this.size - this.currentpos;");
                theWriter.println("                        var possible = Math.min(remaining, amount);");
                theWriter.println("                        this.currentpos+=possible;");
                theWriter.println("                        return possible;");
                theWriter.println("                    },");
                theWriter.println("                    available0LONG: function(handle) {");
                theWriter.println("                        return this.size - this.currentpos;");
                theWriter.println("                    },");
                theWriter.println("                    read0LONG: function(handle) {");
                theWriter.println("                        return this.data[this.currentpos++];");
                theWriter.println("                    },");
                theWriter.println("                    readBytesLONGL1BYTEINTINT: function(handle,target,offset,length) {");
                theWriter.println("                        if (length === 0) {");
                theWriter.println("                            return 0;");
                theWriter.println("                        }");
                theWriter.println("                        var remaining = this.size - this.currentpos;");
                theWriter.println("                        var possible = Math.min(remaining, length);");
                theWriter.println("                        if (possible === 0) {");
                theWriter.println("                            return -1;");
                theWriter.println("                        }");
                theWriter.println("                        for (var j=0;j<possible;j++) {");
                theWriter.println("                            bytecoder.runningInstanceMemory[target + 20 + offset * 4]=this.data[this.currentpos++];");
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
                theWriter.println("             writeBytesLONGL1BYTEINTINT: function(handle, data, offset, length) {");
                theWriter.println("                 if (length > 0) {");
                theWriter.println("                     var array = new Uint8Array(length);");
                theWriter.println("                     data+=20;");
                theWriter.println("                     for (var i = 0; i < length; i++) {");
                theWriter.println("                         array[i] = bytecoder.intInMemory(data);");
                theWriter.println("                         data+=4;");
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
                theWriter.println("             close0LONG: function(handle) {");
                theWriter.println("             },");
                theWriter.println("             writeIntLONGINT: function(handle,value) {");
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
                theWriter.println("         bytecoder.exports.initDefaultFileHandles(-1,0,1,2);");
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
                theWriter.println("             value = value + 4;");
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
                theWriter.println("             debug: function(thisref, f1) {console.log(f1);}");
                theWriter.println("         },");
                theWriter.println("         system: {");
                theWriter.println("             currentTimeMillis: function() {return Date.now();},");
                theWriter.println("             nanoTime: function() {return Date.now() * 1000000;},");
                theWriter.println("         },");
                theWriter.println("         vm: {");
                theWriter.println("             newRuntimeGeneratedTypeStringMethodTypeMethodHandleObject: function() {},");
                theWriter.println("         },");
                theWriter.println("         memorymanager: {");
                theWriter.println("             isUsedAsCallbackINT : function(thisref, ptr) {");
                theWriter.println("                 return bytecoder.callbacks.includes(ptr);");
                theWriter.println("             },");
                theWriter.println("             printObjectDebugInternalObjectINTINTBOOLEANBOOLEAN: function(thisref, ptr, indexAlloc, indexFree, usedByStack, usedByHeap) {");
                theWriter.println("                 console.log('Memory debug for ' + ptr);");
                theWriter.println("                 var theAllocatedBlock = ptr - 12;");
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

                theWriter.println("         fileoutputstream : {");
                theWriter.println("             writeBytesLONGL1BYTEINTINT : function(thisref, handle, data, offset, length) {");
                theWriter.println("                 bytecoder.filehandles[handle].writeBytesLONGL1BYTEINTINT(handle,data,offset,length);");
                theWriter.println("             },");
                theWriter.println("             writeIntLONGINT : function(thisref, handle, intvalue) {");
                theWriter.println("                 bytecoder.filehandles[handle].writeIntLONGINT(handle,intvalue);");
                theWriter.println("             },");
                theWriter.println("             close0LONG : function(thisref,handle) {");
                theWriter.println("                 bytecoder.filehandles[handle].close0LONG(handle);");
                theWriter.println("             },");
                theWriter.println("         },");

                theWriter.println("         fileinputstream : {");
                theWriter.println("             open0String : function(thisref,name) {");
                theWriter.println("                 return bytecoder.openForRead(bytecoder.toJSString(name));");
                theWriter.println("             },");
                theWriter.println("             read0LONG : function(thisref,handle) {");
                theWriter.println("                 return bytecoder.filehandles[handle].read0LONG(handle);");
                theWriter.println("             },");
                theWriter.println("             readBytesLONGL1BYTEINTINT : function(thisref,handle,data,offset,length) {");
                theWriter.println("                 return bytecoder.filehandles[handle].readBytesLONGL1BYTEINTINT(handle,data,offset,length);");
                theWriter.println("             },");
                theWriter.println("             skip0LONGLONG : function(thisref,handle,amount) {");
                theWriter.println("                 return bytecoder.filehandles[handle].skip0LONGLONG(handle,amount);");
                theWriter.println("             },");
                theWriter.println("             available0LONG : function(thisref,handle) {");
                theWriter.println("                 return bytecoder.filehandles[handle].available0LONG(handle);");
                theWriter.println("             },");
                theWriter.println("             close0LONG : function(thisref,handle) {");
                theWriter.println("                 bytecoder.filehandles[handle].close0LONG(handle);");
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

                final Map<String, List<OpaqueReferenceMethod>> theMethods = opaqueReferenceMethods.stream().collect(Collectors.groupingBy(opaqueReferenceMethod -> opaqueReferenceMethod.linkedClass.linkfor(opaqueReferenceMethod.getMethod()).getModuleName()));
                for (final Map.Entry<String, List<OpaqueReferenceMethod>> theEntry : theMethods.entrySet()) {

                    theWriter.print("         ");
                    theWriter.print(theEntry.getKey());
                    theWriter.println(": {");

                    for (final OpaqueReferenceMethod theMethod : theEntry.getValue()) {
                        final BytecodeMethod theBytecdeMethod = theMethod.getMethod();

                        final BytecodeImportedLink theImportedLink = theMethod.getLinkedClass().linkfor(theBytecdeMethod);
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

                                final String theConversionFunction = conversionFunctionToJSForOpaqueType(aLinkerContext, theSignature.getArguments()[0]);
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

                                        final List<BytecodeMethod> theCallbackMethods = theLinkedClass.resolvedMethods().stream().filter(x -> x.getProvidingClass() == theLinkedClass).map(BytecodeResolvedMethods.MethodEntry::getValue).collect(Collectors.toList());
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
            theLLCommand.add("llc-10");
            theLLCommand.add("-O3");
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
                        System.out.println(line);
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
                        System.out.println(line);
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
}
