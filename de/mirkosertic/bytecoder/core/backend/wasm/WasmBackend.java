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
package de.mirkosertic.bytecoder.core.backend.wasm;

import de.mirkosertic.bytecoder.api.Callback;
import de.mirkosertic.bytecoder.api.ClassLibProvider;
import de.mirkosertic.bytecoder.classlib.Array;
import de.mirkosertic.bytecoder.core.backend.CodeGenerationFailure;
import de.mirkosertic.bytecoder.core.backend.CompileOptions;
import de.mirkosertic.bytecoder.core.backend.CompileResult;
import de.mirkosertic.bytecoder.core.backend.OpaqueReferenceTypeHelpers;
import de.mirkosertic.bytecoder.core.backend.StringConcatMethod;
import de.mirkosertic.bytecoder.core.backend.StringConcatRegistry;
import de.mirkosertic.bytecoder.core.backend.sequencer.DominatorTree;
import de.mirkosertic.bytecoder.core.backend.sequencer.Sequencer;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.ConstExpressions;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.ExportableFunction;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.Exporter;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.Function;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.FunctionType;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.FunctionsSection;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.Global;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.GlobalsSection;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.Iff;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.ImportReference;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.Local;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.Loop;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.Module;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.Param;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.PrimitiveType;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.ReferencableType;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.StructType;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.TypesSection;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.WasmType;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.WasmValue;
import de.mirkosertic.bytecoder.core.ir.AnnotationUtils;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.ResolvedClass;
import de.mirkosertic.bytecoder.core.ir.ResolvedField;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.optimizer.Optimizer;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;
import de.mirkosertic.bytecoder.core.parser.ConstantPool;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.objectweb.asm.Type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class WasmBackend {

    private WasmValue initCodeForPrimitiveRuntimeClass(final StructType type, final int typeId) {
        final List<WasmValue> initArgs = new ArrayList<>();
        initArgs.add(ConstExpressions.i32.c(typeId));
        initArgs.add(ConstExpressions.ref.ref(ConstExpressions.weakFunctionReference(WasmHelpers.generateClassName(Type.getType(Class.class)) + "_vt")));
        initArgs.add(ConstExpressions.ref.externNullRef());
        initArgs.add(ConstExpressions.ref.nullRef()); // TODO: Impltypes definieren
        return ConstExpressions.struct.newInstance(type, initArgs);
    }

    public WasmCompileResult generateCodeFor(final CompileUnit compileUnit, final CompileOptions compileOptions) {

        final List<ResolvedClass> resolvedClasses = compileUnit.computeClassDependencies();

        final Module module = new Module("bytecoder", compileOptions.getFilenamePrefix() + ".wasm.map");

        final TypesSection types = module.getTypes();
        // Type for _vt function resolvers
        final List<WasmType> vtArgs = new ArrayList<>();
        vtArgs.add(PrimitiveType.i32);
        final FunctionType vtType = types.functionType(vtArgs, PrimitiveType.i32);

        // Type for runtime types
        final StructType rtType = types.structType("runtimetype", Collections.emptyList());

        final Map<ResolvedClass, StructType> objectTypeMappings = new HashMap<>();
        final Map<ResolvedClass, StructType> rtTypeMappings = new HashMap<>();
        final Map<ResolvedClass, Function> initFunctions = new HashMap<>();
        ResolvedClass objectClass = null;
        StructType runtimeClassType = null;

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
                        return ConstExpressions.ref.type(module.getTypes().structTypeByName(WasmHelpers.generateClassName(Type.getType(Object.class))), true);
                    case Type.ARRAY:
                        // TODO
                        return ConstExpressions.ref.type(module.getTypes().structTypeByName(WasmHelpers.generateClassName(Type.getType(Object.class))), true);
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

        // Store to uniquely assign an identifier to a method
        final MethodToIDMapper methodToIDMapper = new MethodToIDMapper();
        final VTableResolver vTableResolver = new VTableResolver(methodToIDMapper);

        // Here goes all the init logic
        final ExportableFunction bootstrap = functionsSection.newFunction("bootstrap");

        for (final ResolvedClass cl : resolvedClasses) {
            // Class objects for
            final String className = WasmHelpers.generateClassName(cl.type);

            final ReferencableType implTypesArray = types.arrayType(PrimitiveType.i32);

            final List<StructType.Field> instanceFields = new ArrayList<>();
            if (cl.superClass == null) {
                instanceFields.add(new StructType.Field("typeId", PrimitiveType.i32));
                instanceFields.add(new StructType.Field("vt_resolver", ConstExpressions.ref.type(vtType, true), false));
                instanceFields.add(new StructType.Field("nativeObject", ConstExpressions.ref.host()));
                instanceFields.add(new StructType.Field("implTypes", ConstExpressions.ref.type(implTypesArray, true), false));
            }

            final List<StructType.Field> classFields = new ArrayList<>();

            final List<WasmValue> classFieldDefaults = new ArrayList<>();

            for (final ResolvedField rf : cl.resolvedFields) {
                if (rf.owner == cl) {
                    final String fieldName = WasmHelpers.generateFieldName(rf.name);

                    final StructType.Field field;

                    final WasmValue defaultValue;
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
                        if (!"$VALUES".equals(fieldName)) {
                            classFields.add(field);
                            classFieldDefaults.add(defaultValue);
                        }
                    } else {
                        instanceFields.add(field);
                    }
                }
            }

            if (cl.superClass == null) {
                objectClass = cl;
                final StructType objectType = types.structSubtype(className, rtType, instanceFields);

                objectTypeMappings.put(cl, objectType);

                final String runtimeTypeName = WasmHelpers.generateClassName(Type.getType(Class.class)) + "_rtt";

                final List<StructType.Field> rttypeFields = new ArrayList<>();
                rttypeFields.add(new StructType.Field("typeId", PrimitiveType.i32, false));
                rttypeFields.add(new StructType.Field("classImplTypes", ConstExpressions.ref.type(implTypesArray, true), false));
                rttypeFields.add(new StructType.Field("initStatus", PrimitiveType.i32));
                rttypeFields.add(new StructType.Field("factoryFor", PrimitiveType.i32));
                rttypeFields.add(new StructType.Field("$VALUES", ConstExpressions.ref.type(objectType, true)));

                runtimeClassType = module.getTypes().structSubtype(
                        runtimeTypeName,
                        objectType,
                        rttypeFields
                );

                rtTypeMappings.put(compileUnit.findClass(Type.getType(Class.class)), runtimeClassType);

            } else {
                objectTypeMappings.put(cl, types.structSubtype(className, objectTypeMappings.get(cl.superClass), instanceFields));
            }

            if (!Class.class.getName().equals(cl.type.getClassName())) {
                rtTypeMappings.put(cl, types.structSubtype(className + "_rtt", runtimeClassType, classFields));
            }

            WasmHelpers.createVTableResolver(module, cl, vTableResolver.resolveFor(cl));

            final List<WasmValue> typeIds = cl.allTypesOf().stream().map(t -> ConstExpressions.i32.c(resolvedClasses.indexOf(t))).collect(Collectors.toList());

            final ReferencableType rttType = rtTypeMappings.get(cl);
            final List<WasmValue> initArgs = new ArrayList<>();
            initArgs.add(ConstExpressions.i32.c(WasmHelpers.TYPE_ID_RUNTIMECLASS));
            initArgs.add(ConstExpressions.ref.ref(ConstExpressions.weakFunctionReference(WasmHelpers.generateClassName(Type.getType(Class.class)) + "_vt")));
            initArgs.add(ConstExpressions.ref.externNullRef());
            initArgs.add(ConstExpressions.ref.nullRef());
            initArgs.add(ConstExpressions.i32.c(resolvedClasses.indexOf(cl))); // type id
            initArgs.add(ConstExpressions.array.newInstance(
                    implTypesArray,
                    typeIds
            )); // class impl types
            initArgs.add(ConstExpressions.i32.c(0)); // initstatus
            initArgs.add(ConstExpressions.i32.c(resolvedClasses.indexOf(cl))); // Factory for
            initArgs.add(ConstExpressions.ref.nullRef()); // $VALUES

            initArgs.addAll(classFieldDefaults);
            final Global runtimeClassGlobal = globalsSection.newConstantGlobal(className + "_cls",
                    ConstExpressions.ref.type(rttType, false),
                    ConstExpressions.struct.newInstance(
                            rttType, initArgs
                    )
            );

            final StructType rttTypeStruct = types.structTypeByName(className + "_rtt");
            final ExportableFunction initFunction = functionsSection.newFunction(className + "_i", ConstExpressions.ref.type(rttTypeStruct, false));
            final Iff initCheck = initFunction.flow.iff("check", ConstExpressions.i32.eq(ConstExpressions.i32.c(0),
                    ConstExpressions.struct.get(rttTypeStruct, ConstExpressions.getGlobal(runtimeClassGlobal), "initStatus")));
            initCheck.flow.setStruct(rttTypeStruct, ConstExpressions.getGlobal(runtimeClassGlobal), "initStatus", ConstExpressions.i32.c(1));

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

        {
            // Create import and export functions for all java objects
            final String className = WasmHelpers.generateClassName(objectClass.type);
            final List<Param> getValueParams = new ArrayList<>();
            getValueParams.add(ConstExpressions.param("instance", ConstExpressions.ref.type(objectTypeMappings.get(objectClass), false)));
            final ExportableFunction getValue = module.getFunctions().newFunction(
                    objectClass.type.getClassName()+"_getNativeObject",
                    getValueParams,
                    ConstExpressions.ref.host()
            );
            getValue.flow.ret(
                    ConstExpressions.struct.get(
                            objectTypeMappings.get(objectClass),
                            ConstExpressions.getLocal(getValue.localByLabel("instance")),
                            "nativeObject"
                    )
            );

            getValue.exportAs(className + "$getNativeObject");

            final List<Param> setValueParams = new ArrayList<>();
            setValueParams.add(ConstExpressions.param("instance", ConstExpressions.ref.type(objectTypeMappings.get(objectClass), false)));
            setValueParams.add(ConstExpressions.param("value", ConstExpressions.ref.host()));
            final ExportableFunction setValue = module.getFunctions().newFunction(
                    objectClass.type.getClassName()+"_setNativeObject",
                    setValueParams
            );
            setValue.flow.setStruct(
                    objectTypeMappings.get(objectClass),
                    ConstExpressions.getLocal(setValue.localByLabel("instance")),
                    "nativeObject",
                    ConstExpressions.getLocal(setValue.localByLabel("value"))
            );
            setValue.flow.ret();

            setValue.exportAs(className + "$setNativeObject");
        }

        final List<WasmType> exceptionArguments = new ArrayList<>();
        exceptionArguments.add(ConstExpressions.ref.type(module.getTypes().structTypeByName(WasmHelpers.generateClassName(Type.getType(Object.class))), true));
        module.getTags().tagIndex().add(ConstExpressions.tag("javaexception",  module.getTypes().functionType(exceptionArguments)));

        // Primitive types
        final StructType javaLangObjectType = objectTypeMappings.get(objectClass);
        globalsSection.newConstantGlobal("primitive_boolean",
                ConstExpressions.ref.type(javaLangObjectType, false),
                initCodeForPrimitiveRuntimeClass(javaLangObjectType, WasmHelpers.TYPE_ID_BOOLEAN)
        );
        globalsSection.newConstantGlobal("primitive_byte",
                ConstExpressions.ref.type(javaLangObjectType, false),
                initCodeForPrimitiveRuntimeClass(javaLangObjectType, WasmHelpers.TYPE_ID_BYTE)
        );
        globalsSection.newConstantGlobal("primitive_char",
                ConstExpressions.ref.type(javaLangObjectType, false),
                initCodeForPrimitiveRuntimeClass(javaLangObjectType, WasmHelpers.TYPE_ID_CHAR)
        );
        globalsSection.newConstantGlobal("primitive_short",
                ConstExpressions.ref.type(javaLangObjectType, false),
                initCodeForPrimitiveRuntimeClass(javaLangObjectType, WasmHelpers.TYPE_ID_SHORT)
        );
        globalsSection.newConstantGlobal("primitive_int",
                ConstExpressions.ref.type(javaLangObjectType, false),
                initCodeForPrimitiveRuntimeClass(javaLangObjectType, WasmHelpers.TYPE_ID_INT)
        );
        globalsSection.newConstantGlobal("primitive_long",
                ConstExpressions.ref.type(javaLangObjectType, false),
                initCodeForPrimitiveRuntimeClass(javaLangObjectType, WasmHelpers.TYPE_ID_LONG)
        );
        globalsSection.newConstantGlobal("primitive_float",
                ConstExpressions.ref.type(javaLangObjectType, false),
                initCodeForPrimitiveRuntimeClass(javaLangObjectType, WasmHelpers.TYPE_ID_FLOAT)
        );
        globalsSection.newConstantGlobal("primitive_double",
                ConstExpressions.ref.type(javaLangObjectType, false),
                initCodeForPrimitiveRuntimeClass(javaLangObjectType, WasmHelpers.TYPE_ID_DOUBLE)
        );
        globalsSection.newConstantGlobal("primitive_void",
                ConstExpressions.ref.type(javaLangObjectType, false),
                initCodeForPrimitiveRuntimeClass(javaLangObjectType, WasmHelpers.TYPE_ID_VOID)
        );

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
                ConstExpressions.ref.type(module.getTypes().structTypeByName(WasmHelpers.generateClassName(Type.getType(Object.class))), true),
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
        arrayTypeFactory.accept("obj_array", ConstExpressions.ref.type(module.getTypes().structTypeByName(WasmHelpers.generateClassName(Type.getType(Object.class))), true));

        {
            // InstanceOf Check
            final StructType objectType = module.getTypes().structTypeByName(WasmHelpers.generateClassName(Type.getType(Object.class)));

            final List<Param> instanceOfParams = new ArrayList<>();
            instanceOfParams.add(ConstExpressions.param("obj", ConstExpressions.ref.type(objectType, true)));
            instanceOfParams.add(ConstExpressions.param("runtimeTypeId", PrimitiveType.i32));
            final ExportableFunction instanceOfCheck = module.getFunctions().newFunction("instanceOf", instanceOfParams, PrimitiveType.i32);
            final Local obj = instanceOfCheck.localByLabel("obj");
            final Local idx = instanceOfCheck.newLocal("idx", PrimitiveType.i32);
            final Local len = instanceOfCheck.newLocal("len", PrimitiveType.i32);

            final ReferencableType implTypesArray = types.arrayType(PrimitiveType.i32);
            final WasmType arrayType = ConstExpressions.ref.type(implTypesArray, true);
            final Local arr = instanceOfCheck.newLocal("arr", arrayType);

            // Null values are not equal
            final Iff nullCheck = instanceOfCheck.flow.iff("nullcheck", ConstExpressions.ref.eq(ConstExpressions.getLocal(obj), ConstExpressions.ref.nullRef()));
            nullCheck.flow.ret(ConstExpressions.i32.c(0));

            nullCheck.falseFlow.setLocal(
                    arr,
                    ConstExpressions.struct.get(
                            objectType,
                            ConstExpressions.getLocal(obj),
                            "implTypes"
                    )
            );
            nullCheck.falseFlow.setLocal(
                    len,
                    ConstExpressions.array.len(implTypesArray, ConstExpressions.getLocal(arr))
            );
            nullCheck.falseFlow.setLocal(
                    idx,
                    ConstExpressions.i32.c(0)
            );
            final Loop l = nullCheck.falseFlow.loop("iter");

            final Iff idxcheck = l.flow.iff("idxcheck", ConstExpressions.i32.eq(
                    ConstExpressions.getLocal(idx),
                    ConstExpressions.getLocal(len)
            ));
            idxcheck.flow.ret(ConstExpressions.i32.c(0));

            final Iff ischeck = idxcheck.falseFlow.iff("ischeck", ConstExpressions.i32.eq(
                    ConstExpressions.getLocal(instanceOfCheck.localByLabel("runtimeTypeId")),
                    ConstExpressions.array.get(implTypesArray, ConstExpressions.getLocal(arr), ConstExpressions.getLocal(idx))
            ));
            ischeck.flow.ret(ConstExpressions.i32.c(1));

            ischeck.falseFlow.setLocal(idx, ConstExpressions.i32.add(ConstExpressions.getLocal(idx), ConstExpressions.i32.c(1)));
            ischeck.falseFlow.branch(l);

            nullCheck.falseFlow.ret(ConstExpressions.i32.c(0));

            instanceOfCheck.flow.unreachable();
        }

        {
            // Runtimetypeof Check
            final StructType objectType = module.getTypes().structTypeByName(WasmHelpers.generateClassName(Type.getType(Object.class)));

            final List<Param> params = new ArrayList<>();
            params.add(ConstExpressions.param("obj", ConstExpressions.ref.type(objectType, true)));
            final ExportableFunction runtimetypeof = module.getFunctions().newFunction("runtimetypeof", params, ConstExpressions.ref.type(objectType, false));
            final Local obj = runtimetypeof.localByLabel("obj");
            final Local id = runtimetypeof.newLocal("id", PrimitiveType.i32);

            runtimetypeof.flow.setLocal(
                    id,
                    ConstExpressions.struct.get(
                            objectType,
                            ConstExpressions.getLocal(obj),
                            "typeId"
                    )
            );

            for (final ResolvedClass rl : resolvedClasses) {
                final int typeId = resolvedClasses.indexOf(rl);
                final Iff i = runtimetypeof.flow.iff("check_" + typeId, ConstExpressions.i32.eq(
                        ConstExpressions.i32.c(typeId), ConstExpressions.getLocal(id)
                ));

                final Global rttTypeGlobal = module.getGlobals().globalsIndex().globalByLabel(WasmHelpers.generateClassName(rl.type)  + "_cls");
                i.flow.ret(ConstExpressions.getGlobal(rttTypeGlobal));
            }

            runtimetypeof.flow.unreachable();
        }

        final OpaqueTypesAdapterMethods adapterMethods = new OpaqueTypesAdapterMethods();
        final StringConcatRegistry stringConcatRegistry = new StringConcatRegistry();

        for (final ResolvedClass cl : resolvedClasses) {
            // Class objects for

            final String className = WasmHelpers.generateClassName(cl.type);

            for (final ResolvedMethod method : cl.resolvedMethods) {
                if (method.owner == cl) {
                    final List<Param> functionParams = new ArrayList<>();

                    if (Modifier.isStatic(method.methodNode.access)) {
                        final WasmType clsRef = ConstExpressions.ref.type(module.getTypes().structTypeByName(WasmHelpers.generateClassName(Type.getType(Object.class))), true);
                        functionParams.add(ConstExpressions.param("unused", clsRef));
                    } else {
                        final WasmType thisRef = ConstExpressions.ref.type(module.getTypes().structTypeByName(WasmHelpers.generateClassName(Type.getType(Object.class))), true);
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

                        // Ok, do we need to create an opaque reference wrapper function
                        final Function function;
                        if (cl.isOpaqueReferenceType()) {
                            if (!Modifier.isStatic(method.methodNode.access)) {
                                throw new IllegalStateException("Native Methods must be static for opaque reference types : " + cl.type + "." + methodName);
                            }
                            if (Type.VOID == method.methodType.getReturnType().getSort()) {
                                throw new IllegalStateException("Native Methods must not be void for opaque reference types : " + cl.type + "." + methodName);
                            }

                            final Function delegateFunction = module.getImports().importFunction(new ImportReference(moduleName, functionName),
                                    implMethodName + "_delegate",
                                    functionParams.subList(1, functionParams.size()), ConstExpressions.ref.host());

                            final ExportableFunction impl = module.getFunctions().newFunction(implMethodName, functionParams, toWASMType.apply(method.methodType.getReturnType()));
                            final List<WasmValue> callArgs = new ArrayList<>();
                            for (int i = 0; i < method.methodType.getArgumentTypes().length; i++) {
                                final Type argument = method.methodType.getArgumentTypes()[i];
                                // TODO: Maybe convert opaque reference types here?
                                callArgs.add(ConstExpressions.getLocal(impl.localByLabel("arg" + i)));
                            }

                            switch (method.methodType.getReturnType().getSort()) {
                                case Type.VOID: {
                                    impl.flow.voidCall(delegateFunction, callArgs);
                                    break;
                                }
                                case Type.OBJECT: {
                                    impl.flow.ret(WasmStructuredControlflowCodeGenerator.createNewInstanceOf(
                                            method.methodType.getReturnType(),
                                            module,
                                            compileUnit,
                                            objectTypeMappings,
                                            rtTypeMappings,
                                            ConstExpressions.call(delegateFunction, callArgs)
                                    ));
                                    break;
                                }
                                default: {
                                    impl.flow.ret(
                                            ConstExpressions.call(delegateFunction, callArgs)
                                    );
                                    break;
                                }
                            }

                            function = impl;

                        } else {
                            if (Type.VOID == method.methodType.getReturnType().getSort()) {
                                function = module.getImports().importFunction(new ImportReference(moduleName, functionName),
                                        implMethodName,
                                        functionParams);
                            } else {
                                function = module.getImports().importFunction(new ImportReference(moduleName, functionName),
                                        implMethodName,
                                        functionParams, toWASMType.apply(method.methodType.getReturnType()));
                            }
                        }

                        if (!Modifier.isStatic(method.methodNode.access) && !"<init>".equals(method.methodNode.name)) {
                            function.toTable();
                        }

                    } else if (Modifier.isAbstract(method.methodNode.access)) {

                        if (cl.isOpaqueReferenceType()) {

                            adapterMethods.register(cl, method);

                            final ExportableFunction implFunction;
                            if (Type.VOID == method.methodType.getReturnType().getSort()) {
                                implFunction = module.getFunctions().newFunction(implMethodName, functionParams);
                            } else {
                                implFunction = module.getFunctions().newFunction(implMethodName, functionParams, toWASMType.apply(method.methodType.getReturnType()));
                            }

                            final String moduleName = cl.type.getClassName() + "_generated";

                            final List<Param> delegateParams = new ArrayList<>();
                            delegateParams.add(ConstExpressions.param("thisref", ConstExpressions.ref.host()));
                            for (int i = 0; i < method.methodType.getArgumentTypes().length; i++) {
                                switch (method.methodType.getArgumentTypes()[i].getSort()) {
                                    case Type.OBJECT: {
                                        final Type t = method.methodType.getArgumentTypes()[i];
                                        final ResolvedClass rc = compileUnit.findClass(t);
                                        if (rc.isCallback()) {
                                            delegateParams.add(ConstExpressions.param("arg" + i, toWASMType.apply(t)));
                                        } else {
                                            delegateParams.add(ConstExpressions.param("arg" + i, ConstExpressions.ref.host()));
                                        }
                                        break;
                                    }
                                    default: {
                                        delegateParams.add(ConstExpressions.param("arg" + i, toWASMType.apply(method.methodType.getArgumentTypes()[i])));
                                        break;
                                    }
                                }
                            }

                            final Function delegateFunction;
                            switch (method.methodType.getReturnType().getSort()) {
                                case Type.VOID: {
                                    delegateFunction = module.getImports().importFunction(new ImportReference(moduleName, methodName),
                                            implMethodName + "_delegate",
                                            delegateParams);
                                    break;
                                }
                                case Type.OBJECT: {
                                    delegateFunction = module.getImports().importFunction(new ImportReference(moduleName, methodName),
                                            implMethodName + "_delegate",
                                            delegateParams, ConstExpressions.ref.host());
                                    break;
                                }
                                default: {
                                    delegateFunction = module.getImports().importFunction(new ImportReference(moduleName, methodName),
                                            implMethodName + "_delegate",
                                            delegateParams, toWASMType.apply(method.methodType.getReturnType()));
                                    break;
                                }
                            }

                            final List<WasmValue> callArgs = new ArrayList<>();
                            callArgs.add(
                                    ConstExpressions.struct.get(
                                            objectTypeMappings.get(objectClass),
                                            ConstExpressions.getLocal(implFunction.localByLabel("thisref")),
                                            "nativeObject"
                                    )
                            );

                            for (int i = 0; i < method.methodType.getArgumentTypes().length; i++) {
                                final Type argument = method.methodType.getArgumentTypes()[i];
                                switch (argument.getSort()) {
                                    case Type.ARRAY: {
                                        throw new IllegalArgumentException("Arrays are not supported as an argument for " + method.methodNode.name +" in type " + cl.type);
                                    }
                                    case Type.OBJECT: {
                                        final ResolvedClass argClass = compileUnit.findClass(argument);
                                        if (argClass.isCallback()) {
                                            callArgs.add(ConstExpressions.getLocal(implFunction.localByLabel("arg" + i)));
                                        } else {
                                            callArgs.add(
                                                ConstExpressions.struct.get(
                                                        objectTypeMappings.get(objectClass),
                                                        ConstExpressions.getLocal(implFunction.localByLabel("arg" + i)),
                                                        "nativeObject"
                                                )
                                            );
                                        }
                                        break;
                                    }
                                    default: {
                                        callArgs.add(ConstExpressions.getLocal(implFunction.localByLabel("arg" + i)));
                                        break;
                                    }
                                }
                            }

                            if (Type.VOID == method.methodType.getReturnType().getSort()) {
                                implFunction.flow.voidCall(delegateFunction, callArgs);
                            } else {
                                switch (method.methodType.getReturnType().getSort()) {
                                    case Type.ARRAY: {
                                        throw new IllegalArgumentException("Arrays are not supported as a return type for " + method.methodNode.name +" in type " + cl.type);
                                    }
                                    case Type.OBJECT: {
                                        implFunction.flow.ret(
                                            WasmStructuredControlflowCodeGenerator.createNewInstanceOf(method.methodType.getReturnType(),
                                                    module,
                                                    compileUnit,
                                                    objectTypeMappings,
                                                    rtTypeMappings,
                                                    ConstExpressions.call(delegateFunction, callArgs)));
                                        break;
                                    }
                                    default: {
                                        implFunction.flow.ret(ConstExpressions.call(delegateFunction, callArgs));
                                        break;
                                    }
                                }
                            }

                            implFunction.toTable();

                        } else {
                            // Abstract method, nothing to be generated
                        }

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
                            new Sequencer(g, dt, new WasmStructuredControlflowCodeGenerator(compileUnit, module, rtTypeMappings, objectTypeMappings, implFunction, toWASMType, toFunctionType, methodToIDMapper, g, resolvedClasses, vTableResolver, stringConcatRegistry));
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

        final ResolvedClass stringClass = compileUnit.findClass(Type.getType(String.class));
        final Function stringInitFunction = initFunctions.get(stringClass);
        final StructType stringType = objectTypeMappings.get(stringClass);

        {
            final List<Param> newStringParam = new ArrayList<>();
            newStringParam.add(ConstExpressions.param("str", ConstExpressions.ref.host()));

            final ExportableFunction newStringFunction = module.getFunctions().newFunction("newStringFromJS",
                    newStringParam,
                    ConstExpressions.ref.type(objectTypeMappings.get(objectClass), false));

            final List<WasmValue> initArgs = new ArrayList<>();
            initArgs.add(
                    ConstExpressions.struct.get(
                            rtTypeMappings.get(stringClass),
                            ConstExpressions.call(stringInitFunction, new ArrayList<>()),
                            "typeId"
                    )
            );
            initArgs.add(ConstExpressions.ref.ref(module.functionIndex().firstByLabel(WasmHelpers.generateClassName(stringClass.type) + "_vt")));
            initArgs.add(ConstExpressions.getLocal(newStringFunction.localByLabel("str")));

            final Global stringGlobal = module.getGlobals().globalsIndex().globalByLabel(WasmHelpers.generateClassName(stringClass.type)  + "_cls");

            initArgs.add(
              ConstExpressions.struct.get(
                      rtTypeMappings.get(stringClass),
                      ConstExpressions.getGlobal(stringGlobal),
                      "classImplTypes"
              )
            );

            newStringFunction.flow.ret(
                    ConstExpressions.struct.newInstance(
                            stringType,
                            initArgs
                    )
            );
            newStringFunction.exportAs("newBytecoderString");
        }

        final List<Param> resolveStringConstantParams = new ArrayList<>();
        resolveStringConstantParams.add(ConstExpressions.param("constantId", PrimitiveType.i32));

        final Function resolveStringConstantFunction = module.getImports().importFunction(
                    new ImportReference("bytecoder", "resolveStringConstant"),
                 "resolveStringConstant",
                    resolveStringConstantParams,
                    ConstExpressions.ref.host());

        for (int i = 0; i < pooledStrings.size(); i++) {
            final Global stringpoolGlobal = module.globalsIndex().globalByLabel("stringpool_" + i);

            final List<WasmValue> initArgs = new ArrayList<>();
            initArgs.add(
                    ConstExpressions.struct.get(
                            rtTypeMappings.get(stringClass),
                            ConstExpressions.call(stringInitFunction, new ArrayList<>()),
                            "factoryFor"
                        ));
            initArgs.add(ConstExpressions.ref.ref(module.functionIndex().firstByLabel(WasmHelpers.generateClassName(stringClass.type) + "_vt")));

            final Global stringGlobal = module.getGlobals().globalsIndex().globalByLabel(WasmHelpers.generateClassName(stringClass.type)  + "_cls");

            final List<WasmValue> resolveArgs = new ArrayList<>();
            resolveArgs.add(ConstExpressions.i32.c(i));
            initArgs.add(ConstExpressions.call(resolveStringConstantFunction, resolveArgs));

            initArgs.add(
                    ConstExpressions.struct.get(
                            rtTypeMappings.get(stringClass),
                            ConstExpressions.getGlobal(stringGlobal),
                            "classImplTypes"
                    )
            );

            bootstrap.flow.setGlobal(
                    stringpoolGlobal,
                    ConstExpressions.struct.newInstance(
                            stringType,
                            initArgs
                    )
            );
        }
        bootstrap.exportAs("bootstrap");

        final StringWriter jsContentWriter = new StringWriter();
        final PrintWriter jsContentPrintWriter = new PrintWriter(jsContentWriter);
        try {
            final String runtimeCode = IOUtils.resourceToString("/wasmruntime.js", StandardCharsets.UTF_8);
            jsContentPrintWriter.println(runtimeCode);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to load Wasm runtime js code", e);
        }

        // Generate string constant pool resolver
        jsContentPrintWriter.println("bytecoder.imports[\"bytecoder\"].resolveStringConstant = function(index) {");
        jsContentPrintWriter.println("  switch(index) {");
        for (int i = 0; i < pooledStrings.size(); i++) {
            jsContentPrintWriter.print("      case ");
            jsContentPrintWriter.print(i);
            jsContentPrintWriter.print(": return '");
            jsContentPrintWriter.print(StringEscapeUtils.escapeEcmaScript(pooledStrings.get(i)));
            jsContentPrintWriter.println("';");
        }
        jsContentPrintWriter.println("  }");
        jsContentPrintWriter.println("  throw 'Unknown string index ' + index;");
        jsContentPrintWriter.println("};");

        // We need to create adapter methods for callback types
        for (final ResolvedClass cl : resolvedClasses) {
            if (cl.isCallback() && Modifier.isAbstract(cl.classNode.access) && !Callback.class.getName().equals(cl.type.getClassName())) {
                final List<ResolvedMethod> callbackMethods = cl.resolvedMethods.stream().filter(t -> !t.methodNode.name.equals("init")).collect(Collectors.toList());
                if (callbackMethods.size() != 1) {
                    throw new IllegalStateException("Unexpected number of callback methods, expected 1, got " + callbackMethods.size() + " for type " + cl.type);
                }

                final ResolvedMethod rm = callbackMethods.get(0);

                final List<Param> callbackParams = new ArrayList<>();
                callbackParams.add(ConstExpressions.param("receiver", ConstExpressions.ref.type(objectTypeMappings.get(objectClass), true)));

                boolean isObjectArgument = false;
                final Type argType = rm.methodType.getArgumentTypes()[0];
                switch (argType.getSort()) {
                    case Type.OBJECT: {
                        callbackParams.add(ConstExpressions.param("event", ConstExpressions.ref.host()));
                        isObjectArgument = true;
                        break;
                    }
                    default: {
                        callbackParams.add(ConstExpressions.param("event", toWASMType.apply(argType)));
                        break;
                    }
                }

                final ExportableFunction callback = module.getFunctions().newFunction(cl.type.getClassName() + "_callback", callbackParams);
                if (isObjectArgument) {
                    final Local eventInstance = callback.newLocal("eventInstance", ConstExpressions.ref.type(objectTypeMappings.get(objectClass), true));
                    callback.flow.setLocal(eventInstance, WasmStructuredControlflowCodeGenerator.createNewInstanceOf(
                            argType,
                            module,
                            compileUnit,
                            objectTypeMappings,
                            rtTypeMappings,
                            ConstExpressions.getLocal(callback.localByLabel("event"))
                    ));
                }

                // Virtual call
                final List<WasmValue> indirectCallArgs = new ArrayList<>();

                final List<WasmType> vlResolveArgs = new ArrayList<>();
                vlResolveArgs.add(PrimitiveType.i32);
                final FunctionType vtResolveType = module.getTypes().functionType(vlResolveArgs, PrimitiveType.i32);
                indirectCallArgs.add(ConstExpressions.getLocal(callback.localByLabel("receiver")));
                if (isObjectArgument) {
                    indirectCallArgs.add(ConstExpressions.getLocal(callback.localByLabel("eventInstance")));
                } else {
                    indirectCallArgs.add(ConstExpressions.getLocal(callback.localByLabel("event")));
                }

                final List<WasmValue> resolverArgs = new ArrayList<>();
                resolverArgs.add(ConstExpressions.i32.c(methodToIDMapper.resolveIdFor(rm)));

                final StructType objectType = module.getTypes().structTypeByName(WasmHelpers.generateClassName(Type.getType(Object.class)));

                resolverArgs.add(ConstExpressions.struct.get(
                        objectType,
                        ConstExpressions.getLocal(callback.localByLabel("receiver")),
                        "vt_resolver"
                ));

                final WasmValue resolver = ConstExpressions.ref.callRef(vtResolveType, resolverArgs);
                final FunctionType ft = toFunctionType.apply(rm);

                callback.flow.voidCallIndirect(ft, indirectCallArgs, resolver);

                callback.exportAs(cl.type.getClassName() + "_callback");
            }
        }

        final List<StringConcatMethod> stringConcatMethods = stringConcatRegistry.getMethods();
        for (final StringConcatMethod stringConcatMethod : stringConcatMethods) {
            stringConcatMethod.generateCode(jsContentPrintWriter, stringConcatMethods.indexOf(stringConcatMethod));
        }

        adapterMethods.getKnownMethods().forEach((cl, value) -> {
            final String moduleName = cl.type.getClassName() + "_generated";

            jsContentPrintWriter.print("bytecoder.imports[\"");
            jsContentPrintWriter.print(moduleName);
            jsContentPrintWriter.println("\"] = {");

            for (final ResolvedMethod rm : value) {
                final String methodName = WasmHelpers.generateMethodName(rm.methodNode.name, rm.methodType);
                jsContentPrintWriter.print("    ");
                jsContentPrintWriter.print(methodName);
                jsContentPrintWriter.print(" : function(thisref");
                for (int i = 0; i < rm.methodType.getArgumentTypes().length; i++) {
                    jsContentPrintWriter.print(", arg");
                    jsContentPrintWriter.print(i);
                }
                jsContentPrintWriter.println(") {");

                final Type[] arguments = rm.methodType.getArgumentTypes();

                if (AnnotationUtils.hasAnnotation("Lde/mirkosertic/bytecoder/api/OpaqueProperty;", rm.methodNode.visibleAnnotations)) {
                    final Map<String, Object> values = AnnotationUtils.parseAnnotation("Lde/mirkosertic/bytecoder/api/OpaqueProperty;", rm.methodNode.visibleAnnotations);
                    final String propertyName = (String) values.get("value");

                    final Type methodType = rm.methodType;
                    switch (methodType.getReturnType().getSort()) {
                        case Type.BOOLEAN: {
                            jsContentPrintWriter.print("        return bytecoder.toBytecoderBoolean(");
                            break;
                        }
                        case Type.VOID: {
                            jsContentPrintWriter.print("        (");
                            break;
                        }
                        default: {
                            jsContentPrintWriter.print("        return (");
                            break;
                        }
                    }

                    jsContentPrintWriter.print("thisref.");
                    if (propertyName != null) {
                        jsContentPrintWriter.print(propertyName);
                    } else {
                        jsContentPrintWriter.print(OpaqueReferenceTypeHelpers.derivePropertyNameFromMethodName(rm.methodNode.name));
                    }
                    if (arguments.length > 0) {
                        jsContentPrintWriter.print(" = ");
                        switch (arguments[0].getSort()) {
                            case Type.BOOLEAN: {
                                jsContentPrintWriter.print("(arg0 === 1 ? true : false)");
                                break;
                            }
                            default: {
                                jsContentPrintWriter.print("arg0");
                                break;
                            }
                        }
                    }

                    jsContentPrintWriter.println(");");

                } else if (AnnotationUtils.hasAnnotation("Lde/mirkosertic/bytecoder/api/OpaqueIndexed;", rm.methodNode.visibleAnnotations)) {

                    if (arguments.length > 1) {
                        jsContentPrintWriter.println("        thisref[arg0] = arg1;");
                    } else {
                        jsContentPrintWriter.println("        return thisref[arg0];");
                    }

                } else {

                    final Type returnType = rm.methodType.getReturnType();
                    switch (returnType.getSort()) {
                        case Type.BOOLEAN: {
                            jsContentPrintWriter.print("        return bytecoder.toBytecoderBoolean(");
                            break;
                        }
                        case Type.VOID: {
                            jsContentPrintWriter.print("        (");
                            break;
                        }
                        default: {
                            jsContentPrintWriter.print("        return (");
                            break;
                        }
                    }

                    jsContentPrintWriter.print("thisref.");
                    jsContentPrintWriter.print(rm.methodNode.name);
                    jsContentPrintWriter.print("(");

                    for (int i = 0; i < arguments.length; i++) {
                        if (i > 0) {
                            jsContentPrintWriter.print(", ");
                        }
                        final Type argType = arguments[i];
                        switch (argType.getSort()) {
                            case Type.BOOLEAN: {
                                jsContentPrintWriter.print("(arg");
                                jsContentPrintWriter.print(i);
                                jsContentPrintWriter.print(" === 1 ? true : false)");
                                break;
                            }
                            case Type.OBJECT: {
                                final ResolvedClass typeClass = compileUnit.findClass(argType);
                                if (typeClass == null) {
                                    throw new IllegalStateException("Cannot find linked class for type " + argType);
                                }
                                if (typeClass.isCallback()) {
                                    if (!Modifier.isInterface(typeClass.classNode.access)) {
                                        throw new IllegalStateException("Only callback interfaces are allowed in method signatures!");
                                    }

                                    final List<ResolvedMethod> callbackMethods = typeClass.resolvedMethods.stream().filter(t -> !t.methodNode.name.equals("init")).collect(Collectors.toList());
                                    if (callbackMethods.size() != 1) {
                                        throw new IllegalStateException("Unexpected number of callback methods, expected 1, got " + callbackMethods.size() + " for type " + typeClass.type);
                                    }
                                    final ResolvedMethod callbackMethod = callbackMethods.get(0);
                                    final Type methodType = callbackMethod.methodType;

                                    jsContentPrintWriter.print("function(evt) {bytecoder.instance.exports['");
                                    jsContentPrintWriter.print(typeClass.type.getClassName());
                                    jsContentPrintWriter.print("_callback'](arg");
                                    jsContentPrintWriter.print(i);
                                    jsContentPrintWriter.print(",evt);}");
                                } else {
                                    jsContentPrintWriter.print("arg");
                                    jsContentPrintWriter.print(i);
                                }
                                break;
                            }
                            default: {
                                jsContentPrintWriter.print("arg");
                                jsContentPrintWriter.print(i);
                                break;
                            }
                        }
                    }

                    jsContentPrintWriter.println("));");
                }
                jsContentPrintWriter.println("    },");
            }
            jsContentPrintWriter.println("};");
        });

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

        // Flush stuff
        jsContentWriter.flush();
        jsContentPrintWriter.flush();

        final WasmCompileResult result = new WasmCompileResult();
        if (!StringUtils.isEmpty(compileOptions.getFilenamePrefix())) {
            result.add(new CompileResult.StringContent(compileOptions.getFilenamePrefix() + "wasmclasses.wat", theStringWriter.toString()));
            result.add(new CompileResult.StringContent(compileOptions.getFilenamePrefix() + "runtime.js", jsContentWriter.toString()));
            result.add(new CompileResult.BinaryContent(compileOptions.getFilenamePrefix() + "wasmclasses.wasm", theBinaryOutput.toByteArray()));

        } else{
            result.add(new CompileResult.BinaryContent("wasmclasses.wasm", theBinaryOutput.toByteArray()));
            result.add(new CompileResult.StringContent("wasmclasses.wat", theStringWriter.toString()));
            result.add(new CompileResult.StringContent("runtime.js", jsContentWriter.toString()));
        }

        final List<String> resourcesToInclude = new ArrayList<>();
        for (final ClassLibProvider provider : ClassLibProvider.availableProviders()) {
            Collections.addAll(resourcesToInclude, provider.additionalResources());
        }
        Collections.addAll(resourcesToInclude, compileOptions.getAdditionalResources());

        for (final String theResource : resourcesToInclude) {
            final URL theUrl = compileUnit.getLoader().getResource(theResource);
            if (theUrl != null) {
                result.add(new CompileResult.URLContent(theResource, theUrl));
            } else {
                compileOptions.getLogger().warn("Cannot find resource {}", theResource);
            }
        }

        return result;
    }
}
