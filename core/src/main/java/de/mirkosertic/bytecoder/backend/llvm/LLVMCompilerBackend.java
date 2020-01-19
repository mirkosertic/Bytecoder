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
import de.mirkosertic.bytecoder.backend.CompileBackend;
import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.CompileResult;
import de.mirkosertic.bytecoder.backend.NativeMemoryLayouter;
import de.mirkosertic.bytecoder.classlib.Address;
import de.mirkosertic.bytecoder.classlib.MemoryManager;
import de.mirkosertic.bytecoder.core.BytecodeAnnotation;
import de.mirkosertic.bytecoder.core.BytecodeImportedLink;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeResolvedMethods;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.intrinsics.Intrinsics;
import de.mirkosertic.bytecoder.optimizer.KnownOptimizer;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.ssa.ProgramGeneratorFactory;
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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class LLVMCompilerBackend implements CompileBackend<LLVMCompileResult> {

    private final ProgramGeneratorFactory programGeneratorFactory;

    public LLVMCompilerBackend(final ProgramGeneratorFactory aProgramGeneratorFactory) {
        this.programGeneratorFactory = aProgramGeneratorFactory;
    }

    @Override
    public LLVMCompileResult generateCodeFor(final CompileOptions aOptions, final BytecodeLinkerContext aLinkerContext,
            final Class aEntryPointClass, final String aEntryPointMethodName, final BytecodeMethodSignature aEntryPointSignatue) {

        // We need to link the memory manager
        final BytecodeLinkedClass theMemoryManagerClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class));

        theMemoryManagerClass.resolveStaticMethod("freeMem", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.LONG, new BytecodeTypeRef[0]));
        theMemoryManagerClass.resolveStaticMethod("usedMem", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.LONG, new BytecodeTypeRef[0]));

        theMemoryManagerClass.resolveStaticMethod("free", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT}));
        theMemoryManagerClass.resolveStaticMethod("malloc", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT}));
        theMemoryManagerClass.resolveStaticMethod("newObject", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
        theMemoryManagerClass.resolveStaticMethod("newArray", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
        theMemoryManagerClass.resolveStaticMethod("newArray", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

        try {
            final NativeMemoryLayouter memoryLayouter = new NativeMemoryLayouter(aLinkerContext);

            final File theLLFile = File.createTempFile("llvm", ".ll");
            try (final PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(theLLFile), StandardCharsets.UTF_8))) {
                // We write the header first
                pw.println("target triple = \"wasm32-unknown-unknown\"");
                pw.println();

                pw.print("@stacktop = global i32 0");
                pw.println();
                pw.println();

                pw.print("declare i32 @llvm.wasm.memory.size.i32(i32) nounwind readonly");
                pw.println();
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

                // Now, we can continue to write implementation code
                aLinkerContext.linkedClasses().forEach(aEntry -> {

                    final BytecodeLinkedClass theLinkedClass = aEntry.targetNode();

                    if (Objects.equals(aEntry.targetNode().getClassName(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                        return;
                    }
                    if (theLinkedClass.emulatedByRuntime()) {
                        return;
                    }
                    // Hack for unit-testing
                    if (!theLinkedClass.getClassName().name().endsWith("LLVMCompilerBackendTest") && !theLinkedClass.getClassName().name().endsWith("MemoryManager")) {
                        return;
                    }

                    final BytecodeResolvedMethods theMethodMap = theLinkedClass.resolvedMethods();
                    final String theClassName = LLVMWriterUtils.toClassName(aEntry.targetNode().getClassName());

                    // TODO: implement class init function

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

                        // TODO: generate vtable resolver function
                        pw.print("define internal i32 @");
                        pw.print(theClassName);
                        pw.print(LLVMWriter.VTABLEFUNCTIONSUFFIX);
                        pw.println("(i32 %thisRef,i32 %methodId) {");
                        pw.println("entry:");
                        pw.println("    switch i32 %methodId, label %default [");
                        pw.print("        i32 ");
                        pw.print(LLVMWriter.GENERATED_INSTANCEOF_METHOD_ID);
                        pw.println(",label %instanceof");
                        pw.println("    ]");
                        pw.println("default:");
                        pw.println("    unreachable");
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
                            if (aMethodMapEntry.getValue().getAccessFlags().isStatic() && !aMethodMapEntry.getValue().isClassInitializer()) {
                                // We need to create a delegate function here
                                if (!theMethodMap.isImplementedBy(aMethodMapEntry.getValue(), theLinkedClass)) {
                                    // TODO: generate delegate function for static methods
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
                            // TODO: Invoke constructor
                            pw.println("    ret i32 %allocated");
                            pw.println("}");
                            pw.println();
                            return;
                        }

                        final ProgramGenerator theGenerator = programGeneratorFactory.createFor(aLinkerContext, new Intrinsics());
                        final Program theSSAProgram = theGenerator.generateFrom(aMethodMapEntry.getProvidingClass().getBytecodeClass(), theMethod);

                        // Run optimizer
                        // We use a special LLVM optimizer, which does only stuff LLVM CANNOT do, such
                        // as virtual method invocation optimization. All other optimization work
                        // is done by LLVM!
                        KnownOptimizer.LLVM.optimize(theSSAProgram.getControlFlowGraph(), aLinkerContext);

                        // Now, we can generate the instance method here
                        final String methodName = LLVMWriterUtils
                                .toMethodName(theLinkedClass.getClassName(), theMethod.getName(), theSignature);

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
                        }

                        pw.print("define ");
                        if (theExport == null) {
                            pw.print("internal ");
                        }
                        pw.print(LLVMWriterUtils.toType(TypeRef.toType(theSignature.getReturnType())));
                        pw.print(" @");
                        pw.print(methodName);
                        pw.print("(");

                        pw.print(LLVMWriterUtils.toType(TypeRef.Native.REFERENCE));
                        pw.print(" ");
                        if (theMethod.getAccessFlags().isStatic()) {
                            pw.print("%UNUSED");
                        } else {
                            pw.print("%thisRef");
                        }
                        final List<Variable> theArguments = theSSAProgram.getArguments();
                        for (final Variable theArgument : theArguments) {
                            final TypeRef theParamType = theArgument.resolveType();
                            pw.print(",");
                            pw.print(LLVMWriterUtils.toType(theParamType));
                            pw.print(" ");
                            pw.print("%");
                            pw.print(theArgument.getName());
                            pw.print("_");
                        }

                        if (theExport != null) {
                            pw.print(") #");
                            pw.print(attributeCounter.get());
                            pw.println(" {");
                            attributeCounter.incrementAndGet();
                        } else {
                            pw.println(") {");
                        }

                        try (final LLVMWriter theWriter = new LLVMWriter(pw)) {
                            theWriter.write(theSSAProgram);
                        }

                        pw.println("}");
                        pw.println();
                    });
                });
            }

            final LLVMCompileResult theCompileResult = new LLVMCompileResult();
            try (final Reader reader = new InputStreamReader(new FileInputStream(theLLFile))) {
                final String theLLContent = IOUtils.toString(reader);
                theCompileResult.add(new CompileResult.StringContent("bytecoder.ll", theLLContent));
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
                    theCompileResult.add(new CompileResult.BinaryContent(theObjectFileName,
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
                try (final BufferedReader processOutput = new BufferedReader(new InputStreamReader(theLinkerProcess.getErrorStream()))) {
                    String line;
                    while ((line = processOutput.readLine()) != null) {
                        System.out.println(line);
                    }
                }
            } else {
                try (final FileInputStream inputStream = new FileInputStream(new File(theLLFile.getParent(), theWASMFileName))) {
                    theCompileResult.add(new CompileResult.BinaryContent(theWASMFileName,
                            IOUtils.toByteArray(inputStream)));
                }
            }

            return theCompileResult;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
