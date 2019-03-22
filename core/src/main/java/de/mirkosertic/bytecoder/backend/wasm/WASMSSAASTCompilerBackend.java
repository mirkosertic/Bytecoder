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

import de.mirkosertic.bytecoder.api.EmulatedByRuntime;
import de.mirkosertic.bytecoder.api.Export;
import de.mirkosertic.bytecoder.api.OpaqueIndexed;
import de.mirkosertic.bytecoder.api.OpaqueMethod;
import de.mirkosertic.bytecoder.api.OpaqueProperty;
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

        final WASMMinifier theMinifier = new WASMMinifier();

        // Link required mamory management code
        final BytecodeLinkedClass theArrayClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Array.class));

        final BytecodeLinkedClass theMemoryManagerClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class));

        theMemoryManagerClass.resolveStaticMethod("logException", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {BytecodeObjectTypeRef.fromRuntimeClass(Exception.class)}));

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

        final Module module = new Module("bytecoder", aOptions.getFilenamePrefix() + ".wasm.map");

        // Exception handling
        if (aOptions.isEnableExceptions()) {
            module.getExceptions().newException(WASMSSAASTWriter.EXCEPTION_NAME, Collections.singletonList(PrimitiveType.i32));
        }

        final Global stackTop = module.getGlobals().newMutableGlobal(WASMSSAASTWriter.STACKTOP, PrimitiveType.i32, i32.c(-1, null));

        // Instanceof check
        {
            final ExportableFunction instanceOfCheck = module.getFunctions().newFunction("INSTANCEOF_CHECK", Arrays.asList(param("thisRef", PrimitiveType.i32), param("type", PrimitiveType.i32)), PrimitiveType.i32);
            final Iff nullCheck = instanceOfCheck.flow.iff("nullcheck", i32.eq(getLocal(instanceOfCheck.localByLabel("thisRef"), null), i32.c(0, null), null), null);
            nullCheck.flow.ret(i32.c(0, null), null);

            final WASMType instanceOfType = module.getTypes().typeFor(Arrays.asList(PrimitiveType.i32, PrimitiveType.i32), PrimitiveType.i32);
            final WASMType resolveType = module.getTypes().typeFor(Arrays.asList(PrimitiveType.i32, PrimitiveType.i32), PrimitiveType.i32);
            final WASMValue theIndex = call(resolveType, Arrays.asList(getLocal(instanceOfCheck.localByLabel("thisRef"), null),
                    i32.c(WASMSSAASTWriter.GENERATED_INSTANCEOF_METHOD_ID, null)), i32.load(4, getLocal(instanceOfCheck.localByLabel("thisRef"), null), null), null);

            instanceOfCheck.flow.ret(call(instanceOfType, Arrays.asList(getLocal(instanceOfCheck.localByLabel("thisRef"), null), getLocal(instanceOfCheck.localByLabel("type"), null)), theIndex, null), null);
        }

        // We need a memory
        module.getMems().newMemory(aOptions.getWasmMinimumPageSize(), aOptions.getWasmMaximumPageSize()).exportAs("memory");

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
        lambdaResolvevtableindex.flow.ret(i32.load(8, getLocal(lambdaResolvevtableindex.localByLabel("thisRef"), null), null), null);

        final ExportableFunction classGetEnumConstants = module.getFunctions().newFunction("jlClass_A1jlObjectgetEnumConstants", Collections.singletonList(param("thisRef", PrimitiveType.i32)), PrimitiveType.i32).toTable();
        classGetEnumConstants.flow.ret(i32.load(0, i32.load(12, getLocal(classGetEnumConstants.localByLabel("thisRef"), null), null), null), null);

        final ExportableFunction classGetName = module.getFunctions().newFunction("jlClass_jlStringgetName", Collections.singletonList(param("thisRef", PrimitiveType.i32)), PrimitiveType.i32).toTable();
        {
            final List<WASMValue> theGetArguments = new ArrayList<>();
            theGetArguments.add(i32.load(16, getLocal(classGetName.localByLabel("thisRef"), null), null));
            classGetName.flow.ret(call(weakFunctionReference("STRINGPOOL_GLOBAL_BY_INDEX", null), theGetArguments, null), null);
        }

        final ExportableFunction classDesiredAssertionStatus = module.getFunctions().newFunction("jlClass_BOOLEANdesiredAssertionStatus", Collections.singletonList(param("thisRef", PrimitiveType.i32)), PrimitiveType.i32).toTable();
        classDesiredAssertionStatus.flow.ret(i32.c(0, null), null);

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
                    return module.getGlobals().newMutableGlobal(theLabel, PrimitiveType.i32, i32.c(-1, null));
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

                    final Block block = runtimeResolvevtableindex.flow.block("m" + theMethodIdentifier.getIdentifier(), null);
                    block.flow.branchIff(block, i32.ne(getLocal(runtimeResolvevtableindex.localByLabel("methodId"), null), i32.c(theMethodIdentifier.getIdentifier(), null), null), null);
                    if (Objects.equals("getClass", theMethod.getName().stringValue())) {
                        block.flow.unreachable(null);
                    } else if (Objects.equals("toString", theMethod.getName().stringValue())) {
                        block.flow.unreachable(null);
                    } else if (Objects.equals("getName", theMethod.getName().stringValue())) {
                        block.flow.ret(i32.c(module.getTables().funcTable().indexOf(classGetName), null), null);
                    } else if (Objects.equals("equals", theMethod.getName().stringValue())) {
                        block.flow.unreachable(null);
                    } else if (Objects.equals("hashCode", theMethod.getName().stringValue())) {
                        block.flow.unreachable(null);
                    } else if (Objects.equals("desiredAssertionStatus", theMethod.getName().stringValue())) {
                        block.flow.ret(i32.c(module.getTables().funcTable().indexOf(classDesiredAssertionStatus), null), null);
                    } else if (Objects.equals("getEnumConstants", theMethod.getName().stringValue())) {
                        block.flow.ret(i32.c(module.getTables().funcTable().indexOf(classGetEnumConstants), null), null);
                    } else {
                        block.flow.unreachable(null);
                    }
                }
            });
            runtimeResolvevtableindex.flow.unreachable(null);
        }

        final ExportableFunction newLambdaFunction = module.getFunctions().newFunction("newLambda", Arrays.asList(param("type", PrimitiveType.i32), param("implMethodNumber", PrimitiveType.i32), param("staticArguments", PrimitiveType.i32)), PrimitiveType.i32);

        aLinkerContext.linkedClasses().forEach(aEntry -> {

            if (Objects.equals(aEntry.edgeType().objectTypeRef(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                return;
            }

            final String theClassName = WASMWriterUtils.toClassName(aEntry.edgeType().objectTypeRef());

            // We also create a global for the runtime class
            module.getGlobals().newMutableGlobal(theClassName + WASMSSAASTWriter.RUNTIMECLASSSUFFIX, PrimitiveType.i32, i32.c(-1, null));

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
                    final Iff b = instanceOf.flow.iff("b" + theType.getUniqueId(), i32.eq(getLocal(instanceOf.localByLabel("p1"), null), i32.c(theType.getUniqueId(), null), null), null);
                    b.flow.ret(i32.c(1, null), null);
                }
                instanceOf.flow.ret(i32.c(0, null), null);

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

                            final Iff iff = resolveTableIndex.flow.iff("b" + theMethodIdentifier.getIdentifier(), i32.eq(getLocal(resolveTableIndex.localByLabel("p1"), null), i32.c(theMethodIdentifier.getIdentifier(), null), null), null);

                            final String theFullMethodName = WASMWriterUtils
                                    .toMethodName(aMethodMapEntry.getProvidingClass().getClassName(),
                                            theMethod.getName(),
                                            theMethod.getSignature());

                            iff.flow.ret(weakFunctionTableReference(theFullMethodName, null), null);
                        }
                    }
                }

                final Iff block = resolveTableIndex.flow.iff("b", i32.eq(getLocal(resolveTableIndex.localByLabel("p1"), null), i32.c(WASMSSAASTWriter.GENERATED_INSTANCEOF_METHOD_ID, null), null), null);
                final int theIndex = module.getTables().funcTable().indexOf(instanceOf);
                block.flow.ret(i32.c(theIndex, null), null);

                resolveTableIndex.flow.unreachable(null);
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
                                                theSignature), null);

                                final List<WASMValue> callValues = new ArrayList<>();
                                for (final Param p : params) {
                                    callValues.add(getLocal(p, null));
                                }
                                function.flow.ret(call(theImplementation, callValues, null), null);
                            } else {

                                final ExportableFunction function = module.getFunctions().newFunction(
                                        WASMWriterUtils
                                                .toMethodName(theLinkedClass.getClassName(), theMethod.getName(), theSignature),
                                        params
                                );

                                final Callable theImplementation = weakFunctionReference(WASMWriterUtils
                                        .toMethodName(aMethodMapEntry.getProvidingClass().getClassName(), theMethod.getName(),
                                                theSignature), null);

                                final List<WASMValue> callValues = new ArrayList<>();
                                for (final Param p : params) {
                                    callValues.add(getLocal(p, null));
                                }
                                function.flow.voidCall(theImplementation, callValues, null);
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
                    final Relooper theRelooper = new Relooper(aOptions);
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
                    throw new IllegalStateException("Error relooping cfg for " + aMethodMapEntry.getProvidingClass().getBytecodeClass().getThisInfo().getConstant().stringValue() + "." + theMethod.getName().stringValue() + " " + theMethod.getSignature() , e);
                }
            });
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
                                    theDispatchArguments.add(getLocal(theAdapterFunction.localByLabel("selfRef"), null));

                                    // Add static arguments
                                    for (int i=0;i<theStaticInvocationType.getSignature().getArguments().length;i++) {

                                        final WASMValue theBasePtr = i32.load(12, getLocal(theAdapterFunction.localByLabel("selfRef"), null), null);
                                        final WASMValue thePtr = i32.add(theBasePtr, i32.c(i * 4, null), null);
                                        switch (TypeRef.toType(theStaticInvocationType.getSignature().getArguments()[i]).resolve()) {
                                            case DOUBLE:
                                            case FLOAT: {
                                                theDispatchArguments.add(ConstExpressions.f32.load(20, thePtr, null));
                                            }
                                            default: {
                                                theDispatchArguments.add(ConstExpressions.i32.load(20, thePtr, null));
                                            }
                                        }
                                    }

                                    // Add dynamic arguments
                                    for (int i=0;i<theDynamicInvocationType.getSignature().getArguments().length;i++) {
                                        theDispatchArguments.add(getLocal(theAdapterFunction.localByLabel("arg" + i), null));
                                    }

                                    theAdapterFunction.flow.voidCall(theImplementationFunction, theDispatchArguments, null);
                                } else {

                                    final ExportableFunction theAdapterFunction = module.getFunctions().newFunction(theFunctionName,
                                            theParams, WASMSSAASTWriter.toType(TypeRef.toType(theDynamicInvocationType.getSignature().getReturnType()))).toTable();

                                    final List<WASMValue> theDispatchArguments = new ArrayList<>();
                                    theDispatchArguments.add(getLocal(theAdapterFunction.localByLabel("selfRef"), null));

                                    // Add static arguments
                                    for (int i=0;i<theStaticInvocationType.getSignature().getArguments().length;i++) {

                                        final WASMValue theBasePtr = i32.load(12, getLocal(theAdapterFunction.localByLabel("selfRef"), null), null);
                                        final WASMValue thePtr = i32.add(theBasePtr, i32.c(i * 4, null), null);
                                        switch (TypeRef.toType(theStaticInvocationType.getSignature().getArguments()[i]).resolve()) {
                                        case DOUBLE:
                                        case FLOAT: {
                                            theDispatchArguments.add(ConstExpressions.f32.load(20, thePtr, null));
                                        }
                                        default: {
                                            theDispatchArguments.add(ConstExpressions.i32.load(20, thePtr, null));
                                        }
                                        }
                                    }

                                    // Add dynamic arguments
                                    for (int i=0;i<theDynamicInvocationType.getSignature().getArguments().length;i++) {
                                        theDispatchArguments.add(getLocal(theAdapterFunction.localByLabel("arg" + i), null));
                                    }

                                    theAdapterFunction.flow.ret(call(theImplementationFunction, theDispatchArguments, null), null);
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
            newRuntimeClassFunction = module.getFunctions().newFunction("newRuntimeClass", Arrays.asList(param("type", PrimitiveType.i32),param("staticSize", PrimitiveType.i32),param("enumValuesOffset", PrimitiveType.i32),param("nameStringPoolIndex", PrimitiveType.i32)), PrimitiveType.i32);
            final Local newRef = newRuntimeClassFunction.newLocal("newRef", PrimitiveType.i32);
            newRuntimeClassFunction.flow.setLocal(newRef,
                    call(module.functionIndex().firstByLabel(mallocFunctionName),
                            Arrays.asList(i32.c(0, null), getLocal(newRuntimeClassFunction.localByLabel("staticSize"), null),
                                    i32.c(-1, null), i32.c(module.getTables().funcTable().indexOf(runtimeResolvevtableindex), null)), null), null);
            newRuntimeClassFunction.flow.i32.store(12, getLocal(newRef, null),
                    i32.add(getLocal(newRef, null), getLocal(newRuntimeClassFunction.localByLabel("enumValuesOffset"), null), null), null);
            newRuntimeClassFunction.flow.i32.store(16, getLocal(newRef, null), getLocal(newRuntimeClassFunction.localByLabel("nameStringPoolIndex"), null), null);
            newRuntimeClassFunction.flow.ret(getLocal(newRef, null), null);
        }

        {
            final ExportableFunction bootstrap = module.getFunctions().newFunction("bootstrap", Collections.emptyList());
            bootstrap.flow.setGlobal(stackTop, i32.sub(i32.mul(currentMemory(null), i32.c(65536, null), null), i32.c(1, null), null), null);

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
                initArguments.add(i32.c(theLinkedClass.getUniqueId(), null));

                final WASMMemoryLayouter.MemoryLayout theLayout = theMemoryLayout.layoutFor(aEntry.edgeType().objectTypeRef());

                initArguments.add(i32.c(theLayout.classSize(), null));

                final BytecodeResolvedFields theStaticFields = theLinkedClass.resolvedFields();
                if (null != theStaticFields.fieldByName("$VALUES")) {
                    initArguments.add(i32.c(theLayout.offsetForClassMember("$VALUES"), null));
                } else {
                    initArguments.add(i32.c(-1, null));
                }
                final StringValue theName = new StringValue(ConstantPool.simpleClassName(theLinkedClass.getClassName().name()));
                final Global theGlobal = theResolver.globalForStringFromPool(theName);
                initArguments.add(i32.c(theConstantPool.register(theName), null));

                bootstrap.flow.setGlobal(runtimeClassGlobal,
                        call(newRuntimeClassFunction, initArguments, null), null);

            });

            final WASMMemoryLayouter.MemoryLayout theStringMemoryLayout = theMemoryLayout.layoutFor(theStringClass.getClassName());
            final List<StringValue> thePoolValues = theConstantPool.stringValues();
            for (int i=0;i<thePoolValues.size();i++) {

                final StringValue theConstantInPool = thePoolValues.get(i);
                final String theData = theConstantInPool.getStringValue();
                final byte[] theDataBytes = theData.getBytes();

                final Global theStringPool = module.getGlobals().globalsIndex().globalByLabel("stringPool" + i);
                final Global theStringPoolData = module.getGlobals().newMutableGlobal("stringPool" + i + "__array", PrimitiveType.i32, i32.c(-1, null));

                final String theMethodName = WASMWriterUtils.toMethodName(
                        BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class),
                        "newArray",
                        new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                                Address.class), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
                final List<WASMValue> theMallocArguments = new ArrayList<>();
                theMallocArguments.add(i32.c(0, null));
                theMallocArguments.add(i32.c(theDataBytes.length, null));
                theMallocArguments.add(call(weakFunctionReference(WASMWriterUtils.toClassName(theArrayClass.getClassName()) + WASMSSAASTWriter.CLASSINITSUFFIX, null), Collections.emptyList(), null));
                final Function theVtableFunction = module.functionIndex().firstByLabel(WASMWriterUtils.toClassName(theArrayClass.getClassName())+ WASMSSAASTWriter.VTABLEFUNCTIONSUFFIX);
                theMallocArguments.add(i32.c(module.getTables().funcTable().indexOf(theVtableFunction), null));
                bootstrap.flow.setGlobal(theStringPoolData, call(module.functionIndex().firstByLabel(theMethodName), theMallocArguments, null), null);
                // Set array value
                for (int j=0; j< theDataBytes.length;j++) {
                    //
                    final int offset = 20 + j * 4;
                    bootstrap.flow.i32.store(offset, getGlobal(theStringPoolData, null), i32.c(theDataBytes[j], null), null);
                }

                final Function theMallocFunction = module.functionIndex().firstByLabel(WASMWriterUtils.toClassName(theMemoryManagerClass.getClassName()) + "_dmbcAddressnewObjectINTINTINT");
                final Function theStringVTable = module.functionIndex().firstByLabel(WASMWriterUtils.toClassName(theStringClass.getClassName())+ WASMSSAASTWriter.VTABLEFUNCTIONSUFFIX);

                bootstrap.flow.setGlobal(theStringPool,
                        call(theMallocFunction, Arrays.asList(i32.c(0, null), i32.c(theStringMemoryLayout.instanceSize(), null),
                                i32.c(theStringClass.getUniqueId(), null),
                                i32.c(module.getTables().funcTable().indexOf(theStringVTable), null)), null), null);

                final Function theStringConstructor = module.functionIndex().firstByLabel(WASMWriterUtils.toClassName(theStringClass.getClassName())+ "_VOIDinitA1BYTE");
                bootstrap.flow.voidCall(theStringConstructor, Arrays.asList(getGlobal(theStringPool, null), getGlobal(theStringPoolData, null)), null);
            }

            final ExportableFunction theGet = module.getFunctions().newFunction("STRINGPOOL_GLOBAL_BY_INDEX",
                    Collections.singletonList(param("index", PrimitiveType.i32)), PrimitiveType.i32);
            {
                for (int i=0;i<thePoolValues.size();i++) {
                    final Global theStringPool = module.getGlobals().globalsIndex().globalByLabel("stringPool" + i);
                    final Iff theIff = theGet.flow.iff("g" + i, i32.eq(getLocal(theGet.localByLabel("index"), null), i32.c(i, null), null), null);
                    theIff.flow.ret(getGlobal(theStringPool, null), null);
                }
                theGet.flow.unreachable(null);
            }

            aLinkerContext.linkedClasses().forEach(aEntry -> {

                if (null != aEntry.targetNode().getBytecodeClass().getAttributes()
                        .getAnnotationByType(EmulatedByRuntime.class.getName())) {
                    return;
                }

                if (!Objects.equals(aEntry.edgeType().objectTypeRef(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {

                    final BytecodeLinkedClass theLinkedClass = aEntry.targetNode();
                    final String theClassName = WASMWriterUtils.toClassName(aEntry.edgeType().objectTypeRef());
                    final Global theGlobal = module.globalsIndex().globalByLabel(theClassName + WASMSSAASTWriter.RUNTIMECLASSSUFFIX);
                    final ExportableFunction theClassInitFunction = module.getFunctions().newFunction( theClassName + WASMSSAASTWriter.CLASSINITSUFFIX, PrimitiveType.i32);

                    final Iff check = theClassInitFunction.flow.iff("check", i32.ne(i32.load(8, getGlobal(theGlobal, null), null), i32.c(1, null), null), null);
                    check.flow.i32.store(8, getGlobal(theGlobal, null), i32.c(1, null), null);

                    if (!theLinkedClass.getClassName().name().equals(Object.class.getName())) {
                        final BytecodeLinkedClass theSuper = theLinkedClass.getSuperClass();
                        final String theSuperWASMName = WASMWriterUtils.toClassName(theSuper.getClassName());
                        check.flow.drop(call(weakFunctionReference(theSuperWASMName + WASMSSAASTWriter.CLASSINITSUFFIX, null), Collections.emptyList(), null), null);
                    }

                    if (aEntry.targetNode().hasClassInitializer()) {
                        check.flow.voidCall(weakFunctionReference(theClassName + "_VOIDclinit", null), Collections.singletonList(i32.c(-1, null)), null);
                    }

                    theClassInitFunction.flow.ret(getGlobal(theGlobal, null), null);
                }
            });

            // After the Bootstrap, we need to all the static stuff on the stack, so it is not garbage collected
            final GlobalsIndex globalIndex = module.globalsIndex();
            bootstrap.flow.setGlobal(stackTop, i32.sub(getGlobal(stackTop, null), i32.c(globalIndex.size() * 4, null), null), null);
            for (int i=0;i<globalIndex.size();i++) {
                final Global global = globalIndex.get(i);
                bootstrap.flow.i32.store(i * 4, getGlobal(stackTop, null), getGlobal(global, null), null);
            }

            bootstrap.exportAs("bootstrap");
        }

        {
            final String mallocFunctionName = WASMWriterUtils.toClassName(theMemoryManagerClass.getClassName()) + "_dmbcAddressnewObjectINTINTINT";
            final Local newRef = newLambdaFunction.newLocal("newRef", PrimitiveType.i32);
            newLambdaFunction.flow.setLocal(newRef,
                    call(module.functionIndex().firstByLabel(mallocFunctionName),
                            Arrays.asList(i32.c(0, null), i32.c(16, null),
                                    getLocal(newLambdaFunction.localByLabel("type"), null), i32.c(module.getTables().funcTable().indexOf(lambdaResolvevtableindex), null)), null), null);
            newLambdaFunction.flow.i32.store(8, getLocal(newRef, null), getLocal(newLambdaFunction.localByLabel("implMethodNumber"), null), null);
            newLambdaFunction.flow.i32.store(12, getLocal(newRef, null), getLocal(newLambdaFunction.localByLabel("staticArguments"), null), null);
            newLambdaFunction.flow.ret(getLocal(newRef, null), null);
        }

        // Main function must be exported
        {
            final ExportableFunction mainFunction = module.functionIndex().firstByLabel(WASMWriterUtils
                    .toMethodName(BytecodeObjectTypeRef.fromRuntimeClass(aEntryPointClass), aEntryPointMethodName,
                            aEntryPointSignatue));
            mainFunction.exportAs("main");
        }

        // We need to generate
        aLinkerContext.linkedClasses().map(t -> t.targetNode()).filter(t -> t.isCallback() && t.getBytecodeClass().getAccessFlags().isInterface()).forEach(t -> {

            final BytecodeResolvedMethods theMethods = t.resolvedMethods();
            final List<BytecodeMethod> availableCallbacks = theMethods.stream().filter(x -> !x.getValue().isConstructor() && !x.getValue().isClassInitializer()
                    && x.getProvidingClass() == t).map(x -> x.getValue()).collect(Collectors.toList());

            if (availableCallbacks.size() > 0) {

                if (availableCallbacks.size() != 1) {
                    throw new IllegalStateException(
                            "Invalid number of callback methods available for type " + t.getClassName().name()
                                    + ", expected 1, got " + availableCallbacks.size());
                }

                final BytecodeMethod theDelegateMethod = availableCallbacks.get(0);

                // Now we need to create a delegate method for this
                final List<PrimitiveType> theSignatureParams = new ArrayList<>();

                final List<Param> theArguments = new ArrayList<>();
                theSignatureParams.add(PrimitiveType.i32);
                theArguments.add(param("target", PrimitiveType.i32));
                final BytecodeTypeRef[] theSignatureArguments = theDelegateMethod.getSignature().getArguments();
                for (int i = 0; i < theSignatureArguments.length; i++) {
                    final PrimitiveType theSignatureType = WASMSSAASTWriter.toType(TypeRef.toType(theSignatureArguments[i]));
                    theArguments.add(param("param" + i, theSignatureType));
                    theSignatureParams.add(theSignatureType);
                }

                final WASMType theCalledFunction = module.getTypes().typeFor(theSignatureParams);

                final BytecodeVirtualMethodIdentifier theMethodIdentifier = aLinkerContext.getMethodCollection().identifierFor(theDelegateMethod);

                final String theFunctionName = WASMWriterUtils
                        .toMethodName(t.getClassName(), theDelegateMethod.getName(), theDelegateMethod.getSignature());
                final ExportableFunction theFunction = module.getFunctions().newFunction(theFunctionName, theArguments);

                final WASMType theResolveType = module.getTypes().typeFor(Arrays.asList(PrimitiveType.i32, PrimitiveType.i32), PrimitiveType.i32);
                final List<WASMValue> theResolveArgument = new ArrayList<>();
                theResolveArgument.add(getLocal(theFunction.localByLabel("target"), null));
                theResolveArgument.add(i32.c(theMethodIdentifier.getIdentifier(), null));
                final WASMValue theIndex = call(theResolveType, theResolveArgument, i32.load(4, getLocal(theFunction.localByLabel("target"), null), null), null);

                final List<WASMValue> theOtherArguments = new ArrayList<>();
                theOtherArguments.add(getLocal(theFunction.localByLabel("target"), null));
                for (int i = 0; i < theSignatureArguments.length; i++) {
                    theOtherArguments.add(getLocal(theFunction.localByLabel("param" + i), null));
                }

                theFunction.flow.voidCallIndirect(theCalledFunction, theOtherArguments, theIndex, null);

                theFunction.exportAs(theFunctionName);
            }
        });

        final StringWriter theStringWriter = new StringWriter();
        final ByteArrayOutputStream theBinaryOutput = new ByteArrayOutputStream();
        final StringWriter theBinarySourceMap = new StringWriter();
        try {
            final PrintWriter theWriter = new PrintWriter(theStringWriter);
            final Exporter exporter = new Exporter(aOptions);
            exporter.export(module, theWriter);
            exporter.export(module, theBinaryOutput, theBinarySourceMap);
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
            theWriter.println("             initialize: function() {},");
            theWriter.println("             initializeFromArchiveClass: function() {},");
            theWriter.println("         },");
            theWriter.println("         tsystem: {");
            theWriter.println("             logDebugObject: function(caller, value) {bytecoder.logDebug(caller, value);},");
            theWriter.println("         },");
            theWriter.println("         printstream: {");
            theWriter.println("             logDebug: function(caller, value) {bytecoder.logDebug(caller,value);},");
            theWriter.println("         },");
            theWriter.println("         memorymanager: {");
            theWriter.println("             logExceptionTextString : function(thisref, p1) {");
            theWriter.println("                 console.log('Exception with message : ' + bytecoder.toJSString(p1));");
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
            theWriter.println("         math: {");
            theWriter.println("             floorDOUBLE: function (thisref, p1) {return Math.floor(p1);},");
            theWriter.println("             ceilDOUBLE: function (thisref, p1) {return Math.ceil(p1);},");
            theWriter.println("             sinDOUBLE: function (thisref, p1) {return Math.sin(p1);},");
            theWriter.println("             cosDOUBLE: function  (thisref, p1) {return Math.cos(p1);},");
            theWriter.println("             tanDOUBLE: function  (thisref, p1) {return Math.tan(p1);},");
            theWriter.println("             roundDOUBLE: function  (thisref, p1) {return Math.round(p1);},");
            theWriter.println("             sqrtDOUBLE: function(thisref, p1) {return Math.sqrt(p1);},");
            theWriter.println("             add: function(thisref, p1, p2) {return p1 + p2;},");
            theWriter.println("             maxLONGLONG: function(thisref, p1, p2) { return Math.max(p1, p2);},");
            theWriter.println("             maxDOUBLEDOUBLE: function(thisref, p1, p2) { return Math.max(p1, p2);},");
            theWriter.println("             maxINTINT: function(thisref, p1, p2) { return Math.max(p1, p2);},");
            theWriter.println("             maxFLOATFLOAT: function(thisref, p1, p2) { return Math.max(p1, p2);},");
            theWriter.println("             minFLOATFLOAT: function(thisref, p1, p2) { return Math.min(p1, p2);},");
            theWriter.println("             minINTINT: function(thisref, p1, p2) { return Math.min(p1, p2);},");
            theWriter.println("             minDOUBLEDOUBLE: function(thisref, p1, p2) { return Math.min(p1, p2);},");
            theWriter.println("             toRadiansDOUBLE: function(thisref, p1) {");
            theWriter.println("                 return p1 * (Math.PI / 180);");
            theWriter.println("             },");
            theWriter.println("             toDegreesDOUBLE: function(thisref, p1) {");
            theWriter.println("                 return p1 * (180 / Math.PI);");
            theWriter.println("             },");
            theWriter.println("             random: function(thisref) { return Math.random();},");
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
            theWriter.println("             nativeconsole: function(caller) {return bytecoder.toBytecoderReference(console);},");
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

                        for (int i=0;i<theSignature.getArguments().length;i++) {
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

                                    final List<BytecodeMethod> theCallbackMethods = theLinkedClass.resolvedMethods().stream().filter(x -> x.getProvidingClass() == theLinkedClass).map(x -> x.getValue()).collect(Collectors.toList());
                                    if (theCallbackMethods.size() != 1) {
                                        throw new IllegalStateException("Wrong number of callback methods in " + theLinkedClass.getClassName().name() + ", expected 1, got " + theCallbackMethods.size());
                                    }
                                    final BytecodeMethod theImpl = theCallbackMethods.get(0);

                                    theWriter.print("function (");
                                    for (int j=0;j<theImpl.getSignature().getArguments().length;j++) {
                                        if (j>0) {
                                            theWriter.print(",");
                                        }
                                        theWriter.print("farg");
                                        theWriter.print(j);
                                    }
                                    theWriter.print(") {");

                                    for (int j=0;j<theImpl.getSignature().getArguments().length;j++) {
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

                                    final String theCallbackMethod = WASMWriterUtils.toMethodName(theLinkedClass.getClassName(), theImpl.getName(), theImpl.getSignature());
                                    theWriter.print("bytecoder.exports.");
                                    theWriter.print(theCallbackMethod);
                                    theWriter.print("(arg");
                                    theWriter.print(i);

                                    for (int j=0;j<theImpl.getSignature().getArguments().length;j++) {
                                        theWriter.print(",");
                                        theWriter.print("marg");
                                        theWriter.print(j);
                                    }

                                    theWriter.print(");");

                                    for (int j=0;j<theImpl.getSignature().getArguments().length;j++) {
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

                                    theWriter.print("}");
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
        return new WASMCompileResult(
                theMinifier,
                new WASMCompileResult.WASMTextualCompileResult(theMemoryLayout, aLinkerContext, new ArrayList<>(), theStringWriter.toString(), aOptions.getFilenamePrefix()),
                new WASMCompileResult.WASMBinaryCompileResult(theMemoryLayout, aLinkerContext, new ArrayList<>(), theBinaryOutput.toByteArray(), aOptions.getFilenamePrefix()),
                new WASMCompileResult.WASMTextualJSCompileResult(theMemoryLayout, aLinkerContext, new ArrayList<>(), theJSCode.toString(), aOptions.getFilenamePrefix()),
                new WASMCompileResult.WASMSourcemapCompileResult(theMemoryLayout, aLinkerContext, new ArrayList<>(), theBinarySourceMap.toString(), aOptions.getFilenamePrefix()));
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