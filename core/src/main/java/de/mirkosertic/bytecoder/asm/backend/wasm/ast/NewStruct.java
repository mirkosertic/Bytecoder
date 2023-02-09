package de.mirkosertic.bytecoder.asm.backend.wasm.ast;

import java.io.IOException;
import java.util.List;

public class NewStruct implements WasmValue{

    private final ReferencableType type;

    private final List<WasmValue> arguments;

    NewStruct(final ReferencableType type, final List<WasmValue> arguments) {
        this.type = type;
        this.arguments = arguments;
    }

    @Override
    public void writeTo(final TextWriter writer, final ExportContext context) throws IOException {
        writer.opening();
        writer.write("struct.new ");
        type.writeRefTo(writer);
        for (final WasmValue arg : arguments) {
            writer.space();
            arg.writeTo(writer, context);
        }
        writer.closing();

    }

    @Override
    public void writeTo(final BinaryWriter.Writer binaryWriter, final ExportContext context) {
        //TODO
    }
}
