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

import de.mirkosertic.bytecoder.asm.backend.CompileOptions;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.ConstExpressions;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.ExportableFunction;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Exporter;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.FunctionType;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.FunctionsSection;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Global;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.GlobalsSection;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Iff;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Module;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Param;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.PrimitiveType;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.ReferencableType;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.StructType;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.TypesSection;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.WasmType;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.WasmValue;
import de.mirkosertic.bytecoder.asm.ir.ResolvedClass;
import de.mirkosertic.bytecoder.asm.ir.ResolvedField;
import de.mirkosertic.bytecoder.asm.parser.CompileUnit;
import de.mirkosertic.bytecoder.backend.CompileResult;
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

        // Type for runtime types
        final List<StructType.Field> rtFields = new ArrayList<>();
        rtFields.add(new StructType.Field("typeId", PrimitiveType.i32, false));
        rtFields.add(new StructType.Field("impTypes", ConstExpressions.ref.type(implTypesArray, true), false));
        rtFields.add(new StructType.Field("lambdaMethod", PrimitiveType.i32, false));
        rtFields.add(new StructType.Field("vt_resolver", ConstExpressions.ref.type(vtType, true), false));
        rtFields.add(new StructType.Field("initStatus", PrimitiveType.i32));
        final StructType rtType = types.structType("runtimetype", rtFields);

        final FunctionType iType = types.functionType(Collections.emptyList(), ConstExpressions.ref.type(rtType, false));

        final Map<ResolvedClass, StructType> objectTypeMappings = new HashMap<>();
        final Map<ResolvedClass, StructType> rtTypeMappings = new HashMap<>();
        ResolvedClass objectClass = null;

        final FunctionsSection functionsSection = module.getFunctions();

        final GlobalsSection globalsSection = module.getGlobals();

        for (final ResolvedClass cl : resolvedClasses) {
            // Class objects for
            final String className = WasmHelpers.generateClassName(cl.type);

            final List<StructType.Field> instanceFields = new ArrayList<>();
            if (cl.superClass == null) {
                instanceFields.add(new StructType.Field("runtimetype", ConstExpressions.ref.type(rtType, false)));
            }

            if (cl.isNativeReferenceHolder()) {
                instanceFields.add(new StructType.Field("nativeObject", PrimitiveType.anyref));
            }

            // TODO: Array types!

            final List<StructType.Field> classFields = new ArrayList<>();
            final List<WasmValue> classFieldDefaults = new ArrayList<>();

            for (final ResolvedField rf : cl.resolvedFields) {
                if (rf.owner == cl) {
                    final String fieldName = WasmHelpers.generateFieldName(rf.name);

                    StructType.Field field = null;

                    WasmValue defaultValue = null;
                    switch (rf.type.getSort()) {
                        case Type.BYTE:
                            field = new StructType.Field(fieldName, PrimitiveType.i32);
                            defaultValue = ConstExpressions.i32.c(0);
                            break;
                        case Type.CHAR:
                            field = new StructType.Field(fieldName, PrimitiveType.i32);
                            defaultValue = ConstExpressions.i32.c(0);
                            break;
                        case Type.SHORT:
                            field = new StructType.Field(fieldName, PrimitiveType.i32);
                            defaultValue = ConstExpressions.i32.c(0);
                            break;
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
                            defaultValue = ConstExpressions.ref.nullRef(objectTypeMappings.get(objectClass));
                            break;
                        case Type.ARRAY:
                            // TODO:
                            break;
                    }

                    if (field != null) {
                        if (Modifier.isStatic(rf.access)) {
                            classFields.add(field);
                            classFieldDefaults.add(defaultValue);
                        } else {
                            instanceFields.add(field);
                        }
                    }
                }
            }

            if (!Modifier.isInterface(cl.classNode.access)) {
                if (cl.superClass == null) {
                    objectClass = cl;
                    objectTypeMappings.put(cl, types.structType(className, instanceFields));
                } else {
                    objectTypeMappings.put(cl, types.structSubtype(className, objectTypeMappings.get(cl.superClass), instanceFields));
                }
            }

            rtTypeMappings.put(cl, types.structSubtype(className + "_rtt", rtType, classFields));

            // TODO: Generate vtable
            final List<Param> params = new ArrayList<>();
            params.add(ConstExpressions.param("methodid", PrimitiveType.i32));
            final ExportableFunction vtFunction = functionsSection.newFunction(className + "_vt", vtType, params, PrimitiveType.i32);
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
            final Global g = globalsSection.newConstantGlobal(className + "_cls",
                    ConstExpressions.ref.type(rttType, false),
                    ConstExpressions.struct.newInstance(
                            rttType, initArgs
                    )
            );

            final ExportableFunction initFunction = functionsSection.newFunction(className + "_i", iType, ConstExpressions.ref.type(rtType, false));
            final Iff initCheck = initFunction.flow.iff("check", ConstExpressions.i32.eq(ConstExpressions.i32.c(0),
                    ConstExpressions.struct.get(rtType, ConstExpressions.getGlobal(g), 4)));
            initCheck.flow.setStruct(rtType, ConstExpressions.getGlobal(g), 4, ConstExpressions.i32.c(1));
            // TODO: Call class init function here
            initCheck.flow.branch(initCheck);
            initFunction.flow.ret(ConstExpressions.getGlobal(g));


            // TODO: generate impl functions
        }

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

        System.out.println(theStringWriter);

        final WasmCompileResult result = new WasmCompileResult();
        result.add(new CompileResult.BinaryContent(compileOptions.getFilenamePrefix() + "wasmclasses.wasm", theBinaryOutput.toByteArray()));
        result.add(new CompileResult.StringContent(compileOptions.getFilenamePrefix() + "wasmclasses.wat", theStringWriter.toString()));

        return result;
    }
}
