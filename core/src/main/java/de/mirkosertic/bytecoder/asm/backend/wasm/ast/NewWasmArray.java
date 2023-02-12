package de.mirkosertic.bytecoder.asm.backend.wasm.ast;

import java.io.IOException;
import java.util.List;

public class NewWasmArray implements WasmValue {

    private final WasmType type;

    private final WasmValue length;
    private final List<WasmValue> arguments;

    NewWasmArray(final WasmType type, final WasmValue length, final List<WasmValue> arguments) {
        this.type = type;
        this.length = length;
        this.arguments = arguments;
    }

    @Override
    public void writeTo(final TextWriter writer, final ExportContext context) throws IOException {
        writer.opening();
        writer.write("array.new ");
        type.writeRefTo(writer);
        writer.space();
        length.writeTo(writer, context);
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
