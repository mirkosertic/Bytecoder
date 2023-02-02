package de.mirkosertic.bytecoder.asm.backend.wasm.ast;

import de.mirkosertic.bytecoder.asm.backend.CompileOptions;
import de.mirkosertic.bytecoder.asm.optimizer.Optimizations;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;


public class ModuleTest {

    @Test
    public void testType() {
        final CompileOptions options = new CompileOptions(new Slf4JLogger(), Optimizations.DISABLED, new String[0], "", true);
        final Module module = new Module("bytecoder", "bytecoder" + ".wasm.map");

        final List<WasmType> args = new ArrayList<>();
        args.add(PrimitiveType.i32);
        args.add(PrimitiveType.f32);
        module.getTypes().functionType(args, PrimitiveType.i32);

        final List<StructType.Field> fields = new ArrayList<>();
        fields.add(new StructType.Field("a", PrimitiveType.i32));
        final StructType supertype = module.getTypes().structType("parent", fields);

        final List<StructType.Field> otherfields = new ArrayList<>();
        otherfields.add(new StructType.Field("b", PrimitiveType.f32));

        module.getTypes().structSubtype("child", supertype, otherfields);

        /*module.getGlobals().newConstantGlobal("glob",
                new RefType(supertype),
                ConstExpressions.i32.c(10));*/

        final StringWriter theStringWriter = new StringWriter();
        final ByteArrayOutputStream theBinaryOutput = new ByteArrayOutputStream();
        final StringWriter theBinarySourceMap = new StringWriter();
        try {
            final PrintWriter theWriter = new PrintWriter(theStringWriter);
            final Exporter exporter = new Exporter(options);
            exporter.export(module, theWriter);
            exporter.export(module, theBinaryOutput, theBinarySourceMap);

            theBinaryOutput.flush();
            theStringWriter.flush();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println(theStringWriter);
    }

}
