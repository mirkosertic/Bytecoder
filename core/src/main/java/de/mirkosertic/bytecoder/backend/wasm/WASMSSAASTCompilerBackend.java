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
import static de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions.f32;
import static de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions.getGlobal;
import static de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions.getLocal;
import static de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions.i32;
import static de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions.param;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import de.mirkosertic.bytecoder.api.EmulatedByRuntime;
import de.mirkosertic.bytecoder.api.Export;
import de.mirkosertic.bytecoder.backend.CompileBackend;
import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.wasm.ast.Block;
import de.mirkosertic.bytecoder.backend.wasm.ast.ExportableFunction;
import de.mirkosertic.bytecoder.backend.wasm.ast.Exporter;
import de.mirkosertic.bytecoder.backend.wasm.ast.Function;
import de.mirkosertic.bytecoder.backend.wasm.ast.Global;
import de.mirkosertic.bytecoder.backend.wasm.ast.GlobalsIndex;
import de.mirkosertic.bytecoder.backend.wasm.ast.ImportReference;
import de.mirkosertic.bytecoder.backend.wasm.ast.Local;
import de.mirkosertic.bytecoder.backend.wasm.ast.Module;
import de.mirkosertic.bytecoder.backend.wasm.ast.Param;
import de.mirkosertic.bytecoder.backend.wasm.ast.PrimitiveType;
import de.mirkosertic.bytecoder.backend.wasm.ast.Value;
import de.mirkosertic.bytecoder.classlib.Address;
import de.mirkosertic.bytecoder.classlib.MemoryManager;
import de.mirkosertic.bytecoder.core.BytecodeAnnotation;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
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

    private static PrimitiveType toType(final TypeRef aType) {
        switch (aType.resolve()) {
        case DOUBLE:
            return PrimitiveType.f32;
        case FLOAT:
            return PrimitiveType.f32;
        default:
            return PrimitiveType.i32;
        }
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

        final BytecodeLinkedClass theStringClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(String.class));
        if (!theStringClass.resolveConstructorInvocation(new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.BYTE, 1)}))) {
            throw new IllegalStateException("No matching constructor!");
        }

        final Module module = new Module();

        // Comparison of two f32
        {
            final ExportableFunction compareValueF32 = module.getFunctions().newFunction("compareValueF32", Arrays.asList(param("p1", PrimitiveType.f32), param("p2", PrimitiveType.f32)), PrimitiveType.i32);
            final Block b1 = compareValueF32.flow.block("b1");
            b1.flow.branchIff(b1, f32.ne(getLocal(compareValueF32.localByLabel("p1")), getLocal(compareValueF32.localByLabel("p2"))));
            b1.flow.ret(i32.c(0));
            final Block b2 = compareValueF32.flow.block("b2");
            b2.flow.branchIff(b2, f32.ge(getLocal(compareValueF32.localByLabel("p1")), getLocal(compareValueF32.localByLabel("p2"))));
            b2.flow.ret(i32.c(-1));
            compareValueF32.flow.ret(i32.c(1));
        }

        // Comparison of two i32
        {
            final ExportableFunction compareValueI32 = module.getFunctions().newFunction("compareValueI32", Arrays.asList(param("p1", PrimitiveType.i32), param("p2", PrimitiveType.i32)), PrimitiveType.i32);
            final Block b1 = compareValueI32.flow.block("b1");
            b1.flow.branchIff(b1, i32.ne(getLocal(compareValueI32.localByLabel("p1")), getLocal(compareValueI32.localByLabel("p2"))));
            b1.flow.ret(i32.c(0));
            final Block b2 = compareValueI32.flow.block("b2");
            b2.flow.branchIff(b2, i32.ge_s(getLocal(compareValueI32.localByLabel("p1")), getLocal(compareValueI32.localByLabel("p2"))));
            b2.flow.ret(i32.c(-1));
            compareValueI32.flow.ret(i32.c(1));
        }


        // We need some Runtime Imports
        final Function floatRemainder = module.getImports().importFunction(new ImportReference("math", "float_rem"),
                "float_remainder", Arrays.asList(param("p1", PrimitiveType.f32), param("p2", PrimitiveType.f32)), PrimitiveType.f32);

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
                    params.add(param("thisRef",toType(TypeRef.Native.REFERENCE)));
                    for (int i = 0; i < theSignature.getArguments().length; i++) {
                        final BytecodeTypeRef theParamType = theSignature.getArguments()[i];
                        params.add(param("p" + (i + 1), toType(TypeRef.toType(theParamType))));
                    }

                    if (!theSignature.getReturnType().isVoid()) {
                        final Function imported = module.getImports().importFunction(
                                importReference,
                                methodName,
                                params,
                                toType(TypeRef.toType(theSignature.getReturnType()))).toTable();
                    } else {
                        final Function imported = module.getImports().importFunction(
                                importReference,
                                methodName,
                                params).toTable();
                    }
                }
            });
        });

        final ExportableFunction lambdaResolvevtableindex = module.getFunctions().newFunction("LAMBDA__resolvevtableindex", Arrays.asList(param("thisRef", PrimitiveType.i32), param("methodId", PrimitiveType.i32)), PrimitiveType.i32).toTable();
        lambdaResolvevtableindex.flow.ret(i32.load(8, getLocal(lambdaResolvevtableindex.localByLabel("thisRef"))));

        final ExportableFunction classGetEnumConstants = module.getFunctions().newFunction("jlClass_A1jlObjectgetEnumConstants", Arrays.asList(param("thisRef", PrimitiveType.i32)), PrimitiveType.i32).toTable();
        classGetEnumConstants.flow.ret(i32.load(0, i32.load(12, getLocal(classGetEnumConstants.localByLabel("thisRef")))));

        final ExportableFunction classDesiredAssertionStatus = module.getFunctions().newFunction("jlClass_BOOLEANdesiredAssertionStatus", Arrays.asList(param("thisRef", PrimitiveType.i32)), PrimitiveType.i32).toTable();
        classDesiredAssertionStatus.flow.ret(i32.c(0));

        final ExportableFunction runtimeResolvevtableindex = module.getFunctions().newFunction("RUNTIMECLASS__resolvevtableindex", Arrays.asList(param("thisRef", PrimitiveType.i32), param("methodId", PrimitiveType.i32)), PrimitiveType.i32).toTable();
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
                // Constructors for the same reason
                if (t.isConstructor()) {
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
                if (module.functionIndex().firstByLabel(theMethodName) == null) {
                    final List<Param> params = new ArrayList<>();
                    params.add(param("thisRef",toType(TypeRef.Native.REFERENCE)));
                    for (int i = 0; i < theSignature.getArguments().length; i++) {
                        final BytecodeTypeRef theParamType = theSignature.getArguments()[i];
                        params.add(param("p" + (i + 1), toType(TypeRef.toType(theParamType))));
                    }
                    if (!theSignature.getReturnType().isVoid()) {
                        module.getFunctions().newFunction(theMethodName, params, toType(TypeRef.toType(theSignature.getReturnType()))).toTable();
                    } else {
                        module.getFunctions().newFunction(theMethodName, params).toTable();
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

                            params.add(param("UNUSED",toType(TypeRef.Native.REFERENCE)));

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

                                final List<Value> callValues = new ArrayList<>();
                                for (final Param p : params) {
                                    callValues.add(getLocal(p));
                                }
                                function.flow.ret(call(function, callValues));
                            } else {

                                final ExportableFunction function = module.getFunctions().newFunction(
                                        WASMWriterUtils
                                                .toMethodName(theLinkedClass.getClassName(), theMethod.getName(), theSignature),
                                        params
                                );

                                final List<Value> callValues = new ArrayList<>();
                                for (final Param p : params) {
                                    callValues.add(getLocal(p));
                                }
                                function.flow.voidCall(function, callValues);
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
                    params.add(param("UNUSED",toType(TypeRef.Native.REFERENCE)));
                }
                for (final Program.Argument theArgument : theSSAProgram.getArguments()) {
                    final Variable theVariable = theArgument.getVariable();
                    params.add(param(theVariable.getName() ,toType(theVariable.resolveType())));
                }
                ExportableFunction instanceFunction = module.functionIndex().firstByLabel(WASMWriterUtils.toMethodName(theLinkedClass.getClassName(), theMethod.getName(), theSignature));
                if (instanceFunction == null) {
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

                    // TODO: Write real programm logic here
                    instanceFunction.flow.unreachable();
                } catch (final Exception e) {
                    throw new IllegalStateException("Error relooping cfg", e);
                }
            });

            final String theClassName = WASMWriterUtils.toClassName(aEntry.edgeType().objectTypeRef());

            if (!theLinkedClass.getBytecodeClass().getAccessFlags().isInterface()) {

                final ExportableFunction instanceOf = module.getFunctions()
                        .newFunction(theClassName + "__instanceof",
                                Arrays.asList(param("thisRef", PrimitiveType.i32), param("p1", PrimitiveType.i32)), PrimitiveType.i32).toTable();

                for (final BytecodeLinkedClass theType : theLinkedClass.getImplementingTypes()) {
                    final Block b = instanceOf.flow.block("b" + theType.getUniqueId());
                    b.flow.branchIff(b, i32.ne(getLocal(instanceOf.localByLabel("p1")), i32.c(theType.getUniqueId())));
                    b.flow.ret(i32.c(1));
                }
                instanceOf.flow.ret(i32.c(0));


                final ExportableFunction resolveTableIndex = module.getFunctions()
                        .newFunction(theClassName + "__resolvevtableindex",
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

                            final Block block = resolveTableIndex.flow.block("b");
                            block.flow.branchIff(block, i32.ne(getLocal(resolveTableIndex.localByLabel("p1")), i32.c(theMethodIdentifier.getIdentifier())));

                            final String theFullMethodName = WASMWriterUtils
                                    .toMethodName(aMethodMapEntry.getProvidingClass().getClassName(),
                                            theMethod.getName(),
                                            theMethod.getSignature());

                            final int theIndex = module.getTables().funcTable()
                                    .indexOf(module.functionIndex().firstByLabel(theFullMethodName));
                            if (0 > theIndex) {
                                throw new IllegalStateException("Unknown index : " + theFullMethodName);
                            }

                            block.flow.ret(i32.c(theIndex));
                        }
                    }
                }

                final Block block = resolveTableIndex.flow.block("b");
                block.flow.branchIff(block, i32.ne(getLocal(resolveTableIndex.localByLabel("p1")), i32.c(WASMSSAWriter.GENERATED_INSTANCEOF_METHOD_ID)));
                final int theIndex = module.getTables().funcTable().indexOf(instanceOf);
                block.flow.ret(i32.c(theIndex));

                resolveTableIndex.flow.unreachable();
            }

            final ExportableFunction initFunction = module.functionIndex().firstByLabel(theClassName + "__classinitcheck");
            final Global initGlobal = module.getGlobals().newMutableGlobal(theClassName + "__runtimeClass", PrimitiveType.i32, i32.c(-1));
            final Block check = initFunction.flow.block("check");
            check.flow.branchIff(check, i32.eq(i32.load(8, getGlobal(initGlobal)), i32.c(1)));
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

        final Global stackTop = module.getGlobals().newMutableGlobal("STACKTOP", PrimitiveType.i32, i32.c(-1));

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


        // TODO: Write general functions and bootstrap code here
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

                final Global runtimeClassGlobal = module.globalsIndex().globalByLabel(WASMWriterUtils.toClassName(aEntry.edgeType().objectTypeRef()) + "__runtimeClass");

                final List<Value> initArguments = new ArrayList<>();
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

            // TODO: Write String Constants

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
        try {
            final PrintWriter theWriter = new PrintWriter(theStringWriter);

            final Exporter exporter = new Exporter();
            exporter.export(module, theWriter);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        return new WASMCompileResult(
                new WASMCompileResult.WASMCompileContent(theMemoryLayout, aLinkerContext, new ArrayList<>(), theStringWriter.toString()));
    }
}