/*
 * Copyright 2018 Mirko Sertic
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
package de.mirkosertic.bytecoder.backend.wasm;

import static de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions.call;
import static de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions.currentMemory;
import static de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions.getGlobal;
import static de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions.getLocal;
import static de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions.i32;
import static de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions.param;
import static de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions.weakFunctionReference;
import static de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions.weakFunctionTableReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import de.mirkosertic.bytecoder.api.EmulatedByRuntime;
import de.mirkosertic.bytecoder.api.Export;
import de.mirkosertic.bytecoder.backend.CompileBackend;
import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.ConstantPool;
import de.mirkosertic.bytecoder.backend.wasm.ast.Block;
import de.mirkosertic.bytecoder.backend.wasm.ast.Callable;
import de.mirkosertic.bytecoder.backend.wasm.ast.ExportableFunction;
import de.mirkosertic.bytecoder.backend.wasm.ast.Exporter;
import de.mirkosertic.bytecoder.backend.wasm.ast.Function;
import de.mirkosertic.bytecoder.backend.wasm.ast.Iff;
import de.mirkosertic.bytecoder.backend.wasm.ast.WASMType;
import de.mirkosertic.bytecoder.backend.wasm.ast.Global;
import de.mirkosertic.bytecoder.backend.wasm.ast.GlobalsIndex;
import de.mirkosertic.bytecoder.backend.wasm.ast.ImportReference;
import de.mirkosertic.bytecoder.backend.wasm.ast.Local;
import de.mirkosertic.bytecoder.backend.wasm.ast.Module;
import de.mirkosertic.bytecoder.backend.wasm.ast.Param;
import de.mirkosertic.bytecoder.backend.wasm.ast.PrimitiveType;
import de.mirkosertic.bytecoder.backend.wasm.ast.WASMValue;
import de.mirkosertic.bytecoder.classlib.Address;
import de.mirkosertic.bytecoder.classlib.ExceptionManager;
import de.mirkosertic.bytecoder.classlib.MemoryManager;
import de.mirkosertic.bytecoder.core.BytecodeAnnotation;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
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
import de.mirkosertic.bytecoder.relooper.Relooper;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.ssa.ProgramGeneratorFactory;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.StringValue;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Variable;

public class WASMSSAASTCompilerBackend implements CompileBackend<WASMCompileResult> {

    private static class CallSite {
        private final Program program;
        private final RegionNode bootstrapMethod;

        private CallSite(final Program aProgram, final RegionNode aBootstrapMethod) {
            this.program = aProgram;
            this.bootstrapMethod = aBootstrapMethod;
        }
    }

    private final ProgramGeneratorFactory programGeneratorFactory;

    public WASMSSAASTCompilerBackend(final ProgramGeneratorFactory aProgramGeneratorFactory) {
        this.programGeneratorFactory = aProgramGeneratorFactory;
    }

    @Override
    public WASMCompileResult generateCodeFor(
            final CompileOptions aOptions, final BytecodeLinkerContext aLinkerContext, final Class aEntryPointClass, final String aEntryPointMethodName, final BytecodeMethodSignature aEntryPointSignatue) {

        // Link required mamory management code
        final BytecodeLinkedClass theArrayClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Array.class));

        final BytecodeLinkedClass theMemoryManagerClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class));

        theMemoryManagerClass.resolveStaticMethod("freeMem", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.LONG, new BytecodeTypeRef[0]));
        theMemoryManagerClass.resolveStaticMethod("usedMem", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.LONG, new BytecodeTypeRef[0]));

        theMemoryManagerClass.resolveStaticMethod("free", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {BytecodeObjectTypeRef.fromRuntimeClass(
                Address.class)}));
        theMemoryManagerClass.resolveStaticMethod("malloc", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                Address.class), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT}));
        theMemoryManagerClass.resolveStaticMethod("newObject", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                Address.class), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
        theMemoryManagerClass.resolveStaticMethod("newArray", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                Address.class), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
        theMemoryManagerClass.resolveStaticMethod("newArray", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                Address.class), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

        final BytecodeMethodSignature pushExceptionSignature = new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {BytecodeObjectTypeRef.fromRuntimeClass(Throwable.class)});
        final BytecodeMethodSignature popExceptionSignature = new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Throwable.class), new BytecodeTypeRef[0]);

        if (aOptions.isEnableExceptions()) {
            final BytecodeLinkedClass theExceptionManager = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(
                    ExceptionManager.class));
            theExceptionManager.resolveStaticMethod("push", pushExceptionSignature);
            theExceptionManager.resolveStaticMethod("pop", popExceptionSignature);
            theExceptionManager.resolveStaticMethod("lastExceptionOrNull", popExceptionSignature);
        }

        final BytecodeLinkedClass theStringClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(String.class));
        if (!theStringClass.resolveConstructorInvocation(new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.BYTE, 1)}))) {
            throw new IllegalStateException("No matching constructor!");
        }

        final Module module = new Module("bytecoder");

        // Exception handling
        if (aOptions.isEnableExceptions()) {
            module.getExceptions().newException(WASMSSAASTWriter.EXCEPTION_NAME, Collections.singletonList(PrimitiveType.i32));
        }

        final Global stackTop = module.getGlobals().newMutableGlobal(WASMSSAASTWriter.STACKTOP, PrimitiveType.i32, i32.c(-1));

        // Instanceof check
        {
            final ExportableFunction instanceOfCheck = module.getFunctions().newFunction("INSTANCEOF_CHECK", Arrays.asList(param("thisRef", PrimitiveType.i32), param("type", PrimitiveType.i32)), PrimitiveType.i32);
            final Iff nullCheck = instanceOfCheck.flow.iff("nullcheck", i32.eq(getLocal(instanceOfCheck.localByLabel("thisRef")), i32.c(0)));
            nullCheck.flow.ret(i32.c(0));

            final WASMType instanceOfType = module.getTypes().typeFor(Arrays.asList(PrimitiveType.i32, PrimitiveType.i32), PrimitiveType.i32);
            final WASMType resolveType = module.getTypes().typeFor(Arrays.asList(PrimitiveType.i32, PrimitiveType.i32), PrimitiveType.i32);
            final WASMValue theIndex = call(resolveType, Arrays.asList(getLocal(instanceOfCheck.localByLabel("thisRef")),
                    i32.c(WASMSSAASTWriter.GENERATED_INSTANCEOF_METHOD_ID)), i32.load(4, getLocal(instanceOfCheck.localByLabel("thisRef"))));

            instanceOfCheck.flow.ret(call(instanceOfType, Arrays.asList(getLocal(instanceOfCheck.localByLabel("thisRef")), getLocal(instanceOfCheck.localByLabel("type"))), theIndex));
        }

        // We need a memory
        module.getMems().newMemory(512, 512).exportAs("memory");

        // Also import functions first
        aLinkerContext.linkedClasses().forEach(aEntry -> {

            if (aEntry.targetNode().getBytecodeClass().getAccessFlags().isInterface()) {
                return;
            }
            if (Objects.equals(aEntry.edgeType().objectTypeRef(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                return;
            }

            final BytecodeResolvedMethods theMethodMap = aEntry.targetNode().resolvedMethods();
            theMethodMap.stream().forEach(aMethodMapEntry -> {

                // Only add implementation methods
                if (!(aMethodMapEntry.getProvidingClass() == aEntry.targetNode())) {
                    return;
                }

                final BytecodeMethod t = aMethodMapEntry.getValue();
                final BytecodeMethodSignature theSignature = t.getSignature();

                if (t.getAccessFlags().isNative()) {
                    if (null != aMethodMapEntry.getProvidingClass().getBytecodeClass().getAttributes()
                            .getAnnotationByType(EmulatedByRuntime.class.getName())) {
                        return;
                    }

                    final BytecodeImportedLink theLink = aMethodMapEntry.getProvidingClass().linkfor(t);

                    final String methodName = WASMWriterUtils.toMethodName(aMethodMapEntry.getProvidingClass().getClassName(), t.getName(), theSignature);
                    final ImportReference importReference = new ImportReference(theLink.getModuleName(), theLink.getLinkName());

                    final List<Param> params = new ArrayList<>();
                    params.add(param("thisRef",WASMSSAASTWriter.toType(TypeRef.Native.REFERENCE)));
                    for (int i = 0; i < theSignature.getArguments().length; i++) {
                        final BytecodeTypeRef theParamType = theSignature.getArguments()[i];
                        params.add(param("p" + (i + 1), WASMSSAASTWriter.toType(TypeRef.toType(theParamType))));
                    }

                    final Function imported;
                    if (!theSignature.getReturnType().isVoid()) {
                        imported = module.getImports().importFunction(
                                importReference,
                                methodName,
                                params,
                                WASMSSAASTWriter.toType(TypeRef.toType(theSignature.getReturnType()))).toTable();
                    } else {
                        imported = module.getImports().importFunction(
                                importReference,
                                methodName,
                                params).toTable();
                    }
                }
            });
        });

        final ExportableFunction lambdaResolvevtableindex = module.getFunctions().newFunction("LAMBDA" + WASMSSAASTWriter.VTABLEFUNCTIONSUFFIX, Arrays.asList(param("thisRef", PrimitiveType.i32), param("methodId", PrimitiveType.i32)), PrimitiveType.i32).toTable();
        lambdaResolvevtableindex.flow.ret(i32.load(8, getLocal(lambdaResolvevtableindex.localByLabel("thisRef"))));

        final ExportableFunction classGetEnumConstants = module.getFunctions().newFunction("jlClass_A1jlObjectgetEnumConstants", Collections.singletonList(param("thisRef", PrimitiveType.i32)), PrimitiveType.i32).toTable();
        classGetEnumConstants.flow.ret(i32.load(0, i32.load(12, getLocal(classGetEnumConstants.localByLabel("thisRef")))));

        final ExportableFunction classDesiredAssertionStatus = module.getFunctions().newFunction("jlClass_BOOLEANdesiredAssertionStatus", Collections.singletonList(param("thisRef", PrimitiveType.i32)), PrimitiveType.i32).toTable();
        classDesiredAssertionStatus.flow.ret(i32.c(0));

        final ConstantPool theConstantPool = new ConstantPool();

        final Map<String, WASMSSAASTCompilerBackend.CallSite> theCallsites = new HashMap<>();
        final WASMSSAASTWriter.Resolver theResolver = new WASMSSAASTWriter.Resolver() {
            @Override
            public Global globalForStringFromPool(final StringValue aValue) {
                final int thePoolIndex = theConstantPool.register(aValue);
                final String theLabel = "stringPool" + thePoolIndex;
                try {
                    return module.getGlobals().globalsIndex().globalByLabel(theLabel);
                } catch (final IllegalArgumentException e) {
                    return module.getGlobals().newMutableGlobal(theLabel, PrimitiveType.i32, i32.c(-1));
                }
            }

            @Override
            public Function resolveCallsiteBootstrapFor(final BytecodeClass owningClass, final String callsiteId, final Program program,
                    final RegionNode bootstrapMethod) {
                final String theID = "callsite_" + callsiteId.replace("/","_");
                if (!theCallsites.containsKey(theID)) {
                    final CallSite theCallsite = new CallSite(program, bootstrapMethod);
                    theCallsites.put(theID, theCallsite);
                    return module.getFunctions().newFunction(theID, PrimitiveType.i32);
                }
                return module.functionIndex().firstByLabel(theID);
            }
        };

        final ExportableFunction runtimeResolvevtableindex = module.getFunctions().newFunction("RUNTIMECLASS" + WASMSSAASTWriter.VTABLEFUNCTIONSUFFIX, Arrays.asList(param("thisRef", PrimitiveType.i32), param("methodId", PrimitiveType.i32)), PrimitiveType.i32).toTable();
        {
            final BytecodeLinkedClass theClassLinkedCass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Class.class));
            final BytecodeResolvedMethods theRuntimeMethodMap = theClassLinkedCass.resolvedMethods();
            theRuntimeMethodMap.stream().forEach(aMethodMapEntry -> {
                final BytecodeMethod theMethod = aMethodMapEntry.getValue();

                if (!theMethod.getAccessFlags().isStatic()) {
                    final BytecodeVirtualMethodIdentifier theMethodIdentifier = aLinkerContext.getMethodCollection().identifierFor(theMethod);

                    final Block block = runtimeResolvevtableindex.flow.block("m" + theMethodIdentifier.getIdentifier());
                    block.flow.branchIff(block, i32.ne(getLocal(runtimeResolvevtableindex.localByLabel("methodId")), i32.c(theMethodIdentifier.getIdentifier())));
                    if (Objects.equals("getClass", theMethod.getName().stringValue())) {
                        block.flow.unreachable();
                    } else if (Objects.equals("toString", theMethod.getName().stringValue())) {
                        block.flow.unreachable();
                    } else if (Objects.equals("equals", theMethod.getName().stringValue())) {
                        block.flow.unreachable();
                    } else if (Objects.equals("hashCode", theMethod.getName().stringValue())) {
                        block.flow.unreachable();
                    } else if (Objects.equals("desiredAssertionStatus", theMethod.getName().stringValue())) {
                        block.flow.ret(i32.c(module.getTables().funcTable().indexOf(classDesiredAssertionStatus)));
                    } else if (Objects.equals("getEnumConstants", theMethod.getName().stringValue())) {
                        block.flow.ret(i32.c(module.getTables().funcTable().indexOf(classGetEnumConstants)));
                    } else {
                        block.flow.unreachable();
                    }
                }
            });
            runtimeResolvevtableindex.flow.unreachable();
        }

        final ExportableFunction newLambdaFunction = module.getFunctions().newFunction("newLambda", Arrays.asList(param("type", PrimitiveType.i32), param("implMethodNumber", PrimitiveType.i32)), PrimitiveType.i32);

        aLinkerContext.linkedClasses().forEach(aEntry -> {

            if (Objects.equals(aEntry.edgeType().objectTypeRef(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                return;
            }

            // Create a class init check function first
            final String theClassName = WASMWriterUtils.toClassName(aEntry.edgeType().objectTypeRef());
            module.getFunctions().newFunction(theClassName + "__classinitcheck", Collections.emptyList());

            // We also create a global for the runtime class
            final Global runtimeClass = module.getGlobals().newMutableGlobal(theClassName + WASMSSAASTWriter.RUNTIMECLASSSUFFIX, PrimitiveType.i32, i32.c(-1));

            final BytecodeResolvedMethods theMethodMap = aEntry.targetNode().resolvedMethods();
            theMethodMap.stream().forEach(aMapEntry -> {
                final BytecodeMethod t = aMapEntry.getValue();

                // If the method is provided by the runtime, we do not need to generate the implementation
                if (null != t.getAttributes().getAnnotationByType(EmulatedByRuntime.class.getName())) {
                    return;
                }
                // Do not generate code for abstract methods
                if (t.getAccessFlags().isAbstract()) {
                    return;
                }
                // Class initializer also
                if (t.isClassInitializer()) {
                    return;
                }
                // Only write real methods
                if (!(aMapEntry.getProvidingClass() == aEntry.targetNode())) {
                    return;
                }

                // Do not write imported functions here
                if (t.getAccessFlags().isNative()) {
                    return;
                }

                final BytecodeMethodSignature theSignature = t.getSignature();
                if (null != aEntry.targetNode().getBytecodeClass().getAttributes()
                        .getAnnotationByType(EmulatedByRuntime.class.getName())) {
                    return;
                }

                final String theMethodName = WASMWriterUtils.toMethodName(aEntry.edgeType().objectTypeRef(), t.getName(), theSignature);
                if (!module.functionIndex().hasFunction(theMethodName)) {
                    final List<Param> params = new ArrayList<>();
                    params.add(param("thisRef", WASMSSAASTWriter.toType(TypeRef.Native.REFERENCE)));
                    for (int i = 0; i < theSignature.getArguments().length; i++) {
                        final BytecodeTypeRef theParamType = theSignature.getArguments()[i];
                        params.add(param("p" + (i + 1), WASMSSAASTWriter.toType(TypeRef.toType(theParamType))));
                    }
                    final Function theFunction;
                    if (!theSignature.getReturnType().isVoid()) {
                        theFunction = module.getFunctions().newFunction(theMethodName, params, WASMSSAASTWriter.toType(TypeRef.toType(theSignature.getReturnType())));
                    } else {
                        theFunction = module.getFunctions().newFunction(theMethodName, params);
                    }

                    // Only real functions inclusive static ones need to be callable by function table
                    // Note: synthetic lambda functions are static AND called by method reference, and they are also private!!!!
                    // So they must also be part of the function reference table
                    if (!t.isConstructor()) {
                        theFunction.toTable();
                    }
                }
            });
        });

        // Initialize memory layout for classes and instances
        final WASMMemoryLayouter theMemoryLayout = new WASMMemoryLayouter(aLinkerContext);

        // Now everything else
        aLinkerContext.linkedClasses().forEach(aEntry -> {

            final BytecodeLinkedClass theLinkedClass = aEntry.targetNode();

            if (Objects.equals(aEntry.edgeType().objectTypeRef(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                return;
            }
            if (null != theLinkedClass.getBytecodeClass().getAttributes().getAnnotationByType(EmulatedByRuntime.class.getName())) {
                return;
            }

            final Set<BytecodeObjectTypeRef> theStaticReferences = new HashSet<>();
            final BytecodeResolvedMethods theMethodMap = theLinkedClass.resolvedMethods();
            final String theClassName = WASMWriterUtils.toClassName(aEntry.edgeType().objectTypeRef());

            if (!theLinkedClass.getBytecodeClass().getAccessFlags().isInterface()) {

                final ExportableFunction instanceOf = module.getFunctions()
                        .newFunction(theClassName + WASMSSAASTWriter.INSTANCEOFSUFFIX,
                                Arrays.asList(param("thisRef", PrimitiveType.i32), param("p1", PrimitiveType.i32)), PrimitiveType.i32).toTable();

                for (final BytecodeLinkedClass theType : theLinkedClass.getImplementingTypes()) {
                    final Iff b = instanceOf.flow.iff("b" + theType.getUniqueId(), i32.eq(getLocal(instanceOf.localByLabel("p1")), i32.c(theType.getUniqueId())));
                    b.flow.ret(i32.c(1));
                }
                instanceOf.flow.ret(i32.c(0));

                final ExportableFunction resolveTableIndex = module.getFunctions()
                        .newFunction(theClassName + WASMSSAASTWriter.VTABLEFUNCTIONSUFFIX,
                                Arrays.asList(param("thisRef", PrimitiveType.i32), param("p1", PrimitiveType.i32)), PrimitiveType.i32).toTable();

                final List<BytecodeResolvedMethods.MethodEntry> theEntries = theMethodMap.stream().collect(Collectors.toList());
                final Set<BytecodeVirtualMethodIdentifier> theVisitedMethods = new HashSet<>();
                for (int i = theEntries.size() - 1; 0 <= i; i--) {
                    final BytecodeResolvedMethods.MethodEntry aMethodMapEntry = theEntries.get(i);
                    final BytecodeMethod theMethod = aMethodMapEntry.getValue();

                    if (!theMethod.getAccessFlags().isStatic() &&
                            !theMethod.getAccessFlags().isPrivate() &&
                            !theMethod.isConstructor() &&
                            !theMethod.getAccessFlags().isAbstract() &&
                            (theMethod != BytecodeLinkedClass.GET_CLASS_PLACEHOLDER)) {

                        final BytecodeVirtualMethodIdentifier theMethodIdentifier = aLinkerContext.getMethodCollection()
                                .identifierFor(theMethod);

                        if (theVisitedMethods.add(theMethodIdentifier)) {

                            final Iff iff = resolveTableIndex.flow.iff("b" + theMethodIdentifier.getIdentifier(), i32.eq(getLocal(resolveTableIndex.localByLabel("p1")), i32.c(theMethodIdentifier.getIdentifier())));

                            final String theFullMethodName = WASMWriterUtils
                                    .toMethodName(aMethodMapEntry.getProvidingClass().getClassName(),
                                            theMethod.getName(),
                                            theMethod.getSignature());

                            iff.flow.ret(weakFunctionTableReference(theFullMethodName));
                        }
                    }
                }

                final Iff block = resolveTableIndex.flow.iff("b", i32.eq(getLocal(resolveTableIndex.localByLabel("p1")), i32.c(WASMSSAASTWriter.GENERATED_INSTANCEOF_METHOD_ID)));
                final int theIndex = module.getTables().funcTable().indexOf(instanceOf);
                block.flow.ret(i32.c(theIndex));

                resolveTableIndex.flow.unreachable();
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

                            final List<Param> params = new ArrayList<>();

                            params.add(param("UNUSED", WASMSSAASTWriter.toType(TypeRef.Native.REFERENCE)));

                            for (int i = 0; i < theSignature.getArguments().length; i++) {
                                params.add(param("p" + i, WASMSSAASTWriter.toType(TypeRef.toType(theSignature.getArguments()[i]))));
                            }

                            if (!theSignature.getReturnType().isVoid()) {
                                final PrimitiveType returnType = WASMSSAASTWriter.toType(TypeRef.toType(theSignature.getReturnType()));

                                final ExportableFunction function = module.getFunctions().newFunction(
                                        WASMWriterUtils
                                                .toMethodName(theLinkedClass.getClassName(), theMethod.getName(), theSignature),
                                        params,
                                        returnType
                                );

                                final Callable theImplementation = weakFunctionReference(WASMWriterUtils
                                        .toMethodName(aMethodMapEntry.getProvidingClass().getClassName(), theMethod.getName(),
                                                theSignature));

                                final List<WASMValue> callValues = new ArrayList<>();
                                for (final Param p : params) {
                                    callValues.add(getLocal(p));
                                }
                                function.flow.ret(call(theImplementation, callValues));
                            } else {

                                final ExportableFunction function = module.getFunctions().newFunction(
                                        WASMWriterUtils
                                                .toMethodName(theLinkedClass.getClassName(), theMethod.getName(), theSignature),
                                        params
                                );

                                final Callable theImplementation = weakFunctionReference(WASMWriterUtils
                                        .toMethodName(aMethodMapEntry.getProvidingClass().getClassName(), theMethod.getName(),
                                                theSignature));

                                final List<WASMValue> callValues = new ArrayList<>();
                                for (final Param p : params) {
                                    callValues.add(getLocal(p));
                                }
                                function.flow.voidCall(theImplementation, callValues);
                            }
                        }
                    }
                    return;
                }

                final ProgramGenerator theGenerator = programGeneratorFactory.createFor(aLinkerContext);
                final Program theSSAProgram = theGenerator.generateFrom(aMethodMapEntry.getProvidingClass().getBytecodeClass(), theMethod);

                //Run optimizer
                aOptions.getOptimizer().optimize(theSSAProgram.getControlFlowGraph(), aLinkerContext);

                final List<Param> params = new ArrayList<>();
                if (theMethod.getAccessFlags().isStatic()) {
                    params.add(param("UNUSED", WASMSSAASTWriter.toType(TypeRef.Native.REFERENCE)));
                }
                for (final Program.Argument theArgument : theSSAProgram.getArguments()) {
                    final Variable theVariable = theArgument.getVariable();
                    params.add(param(theVariable.getName(), WASMSSAASTWriter.toType(theVariable.resolveType())));
                }
                final String theFunctionLabel = WASMWriterUtils.toMethodName(theLinkedClass.getClassName(), theMethod.getName(), theSignature);
                final ExportableFunction instanceFunction;
                if (!module.functionIndex().hasFunction(theFunctionLabel)) {
                    if (theSignature.getReturnType().isVoid()) {
                        instanceFunction = module.getFunctions().newFunction(
                                WASMWriterUtils.toMethodName(theLinkedClass.getClassName(), theMethod.getName(), theSignature),
                                params);
                    } else {
                        final PrimitiveType returnType = WASMSSAASTWriter.toType(TypeRef.toType(theSignature.getReturnType()));
                        instanceFunction = module.getFunctions().newFunction(
                                WASMWriterUtils.toMethodName(theLinkedClass.getClassName(), theMethod.getName(), theSignature),
                                params,
                                returnType);
                    }
                } else {
                    instanceFunction = module.functionIndex().firstByLabel(theFunctionLabel);
                }

                // Check if there is an export defined
                final BytecodeAnnotation theExport = theMethod.getAttributes().getAnnotationByType(Export.class.getName());
                if (null != theExport) {
                    instanceFunction.exportAs(theExport.getElementValueByName("value").stringValue());
                }

                theStaticReferences.addAll(theSSAProgram.getStaticReferences());

                // Try to reloop it!
                try {
                    final Relooper theRelooper = new Relooper();
                    final Relooper.Block theReloopedBlock = theRelooper.reloop(theSSAProgram.getControlFlowGraph());

                    final WASMSSAASTWriter writer = new WASMSSAASTWriter(theResolver, aLinkerContext, module, aOptions, theSSAProgram, theMemoryLayout, instanceFunction);

                    for (final Variable theVariable : theSSAProgram.getVariables()) {
                        if (!(theVariable.isSynthetic())) {
                            instanceFunction.newLocal(theVariable.getName(), WASMSSAASTWriter.toType(theVariable.resolveType()));
                        }
                    }

                    // Now we know the exact fucntion argument names, so we rename them
                    int paramIndex = -1;
                    if (theMethod.getAccessFlags().isStatic()) {
                        final Param theParam = instanceFunction.getParams().get(++paramIndex);
                        theParam.renameTo("UNUSED");
                    }

                    for (final Program.Argument theArgument : theSSAProgram.getArguments()) {
                        final Param theParam = instanceFunction.getParams().get(++paramIndex);
                        theParam.renameTo(theArgument.getVariable().getName());
                    }

                    writer.writeRelooped(theReloopedBlock);
                } catch (final Exception e) {
                    throw new IllegalStateException("Error relooping cfg", e);
                }
            });

            final ExportableFunction initFunction = module.functionIndex().firstByLabel(theClassName + "__classinitcheck");
            final Global initGlobal = module.globalsIndex().globalByLabel(theClassName + WASMSSAASTWriter.RUNTIMECLASSSUFFIX);
            final Iff check = initFunction.flow.iff("check", i32.ne(i32.load(8, getGlobal(initGlobal)), i32.c(1)));
            check.flow.i32.store(8, getGlobal(initGlobal), i32.c(1));

            for (final BytecodeObjectTypeRef theRef : theStaticReferences) {
                if (!Objects.equals(theRef, aEntry.edgeType().objectTypeRef())) {
                    final Function theOtherInit = module.functionIndex().firstByLabel(WASMWriterUtils.toClassName(theRef) + "__classinitcheck");
                    check.flow.voidCall(theOtherInit, Collections.emptyList());
                }
            }

            if (theLinkedClass.hasClassInitializer()) {
                check.flow.voidCall(module.functionIndex().firstByLabel(theClassName + "_VOIDclinit"), Collections.singletonList(i32.c(-1)));
            }
        });

        // Render callsites
        for (final Map.Entry<String, CallSite> theEntry : theCallsites.entrySet()) {

            final ExportableFunction theFunction = module.functionIndex().firstByLabel(theEntry.getKey());
            final Program theSSAProgram = theEntry.getValue().program;

            final WASMSSAASTWriter writer = new WASMSSAASTWriter(theResolver, aLinkerContext, module, aOptions, theSSAProgram, theMemoryLayout, theFunction);
            for (final Variable theVariable : theSSAProgram.getVariables()) {

                if (!(theVariable.isSynthetic())) {
                    theFunction.newLocal(theVariable.getName(), WASMSSAASTWriter.toType(theVariable.resolveType()));
                }
            }

            writer.stackEnter();
            writer.writeExpressionList(theEntry.getValue().bootstrapMethod.getExpressions());
        }

        final ExportableFunction newRuntimeClassFunction;
        {
            final String mallocFunctionName = WASMWriterUtils.toClassName(theMemoryManagerClass.getClassName()) + "_dmbcAddressnewObjectINTINTINT";
            newRuntimeClassFunction = module.getFunctions().newFunction("newRuntimeClass", Arrays.asList(param("type", PrimitiveType.i32),param("staticSize", PrimitiveType.i32),param("enumValuesOffset", PrimitiveType.i32)), PrimitiveType.i32);
            final Local newRef = newRuntimeClassFunction.newLocal("newRef", PrimitiveType.i32);
            newRuntimeClassFunction.flow.setLocal(newRef,
                    call(module.functionIndex().firstByLabel(mallocFunctionName),
                            Arrays.asList(i32.c(0), getLocal(newRuntimeClassFunction.localByLabel("staticSize")),
                                    i32.c(-1), i32.c(module.getTables().funcTable().indexOf(runtimeResolvevtableindex)))));
            newRuntimeClassFunction.flow.i32.store(12, getLocal(newRef),
                    i32.add(getLocal(newRef), getLocal(newRuntimeClassFunction.localByLabel("enumValuesOffset"))));
            newRuntimeClassFunction.flow.ret(getLocal(newRef));
        }


        {
            final ExportableFunction bootstrap = module.getFunctions().newFunction("bootstrap", Collections.emptyList());
            bootstrap.flow.setGlobal(stackTop, i32.sub(i32.mul(currentMemory(), i32.c(65536)), i32.c(1)));

            // Globals for static class data
            aLinkerContext.linkedClasses().forEach(aEntry -> {

                final BytecodeLinkedClass theLinkedClass = aEntry.targetNode();

                if (Objects.equals(aEntry.edgeType().objectTypeRef(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                    return;
                }
                if (null != theLinkedClass.getBytecodeClass().getAttributes().getAnnotationByType(EmulatedByRuntime.class.getName())) {
                    return;
                }

                final Global runtimeClassGlobal = module.globalsIndex().globalByLabel(WASMWriterUtils.toClassName(aEntry.edgeType().objectTypeRef()) + WASMSSAASTWriter.RUNTIMECLASSSUFFIX);

                final List<WASMValue> initArguments = new ArrayList<>();
                initArguments.add(i32.c(theLinkedClass.getUniqueId()));

                final WASMMemoryLayouter.MemoryLayout theLayout = theMemoryLayout.layoutFor(aEntry.edgeType().objectTypeRef());

                initArguments.add(i32.c(theLayout.classSize()));

                final BytecodeResolvedFields theStaticFields = theLinkedClass.resolvedFields();
                if (null != theStaticFields.fieldByName("$VALUES")) {
                    initArguments.add(i32.c(theLayout.offsetForClassMember("$VALUES")));
                } else {
                    initArguments.add(i32.c(-1));
                }

                bootstrap.flow.setGlobal(runtimeClassGlobal,
                        call(newRuntimeClassFunction, initArguments));

            });

            final WASMMemoryLayouter.MemoryLayout theStringMemoryLayout = theMemoryLayout.layoutFor(theStringClass.getClassName());
            final List<StringValue> thePoolValues = theConstantPool.stringValues();
            for (int i=0;i<thePoolValues.size();i++) {

                final StringValue theConstantInPool = thePoolValues.get(i);
                final String theData = theConstantInPool.getStringValue();
                final byte[] theDataBytes = theData.getBytes();

                final Global theStringPool = module.getGlobals().globalsIndex().globalByLabel("stringPool" + i);
                final Global theStringPoolData = module.getGlobals().newMutableGlobal("stringPool" + i + "__array", PrimitiveType.i32, i32.c(-1));

                final String theMethodName = WASMWriterUtils.toMethodName(
                        BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class),
                        "newArray",
                        new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                                Address.class), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
                final List<WASMValue> theMallocArguments = new ArrayList<>();
                theMallocArguments.add(i32.c(0));
                theMallocArguments.add(i32.c(theDataBytes.length));
                theMallocArguments.add(getGlobal(module.globalsIndex().globalByLabel(WASMWriterUtils.toClassName(theArrayClass.getClassName())+ WASMSSAASTWriter.RUNTIMECLASSSUFFIX)));
                final Function theVtableFunction = module.functionIndex().firstByLabel(WASMWriterUtils.toClassName(theArrayClass.getClassName())+ WASMSSAASTWriter.VTABLEFUNCTIONSUFFIX);
                theMallocArguments.add(i32.c(module.getTables().funcTable().indexOf(theVtableFunction)));
                bootstrap.flow.setGlobal(theStringPoolData, call(module.functionIndex().firstByLabel(theMethodName), theMallocArguments));
                // Set array value
                for (int j=0; j< theDataBytes.length;j++) {
                    //
                    final int offset = 20 + j * 4;
                    bootstrap.flow.i32.store(offset, getGlobal(theStringPoolData), i32.c(theDataBytes[j]));
                }

                final Function theMallocFunction = module.functionIndex().firstByLabel(WASMWriterUtils.toClassName(theMemoryManagerClass.getClassName()) + "_dmbcAddressnewObjectINTINTINT");
                final Function theStringVTable = module.functionIndex().firstByLabel(WASMWriterUtils.toClassName(theStringClass.getClassName())+ WASMSSAASTWriter.VTABLEFUNCTIONSUFFIX);

                bootstrap.flow.setGlobal(theStringPool,
                        call(theMallocFunction, Arrays.asList(i32.c(0), i32.c(theStringMemoryLayout.instanceSize()),
                                i32.c(theStringClass.getUniqueId()),
                                i32.c(module.getTables().funcTable().indexOf(theStringVTable)))));

                final Function theStringConstructor = module.functionIndex().firstByLabel(WASMWriterUtils.toClassName(theStringClass.getClassName())+ "_VOIDinitA1BYTE");
                bootstrap.flow.voidCall(theStringConstructor, Arrays.asList(getGlobal(theStringPool), getGlobal(theStringPoolData)));
            }

            aLinkerContext.linkedClasses().forEach(aEntry -> {

                if (null != aEntry.targetNode().getBytecodeClass().getAttributes()
                        .getAnnotationByType(EmulatedByRuntime.class.getName())) {
                    return;
                }

                if (!Objects.equals(aEntry.edgeType().objectTypeRef(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                    final Function classInitCheckFunction = module.functionIndex().firstByLabel(WASMWriterUtils.toClassName(aEntry.edgeType().objectTypeRef()) + "__classinitcheck");
                    bootstrap.flow.voidCall(classInitCheckFunction, Collections.emptyList());
                }
            });

            // After the Bootstrap, we need to all the static stuff on the stack, so it is not garbage collected
            final GlobalsIndex globalIndex = module.globalsIndex();
            bootstrap.flow.setGlobal(stackTop, i32.sub(getGlobal(stackTop), i32.c(globalIndex.size() * 4)));
            for (int i=0;i<globalIndex.size();i++) {
                final Global global = globalIndex.get(i);
                bootstrap.flow.i32.store(i * 4, getGlobal(stackTop), getGlobal(global));
            }

            bootstrap.exportAs("bootstrap");
        }

        {
            final String mallocFunctionName = WASMWriterUtils.toClassName(theMemoryManagerClass.getClassName()) + "_dmbcAddressnewObjectINTINTINT";
            final Local newRef = newLambdaFunction.newLocal("newRef", PrimitiveType.i32);
            newLambdaFunction.flow.setLocal(newRef,
                    call(module.functionIndex().firstByLabel(mallocFunctionName),
                            Arrays.asList(i32.c(0), i32.c(12),
                                    getLocal(newLambdaFunction.localByLabel("type")), i32.c(module.getTables().funcTable().indexOf(lambdaResolvevtableindex)))));
            newLambdaFunction.flow.i32.store(8, getLocal(newRef), getLocal(newLambdaFunction.localByLabel("implMethodNumber")));
            newLambdaFunction.flow.ret(getLocal(newRef));
        }

        // Main function must be exported
        {
            final ExportableFunction mainFunction = module.functionIndex().firstByLabel(WASMWriterUtils
                    .toMethodName(BytecodeObjectTypeRef.fromRuntimeClass(aEntryPointClass), aEntryPointMethodName,
                            aEntryPointSignatue));
            mainFunction.exportAs("main");
        }

        final StringWriter theStringWriter = new StringWriter();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            final PrintWriter theWriter = new PrintWriter(theStringWriter);
            final Exporter exporter = new Exporter(aOptions.isDebugOutput());
            exporter.export(module, theWriter);
            exporter.export(module, bos);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        return new WASMCompileResult(
                new WASMCompileResult.WASMTextualCompileResult(theMemoryLayout, aLinkerContext, new ArrayList<>(), theStringWriter.toString(), aOptions.getFilenamePrefix()),
                new WASMCompileResult.WASMBinaryCompileResult(theMemoryLayout, aLinkerContext, new ArrayList<>(), bos.toByteArray(), aOptions.getFilenamePrefix()));
    }
}