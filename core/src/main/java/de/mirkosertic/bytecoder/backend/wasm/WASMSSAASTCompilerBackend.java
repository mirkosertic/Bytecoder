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
import java.lang.invoke.LambdaMetafactory;
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

import de.mirkosertic.bytecoder.api.Callback;
import de.mirkosertic.bytecoder.api.EmulatedByRuntime;
import de.mirkosertic.bytecoder.api.Export;
import de.mirkosertic.bytecoder.api.OpaqueMethod;
import de.mirkosertic.bytecoder.api.OpaqueProperty;
import de.mirkosertic.bytecoder.api.OpaqueReferenceType;
import de.mirkosertic.bytecoder.backend.CompileBackend;
import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.ConstantPool;
import de.mirkosertic.bytecoder.backend.wasm.ast.Block;
import de.mirkosertic.bytecoder.backend.wasm.ast.Callable;
import de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions;
import de.mirkosertic.bytecoder.backend.wasm.ast.ExportableFunction;
import de.mirkosertic.bytecoder.backend.wasm.ast.Exporter;
import de.mirkosertic.bytecoder.backend.wasm.ast.Function;
import de.mirkosertic.bytecoder.backend.wasm.ast.Global;
import de.mirkosertic.bytecoder.backend.wasm.ast.GlobalsIndex;
import de.mirkosertic.bytecoder.backend.wasm.ast.Iff;
import de.mirkosertic.bytecoder.backend.wasm.ast.ImportReference;
import de.mirkosertic.bytecoder.backend.wasm.ast.Local;
import de.mirkosertic.bytecoder.backend.wasm.ast.Module;
import de.mirkosertic.bytecoder.backend.wasm.ast.Param;
import de.mirkosertic.bytecoder.backend.wasm.ast.PrimitiveType;
import de.mirkosertic.bytecoder.backend.wasm.ast.WASMType;
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
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.InvokeStaticMethodExpression;
import de.mirkosertic.bytecoder.ssa.MethodRefExpression;
import de.mirkosertic.bytecoder.ssa.MethodTypeExpression;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.ssa.ProgramGeneratorFactory;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.StringValue;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Value;
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

        theMemoryManagerClass.resolveStaticMethod("newString", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                String.class), new BytecodeTypeRef[] {new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.BYTE, 1)}));
        theMemoryManagerClass.resolveStaticMethod("newByteArray", new BytecodeMethodSignature(new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.BYTE, 1), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT}));
        theMemoryManagerClass.resolveStaticMethod("setByteArrayEntry", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.BYTE, 1), BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.BYTE}));
        theMemoryManagerClass.resolveStaticMethod("summonCallback",
                new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {BytecodeObjectTypeRef.fromRuntimeClass(
                        Callback.class), BytecodeObjectTypeRef.fromRuntimeClass(
                        OpaqueReferenceType.class)}));

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

        // Out list of opaque reference methods, which are implemented
        // by JS wrapper functions
        final List<OpaqueReferenceMethod> opaqueReferenceMethods = new ArrayList<>();

        // Also import functions first
        aLinkerContext.linkedClasses().forEach(aEntry -> {

            if (aEntry.targetNode().getBytecodeClass().getAccessFlags().isInterface() && !aEntry.targetNode().isOpaqueType()) {
                return;
            }
            if (Objects.equals(aEntry.edgeType().objectTypeRef(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
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
                    if (null != theProvidingClass.getBytecodeClass().getAttributes()
                            .getAnnotationByType(EmulatedByRuntime.class.getName())) {
                        return;
                    }

                    // Native methods are imported via annotation
                    if (!t.getAccessFlags().isNative() && theProvidingClass.isOpaqueType()) {
                        // For all the other methods we programatically generate
                        // the JS wrapper implementation later by this compiler
                        opaqueReferenceMethods.add(new OpaqueReferenceMethod(theProvidingClass, t));
                    }

                    final BytecodeImportedLink theLink = theProvidingClass.linkfor(t);

                    final String methodName = WASMWriterUtils.toMethodName(theProvidingClass.getClassName(), t.getName(), theSignature);
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

        final ExportableFunction newLambdaFunction = module.getFunctions().newFunction("newLambda", Arrays.asList(param("type", PrimitiveType.i32), param("implMethodNumber", PrimitiveType.i32), param("staticArguments", PrimitiveType.i32)), PrimitiveType.i32);

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
                                final List<Param> theParams = new ArrayList<>();
                                theParams.add(param("selfRef", PrimitiveType.i32));
                                for (int i=0;i<theDynamicInvocationType.getSignature().getArguments().length;i++) {
                                    theParams.add(param("arg" + i, WASMSSAASTWriter.toType(TypeRef.toType(theDynamicInvocationType.getSignature().getArguments()[i]))));
                                }
                                final BytecodeMethodSignature theImplementationSignature = theImplementationMethod.getSignature();

                                final Function theImplementationFunction = module.functionIndex().firstByLabel(WASMWriterUtils.toMethodName(
                                        theImplementationMethod.getClassName(),
                                        theImplementationMethod.getMethodName(),
                                        theImplementationSignature
                                ));

                                final String theFunctionName = WASMWriterUtils.toMethodName(theImplementationMethod.getClassName(),
                                        theImplementationMethod.getMethodName() + theEntry.getKey(), theImplementationMethod.getSignature());

                                // This is our new implementation
                                theImplementationMethod.retargetToMethodName(theImplementationMethod.getMethodName() + theEntry.getKey());

                                if (theImplementationSignature.getReturnType().isVoid()) {

                                    ExportableFunction theAdapterFunction= theAdapterFunction = module.getFunctions().newFunction(theFunctionName,
                                            theParams).toTable();

                                    final List<WASMValue> theDispatchArguments = new ArrayList<>();
                                    theDispatchArguments.add(getLocal(theAdapterFunction.localByLabel("selfRef")));

                                    // Add static arguments
                                    for (int i=0;i<theStaticInvocationType.getSignature().getArguments().length;i++) {

                                        final WASMValue theBasePtr = i32.load(12, getLocal(theAdapterFunction.localByLabel("selfRef")));
                                        final WASMValue thePtr = i32.add(theBasePtr, i32.c(i * 4));
                                        switch (TypeRef.toType(theStaticInvocationType.getSignature().getArguments()[i]).resolve()) {
                                            case DOUBLE:
                                            case FLOAT: {
                                                theDispatchArguments.add(ConstExpressions.f32.load(20, thePtr));
                                            }
                                            default: {
                                                theDispatchArguments.add(ConstExpressions.i32.load(20, thePtr));
                                            }
                                        }
                                    }

                                    // Add dynamic arguments
                                    for (int i=0;i<theDynamicInvocationType.getSignature().getArguments().length;i++) {
                                        theDispatchArguments.add(getLocal(theAdapterFunction.localByLabel("arg" + i)));
                                    }

                                    theAdapterFunction.flow.voidCall(theImplementationFunction, theDispatchArguments);
                                } else {

                                    final ExportableFunction theAdapterFunction = module.getFunctions().newFunction(theFunctionName,
                                            theParams, WASMSSAASTWriter.toType(TypeRef.toType(theDynamicInvocationType.getSignature().getReturnType()))).toTable();

                                    final List<WASMValue> theDispatchArguments = new ArrayList<>();
                                    theDispatchArguments.add(getLocal(theAdapterFunction.localByLabel("selfRef")));

                                    // Add static arguments
                                    for (int i=0;i<theStaticInvocationType.getSignature().getArguments().length;i++) {

                                        final WASMValue theBasePtr = i32.load(12, getLocal(theAdapterFunction.localByLabel("selfRef")));
                                        final WASMValue thePtr = i32.add(theBasePtr, i32.c(i * 4));
                                        switch (TypeRef.toType(theStaticInvocationType.getSignature().getArguments()[i]).resolve()) {
                                        case DOUBLE:
                                        case FLOAT: {
                                            theDispatchArguments.add(ConstExpressions.f32.load(20, thePtr));
                                        }
                                        default: {
                                            theDispatchArguments.add(ConstExpressions.i32.load(20, thePtr));
                                        }
                                        }
                                    }

                                    // Add dynamic arguments
                                    for (int i=0;i<theDynamicInvocationType.getSignature().getArguments().length;i++) {
                                        theDispatchArguments.add(getLocal(theAdapterFunction.localByLabel("arg" + i)));
                                    }

                                    theAdapterFunction.flow.ret(call(theImplementationFunction, theDispatchArguments));
                                }
                            }
                        }
                    }
                }
            }

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
                            Arrays.asList(i32.c(0), i32.c(16),
                                    getLocal(newLambdaFunction.localByLabel("type")), i32.c(module.getTables().funcTable().indexOf(lambdaResolvevtableindex)))));
            newLambdaFunction.flow.i32.store(8, getLocal(newRef), getLocal(newLambdaFunction.localByLabel("implMethodNumber")));
            newLambdaFunction.flow.i32.store(12, getLocal(newRef), getLocal(newLambdaFunction.localByLabel("staticArguments")));
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

        // Finally, we need to generate the opaque referene type JS adapter code
        final StringWriter theJSCode = new StringWriter();
        try (final PrintWriter theWriter = new PrintWriter(theJSCode)) {

            theWriter.println("var bytecoder = {");
            theWriter.println();
            theWriter.println("     runningInstance: undefined,");
            theWriter.println("     runningInstanceMemory: undefined,");
            theWriter.println("     exports: undefined,");
            theWriter.println("     referenceTable: ['EMPTY'],");
            theWriter.println();

            theWriter.println("     init: function(instance) {");
            theWriter.println("         bytecoder.runningInstance = instance;");
            theWriter.println("         bytecoder.runningInstanceMemory = new Uint8Array(instance.exports.memory.buffer);");
            theWriter.println("         bytecoder.exports = instance.exports;");
            theWriter.println("     },");
            theWriter.println();

            theWriter.println("     intInMemory: function(value) {");
            theWriter.println("         return bytecoder.runningInstanceMemory[value]");
            theWriter.println("                + (bytecoder.runningInstanceMemory[value + 1] * 256)");
            theWriter.println("                + (bytecoder.runningInstanceMemory[value + 2] * 256 * 256)");
            theWriter.println("                + (bytecoder.runningInstanceMemory[value + 3] * 256 * 256 * 256);");
            theWriter.println("     },");

            theWriter.println();
            theWriter.println("     logByteArrayAsString: function(acaller, value) {");
            theWriter.println("         console.log(bytecoder.toJSString(value));");
            theWriter.println("     },");
            theWriter.println();

            theWriter.println();
            theWriter.println("     toJSString: function(value) {");
            theWriter.println("         var theByteArray = bytecoder.intInMemory(value + 8);");
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


            theWriter.println();
            theWriter.println("     toJSEventListener: function(value) {");
            theWriter.println("         return function(event) {");
            theWriter.println("             try {");
            theWriter.println("                 var eventIndex = bytecoder.toBytecoderReference(event);");
            theWriter.println("                 bytecoder.exports.summonCallback(0,value,eventIndex);");
            theWriter.println("                 delete bytecoder.referenceTable[eventIndex];");
            theWriter.println("             } catch (e) {");
            theWriter.println("                 console.log(e);");
            theWriter.println("             }");
            theWriter.println("         };");
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

            theWriter.println("     toBytecoderString: function(value) {");
            theWriter.println("         var newArray = bytecoder.exports.newByteArray(0, value.length);");
            theWriter.println("         for (var i=0;i<value.length;i++) {");
            theWriter.println("             bytecoder.exports.setByteArrayEntry(0,newArray,i,value.charCodeAt(i));");
            theWriter.println("         }");
            theWriter.println("         return bytecoder.exports.newString(0, newArray);");
            theWriter.println("     },");
            theWriter.println();

            theWriter.println("     logDebug: function(caller,value) {");
            theWriter.println("         console.log(value);");
            theWriter.println("     },");
            theWriter.println();

            theWriter.println("     imports: {");
            theWriter.println("         system: {");
            theWriter.println("             currentTimeMillis: function() {return Date.now();},");
            theWriter.println("             nanoTime: function() {return Date.now() * 1000000;},");
            theWriter.println("             logDebugObject: function(caller, value) {bytecoder.logDebug(caller, value);},");
            theWriter.println("             writeByteArrayToConsole: function(caller, value) {bytecoder.byteArraytoJSString(caller, value);},");
            theWriter.println("         },");
            theWriter.println("         vm: {");
            theWriter.println("             newRuntimeGeneratedTypeMethodTypeMethodHandleObject: function() {},");
            theWriter.println("         },");
            theWriter.println("         tsystem: {");
            theWriter.println("             logDebugObject: function(caller, value) {bytecoder.logDebug(caller, value);},");
            theWriter.println("         },");
            theWriter.println("         printstream: {");
            theWriter.println("             logDebug: function(caller, value) {bytecoder.logDebug(caller,value);},");
            theWriter.println("         },");
            theWriter.println("         math: {");
            theWriter.println("             floorDOUBLE: function (thisref, p1) {return Math.floor(p1);},");
            theWriter.println("             ceilDOUBLE: function (thisref, p1) {return Math.ceil(p1);},");
            theWriter.println("             sinDOUBLE: function (thisref, p1) {return Math.sin(p1);},");
            theWriter.println("             cosDOUBLE: function  (thisref, p1) {return Math.cos(p1);},");
            theWriter.println("             roundDOUBLE: function  (thisref, p1) {return Math.round(p1);},");
            theWriter.println("             sqrtDOUBLE: function(thisref, p1) {return Math.sqrt(p1);},");
            theWriter.println("             add: function(thisref, p1, p2) {return p1 + p2;},");
            theWriter.println("             maxLONGLONG: function(thisref, p1, p2) { return Math.max(p1, p2);},");
            theWriter.println("             maxINTINT: function(thisref, p1, p2) { return Math.max(p1, p2);},");
            theWriter.println("             minINTINT: function(thisref, p1, p2) { return Math.min(p1, p2);},");
            theWriter.println("             toRadiansDOUBLE: function(thisref, p1) {");
            theWriter.println("                 return Math.toRadians(p1);");
            theWriter.println("             },");
            theWriter.println("             toDegreesDOUBLE: function(thisref, p1) {");
            theWriter.println("                 return Math.toDegrees(p1);");
            theWriter.println("             },");
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
            theWriter.println("         profiler: {");
            theWriter.println("             logMemoryLayoutBlock: function(aCaller, aStart, aUsed, aNext) {");
            theWriter.println("                 if (aUsed == 1) return;");
            theWriter.println("                 console.log('   Block at ' + aStart + ' status is ' + aUsed + ' points to ' + aNext);");
            theWriter.println("                 console.log('      Block size is ' + bytecoder.intInMemory(aStart));");
            theWriter.println("                 console.log('      Object type ' + bytecoder.intInMemory(aStart + 12));");
            theWriter.println("             },");
            theWriter.println("         },");
            theWriter.println("         runtime: {");
            theWriter.println("             nativewindow: function(caller) {return bytecoder.toBytecoderReference(window);},");
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
                    for (int i=0;i<theSignature.getArguments().length;i++) {
                        theWriter.print(",arg");
                        theWriter.print(i);
                    }
                    theWriter.println(") {");

                    String theMethodName = theBytecdeMethod.getName().stringValue();
                    final BytecodeAnnotation theOpaqueProperty = theBytecdeMethod.getAttributes().getAnnotationByType(OpaqueProperty.class.getName());
                    if (theOpaqueProperty != null) {
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
                            theOpaquePropertyName = theMethodName;
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

                            final String theConversionFunction = conversionFunctionToWASNForOpaqueType(aLinkerContext, theSignature.getReturnType());
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

                            final String theConversionFunction = conversionFunctionToWASNForOpaqueType(aLinkerContext, theSignature.getReturnType());
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

                        for (int i=0;i<theSignature.getArguments().length;i++) {
                            if (i > 0) {
                                theWriter.print(",");
                            }

                            final String theConversionFunction = conversionFunctionToJSForOpaqueType(aLinkerContext, theSignature.getArguments()[i]);
                            if (theConversionFunction != null) {
                                theWriter.print(theConversionFunction);
                                theWriter.print("(arg");
                                theWriter.print(i);
                                theWriter.print(")");
                            } else {
                                theWriter.print("arg");
                                theWriter.print(i);
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
        return new WASMCompileResult(
                new WASMCompileResult.WASMTextualCompileResult(theMemoryLayout, aLinkerContext, new ArrayList<>(), theStringWriter.toString(), aOptions.getFilenamePrefix()),
                new WASMCompileResult.WASMBinaryCompileResult(theMemoryLayout, aLinkerContext, new ArrayList<>(), bos.toByteArray(), aOptions.getFilenamePrefix()),
                new WASMCompileResult.WASMTextualJSCompileResult(theMemoryLayout, aLinkerContext, new ArrayList<>(), theJSCode.toString(), aOptions.getFilenamePrefix()));
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
            } else if (theLinkedClass.isCallback()) {
                return "bytecoder.toJSEventListener";
            } else {
                throw new IllegalStateException("Type conversion from " + aTypeRef.name() + " is not supported!");
            }
        }
    }

    private String conversionFunctionToWASNForOpaqueType(final BytecodeLinkerContext alinkerContext, final BytecodeTypeRef aTypeRef) {
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