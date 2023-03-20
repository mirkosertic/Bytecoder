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
package de.mirkosertic.bytecoder.core.backend.wasm.ast;

import de.mirkosertic.bytecoder.core.Slf4JLogger;
import de.mirkosertic.bytecoder.core.backend.CompileOptions;
import de.mirkosertic.bytecoder.core.optimizer.Optimizations;
import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class BinaryFormatTest {

    @Test
    public void testSimpleStructType() throws IOException {
        final Module module = new Module("test", "");
        final List<StructType.Field> fields = new ArrayList<>();
        fields.add(new StructType.Field("field1", PrimitiveType.i32));
        fields.add(new StructType.Field("field2", PrimitiveType.f32));
        final StructType str = module.getTypes().structType("str", fields);

        final List<StructType.Field> subtypeFields = new ArrayList<>();
        subtypeFields.add(new StructType.Field("field3", PrimitiveType.i64));
        subtypeFields.add(new StructType.Field("field4", ConstExpressions.ref.type(str, true)));

        final StructSubtype sub = module.getTypes().structSubtype("sub", str, subtypeFields);

        final ArrayType arr = module.getTypes().arrayType(PrimitiveType.i32);

        final ExportableFunction f = module.getFunctions().newFunction("lala");
        f.newLocal("local", ConstExpressions.ref.type(str, true));
        final List<WasmValue> args = new ArrayList<>();
        args.add(ConstExpressions.i32.c(10));
        args.add(ConstExpressions.f32.c(20f));
        args.add(ConstExpressions.i64.c(30L));
        args.add(ConstExpressions.ref.nullRef());
        f.flow.drop(ConstExpressions.struct.newInstance(sub, args));

        final List<WasmValue> args2 = new ArrayList<>();
        args2.add(ConstExpressions.i32.c(10));
        args2.add(ConstExpressions.f32.c(20f));
        f.flow.drop(ConstExpressions.struct.newInstance(str, args2));

        final List<WasmValue> arrayValues = new ArrayList<>();
        arrayValues.add(ConstExpressions.i32.c(100));
        f.flow.drop(ConstExpressions.array.newInstance(arr,arrayValues));

        f.flow.drop(ConstExpressions.ref.ref(f));
        f.flow.drop(ConstExpressions.i64.c(100000L));
        f.flow.drop(ConstExpressions.f64.c(0.2d));
        f.flow.drop(ConstExpressions.ref.nullRef());
        f.flow.drop(ConstExpressions.ref.externNullRef());

        final List<WasmType> exceptionArguments = new ArrayList<>();
        exceptionArguments.add(ConstExpressions.ref.type(str, true));
        module.getTags().tagIndex().add(ConstExpressions.tag("javaexception",  module.getTypes().functionType(exceptionArguments)));

        final CompileOptions options = new CompileOptions(new Slf4JLogger(), Optimizations.DISABLED, new String[0], "prefix", true);
        final Exporter exporter = new Exporter(options);

        try (final FileOutputStream fos = new FileOutputStream("target/testfile.wasm")) {
            exporter.export(module, fos);
        }

        try (final PrintWriter pw = new PrintWriter(new FileOutputStream("target/testfile.wat"))) {
            exporter.export(module, pw);
        }
    }
}
