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

import java.io.File;
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

        final ExportableFunction f = module.getFunctions().newFunction("lala");
        final List<WasmValue> args = new ArrayList<>();
        args.add(ConstExpressions.i32.c(10));
        args.add(ConstExpressions.f32.c(20f));
        f.flow.drop(ConstExpressions.struct.newInstance(str, args));

        final CompileOptions options = new CompileOptions(new Slf4JLogger(), Optimizations.DISABLED, new String[0], "prefix", true);
        final Exporter exporter = new Exporter(options);

        try (FileOutputStream fos = new FileOutputStream(new File("target/testfile.wasm"))) {
            exporter.export(module, fos);
        }

         try (PrintWriter pw = new PrintWriter(new FileOutputStream(new File("target/testfile.wat")))) {
             exporter.export(module, pw);
         }

         /*

Generated:

[mirkosertic@laptop-mse target]$ xxd testfile.wasm
00000000: 0061 736d 0100 0000 0107 015f 027f 017d  .asm......._...}
00000010: 0102 0100 0301 0104 0100 0501 0006 0100  ................
00000020: 0701 0009 0100 0a0f 010d 0041 0a43 0000  ...........A.C..
00000030: a041 fb07 001a 0b00 1a04 6e61 6d65 0005  .A........name..
00000040: 0474 6573 7401 0701 0004 6c61 6c61 0203  .test.....lala..
00000050: 0100 0000 1210 736f 7572 6365 4d61 7070  ......sourceMapp
00000060: 696e 6755 524c 00                        ingURL.

Correct

[mirkosertic@laptop-mse target]$ xxd testfile.wasm
00000000: 0061 736d 0100 0000 010a 0260 0000 5f02  .asm.......`.._.
00000010: 7f01 7d01 0302 0100 0a0f 010d 0041 0a43  ..}..........A.C
00000020: 0000 a041 fb07 011a 0b00 3204 6e61 6d65  ...A......2.name
00000030: 0005 0474 6573 7401 0701 0004 6c61 6c61  ...test.....lala
00000040: 0406 0101 0373 7472 0a13 0101 0201 0666  .....str.......f
00000050: 6965 6c64 3200 0666 6965 6c64 31         ield2..field1





          */
     }
}
