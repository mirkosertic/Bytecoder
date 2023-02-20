/*
 * Copyright 2023 Mirko Sertic
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
package de.mirkosertic.bytecoder.asm.backend.wasm;

import de.mirkosertic.bytecoder.asm.backend.CodeGenerationFailure;
import de.mirkosertic.bytecoder.asm.backend.CompileOptions;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.ConstExpressions;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.ExportableFunction;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Exporter;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Function;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.FunctionType;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.FunctionsSection;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Global;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.GlobalsSection;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Iff;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.ImportReference;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Module;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Param;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.PrimitiveType;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.ReferencableType;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.StructType;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.TypesSection;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.WasmType;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.WasmValue;
import de.mirkosertic.bytecoder.asm.ir.AnnotationUtils;
import de.mirkosertic.bytecoder.asm.ir.Graph;
import de.mirkosertic.bytecoder.asm.ir.ResolvedClass;
import de.mirkosertic.bytecoder.asm.ir.ResolvedField;
import de.mirkosertic.bytecoder.asm.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.asm.optimizer.Optimizer;
import de.mirkosertic.bytecoder.asm.parser.CompileUnit;
import de.mirkosertic.bytecoder.asm.parser.ConstantPool;
import de.mirkosertic.bytecoder.asm.backend.sequencer.DominatorTree;
import de.mirkosertic.bytecoder.asm.backend.sequencer.Sequencer;
import de.mirkosertic.bytecoder.backend.CompileResult;
import de.mirkosertic.bytecoder.classlib.Array;
import org.objectweb.asm.Type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class WasmBackend {

    public WasmCompileResult generateCodeFor(final CompileUnit compileUnit, final CompileOptions compileOptions) {

        final List<ResolvedClass> resolvedClasses = compileUnit.computeClassDependencies();

        final Module module = new Module("bytecoder", compileOptions.getFilenamePrefix() + ".wasm.map");

        final TypesSection types = module.getTypes();
        // Type for virtual function resolvers
        final List<WasmType> vtArgs = new ArrayList<>();
        vtArgs.add(PrimitiveType.i32);
        final FunctionType vtType = types.functionType(vtArgs, PrimitiveType.i32);

        final ReferencableType implTypesArray = types.arrayType(PrimitiveType.i32);

        final StructType runtimeObject = module.getTypes().structType("runtimeObject", Collections.emptyList());

        // Type for runtime types
        final List<StructType.Field> rtFields = new ArrayList<>();
        rtFields.add(new StructType.Field("typeId", PrimitiveType.i32, false));
        rtFields.add(new StructType.Field("impTypes", ConstExpressions.ref.type(implTypesArray, true), false));
        rtFields.add(new StructType.Field("lambdaMethod", PrimitiveType.i32, false));
        rtFields.add(new StructType.Field("vt_resolver", ConstExpressions.ref.type(vtType, true), false));
        rtFields.add(new StructType.Field("initStatus", PrimitiveType.i32));
        final StructType rtType = types.structSubtype("runtimetype", runtimeObject, rtFields);

        final Map<ResolvedClass, StructType> objectTypeMappings = new HashMap<>();
        final Map<ResolvedClass, StructType> rtTypeMappings = new HashMap<>();
        final Map<ResolvedClass, Function> initFunctions = new HashMap<>();
        ResolvedClass objectClass = null;

        final FunctionsSection functionsSection = module.getFunctions();

        final GlobalsSection globalsSection = module.getGlobals();

        final java.util.function.Function<Type, WasmType> toWASMType = new java.util.function.Function<Type, WasmType>() {
            @Override
            public WasmType apply(final Type argument) {
                switch (argument.getSort()) {
                    case Type.BOOLEAN:
                    case Type.BYTE:
                    case Type.CHAR:
                    case Type.SHORT:
                    case Type.INT:
                        return PrimitiveType.i32;
                    case Type.LONG:
                        return PrimitiveType.i64;
                    case Type.FLOAT:
                        return PrimitiveType.f32;
                    case Type.DOUBLE:
                        return PrimitiveType.f64;
                    case Type.OBJECT:
                        return ConstExpressions.ref.type(runtimeObject, true);
                    case Type.ARRAY:
                        // TODO
                        return ConstExpressions.ref.type(runtimeObject, true);
                    case Type.METHOD:
                        final List<WasmType> arguments = new ArrayList<>();
                        arguments.add(this.apply(Type.getType(Object.class)));
                        for (final Type a : argument.getArgumentTypes()) {
                            arguments.add(this.apply(a));
                        }
                        if (argument.getReturnType().getSort() == Type.VOID) {
                            return module.getTypes().functionType(arguments);
                        }
                        return module.getTypes().functionType(arguments, this.apply(argument.getReturnType()));
                    default:
                        throw new IllegalStateException("Not supported " + argument);
                }
            }
        };

        final java.util.function.Function<ResolvedMethod, FunctionType> toFunctionType = resolvedMethod -> {

            final Type methodType = resolvedMethod.methodType;

            final List<WasmType> arguments = new ArrayList<>();
            if (Modifier.isStatic(resolvedMethod.methodNode.access)) {
                arguments.add(ConstExpressions.ref.type(rtType, true));
            } else {
                arguments.add(toWASMType.apply(Type.getType(Object.class)));
            }
            for (final Type a : methodType.getArgumentTypes()) {
                arguments.add(toWASMType.apply(a));
            }
            if (methodType.getReturnType().getSort() == Type.VOID) {
                return module.getTypes().functionType(arguments);
            }
            return module.getTypes().functionType(arguments, toWASMType.apply(methodType.getReturnType()));
        };

        final List<Param> compareI32Params = new ArrayList<>();
        compareI32Params.add(ConstExpressions.param("a", PrimitiveType.i32));
        compareI32Params.add(ConstExpressions.param("b", PrimitiveType.i32));
        final ExportableFunction compare_i32 = module.getFunctions().newFunction("compare_i32", compareI32Params, PrimitiveType.i32);
        compare_i32.flow.iff("gt", ConstExpressions.i32.gt_s(
                        ConstExpressions.getLocal(compare_i32.localByLabel("a")),
                        ConstExpressions.getLocal(compare_i32.localByLabel("b"))))
                        .flow.ret(ConstExpressions.i32.c(1));
        compare_i32.flow.iff("gt", ConstExpressions.i32.lt_s(
                        ConstExpressions.getLocal(compare_i32.localByLabel("a")),
                        ConstExpressions.getLocal(compare_i32.localByLabel("b"))))
                .flow.ret(ConstExpressions.i32.c(-1));
        compare_i32.flow.ret(ConstExpressions.i32.c(0));

        final List<Param> compareI64Params = new ArrayList<>();
        compareI64Params.add(ConstExpressions.param("a", PrimitiveType.i64));
        compareI64Params.add(ConstExpressions.param("b", PrimitiveType.i64));
        final ExportableFunction compare_i64 = module.getFunctions().newFunction("compare_i64", compareI64Params, PrimitiveType.i32);
        compare_i64.flow.iff("gt", ConstExpressions.i64.gt_s(
                        ConstExpressions.getLocal(compare_i64.localByLabel("a")),
                        ConstExpressions.getLocal(compare_i64.localByLabel("b"))))
                .flow.ret(ConstExpressions.i32.c(1));
        compare_i64.flow.iff("gt", ConstExpressions.i64.lt_s(
                        ConstExpressions.getLocal(compare_i64.localByLabel("a")),
                        ConstExpressions.getLocal(compare_i64.localByLabel("b"))))
                .flow.ret(ConstExpressions.i32.c(-1));
        compare_i64.flow.ret(ConstExpressions.i32.c(0));

        final List<Param> compareF32Params = new ArrayList<>();
        compareF32Params.add(ConstExpressions.param("a", PrimitiveType.f32));
        compareF32Params.add(ConstExpressions.param("b", PrimitiveType.f32));
        final ExportableFunction compare_f32 = module.getFunctions().newFunction("compare_f32", compareF32Params, PrimitiveType.i32);
        compare_f32.flow.iff("gt", ConstExpressions.f32.gt(
                        ConstExpressions.getLocal(compare_f32.localByLabel("a")),
                        ConstExpressions.getLocal(compare_f32.localByLabel("b"))))
                .flow.ret(ConstExpressions.i32.c(1));
        compare_f32.flow.iff("gt", ConstExpressions.f32.lt(
                        ConstExpressions.getLocal(compare_f32.localByLabel("a")),
                        ConstExpressions.getLocal(compare_f32.localByLabel("b"))))
                .flow.ret(ConstExpressions.i32.c(-1));
        compare_f32.flow.ret(ConstExpressions.i32.c(0));

        final List<Param> compareF64Params = new ArrayList<>();
        compareF64Params.add(ConstExpressions.param("a", PrimitiveType.f64));
        compareF64Params.add(ConstExpressions.param("b", PrimitiveType.f64));
        final ExportableFunction compare_f64 = module.getFunctions().newFunction("compare_f64", compareF64Params, PrimitiveType.i32);
        compare_f64.flow.iff("gt", ConstExpressions.f64.gt(
                        ConstExpressions.getLocal(compare_f64.localByLabel("a")),
                        ConstExpressions.getLocal(compare_f64.localByLabel("b"))))
                .flow.ret(ConstExpressions.i32.c(1));
        compare_f64.flow.iff("gt", ConstExpressions.f64.lt(
                        ConstExpressions.getLocal(compare_f64.localByLabel("a")),
                        ConstExpressions.getLocal(compare_f64.localByLabel("b"))))
                .flow.ret(ConstExpressions.i32.c(-1));
        compare_f64.flow.ret(ConstExpressions.i32.c(0));

        for (final ResolvedClass cl : resolvedClasses) {
            // Class objects for
            final String className = WasmHelpers.generateClassName(cl.type);

            final List<StructType.Field> instanceFields = new ArrayList<>();
            if (cl.superClass == null) {
                instanceFields.add(new StructType.Field("runtimetype", ConstExpressions.ref.type(rtType, false)));
            }

            if (cl.isNativeReferenceHolder()) {
                instanceFields.add(new StructType.Field("nativeObject", ConstExpressions.ref.host()));
            }

            final List<StructType.Field> classFields = new ArrayList<>();
            final List<WasmValue> classFieldDefaults = new ArrayList<>();

            for (final ResolvedField rf : cl.resolvedFields) {
                if (rf.owner == cl) {
                    final String fieldName = WasmHelpers.generateFieldName(rf.name);

                    StructType.Field field = null;

                    WasmValue defaultValue = null;
                    switch (rf.type.getSort()) {
                        case Type.BOOLEAN:
                        case Type.BYTE:
                        case Type.CHAR:
                        case Type.SHORT:
                        case Type.INT:
                            field = new StructType.Field(fieldName, PrimitiveType.i32);
                            defaultValue = ConstExpressions.i32.c(0);
                            break;
                        case Type.LONG:
                            field = new StructType.Field(fieldName, PrimitiveType.i64);
                            defaultValue = ConstExpressions.i64.c(0L);
                            break;
                        case Type.FLOAT:
                            field = new StructType.Field(fieldName, PrimitiveType.f32);
                            defaultValue = ConstExpressions.f32.c(0.0f);
                            break;
                        case Type.DOUBLE:
                            field = new StructType.Field(fieldName, PrimitiveType.f64);
                            defaultValue = ConstExpressions.f64.c(0.0d);
                            break;
                        case Type.OBJECT:
                            field = new StructType.Field(fieldName, ConstExpressions.ref.type(objectTypeMappings.get(objectClass), true));
                            defaultValue = ConstExpressions.ref.nullRef();
                            break;
                        case Type.ARRAY:
                            // TODO
                            field = new StructType.Field(fieldName, ConstExpressions.ref.type(objectTypeMappings.get(objectClass), true));
                            defaultValue = ConstExpressions.ref.nullRef();
                            break;
                        default:
                            throw new IllegalStateException("Not supported " + rf.type);
                    }

                    if (Modifier.isStatic(rf.access)) {
                        classFields.add(field);
                        classFieldDefaults.add(defaultValue);
                    } else {
                        instanceFields.add(field);
                    }
                }
            }

            if (!Modifier.isInterface(cl.classNode.access)) {
                if (cl.superClass == null) {
                    objectClass = cl;
                    objectTypeMappings.put(cl, types.structSubtype(className, runtimeObject, instanceFields));
                } else {
                    objectTypeMappings.put(cl, types.structSubtype(className, objectTypeMappings.get(cl.superClass), instanceFields));
                }
            }

            rtTypeMappings.put(cl, types.structSubtype(className + "_rtt", rtType, classFields));

            // TODO: Generate vtable
            final List<Param> params = new ArrayList<>();
            params.add(ConstExpressions.param("methodid", PrimitiveType.i32));
            final ExportableFunction vtFunction = functionsSection.newFunction(className + "_vt", params, PrimitiveType.i32);
            // TODO: either call supertype vtable or lambda method
            vtFunction.flow.unreachable();

            final List<WasmValue> typeIds = cl.allTypesOf().stream().map(t -> ConstExpressions.i32.c(resolvedClasses.indexOf(t))).collect(Collectors.toList());

            final ReferencableType rttType = rtTypeMappings.get(cl);
            final List<WasmValue> initArgs = new ArrayList<>();
            initArgs.add(ConstExpressions.i32.c(resolvedClasses.indexOf(cl))); // type id
            initArgs.add(ConstExpressions.array.newInstance(
                    implTypesArray,
                    ConstExpressions.i32.c(typeIds.size()),
                    typeIds
            )); // impl types
            initArgs.add(ConstExpressions.i32.c(-1)); // lambda method id
            initArgs.add(ConstExpressions.ref.ref(vtFunction)); // vt_resolver
            initArgs.add(ConstExpressions.i32.c(0)); // initstatus

            initArgs.addAll(classFieldDefaults);
            final Global runtimeClassGlobal = globalsSection.newConstantGlobal(className + "_cls",
                    ConstExpressions.ref.type(rttType, false),
                    ConstExpressions.struct.newInstance(
                            rttType, initArgs
                    )
            );

            final ExportableFunction initFunction = functionsSection.newFunction(className + "_i", ConstExpressions.ref.type(rtType, false));
            final Iff initCheck = initFunction.flow.iff("check", ConstExpressions.i32.eq(ConstExpressions.i32.c(0),
                    ConstExpressions.struct.get(rtType, ConstExpressions.getGlobal(runtimeClassGlobal), "initStatus")));
            initCheck.flow.setStruct(rtType, ConstExpressions.getGlobal(runtimeClassGlobal), "initStatus", ConstExpressions.i32.c(1));

            if (cl.superClass != null) {
                final String superInit = WasmHelpers.generateClassName(cl.superClass.type) + "_i";
                initCheck.flow.drop(ConstExpressions.call(ConstExpressions.weakFunctionReference(superInit), Collections.emptyList()));

                if (cl.classInitializer != null) {
                    final List<WasmValue> clInitArgs = new ArrayList<>();
                    clInitArgs.add(ConstExpressions.ref.nullRef());
                    initCheck.flow.voidCall(ConstExpressions.weakFunctionReference(className + "$" + WasmHelpers.generateMethodName(cl.classInitializer.methodNode.name, cl.classInitializer.methodType)), clInitArgs);
                }
            }
            initCheck.flow.branch(initCheck);
            initFunction.flow.ret(ConstExpressions.getGlobal(runtimeClassGlobal));

            initFunctions.put(cl, initFunction);
        }

        module.getTags().tagIndex().add(ConstExpressions.tag("javaexception", ConstExpressions.ref.type(objectTypeMappings.get(objectClass), false)));

        final ConstantPool cs = compileUnit.getConstantPool();
        final List<String> pooledStrings = cs.getPooledStrings();
        for (int i = 0; i < pooledStrings.size(); i++) {
            final StructType stringType = objectTypeMappings.get(compileUnit.findClass(Type.getType(String.class)));
            globalsSection.newMutableGlobal("stringpool_" + i,
                    ConstExpressions.ref.type(stringType, true),
                    ConstExpressions.ref.nullRef()
            );
        }

        // Store for last thrown exception
        globalsSection.newMutableGlobal("lastcaughtexception",
                ConstExpressions.ref.type(objectTypeMappings.get(objectClass), true),
                ConstExpressions.ref.nullRef()
        );

        final BiConsumer<String, WasmType> arrayTypeFactory = (name, wasmType) -> {
            final ResolvedClass arrayBaseClass = compileUnit.findClass(Type.getType(Array.class));
            final StructType arrayBaseType = objectTypeMappings.get(arrayBaseClass);
            final List<StructType.Field> instanceFields = new ArrayList<>();
            instanceFields.add(new StructType.Field("data", ConstExpressions.ref.type(module.getTypes().arrayType(wasmType), false)));
            types.structSubtype(name, arrayBaseType, instanceFields);
        };

        arrayTypeFactory.accept("i32_array", PrimitiveType.i32);
        arrayTypeFactory.accept("i64_array", PrimitiveType.i64);
        arrayTypeFactory.accept("f32_array", PrimitiveType.f32);
        arrayTypeFactory.accept("f64_array", PrimitiveType.f64);
        arrayTypeFactory.accept("obj_array", ConstExpressions.ref.type(runtimeObject, true));

        // InstanceOf Check
        final List<Param> instanceOfParams = new ArrayList<>();
        instanceOfParams.add(ConstExpressions.param("obj", ConstExpressions.ref.type(runtimeObject, true)));
        instanceOfParams.add(ConstExpressions.param("runtimeType", ConstExpressions.ref.type(rtType, false)));
        final ExportableFunction instanceOfCheck = module.getFunctions().newFunction("instanceOf", instanceOfParams, PrimitiveType.i32);
        //TODO: implement instanceof
        instanceOfCheck.flow.ret(ConstExpressions.i32.c(0));


        for (final ResolvedClass cl : resolvedClasses) {
            // Class objects for

            final String className = WasmHelpers.generateClassName(cl.type);

            for (final ResolvedMethod method : cl.resolvedMethods) {
                if (method.owner == cl) {
                    final List<Param> functionParams = new ArrayList<>();

                    if (Modifier.isStatic(method.methodNode.access)) {
                        final WasmType clsRef = ConstExpressions.ref.type(rtType, true);
                        functionParams.add(ConstExpressions.param("unused", clsRef));
                    } else {
                        final WasmType thisRef = ConstExpressions.ref.type(runtimeObject, true);
                        functionParams.add(ConstExpressions.param("thisref", thisRef));
                    }

                    for (int i = 0; i < method.methodType.getArgumentTypes().length; i++) {
                        final Type argument = method.methodType.getArgumentTypes()[i];
                        final WasmType argType = toWASMType.apply(argument);
                        functionParams.add(ConstExpressions.param("arg" + i, argType));
                    }

                    final String methodName = WasmHelpers.generateMethodName(method.methodNode.name, method.methodType);

                    final String implMethodName = className + "$" + methodName;

                    if (Modifier.isNative(method.methodNode.access)) {
                        // Imported function
                        String moduleName = cl.type.getClassName();
                        String functionName = methodName;

                        if (AnnotationUtils.hasAnnotation("Lde/mirkosertic/bytecoder/api/Import;", method.methodNode.visibleAnnotations)) {
                            final Map<String, Object> values = AnnotationUtils.parseAnnotation("Lde/mirkosertic/bytecoder/api/Import;", method.methodNode.visibleAnnotations);
                            moduleName = (String) values.get("module");
                            functionName = (String) values.get("name");
                        }

                        final Function function;
                        if (Type.VOID == method.methodType.getReturnType().getSort()) {
                            function = module.getImports().importFunction(new ImportReference(moduleName, functionName),
                                    implMethodName,
                                    functionParams);
                        } else {
                            function = module.getImports().importFunction(new ImportReference(moduleName, functionName),
                                    implMethodName,
                                    functionParams, toWASMType.apply(method.methodType.getReturnType()));
                        }

                        if (!Modifier.isStatic(method.methodNode.access) && !"<init>".equals(method.methodNode.name)) {
                            function.toTable();
                        }

                    } else if (Modifier.isAbstract(method.methodNode.access)) {

                        // Abstract method, nothing to to
                        // TODO: Handle opaque reference types here

                    } else {

                        // Generate Wasm impl code
                        final ExportableFunction implFunction;
                        if (Type.VOID == method.methodType.getReturnType().getSort()) {
                            implFunction = module.getFunctions().newFunction(implMethodName, functionParams);
                        } else {
                            implFunction = module.getFunctions().newFunction(implMethodName, functionParams, toWASMType.apply(method.methodType.getReturnType()));
                        }

                        final Graph g = method.methodBody;
                        final Optimizer o = compileOptions.getOptimizer();
                        while (o.optimize(method)) {
                            //
                        }

                        final DominatorTree dt = new DominatorTree(g);

                        if (cl.classNode.sourceFile != null) {
                            implFunction.flow.comment("source file is " + cl.classNode.sourceFile);
                        }

                        try {
                            new Sequencer(g, dt, new WasmStructuredControlflowCodeGenerator(compileUnit, module, rtTypeMappings, objectTypeMappings, implFunction, toWASMType, toFunctionType, g));
                        } catch (final RuntimeException e) {
                            throw new CodeGenerationFailure(method, dt, e);
                        }
                        implFunction.flow.unreachable();

                        // Brute force scan over all methods
                        compileUnit.processExportedMethods((name, rm) -> {
                            if (rm == method) {
                                implFunction.exportAs(name);
                            }
                        });

                        if (!Modifier.isStatic(method.methodNode.access)) {
                            implFunction.toTable();
                        }
                    }
                }
            }
        }

        final List<Param> resolveStringConstantParams = new ArrayList<>();
        resolveStringConstantParams.add(ConstExpressions.param("constantId", PrimitiveType.i32));

        final Function resolveStringConstantFunction = module.getImports().importFunction(
                    new ImportReference("bytecoder", "resolveStringConstant"),
                 "resolveStringConstant",
                    resolveStringConstantParams,
                    ConstExpressions.ref.host());

        final ExportableFunction bootstrap = functionsSection.newFunction("bootstrap");
        for (int i = 0; i < pooledStrings.size(); i++) {
            final ResolvedClass stringClass = compileUnit.findClass(Type.getType(String.class));
            final Function stringInitFunction = initFunctions.get(stringClass);
            final StructType stringType = objectTypeMappings.get(stringClass);
            final Global stringpoolGlobal = module.globalsIndex().globalByLabel("stringpool_" + i);

            final List<WasmValue> initArgs = new ArrayList<>();
            initArgs.add(ConstExpressions.call(stringInitFunction, new ArrayList<>()));

            final List<WasmValue> resolveArgs = new ArrayList<>();
            resolveArgs.add(ConstExpressions.i32.c(i));
            initArgs.add(ConstExpressions.call(resolveStringConstantFunction, resolveArgs));

            bootstrap.flow.setGlobal(
                    stringpoolGlobal,
                    ConstExpressions.struct.newInstance(
                            stringType,
                            initArgs
                    )
            );
        }
        bootstrap.exportAs("bootstrap");
        // TODO: fill constant pool and generate JS binding class

        final StringWriter theStringWriter = new StringWriter();
        final ByteArrayOutputStream theBinaryOutput = new ByteArrayOutputStream();
        try {
            final PrintWriter theWriter = new PrintWriter(theStringWriter);
            final Exporter exporter = new Exporter(compileOptions);
            exporter.export(module, theWriter);
            exporter.export(module, theBinaryOutput);

            theBinaryOutput.flush();
            theStringWriter.flush();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        final WasmCompileResult result = new WasmCompileResult();
        //result.add(new CompileResult.BinaryContent(compileOptions.getFilenamePrefix() + "wasmclasses.wasm", theBinaryOutput.toByteArray()));
        result.add(new CompileResult.StringContent(compileOptions.getFilenamePrefix() + "wasmclasses.wat", theStringWriter.toString()));

        return result;
    }
}
