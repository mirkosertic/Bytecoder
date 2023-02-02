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
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Exporter;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.FunctionType;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Module;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.PrimitiveType;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.StructType;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.TypesSection;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.WasmType;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WasmBackend {

    public WasmCompileResult generateCodeFor(final CompileUnit compileUnit, final CompileOptions compileOptions) {

        final Module module = new Module("bytecoder", compileOptions.getFilenamePrefix() + ".wasm.map");

        final TypesSection types = module.getTypes();
        // Type for virtual function resolvers
        final List<WasmType> vtArgs = new ArrayList<>();
        vtArgs.add(PrimitiveType.i32);
        final FunctionType vtType = types.functionType(vtArgs, PrimitiveType.i32);

        // Type for runtime types
        final List<StructType.Field> rtFields = new ArrayList<>();
        rtFields.add(new StructType.Field("vt_resolver", ConstExpressions.ref(vtType)));
        final StructType rtType = types.structType("runtimetype", rtFields);

        final Map<ResolvedClass, StructType> objectTypeMappings = new HashMap<>();
        ResolvedClass objectClass = null;

        for (final ResolvedClass cl : compileUnit.computeClassDependencies()) {
            if (!Modifier.isInterface(cl.classNode.access)) {
                // Class objects for
                final String className = WasmHelpers.generateClassName(cl.type);

                final List<StructType.Field> fields = new ArrayList<>();
                if (cl.superClass == null) {
                    fields.add(new StructType.Field("runtimetype", ConstExpressions.ref(rtType)));
                }

                for (final ResolvedField rf : cl.resolvedFields) {
                    if ((rf.owner == cl) && (!Modifier.isStatic(rf.access))) {
                        final String fieldName = WasmHelpers.generateFieldName(rf.name);
                        switch (rf.type.getSort()) {
                            case Type.BYTE:
                                fields.add(new StructType.Field(fieldName, PrimitiveType.i32));
                                break;
                            case Type.CHAR:
                                fields.add(new StructType.Field(fieldName, PrimitiveType.i32));
                                break;
                            case Type.SHORT:
                                fields.add(new StructType.Field(fieldName, PrimitiveType.i32));
                                break;
                            case Type.INT:
                                fields.add(new StructType.Field(fieldName, PrimitiveType.i32));
                                break;
                            case Type.LONG:
                                fields.add(new StructType.Field(fieldName, PrimitiveType.i64));
                                break;
                            case Type.FLOAT:
                                fields.add(new StructType.Field(fieldName, PrimitiveType.f32));
                                break;
                            case Type.DOUBLE:
                                fields.add(new StructType.Field(fieldName, PrimitiveType.f64));
                                break;
                            case Type.OBJECT:
                                fields.add(new StructType.Field(fieldName, ConstExpressions.ref(objectTypeMappings.get(objectClass))));
                                break;
                            case Type.ARRAY:
                                //TODO
                                break;
                        }
                    }
                }

                if (cl.superClass == null) {
                    objectClass = cl;
                    objectTypeMappings.put(cl, types.structType(className, fields));
                } else {
                    objectTypeMappings.put(cl, types.structSubtype(className, objectTypeMappings.get(cl.superClass), fields));
                }
            }
        }


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
