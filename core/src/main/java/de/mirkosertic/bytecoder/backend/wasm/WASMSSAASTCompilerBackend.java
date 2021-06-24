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

import de.mirkosertic.bytecoder.allocator.AbstractAllocator;
import de.mirkosertic.bytecoder.allocator.Register;
import de.mirkosertic.bytecoder.api.EmulatedByRuntime;
import de.mirkosertic.bytecoder.api.Export;
import de.mirkosertic.bytecoder.api.OpaqueIndexed;
import de.mirkosertic.bytecoder.api.OpaqueMethod;
import de.mirkosertic.bytecoder.api.OpaqueProperty;
import de.mirkosertic.bytecoder.api.Substitutes;
import de.mirkosertic.bytecoder.backend.CompileBackend;
import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.ConstantPool;
import de.mirkosertic.bytecoder.backend.NativeMemoryLayouter;
import de.mirkosertic.bytecoder.backend.wasm.ast.Block;
import de.mirkosertic.bytecoder.backend.wasm.ast.Callable;
import de.mirkosertic.bytecoder.backend.wasm.ast.ExportableFunction;
import de.mirkosertic.bytecoder.backend.wasm.ast.Exporter;
import de.mirkosertic.bytecoder.backend.wasm.ast.Expressions;
import de.mirkosertic.bytecoder.backend.wasm.ast.Function;
import de.mirkosertic.bytecoder.backend.wasm.ast.Global;
import de.mirkosertic.bytecoder.backend.wasm.ast.GlobalsIndex;
import de.mirkosertic.bytecoder.backend.wasm.ast.Iff;
import de.mirkosertic.bytecoder.backend.wasm.ast.ImportReference;
import de.mirkosertic.bytecoder.backend.wasm.ast.Local;
import de.mirkosertic.bytecoder.backend.wasm.ast.Module;
import de.mirkosertic.bytecoder.backend.wasm.ast.Param;
import de.mirkosertic.bytecoder.backend.wasm.ast.PrimitiveType;
import de.mirkosertic.bytecoder.backend.wasm.ast.WASMExpression;
import de.mirkosertic.bytecoder.backend.wasm.ast.WASMType;
import de.mirkosertic.bytecoder.backend.wasm.ast.WASMValue;
import de.mirkosertic.bytecoder.backend.wasm.ast.WeakFunctionReferenceCallable;
import de.mirkosertic.bytecoder.backend.wasm.ast.WeakFunctionTableReference;
import de.mirkosertic.bytecoder.classlib.Address;
import de.mirkosertic.bytecoder.classlib.Array;
import de.mirkosertic.bytecoder.classlib.ExceptionManager;
import de.mirkosertic.bytecoder.classlib.MemoryManager;
import de.mirkosertic.bytecoder.classlib.VM;
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
import de.mirkosertic.bytecoder.ssa.MethodHandleExpression;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.ssa.ProgramGeneratorFactory;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.StringValue;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.stackifier.HeadToHeadControlFlowException;
import de.mirkosertic.bytecoder.stackifier.Stackifier;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import static de.mirkosertic.bytecoder.backend.wasm.WASMSSAASTWriter.toType;
import static de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions.call;
import static de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions.currentMemory;
import static de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions.f32;
import static de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions.getGlobal;
import static de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions.getLocal;
import static de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions.i32;
import static de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions.param;
import static de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions.weakFunctionReference;
import static de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions.weakFunctionTableReference;

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

        theMemoryManagerClass.resolveStaticMethod("logAllocations", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[0]));

        theMemoryManagerClass.resolveStaticMethod("freeMem", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[0]));
        theMemoryManagerClass.resolveStaticMethod("usedMem", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[0]));

        theMemoryManagerClass.resolveStaticMethod("free", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT}));
        theMemoryManagerClass.resolveStaticMethod("malloc", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT}));
        theMemoryManagerClass.resolveStaticMethod("newObject", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
        theMemoryManagerClass.resolveStaticMethod("newArray", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
        theMemoryManagerClass.resolveStaticMethod("newArray", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

        final BytecodeLinkedClass theVMClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(VM.class));
        theVMClass.resolveStaticMethod("newStringUTF8", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                String.class), new BytecodeTypeRef[] {new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.BYTE, 1)}));
        theVMClass.resolveStaticMethod("newByteArray", new BytecodeMethodSignature(new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.BYTE, 1), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT}));
        theVMClass.resolveStaticMethod("setByteArrayEntry", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.BYTE, 1), BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.BYTE}));

        // We need this package-private constructor in String.class for bootstrap
        aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(String.class))
                .resolveConstructorInvocation(new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID,
                        new BytecodeTypeRef[] {new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.BYTE, 1),BytecodePrimitiveTypeRef.BYTE}));

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
        if (!theStringClass.resolveVirtualMethod("equals", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.BOOLEAN, new BytecodeTypeRef[] {BytecodeObjectTypeRef.fromRuntimeClass(Object.class)}))) {
            throw new IllegalStateException("No matching stringequals method!");
        }

        final Module module = new Module("bytecoder", aOptions.getFilenamePrefix() + ".wasm.map");

        // Exception handling
        if (aOptions.isEnableExceptions()) {
            module.getEvents().newException(WASMSSAASTWriter.EXCEPTION_NAME, Collections.singletonList(PrimitiveType.i32));
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
                        // For all the other methods we programatically generate
                        // the JS wrapper implementation later by this compiler
                        opaqueReferenceMethods.add(new OpaqueReferenceMethod(theProvidingClass, t));
                    }

                    final BytecodeImportedLink theLink = theProvidingClass.linkFor(t);

                    final String methodName = WASMWriterUtils.toMethodName(theProvidingClass.getClassName(), t.getName(), theSignature);
                    final ImportReference importReference = new ImportReference(theLink.getModuleName(), theLink.getLinkName());

                    final List<Param> params = new ArrayList<>();
                    params.add(param("thisRef", toType(TypeRef.Native.REFERENCE)));
                    for (int i = 0; i < theSignature.getArguments().length; i++) {
                        final BytecodeTypeRef theParamType = theSignature.getArguments()[i];
                        params.add(param("p" + (i + 1), toType(TypeRef.toType(theParamType))));
                    }

                    if (!theSignature.getReturnType().isVoid()) {
                        module.getImports().importFunction(
                                importReference,
                                methodName,
                                params,
                                toType(TypeRef.toType(theSignature.getReturnType()))).toTable();
                    } else {
                        module.getImports().importFunction(
                                importReference,
                                methodName,
                                params).toTable();
                    }
                }
            });
        });

        // Virtual lamnda handler
        final ExportableFunction lambdaStaticResolvevtableindex = module.getFunctions().newFunction("LAMBDAWITHSTATICIMPL" + WASMSSAASTWriter.VTABLEFUNCTIONSUFFIX, Arrays.asList(param("thisRef", PrimitiveType.i32), param("methodId", PrimitiveType.i32)), PrimitiveType.i32).toTable();
        {
            // First of all, we extract the invoked method from the thisref
            final Local methodTypeLocal = lambdaStaticResolvevtableindex.newLocal("methodtype", PrimitiveType.i32);
            lambdaStaticResolvevtableindex.flow.setLocal(methodTypeLocal, i32.load(16, getLocal(lambdaStaticResolvevtableindex.localByLabel("thisRef"), null), null), null);
            final Local runtimeClassLocal = lambdaStaticResolvevtableindex.newLocal("runtimeclass", PrimitiveType.i32);

            final List<WASMValue> arguments = new ArrayList<>();
            arguments.add(i32.load(20, getLocal(methodTypeLocal, null), null));
            lambdaStaticResolvevtableindex.flow.setLocal(
                    runtimeClassLocal,
                    call(weakFunctionReference(WASMSSAASTWriter.RUNTIMECLASSRESOLVER, null),arguments, null)
                    , null);
            final Local defaultVtableResolver = lambdaStaticResolvevtableindex.newLocal("defaultvtable", PrimitiveType.i32);
            lambdaStaticResolvevtableindex.flow.setLocal(defaultVtableResolver, i32.load(24, getLocal(runtimeClassLocal, null), null), null);

            final List<WASMValue> theOtherArguments = new ArrayList<>();
            theOtherArguments.add(getLocal(lambdaStaticResolvevtableindex.localByLabel("thisRef"), null));
            theOtherArguments.add(getLocal(lambdaStaticResolvevtableindex.localByLabel("methodId"), null));

            final List<PrimitiveType> types = new ArrayList<>();
            types.add(PrimitiveType.i32);
            types.add(PrimitiveType.i32);
            final WASMType theCalledFunction = module.getTypes().typeFor(types, PrimitiveType.i32);

            final Local defaulthandlermethod = lambdaStaticResolvevtableindex.newLocal("defaulthandlermethod", PrimitiveType.i32);
            lambdaStaticResolvevtableindex.flow.setLocal(defaulthandlermethod, call(theCalledFunction, theOtherArguments, getLocal(defaultVtableResolver, null), null), null);

            final Iff check = lambdaStaticResolvevtableindex.flow.iff("check",
                    i32.eq(getLocal(defaulthandlermethod, null), i32.c(-1, null), null), null);
            check.flow.ret(i32.load(8, getLocal(lambdaStaticResolvevtableindex.localByLabel("thisRef"), null), null), null);
            check.falseFlow.ret(getLocal(defaulthandlermethod, null), null);
            lambdaStaticResolvevtableindex.flow.unreachable(null);
        }

        final ExportableFunction lambdaConstructorRef = module.getFunctions().newFunction("LAMBDACONSTRUCTORREF" + WASMSSAASTWriter.VTABLEFUNCTIONSUFFIX, Arrays.asList(param("thisRef", PrimitiveType.i32), param("methodId", PrimitiveType.i32)), PrimitiveType.i32).toTable();
        lambdaConstructorRef.flow.ret(i32.load(8, getLocal(lambdaConstructorRef.localByLabel("thisRef"), null), null), null);

        final ExportableFunction classGetName = module.getFunctions().newFunction("jlClass_jlStringgetName", Collections.singletonList(param("thisRef", PrimitiveType.i32)), PrimitiveType.i32).toTable();
        {
            final List<WASMValue> theGetArguments = new ArrayList<>();
            theGetArguments.add(i32.load(16, getLocal(classGetName.localByLabel("thisRef"), null), null));
            classGetName.flow.ret(call(weakFunctionReference("STRINGPOOL_GLOBAL_BY_INDEX", null), theGetArguments, null), null);
        }

        final ExportableFunction classGetSuperClass = module.getFunctions().newFunction("jlClass_jlClassgetSuperclass", Collections.singletonList(param("thisRef", PrimitiveType.i32)), PrimitiveType.i32).toTable();
        {
            final List<WASMValue> theGetArguments = new ArrayList<>();
            theGetArguments.add(getLocal(classGetSuperClass.localByLabel("thisRef"), null));
            classGetSuperClass.flow.ret(call(weakFunctionReference("superTypeOf", null), theGetArguments, null), null);
        }


        final ConstantPool theConstantPool = new ConstantPool();

        final Map<String, WASMSSAASTCompilerBackend.CallSite> theCallsites = new HashMap<>();
        final List<MethodHandleExpression> methodHandles = new ArrayList<>();
        final WASMSSAASTWriter.Resolver theResolver = new WASMSSAASTWriter.Resolver() {

            @Override
            public Global runtimeClassFor(final BytecodeObjectTypeRef aObjectType) {
                final String theGlobalName = WASMWriterUtils.toClassName(aObjectType) + WASMSSAASTWriter.RUNTIMECLASSSUFFIX;
                try {
                    return module.globalsIndex().globalByLabel(theGlobalName);
                } catch (final Exception e) {
                    return module.getGlobals().newMutableGlobal(theGlobalName, PrimitiveType.i32, i32.c(-1, null));
                }
            }

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

            @Override
            public String methodHandleDelegateFor(final MethodHandleExpression e) {
                final int pos = methodHandles.size();
                methodHandles.add(e);
                return "handle" + pos;
            }
        };

        // Superclass resolver
        {
            final ExportableFunction superTypeOf = module.getFunctions().newFunction("superTypeOf", Collections.singletonList(param("thisRef", PrimitiveType.i32)), PrimitiveType.i32);
            aLinkerContext.linkedClasses().forEach(aEntry -> {
                if (aEntry.emulatedByRuntime()) {
                    return;
                }

                final Iff theCheck = superTypeOf.flow.iff(aEntry.getClassName().name(), i32.eq(getLocal(superTypeOf.localByLabel("thisRef"), null),
                        getGlobal(theResolver.runtimeClassFor(aEntry.getClassName()), null), null), null);

                if (!aEntry.getClassName().name().equals(Object.class.getName())) {
                    theCheck.flow.ret(getGlobal(theResolver.runtimeClassFor(aEntry.getSuperClass().getClassName()), null), null);
                } else {
                    theCheck.flow.ret(i32.c(0, null), null);
                }

            });
            superTypeOf.flow.ret(i32.c(0, null), null);
        }

        final ExportableFunction classIsAssignableFrom = module.getFunctions().newFunction("jlClass_BOOLEANisAssignableFromjlClass", Arrays.asList(param("thisRef", PrimitiveType.i32), param("otherType", PrimitiveType.i32)), PrimitiveType.i32).toTable();
        {
            aLinkerContext.linkedClasses().forEach(aEntry -> {
                if (aEntry.emulatedByRuntime()) {
                    return;
                }

                if (aEntry.getClassName().equals(BytecodeObjectTypeRef.fromRuntimeClass(Class.class))) {
                    return;
                }
                final String typeLabel = "" + aEntry.getUniqueId();

                final Global theRuntimeClass = theResolver.runtimeClassFor(aEntry.getClassName());
                final Iff theIff = classIsAssignableFrom.flow.iff(
                        typeLabel, i32.eq(getGlobal(theRuntimeClass, null), getLocal(classIsAssignableFrom.localByLabel("otherType"), null), null), null);


                for (final BytecodeLinkedClass theImplType : aEntry.getImplementingTypes()) {
                    final Iff theInstanceCheckIff = theIff.flow.iff(
                            typeLabel, i32.eq(i32.c(theImplType.getUniqueId(), null), i32.load(20, getLocal(classIsAssignableFrom.localByLabel("thisRef"), null), null), null), null);
                    theInstanceCheckIff.flow.ret(i32.c(1, null), null);
                }
                theIff.flow.ret(i32.c(0, null), null);
            });
            classIsAssignableFrom.flow.ret(i32.c(0, null), null);
        }

        final ExportableFunction classIsInstance = module.getFunctions().newFunction("jlClass_BOOLEANisInstancejlObject", Arrays.asList(param("thisRef", PrimitiveType.i32), param("instance", PrimitiveType.i32)), PrimitiveType.i32).toTable();
        {
            final Local instance = classIsInstance.localByLabel("instance");
            final Iff iff = classIsInstance.flow.iff("nullcheck", i32.eq(i32.c(0, null), getLocal(instance, null), null), null);
            iff.flow.ret(i32.c(0, null), null);
            iff.falseFlow.ret(call(classIsAssignableFrom, Arrays.asList(
                    getLocal(classIsInstance.localByLabel("thisRef"), null),
                    i32.load(0, getLocal(instance, null), null)
            ), null), null);
            classIsInstance.flow.unreachable(null);
        }

        {
            final ExportableFunction theMethod = module.getFunctions()
                    .newFunction("jlClass_BOOLEANdesiredAssertionStatus",
                            Collections.singletonList(param("thisRef", PrimitiveType.i32)), PrimitiveType.i32).toTable();
            theMethod.flow.ret(i32.c(0, null), null);
        }

        {
            final ExportableFunction theMethod = module.getFunctions()
                    .newFunction("jlClass_A1jlObjectgetEnumConstants",
                            Collections.singletonList(param("thisRef", PrimitiveType.i32)), PrimitiveType.i32).toTable();
            theMethod.flow.ret(i32.c(0, null), null);
        }

        {
            final String theWASMMethodName = WASMWriterUtils.toMethodName(BytecodeObjectTypeRef.fromRuntimeClass(Class.class), "getClassLoader", BytecodeLinkedClass.GET_CLASSLOADER_SIGNATURE);
            final ExportableFunction theMethod = module.getFunctions()
                    .newFunction(theWASMMethodName,
                            Collections.singletonList(param("thisRef", PrimitiveType.i32)), PrimitiveType.i32).toTable();

            theMethod.flow.ret(i32.c(0, null), null);
        }

        final ExportableFunction runtimeInstanceOf = module.getFunctions().newFunction("RUNTIMECLASS" + WASMSSAASTWriter.INSTANCEOFSUFFIX, Arrays.asList(param("thisRef", PrimitiveType.i32), param("otherType", PrimitiveType.i32)), PrimitiveType.i32).toTable();
        {
            final BytecodeLinkedClass theClassLinkedCass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Class.class));
            final Block checkblock = runtimeInstanceOf.flow.block("check", null);
            checkblock.flow.branchIff(checkblock, i32.ne(getLocal(runtimeInstanceOf.localByLabel("otherType"), null), i32.c(theClassLinkedCass.getUniqueId(), null), null), null);
            checkblock.flow.ret(i32.c(1, null), null);

            runtimeInstanceOf.flow.ret(i32.c(0, null), null);
        }

        final ExportableFunction runtimeResolvevtableindex = module.getFunctions().newFunction("RUNTIMECLASS" + WASMSSAASTWriter.VTABLEFUNCTIONSUFFIX, Arrays.asList(param("thisRef", PrimitiveType.i32), param("methodId", PrimitiveType.i32)), PrimitiveType.i32).toTable();
        {
            final Block instanceOfBlock = runtimeResolvevtableindex.flow.block("instanceof", null);
            instanceOfBlock.flow.branchIff(instanceOfBlock, i32.ne(getLocal(runtimeResolvevtableindex.localByLabel("methodId"), null), i32.c(WASMSSAASTWriter.GENERATED_INSTANCEOF_METHOD_ID, null), null), null);
            instanceOfBlock.flow.ret(weakFunctionTableReference("RUNTIMECLASS" + WASMSSAASTWriter.INSTANCEOFSUFFIX, null), null);

            final BytecodeLinkedClass theClassLinkedCass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Class.class));
            final BytecodeResolvedMethods theRuntimeMethodMap = aLinkerContext.resolveMethods(theClassLinkedCass);
            theRuntimeMethodMap.stream().forEach(aMethodMapEntry -> {
                final BytecodeMethod theMethod = aMethodMapEntry.getValue();

                if (!theMethod.getAccessFlags().isStatic() && !theMethod.isConstructor() && !theMethod.isClassInitializer() &&
                        (aMethodMapEntry.getProvidingClass().getClassName().equals(BytecodeObjectTypeRef.fromRuntimeClass(Class.class)) || (theMethod.getName().stringValue().equals("equals")))) {

                    final BytecodeVirtualMethodIdentifier theMethodIdentifier = aLinkerContext.getMethodCollection().identifierFor(theMethod);
                    final Block block = runtimeResolvevtableindex.flow.block("m" + theMethodIdentifier.getIdentifier(), null);
                    block.flow.branchIff(block, i32.ne(getLocal(runtimeResolvevtableindex.localByLabel("methodId"), null), i32.c(theMethodIdentifier.getIdentifier(), null), null), null);
                    if (Objects.equals("getClass", theMethod.getName().stringValue())) {
                        block.flow.unreachable(null);
                    } else {
                        // delegate to the corresponding method of java.lang.Class
                        final String theMethodName = WASMWriterUtils.toMethodName(aMethodMapEntry.getProvidingClass().getClassName(),
                                theMethod.getName(), theMethod.getSignature());
                        block.flow.ret(weakFunctionTableReference(theMethodName, null), null);
                    }
                }
            });
            runtimeResolvevtableindex.flow.unreachable(null);
        }

        final ExportableFunction newLambdaImplFunction = module.getFunctions().newFunction("newLambdaImpl", Arrays.asList(param("type", PrimitiveType.i32), param("implMethodNumber", PrimitiveType.i32), param("staticArguments", PrimitiveType.i32)), PrimitiveType.i32);

        aLinkerContext.linkedClasses().forEach(aEntry -> {

            if (Objects.equals(aEntry.getClassName(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                return;
            }

            // We also create a global for the runtime class
            final Global theRuntimeClass = theResolver.runtimeClassFor(aEntry.getClassName());

            final BytecodeResolvedMethods theMethodMap = aLinkerContext.resolveMethods(aEntry);
            theMethodMap.stream().forEach(aMapEntry -> {
                final BytecodeMethod t = aMapEntry.getValue();

                // If the method is provided by the runtime, we do not need to generate the implementation
                if (null != t.getAttributes().getAnnotationByType(EmulatedByRuntime.class.getName())) {

                    if (aMapEntry.getProvidingClass().getClassName().equals(BytecodeObjectTypeRef.fromRuntimeClass(Class.class))
                        && t.getName().stringValue().equals("forName")
                        && t.getSignature().matchesExactlyTo(BytecodeLinkedClass.CLASS_FOR_NAME_SIGNATURE)) {

                        // Special method: we resolve a runtime class by name here

                        final String theWASMMethodName = WASMWriterUtils.toMethodName(aMapEntry.getProvidingClass().getClassName(), t.getName().stringValue(), t.getSignature());

                        final ExportableFunction forNameMethod = module.getFunctions()
                                .newFunction(theWASMMethodName,
                                        Arrays.asList(param("UNUSED", PrimitiveType.i32),
                                        param("name", PrimitiveType.i32),
                                        param("initialize", PrimitiveType.i32),
                                        param("classloader", PrimitiveType.i32)), PrimitiveType.i32).toTable();

                        final String theStringEqualsClass = WASMWriterUtils.toMethodName(BytecodeObjectTypeRef.fromRuntimeClass(String.class), "equals", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.BOOLEAN, new BytecodeTypeRef[] {BytecodeObjectTypeRef.fromRuntimeClass(Object.class)}));

                        // We search for all non abstract non interface classes
                        aLinkerContext.linkedClasses().filter(c -> aLinkerContext.reflectionConfiguration().resolve(c.getClassName().name()).supportsClassForName()).forEach(search -> {
                            if (!search.getBytecodeClass().getAccessFlags().isAbstract() && !search.getBytecodeClass().getAccessFlags().isInterface()) {
                                // Only if the class has a zero arg constructor
                                final BytecodeResolvedMethods theResolved = aLinkerContext.resolveMethods(search);
                                theResolved.stream().filter(j -> j.getProvidingClass() == search).map(BytecodeResolvedMethods.MethodEntry::getValue).filter(j -> j.isConstructor() && j.getSignature().getArguments().length == 0).forEach(m -> {
                                    final Global theGlobal = theResolver.globalForStringFromPool(new StringValue(search.getClassName().name()));
                                    final Global theSearchRuntimeClass = theResolver.runtimeClassFor(search.getClassName());
                                    final WASMExpression stringEqualCall = call(weakFunctionReference(theStringEqualsClass, null), Arrays.asList(getLocal(forNameMethod.localByLabel("name"), null), getGlobal(theGlobal, null)), null);
                                    final Iff theIff = forNameMethod.flow.iff(search.getClassName().name(),
                                            i32.eq(stringEqualCall, i32.c(1, null), null), null);

                                    theIff.flow.ret(getGlobal(theSearchRuntimeClass, null), null);
                                });
                            }
                        });

                        forNameMethod.flow.unreachable(null);
                    }

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
                if (!(aMapEntry.getProvidingClass() == aEntry)) {
                    return;
                }

                // Do not write imported functions here
                if (t.getAccessFlags().isNative()) {
                    return;
                }

                final BytecodeMethodSignature theSignature = t.getSignature();
                if (aEntry.emulatedByRuntime()) {
                    return;
                }

                final String theMethodName = WASMWriterUtils.toMethodName(aEntry.getClassName(), t.getName(), theSignature);
                if (!module.functionIndex().hasFunction(theMethodName)) {
                    final List<Param> params = new ArrayList<>();
                    params.add(param("thisRef", toType(TypeRef.Native.REFERENCE)));
                    for (int i = 0; i < theSignature.getArguments().length; i++) {
                        final BytecodeTypeRef theParamType = theSignature.getArguments()[i];
                        params.add(param("p" + (i + 1), toType(TypeRef.toType(theParamType))));
                    }
                    final Function theFunction;
                    if (!theSignature.getReturnType().isVoid()) {
                        theFunction = module.getFunctions().newFunction(theMethodName, params, toType(TypeRef.toType(theSignature.getReturnType())));
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
        final NativeMemoryLayouter theMemoryLayout = new NativeMemoryLayouter(aLinkerContext, 4);

        // Now everything else
        aLinkerContext.linkedClasses().forEach(aEntry -> {

            final BytecodeLinkedClass theLinkedClass = aEntry;

            if (Objects.equals(theLinkedClass.getClassName(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                return;
            }
            if (theLinkedClass.emulatedByRuntime()) {
                return;
            }

            final BytecodeResolvedMethods theMethodMap = aLinkerContext.resolveMethods(theLinkedClass);
            final String theClassName = WASMWriterUtils.toClassName(aEntry.getClassName());

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

            // First of all, we collect the list of implementation methods
            final Map<Integer, WeakFunctionTableReference> theImplementedMethods = new HashMap<>();

            // The instanceof method is also part of the vtable
            theImplementedMethods.put(WASMSSAASTWriter.GENERATED_INSTANCEOF_METHOD_ID,
                    weakFunctionTableReference(instanceOf.getLabel(), null));

            // Collect all virtual methods and create function references
            final List<BytecodeResolvedMethods.MethodEntry> theEntries = theMethodMap.stream().collect(Collectors.toList());
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

                        final String theFullMethodName = WASMWriterUtils
                                .toMethodName(aMethodMapEntry.getProvidingClass().getClassName(),
                                        theMethod.getName(),
                                        theMethod.getSignature());

                        theImplementedMethods.put(theMethodIdentifier.getIdentifier(), weakFunctionTableReference(theFullMethodName, null));
                    }
                }
            }

            final int binary_search_threshold = 8;

            // Now, we have to check
            // If there are only a few implementation methods,
            // we can use a simple linear comparison chain to find the right method
            // if there are many, we implement a binary search strategy
            if (theImplementedMethods.size() < binary_search_threshold) {
                Expressions theContainerToAdd = resolveTableIndex.flow;
                for (final Map.Entry<Integer, WeakFunctionTableReference> theEntry : theImplementedMethods.entrySet()) {

                    final Iff iff = theContainerToAdd.iff("b" + theEntry.getKey(), i32.eq(getLocal(resolveTableIndex.localByLabel("p1"), null), i32.c(theEntry.getKey(), null), null), null);
                    iff.flow.ret(theEntry.getValue(), null);
                    theContainerToAdd = iff.falseFlow;
                }
            } else {
                final List<Integer> theSorted = theImplementedMethods.keySet().stream().sorted().collect(Collectors.toList());
                final Stack<List<Integer>> theWorkList = new Stack<>();
                theWorkList.push(theSorted);

                final Stack<Expressions> theContainer = new Stack<>();
                theContainer.push(resolveTableIndex.flow);

                int stepCounter = 1;
                while (!theWorkList.isEmpty()) {
                    final List<Integer> theStackTop = theWorkList.pop();
                    Expressions theContainerToAdd = theContainer.pop();
                    if (theStackTop.size() < binary_search_threshold) {
                        for (final int theMethodIdentifier : theStackTop) {
                            final WeakFunctionTableReference theEntry = theImplementedMethods
                                    .get(theMethodIdentifier);

                            // Do we need some sanity check here?
                            final Iff iff = theContainerToAdd.iff("b" + stepCounter++,
                                    i32.eq(getLocal(resolveTableIndex.localByLabel("p1"), null),
                                            i32.c(theMethodIdentifier, null), null), null);
                            iff.flow.ret(theEntry, null);
                            theContainerToAdd = iff.falseFlow;
                        }
                        // We can trap here if nothing was found
                        theContainerToAdd.unreachable(null);
                    } else {
                        final int half = theStackTop.size() / 2;
                        final int theSplitPoint = theStackTop.get(half);
                        final List<Integer> theLowerBound = theStackTop.subList(0, half);
                        final List<Integer> theUpperBound = theStackTop.subList(half, theStackTop.size());

                        final Iff iff = theContainerToAdd.iff("b" + stepCounter++, i32.lt_s(getLocal(resolveTableIndex.localByLabel("p1"), null), i32.c(theSplitPoint, null), null), null);
                        theWorkList.push(theUpperBound);
                        theContainer.push(iff.falseFlow);

                        theWorkList.push(theLowerBound);
                        theContainer.push(iff.flow);
                    }
                }
            }

            // Nothing was found
            // We return -1 for the case the method was called by a lambda handler
            resolveTableIndex.flow.ret(i32.c(-1, null), null);

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

                            params.add(param("UNUSED", toType(TypeRef.Native.REFERENCE)));

                            for (int i = 0; i < theSignature.getArguments().length; i++) {
                                params.add(param("p" + i, toType(TypeRef.toType(theSignature.getArguments()[i]))));
                            }

                            if (!theSignature.getReturnType().isVoid()) {
                                final PrimitiveType returnType = toType(TypeRef.toType(theSignature.getReturnType()));

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

                // We need to create a newInstance function in case this is a constructor
                if (theMethod.isConstructor() && !theLinkedClass.getBytecodeClass().getAccessFlags().isAbstract() && !theLinkedClass.getBytecodeClass().getAccessFlags().isInterface()) {

                    final String theMethodName = WASMWriterUtils.toMethodName(theLinkedClass.getClassName(), "$newInstance", theMethod.getSignature());
                    final List<Param> theParams = new ArrayList<>();
                    theParams.add(param("thisRef", PrimitiveType.i32));
                    for (int i=0;i<theMethod.getSignature().getArguments().length;i++) {
                        theParams.add(param("p" + i, toType(TypeRef.toType(theMethod.getSignature().getArguments()[i]))));
                    }
                    final ExportableFunction theCreateFunction = module.getFunctions().newFunction(
                            theMethodName, theParams, PrimitiveType.i32
                    );
                    theCreateFunction.exportAs(theMethodName);
                    final Local newInstance = theCreateFunction.newLocal("newInstance", PrimitiveType.i32);

                    final NativeMemoryLayouter.MemoryLayout theLayout = theMemoryLayout.layoutFor(theLinkedClass.getClassName());

                    final String theNewObjectMethodName = WASMWriterUtils.toMethodName(
                            BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class),
                            "newObject",
                            new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

                    final String theClassNameToCreate = WASMWriterUtils.toClassName(theLinkedClass.getClassName());
                    final WeakFunctionReferenceCallable theClassInit = weakFunctionReference(theClassNameToCreate + WASMSSAASTWriter.CLASSINITSUFFIX, null);
                    final WeakFunctionReferenceCallable theFunction = weakFunctionReference(theNewObjectMethodName, null);

                    theCreateFunction.flow.setLocal(newInstance, call(theFunction, Arrays.asList(i32.c(0, null), i32.c(theLayout.instanceSize(), null), call(theClassInit, Collections.emptyList(), null), weakFunctionTableReference(theClassName + WASMSSAASTWriter.VTABLEFUNCTIONSUFFIX, null)), null), null);

                    final String theConstructorMethod = WASMWriterUtils.toMethodName(theLinkedClass.getClassName(), theMethod.getName(), theMethod.getSignature());
                    final WeakFunctionReferenceCallable theConsRef = weakFunctionReference(theConstructorMethod, null);

                    final List<WASMValue> theArguments = new ArrayList<>();
                    theArguments.add(getLocal(newInstance, null));
                    for (int i=0;i<theMethod.getSignature().getArguments().length;i++) {
                        theArguments.add(getLocal(theCreateFunction.localByLabel("p" + i), null));
                    }
                    theCreateFunction.flow.voidCall(theConsRef, theArguments, null);

                    theCreateFunction.flow.ret(getLocal(newInstance, null), null);

                    theCreateFunction.toTable();
                }

                final ProgramGenerator theGenerator = programGeneratorFactory.createFor(aLinkerContext, new WASMIntrinsics());
                final Program theSSAProgram = theGenerator.generateFrom(aMethodMapEntry.getProvidingClass().getBytecodeClass(), theMethod);

                //Run optimizer
                aOptions.getOptimizer().optimize(this, theSSAProgram.getControlFlowGraph(), aLinkerContext);

                // Perform register allocation
                final AbstractAllocator theAllocator = aOptions.getAllocator().allocate(theSSAProgram, variable -> {
                    switch (variable.resolveType().resolve()) {
                        case INT:
                        case LONG:
                        case BYTE:
                        case SHORT:
                        case BOOLEAN:
                        case CHAR:
                            return TypeRef.Native.INT;
                        case DOUBLE:
                        case FLOAT:
                            return TypeRef.Native.FLOAT;
                        case REFERENCE:
                            return TypeRef.Native.REFERENCE;
                        default:
                            throw new IllegalArgumentException("Not supported type : " + variable.resolveType().resolve());
                    }
                }, aLinkerContext);

                final List<Param> params = new ArrayList<>();
                if (theMethod.getAccessFlags().isStatic()) {
                    params.add(param("UNUSED", toType(TypeRef.Native.REFERENCE)));
                }
                for (final Variable theVariable : theSSAProgram.getArguments()) {
                    params.add(param(theVariable.getName(), toType(theVariable.resolveType())));
                }
                final String theFunctionLabel = WASMWriterUtils.toMethodName(theLinkedClass.getClassName(), theMethod.getName(), theSignature);
                final ExportableFunction instanceFunction;
                if (!module.functionIndex().hasFunction(theFunctionLabel)) {
                    if (theSignature.getReturnType().isVoid()) {
                        instanceFunction = module.getFunctions().newFunction(
                                WASMWriterUtils.toMethodName(theLinkedClass.getClassName(), theMethod.getName(), theSignature),
                                params);
                    } else {
                        final PrimitiveType returnType = toType(TypeRef.toType(theSignature.getReturnType()));
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

                try {
                    final List<Register> theRegister = theAllocator.assignedRegister();
                    for (final Register r : theRegister) {
                        instanceFunction.newLocal(WASMSSAASTWriter.registerName(r), toType(r.getType()));
                    }

                    // Now we know the exact function argument names, so we rename them
                    int paramIndex = -1;
                    if (theMethod.getAccessFlags().isStatic()) {
                        final Param theParam = instanceFunction.getParams().get(++paramIndex);
                        theParam.renameTo("UNUSED");
                    }

                    for (final Variable theVariable : theSSAProgram.getArguments()) {
                        final Param theParam = instanceFunction.getParams().get(++paramIndex);
                        theParam.renameTo(theVariable.getName());
                    }

                    final WASMSSAASTWriter writer = new WASMSSAASTWriter(theResolver, aLinkerContext, module, aOptions, theSSAProgram, theMemoryLayout, instanceFunction, theAllocator);

                    if (aOptions.isPreferStackifier()) {
                        try {
                            final Stackifier st = new Stackifier(theSSAProgram.getControlFlowGraph());
                            writer.writeStackified(st);

                            aOptions.getLogger().debug("Method {}.{} successfully stackified ", theLinkedClass.getClassName().name(), theMethod.getName().stringValue());

                        } catch (final HeadToHeadControlFlowException e) {

                            // Stackifier has problems, we fallback to relooper instead
                            aOptions.getLogger().warn("Method {}.{} could not be stackified, using Relooper instead", theLinkedClass.getClassName().name(), theMethod.getName().stringValue());

                            final Relooper theRelooper = new Relooper(aOptions);
                            final Relooper.Block theReloopedBlock = theRelooper.reloop(theSSAProgram.getControlFlowGraph());

                            writer.writeRelooped(theReloopedBlock);
                        }
                    } else {
                        final Relooper theRelooper = new Relooper(aOptions);
                        final Relooper.Block theReloopedBlock = theRelooper.reloop(theSSAProgram.getControlFlowGraph());

                        writer.writeRelooped(theReloopedBlock);
                    }
                } catch (final Exception e) {
                    throw new IllegalStateException("Error relooping cfg for " + aMethodMapEntry.getProvidingClass().getBytecodeClass().getThisInfo().getConstant().stringValue() + "." + theMethod.getName().stringValue() + " " + theMethod.getSignature() , e);
                }
            });
        });

        // Convert runtime class it to runtime class
        {
            final ExportableFunction theInstanceOfHelper = module.getFunctions().newFunction(WASMSSAASTWriter.RUNTIMECLASSRESOLVER,
                    Collections.singletonList(param("runtimeClass", PrimitiveType.i32)), PrimitiveType.i32);

            aLinkerContext.linkedClasses().forEach(search -> {
                if (!search.emulatedByRuntime()) {
                    final Iff theIff = theInstanceOfHelper.flow.iff(search.getClassName().name(),
                            i32.eq(i32.c(search.getUniqueId(), null), getLocal(theInstanceOfHelper.localByLabel("runtimeClass"), null), null), null);

                    theIff.flow.ret(call(weakFunctionReference(WASMWriterUtils.toClassName(search.getClassName()) + WASMSSAASTWriter.CLASSINITSUFFIX, null), Collections.emptyList(), null), null);
                }
            });

            theInstanceOfHelper.flow.unreachable(null);
        }

        // NewInstance reflection helper
        {
            final ExportableFunction theInstanceOfHelper = module.getFunctions().newFunction(WASMSSAASTWriter.NEWINSTANCEHELPER,
                    Collections.singletonList(param("runtimeClass", PrimitiveType.i32)), PrimitiveType.i32);

            aLinkerContext.linkedClasses().forEach(search -> {
                if (!search.getBytecodeClass().getAccessFlags().isAbstract() && !search.getBytecodeClass().getAccessFlags()
                        .isInterface() && !search.emulatedByRuntime()) {
                    // Only if the class has a zero arg constructor
                    final BytecodeResolvedMethods theResolved = aLinkerContext.resolveMethods(search);
                    theResolved.stream().filter(j -> j.getProvidingClass() == search).map(BytecodeResolvedMethods.MethodEntry::getValue)
                            .filter(j -> j.isConstructor() && j.getSignature().getArguments().length == 0).forEach(m -> {
                        final Global theGlobal = theResolver.runtimeClassFor(search.getClassName());
                        final String theNewInstanceMethodName = WASMWriterUtils.toMethodName(search.getClassName(), "$newInstance", m.getSignature());
                        final Iff theIff = theInstanceOfHelper.flow.iff(search.getClassName().name(),
                                i32.eq(getGlobal(theGlobal, null), getLocal(theInstanceOfHelper.localByLabel("runtimeClass"), null), null), null);

                        theIff.flow.ret(call(weakFunctionReference(theNewInstanceMethodName, null),
                                Collections.singletonList(i32.c(0, null)), null), null);
                    });
                }
            });

            theInstanceOfHelper.flow.unreachable(null);
        }


        // Render callsites
        for (final Map.Entry<String, CallSite> theEntry : theCallsites.entrySet()) {

            final ExportableFunction theFunction = module.functionIndex().firstByLabel(theEntry.getKey());
            final Program theSSAProgram = theEntry.getValue().program;

            // Perform register allocation
            final AbstractAllocator theAllocator = aOptions.getAllocator().allocate(theSSAProgram, variable -> {
                switch (variable.resolveType().resolve()) {
                    case INT:
                    case LONG:
                    case BYTE:
                    case SHORT:
                    case BOOLEAN:
                    case CHAR:
                        return TypeRef.Native.INT;
                    case DOUBLE:
                    case FLOAT:
                        return TypeRef.Native.FLOAT;
                    case REFERENCE:
                        return TypeRef.Native.REFERENCE;
                    default:
                        throw new IllegalArgumentException("Not supported type : " + variable.resolveType().resolve());
                }
            }, aLinkerContext);

            for (final Register r : theAllocator.assignedRegister()) {
                theFunction.newLocal(WASMSSAASTWriter.registerName(r), toType(r.getType()));
            }

            final WASMSSAASTWriter writer = new WASMSSAASTWriter(theResolver, aLinkerContext, module, aOptions, theSSAProgram, theMemoryLayout, theFunction, theAllocator);

            writer.stackEnter();
            writer.writeExpressionList(theEntry.getValue().bootstrapMethod.getExpressions());
        }

        final ExportableFunction newRuntimeClassFunction;
        {
            final String mallocFunctionName = WASMWriterUtils.toClassName(theMemoryManagerClass.getClassName()) + "_INTnewObjectINTINTINT";
            newRuntimeClassFunction = module.getFunctions().newFunction("newRuntimeClass", Arrays.asList(param("type", PrimitiveType.i32),param("staticSize", PrimitiveType.i32),param("enumValuesOffset", PrimitiveType.i32),param("nameStringPoolIndex", PrimitiveType.i32),param("vtableFunctionIndex", PrimitiveType.i32)), PrimitiveType.i32);
            final Local newRef = newRuntimeClassFunction.newLocal("newRef", PrimitiveType.i32);
            newRuntimeClassFunction.flow.setLocal(newRef,
                    call(module.functionIndex().firstByLabel(mallocFunctionName),
                            Arrays.asList(i32.c(0, null), getLocal(newRuntimeClassFunction.localByLabel("staticSize"), null),
                                    i32.c(-1, null), i32.c(module.getTables().funcTable().indexOf(runtimeResolvevtableindex), null)), null), null);
            newRuntimeClassFunction.flow.i32.store(12, getLocal(newRef, null),
                    i32.add(getLocal(newRef, null), getLocal(newRuntimeClassFunction.localByLabel("enumValuesOffset"), null), null), null);
            newRuntimeClassFunction.flow.i32.store(16, getLocal(newRef, null), getLocal(newRuntimeClassFunction.localByLabel("nameStringPoolIndex"), null), null);
            newRuntimeClassFunction.flow.i32.store(20, getLocal(newRef, null), getLocal(newRuntimeClassFunction.localByLabel("type"), null), null);
            newRuntimeClassFunction.flow.i32.store(24, getLocal(newRef, null), getLocal(newRuntimeClassFunction.localByLabel("vtableFunctionIndex"), null), null);
            newRuntimeClassFunction.flow.ret(getLocal(newRef, null), null);

        }

        {
            final ExportableFunction bootstrap = module.getFunctions().newFunction("bootstrap", Collections.emptyList());
            bootstrap.flow.setGlobal(stackTop, i32.sub(i32.mul(currentMemory(null), i32.c(65536, null), null), i32.c(1, null), null), null);

            // Globals for static class data
            aLinkerContext.linkedClasses().forEach(aEntry -> {

                if (Objects.equals(aEntry.getClassName(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                    return;
                }
                if (aEntry.emulatedByRuntime()) {
                    return;
                }

                final Global runtimeClassGlobal = module.globalsIndex().globalByLabel(WASMWriterUtils.toClassName(aEntry.getClassName()) + WASMSSAASTWriter.RUNTIMECLASSSUFFIX);

                final List<WASMValue> initArguments = new ArrayList<>();
                initArguments.add(i32.c(aEntry.getUniqueId(), null));

                final NativeMemoryLayouter.MemoryLayout theLayout = theMemoryLayout.layoutFor(aEntry.getClassName());

                initArguments.add(i32.c(theLayout.classSize(), null));

                final BytecodeResolvedFields theStaticFields = aEntry.resolvedFields();
                if (null != theStaticFields.fieldByName("$VALUES")) {
                    initArguments.add(i32.c(theLayout.offsetForClassMember("$VALUES"), null));
                } else {
                    initArguments.add(i32.c(-1, null));
                }
                final StringValue theName = new StringValue(aEntry.getClassName().name());
                final Global theGlobal = theResolver.globalForStringFromPool(theName);
                initArguments.add(i32.c(theConstantPool.register(theName), null));
                initArguments.add(weakFunctionTableReference(WASMWriterUtils.toClassName(aEntry.getClassName()) + WASMSSAASTWriter.VTABLEFUNCTIONSUFFIX, null));

                bootstrap.flow.setGlobal(runtimeClassGlobal,
                        call(newRuntimeClassFunction, initArguments, null), null);

            });

            final NativeMemoryLayouter.MemoryLayout theStringMemoryLayout = theMemoryLayout.layoutFor(theStringClass.getClassName());
            final List<StringValue> thePoolValues = theConstantPool.stringValues();
            for (int i=0;i<thePoolValues.size();i++) {

                final StringValue theConstantInPool = thePoolValues.get(i);
                final String theData = theConstantInPool.getStringValue();
                final int l = theData.length();
                final int[] theDataCharacters = new int[l];
                for (int j=0;j<l;j++) {
                    theDataCharacters[j] = theData.charAt(j);
                }

                final Global theStringPool = module.getGlobals().globalsIndex().globalByLabel("stringPool" + i);
                final Global theStringPoolData = module.getGlobals().newMutableGlobal("stringPool" + i + "__array", PrimitiveType.i32, i32.c(-1, null));

                final String theMethodName = WASMWriterUtils.toMethodName(
                        BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class),
                        "newArray",
                        new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
                final List<WASMValue> theMallocArguments = new ArrayList<>();
                theMallocArguments.add(i32.c(0, null));
                theMallocArguments.add(i32.c(theDataCharacters.length, null));
                theMallocArguments.add(call(weakFunctionReference(WASMWriterUtils.toClassName(theArrayClass.getClassName()) + WASMSSAASTWriter.CLASSINITSUFFIX, null), Collections.emptyList(), null));
                final Function theVtableFunction = module.functionIndex().firstByLabel(WASMWriterUtils.toClassName(theArrayClass.getClassName())+ WASMSSAASTWriter.VTABLEFUNCTIONSUFFIX);
                theMallocArguments.add(i32.c(module.getTables().funcTable().indexOf(theVtableFunction), null));
                bootstrap.flow.setGlobal(theStringPoolData, call(module.functionIndex().firstByLabel(theMethodName), theMallocArguments, null), null);
                // Set array value
                for (int j=0; j< theDataCharacters.length;j++) {
                    //
                    final int offset = 20 + j * 8;
                    bootstrap.flow.i32.store(offset, getGlobal(theStringPoolData, null), i32.c(theDataCharacters[j], null), null);
                }

                final Function theMallocFunction = module.functionIndex().firstByLabel(WASMWriterUtils.toClassName(theMemoryManagerClass.getClassName()) + "_INTnewObjectINTINTINT");
                final Function theStringVTable = module.functionIndex().firstByLabel(WASMWriterUtils.toClassName(theStringClass.getClassName())+ WASMSSAASTWriter.VTABLEFUNCTIONSUFFIX);

                final Global runtimeClassGlobal = module.globalsIndex().globalByLabel(WASMWriterUtils.toClassName(BytecodeObjectTypeRef.fromRuntimeClass(String.class)) + WASMSSAASTWriter.RUNTIMECLASSSUFFIX);

                bootstrap.flow.setGlobal(theStringPool,
                        call(theMallocFunction, Arrays.asList(i32.c(0, null), i32.c(theStringMemoryLayout.instanceSize(), null),
                                getGlobal(runtimeClassGlobal, null),
                                i32.c(module.getTables().funcTable().indexOf(theStringVTable), null)), null), null);

                final Function theStringConstructor = module.functionIndex().firstByLabel(WASMWriterUtils.toClassName(theStringClass.getClassName())+ "_VOID$init$A1BYTEBYTE");
                bootstrap.flow.voidCall(theStringConstructor, Arrays.asList(getGlobal(theStringPool, null), getGlobal(theStringPoolData, null), i32.c(0, null)), null);
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

                if (aEntry.emulatedByRuntime()) {
                    return;
                }

                if (!Objects.equals(aEntry.getClassName(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {

                    final String theClassName = WASMWriterUtils.toClassName(aEntry.getClassName());
                    final Global theGlobal = module.globalsIndex().globalByLabel(theClassName + WASMSSAASTWriter.RUNTIMECLASSSUFFIX);
                    final ExportableFunction theClassInitFunction = module.getFunctions().newFunction( theClassName + WASMSSAASTWriter.CLASSINITSUFFIX, PrimitiveType.i32);

                    final Iff check = theClassInitFunction.flow.iff("check", i32.ne(i32.load(8, getGlobal(theGlobal, null), null), i32.c(1, null), null), null);
                    check.flow.i32.store(8, getGlobal(theGlobal, null), i32.c(1, null), null);

                    if (!aEntry.getClassName().name().equals(Object.class.getName())) {
                        final BytecodeLinkedClass theSuper = aEntry.getSuperClass();
                        final String theSuperWASMName = WASMWriterUtils.toClassName(theSuper.getClassName());
                        check.flow.drop(call(weakFunctionReference(theSuperWASMName + WASMSSAASTWriter.CLASSINITSUFFIX, null), Collections.emptyList(), null), null);
                    }

                    if (aEntry.hasClassInitializer()) {
                        check.flow.voidCall(weakFunctionReference(theClassName + "_VOID$clinit$", null), Collections.singletonList(i32.c(-1, null)), null);
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
            final String mallocFunctionName = WASMWriterUtils.toClassName(theMemoryManagerClass.getClassName()) + "_INTnewObjectINTINTINT";
            final Local newRef = newLambdaImplFunction.newLocal("newRef", PrimitiveType.i32);
            newLambdaImplFunction.flow.setLocal(newRef,
                    call(module.functionIndex().firstByLabel(mallocFunctionName),
                            Arrays.asList(i32.c(0, null), i32.c(20, null),
                                    getLocal(newLambdaImplFunction.localByLabel("type"), null), i32.c(module.getTables().funcTable().indexOf(lambdaStaticResolvevtableindex), null)), null), null);
            newLambdaImplFunction.flow.i32.store(8, getLocal(newRef, null), getLocal(newLambdaImplFunction.localByLabel("implMethodNumber"), null), null);
            newLambdaImplFunction.flow.i32.store(12, getLocal(newRef, null), getLocal(newLambdaImplFunction.localByLabel("staticArguments"), null), null);
            newLambdaImplFunction.flow.i32.store(16, getLocal(newRef, null), getLocal(newLambdaImplFunction.localByLabel("type"), null), null);
            newLambdaImplFunction.flow.ret(getLocal(newRef, null), null);
        }

        // Main function must be exported
        {
            final ExportableFunction mainFunction = module.functionIndex().firstByLabel(WASMWriterUtils
                    .toMethodName(BytecodeObjectTypeRef.fromRuntimeClass(aEntryPointClass), aEntryPointMethodName,
                            aEntryPointSignatue));
            mainFunction.exportAs("main");
        }

        // Generate method handle delegate functions here
        for (int i=0;i<methodHandles.size();i++) {

            final MethodHandleExpression theMethodHandle = methodHandles.get(i);

            final String theDelegateMethodName = "handle" + i;

            switch (theMethodHandle.getReferenceKind()) {
                case REF_invokeStatic:
                    writeMethodHandleDelegateInvokeStatic(aLinkerContext, theMethodHandle, theDelegateMethodName, module);
                    break;
                case REF_invokeVirtual:
                    writeMethodHandleDelegateInvokeVirtual(aLinkerContext, theMethodHandle, theDelegateMethodName, module);
                    break;
                case REF_invokeInterface:
                    writeMethodHandleDelegateInvokeInterface(aLinkerContext, theMethodHandle, theDelegateMethodName, module);
                    break;
                case REF_invokeSpecial:
                    writeMethodHandleDelegateInvokeSpecial(aLinkerContext, theMethodHandle, theDelegateMethodName, module);
                    break;
                case REF_newInvokeSpecial:
                    writeMethodHandleDelegateNewInvokeSpecial(aLinkerContext, theMethodHandle, theDelegateMethodName, module);
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

                // Now we need to create a delegate method for this
                final List<PrimitiveType> theSignatureParams = new ArrayList<>();

                final List<Param> theArguments = new ArrayList<>();
                theSignatureParams.add(PrimitiveType.i32);
                theArguments.add(param("target", PrimitiveType.i32));
                final BytecodeTypeRef[] theSignatureArguments = theDelegateMethod.getSignature().getArguments();
                for (int i = 0; i < theSignatureArguments.length; i++) {
                    final PrimitiveType theSignatureType = toType(TypeRef.toType(theSignatureArguments[i]));
                    theArguments.add(param("param" + i, theSignatureType));
                    theSignatureParams.add(theSignatureType);
                }



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

                final WASMType theCalledFunction = module.getTypes().typeFor(theSignatureParams);
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
            theWriter.println("         bytecoder.exports.initDefaultFileHandles(-1,0,1,2);");
            theWriter.println("     },");
            theWriter.println();

            theWriter.println("     intInMemory: function(value) {");
            theWriter.println("         return bytecoder.runningInstanceMemory[value]");
            theWriter.println("                + (bytecoder.runningInstanceMemory[value + 1] * 256)");
            theWriter.println("                + (bytecoder.runningInstanceMemory[value + 2] * 256 * 256)");
            theWriter.println("                + (bytecoder.runningInstanceMemory[value + 3] * 256 * 256 * 256);");
            theWriter.println("     },");

            final int theStringDataOffset = theMemoryLayout.layoutFor(theStringClass.getClassName()).offsetForInstanceMember("value");

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

                                    final List<BytecodeMethod> theCallbackMethods = aLinkerContext.resolveMethods(theLinkedClass).stream().filter(x -> x.getProvidingClass() == theLinkedClass).map(BytecodeResolvedMethods.MethodEntry::getValue).collect(Collectors.toList());
                                    if (theCallbackMethods.size() != 1) {
                                        throw new IllegalStateException("Wrong number of callback methods in " + theLinkedClass.getClassName().name() + ", expected 1, got " + theCallbackMethods.size());
                                    }
                                    final BytecodeMethod theImpl = theCallbackMethods.get(0);

                                    theWriter.print("bytecoder.registerCallback(arg");
                                    theWriter.print(i);
                                    theWriter.print(",function (");
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
        return new WASMCompileResult(
                theMinifier,
                new WASMCompileResult.WASMTextualCompileResult(theStringWriter.toString(), aOptions.getFilenamePrefix()),
                new WASMCompileResult.WASMBinaryCompileResult(theBinaryOutput.toByteArray(), aOptions.getFilenamePrefix()),
                new WASMCompileResult.WASMTextualJSCompileResult(theJSCode.toString(), aOptions.getFilenamePrefix()),
                new WASMCompileResult.WASMSourcemapCompileResult(theBinarySourceMap.toString(), aOptions.getFilenamePrefix()));
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

    private void writeMethodHandleDelegateInvokeStatic(final BytecodeLinkerContext aLinkerContext, final MethodHandleExpression aMethodHandle, final String aDelegateMethodName, final Module aModule) {

        final BytecodeMethodSignature theSignature = aMethodHandle.getImplementationSignature();

        // We compile the list of arguments
        final MethodHandleExpression.AdapterAnnotation theAdapterAnnotation = aMethodHandle.getAdapterAnnotation();

        final ExportableFunction theAdapter;

        final List<Param> theArgs = new ArrayList<>();
        theArgs.add(param("lambdaRef", PrimitiveType.i32));
        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            final PrimitiveType theType = toType(TypeRef.toType(theAdapterAnnotation.getCaptureSignature().getArguments()[k]));
            theArgs.add(param(theArgName, theType));
        }

        if (theSignature.getReturnType().isVoid()) {
            theAdapter = aModule.getFunctions().newFunction(aDelegateMethodName, theArgs).toTable();
        } else {
            theAdapter = aModule.getFunctions().newFunction(aDelegateMethodName, theArgs, toType(TypeRef.toType(theSignature.getReturnType()))).toTable();
        }

        // Now, we reed to resolve the linkargs
        // Offset 12 == list of static arguments
        if (theAdapterAnnotation.getLinkageSignature().getArguments().length > 0) {
            final Local theStaticData = theAdapter.newLocal("staticData", PrimitiveType.i32);
            theAdapter.flow.setLocal(theStaticData, i32.load(12,
                    getLocal(theAdapter.localByLabel("lambdaRef"), null), null), null);

            // For every link argument, we load it from the static data list
            for (int k=0;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {

                final String theArgName = "linkArg" + k;
                final PrimitiveType theType = toType(TypeRef.toType(theAdapterAnnotation.getLinkageSignature().getArguments()[k]));

                final Local theLocal = theAdapter.newLocal(theArgName, theType);
                switch (theType) {
                    case f32:
                        theAdapter.flow.setLocal(
                                theLocal,
                                f32.load(20 + k * 8, getLocal(theStaticData, null), null),
                                null
                        );
                        break;
                    case i32:
                        theAdapter.flow.setLocal(
                                theLocal,
                                i32.load(20 + k * 8, getLocal(theStaticData, null), null),
                                null
                        );
                        break;
                    default:
                        throw new IllegalStateException("Not supported type for link arg " + theType);
                }
            }
        }

        final List<BytecodeTypeRef> theEffectiveSignatureArguments = new ArrayList<>();
        theEffectiveSignatureArguments.addAll(Arrays.asList(theAdapterAnnotation.getLinkageSignature().getArguments()));
        theEffectiveSignatureArguments.addAll(Arrays.asList(theAdapterAnnotation.getCaptureSignature().getArguments()));
        final BytecodeMethodSignature theEffectiveSignature = new BytecodeMethodSignature(theSignature.getReturnType(), theEffectiveSignatureArguments.toArray(new BytecodeTypeRef[0]));

        final List<WASMValue> theArguments = new ArrayList<>();
        theArguments.add(i32.c(0, null));
        for (int k=0;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
            final String theArgName = "linkArg" + k;
            theArguments.add(getLocal(theAdapter.localByLabel(theArgName), null));
        }
        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            theArguments.add(getLocal(theAdapter.localByLabel(theArgName), null));
        }

        final Function theCallee = aModule.functionIndex().firstByLabel(
                WASMWriterUtils.toMethodName(aMethodHandle.getClassName(), aMethodHandle.getMethodName(), theEffectiveSignature)
        );

        if (theSignature.getReturnType().isVoid()) {
            theAdapter.flow.voidCall(
                theCallee,
                theArguments,
                null
            );
        } else {
            theAdapter.flow.ret(call(theCallee, theArguments, null),null);
        }
    }

    private void writeMethodHandleDelegateInvokeVirtual(final BytecodeLinkerContext aLinkerContext, final MethodHandleExpression aMethodHandle, final String aDelegateMethodName, final Module aModule) {

        final BytecodeMethodSignature theSignature = aMethodHandle.getImplementationSignature();

        // We compile the list of arguments
        final MethodHandleExpression.AdapterAnnotation theAdapterAnnotation = aMethodHandle.getAdapterAnnotation();

        final ExportableFunction theAdapter;

        final List<Param> theArgs = new ArrayList<>();
        theArgs.add(param("lambdaRef", PrimitiveType.i32));
        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            final PrimitiveType theType = toType(TypeRef.toType(theAdapterAnnotation.getCaptureSignature().getArguments()[k]));
            theArgs.add(param(theArgName, theType));
        }

        if (theSignature.getReturnType().isVoid()) {
            theAdapter = aModule.getFunctions().newFunction(aDelegateMethodName, theArgs).toTable();
        } else {
            theAdapter = aModule.getFunctions().newFunction(aDelegateMethodName, theArgs, toType(TypeRef.toType(theSignature.getReturnType()))).toTable();
        }

        // Now, we reed to resolve the linkargs
        // Offset 12 == list of static arguments
        if (theAdapterAnnotation.getLinkageSignature().getArguments().length > 0) {
            final Local theStaticData = theAdapter.newLocal("staticData", PrimitiveType.i32);
            theAdapter.flow.setLocal(theStaticData, i32.load(12,
                    getLocal(theAdapter.localByLabel("lambdaRef"), null), null), null);

            // For every link argument, we load it from the static data list
            for (int k=0;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {

                final String theArgName = "linkArg" + k;
                final PrimitiveType theType = toType(TypeRef.toType(theAdapterAnnotation.getLinkageSignature().getArguments()[k]));

                final Local theLocal = theAdapter.newLocal(theArgName, theType);
                switch (theType) {
                    case f32:
                        theAdapter.flow.setLocal(
                                theLocal,
                                f32.load(20 + k * 8, getLocal(theStaticData, null), null),
                                null
                        );
                        break;
                    case i32:
                        theAdapter.flow.setLocal(
                                theLocal,
                                i32.load(20 + k * 8 , getLocal(theStaticData, null), null),
                                null
                        );
                        break;
                    default:
                        throw new IllegalStateException("Not supported type for link arg " + theType);
                }
            }
        }

        final List<BytecodeTypeRef> theEffectiveSignatureArguments = new ArrayList<>();
        for (int k=1;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
            theEffectiveSignatureArguments.add(theAdapterAnnotation.getLinkageSignature().getArguments()[k]);
        }
        theEffectiveSignatureArguments.addAll(Arrays.asList(theAdapterAnnotation.getCaptureSignature().getArguments()));
        final BytecodeMethodSignature theEffectiveSignature = new BytecodeMethodSignature(theSignature.getReturnType(), theEffectiveSignatureArguments.toArray(new BytecodeTypeRef[0]));

        final BytecodeVirtualMethodIdentifier theMethodIdentifier = aLinkerContext.getMethodCollection().identifierFor(aMethodHandle.getMethodName(), theEffectiveSignature);

        final List<PrimitiveType> theSignatureParams = new ArrayList<>();
        theSignatureParams.add(PrimitiveType.i32);
        for (int i = 0; i < theEffectiveSignature.getArguments().length; i++) {
            final BytecodeTypeRef theParamType = theEffectiveSignature.getArguments()[i];
            theSignatureParams.add(toType(TypeRef.toType(theParamType)));
        }

        final WASMType theCalledFunctionType;
        if (!theEffectiveSignature.getReturnType().isVoid()) {
            theCalledFunctionType = aModule.getTypes().typeFor(theSignatureParams, toType(TypeRef.toType(theEffectiveSignature.getReturnType())));
        } else {
            theCalledFunctionType = aModule.getTypes().typeFor(theSignatureParams);
        }

        final WASMType theResolveType = aModule.getTypes().typeFor(Arrays.asList(PrimitiveType.i32, PrimitiveType.i32), PrimitiveType.i32);
        final List<WASMValue> theResolveArgument = new ArrayList<>();
        final Local theTarget = theAdapter.localByLabel("linkArg0");
        theResolveArgument.add(getLocal(theTarget, null));
        theResolveArgument.add(i32.c(theMethodIdentifier.getIdentifier(), null));
        final WASMValue theIndex = call(theResolveType, theResolveArgument, i32.load(4, getLocal(theTarget, null), null), null);

        final List<WASMValue> theArguments = new ArrayList<>();
        theArguments.add(getLocal(theTarget, null));
        for (int k=1;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
            final String theArgName = "linkArg" + k;
            theArguments.add(getLocal(theAdapter.localByLabel(theArgName), null));
        }
        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            theArguments.add(getLocal(theAdapter.localByLabel(theArgName), null));
        }

        if (theSignature.getReturnType().isVoid()) {
            theAdapter.flow.voidCallIndirect(theCalledFunctionType, theArguments, theIndex, null);
        } else {
            theAdapter.flow.ret(call(theCalledFunctionType, theArguments, theIndex, null), null);
        }
    }

    private void writeMethodHandleDelegateNewInvokeSpecial(final BytecodeLinkerContext aLinkerContext, final MethodHandleExpression aMethodHandle, final String aDelegateMethodName, final Module aModule) {
        final BytecodeMethodSignature theSignature = aMethodHandle.getImplementationSignature();

        // We compile the list of arguments
        final MethodHandleExpression.AdapterAnnotation theAdapterAnnotation = aMethodHandle.getAdapterAnnotation();

        final List<Param> theArgs = new ArrayList<>();
        theArgs.add(param("lambdaRef", PrimitiveType.i32));
        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            final PrimitiveType theType = toType(TypeRef.toType(theAdapterAnnotation.getCaptureSignature().getArguments()[k]));
            theArgs.add(param(theArgName, theType));
        }

        final ExportableFunction theAdapter = aModule.getFunctions().newFunction(aDelegateMethodName, theArgs, PrimitiveType.i32).toTable();

        // Now, we reed to resolve the linkargs
        // Offset 12 == list of static arguments
        if (theAdapterAnnotation.getLinkageSignature().getArguments().length > 0) {
            final Local theStaticData = theAdapter.newLocal("staticData", PrimitiveType.i32);
            theAdapter.flow.setLocal(theStaticData, i32.load(12,
                    getLocal(theAdapter.localByLabel("lambdaRef"), null), null), null);

            // For every link argument, we load it from the static data list
            for (int k=0;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {

                final String theArgName = "linkArg" + k;
                final PrimitiveType theType = toType(TypeRef.toType(theAdapterAnnotation.getLinkageSignature().getArguments()[k]));

                final Local theLocal = theAdapter.newLocal(theArgName, theType);
                switch (theType) {
                    case f32:
                        theAdapter.flow.setLocal(
                                theLocal,
                                f32.load(20 + k * 8, getLocal(theStaticData, null), null),
                                null
                        );
                        break;
                    case i32:
                        theAdapter.flow.setLocal(
                                theLocal,
                                i32.load(20 + k * 8, getLocal(theStaticData, null), null),
                                null
                        );
                        break;
                    default:
                        throw new IllegalStateException("Not supported type for link arg " + theType);
                }
            }
        }

        final List<BytecodeTypeRef> theEffectiveSignatureArguments = new ArrayList<>();
        theEffectiveSignatureArguments.addAll(Arrays.asList(theAdapterAnnotation.getLinkageSignature().getArguments()));
        theEffectiveSignatureArguments.addAll(Arrays.asList(theAdapterAnnotation.getCaptureSignature().getArguments()));
        final BytecodeMethodSignature theEffectiveSignature = new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), theEffectiveSignatureArguments.toArray(new BytecodeTypeRef[0]));

        final List<WASMValue> theArguments = new ArrayList<>();
        theArguments.add(i32.c(0, null));
        for (int k=0;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
            final String theArgName = "linkArg" + k;
            theArguments.add(getLocal(theAdapter.localByLabel(theArgName), null));
        }
        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            theArguments.add(getLocal(theAdapter.localByLabel(theArgName), null));
        }

        final Function theCallee = aModule.functionIndex().firstByLabel(
                WASMWriterUtils.toMethodName(aMethodHandle.getClassName(), "$newInstance", new BytecodeMethodSignature(
                        theSignature.getReturnType(), theEffectiveSignature.getArguments()
                ))
        );

        theAdapter.flow.ret(call(theCallee, theArguments, null),null);
    }

    private void writeMethodHandleDelegateInvokeSpecial(final BytecodeLinkerContext aLinkerContext, final MethodHandleExpression aMethodHandle, final String aDelegateMethodName, final Module aModule) {

        final BytecodeMethodSignature theSignature = aMethodHandle.getImplementationSignature();

        // We compile the list of arguments
        final MethodHandleExpression.AdapterAnnotation theAdapterAnnotation = aMethodHandle.getAdapterAnnotation();

        final ExportableFunction theAdapter;

        final List<Param> theArgs = new ArrayList<>();
        theArgs.add(param("lambdaRef", PrimitiveType.i32));
        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            final PrimitiveType theType = toType(TypeRef.toType(theAdapterAnnotation.getCaptureSignature().getArguments()[k]));
            theArgs.add(param(theArgName, theType));
        }

        if (theSignature.getReturnType().isVoid()) {
            theAdapter = aModule.getFunctions().newFunction(aDelegateMethodName, theArgs).toTable();
        } else {
            theAdapter = aModule.getFunctions().newFunction(aDelegateMethodName, theArgs, toType(TypeRef.toType(theSignature.getReturnType()))).toTable();
        }

        // Now, we reed to resolve the linkargs
        // Offset 12 == list of static arguments
        if (theAdapterAnnotation.getLinkageSignature().getArguments().length > 0) {
            final Local theStaticData = theAdapter.newLocal("staticData", PrimitiveType.i32);
            theAdapter.flow.setLocal(theStaticData, i32.load(12,
                    getLocal(theAdapter.localByLabel("lambdaRef"), null), null), null);

            // For every link argument, we load it from the static data list
            for (int k=0;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {

                final String theArgName = "linkArg" + k;
                final PrimitiveType theType = toType(TypeRef.toType(theAdapterAnnotation.getLinkageSignature().getArguments()[k]));

                final Local theLocal = theAdapter.newLocal(theArgName, theType);
                switch (theType) {
                    case f32:
                        theAdapter.flow.setLocal(
                                theLocal,
                                f32.load(20 + k * 8, getLocal(theStaticData, null), null),
                                null
                        );
                        break;
                    case i32:
                        theAdapter.flow.setLocal(
                                theLocal,
                                i32.load(20 + k * 8, getLocal(theStaticData, null), null),
                                null
                        );
                        break;
                    default:
                        throw new IllegalStateException("Not supported type for link arg " + theType);
                }
            }
        }

        final List<BytecodeTypeRef> theEffectiveSignatureArguments = new ArrayList<>();
        for (int k=1;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
            theEffectiveSignatureArguments.add(theAdapterAnnotation.getLinkageSignature().getArguments()[k]);
        }
        theEffectiveSignatureArguments.addAll(Arrays.asList(theAdapterAnnotation.getCaptureSignature().getArguments()));
        final BytecodeMethodSignature theEffectiveSignature = new BytecodeMethodSignature(theSignature.getReturnType(), theEffectiveSignatureArguments.toArray(new BytecodeTypeRef[0]));

        final Function theFunction = aModule.functionIndex().firstByLabel(
                WASMWriterUtils.toMethodName(aMethodHandle.getClassName(), aMethodHandle.getMethodName(), theEffectiveSignature)
        );

        final Local theTarget = theAdapter.localByLabel("linkArg0");
        final List<WASMValue> theArguments = new ArrayList<>();
        theArguments.add(getLocal(theTarget, null));
        for (int k=1;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
            final String theArgName = "linkArg" + k;
            theArguments.add(getLocal(theAdapter.localByLabel(theArgName), null));
        }
        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            theArguments.add(getLocal(theAdapter.localByLabel(theArgName), null));
        }

        if (theSignature.getReturnType().isVoid()) {
            theAdapter.flow.voidCall(theFunction, theArguments, null);
        } else {
            theAdapter.flow.ret(call(theFunction, theArguments,null), null);
        }
    }

    private void writeMethodHandleDelegateInvokeInterface(final BytecodeLinkerContext aLinkerContext, final MethodHandleExpression aMethodHandle, final String aDelegateMethodName, final Module aModule) {
        final BytecodeMethodSignature theSignature = aMethodHandle.getImplementationSignature();

        // We compile the list of arguments
        final MethodHandleExpression.AdapterAnnotation theAdapterAnnotation = aMethodHandle.getAdapterAnnotation();

        final ExportableFunction theAdapter;

        final List<Param> theArgs = new ArrayList<>();
        theArgs.add(param("lambdaRef", PrimitiveType.i32));
        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            final PrimitiveType theType = toType(TypeRef.toType(theAdapterAnnotation.getCaptureSignature().getArguments()[k]));
            theArgs.add(param(theArgName, theType));
        }

        if (theSignature.getReturnType().isVoid()) {
            theAdapter = aModule.getFunctions().newFunction(aDelegateMethodName, theArgs).toTable();
        } else {
            theAdapter = aModule.getFunctions().newFunction(aDelegateMethodName, theArgs, toType(TypeRef.toType(theSignature.getReturnType()))).toTable();
        }

        // Now, we reed to resolve the linkargs
        // Offset 12 == list of static arguments
        if (theAdapterAnnotation.getLinkageSignature().getArguments().length > 0) {
            final Local theStaticData = theAdapter.newLocal("staticData", PrimitiveType.i32);
            theAdapter.flow.setLocal(theStaticData, i32.load(12,
                    getLocal(theAdapter.localByLabel("lambdaRef"), null), null), null);

            // For every link argument, we load it from the static data list
            for (int k=0;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {

                final String theArgName = "linkArg" + k;
                final PrimitiveType theType = toType(TypeRef.toType(theAdapterAnnotation.getLinkageSignature().getArguments()[k]));

                final Local theLocal = theAdapter.newLocal(theArgName, theType);
                switch (theType) {
                    case f32:
                        theAdapter.flow.setLocal(
                                theLocal,
                                f32.load(20 + k * 8, getLocal(theStaticData, null), null),
                                null
                        );
                        break;
                    case i32:
                        theAdapter.flow.setLocal(
                                theLocal,
                                i32.load(20 + k * 8, getLocal(theStaticData, null), null),
                                null
                        );
                        break;
                    default:
                        throw new IllegalStateException("Not supported type for link arg " + theType);
                }
            }
        }

        String theTargetName = null;

        // The first of either the link or capture arguments is the invocation target
        final List<BytecodeTypeRef> theEffectiveSignatureArguments = new ArrayList<>();
        for (int k=0;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
            if (theTargetName == null) {
                theTargetName = "linkArg" + k;
            } else {
                theEffectiveSignatureArguments.add(theAdapterAnnotation.getLinkageSignature().getArguments()[k]);
            }
        }
        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            if (theTargetName == null) {
                theTargetName = "captureArg" + k;
            } else {
                theEffectiveSignatureArguments.add(theAdapterAnnotation.getCaptureSignature().getArguments()[k]);
            }
        }

        final BytecodeMethodSignature theEffectiveSignature = new BytecodeMethodSignature(theSignature.getReturnType(), theEffectiveSignatureArguments.toArray(new BytecodeTypeRef[0]));

        final BytecodeVirtualMethodIdentifier theMethodIdentifier = aLinkerContext.getMethodCollection().identifierFor(aMethodHandle.getMethodName(), theEffectiveSignature);

        final List<PrimitiveType> theSignatureParams = new ArrayList<>();
        theSignatureParams.add(PrimitiveType.i32);
        for (int i = 0; i < theEffectiveSignature.getArguments().length; i++) {
            final BytecodeTypeRef theParamType = theEffectiveSignature.getArguments()[i];
            theSignatureParams.add(toType(TypeRef.toType(theParamType)));
        }

        final WASMType theCalledFunction;
        if (!theEffectiveSignature.getReturnType().isVoid()) {
            theCalledFunction = aModule.getTypes().typeFor(theSignatureParams, toType(TypeRef.toType(theEffectiveSignature.getReturnType())));
        } else {
            theCalledFunction = aModule.getTypes().typeFor(theSignatureParams);
        }

        final WASMType theResolveType = aModule.getTypes().typeFor(Arrays.asList(PrimitiveType.i32, PrimitiveType.i32), PrimitiveType.i32);
        final List<WASMValue> theResolveArgument = new ArrayList<>();
        final Local theTarget = theAdapter.localByLabel(theTargetName);
        theResolveArgument.add(getLocal(theTarget, null));
        theResolveArgument.add(i32.c(theMethodIdentifier.getIdentifier(), null));
        final WASMValue theIndex = call(theResolveType, theResolveArgument, i32.load(4, getLocal(theTarget, null), null), null);

        final List<WASMValue> theArguments = new ArrayList<>();
        theArguments.add(getLocal(theTarget, null));
        for (int k=0;k<theAdapterAnnotation.getLinkageSignature().getArguments().length;k++) {
            final String theArgName = "linkArg" + k;
            if (!theArgName.equals(theTargetName)) {
                theArguments.add(getLocal(theAdapter.localByLabel(theArgName), null));
            }
        }
        for (int k=0;k<theAdapterAnnotation.getCaptureSignature().getArguments().length;k++) {
            final String theArgName = "captureArg" + k;
            if (!theArgName.equals(theTargetName)) {
                theArguments.add(getLocal(theAdapter.localByLabel(theArgName), null));
            }
        }

        if (theSignature.getReturnType().isVoid()) {
            theAdapter.flow.voidCallIndirect(theCalledFunction, theArguments, theIndex, null);
        } else {
            theAdapter.flow.ret(call(theCalledFunction, theArguments, theIndex, null), null);
        }
    }
}